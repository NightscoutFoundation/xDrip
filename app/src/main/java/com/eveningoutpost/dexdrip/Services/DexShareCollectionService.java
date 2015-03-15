package com.eveningoutpost.dexdrip.Services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.ReadDataShare;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.DexShareAttributes;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import rx.Observable;
import rx.functions.Action1;

public class DexShareCollectionService extends Service {
    private final static String TAG = DexShareCollectionService.class.getSimpleName();
    private String mDeviceAddress;
    private String mDeviceName;
    private boolean is_connected = false;
    private boolean reconnecting = false;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mBluetoothDeviceAddress;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    private BluetoothGattService mShareService;
    private BluetoothGattCharacteristic mAuthenticationCharacteristic;
    private BluetoothGattCharacteristic mSendDataCharacteristic;
    private BluetoothGattCharacteristic mReceiveDataCharacteristic;
    private BluetoothGattCharacteristic mCommandCharacteristic;
    private BluetoothGattCharacteristic mResponseCharacteristic;

    //Gatt Tasks
    public final int GATT_NOTHING = 0;
    public final int GATT_SETUP = 1;
    public final int GATT_WRITING_COMMANDS = 2;
    public final int GATT_READING_RESPONSE = 3;
    public int successfulWrites;

    //RXJAVA FUN
    Action1<byte[]> mDataResponseListener;
    public int currentGattTask;
    public int step;
    public List<byte[]> writePackets;
    public int recordType;
    SharedPreferences prefs;
    ReadDataShare readData;

    public boolean shouldDisconnect = true;
    public boolean share2 = false;

