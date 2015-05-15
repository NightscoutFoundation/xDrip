/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.eveningoutpost.dexdrip.Services;

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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;
import com.eveningoutpost.dexdrip.Models.TransmitterData;

import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import static com.activeandroid.ActiveAndroid.beginTransaction;
import static com.activeandroid.ActiveAndroid.endTransaction;
import static com.activeandroid.ActiveAndroid.setTransactionSuccessful;
import com.activeandroid.query.Select;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DexCollectionService extends Service {
    private final static String TAG = DexCollectionService.class.getSimpleName();
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean is_connected = false;
    SharedPreferences prefs;
    private static byte bridgeBattery = 0;

    public DexCollectionService dexCollectionService;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGattService mService;
    //queues for GattDescriptor and GattCharacteristic to ensure we get all messages and can clear the the message once it is processed.
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();
    private Queue<BluetoothGattCharacteristic> characteristicReadQueue = new LinkedList<BluetoothGattCharacteristic>();
    //int mStartMode;
    long lastPacketTime;
    private static byte[] lastdata = null;

    private Context mContext = null;


    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static UUID xDripDataService = UUID.fromString(HM10Attributes.HM_10_SERVICE);
    public final static UUID xDripDataCharacteristic = UUID.fromString(HM10Attributes.HM_RX_TX);

    @Override
    public IBinder onBind(Intent intent) {
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        mContext = getApplicationContext();

        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        Log.w(TAG, "onCreate: STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Log.w(TAG,"onStartCommand: Unsupported Android Version");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (CollectionServiceStarter.isBTWixel(getApplicationContext()) || CollectionServiceStarter.isDexbridgeWixel(getApplicationContext())) {
            setFailoverTimer();
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
        attemptConnection();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "onDestroy entered");
        super.onDestroy();
        //close();
        foregroundServiceStarter.stop();
        setRetryTimer();
        Log.w(TAG, "SERVICE STOPPED");
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if (key.compareTo("run_service_in_foreground") == 0) {
                Log.e("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                    foregroundServiceStarter.start();
                    Log.w(TAG, "Moving to foreground");
                } else {
                    dexCollectionService.stopForeground(true);
                    Log.w(TAG, "Removing from foreground");
                }
            }
        }
    };

    public static byte getBridgeBattery() {
        return bridgeBattery;
    }


    public boolean isUnitsMmol() {
        return (PreferenceManager.getDefaultSharedPreferences(this).getString("units", "mmol").compareTo("mmol") != 0);
    }

    public static String getBridgeBatteryAsString() {
        return String.format("%d", bridgeBattery);
    }

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public void setRetryTimer() {
        if (CollectionServiceStarter.isBTWixel(getApplicationContext()) || CollectionServiceStarter.isDexbridgeWixel(getApplicationContext())) {
            long retry_in = (1000 * 62);
            Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / 1000) + " seconds");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexCollectionService.class), 0));
        }
    }

    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        if (CollectionServiceStarter.isBTWixel(getApplicationContext()) || CollectionServiceStarter.isDexbridgeWixel(getApplicationContext())) {
            long retry_in = (1000 * 60 * 5);
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexCollectionService.class), 0));
        } else {
            stopSelf();
        }
    }

    /*    public void listenForChangeInSettings() {
            SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    if (key.compareTo("run_service_in_foreground") == 0) {
                        if (prefs.getBoolean("run_service_in_foreground", false)) {
                            foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                            foregroundServiceStarter.start();
                            Log.w(TAG, "listenForChangeInSettings: Moving to foreground");
                            setRetryTimer();
                        } else {
                            dexCollectionService.stopForeground(true);
                            Log.w(TAG, "listenForChangeInSettings: Removing from foreground");
                            setRetryTimer();
                        }
                    }
                    if (key.compareTo("dex_collection_method") == 0) {
                        CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter(getApplicationContext());
                        collectionServiceStarter.start(getApplicationContext());
                    }
                }
            };
            prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            prefs.registerOnSharedPreferenceChangeListener(listener);
        }
    */
    public void attemptConnection() {
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
                if (device != null) {
                    mConnectionState = STATE_DISCONNECTED;
                    for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                        if (bluetoothDevice.getAddress().compareTo(device.getAddress()) == 0) {
                            mConnectionState = STATE_CONNECTED;
                        }
                    }
                }

                Log.w(TAG, "attemptConnection: Connection state: " + mConnectionState);
                if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
                    ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
                    if (btDevice != null) {
                        mDeviceName = btDevice.name;
                        mDeviceAddress = btDevice.address;
                        if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getRemoteDevice(mDeviceAddress) != null) {
                            connect(mDeviceAddress);
                            return;
                        }
                    }
                } else if (mConnectionState == STATE_CONNECTED) { //WOOO, we are good to go, nothing to do here!
                    Log.w(TAG, "attemptConnection: Looks like we are already connected, going to read!");
                    return;
                }
            }
        }
        setRetryTimer();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                ActiveBluetoothDevice.connected();
                Log.w(TAG, "onConnectionStateChange: Connected to GATT server.");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                ActiveBluetoothDevice.disconnected();
                Log.w(TAG, "onConnectionStateChange: Disconnected from GATT server.");
                setRetryTimer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService gattService = mBluetoothGatt.getService(xDripDataService);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(xDripDataCharacteristic);
                    if (gattCharacteristic != null) {
                        final int charaProp = gattCharacteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                                mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                            } else {
                                Log.e(TAG, "onServicesDiscovered: characteristic " + gattCharacteristic.getUuid() + " doesn't have notify properties");
                            }
                        } else {
                            Log.e(TAG, "onServicesDiscovered: characteristic " + xDripDataCharacteristic + " not found");
                        }
                    } else {
                        Log.e(TAG, "onServicesDiscovered: service " + xDripDataService + " not found");
                        //Log.v(TAG, "onServicesDiscovered: Already have that service");
                    }
                    Log.w(TAG, "onServicesDiscovered received success: " + status);
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.w(TAG, "onCharacteristicRead entered.");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                Log.w(TAG, "onCharacteristicRead error: " + status);
            }
            if (characteristicReadQueue.size() > 0)
                mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
            characteristicReadQueue.remove();
            Log.w(TAG, "onCharacteristicRead exited.");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            //final byte[] data;
            Log.w(TAG, "onCharacteristicChanged entered");
            mCharacteristic = characteristic;
            //data = characteristic.getValue();
            //if(lastdata == null) lastdata = characteristic.getValue();
            //if(Arrays.equals(lastdata, data) && lastdata != null) {
            //    Log.w(TAG, "onCharacteristicChanged: duplicate packet.  Ignoring");
            //    return;
            //}
            Log.w(TAG, "onCharacteristicChanged: new packet.");
            //lastdata = data.clone();;
            characteristicReadQueue.add(characteristic);
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristicReadQueue.poll());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onDescriptorWrite: Wrote GATT Descriptor successfully.");
            } else {
                Log.d(TAG, "onDescriptorWrite: Error writing GATT Descriptor: " + status);
            }
            descriptorWriteQueue.remove();  //pop the item that we just finishing writing
            if (descriptorWriteQueue.size() > 0) // if there is more to write, do it!
                mBluetoothGatt.writeDescriptor(descriptorWriteQueue.element());
             /* else if(characteristicReadQueue.size() > 0)
              mBluetoothGatt.readCharacteristic(characteristicReadQueue.element());
             */
        }
    };


    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        Log.w(TAG, "broadcastUpdate got action: " + action + ", characteristic: " + characteristic.toString());
        final byte[] data = characteristic.getValue();
        Calendar c = Calendar.getInstance();
        long secondsNow = c.getTimeInMillis();
        if (secondsNow - lastPacketTime > 60000) {
            lastdata = null;
        }
        if (lastdata != null) {
            if (data != null && data.length > 0 && !Arrays.equals(lastdata, data)) {
                Log.v(TAG, "broadcastUpdate: new data.");
                setSerialDataToTransmitterRawData(data, data.length);
                lastdata = data;
            } else if (Arrays.equals(lastdata, data)) {
                Log.v(TAG, "broadcastUpdate: duplicate data, ignoring");
                //return;
            }
        } else if (data != null && data.length > 0) {
            setSerialDataToTransmitterRawData(data, data.length);
            lastdata = data;
        }
    }

    private boolean sendBtMessage(final ByteBuffer message) {
        //check mBluetoothGatt is available
        Log.w(TAG, "sendBtMessage: entered");
        if (mBluetoothGatt == null) {
            Log.e(TAG, "sendBtMessage: lost connection");
            return false;
        }

        byte[] value = message.array();
        Log.w(TAG, "sendBtMessage: sending message");
        mCharacteristic.setValue(value);

        return mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    private Integer convertSrc(final String Src) {
        Integer res = 0;
        res |= getSrcValue(Src.charAt(0)) << 20;
        res |= getSrcValue(Src.charAt(1)) << 15;
        res |= getSrcValue(Src.charAt(2)) << 10;
        res |= getSrcValue(Src.charAt(3)) << 5;
        res |= getSrcValue(Src.charAt(4));
        return res;
    }

    private int getSrcValue(char ch) {
        int i;
        char[] cTable = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'W', 'X', 'Y'};
        for (i = 0; i < cTable.length; i++) {
            if (cTable[i] == ch) break;
        }
        return i;
    }

    public boolean connect(final String address) {
        Log.w(TAG, "connect: going to connect to device at address" + address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "connect: BluetoothAdapter not initialized or unspecified address.");
            setRetryTimer();
            return false;
        }
        if (mBluetoothGatt != null) {
            Log.w(TAG, "connect: mBluetoothGatt isnt null, Closing.");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
/*        if (device == null) {
            Log.w(TAG, "connect: Device not found.  Unable to connect.");
            setRetryTimer();
            return false;
        } */
        Log.w(TAG, "connect: Trying to create a new connection.");
        mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

/*    public void disconnect() {
        if (mBluetoothGatt == null) {
            return;
        }
        Log.d(TAG, "disconnect: Gatt Disconnect");
        mBluetoothGatt.disconnect();
    }*/

    public void close() {
        Log.w(TAG, "close: Closing Connection");
        //disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        setRetryTimer();
        mBluetoothGatt = null;
        mCharacteristic = null;
        mConnectionState = STATE_DISCONNECTED;
    }

    public void setSerialDataToTransmitterRawData(byte[] buffer, int len) {
        try {
            Log.w(TAG, "setSerialDataToTransmitterRawData: received some data: " + new String(buffer, 0, len, Charset.forName("ISO-8859-1")));
        } catch (Exception ex) {
            Log.w(TAG, "setSerialDataToTransmitterRawData: received some data!");
        }
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ReceivedReading");
        wakeLock.acquire();
        try {
            beginTransaction();
            try {
                long timestamp = new Date().getTime();
                if (CollectionServiceStarter.isDexbridgeWixel(getApplicationContext())) {
                    Log.w(TAG, "setSerialDataToTransmitterRawData: Dealing with Dexbridge packet!");
                    int DexSrc;
                    int TransmitterID;
                    String TxId;
                    Calendar c = Calendar.getInstance();
                    long secondsNow = c.getTimeInMillis();
                    ByteBuffer tmpBuffer = ByteBuffer.allocate(len);
                    tmpBuffer.order(ByteOrder.LITTLE_ENDIAN);
                    tmpBuffer.put(buffer, 0, len);
                    ByteBuffer txidMessage = ByteBuffer.allocate(6);
                    txidMessage.order(ByteOrder.LITTLE_ENDIAN);
                    if (buffer[0] == 0x07 && buffer[1] == -15) {
                        //We have a Beacon packet.  Get the TXID value and compare with dex_txid
                        Log.w(TAG, "setSerialDataToTransmitterRawData: Received Beacon packet.");
                        //DexSrc starts at Byte 2 of a Beacon packet.
                        DexSrc = tmpBuffer.getInt(2);
                        TxId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("dex_txid", "00000");
                        TransmitterID = convertSrc(TxId);
                        if (TxId.compareTo("00000") != 0 && Integer.compare(DexSrc, TransmitterID) != 0) {
                            Log.w(TAG, "setSerialDataToTransmitterRawData: TXID wrong.  Expected " + TransmitterID + " but got " + DexSrc);
                            txidMessage.put(0, (byte) 0x06);
                            txidMessage.put(1, (byte) 0x01);
                            txidMessage.putInt(2, TransmitterID);
                            sendBtMessage(txidMessage);
                        }
                        return;
                    }
                    if (buffer[0] == 0x11 && buffer[1] == 0x00) {
                        //we have a data packet.  Check to see if the TXID is what we are expecting.
                        Log.w(TAG, "setSerialDataToTransmitterRawData: Received Data packet");
                        //make sure we are not processing a packet we already have
                        if (secondsNow - lastPacketTime < 60000) {
                            Log.v(TAG, "setSerialDataToTransmitterRawData: Received Duplicate Packet.  Exiting.");
                            return;
                        } else {
                            lastPacketTime = secondsNow;
                        }
                        if (len >= 0x11) {
                            //DexSrc starts at Byte 12 of a data packet.
                            DexSrc = tmpBuffer.getInt(12);
                            TxId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("dex_txid", "00000");
                            TransmitterID = convertSrc(TxId);
                            if (Integer.compare(DexSrc, TransmitterID) != 0) {
                                Log.w(TAG, "TXID wrong.  Expected " + TransmitterID + " but got " + DexSrc);
                                txidMessage.put(0, (byte) 0x06);
                                txidMessage.put(1, (byte) 0x01);
                                txidMessage.putInt(2, TransmitterID);
                                sendBtMessage(txidMessage);
                            }
                            bridgeBattery = ByteBuffer.wrap(buffer).get(11);
                            //All is OK, so process it.
                            //first, tell the wixel it is OK to sleep.
                            Log.d(TAG, "setSerialDataToTransmitterRawData: Sending Data packet Ack, to put wixel to sleep");
                            ByteBuffer ackMessage = ByteBuffer.allocate(2);
                            ackMessage.put(0, (byte) 0x02);
                            ackMessage.put(1, (byte) 0xF0);
                            sendBtMessage(ackMessage);
                            timestamp = new Date().getTime();
                            Log.v(TAG, "setSerialDataToTransmitterRawData: Creating TransmitterData at " + timestamp);
                            TransmitterData transmitterData = TransmitterData.create(buffer, len, timestamp);
                            if (transmitterData != null) {
                                Sensor sensor = Sensor.currentSensor();
                                if (sensor != null) {
                                    sensor.latest_battery_level = transmitterData.sensor_battery_level;
                                    sensor.save();

                                    BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, timestamp);
                                    Intent intent = new Intent("com.eveningoutpost.dexdrip.DexCollectionService.SAVED_BG");
                                    sendBroadcast(intent);

                                } else {
                                    Log.w(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
                                }
                            }
                        }
                    } else {
                        TransmitterData transmitterData = TransmitterData.create(buffer, len, timestamp);
                        //TransmitterData transmitterData = TransmitterData.create(buffer, len);
                        if (transmitterData != null) {
                            Sensor sensor = Sensor.currentSensor();
                            if (sensor != null) {
                                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                                sensor.save();

                                BgReading bgReading = BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, timestamp);
                                Intent intent = new Intent("com.eveningoutpost.dexdrip.DexCollectionService.SAVED_BG");
                                sendBroadcast(intent);
                            } else {
                                Log.w(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
                            }
                        }
                    }
                }
                setTransactionSuccessful();
            }finally{
                endTransaction();
            }
        } finally {
            wakeLock.release();
        }
    }
}
