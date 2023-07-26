package com.eveningoutpost.dexdrip.services;

import android.annotation.TargetApi;
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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.ReadDataShare;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.CalRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.EGVRecord;
import com.eveningoutpost.dexdrip.importedlibraries.dexcom.records.SensorRecord;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.DexShareAttributes;
//KS import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.HM10Attributes;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
//KS import com.eveningoutpost.dexdrip.utils.BgToSpeech;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import rx.Observable;
import rx.functions.Action1;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DexShareCollectionService extends Service {
    private final static String TAG = DexShareCollectionService.class.getSimpleName();
    //KS private ForegroundServiceStarter foregroundServiceStarter;
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
    private BluetoothGattCharacteristic mHeartBeatCharacteristic;

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

    public boolean state_authSucess = false;
    public boolean state_authInProgress = false;
    public boolean state_notifSetupSucess = false;

    public boolean shouldDisconnect = false;
    public boolean share2 = false;
    public Service service;
    //KS private BgToSpeech bgToSpeech;

    private long lastHeartbeat = 0;
    private int heartbeatCount = 0;

    private static PendingIntent pendingIntent;
    private static int statusErrors = 0;
    private double instance = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ENTER");
        readData = new ReadDataShare(this);
        service = this;
        //KS foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
        //KS foregroundServiceStarter.start();
        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, bondintent);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        //KS bgToSpeech = BgToSpeech.setupTTS(getApplicationContext()); //keep reference to not being garbage collected
        instance = JoH.ts();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DexShareCollectionStart");
        wakeLock.acquire(40000);
        Log.d(TAG, "onStartCommand");
        try {

            if (shouldServiceRun(getApplicationContext())) {
                setFailoverTimer();
            } else {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (Sensor.currentSensor() == null) {
                setRetryTimer();
                return START_NOT_STICKY;
            }
            Log.i(TAG, "STARTING SERVICE");
            attemptConnection();
        } finally {
            if(wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        }
        return START_STICKY;
    }

    private static boolean shouldServiceRun(Context context) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return false;
        final boolean result = CollectionServiceStarter.isBTShare(context) && PersistentStore.getBoolean(CollectionServiceStarter.pref_run_wear_collector);
        Log.d(TAG, "shouldServiceRun() returning: " + result);
        return result;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        close();
        if (shouldServiceRun(getApplicationContext())) {//Android killed service
            setRetryTimer();
        }
        else {//onDestroy triggered by CollectionServiceStart.stopBtService
            if (pendingIntent != null) {
                Log.d(TAG, "onDestroy stop Alarm serviceIntent");
                AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
                alarm.cancel(pendingIntent);
            }
        }
        //KS foregroundServiceStarter.stop();
        unregisterReceiver(mPairReceiver);
        //KS BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.compareTo("run_service_in_foreground") == 0) {
                Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    //KS foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), service);
                    //KS foregroundServiceStarter.start();
                    Log.i(TAG, "Moving to foreground");
                } else {
                    //KS service.stopForeground(true);
                    Log.i(TAG, "Removing from foreground");
                }
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
                retry_in = Math.min(Math.max((1000 * 30), (1000 * 60 * 5) - (new Date().getTime() - bgReading.timestamp) + (1000 * 5)), (1000 * 60 * 5));
            } else {
                retry_in = (1000 * 20);
            }
            Log.d(TAG, "Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (pendingIntent != null)
                alarm.cancel(pendingIntent);
            long wakeTime = calendar.getTimeInMillis() + retry_in;
            pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        }
    }

    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            long retry_in = (1000 * 60 * 5);
            Log.d(TAG, "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (pendingIntent != null)
                alarm.cancel(pendingIntent);
            long wakeTime = calendar.getTimeInMillis() + retry_in;
            pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, pendingIntent);
        } else {
            stopSelf();
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
            Log.i(TAG, "Connection state: " + mConnectionState);
            if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
                ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
                if (btDevice != null) {
                    mDeviceName = btDevice.name;
                    mDeviceAddress = btDevice.address;
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    try {
                        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getRemoteDevice(mDeviceAddress) != null) {
                            connect(mDeviceAddress);
                            return;
                        } else {
                            Log.w(TAG, "Bluetooth is disabled or BT device cant be found");
                            setRetryTimer();
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        if (JoH.ratelimit("dex-share-error-log", 180)) {
                            Log.wtf(TAG, "Error connecting: " + e);
                        }
                    }
                } else {
                    Log.w(TAG, "No bluetooth device to try and connect to");
                    setRetryTimer();
                    return;
                }
            } else if (mConnectionState == STATE_CONNECTED) {
                Log.i(TAG, "Looks like we are already connected, going to read!");
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

    public void requestHighPriority() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothGatt != null) {
            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
    }

    public void requestLowPriority() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mBluetoothGatt != null) {
            mBluetoothGatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER);
        }
    }

    public void attemptRead() {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock1 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ReadingShareData");
        wakeLock1.acquire(60000);
        requestHighPriority();
        Log.d(TAG, "Attempting to read data");
        final Action1<Long> systemTimeListener = new Action1<Long>() {
            @Override
            public void call(Long s) {
                if (s != null) {
                    Log.d(TAG, "Made the full round trip, got " + s + " as the system time");
                    final long additiveSystemTimeOffset = new Date().getTime() - s;

                    final Action1<Long> dislpayTimeListener = new Action1<Long>() {
                        @Override
                        public void call(Long s) {
                            if (s != null) {
                                Log.d(TAG, "Made the full round trip, got " + s + " as the display time offset");
                                final long addativeDisplayTimeOffset = additiveSystemTimeOffset - (s * 1000);

                                Log.d(TAG, "Making " + addativeDisplayTimeOffset + " the the total time offset");

                                final Action1<EGVRecord[]> evgRecordListener = new Action1<EGVRecord[]>() {
                                    @Override
                                    public void call(EGVRecord[] egvRecords) {
                                        if (egvRecords != null) {
                                            Log.d(TAG, "Made the full round trip, got " + egvRecords.length + " EVG Records");
                                            BgReading.create(egvRecords, additiveSystemTimeOffset, getApplicationContext());
                                            statusErrors=0;
                                            {
                                                Log.d(TAG, "Releasing wl in egv");
                                                requestLowPriority();
                                                if(wakeLock1 != null && wakeLock1.isHeld()) wakeLock1.release();
                                                Log.d(TAG, "released");
                                            }
                                            if (shouldDisconnect) {
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
                                            BgReading.create(sensorRecords, additiveSystemTimeOffset, getApplicationContext());
                                            statusErrors=0;
                                            readData.getRecentEGVs(evgRecordListener);
                                        }
                                    }
                                };

                                final Action1<CalRecord[]> calRecordListener = new Action1<CalRecord[]>() {
                                    @Override
                                    public void call(CalRecord[] calRecords) {
                                        if (calRecords != null) {
                                            Log.d(TAG, "Made the full round trip, got " + calRecords.length + " Cal Records");
                                            Calibration.create(calRecords, addativeDisplayTimeOffset, getApplicationContext());
                                            statusErrors=0;
                                            readData.getRecentSensorRecords(sensorRecordListener);
                                        }
                                    }
                                };
                                readData.getRecentCalRecords(calRecordListener);
                            } else
                            if(wakeLock1 != null && wakeLock1.isHeld()) wakeLock1.release();
                        }
                    };
                    readData.readDisplayTimeOffset(dislpayTimeListener);
                } else
                if(wakeLock1 != null && wakeLock1.isHeld()) wakeLock1.release();

            }
        };
        readData.readSystemTime(systemTimeListener);
    }

    public synchronized boolean connect(final String address) {
        PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DexShareCollectionStart");
        wakeLock.acquire(30000);
        Log.i(TAG, "going to connect to device at address" + address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            setRetryTimer();
            return false;
        }
        if (mBluetoothGatt != null) {
            Log.i(TAG, "BGatt isnt null, Closing.");
            try {
                mBluetoothGatt.close();
            } catch (NullPointerException e) {
                Log.d(TAG, "concurrency related null pointer exception in connect");
            }
            mBluetoothGatt = null;
        }
        for (BluetoothDevice bluetoothDevice : mBluetoothAdapter.getBondedDevices()) {
            if (bluetoothDevice.getAddress().compareTo(address) == 0) {
                Log.v(TAG, "Device found, already bonded, going to connect");
               if(mBluetoothAdapter.getRemoteDevice(bluetoothDevice.getAddress()) != null) {
                   device = bluetoothDevice;
                   mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
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
        Log.i(TAG, "Trying to create a new connection.");
        mBluetoothGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void authenticateConnection() {
        Log.i(TAG, "Trying to auth");
        String receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase() + "000000";
        if(receiverSn.compareTo("SM00000000000000") == 0) { // They havnt set their serial number, dont bond!
            setRetryTimer();
            return;
        }
        byte[] bondkey = (receiverSn).getBytes(StandardCharsets.US_ASCII);
        if (mBluetoothGatt != null) {
            if (mShareService != null) {
                if(!share2) {
                    mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode);
                } else {
                    mAuthenticationCharacteristic = mShareService.getCharacteristic(DexShareAttributes.AuthenticationCode2);
                }
                if (mAuthenticationCharacteristic != null) {
                    Log.v(TAG, "Auth Characteristic found: " + mAuthenticationCharacteristic.toString());
                    if (mAuthenticationCharacteristic.setValue(bondkey)) {
                        mBluetoothGatt.writeCharacteristic(mAuthenticationCharacteristic);
                    } else {
                        setRetryTimer();
                    }
                } else {
                    Log.w(TAG, "Authentication Characteristic IS NULL");
                    setRetryTimer();
                }
            } else {
                Log.w(TAG, "CRADLE SERVICE IS NULL");
            }
        } else {
            setRetryTimer();
        }
    }

    public void assignCharacteristics() {
        if(!share2) {
            Log.d(TAG, "Setting #1 characteristics");
            mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver);
            mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse);
            mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command);
            mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response);
            mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat);
        } else {
            Log.d(TAG, "Setting #1 characteristics");
            mSendDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageReceiver2);
            mReceiveDataCharacteristic = mShareService.getCharacteristic(DexShareAttributes.ShareMessageResponse2);
            mCommandCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Command2);
            mResponseCharacteristic = mShareService.getCharacteristic(DexShareAttributes.Response2);
            mHeartBeatCharacteristic = mShareService.getCharacteristic(DexShareAttributes.HeartBeat2);
        }
    }

    public void setListeners(int listener_number) {
        Log.i(TAG, "Setting Listener: #" + listener_number);
        if (listener_number == 1) {
            step = 2;
            setCharacteristicIndication(mReceiveDataCharacteristic);
        } else {
            step = 3;
            attemptRead();
        }
    }


    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        try {
            mBluetoothGatt.close();
        } catch (NullPointerException e) {
            Log.d(TAG, "concurrency related null pointer exception in close");
        }
        setRetryTimer();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
        Log.i(TAG, "bt Disconnected");
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic) {
        setCharacteristicNotification(characteristic, true);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.i(TAG, "Characteristic setting notification");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
            Log.i(TAG, "Descriptor found: " + descriptor.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic) {
        setCharacteristicIndication(characteristic, true);
    }

    public void setCharacteristicIndication(BluetoothGattCharacteristic characteristic, boolean enabled) {
        Log.i(TAG, "Characteristic setting indication");
        if (mBluetoothGatt != null) {
            mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
            Log.i(TAG, "Descriptor found: " + descriptor.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
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

    public void clearGattTask() {
        currentGattTask = GATT_NOTHING;
        step = 0;
    }

    private void gattSetupStep() {
        step = 1;
        if(share2) { assignCharacteristics(); }
        setListeners(1);
    }

    private void gattWritingStep() {
        Log.d(TAG, "Writing command to the Gatt, step: " + step);
        int index = step;
        if (index <= (writePackets.size() - 1)) {
            Log.d(TAG, "Writing: " + writePackets.get(index) + " index: " + index);
            if(mSendDataCharacteristic != null && writePackets != null) {
                mSendDataCharacteristic.setValue(writePackets.get(index));
                if (mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(mSendDataCharacteristic)) {
                    Log.d(TAG, "Wrote Successfully");
                }
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

            if (mBluetoothGatt != null && mBluetoothGatt.getDevice() != null && bondDevice != null) {
                if (!bondDevice.getAddress().equals(mBluetoothGatt.getDevice().getAddress())) {
                    Log.d(TAG, "Bond state wrong device");
                    return; // That wasnt a device we care about!!
                }
            }

            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                if (state == BluetoothDevice.BOND_BONDED) {
                    authenticateConnection();
                }
            }
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i(TAG, "Gatt state change status: " + status + " new state: " + newState);
            if (status == 133) {
                statusErrors++;
                Log.e(TAG, "Got the status 133 bug, bad news! count:"+statusErrors+" - Might require devices to forget each other: instance uptime: "+JoH.qs((JoH.ts()-instance)/1000,0));
                if (statusErrors>4)
                {
                    Log.wtf(TAG,"Forcing bluetooth reset to try to combat errors");
                    statusErrors=0;
                    JoH.niceRestartBluetooth(getApplicationContext());
                    setRetryTimer();
                    close();
                    stopSelf();
                    return;
                }
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                device = mBluetoothGatt.getDevice();
                mConnectionState = STATE_CONNECTED;
                ActiveBluetoothDevice.connected();
                Log.i(TAG, "Connected to GATT server.");

                Log.i(TAG, "discovering services");
                currentGattTask = GATT_SETUP;
                if (mBluetoothGatt == null || !mBluetoothGatt.discoverServices()) {
                    Log.w(TAG, "discovering failed");
                    if(shouldDisconnect) {
                        stopSelf();
                    } else {
                        setRetryTimer();
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                ActiveBluetoothDevice.disconnected();
                if(shouldDisconnect) {
                    stopSelf();
                } else {
                    setRetryTimer();
                }
                Log.d(TAG, "Disconnected from GATT server.");
            } else {
                Log.d(TAG, "Gatt callback... strange state.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "services discovered " + status);
           if (status == BluetoothGatt.GATT_SUCCESS && mBluetoothGatt != null) {
               mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService);
               if(mShareService == null) {
                   mShareService = mBluetoothGatt.getService(DexShareAttributes.CradleService2);
                   share2 = true;
               } else {
                   share2 = false;
               }
                assignCharacteristics();
                authenticateConnection();
                gattSetupStep();
            } else {
                Log.w(TAG, "No Services Discovered!");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v(TAG, "Characteristic Read " + characteristic.getUuid());
                if(mHeartBeatCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    Log.v(TAG, "Characteristic Read " + characteristic.getUuid() + " " + characteristic.getValue());
                    setCharacteristicNotification(mHeartBeatCharacteristic);
                }
                gatt.readCharacteristic(mHeartBeatCharacteristic);
            } else {
                Log.e(TAG, "Characteristic failed to read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            UUID charUuid = characteristic.getUuid();
            Log.d(TAG, "Characteristic Update Received: " + charUuid);
            if (charUuid.compareTo(mReceiveDataCharacteristic.getUuid()) == 0) {
                Log.d(TAG, "mCharReceiveData Update");
                byte[] value = characteristic.getValue();
                if (value != null) {
                    Observable.just(characteristic.getValue()).subscribe(mDataResponseListener);
                }
            } else if (charUuid.compareTo(mHeartBeatCharacteristic.getUuid()) == 0) {
                long heartbeat = System.currentTimeMillis();
                Log.d(TAG, "Heartbeat delta: " + (heartbeat - lastHeartbeat));
                if ((heartbeat-lastHeartbeat < 59000) || heartbeatCount > 5) {
                    Log.d(TAG, "Early heartbeat.  Fetching data.");
                    AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
                    alarm.cancel(pendingIntent);
                    heartbeatCount = 0;
                    attemptConnection();
                }
                heartbeatCount += 1;
                lastHeartbeat = heartbeat;
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                Log.d(TAG, "Characteristic onDescriptorWrite ch " + characteristic.getUuid());
                if(mHeartBeatCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    state_notifSetupSucess = true;
                    setCharacteristicIndication(mReceiveDataCharacteristic);
                }
                if(mReceiveDataCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    setCharacteristicIndication(mResponseCharacteristic);
                }
                if(mResponseCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    attemptRead();
                }
            } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) != 0 || (status & BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) != 0) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    state_authInProgress = true;
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug? Have the dexcom forget whatever device it was previously paired to: ondescriptorwrite code: "+status+ "bond: "+gatt.getDevice().getBondState());
                }
            } else {
                Log.e(TAG, "Unknown error writing descriptor");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "characteristic wrote " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Wrote a characteristic successfully " + characteristic.getUuid());
                if (mAuthenticationCharacteristic.getUuid().equals(characteristic.getUuid())) {
                    state_authSucess = true;
                    gatt.readCharacteristic(mHeartBeatCharacteristic);
                }
            } else if ((status & BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) != 0 || (status & BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION) != 0) {
                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    device = gatt.getDevice();
                    state_authInProgress = true;
                    bondDevice();
                } else {
                    Log.e(TAG, "The phone is trying to read from paired device without encryption. Android Bug? Have the dexcom forget whatever device it was previously paired to: oncharacteristicwrite code: "+status+ "bond: "+gatt.getDevice().getBondState());
                }
            } else {
                Log.e(TAG, "Unknown error writing Characteristic");
            }
        }
    };

    public void bondDevice() {
        final IntentFilter bondintent = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mPairReceiver, bondintent);
        if(!share2){ device.setPin("000000".getBytes()); }
        device.createBond();
    }
}