    @Override
    public void onCreate() {
        super.onCreate();
        readData = new ReadDataShare(this);
        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, bondintent);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion < android.os.Build.VERSION_CODES.LOLLIPOP){
            shouldDisconnect = false;
        }
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            setFailoverTimer();
        } else {
            return START_NOT_STICKY;
        }
        if (Sensor.currentSensor() == null) {
            setRetryTimer();
            return START_NOT_STICKY;
        }
        Log.w(TAG, "STARTING SERVICE");
        attemptConnection();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        setRetryTimer();
        unregisterReceiver(mPairReceiver);
        Log.w(TAG, "SERVICE STOPPED");
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.compareTo("dex_collection_method") == 0) {
                CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter();
                collectionServiceStarter.start(getApplicationContext());
            }
        }
    };

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public void setRetryTimer() {
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            BgReading bgReading = BgReading.last();
            long retry_in;
            if (bgReading != null) {
                retry_in = Math.min(Math.max((1000 * 60), (1000 * 60 * 5) - (new Date().getTime() - bgReading.timestamp) + 30), (1000 * 60 * 6));
            } else {
                retry_in = (1000 * 60);
            }
            Log.d(TAG, "Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexShareCollectionService.class), 0));
        }
    }

    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            long retry_in = (1000 * 60 * 3);
            Log.d(TAG, "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexShareCollectionService.class), 0));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void attemptConnection() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            if (device != null) {
                mConnectionState = STATE_DISCONNECTED;
                for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                    if (bluetoothDevice.getAddress().compareTo(device.getAddress()) == 0) {
                        mConnectionState = STATE_CONNECTED;
                    }
                }
            }
            Log.w(TAG, "Connection state: " + mConnectionState);
            if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
                ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
                if (btDevice != null) {
                    mDeviceName = btDevice.name;
                    mDeviceAddress = btDevice.address;
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getRemoteDevice(mDeviceAddress) != null) {
                        connect(mDeviceAddress);
                        return;
                    } else {
                        Log.w(TAG, "Bluetooth is disabled or BT device cant be found");
                        setRetryTimer();
                        return;
                    }
                } else {
                    Log.w(TAG, "No bluetooth device to try and connect to");
                    setRetryTimer();
                    return;
                }
            } else if (mConnectionState == STATE_CONNECTED) {
                Log.w(TAG, "Looks like we are already connected, going to read!");
                attemptRead();
                return;
            } else {
                setRetryTimer();
                return;
            }
        } else {
            setRetryTimer();
            return;
        }
    }

    public void attemptRead() {
        Log.d(TAG, "Attempting to read data");
        final Action1<Long> systemTimeListener = new Action1<Long>() {
            @Override
            public void call(Long s) {
                if (s != null) {
                    Log.d(TAG, "Made the full round trip, got " + s + " as the system time");
                    final long addativeSystemTimeOffset = new Date().getTime() - s;

                    final Action1<EGVRecord[]> evgRecordListener = new Action1<EGVRecord[]>() {
                        @Override
                        public void call(EGVRecord[] egvRecords) {
                            if (egvRecords != null) {
                                Log.d(TAG, "Made the full round trip, got " + egvRecords.length + " EVG Records");
                                BgReading.create(egvRecords, addativeSystemTimeOffset, getApplicationContext());
                                if(shouldDisconnect) {
                                    stopSelf();
                                } else {
                                    setRetryTimer();
                                }
                            }
                        }
                    };

                    final Action1<SensorRecord[]> sensorRecordListener = new Action1<SensorRecord[]>() {
                        @Override
                        public void call(SensorRecord[] sensorRecords) {
                            if (sensorRecords != null) {
                                Log.d(TAG, "Made the full round trip, got " + sensorRecords.length + " Sensor Records");
                                BgReading.create(sensorRecords, addativeSystemTimeOffset, getApplicationContext());
                                readData.getRecentEGVs(evgRecordListener);
                            }
                        }
                    };

                    final Action1<CalRecord[]> calRecordListener = new Action1<CalRecord[]>() {
                        @Override
                        public void call(CalRecord[] calRecords) {
                            if (calRecords != null) {
                                Log.d(TAG, "Made the full round trip, got " + calRecords.length + " Cal Records");
                                Calibration.create(calRecords, addativeSystemTimeOffset, getApplicationContext());
                                readData.getRecentSensorRecords(sensorRecordListener);
                            }
                        }
                    };
                    readData.getRecentCalRecords(calRecordListener);
                }
            }
        };
        readData.readSystemTime(systemTimeListener);
    }

    public boolean connect(final String address) {
        Log.w(TAG, "going to connect to device at address" + address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            setRetryTimer();
            return false;
        }
        if (mBluetoothGatt != null) {
            Log.w(TAG, "BGatt isnt null, Closing.");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
                Log.w(TAG, "Device found, already bonded, going to connect");
               if(mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress()) != null) {
                   device = bluetoothDevice;
                   device.setPin("000000".getBytes());
                   device.setPairingConfirmation(true);
                   mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
//                   refreshDeviceCache(mBluetoothGatt);
                   return true;
               }
            }
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            setRetryTimer();
            return false;
        }
        device.setPin("000000".getBytes());
        device.setPairingConfirmation(true);

        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void authenticateConnection() {
        Log.w(TAG, "Trying to auth");
        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase() + "000000";
        share2 = prefs.getBoolean("share_auth_mode_two", false);
        byte[] bondkey = (receiverSn).getBytes(StandardCharsets.US_ASCII);


        if (mBluetoothGatt != null) {
            if(!share2) {
                mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService);
                if (mShareService != null) {
                    mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode);
                    if (mAuthenticationCharacteristic != null) {
                        Log.w(TAG, "Auth Characteristic found: " + mAuthenticationCharacteristic.toString());
                        if (mAuthenticationCharacteristic.setValue(bondkey)) {
                            currentGattTask = GATT_SETUP;
                            step = 1;
                            mBluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                        } else {
                            setRetryTimer();
                        }
                    } else {
                        Log.w(TAG, "Authentication Characteristic IS NULL");
                        setRetryTimer();
                    }
                } else {
                    Log.w(TAG, "STANDARD CRADLE SERVICE IS NULL");
                }
            } else {
                mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService2);
                if (mShareService != null) {
                    BluetoothGattService service = mShareService;
                    Log.e(TAG, "=================================================================================");
                    Log.e(TAG, "AFTERAUTHUUIDS Service: " + service.getUuid());
                    for(BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        Log.e(TAG, "Characteristic: " + characteristic.getUuid());
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_INDICATE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE));
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_NOTIFY: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY));
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_READ: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ));
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_SIGNED_WRITE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE));
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_WRITE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE));
                        Log.e(TAG, "CHARACTERISTIC PROPERTY_WRITE_NO_RESPONSE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
                        Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE));
                        Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_ENCRYPTED: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED));
                        Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_ENCRYPTED_MITM: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM));
                        Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_SIGNED: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED));
                        Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_SIGNED_MITM: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM));


                    }
                    mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode2);
                    if (mAuthenticationCharacteristic != null) {
                        Log.w(TAG, "Auth Characteristic found: " + mAuthenticationCharacteristic.toString());
                        if (mAuthenticationCharacteristic.setValue(bondkey)) {
                            currentGattTask = GATT_SETUP;
                            step = 1;
                            mBluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                        } else {
                            setRetryTimer();
                        }
                    } else {
                        Log.w(TAG, "Authentication Characteristic IS NULL");
                        setRetryTimer();
                    }
                } else {
                    Log.w(TAG, "SHARE2 SERVICE IS NULL");
                    setRetryTimer();
                }
            }
        } else {
            setRetryTimer();
        }
    }

    public void assignCharacteristics() {
        if(!share2) {
            Log.e(TAG, "Setting #1 characteristics");
            mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
            mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse);
            mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command);
            mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response);
        } else {
            Log.e(TAG, "Setting #2 characteristics");
            mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver2);
            mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse2);
            mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command2);
            mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response2);
