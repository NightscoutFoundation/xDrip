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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;
import com.eveningoutpost.dexdrip.Models.TransmitterData;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@TargetApi(android.os.Build.VERSION_CODES.JELLY_BEAN_MR2)
public class DexCollectionService extends Service {
    private final static String TAG = DexCollectionService.class.getSimpleName();
    private String mDeviceName;
    private String mDeviceAddress;
    private boolean is_connected = false;
    SharedPreferences prefs;

    public DexCollectionService dexCollectionService;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = STATE_DISCONNECTED;
    private BluetoothDevice device;
    int mStartMode;

    private Context mContext = null;

    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static UUID DexDripDataService = UUID.fromString(HM10Attributes.HM_10_SERVICE);
    public final static UUID DexDripDataCharacteristic = UUID.fromString(HM10Attributes.HM_RX_TX);

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        mContext = getApplicationContext();
        dexCollectionService = this;
        listenForChangeInSettings();
        this.startService(new Intent(this, SyncService.class));
        Log.w(TAG, "STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        attemptConnection();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        setRetryTimer();
        close();
        foregroundServiceStarter.stop();
        Log.w(TAG, "SERVICE STOPPED");
    }

    //TODO: Move this somewhere more reusable
    public void listenForChangeInSettings() {
        SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if(key.compareTo("run_service_in_foreground") == 0) {
                    if (prefs.getBoolean("run_service_in_foreground", false)) {
                        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                        foregroundServiceStarter.start();
                        Log.w(TAG, "Moving to foreground");
                        setRetryTimer();
                    } else {
                        dexCollectionService.stopForeground(true);
                        Log.w(TAG, "Removing from foreground");
                        setRetryTimer();
                    }
                }
                if(key.compareTo("dex_collection_method") == 0) {
                    CollectionServiceStarter collectionServiceStarter = new CollectionServiceStarter();
                    collectionServiceStarter.start(getApplicationContext());
                }
            }
        };
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.registerOnSharedPreferenceChangeListener(listener);
    }

    public void attemptConnection() {
        if (device != null) {
            mConnectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        }
        Log.w(TAG, "Connection state: " + mConnectionState);
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                    .orderBy("_ID desc")
                    .executeSingle();
            if (btDevice != null) {
                mDeviceName = btDevice.name;
                mDeviceAddress = btDevice.address;

                if (mBluetoothManager == null) {
                    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    if (mBluetoothManager == null) {
                        Log.w(TAG, "Unable to initialize BluetoothManager.");
                    }
                }
                if (mBluetoothManager != null) {
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                    if (mBluetoothAdapter == null) {
                        Log.w(TAG, "Unable to obtain a BluetoothAdapter.");
                        setRetryTimer();
                    }
                    is_connected = connect(mDeviceAddress);
                    if (is_connected) {
                        Log.w(TAG, "connected to device");
                    } else {
                        Log.w(TAG, "Unable to connect to device");
                        setRetryTimer();
                    }

                } else {
                    Log.w(TAG, "Still no bluetooth Manager");
                    setRetryTimer();
                }
            } else {
                Log.w(TAG, "No bluetooth device to try to connect to");
                setRetryTimer();
            }
        }
    }

    public void setRetryTimer() {
        Calendar calendar = Calendar.getInstance();
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + (1000 * 60 * 2), PendingIntent.getService(this, 0, new Intent(this, DexCollectionService.class), 0));
        Log.w(TAG, "Retry set for" +  (((calendar.getTimeInMillis() + (1000 * 60 * 2)) - (int) (new Date().getTime())) / (60000)) + "mins from now!");
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.w(TAG, "Connected to GATT server.");
                Log.w(TAG, "Attempting to start service discovery: " +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.w(TAG, "Disconnected from GATT server.");
                setRetryTimer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (BluetoothGattService gattService : mBluetoothGatt.getServices()) {
                    Log.w(TAG, "Service Found");
                    for (BluetoothGattCharacteristic gattCharacteristic : gattService.getCharacteristics()) {
                        Log.w(TAG, "Characteristic Found");
                        setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
                Log.w(TAG, "onServicesDiscovered received success: " + status);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
};

    private void broadcastUpdate(final String action) {
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        final byte[] data = characteristic.getValue();

        if (data != null && data.length > 0) {
            setSerialDataToTransmitterRawData(data, data.length);
        }
    }

    public class LocalBinder extends Binder {
        DexCollectionService getService() {
            return DexCollectionService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.w(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.w(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    public boolean connect(final String address) {
        Log.w(TAG, "CONNECTING TO DEVICE");
        Log.w(TAG, address);
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.w(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);
        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        disconnect();
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.w(TAG, "UUID FOUND: " + characteristic.getUuid());
        if (DexDripDataCharacteristic.equals(characteristic.getUuid())) {
            Log.w(TAG, "UUID MATCH FOUND!!! " + characteristic.getUuid());
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    public void setSerialDataToTransmitterRawData(byte[] buffer, int len) {

        Log.w(TAG, "received some data!");
        Long timestamp = new Date().getTime();
        TransmitterData transmitterData = TransmitterData.create(buffer, len, timestamp);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();

                BgReading bgReading = BgReading.create(transmitterData.raw_data, this, timestamp);
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
    }
}
