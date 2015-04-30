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

@TargetApi(Build.VERSION_CODES.KITKAT)
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
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        mContext = getApplicationContext();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
            stopSelf();
            return START_NOT_STICKY;
        }
        if (CollectionServiceStarter.isBTWixel(getApplicationContext())) {
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
        super.onDestroy();
        close();
        foregroundServiceStarter.stop();
        setRetryTimer();
        Log.w(TAG, "SERVICE STOPPED");
    }
    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            if(key.compareTo("run_service_in_foreground") == 0) {
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

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public void setRetryTimer() {
        if (CollectionServiceStarter.isBTWixel(getApplicationContext())) {
            long retry_in = (1000 * 60 * 2);
            Log.d(TAG, "Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.setExact(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexCollectionService.class), 0));
        }
    }

    public void setFailoverTimer() { //Sometimes it gets stuck in limbo on 4.4, this should make it try again
        if (CollectionServiceStarter.isBTWixel(getApplicationContext())) {
            long retry_in = (1000 * 60 * 5);
            Log.d(TAG, "Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            alarm.set(alarm.RTC_WAKEUP, calendar.getTimeInMillis() + retry_in, PendingIntent.getService(this, 0, new Intent(this, DexCollectionService.class), 0));
        } else {
            stopSelf();
        }
    }



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

                Log.w(TAG, "Connection state: " + mConnectionState);
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
                    Log.w(TAG, "Looks like we are already connected, going to read!");
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
                Log.w(TAG, "Connected to GATT server.");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                ActiveBluetoothDevice.disconnected();
                Log.w(TAG, "Disconnected from GATT server.");
                setRetryTimer();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService gattService = mBluetoothGatt.getService(xDripDataService);
                if (gattService != null) {
                    BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(xDripDataCharacteristic);
                    if (gattCharacteristic != null ) {
                        final int charaProp = gattCharacteristic.getProperties();

                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);
                        } else {
                            Log.e(TAG, "characteristic " + gattCharacteristic.getUuid() + " doesn't have notify properties");
                        }
                    } else {
                        Log.e(TAG, "characteristic " + xDripDataCharacteristic + " not found");
                    }
                } else {
                    Log.e(TAG, "service " + xDripDataCharacteristic + " not found");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) { setSerialDataToTransmitterRawData(data, data.length); }
        }
    };

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
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            setRetryTimer();
            return false;
        }
        Log.w(TAG, "Trying to create a new connection.");
        mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if ( mBluetoothGatt == null) { return; }
        mBluetoothGatt.disconnect();
        Log.d(TAG, "Gatt Disconnect");
    }
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        setRetryTimer();
        mBluetoothGatt = null;
        mConnectionState = STATE_DISCONNECTED;
    }

    public void setSerialDataToTransmitterRawData(byte[] buffer, int len) {
        Log.w(TAG, "received some data!");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "ReceivedReading");
        wakeLock.acquire();


        Long timestamp = new Date().getTime();
        TransmitterData transmitterData = TransmitterData.create(buffer, len, timestamp);
        if (transmitterData != null) {
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                sensor.latest_battery_level = transmitterData.sensor_battery_level;
                sensor.save();

                BgReading.create(transmitterData.raw_data, this, timestamp);
            } else {
                Log.w(TAG, "No Active Sensor, Data only stored in Transmitter Data");
            }
        }
        wakeLock.release();
    }
}