//            mHeartbeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat2);
//            mBluetoothGatt.readCharacteristic(mAuthenticationCharacteristic);
            Log.w(TAG, "AUTH IS: " + mAuthenticationCharacteristic.getStringValue(0));
            BluetoothGattService service = mShareService;
                Log.e(TAG, "=================================================================================");
                Log.e(TAG, "AFTERAUTHUUIDS Service: " + service.getUuid());
                for(BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    Log.e(TAG, "Characteristic: " + characteristic.getUuid());
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_INDICATE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE));
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_NOTIFY: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY));
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_READ: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ));
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_SIGNED_WRITE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE));
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_WRITE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE));
                    Log.e(TAG, "CHARACTERISTIC PROPERTY_WRITE_NO_RESPONSE: " + (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
                    Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE));
                    Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_ENCRYPTED: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED));
                    Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_ENCRYPTED_MITM: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM));
                    Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_SIGNED: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED));
                    Log.e(TAG, "CHARACTERISTIC PERMISSION_WRITE_SIGNED_MITM: " + (characteristic.getPermissions() & BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM));


            }
        }
    }

    public void setListeners(int listener_number) {
        Log.w(TAG, "Setting Listener: #" + listener_number);

        if (listener_number == 1) {
            step = 2;
            setCharacteristicIndication(mReceiveDataCharacteristic);
        } else if(listener_number == 2) {
            step = 3;
            setCharacteristicIndication(mResponseCharacteristic);
        }
    }


    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        setRetryTimer();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
        Log.w(TAG, "bt Disconnected");
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        setCharacteristicNotification(characteristic, true);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting notification");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic) {
        setCharacteristicIndication(characteristic, true);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.w(TAG, "Characteristic setting indication");
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
        Log.w(TAG, "Descriptor found: " + descriptor.getUuid());
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    public void writeCommand(List<byte[]> packets, int aRecordType, Action1<byte[]> dataResponseListener) {
        mDataResponseListener = dataResponseListener;
        successfulWrites = 0;
        writePackets = packets;
        recordType = aRecordType;
        step = 0;
        currentGattTask = GATT_WRITING_COMMANDS;
        gattWritingStep();
    }

    private void nextGattStep() {
        Log.d(TAG, "Next Gatt Step");
        step++;
        switch (currentGattTask) {
            case GATT_NOTHING:
                Log.d(TAG, "Next NOTHING: " + step);
                break;
            case GATT_SETUP:
                Log.d(TAG, "Next GATT SETUP: " + step);
                gattSetupStep();
                break;
            case GATT_WRITING_COMMANDS:
                Log.d(TAG, "Next GATT WRITING: " + step);
                gattWritingStep();
                break;
        }
    }

    public void clearGattTask() {
        currentGattTask = GATT_NOTHING;
        step = 0;
    }

    private void gattSetupStep() {
        step = 1;
        assignCharacteristics();
        setListeners(1);
    }

    private void gattWritingStep() {
        Log.d(TAG, "Writing command to the Gatt, step: " + step);
        int index = step;
        if (index <= (writePackets.size() - 1)) {
            Log.d(TAG, "Writing: " + writePackets.get(index) + " index: " + index);
            mSendDataCharacteristic.setValue(writePackets.get(index));
            if(mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic)) {
                Log.d(TAG, "Wrote Successfully");
            }
        } else {
            Log.d(TAG, "Done Writing commands");
            clearGattTask();
        }
    }

    private final BroadcastReceiver mPairReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            final BluetoothDevice bondDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (!bondDevice.getAddress().equals(mBluetoothGatt.getDevice().getAddress())) {
                Log.d(TAG, "Bond state wrong device");
                return; // That wasnt a device we care about!!
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "CALLBACK RECIEVED Bonded");
                    currentGattTask = GATT_SETUP;
                    Log.d(TAG, "Discovering Services");
                    if (!mBluetoothGatt.discoverServices()) {
                        Log.d(TAG, "Discover Services Failed");
                        setRetryTimer();
                    }
                } else if (state == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "CALLBACK RECIEVED: Not Bonded");
                } else if (state == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "CALLBACK RECIEVED: Trying to bond");
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.w(TAG, "Gatt state change status: " + status + " new state: " + newState);
            writeStatusConnectionFailures(status);
            writeStatusFailures(status);
            if (status == 133) {
                Log.e(TAG, "Got the status 133 bug, GROSS!!");
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                device = mBluetoothGatt.getDevice();
                mConnectionState = STATE_CONNECTED;
                ActiveBluetoothDevice.connected();
                Log.w(TAG, "Connected to GATT server.");

                if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.w(TAG, "Device already bonded, discovering services");
                    currentGattTask = GATT_SETUP;
                    if (!mBluetoothGatt.discoverServices()) {
                        Log.w(TAG, "discovering failed");
                        if(shouldDisconnect) {
                            stopSelf();
                        } else {
                            setRetryTimer();
                        }
                    }
                } else {
                    Log.w(TAG, "Device is not bonded, going to bond!");
                    bondDevice();
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                ActiveBluetoothDevice.disconnected();
                if(shouldDisconnect) {
                    stopSelf();
                } else {
                    setRetryTimer();
                }
                Log.w(TAG, "Disconnected from GATT server.");
            } else {
                Log.w(TAG, "Gatt callback... strange state.");
            }
            writeStatusFailures(status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "services discovered " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Services Discovered!");
                authenticateConnection();
            } else {
                Log.w(TAG, "No Services Discovered!");
                writeStatusFailures(status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "characteristic read " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Characteristic Read");
                nextGattStep();
            } else {
                Log.e(TAG, "Characteristic failed to read");
                writeStatusFailures(status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charUuid = characteristic.getUuid();
            Log.w(TAG, "Characteristic Update Received: " + charUuid);
            if (charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                Log.w(TAG, "mReceiveDataCharacteristic Update");
                byte[] value = characteristic.getValue();
                if (value != null) {
                    Observable.just(characteristic.getValue()).subscribe(mDataResponseListener);
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "descriptor wrote " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (step == 2 && currentGattTask == GATT_SETUP) {
                    setListeners(2);
                } else if (step == 3 && currentGattTask == GATT_SETUP) {
                    Log.w(TAG, "Done setting Listeners");
                    attemptRead();
                }
            } else if(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION == status ||
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION == status) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
                writeStatusFailures(status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "characteristic wrote " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Wrote a characteristic successfully");
                nextGattStep();
            } else if(BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION == status ||
                    BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION == status) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug?");
                }
            } else {
                Log.e(TAG, "Unknown error writing Characteristic");
                writeStatusFailures(status);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gat, int status) {
            Log.e(TAG, "Reliable write complete, status: " + status);
        }


    };

    public void bondDevice() {
        Log.w(TAG, "Setting the Pin then bonding");
        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, bondintent);
        device.setPin("000000".getBytes());
        device.setPairingConfirmation(true);
        device.createBond();
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e(TAG, "An exception occured while refreshing device");
        }
        return false;
    }

    private void writeStatusFailures(int status) {
        switch (status) {
            case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                Log.e(TAG, "error GATT_WRITE_NOT_PERMITTED");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION:
                Log.e(TAG, "error GATT_INSUFFICIENT_AUTHENTICATION");
                break;
            case BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED:
                Log.e(TAG, "error GATT_REQUEST_NOT_SUPPORTED");
                break;
            case BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION:
                Log.e(TAG, "error GATT_INSUFFICIENT_ENCRYPTION");
                break;
            case BluetoothGatt.GATT_INVALID_OFFSET:
                Log.e(TAG, "error GATT_INVALID_OFFSET");
                break;
            case BluetoothGatt.GATT_FAILURE:
                Log.e(TAG, "error GATT_FAILURE");
                break;
            case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                Log.e(TAG, "error GATT_INVALID_ATTRIBUTE_LENGTH");
                break;
            case BluetoothGatt.GATT_READ_NOT_PERMITTED:
                Log.e(TAG, "error GATT_READ_NOT_PERMITTED");
                break;
            case BluetoothGatt.GATT_SUCCESS:
                Log.d(TAG, "success GATT_SUCCESS");
                break;
            default:
                Log.e(TAG, "error no idea!");
                break;
        }
    }private void writeStatusConnectionFailures(int status) {
        Log.e(TAG, "ERRR: GATT_WRITE_NOT_PERMITTED " + (status & BluetoothGatt.GATT_WRITE_NOT_PERMITTED));
        Log.e(TAG, "ERRR: GATT_INSUFFICIENT_AUTHENTICATION " + (status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION));
        Log.e(TAG, "ERRR: GATT_REQUEST_NOT_SUPPORTED " + (status & BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED));
        Log.e(TAG, "ERRR: GATT_INSUFFICIENT_ENCRYPTION " + (status & BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION));
        Log.e(TAG, "ERRR: GATT_INVALID_OFFSET " + (status & BluetoothGatt.GATT_INVALID_OFFSET));
        Log.e(TAG, "ERRR: GATT_FAILURE " + (status & BluetoothGatt.GATT_FAILURE));
        Log.e(TAG, "ERRR: GATT_INVALID_ATTRIBUTE_LENGTH " + (status & BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH));
        Log.e(TAG, "ERRR: GATT_READ_NOT_PERMITTED" + (status & BluetoothGatt.GATT_READ_NOT_PERMITTED));

    }
}
