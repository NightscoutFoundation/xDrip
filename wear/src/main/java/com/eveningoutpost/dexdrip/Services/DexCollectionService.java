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
import android.app.PendingIntent;
import android.app.Service;
import android.app.UiModeManager;
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
import android.content.res.Configuration;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.blueReader;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.ForegroundServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.HM10Attributes;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.UtilityModels.XbridgePlus;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.wearable.DataMap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.G5Model.BluetoothServices.getStatusName;
import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DexCollectionService extends Service {
    private final static String TAG = DexCollectionService.class.getSimpleName();
    private static final boolean d = false;
    private SharedPreferences prefs;

    private static PendingIntent serviceIntent;
    private static PendingIntent serviceFailoverIntent;
    public DexCollectionService dexCollectionService;

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private String mDeviceAddress;
    private ForegroundServiceStarter foregroundServiceStarter;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTING;
    private static int mStaticState = BluetoothProfile.STATE_DISCONNECTING;
    private static int mStaticStateWatch = 0; // default unknown
    private static byte[] immediateSend;
    private static String bondedState;
    private static int bondingTries = 0;
    private BluetoothDevice device;
    private BluetoothGattCharacteristic mCharacteristic;
    // Experimental support for rfduino from Tomasz Stachowicz
    private BluetoothGattCharacteristic mCharacteristicSend;
    long lastPacketTime;
    private byte[] lastdata = null;
    private final Object mLock = new Object();
    //private Context mContext;
    private static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;
    private static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    private static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    private static final String PREF_DEX_COLLECTION_BONDING = "pref_dex_collection_bonding";
    private static final String PREF_DEX_COLLECTION_POLLING = "pref_dex_collection_polling";
    private static final long POLLING_PERIOD = (Constants.MINUTE_IN_MS * 5) - Constants.SECOND_IN_MS;
    private static final long RETRY_PERIOD = DEXCOM_PERIOD - (Constants.SECOND_IN_MS * 35);
    private static final long TOLERABLE_JITTER = 10000;

    public static double last_time_seen = 0;
    public static String lastState = "Not running";
    public static TransmitterData last_transmitter_Data;
    private static int last_battery_level = -1;
    private static long retry_time = 0;
    private static long failover_time = 0;
    private static long poll_backoff = 0;
    private static long retry_backoff = 0;
    private static long descriptor_time = 0;
    private static int watchdog_count = 0;
    private static long max_wakeup_jitter = 0;
    private static DISCOVERED servicesDiscovered = DISCOVERED.NULL;


    private static boolean static_use_transmiter_pl_bluetooth = false;
    private static boolean static_use_rfduino_bluetooth = false;
    private static boolean static_use_polling = false;
    private static boolean static_use_blukon = false;
    private static boolean static_use_nrf = false;
    private static String static_last_hexdump;
    private static String static_last_sent_hexdump;

    //WATCH:
    public static String lastStateWatch = "Not running";
    private static TransmitterData last_transmitter_DataWatch;
    private static int last_battery_level_watch = -1;
    private static long last_poll_sent = 0;
    private static long retry_time_watch = 0;
    private static long failover_time_watch = 0;
    private static String static_last_hexdump_watch;
    private static String static_last_sent_hexdump_watch;

    // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
    private final boolean use_transmiter_pl_bluetooth = Pref.getBooleanDefaultFalse("use_transmiter_pl_bluetooth");
    private final boolean use_rfduino_bluetooth = Pref.getBooleanDefaultFalse("use_rfduino_bluetooth");
  //  private final boolean use_polling = Home.getBooleanDefaultFalse(PREF_DEX_COLLECTION_POLLING) && DexCollectionType.hasLibre();
    private final boolean use_polling = Pref.getBooleanDefaultFalse(PREF_DEX_COLLECTION_POLLING);
    private final UUID xDripDataService = use_transmiter_pl_bluetooth ? UUID.fromString(HM10Attributes.TRANSMITER_PL_SERVICE) : UUID.fromString(HM10Attributes.HM_10_SERVICE);
    private final UUID xDripDataCharacteristic = use_transmiter_pl_bluetooth ? UUID.fromString(HM10Attributes.TRANSMITER_PL_RX_TX) : UUID.fromString(HM10Attributes.HM_RX_TX);
    // Experimental support for rfduino from Tomasz Stachowicz
    private final UUID xDripDataCharacteristicSend = use_rfduino_bluetooth ? UUID.fromString(HM10Attributes.HM_TX) : UUID.fromString(HM10Attributes.HM_RX_TX);

    private final String DEFAULT_BT_PIN = getDefaultPin();

    public final UUID CCCD = UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG);
    public final UUID nrfDataService = UUID.fromString(HM10Attributes.NRF_UART_SERVICE);
    public final UUID nrfDataRXCharacteristic = UUID.fromString(HM10Attributes.NRF_UART_TX);
    public final UUID nrfDataTXCharacteristic = UUID.fromString(HM10Attributes.NRF_UART_RX);

    private final UUID blukonDataService = UUID.fromString(HM10Attributes.BLUKON_SERVICE);

    private enum DISCOVERED {
        NULL,
        PENDING,
        COMPLETE
    }

    private static String getDefaultPin() {
        final String bk_pin = Blukon.getPin();
        return bk_pin != null ? bk_pin : HM10Attributes.HM_DEFAULT_BT_PIN;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        foregroundServiceStarter = new ForegroundServiceStarter(getApplicationContext(), this);
        foregroundServiceStarter.start();
        //mContext = getApplicationContext();
        dexCollectionService = this;
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        listenForChangeInSettings();
        //bgToSpeech = BgToSpeech.setupTTS(mContext); //keep reference to not being garbage collected
        if(CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()){
            Log.i(TAG,"onCreate: resetting bridge_battery preference to 0");
            prefs.edit().putInt("bridge_battery",0).apply();
            //if (Home.get_master()) GcmActivity.sendBridgeBattery(prefs.getInt("bridge_battery",-1));
        }

        final IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
        registerReceiver(mPairingRequestRecevier, pairingRequestFilter);
        Log.i(TAG, "onCreate: STARTING SERVICE: pin code: " + DEFAULT_BT_PIN);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("dexcollect-service", 120000);
        if (retry_time > 0 && failover_time > 0) {
            final long requested_wake_time = Math.min(retry_time, failover_time);
            final long wakeup_jitter = JoH.msSince(requested_wake_time);
            Log.d(TAG, "Wake up jitter: " + JoH.niceTimeScalar(wakeup_jitter));
            if ((wakeup_jitter > TOLERABLE_JITTER) && (!JoH.buggy_samsung) && (Build.MANUFACTURER.toLowerCase().contains("samsung"))) {
                UserError.Log.wtf(TAG, "Enabled Buggy Samsung workaround due to jitter of: " + JoH.niceTimeScalar(wakeup_jitter));
                JoH.buggy_samsung = true;
                max_wakeup_jitter = 0;
            } else {
                max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
            }
        }
        retry_time = 0;
        failover_time = 0;
        static_use_rfduino_bluetooth = use_rfduino_bluetooth;
        static_use_transmiter_pl_bluetooth = use_transmiter_pl_bluetooth;
        static_use_polling = use_polling;
        status("Started");
        if (shouldServiceRun()) {
            setFailoverTimer();
        } else {
            status("Stopping");
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }
        lastdata = null;
        checkConnection();
        watchdog();
        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }


    private static boolean shouldServiceRun() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return false;
        final boolean result = (DexCollectionType.hasXbridgeWixel() || DexCollectionType.hasBtWixel())
                && ((!Home.get_forced_wear() && (((UiModeManager) xdrip.getAppContext().getSystemService(UI_MODE_SERVICE)).getCurrentModeType() != Configuration.UI_MODE_TYPE_WATCH))
                || PersistentStore.getBoolean(CollectionServiceStarter.pref_run_wear_collector));
        if (d) Log.d(TAG, "shouldServiceRun() returning: " + result);
        return result;
    }


    @Override
    public void onDestroy() {
        status("Shutdown");
        super.onDestroy();
        Log.d(TAG, "onDestroy entered");
        close();
        foregroundServiceStarter.stop();

        try {
            unregisterReceiver(mPairingRequestRecevier);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error unregistering pairing receiver: " + e);
        }

        if (shouldServiceRun()) {//Android killed service
            setRetryTimer();
            status("Stopped, attempting restart");
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
        //BgToSpeech.tearDownTTS();

        retry_backoff = 0;
        poll_backoff = 0;
        servicesDiscovered = DISCOVERED.NULL;
        bondingTries = 0;

        Log.i(TAG, "SERVICE STOPPED");
    }

    // remember needs proguard exclusion due to access by reflection
    public static boolean isCollecting() {
       if (static_use_blukon) {
           return Blukon.isCollecting();
       }
        return false;
    }

    public SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
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
        if (shouldServiceRun()) {
            //final long retry_in = (Constants.SECOND_IN_MS * 25);
            final long retry_in = whenToRetryNext();
            Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            serviceIntent = PendingIntent.getService(this, Constants.DEX_COLLECTION_SERVICE_RETRY_ID, new Intent(this, this.getClass()), 0);
            retry_time = JoH.wakeUpIntent(this, retry_in, serviceIntent);
        } else {
            Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    public synchronized void setFailoverTimer() {
        if (shouldServiceRun()) {
            final long retry_in = use_polling ? whenToPollNext() : (Constants.MINUTE_IN_MS * 6);
            Log.d(TAG, "setFailoverTimer: Fallover Restarting in: " + (retry_in / (Constants.MINUTE_IN_MS)) + " minutes");
            serviceFailoverIntent = PendingIntent.getService(this, Constants.DEX_COLLECTION_SERVICE_FAILOVER_ID, new Intent(this, this.getClass()), 0);
            failover_time = JoH.wakeUpIntent(this, retry_in, serviceFailoverIntent);
            retry_time = 0; // only one alarm will run
        } else {
            stopSelf();
        }
    }

    private long whenToRetryNext() {
        final long poll_time = Math.max((Constants.SECOND_IN_MS * 10) + retry_backoff, RETRY_PERIOD - JoH.msSince(lastPacketTime));
        if (retry_backoff < (Constants.MINUTE_IN_MS)) {
            retry_backoff += Constants.SECOND_IN_MS;
        }
        Log.d(TAG, "Scheduling next retry in: " + JoH.niceTimeScalar(poll_time) + " @ " + JoH.dateTimeText(poll_time + JoH.tsl()) + " period diff: " + (RETRY_PERIOD - JoH.msSince(lastPacketTime)));
        return poll_time;
    }

    private long whenToPollNext() {
        final long poll_time = Math.max((Constants.SECOND_IN_MS * 5) + poll_backoff, POLLING_PERIOD - JoH.msSince(lastPacketTime));
        if (poll_backoff < (Constants.MINUTE_IN_MS * 6)) {
            poll_backoff += Constants.SECOND_IN_MS;
        }
        Log.d(TAG, "Scheduling next poll in: " + JoH.niceTimeScalar(poll_time) + " @ " + JoH.dateTimeText(poll_time + JoH.tsl()) + " period diff: " + (POLLING_PERIOD - JoH.msSince(lastPacketTime)));
        return poll_time;
    }

    private static void status(String msg) {
        lastState = msg + " " + JoH.hourMinuteString();
    }

    synchronized void checkConnection() {
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
            if (Pref.getBoolean("automatically_turn_bluetooth_on",true)) {
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

        Log.i(TAG, "checkConnection: Connection state: " + getStateStr(mConnectionState));
        if (mConnectionState == STATE_DISCONNECTED || mConnectionState == STATE_DISCONNECTING) {
            final ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice != null) {
                final String deviceAddress = btDevice.address;
                mDeviceAddress = deviceAddress;
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
            Log.i(TAG, "checkConnection: Looks like we are already connected, ready to receive");
            mStaticState = mConnectionState;
            if (use_polling && (JoH.msSince(lastPacketTime) >= POLLING_PERIOD)) {
                pollForData();
            }
            return;
        }
        setRetryTimer();
    }

    private synchronized void checkImmediateSend() {
        if (immediateSend != null) {
            Log.d(TAG, "Sending immediate data: " + JoH.bytesToHex(immediateSend));
            sendBtMessage(immediateSend);
            immediateSend = null;
        }
    }

    private synchronized void pollForData() {
        if (JoH.ratelimit("poll-for-data", 5)) {
            new Thread() {
                @Override
                public void run() {
                    Log.d(TAG, "Polling for data");
                    int wait_counter = 0;
                    while (servicesDiscovered != DISCOVERED.COMPLETE && wait_counter < 10) {
                        Log.d(TAG, "Waiting for service discovery: " + servicesDiscovered + " count: " + wait_counter);
                        try {
                            Thread.sleep(200); // delay for wakeup readiness
                        } catch (InterruptedException e) {
                            //
                        }
                        wait_counter++;
                    }
                    if (servicesDiscovered == DISCOVERED.NULL) {
                        Log.e(TAG, "Failed to discover services!");
                        try {
                            if (JoH.ratelimit("rediscover-services", 30)) {
                                Log.d(TAG, "Refresh result: " + JoH.refreshDeviceCache(TAG, mBluetoothGatt));
                                mBluetoothGatt.discoverServices();
                            }
                        } catch (Exception e) {
                            Log.d(TAG, "Exception discovering services: " + e);
                        }
                    }
                    last_poll_sent = JoH.tsl();
                    if ((JoH.msSince(lastPacketTime) > Home.stale_data_millis()) && (JoH.ratelimit("poll-request-part-b", 15))) {
                        Log.e(TAG, "Stale data so requesting backfill");
                        sendBtMessage(XbridgePlus.sendLast15BRequestPacket());
                    } else {
                        sendBtMessage(XbridgePlus.sendDataRequestPacket());
                    }
                }
            }.start();
        }
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

    private final BroadcastReceiver mPairingRequestRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Received pairing request");
            JoH.doPairingRequest(context, this, intent, mDeviceAddress, DEFAULT_BT_PIN) ;
        }
    };

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("bluetooth-gatt", 60000);
            try {
                if (Pref.getBoolean("bluetooth_excessive_wakelocks", true)) {
                  /*  PowerManager powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock2 = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "DexCollectionService");
                    wakeLock2.acquire(45000);*/
                    final PowerManager.WakeLock wakeLock2 = JoH.getWakeLock("DexCollectionExcessive", 45000);

                }
                switch (newState) {
                    case BluetoothProfile.STATE_CONNECTED:
                        mConnectionState = STATE_CONNECTED;
                        if ((servicesDiscovered == DISCOVERED.NULL) || Pref.getBoolean("always_discover_services", true)) {
                            Log.d(TAG, "Requesting to discover services: previous: " + servicesDiscovered);
                            servicesDiscovered = DISCOVERED.PENDING;
                        }
                        ActiveBluetoothDevice.connected();
                        Log.i(TAG, "onConnectionStateChange: Connected to GATT server.");
                        if (JoH.ratelimit("attempt-connection", 30)) {
                            checkConnection(); // refresh status info
                        }
                        if (servicesDiscovered != DISCOVERED.COMPLETE) {
                            Log.d(TAG, "Calling discoverServices");
                            mBluetoothGatt.discoverServices();
                        } else {
                            Log.d(TAG, "Services already discovered");
                            checkImmediateSend();
                        }
                        break;
                    case BluetoothProfile.STATE_DISCONNECTED:
                        Log.i(TAG, "onConnectionStateChange: State disconnected.");
                        mConnectionState = STATE_DISCONNECTED;
                        ActiveBluetoothDevice.disconnected();
                        if (prefs.getBoolean("close_gatt_on_ble_disconnect", true)) {
                            if (mBluetoothGatt != null) {
                                Log.i(TAG, "onConnectionStateChange: mBluetoothGatt is not null, closing.");
                                if (JoH.ratelimit("refresh-gatt", 60)) {
                                    Log.d(TAG, "Refresh result state close: " + JoH.refreshDeviceCache(TAG, mBluetoothGatt));
                                }
                                mBluetoothGatt.close();
                                mBluetoothGatt = null;
                                mCharacteristic = null;
                                servicesDiscovered = DISCOVERED.NULL;
                            }
                            lastdata = null;
                        }
                        Log.i(TAG, "onConnectionStateChange: Disconnected from GATT server.");
                        setRetryTimer();
                        break;
                }
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Caught exception in Gatt callback " + e);
                UserError.Log.wtf(TAG, e);
            } finally {
                JoH.releaseWakeLock(wl);
            }
            mStaticState = mConnectionState;
        }

        @Override
        public synchronized void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG,"Services discovered start");
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onServicesDiscovered received: " + status);
                return;
            }
            final PowerManager.WakeLock wl = JoH.getWakeLock("bluetooth-onservices", 60000);
            Log.d(TAG, "onServicesDiscovered received status: " + status);

            if (prefs.getBoolean(PREF_DEX_COLLECTION_BONDING, false)) {
                if ((mDeviceAddress != null) && (device != null) && (!areWeBonded(mDeviceAddress))) {
                    if (JoH.ratelimit("dexcollect-create-bond", 20)) {
                        Log.d(TAG, "Attempting to create bond to: " + mDeviceAddress);
                        bondingTries++;
                        if (bondingTries > 5) {
                            Home.toaststaticnext("Bonding failing so disabling bonding feature");
                            Pref.setBoolean(PREF_DEX_COLLECTION_BONDING, false);
                        } else {
                            device.setPin(JoH.convertPinToBytes(DEFAULT_BT_PIN));
                            device.createBond();
                        }
                    }
                }
            }

            final BluetoothGattService gattService = mBluetoothGatt.getService(xDripDataService);
            if (gattService == null) {
                if (!(static_use_blukon || blueReader.isblueReader())) {
                    Log.w(TAG, "onServicesDiscovered: xdrip service " + xDripDataService + " not found"); //TODO the selection of nrf is not active at the beginning,so this error will be trown one time unneeded, mey to be optimized.
                    // TODO this should be reworked to be an efficient selector
                    listAvailableServices(mBluetoothGatt);
                }
                // try next
            }
            else
            {
                final BluetoothGattCharacteristic gattCharacteristic = gattService.getCharacteristic(xDripDataCharacteristic);
                if (gattCharacteristic == null) {
                    Log.w(TAG, "onServicesDiscovered: characteristic " + xDripDataCharacteristic + " not found");
                    JoH.releaseWakeLock(wl);
                    Log.d(TAG,"onServicesDiscovered: returning due to null xDrip characteristic");
                    return;
                }

                mCharacteristic = gattCharacteristic;
                final int charaProp = gattCharacteristic.getProperties();
                if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    // TODO isn't this condition always true, shouldn't it be & instead of | ?
                    mBluetoothGatt.setCharacteristicNotification(gattCharacteristic, true);

                    try {
                        // TODO use CCCD?
                        final BluetoothGattDescriptor bdescriptor = gattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                        Log.i(TAG, "Bluetooth Notification Descriptor found: " + bdescriptor.getUuid());
                        bdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        descriptor_time = JoH.tsl();
                        mBluetoothGatt.writeDescriptor(bdescriptor);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting notification value descriptor: " + e);
                    }

                    // Experimental support for "Transmiter PL" from Marek Macner @FPV-UAV
                    //if (use_transmiter_pl_bluetooth) {
                    // BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                    //   Log.i(TAG, "Transmiter Descriptor found: " + descriptor.getUuid());
                    //   descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    //   mBluetoothGatt.writeDescriptor(descriptor);
                    //}

                    // Experimental support for rfduino from Tomasz Stachowicz
                    if (use_rfduino_bluetooth) {
                        //  BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                        //  Log.i(TAG, "Transmiter Descriptor found use_rfduino_bluetooth: " + descriptor.getUuid());
                        //  descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        //  mBluetoothGatt.writeDescriptor(descriptor);
                        Log.w(TAG, "onServicesDiscovered: use_rfduino_bluetooth send characteristic " + xDripDataCharacteristicSend + " found");
                        mCharacteristicSend = gattService.getCharacteristic(xDripDataCharacteristicSend);
                    } else {
                        mCharacteristicSend = mCharacteristic;
                    }


                } else {
                    Log.w(TAG, "onServicesDiscovered: characteristic " + xDripDataCharacteristic + " not found");
                }
            }

            final BluetoothGattService nrfGattService = mBluetoothGatt.getService(nrfDataService);
            /* if (nrfGattService == null) {
                Log.w(TAG, "onServicesDiscovered: service " + nrfGattService + " not found");
                listAvailableServices(mBluetoothGatt);
                JoH.releaseWakeLock(wl);
                Log.d(TAG,"onServicesDiscovered: returning due to null nrf service");
                return;
            }
            else*/

            if (nrfGattService != null)
            {
               final BluetoothGattCharacteristic nrfGattCharacteristic = nrfGattService.getCharacteristic(nrfDataRXCharacteristic);
                if (nrfGattCharacteristic == null) {
                    Log.w(TAG, "onServicesDiscovered: characteristic " + nrfGattCharacteristic + " not found");
                    JoH.releaseWakeLock(wl);
                    Log.d(TAG,"onServicesDiscovered: returning due to null nrf characteristic");
                    return;
                } else {
                    static_use_nrf = true;
                    mCharacteristic = nrfGattCharacteristic;
                    final int charaProp = nrfGattCharacteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        // TODO isn't this condition always true, shouldn't it be & instead of | ?
                        mBluetoothGatt.setCharacteristicNotification(nrfGattCharacteristic, true);

                        try {
                            final BluetoothGattDescriptor bdescriptor = nrfGattCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                            Log.i(TAG, "Bluetooth Notification Descriptor found: " + bdescriptor.getUuid());
                            bdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mBluetoothGatt.writeDescriptor(bdescriptor);
                        } catch (Exception e) {
                            Log.e(TAG, "Error setting notification value descriptor: " + e);
                        }
                    }
                    mCharacteristic = nrfGattCharacteristic;
                    mCharacteristicSend = nrfGattService.getCharacteristic(nrfDataTXCharacteristic);
                    if (mCharacteristicSend == null) {
                        Log.w(TAG, "onServicesDiscovered: nrf characteristic " + mCharacteristicSend + " not found");
                        JoH.releaseWakeLock(wl);
                        return;
                    }
                    status("Enabled blueReader" );
                    Log.d(TAG,"blueReader initialized and Version requested");
                    sendBtMessage(blueReader.initialize());
                }
            }

            // TODO make these detection sections a generic method where only one can match
            final BluetoothGattService blukonService = mBluetoothGatt.getService(blukonDataService);
            if (blukonService != null) {
                Log.i(TAG, "Found " + getString(R.string.blukon) + " device");
                mCharacteristic = blukonService.getCharacteristic(UUID.fromString(HM10Attributes.BLUKON_UART_RX));
                if (mCharacteristic == null) {
                    Log.w(TAG, "onServicesDiscovered: blukon characteristic " + mCharacteristic + " not found");
                    JoH.releaseWakeLock(wl);
                    return;
                }
                try {
                    final BluetoothGattDescriptor bdescriptor = mCharacteristic.getDescriptor(UUID.fromString(HM10Attributes.CLIENT_CHARACTERISTIC_CONFIG));
                    Log.i(TAG, "Bluetooth Notification Descriptor found: " + bdescriptor.getUuid());
                    bdescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(bdescriptor);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting notification value descriptor: " + e);
                }
                mBluetoothGatt.setCharacteristicNotification(mCharacteristic, true);

                mCharacteristicSend = blukonService.getCharacteristic(UUID.fromString(HM10Attributes.BLUKON_UART_TX));
                if (mCharacteristicSend == null) {
                    Log.w(TAG, "onServicesDiscovered: blukon send characteristic " + mCharacteristicSend + " not found");
                    JoH.releaseWakeLock(wl);
                    return;
                }
                status("Enabled " + getString(R.string.blukon));
                static_use_blukon = true; // doesn't ever get unset
                Blukon.initialize();

            }

            // TODO is this duplicated in some situations?
            try {
                final BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(CCCD);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Got null pointer trying to set CCCD descriptor");
            }
            try {
                mBluetoothGatt.readCharacteristic(mCharacteristic);
            } catch (NullPointerException e) {
                Log.e(TAG, "Got null pointer trying to readCharacteristic");
            }
            Log.d(TAG, "Services discovered end");
            servicesDiscovered = DISCOVERED.COMPLETE;
            // waitFor(300);
            Log.d(TAG, "Services discovered release");
            checkImmediateSend(); // TODO maybe find a better home for this
            JoH.releaseWakeLock(wl);
        }

        @Override
        public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            try {
                final String msg = "OnCharacteristic WRITE: "
                        + " status: " + getStatusName(status) + " data: " + JoH.bytesToHex(characteristic.getValue()) + " uuid: " + characteristic.getUuid();
                if (status == 0) {
                    Log.d(TAG, msg);
                } else {
                    UserError.Log.e(TAG, msg);
                }
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Got exception trying to display data: " + e);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            onCharacteristicChanged(gatt,characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

            final PowerManager.WakeLock wakeLock1 = JoH.getWakeLock("DexCollectionService", 60000);
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
                setFailoverTimer(); // restart the countdown
                // intentionally left hanging wakelock for 5 seconds after we receive something
                final PowerManager.WakeLock wakeLock2 = JoH.getWakeLock("DexCollectionLinger", 5000);
            } finally {
                if (Pref.getBoolean("bluetooth_frequent_reset",false))
                {
                    Log.e(TAG,"Resetting bluetooth due to constant reset option being set!");
                    JoH.restartBluetooth(getApplicationContext(),5000);
                }
                JoH.releaseWakeLock(wakeLock1);
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
            descriptor_time = 0;
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

    private boolean sendBtMessage(byte[] buffer) {
        return sendBtMessage(JoH.bArrayAsBuffer(buffer));
    }

    private synchronized boolean sendBtMessage(final ByteBuffer message) {
        //check mBluetoothGatt is available
        Log.i(TAG, "sendBtMessage: entered");
        if (mBluetoothGatt == null) {
            Log.w(TAG, "sendBtMessage: lost connection");
            if (JoH.ratelimit("sendbtmessagelost", 60)) {
                mConnectionState = STATE_DISCONNECTED;
                setRetryTimer();
            }
            return false;
        }

        final byte[] value = message.array();

        static_last_sent_hexdump = HexDump.dumpHexString(value);
        Log.i(TAG, "sendBtMessage: sending message: " + static_last_sent_hexdump);

        // Experimental support for rfduino from Tomasz Stachowicz
        if (use_rfduino_bluetooth) {
            Log.w(TAG, "sendBtMessage: use_rfduino_bluetooth");
            if (mCharacteristicSend == null) {
                status("Error: mCharacteristicSend was null in sendBtMessage");
                Log.e(TAG, lastState);
                servicesDiscovered = DISCOVERED.NULL;
                return false;
            }
            //mCharacteristicSend.setValue(value);
            //return mBluetoothGatt.writeCharacteristic(mCharacteristicSend);
            return writeChar(mCharacteristicSend, value);
        }

        if (mCharacteristic == null) {
            status("Error: mCharacteristic was null in sendBtMessage");
            Log.e(TAG, lastState);
            servicesDiscovered = DISCOVERED.NULL;
            return false;
        }

        if (mCharacteristicSend != null && mCharacteristicSend != mCharacteristic) {
            // mCharacteristicSend.setValue(value);
            //  return mBluetoothGatt.writeCharacteristic(mCharacteristicSend);
            return writeChar(mCharacteristicSend, value);

        }

        // mCharacteristic.setValue(value);
        // return mBluetoothGatt.writeCharacteristic(mCharacteristic);
        return writeChar(mCharacteristic, value);
    }

    private boolean writeChar(final BluetoothGattCharacteristic localmCharacteristic, final byte[] value) {
        localmCharacteristic.setValue(value);
        boolean result = mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(localmCharacteristic);
        if (!result) {
            UserError.Log.d(TAG, "Error writing characteristic: " + localmCharacteristic.getUuid() + " " + JoH.bytesToHex(value));
            JoH.runOnUiThreadDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        boolean result = mBluetoothGatt != null && mBluetoothGatt.writeCharacteristic(localmCharacteristic);
                        if (!result) {
                            UserError.Log.e(TAG, "Error writing characteristic: (2nd try) " + localmCharacteristic.getUuid() + " " + JoH.bytesToHex(value));
                        } else {
                            UserError.Log.d(TAG, "Succeeded writing characteristic: (2nd try) " + localmCharacteristic.getUuid() + " " + JoH.bytesToHex(value));
                        }
                    } catch (Exception e) {
                        UserError.Log.wtf(TAG, "Exception during 2nd try write: " + e + " " + localmCharacteristic.getUuid() + " " + JoH.bytesToHex(value));
                    }
                }
            }, 500);
        }
        return result;
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
                if (JoH.ratelimit("refresh-gatt", 60)) {
                    Log.d(TAG, "Refresh result close: " + JoH.refreshDeviceCache(TAG, mBluetoothGatt));
                }
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
        servicesDiscovered = DISCOVERED.NULL;
    }

    public synchronized void setSerialDataToTransmitterRawData(byte[] buffer, int len) {
        long timestamp = new Date().getTime();
        last_time_seen = JoH.ts();
        watchdog_count = 0;
        if (static_use_blukon && Blukon.checkBlukonPacket(buffer)) {
            final byte[] reply = Blukon.decodeBlukonPacket(buffer);
            if (reply != null) {
                Log.d(TAG, "Sending reply message from Blukon decoder");
                sendBtMessage(reply);
            }
        } else  if (blueReader.isblueReader()) {
            final byte[] reply = blueReader.decodeblueReaderPacket(buffer, len);
            if (reply != null) {
                Log.d(TAG, "Sending reply message from blueReader decoder");
                sendBtMessage(reply);
            }
        } else if (XbridgePlus.isXbridgeExtensionPacket(buffer)) {
            // handle xBridge+ protocol packets
            final byte[] reply = XbridgePlus.decodeXbridgeExtensionPacket(buffer);
            if (reply != null) {
                Log.d(TAG, "Sending reply message from xBridge decoder");
                sendBtMessage(reply);
            }
        } else {

            if (((buffer.length > 0) && (buffer[0] == 0x07 || buffer[0] == 0x11 || buffer[0] == 0x15)) || CollectionServiceStarter.isDexBridgeOrWifiandDexBridge()) {
                if ((buffer.length == 1) && (buffer[0] == 0x00)) {
                    return; // null packet
                }
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
                        Log.w(TAG, "setSerialDataToTransmitterRawData: TXID wrong.  Expected " + TransmitterID + " but got " + DexSrc);
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
                        Pref.setInt("bridge_battery", ByteBuffer.wrap(buffer).get(11));
                        // PreferenceManager.getDefaultSharedPreferences(mContext).edit().putInt("bridge_battery", ByteBuffer.wrap(buffer).get(11)).apply();
                        last_battery_level = Pref.getInt("bridge_battery", -1);
                        //All is OK, so process it.
                        //first, tell the wixel it is OK to sleep.
                        Log.d(TAG, "setSerialDataToTransmitterRawData: Sending Data packet Ack, to put wixel to sleep");
                        ByteBuffer ackMessage = ByteBuffer.allocate(2);
                        ackMessage.put(0, (byte) 0x02);
                        ackMessage.put(1, (byte) 0xF0);
                        sendBtMessage(ackMessage);
                        //duplicates are already filtered in TransmitterData.create - so no need to filter here
                        lastPacketTime = secondsNow;
                        poll_backoff = 0;
                        retry_backoff = 0;
                        Log.v(TAG, "setSerialDataToTransmitterRawData: Creating TransmitterData at " + timestamp);
                        processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
                        if (Home.get_master())
                            GcmActivity.sendBridgeBattery(Pref.getInt("bridge_battery", -1));
                        CheckBridgeBattery.checkBridgeBattery();
                    }
                }
            }  else {
                processNewTransmitterData(TransmitterData.create(buffer, len, timestamp), timestamp);
            }
        }
    }

    private synchronized void processNewTransmitterData(TransmitterData transmitterData, long timestamp) {
        if (transmitterData == null) {
            return;
        }

        final Sensor sensor = Sensor.currentSensor();
        if (sensor == null) {
            Log.i(TAG, "setSerialDataToTransmitterRawData: No Active Sensor, Data only stored in Transmitter Data");
            return;
        }

        if (use_transmiter_pl_bluetooth && (transmitterData.raw_data == 100000)) {
            Log.wtf(TAG, "Ignoring probably erroneous Transmiter_PL data: " + transmitterData.raw_data);
            return;
        }



        //sensor.latest_battery_level = (sensor.latest_battery_level != 0) ? Math.min(sensor.latest_battery_level, transmitterData.sensor_battery_level) : transmitterData.sensor_battery_level;
        sensor.latest_battery_level = transmitterData.sensor_battery_level; // allow level to go up and down
        sensor.save();

        last_transmitter_Data = transmitterData;
        Log.d(TAG, "BgReading.create: new BG reading at " + timestamp + " with a timestamp of " + transmitterData.timestamp);
        BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, this, transmitterData.timestamp);
    }

    private synchronized boolean areWeBonded(String hunt_address) {
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "mBluetoothAdapter is null");
            return true; // failsafe
        }
        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
            for (BluetoothDevice device : pairedDevices) {
                final String address = device.getAddress();

                if (address != null) {
                    if (hunt_address.equals(address)) {
                        Log.d(TAG, hunt_address + " is bonded");
                        bondedState = hunt_address;
                        return true;
                    }
                }
            }
        }
        Log.d(TAG, hunt_address + " is not bonded");
        bondedState = "";
        return false;
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

    public void waitFor(final int millis) {
        synchronized (mLock) {
            try {
                UserError.Log.d(TAG, "waiting " + millis + "ms");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                UserError.Log.e(TAG, "Sleeping interrupted", e);
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

    public static final String LIMITTER_NAME = "LimiTTer";
    public static String getBestLimitterHardwareName() {
        if (static_use_nrf) {
            return "BlueReader";
        } else if (static_use_blukon) {
            return xdrip.getAppContext().getString(R.string.blukon);
        } else if (static_use_transmiter_pl_bluetooth) {
            return "Transmiter PL";
        } else if (static_use_rfduino_bluetooth) {
            return "Rfduino";
        } else return LIMITTER_NAME;
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {
        final List<StatusItem> l = new ArrayList<>();

        final boolean forced_wear = Home.get_forced_wear();

        l.add(new StatusItem("Phone Service State", lastState + (forced_wear ? " (Watch Forced)" : "")));
        l.add(new StatusItem("Bluetooth Device", JoH.ucFirst(getStateStr(mStaticState))));

        if (static_use_polling) {
            l.add(new StatusItem("Polling mode", ((last_poll_sent > 0) ? "Last poll: " + JoH.niceTimeSince(last_poll_sent) + " ago" : "Enabled")));
        }

        if (static_use_transmiter_pl_bluetooth) {
            l.add(new StatusItem("Hardware", "Transmiter PL"));
        }

        if (static_use_rfduino_bluetooth) {
            l.add(new StatusItem("Hardware", "Rfduino"));
        }

        if (static_use_blukon) {
            l.add(new StatusItem("Hardware", xdrip.getAppContext().getString(R.string.blukon)));
        }

        if (static_use_nrf) {
            l.add(new StatusItem("Hardware", "BlueReader"));
        }


        // TODO add LimiTTer info

        if (last_transmitter_Data != null) {
            l.add(new StatusItem("Glucose data from", JoH.niceTimeSince(last_transmitter_Data.timestamp) + " ago"));
        }
        if (last_battery_level > -1) {
            l.add(new StatusItem("Battery level", last_battery_level));
        }

        if (Pref.getBooleanDefaultFalse(PREF_DEX_COLLECTION_BONDING)) {
            if (bondedState != null) {
                l.add(new StatusItem("Bluetooth Pairing", (bondedState.length() > 0) ? "Bonded" : "Not bonded" + (bondingTries > 1 ? " (" + bondingTries + ")" : ""), (bondedState.length() > 0) ? StatusItem.Highlight.GOOD : StatusItem.Highlight.NOTICE, "long-press",
                        new Runnable() {
                            @Override
                            public void run() {
                                Pref.setBoolean(PREF_DEX_COLLECTION_BONDING, false);
                                if (bondedState.length() > 0) {
                                    JoH.static_toast_long("If you want to unbond use Android bluetooth system settings to Forget device");
                                    bondedState = null;
                                }
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
                                    }
                                }
                                ).start();
                            }
                        }));
            }
        } else {
            l.add(new StatusItem("Bluetooth Pairing", "Disabled, tap to enable", StatusItem.Highlight.NORMAL, "long-press",
                    new Runnable() {
                        @Override
                        public void run() {
                            Pref.setBoolean(PREF_DEX_COLLECTION_BONDING, true);
                            JoH.static_toast_long("This probably only works on HM10/HM11 devices at the moment and takes a minute");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
                                }
                            }
                            ).start();
                        }
                    }));
        }
        if (max_wakeup_jitter > 2000) {
            l.add(new StatusItem("Slowest wake up", JoH.niceTimeScalar(max_wakeup_jitter) + " late", max_wakeup_jitter > 61000 ? StatusItem.Highlight.CRITICAL : StatusItem.Highlight.NORMAL));
        }
        if (JoH.buggy_samsung) {
            l.add(new StatusItem("Buggy Samsung", "Using workaround", max_wakeup_jitter < TOLERABLE_JITTER ? StatusItem.Highlight.GOOD : StatusItem.Highlight.BAD));
        }
        if (retry_time > 0) l.add(new StatusItem("Next Retry", JoH.niceTimeTill(retry_time), JoH.msTill(retry_time)< -2 ? StatusItem.Highlight.CRITICAL : StatusItem.Highlight.NORMAL));
        if (failover_time > 0)
            l.add(new StatusItem("Next Wake up", JoH.niceTimeTill(failover_time), JoH.msTill(failover_time) < -2 ? StatusItem.Highlight.CRITICAL : StatusItem.Highlight.NORMAL));

        if (Home.get_engineering_mode() && DexCollectionType.hasLibre()) {
            l.add(new StatusItem("Request Data", "Test for xBridgePlus protocol", immediateSend == null ? StatusItem.Highlight.NORMAL : StatusItem.Highlight.NOTICE, "long-press", new Runnable() {
                @Override
                public void run() {
                    immediateSend = XbridgePlus.sendDataRequestPacket();
                    CollectionServiceStarter.restartCollectionService(xdrip.getAppContext()); // TODO quicker/cleaner restart
                }
            }));
        }

        if (Home.get_engineering_mode() && (static_last_hexdump != null)) {
            l.add(new StatusItem("Received Data", filterHexdump(static_last_hexdump)));
        }
        if (Home.get_engineering_mode() && (static_last_sent_hexdump != null)) {
            l.add(new StatusItem("Sent Data", filterHexdump(static_last_sent_hexdump)));
        }

        //WATCH
        if (forced_wear) {
            l.add(new StatusItem());
            l.add(new StatusItem("Watch Service State", lastStateWatch));
            l.add(new StatusItem("Bridge Device", JoH.ucFirst(getStateStr(mStaticStateWatch))));

            // TODO add LimiTTer info

            if ((last_transmitter_DataWatch != null) && (last_transmitter_DataWatch.timestamp > 0)) {
                l.add(new StatusItem("Watch Glucose data", JoH.niceTimeSince(last_transmitter_DataWatch.timestamp) + " ago"));
            }
            if (last_battery_level_watch > -1) {
                l.add(new StatusItem("Bridge Battery level", last_battery_level_watch));
            }

            if (retry_time_watch > 0) l.add(new StatusItem("Watch Next Retry", JoH.niceTimeTill(retry_time_watch)));
            if (failover_time_watch > 0)
                l.add(new StatusItem("Watch Wake up", JoH.niceTimeTill(failover_time_watch)));

            if (Home.get_engineering_mode() && (static_last_hexdump_watch != null) && (static_last_hexdump_watch.length()>0)) {
                l.add(new StatusItem("Watch Received Data", filterHexdump(static_last_hexdump_watch)));
            }
            if (Home.get_engineering_mode() && (static_last_sent_hexdump_watch != null) && (static_last_sent_hexdump_watch.length()>0)) {
                l.add(new StatusItem("Watch Sent Data", filterHexdump(static_last_sent_hexdump_watch)));
            }
        }

        // blueReader
        if (blueReader.isblueReader()) {
            l.add(new StatusItem("blueReader Battery", Pref.getInt("bridge_battery", 0) + "%"));
            l.add(new StatusItem("blueReader rest days", PersistentStore.getString("bridge_battery_days")));
            l.add(new StatusItem("blueReader Firmware",  PersistentStore.getString("blueReaderFirmware")));
        }

        return l;
    }
    private static String filterHexdump(String hex) {
        return hex.replaceAll("[ ]+"," ").replaceAll("\n0x0000[0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f][0-9A-Fa-f] ","\n").replaceFirst("^\n","");
    }
}
