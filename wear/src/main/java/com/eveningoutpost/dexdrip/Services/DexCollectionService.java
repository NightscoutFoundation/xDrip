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
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

//KS import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
//KS import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
//KS import com.eveningoutpost.dexdrip.utils.BgToSpeech;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.google.android.gms.wearable.DataMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DexCollectionService extends Service {
    private final static String TAG = DexCollectionService.class.getSimpleName();
    private SharedPreferences prefs;
    //KS private BgToSpeech bgToSpeech;
    private static PendingIntent serviceIntent;
    private static PendingIntent serviceFailoverIntent;
    public DexCollectionService dexCollectionService;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    //KS private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
    private static int mStaticState = BluetoothProfile.STATE_DISCONNECTING;
    private static int mStaticStateWatch = BluetoothProfile.STATE_DISCONNECTING;
    private BluetoothDevice device;
    private BluetoothGattCharacteristic mCharacteristic;
    // Experimental support for rfduino from Tomasz Stachowicz
    private BluetoothGattCharacteristic mCharacteristicSend;
    long lastPacketTime;
    private byte[] lastdata = null;
    private Context mContext;
    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;

    public static double last_time_seen = 0;
    public static String lastState = "Not running";
    private static TransmitterData last_transmitter_Data;
    private static int last_battery_level = -1;
    private static long retry_time = 0;
    private static long failover_time = 0;
    private static int watchdog_count = 0;

    private static boolean static_use_transmiter_pl_bluetooth = false;
    private static boolean static_use_rfduino_bluetooth = false;
    private static String static_last_hexdump;
    private static String static_last_sent_hexdump;

    //WATCH:
    public static String lastStateWatch = "Not running";
    private static TransmitterData last_transmitter_DataWatch;
    private static int last_battery_level_watch = -1;
    private static long retry_time_watch = 0;
    private static long failover_time_watch = 0;
    private static String static_last_hexdump_watch;
    private static String static_last_sent_hexdump_watch;

    // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
    private final boolean use_transmiter_pl_bluetooth = Home.getPreferencesBooleanDefaultFalse("use_transmiter_pl_bluetooth");
    private final boolean use_rfduino_bluetooth = Home.getPreferencesBooleanDefaultFalse("use_rfduino_bluetooth");
    private final UUID xDripDataService = use_transmiter_pl_bluetooth ? UUID.fromString(HM10Attributes.TRANSMITER_PL_SERVICE) : UUID.fromString(HM10Attributes.HM_10_SERVICE);
    private final UUID xDripDataCharacteristic = use_transmiter_pl_bluetooth ? UUID.fromString(HM10Attributes.TRANSMITER_PL_RX_TX) : UUID.fromString(HM10Attributes.HM_RX_TX);
    // Experimental support for rfduino from Tomasz Stachowicz
    private final UUID xDripDataCharacteristicSend = use_rfduino_bluetooth ? UUID.fromString(HM10Attributes.HM_TX) : UUID.fromString(HM10Attributes.HM_RX_TX);

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        //KS foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        //KS foregroundServiceStarter.start();
        mContext = getApplicationContext();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        //KS bgToSpeech = BgToSpeech.setupTTS(mContext); //keep reference to not being garbage collected
        if(CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()){
            Log.i(TAG,"onCreate: resetting bridge_battery preference to 0");
            prefs.edit().putInt("bridge_battery",0).apply();
            //if (Home.get_master()) GcmActivity.sendBridgeBattery(prefs.getInt("bridge_battery",-1));
        }
        Log.i(TAG, "onCreate: STARTING SERVICE");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("dexcollect-service", 120000);
        retry_time = 0;
        failover_time = 0;
        static_use_rfduino_bluetooth = use_rfduino_bluetooth;
        static_use_transmiter_pl_bluetooth = use_transmiter_pl_bluetooth;
        lastState = "Started " + JoH.hourMinuteString();
        final Context context = getApplicationContext();
        if (shouldServiceRun()) {
            setFailoverTimer();
        } else {
            lastState = "Stopping "+JoH.hourMinuteString();
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }
        lastdata = null;
        attemptConnection();
        watchdog();
        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    private static boolean shouldServiceRun() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return false;
        final boolean result = (DexCollectionType.hasXbridgeWixel() || DexCollectionType.hasBtWixel()) && PersistentStore.getBoolean(CollectionServiceStarter.pref_run_wear_collector);
        Log.d(TAG, "shouldServiceRun() returning: " + result);
        return result;
    }

    @Override
    public void onDestroy() {
        status("Shutdown");
        super.onDestroy();
        Log.d(TAG, "onDestroy entered");
        close();
        //KS foregroundServiceStarter.stop();
        if (shouldServiceRun()) {//Android killed service
            setRetryTimer();
        }
        else {//onDestroy triggered by CollectionServiceStart.stopBtService
            Log.d(TAG, "onDestroy stop Alarm serviceIntent");
            JoH.cancelAlarm(this,serviceIntent);
            Log.d(TAG, "onDestroy stop Alarm serviceFailoverIntent");
            JoH.cancelAlarm(this,serviceFailoverIntent);
            status("Service full stop");
            retry_time = 0;
            failover_time = 0;
        }
        //KS BgToSpeech.tearDownTTS();
        Log.i(TAG, "SERVICE STOPPED");
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            /* //KS not needed on wear device
            if (key.compareTo("run_service_in_foreground") == 0) {
                Log.d("FOREGROUND", "run_service_in_foreground changed!");
                if (prefs.getBoolean("run_service_in_foreground", false)) {
                    foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), dexCollectionService);
                    foregroundServiceStarter.start();
                    Log.d(TAG, "Moving to foreground");
                } else {
                    dexCollectionService.stopForeground(true);
                    Log.d(TAG, "Removing from foreground");
                }
            }
            */
            if(key.equals("dex_collection_method") || key.equals("dex_txid")){
                //if the input method or ID changed, accept any new package once even if they seem duplicates
                Log.d(TAG, "collection method or txID changed - setting lastdata to null");
                lastdata = null;
            }
        }
    };

    public void listenForChangeInSettings() {
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    public void setRetryTimer() {
        mStaticState = mConnectionState;
        if (CollectionServiceStarter.isBTWixel(getApplicationContext())
                || CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()
                || CollectionServiceStarter.isWifiandBTWixel(getApplicationContext())) {
            long retry_in;
            if(CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()) {
                retry_in = (1000 * 25);
            }else {
                //retry_in = (1000*65);
                retry_in = (1000 * 25); // same for both for testing
            }
            Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / 1000) + " seconds");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            long wakeTime = calendar.getTimeInMillis() + retry_in;
            retry_time = wakeTime;
            if (serviceIntent != null)
                alarm.cancel(serviceIntent);
            serviceIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceIntent);
        }
    }

    public void setFailoverTimer() {
        if (CollectionServiceStarter.isBTWixel(getApplicationContext())
                || CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()
                || CollectionServiceStarter.isWifiandBTWixel(getApplicationContext())
                || CollectionServiceStarter.isFollower(getApplicationContext())) {

            long retry_in = (1000 * 60 * 6);
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (60 * 1000)) + " minutes");
            Calendar calendar = Calendar.getInstance();
            AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
            long wakeTime = calendar.getTimeInMillis() + retry_in;
            failover_time = wakeTime;
            if (serviceFailoverIntent != null)
                alarm.cancel(serviceFailoverIntent);
            serviceFailoverIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, wakeTime, serviceFailoverIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarm.setExact(AlarmManager.RTC_WAKEUP, wakeTime, serviceFailoverIntent);
            } else
                alarm.set(AlarmManager.RTC_WAKEUP, wakeTime, serviceFailoverIntent);
        } else {
            stopSelf();
        }
    }

    private static void status(String msg) {
        lastState = msg + " " + JoH.hourMinuteString();
    }

    public void attemptConnection() {
        status("Attempting connection");
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            status("No bluetooth manager");
            setRetryTimer();
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            status("No bluetooth adapter");
            setRetryTimer();
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            if (Home.getPreferencesBoolean("automatically_turn_bluetooth_on",true)) {
                Log.i(TAG, "Turning bluetooth on as appears disabled");
                status("Turning bluetooth on");
                JoH.setBluetoothEnabled(getApplicationContext(), true);
            } else {
                Log.d(TAG,"Not automatically turning on bluetooth due to preferences");
            }
        }

        if (device != null) {
            mConnectionState = STATE_DISCONNECTED;
            for (BluetoothDevice bluetoothDevice : bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                if (bluetoothDevice.getAddress().compareTo(device.getAddress()) == 0) {
                    mConnectionState = STATE_CONNECTED;
                }
            }
        }

        Log.i(TAG, "attemptConnection: Connection state: " + getStateStr(mConnectionState));
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice != null) {
                final String deviceAddress = btDevice.address;
                try {
                    if (mBluetoothAdapter.isEnabled() && mBluetoothAdapter.getRemoteDevice(deviceAddress) != null) {
                        status("Connecting" + (Home.get_engineering_mode() ? ": "+deviceAddress : ""));
                        connect(deviceAddress);
                        mStaticState = mConnectionState;
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "IllegalArgumentException: " + e);
                }
            }
        } else if (mConnectionState == STATE_CONNECTED) { //WOOO, we are good to go, nothing to do here!
            status("Last Connected");
            Log.i(TAG, "attemptConnection: Looks like we are already connected, going to read!");
            mStaticState = mConnectionState;
            return;
        }
        setRetryTimer();
    }

    private static String getStateStr(int mConnectionState) {
        mStaticState = mConnectionState;
        switch (mConnectionState){
            case STATE_CONNECTED:
                return "CONNECTED";
            case STATE_CONNECTING:
                return "CONNECTING";
            case STATE_DISCONNECTED:
                return "DISCONNECTED";
            case STATE_DISCONNECTING:
                return "DISCONNECTING";
            default:
                return "UNKNOWN STATE!";
        }
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("bluetooth-gatt", 60000);
            try {
                if (Home.getPreferencesBoolean("bluetooth_excessive_wakelocks", true)) {
                    PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock2 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "DexCollectionService");
                    wakeLock2.acquire(45000);
                }
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        mConnectionState = STATE_CONNECTED;
                        ActiveBluetoothDevice.connected();
                        Log.i(TAG, "onConnectionStateChange: Connected to GATT server.");
                        if (JoH.ratelimit("attempt-connection", 30)) {
                            attemptConnection(); // refresh status info
                        }
                        mBluetoothGatt.discoverServices();
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.i(TAG, "onConnectionStateChange: State disconnected.");
                        mConnectionState = STATE_DISCONNECTED;
                        ActiveBluetoothDevice.disconnected();
                        if (prefs.getBoolean("close_gatt_on_ble_disconnect", true)) {
                            if (mBluetoothGatt != null) {
                                Log.i(TAG, "onConnectionStateChange: mBluetoothGatt is not null, closing.");
                                mBluetoothGatt.close();
                                mBluetoothGatt = null;
                                mCharacteristic = null;
                            }
                            lastdata = null;
                        }
                        Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server.");
                        setRetryTimer();
                        break;
                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
            mStaticState = mConnectionState;
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered received: " + status);
                return;
            }
            final PowerManager.WakeLock wl = JoH.getWakeLock("bluetooth-onservices", 60000);
            Log.d(TAG, "onServicesDiscovered received status: " + status);

            final BluetoothGattService gattService = mBluetoothGatt.getService(xDripDataService);
            if (gattService == null) {
                Log.w(TAG, "onServicesDiscovered: service " + xDripDataService + " not found");
                listAvailableServices(mBluetoothGatt);
                JoH.releaseWakeLock(wl);
                return;
            }

            final BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(xDripDataCharacteristic);
            if (gattCharacteristic == null) {
                Log.w(TAG, "onServicesDiscovered: characteristic " + xDripDataCharacteristic + " not found");
                JoH.releaseWakeLock(wl);
                return;
            }

            mCharacteristic = gattCharacteristic;
            final int charaProp = gattCharacteristic.getProperties();
            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);

                // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
                if (use_transmiter_pl_bluetooth) {
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                    Log.i(TAG, "Transmiter Descriptor found: " + descriptor.getUuid());
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }

                // Experimental support for rfduino from Tomasz Stachowicz
                if (use_rfduino_bluetooth) {
                    BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                    Log.i(TAG, "Transmiter Descriptor found use_rfduino_bluetooth: " + descriptor.getUuid());
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                    Log.w(TAG, "onServicesDiscovered: use_rfduino_bluetooth send characteristic " + xDripDataCharacteristicSend + " found");
                    final BluetoothGattCharacteristic gattCharacteristicSend = gattService.getCharacteristic(xDripDataCharacteristicSend);
                    mCharacteristicSend = gattCharacteristicSend;
                } else {
                    mCharacteristicSend = mCharacteristic;
                }


            } else {
                Log.w(TAG, "onServicesDiscovered: characteristic " + xDripDataCharacteristic + " not found");
            }
            JoH.releaseWakeLock(wl);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            PowerManager.WakeLock wakeLock1 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "DexCollectionService");
            wakeLock1.acquire();
            try {
                final byte[] data = characteristic.getValue();
                final String hexdump = HexDump.dumpHexString(data);
                if (!hexdump.contains("0x00000000 00      ")) {
                    static_last_hexdump = hexdump;
                }
                Log.i(TAG, "onCharacteristicChanged entered " + hexdump);
                if (data != null && data.length > 0) {
                    setSerialDataToTransmitterRawData(data, data.length);
                }
                lastdata = data;
            } finally {
                if (Home.getPreferencesBoolean("bluetooth_frequent_reset",false))
                {
                    Log.e(TAG,"Resetting bluetooth due to constant reset option being set!");
                    JoH.restartBluetooth(getApplicationContext(),5000);
                }
                wakeLock1.release();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                                      int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onDescriptorWrite: Wrote GATT Descriptor successfully.");
            } else {
                Log.d(TAG, "onDescriptorWrite: Error writing GATT Descriptor: " + status);
            }
        }
    };

    /**
     * Displays all services and characteristics for debugging purposes.
     * @param bluetoothGatt BLE gatt profile.
     */
    private void listAvailableServices(BluetoothGatt bluetoothGatt) {
        Log.d(TAG, "Listing available services:");
        for (BluetoothGattService service : bluetoothGatt.getServices()) {
            Log.d(TAG, "Service: " + service.getUuid().toString());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                Log.d(TAG, "|-- Characteristic: " + characteristic.getUuid().toString());
            }
        }
    }

    private boolean sendBtMessage(final ByteBuffer message) {
        //check mBluetoothGatt is available
        Log.i(TAG, "sendBtMessage: entered");
        if (mBluetoothGatt == null) {
            Log.w(TAG, "sendBtMessage: lost connection");
            return false;
        }

        final byte[] value = message.array();

        static_last_sent_hexdump = HexDump.dumpHexString(value);
        Log.i(TAG, "sendBtMessage: sending message: " + static_last_sent_hexdump);

        // Experimental support for rfduino from Tomasz Stachowicz
        if (use_rfduino_bluetooth ) {
            Log.w(TAG, "sendBtMessage: use_rfduino_bluetooth");
            mCharacteristicSend.setValue(value);
            return mBluetoothGatt.writeCharacteristic(mCharacteristicSend);
        }

        mCharacteristic.setValue(value);
        return mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    private static Integer convertSrc(final String Src) {
        Integer res = 0;
        String tmpSrc = Src.toUpperCase();
        res |= getSrcValue(tmpSrc.charAt(0)) << 20;
        res |= getSrcValue(tmpSrc.charAt(1)) << 15;
        res |= getSrcValue(tmpSrc.charAt(2)) << 10;
        res |= getSrcValue(tmpSrc.charAt(3)) << 5;
        res |= getSrcValue(tmpSrc.charAt(4));
        return res;
    }

    private static int getSrcValue(char ch) {
        int i;
        char[] cTable = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'W', 'X', 'Y'};
        for (i = 0; i < cTable.length; i++) {
            if (cTable[i] == ch) break;
        }
        return i;
    }

    public synchronized boolean connect(final String address) {
        Log.i(TAG, "connect: going to connect to device at address: " + address);
        if (mBluetoothAdapter == null || address == null) {
            Log.i(TAG, "connect: BluetoothAdapter not initialized or unspecified address.");
            setRetryTimer();
            return false;
        }
        if (mBluetoothGatt != null) {
            Log.i(TAG, "connect: mBluetoothGatt isnt null, Closing.");
            try {
                mBluetoothGatt.close();
            } catch (NullPointerException e) {
                Log.wtf(TAG, "Concurrency related null pointer in connect");
            }
            mBluetoothGatt = null;
        }
        device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            setRetryTimer();
            return false;
        }
        Log.i(TAG, "connect: Trying to create a new connection.");
        setRetryTimer();
        mBluetoothGatt = device.connectGatt(getApplicationContext(), true, mGattCallback);
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    public void close() {
        Log.i(TAG, "close: Closing Connection - setting state DISCONNECTED");
        if (mBluetoothGatt == null) {
            return;
        }
        try {
            mBluetoothGatt.close();
        } catch (NullPointerException e) {
            Log.wtf(TAG, "Concurrency related null pointer in close");
        }

        setRetryTimer();
        mBluetoothGatt = null;
        mCharacteristic = null;
        mConnectionState = STATE_DISCONNECTED;
    }

    public void setSerialDataToTransmitterRawData(byte[] buffer, int len) {
        long timestamp = new Date().getTime();
        last_time_seen = JoH.ts();
        watchdog_count=0;
        if (((buffer.length > 0) && (buffer[0] == 0x07 || buffer[0] == 0x11 || buffer[0] == 0x15)) || CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: Dealing with Dexbridge packet!");
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
                Log.i(TAG, "setSerialDataToTransmitterRawData: Received Beacon packet.");
                //DexSrc starts at Byte 2 of a Beacon packet.
                DexSrc = tmpBuffer.getInt(2);
                TxId = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("dex_txid", "00000");
                TransmitterID = convertSrc(TxId);
                if (TxId.compareTo("00000") != 0 && Integer.compare(DexSrc, TransmitterID) != 0) {
                    Log.w(TAG, "setSerialDataToTransmitterRawData: TXID wrong.  Expected " + TransmitterID + " but got " + DexSrc + " dex_txid: " + TxId);
                    txidMessage.put(0, (byte) 0x06);
                    txidMessage.put(1, (byte) 0x01);
                    txidMessage.putInt(2, TransmitterID);
                    sendBtMessage(txidMessage);
                }
                return;
            }
            if ((buffer[0] == 0x11 || buffer[0] == 0x15) && buffer[1] == 0x00) {
                //we have a data packet.  Check to see if the TXID is what we are expecting.
                Log.i(TAG, "setSerialDataToTransmitterRawData: Received Data packet");
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
                    PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("bridge_battery", ByteBuffer.wrap(buffer).get(11)).apply();
                    last_battery_level = Home.getPreferencesInt("bridge_battery",-1);
                    //All is OK, so process it.
                    //first, tell the wixel it is OK to sleep.
                    Log.d(TAG, "setSerialDataToTransmitterRawData: Sending Data packet Ack, to put wixel to sleep");
                    ByteBuffer ackMessage = ByteBuffer.allocate(2);
                    ackMessage.put(0, (byte) 0x02);
                    ackMessage.put(1, (byte) 0xF0);
                    sendBtMessage(ackMessage);
                    //duplicates are already filtered in TransmitterData.create - so no need to filter here
                    lastPacketTime = secondsNow;
                    Log.v(TAG, "setSerialDataToTransmitterRawData: Creating TransmitterData at " + timestamp);
                    processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
                    //KS if (Home.get_master()) GcmActivity.sendBridgeBattery(Home.getPreferencesInt("bridge_battery",-1));
                    CheckBridgeBattery.checkBridgeBattery();
                }
            }
        } else {
            processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
        }
    }

    private void processNewTransmitterData(TransmitterData transmitterData, long timestamp) {
        if (transmitterData == null) {
            return;
        }

        Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        if (use_transmiter_pl_bluetooth && (transmitterData.raw_data == 100000)) {
            Log.wtf(TAG, "Ignoring probably erroneous Transmiter_PL data: " + transmitterData.raw_data);
            return;
        }

        sensor.latest_battery_level = (sensor.latest_battery_level != 0) ? Math.min(sensor.latest_battery_level, transmitterData.sensor_battery_level) : transmitterData.sensor_battery_level;
        sensor.save();

        last_transmitter_Data = transmitterData;
        Log.d(TAG, "BgReading.create: new BG reading at " + timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, transmitterData.timestamp);
    }

    private void watchdog() {
        if (last_time_seen == 0) return;
        if (prefs.getBoolean("bluetooth_watchdog",false)) {
            if ((JoH.ts() - last_time_seen) > 1200000) {
                if (!JoH.isOngoingCall()) {
                    Log.e(TAG, "Watchdog triggered, attempting to reset bluetooth");
                    status("Watchdog triggered");
                    JoH.restartBluetooth(getApplicationContext());
                    last_time_seen = JoH.ts();
                    watchdog_count++;
                    if (watchdog_count>5) last_time_seen=0;
                } else {
                    Log.e(TAG,"Delaying watchdog reset as phone call is ongoing.");
                }
            }
        }
    }

    // Status for Watchface
    public static boolean isRunning() {
        return lastState.equals("Not Running") || lastState.startsWith("Stopping", 0) ? false : true;
    }

    public static void setWatchStatus(DataMap dataMap) {
        lastStateWatch = dataMap.getString("lastState", "");
        last_transmitter_DataWatch = new TransmitterData();
        last_transmitter_DataWatch.timestamp = dataMap.getLong("timestamp", 0);
        mStaticStateWatch = dataMap.getInt("mStaticState", 0);
        last_battery_level_watch = dataMap.getInt("last_battery_level", -1);
        retry_time_watch = dataMap.getLong("retry_time", 0);
        failover_time_watch = dataMap.getLong("failover_time", 0);
        static_last_hexdump_watch = dataMap.getString("static_last_hexdump", "");
        static_last_sent_hexdump_watch = dataMap.getString("static_last_sent_hexdump", "");
    }

    public static DataMap getWatchStatus() {
        DataMap dataMap = new DataMap();
        dataMap.putString("lastState", lastState);
        if (last_transmitter_Data != null) dataMap.putLong("timestamp", last_transmitter_Data.timestamp);
        dataMap.putInt("mStaticState", mStaticState);
        dataMap.putInt("last_battery_level", last_battery_level);
        dataMap.putLong("retry_time", retry_time);
        dataMap.putLong("failover_time", failover_time);
        dataMap.putString("static_last_hexdump", static_last_hexdump);
        dataMap.putString("static_last_sent_hexdump", static_last_sent_hexdump);
        return dataMap;
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();

        l.add(new StatusItem("Phone Service State", lastState));
        l.add(new StatusItem("Bluetooth Device", JoH.ucFirst(getStateStr(mStaticState))));


        if (static_use_transmiter_pl_bluetooth) {
            l.add(new StatusItem("Hardware", "Transmiter PL"));
        }

        if (static_use_rfduino_bluetooth) {
            l.add(new StatusItem("Hardware", "Rfduino"));
        }

        // TODO add LimiTTer info

        if (last_transmitter_Data != null) {
            l.add(new StatusItem("Glucose data from", JoH.niceTimeSince(last_transmitter_Data.timestamp) + " ago"));
        }
        if (last_battery_level > -1) {
            l.add(new StatusItem("Battery level", last_battery_level));
        }

        if (retry_time > 0) l.add(new StatusItem("Next Retry", JoH.niceTimeTill(retry_time)));
        if (failover_time > 0)
            l.add(new StatusItem("Next Wake up", JoH.niceTimeTill(failover_time)));

        if (Home.get_engineering_mode() && (static_last_hexdump != null)) {
            l.add(new StatusItem("Received Data", filterHexdump(static_last_hexdump)));
        }
        if (Home.get_engineering_mode() && (static_last_sent_hexdump != null)) {
            l.add(new StatusItem("Sent Data", filterHexdump(static_last_sent_hexdump)));
        }

        //WATCH
        if (Home.getPreferencesBooleanDefaultFalse("wear_sync") &&
                Home.getPreferencesBooleanDefaultFalse("enable_wearG5") &&
                Home.getPreferencesBooleanDefaultFalse("force_wearG5")) {
            l.add(new StatusItem("Watch Service State", lastStateWatch));
            l.add(new StatusItem("Bluetooth Device", JoH.ucFirst(getStateStr(mStaticStateWatch))));

            // TODO add LimiTTer info

            if (last_transmitter_DataWatch != null) {
                l.add(new StatusItem("Glucose data from", JoH.niceTimeSince(last_transmitter_DataWatch.timestamp) + " ago"));
            }
            if (last_battery_level_watch > -1) {
                l.add(new StatusItem("Battery level", last_battery_level_watch));
            }

            if (retry_time_watch > 0) l.add(new StatusItem("Next Retry", JoH.niceTimeTill(retry_time_watch)));
            if (failover_time_watch > 0)
                l.add(new StatusItem("Next Wake up", JoH.niceTimeTill(failover_time_watch)));

            if (Home.get_engineering_mode() && (static_last_hexdump_watch != null)) {
                l.add(new StatusItem("Received Data", filterHexdump(static_last_hexdump_watch)));
            }
            if (Home.get_engineering_mode() && (static_last_sent_hexdump_watch != null)) {
                l.add(new StatusItem("Sent Data", filterHexdump(static_last_sent_hexdump_watch)));
            }
        }

        return l;
    }
    private static String filterHexdump(String hex) {
        return hex.replaceAll("[ ]+"," ").replaceAll("\n0x0000[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f] ","\n").replaceFirst("^\n","");
    }
}
