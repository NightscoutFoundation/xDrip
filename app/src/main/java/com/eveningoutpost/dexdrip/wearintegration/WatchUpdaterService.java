package com.eveningoutpost.dexdrip.wearintegration;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.G5Model.CalibrationState;
import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.LowPriorityThread;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusLine;
import com.eveningoutpost.dexdrip.UtilityModels.WearSyncBooleans;
import com.eveningoutpost.dexdrip.UtilityModels.WearSyncPersistentStrings;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.GetWearApk;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine.PREF_QUEUE_DRAINED;
import static com.eveningoutpost.dexdrip.Models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.Models.JoH.ts;

@SuppressLint("LogNotTimber")
public class WatchUpdaterService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_RESEND = WatchUpdaterService.class.getName().concat(".Resend");
    public static final String ACTION_OPEN_SETTINGS = WatchUpdaterService.class.getName().concat(".OpenSettings");
    public static final String ACTION_SYNC_DB = WatchUpdaterService.class.getName().concat(".SyncDB");//KS
    public static final String ACTION_RESET_DB = WatchUpdaterService.class.getName().concat(".ResetDB");//KS
    public static final String ACTION_SYNC_LOGS = WatchUpdaterService.class.getName().concat(".SyncLogs");//KS
    public static final String ACTION_CLEAR_LOGS = WatchUpdaterService.class.getName().concat(".ClearLogs");//KS
    public static final String ACTION_STATUS_COLLECTOR = WatchUpdaterService.class.getName().concat(".StatusCollector");//KS
    public static final String ACTION_START_COLLECTOR = WatchUpdaterService.class.getName().concat(".StartCollector");//KS
    public static final String ACTION_SYNC_SENSOR = WatchUpdaterService.class.getName().concat(".SyncSensor");//KS
    public static final String ACTION_SYNC_CALIBRATION = WatchUpdaterService.class.getName().concat(".SyncCalibration");//KS
    public static final String ACTION_SEND_TOAST = WatchUpdaterService.class.getName().concat(".SendWearLocalToast");//KS
    public static final String ACTION_SEND_STATUS = WatchUpdaterService.class.getName().concat(".SendStatus");//KS
    public static final String ACTION_SYNC_ACTIVEBTDEVICE = WatchUpdaterService.class.getName().concat(".SyncActiveBtDevice");//KS
    public static final String ACTION_SYNC_ALERTTYPE = WatchUpdaterService.class.getName().concat(".SyncAlertType");
    public final static String ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_COLLECTION_SERVICE_UPDATE";
    private static final String ACTION_SEND_G5_QUEUE = WatchUpdaterService.class.getName().concat(".SendG5Queue");
    public static final String ACTION_DISABLE_FORCE_WEAR = WatchUpdaterService.class.getName().concat(".DisableForceWear");//KS
    public static final String ACTION_SNOOZE_ALERT = WatchUpdaterService.class.getName().concat(".SnoozeAlert");//KS
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String NEW_STATUS_PATH = "/sendstatustowear";
    private static final String SYNC_DB_PATH = "/xdrip_plus_syncweardb";//KS
    private static final String RESET_DB_PATH = "/xdrip_plus_resetweardb";//KS
    private static final String SYNC_BGS_PATH = "/xdrip_plus_syncwearbgs";//KS
    private static final String SYNC_BGS_PRECALCULATED_PATH = "/xdrip_plus_syncwearbgs2";
    private static final String SYNC_LOGS_PATH = "/xdrip_plus_syncwearlogs";
    private static final String SYNC_TREATMENTS_PATH = "/xdrip_plus_syncweartreatments";
    private static final String SYNC_LOGS_REQUESTED_PATH = "/xdrip_plus_syncwearlogsrequested";
    private static final String SYNC_STEP_SENSOR_PATH = "/xdrip_plus_syncwearstepsensor";
    private static final String SYNC_HEART_SENSOR_PATH = "/xdrip_plus_syncwearheartsensor";
    private static final String CLEAR_LOGS_PATH = "/xdrip_plus_clearwearlogs";
    private static final String CLEAR_TREATMENTS_PATH = "/xdrip_plus_clearweartreatments";
    private static final String STATUS_COLLECTOR_PATH = "/xdrip_plus_statuscollector";
    private static final String START_COLLECTOR_PATH = "/xdrip_plus_startcollector";
    private static final String WEARABLE_REPLYMSG_PATH = "/xdrip_plus_watch_data_replymsg";
    private static final String WEARABLE_INITDB_PATH = "/xdrip_plus_watch_data_initdb";
    public static final String WEARABLE_INITTREATMENTS_PATH = "/xdrip_plus_watch_data_inittreatments";
    private static final String WEARABLE_TREATMENTS_DATA_PATH = "/xdrip_plus_watch_treatments_data";//KS
    private static final String WEARABLE_BLOODTEST_DATA_PATH = "/xdrip_plus_watch_bloodtest_data";//KS
    private static final String WEARABLE_INITPREFS_PATH = "/xdrip_plus_watch_data_initprefs";
    private static final String WEARABLE_LOCALE_CHANGED_PATH = "/xdrip_plus_locale_changed_data";//KS
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/xdrip_plus_watch_cal_data";//KS
    private static final String WEARABLE_BG_DATA_PATH = "/xdrip_plus_watch_bg_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/xdrip_plus_watch_sensor_data";//KS
    private static final String WEARABLE_PREF_DATA_PATH = "/xdrip_plus_watch_pref_data";//KS
    private static final String WEARABLE_ACTIVEBTDEVICE_DATA_PATH = "/xdrip_plus_watch_activebtdevice_data";//KS
    private static final String WEARABLE_ALERTTYPE_DATA_PATH = "/xdrip_plus_watch_alerttype_data";//KS
    private static final String DATA_ITEM_RECEIVED_PATH = "/xdrip_plus_data-item-received";//KS
    private static final String WEARABLE_SNOOZE_ALERT = "/xdrip_plus_snooze_payload";
    public static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";
    public static final String WEARABLE_APPROVE_TREATMENT = "/xdrip_plus_approve_treatment";
    public static final String WEARABLE_CANCEL_TREATMENT = "/xdrip_plus_cancel_treatment";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String WEARABLE_TOAST_LOCAL_NOTIFICATON = "/xdrip_plus_local_toast";
    private static final String WEARABLE_REQUEST_APK = "/xdrip_plus_can_i_has_apk";
    private static final String WEARABLE_APK_DELIVERY = "/xdrip_plus_here_is_apk";
    private static final String WEARABLE_G5_QUEUE_PATH = "/xdrip_plus_watch_g5_queue";
    public static final String WEARABLE_G5BATTERY_PAYLOAD = "/xdrip_plus_battery_payload";
    private static final String CAPABILITY_WEAR_APP = "wear_app_sync_bgs";
    private static final String LAST_RECORD_TIMESTAMP = "wear-sync-last-treatment-record-ts";
    private static String localnode = "";
    private volatile String mWearNodeId = null;
    static final int GET_CAPABILITIES_TIMEOUT_MS = 5000;

    private static final String TAG = "jamorham watchupdater";
    private static final String LAST_WATCH_RECEIVED_TEXT = "watch-last-received-text";
    private static GoogleApiClient googleApiClient;
    private static long lastRequest = 0;//KS
    private static final Integer sendTreatmentsCount = 60;//KS
    private static final Integer sendCalibrationCount = 3;//KS
    private final static Integer sendBgCount = 4;//KS
    private boolean wear_integration = false;
    private boolean pebble_integration = false;
    private boolean is_using_bt = false;
    private static long syncLogsRequested = 0;//KS
    private static byte[] apkBytes;
    private SharedPreferences mPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

    public synchronized static void receivedText(Context context, String text) {
        if (text.equals(PersistentStore.getString(LAST_WATCH_RECEIVED_TEXT))) {
            Log.e(TAG, "Received text is same as previous, ignoring: " + text);
            return;
        }
        PersistentStore.setString(LAST_WATCH_RECEIVED_TEXT, text);
        startHomeWithExtra(context, WEARABLE_VOICE_PAYLOAD, text);
    }

    private static void approveTreatment(Context context, String text) {
        startHomeWithExtra(context, WEARABLE_APPROVE_TREATMENT, text);
    }

    private static void cancelTreatment(Context context, String text) {
        startHomeWithExtra(context, WEARABLE_CANCEL_TREATMENT, text);
    }

    private static void startHomeWithExtra(Context context, String extra, String text) {
        Intent intent = new Intent(context, Home.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(extra, text);
        context.startActivity(intent);
    }

    private void sendDataReceived(String path, String notification, long timeOfLastEntry, String type, long watch_syncLogsRequested) {//KS
        Log.d(TAG, "sendDataReceived timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry) + " Path=" + path);
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putLong("timeOfLastEntry", timeOfLastEntry);
            dataMapRequest.getDataMap().putLong("syncLogsRequested", watch_syncLogsRequested);
            dataMapRequest.getDataMap().putString("type", type);
            dataMapRequest.getDataMap().putString("msg", notification);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "sendDataReceived No connection to wearable available!");
        }
    }

    private void syncFieldData(DataMap dataMap) {
        String dex_txid = dataMap.getString("dex_txid", "");
        byte[] G5_BATTERY_MARKER = dataMap.getByteArray(G5CollectionService.G5_BATTERY_MARKER);
        byte[] G5_FIRMWARE_MARKER = dataMap.getByteArray(G5CollectionService.G5_FIRMWARE_MARKER);
        if (dex_txid != null && dex_txid.equals(mPrefs.getString("dex_txid", "default"))) {
            if (G5_BATTERY_MARKER != null) {
                long watch_last_battery_query = dataMap.getLong(G5CollectionService.G5_BATTERY_FROM_MARKER);
                long phone_last_battery_query = PersistentStore.getLong(G5CollectionService.G5_BATTERY_FROM_MARKER + dex_txid);
                if (watch_last_battery_query > phone_last_battery_query) {
                    G5CollectionService.setStoredBatteryBytes(dex_txid, G5_BATTERY_MARKER);
                    PersistentStore.setLong(G5CollectionService.G5_BATTERY_FROM_MARKER + dex_txid, watch_last_battery_query);
                    G5CollectionService.getBatteryStatusNow = false;
                    Ob1G5CollectionService.getBatteryStatusNow = false;
                }
            }
            if (G5_FIRMWARE_MARKER != null) {
                G5CollectionService.setStoredFirmwareBytes(dex_txid, G5_FIRMWARE_MARKER);
            }
        }
    }

    public static void checkOb1Queue() {
        boolean enable_wearG5 = Pref.getBoolean("enable_wearG5", false);
        if (enable_wearG5) {
            if (Ob1G5CollectionService.usingNativeMode()) {
                final String ob1QueueJson = Ob1G5StateMachine.extractQueueJson();
                if (ob1QueueJson != null) {
                    xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_SEND_G5_QUEUE).putExtra("queueData", ob1QueueJson));
                }
            }
        }
    }

    private void syncPrefData(DataMap dataMap) {
        boolean enable_wearG5 = dataMap.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = dataMap.getBoolean("force_wearG5", false);
        String node_wearG5 = dataMap.getString("node_wearG5", "");
        String dex_txid = dataMap.getString("dex_txid", "");
        int bridge_battery = dataMap.getInt("bridge_battery", -1);//Used in DexCollectionService
        int nfc_sensor_age = dataMap.getInt("nfc_sensor_age", -1);//Used in DexCollectionService LimiTTer
        boolean bg_notifications_watch = dataMap.getBoolean("bg_notifications_watch", false);
        boolean persistent_high_alert_enabled_watch = dataMap.getBoolean("persistent_high_alert_enabled_watch", false);
        boolean show_wear_treatments = dataMap.getBoolean("show_wear_treatments", false);

        boolean change = false;

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        Log.d(TAG, "syncPrefData enable_wearG5: " + enable_wearG5 + " force_wearG5: " + force_wearG5 + " node_wearG5:" + node_wearG5 + " dex_txid: " + dex_txid);

        if (bridge_battery != mPrefs.getInt("bridge_battery", -1)) {//Used by DexCollectionService
            prefs.putInt("bridge_battery", bridge_battery);
            prefs.apply();
            Log.d(TAG, "syncPrefData commit bridge_battery: " + bridge_battery);
            CheckBridgeBattery.checkBridgeBattery();
            if (force_wearG5 && CheckBridgeBattery.checkForceWearBridgeBattery()) {
                force_wearG5 = false;
                change = true;
                Log.d(TAG, "syncPrefData disable_wearG5_on_lowbattery=true; switch force_wearG5:" + force_wearG5);
                String msg = getResources().getString(R.string.notify_when_wear_low_battery);
                JoH.static_toast_long(msg);
                sendWearLocalToast(msg, Toast.LENGTH_LONG);
            }
        }

        if (!node_wearG5.equals(mPrefs.getString("node_wearG5", ""))) {
            change = true;
            prefs.putString("node_wearG5", node_wearG5);
            Log.d(TAG, "syncPrefData node_wearG5:" + node_wearG5);
        }

        if (/*force_wearG5 &&*/ force_wearG5 != mPrefs.getBoolean("force_wearG5", false)) {
            change = true;
            prefs.putBoolean("force_wearG5", force_wearG5);
            Log.d(TAG, "syncPrefData commit force_wearG5:" + force_wearG5);
            if (force_wearG5) {
                final PendingIntent pendingIntent = android.app.PendingIntent.getActivity(xdrip.getAppContext(), 0, new Intent(xdrip.getAppContext(), Home.class), android.app.PendingIntent.FLAG_UPDATE_CURRENT);
                showNotification("Force Wear Enabled", node_wearG5 + " Watch has enabled Force Wear Collection Service", pendingIntent, 771, true, true, true);
            }
        }

        if (nfc_sensor_age != mPrefs.getInt("nfc_sensor_age", 0)) {//Used by DexCollectionService
            change = true;
            prefs.putInt("nfc_sensor_age", nfc_sensor_age);
            Log.d(TAG, "syncPrefData commit nfc_sensor_age: " + nfc_sensor_age);
        }

        if (bg_notifications_watch != mPrefs.getBoolean("bg_notifications_watch", false)) {
            change = true;
            prefs.putBoolean("bg_notifications_watch", bg_notifications_watch);
            Log.d(TAG, "syncPrefData commit bg_notifications_watch: " + bg_notifications_watch);
        }
        PersistentStore.setBoolean("bg_notifications_watch", bg_notifications_watch);
        PersistentStore.setBoolean("persistent_high_alert_enabled_watch", persistent_high_alert_enabled_watch);

        if (/*enable_wearG5 &&*/ enable_wearG5 != mPrefs.getBoolean("enable_wearG5", false)) {
            change = true;
            prefs.putBoolean("enable_wearG5", enable_wearG5);
            Log.d(TAG, "syncPrefData commit enable_wearG5: " + enable_wearG5);
        }

        if (show_wear_treatments != mPrefs.getBoolean("show_wear_treatments", false)) {
            change = true;
            prefs.putBoolean("show_wear_treatments", show_wear_treatments);
            Log.d(TAG, "syncPrefData show_wear_treatments:" + show_wear_treatments);
        }

        if (change) {
            prefs.apply();
        } else if (!dex_txid.equals(mPrefs.getString("dex_txid", "default"))) {
            sendPrefSettings();
            processConnect();
        }
    }

    //Assumes Wear is connected to phone
    private void processConnect() {//KS
        Log.d(TAG, "processConnect enter");
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);

        if (wear_integration) {
            initWearData();
            if (enable_wearG5) {
                if (force_wearG5) {
                    Log.d(TAG, "processConnect force_wearG5=true - stopBtService");
                    stopBtService();
                } else {
                    Log.d(TAG, "processConnect force_wearG5=false - startBtService");
                    startBtService();
                }
            } else {
                Log.d(TAG, "processConnect enable_wearG5=false - startBtService");
                startBtService();
                if (mPrefs.getBoolean("show_wear_treatments", false))
                    initWearTreatments();
            }
        } else {
            Log.d(TAG, "processConnect wear_integration=false - startBtService");
            startBtService();
        }
    }

    private synchronized void syncBgReadingsData(DataMap dataMap) {
        Log.d(TAG, "sync-precalculated-bg-readings-Data");

        final int calibration_state = dataMap.getInt("native_calibration_state", 0);
        Ob1G5CollectionService.processCalibrationState(CalibrationState.parse(calibration_state));
        Ob1G5StateMachine.injectDexTime(dataMap.getString("dextime", null));

        final boolean queue_drained = dataMap.getBoolean(PREF_QUEUE_DRAINED);
        if (queue_drained) {
            Ob1G5StateMachine.emptyQueue();
        }


        final ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {
            final Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();


            final int count = entries.size();

            if (count > 0) {

                final Sensor current_sensor = Sensor.currentSensor();
                if (current_sensor == null) {
                    UserError.Log.e(TAG, "Cannot sync wear BG readings because sensor is marked stopped on phone");
                    return;
                }

                Log.d(TAG, "syncTransmitterData add BgReading Table entries count=" + count);
                int idx = 0;
                long timeOfLastBG = 0;
                for (DataMap entry : entries) {
                    if (entry != null) {
                        idx++;
                        final String bgrecord = entry.getString("bgs");
                        if (bgrecord != null) {

                            final BgReading bgData = gson.fromJson(bgrecord, BgReading.class);

                            final BgReading uuidexists = BgReading.findByUuid(bgData.uuid);
                            if (uuidexists == null) {

                                final BgReading exists = BgReading.getForTimestamp(bgData.timestamp);
                                if (exists == null) {
                                    Log.d(TAG, "Saving new synced pre-calculated bg-reading: " + JoH.dateTimeText(bgData.timestamp) + " last entry: " + (idx == count) + " " + BgGraphBuilder.unitized_string_static(bgData.calculated_value));
                                    bgData.sensor = current_sensor;
                                    bgData.save();
                                    BgSendQueue.handleNewBgReading(bgData, "create", xdrip.getAppContext(), Home.get_follower(), idx != count);
                                } else {
                                    Log.d(TAG, "BgReading for timestamp already exists: " + JoH.dateTimeText(bgData.timestamp));
                                }
                            } else {
                                Log.d(TAG, "BgReading with uuid: " + bgData.uuid + " already exists: " + JoH.dateTimeText(bgData.timestamp));
                            }

                            timeOfLastBG = Math.max(bgData.timestamp + 1, timeOfLastBG);
                        }
                    }
                }
                sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_BGS count=" + entries.size(), timeOfLastBG, "BG", -1);

            } else {
                UserError.Log.e(TAG, "Not acknowledging wear BG readings as count was 0");
            }
        } else {
            UserError.Log.d(TAG, "Null entries list - should only happen with native status update only");
        }
    }

    private synchronized void syncTransmitterData(DataMap dataMap, boolean bBenchmark) {//KS
        Log.d(TAG, "syncTransmitterData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastBG = 0;
        Log.d(TAG, "syncTransmitterData add BgReading Table");
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            int idx = 0;
            int count = entries.size();
            Log.d(TAG, "syncTransmitterData add BgReading Table entries count=" + count);
            for (DataMap entry : entries) {
                if (entry != null) {
                    //Log.d(TAG, "syncTransmitterData add BgReading Table entry=" + entry);
                    idx++;
                    String bgrecord = entry.getString("bgs");
                    if (bgrecord != null) {//for (TransmitterData bgData : bgs) {
                        //Log.d(TAG, "syncTransmitterData add TransmitterData Table bgrecord=" + bgrecord);
                        TransmitterData bgData = gson.fromJson(bgrecord, TransmitterData.class);
                        //TransmitterData bgData = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create().fromJson(bgrecord, TransmitterData.class);
                        TransmitterData exists = TransmitterData.getForTimestamp(bgData.timestamp);
                        TransmitterData uuidexists = TransmitterData.findByUuid(bgData.uuid);
                        timeOfLastBG = bgData.timestamp + 1;
                        if (exists != null || uuidexists != null) {
                            Log.d(TAG, "syncTransmitterData BG already exists for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp) + " raw_data=" + bgData.raw_data);
                        } else {
                            Log.d(TAG, "syncTransmitterData add BG; does NOT exist for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp) + " raw_data=" + bgData.raw_data);
                            if (!bBenchmark) {
                                bgData.save();

                                //Check
                                if (TransmitterData.findByUuid(bgData.uuid) != null)
                                    Log.d(TAG, "syncTransmitterData: TransmitterData was saved for uuid:" + bgData.uuid);
                                else {
                                    Log.e(TAG, "syncTransmitterData: TransmitterData was NOT saved for uuid:" + bgData.uuid);
                                    return;
                                }

                                //KS the following is from G5CollectionService processNewTransmitterData()
                                Sensor sensor = Sensor.currentSensor();
                                if (sensor == null) {
                                    Log.e(TAG, "syncTransmitterData: No Active Sensor, Data only stored in Transmitter Data");
                                    return;
                                }
                                //TODO : LOG if unfiltered or filtered values are zero
                                Sensor.updateBatteryLevel(sensor, bgData.sensor_battery_level);
                                Log.i(TAG, "syncTransmitterData: BG timestamp create " + Long.toString(bgData.timestamp));//android.util.Log.i
                                BgReading bgExists;

                                //KS TODO wear implements limited alerts, therefore continue to process all alerts on phone for last entry
                                if (count > 1 && idx < count) {
                                    bgExists = BgReading.create(bgData.raw_data, bgData.filtered_data, this, bgData.timestamp, true);//Disable Notifications for bulk insert
                                } else {
                                    bgExists = BgReading.create(bgData.raw_data, bgData.filtered_data, this, bgData.timestamp);
                                }
                                if (bgExists != null)
                                    Log.d(TAG, "syncTransmitterData BG GSON saved BG: " + bgExists.toS());
                                else
                                    Log.e(TAG, "syncTransmitterData BG GSON NOT saved");
                            }
                        }
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_BGS count=" + entries.size(), timeOfLastBG, bBenchmark ? "BM" : "BG", -1);
        }
    }

    private synchronized void syncLogData(DataMap dataMap, boolean bBenchmark) {//KS
        Log.d(TAG, "syncLogData");
        long watch_syncLogsRequested = dataMap.getLong("syncLogsRequested", -1);
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastEntry = 0;
        int saved = 0;
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncLogData add Table entries count=" + entries.size() + " watch_syncLogsRequested=" + watch_syncLogsRequested);
            for (DataMap entry : entries) {
                if (entry != null) {
                    String record = entry.getString("entry");
                    if (record != null) {
                        UserError data = gson.fromJson(record, UserError.class);
                        if (data != null) {
                            timeOfLastEntry = (long) data.timestamp + 1;
                            if (data.shortError != null && !data.shortError.isEmpty()) { //add wear prefix
                                if (!data.shortError.startsWith("wear")) {
                                    data.shortError = mPrefs.getString("wear_logs_prefix", "wear") + data.shortError;
                                }
                            }
                            UserError exists = UserError.getForTimestamp(data);
                            if (exists == null && !bBenchmark) {
                                data.save();
                                saved++;
                            } else {
                                //Log.d(TAG, "syncLogData Log entry already exists with shortError=" + data.shortError + " timestamp=" + JoH.dateTimeText((long)data.timestamp));
                            }
                        }
                    }
                }
            }
            if (saved > 0) {
                Log.d(TAG, "syncLogData Saved timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry) + " saved=" + saved);
            } else {
                Log.d(TAG, "syncLogData No records saved due to being duplicates! timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry) + " count=" + entries.size());
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_LOGS count=" + entries.size(), timeOfLastEntry, bBenchmark ? "BM" : "LOG", watch_syncLogsRequested);
        }
    }

    private synchronized void syncStepSensorData(DataMap dataMap, boolean bBenchmark) {
        Log.d(TAG, "syncStepSensorData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastEntry = 0;
        Log.d(TAG, "syncStepSensorData add to Table");
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            StepCounter pm = StepCounter.last();
            Log.d(TAG, "syncStepSensorData add Table entries count=" + entries.size());
            for (DataMap entry : entries) {
                if (entry != null) {
                    Log.d(TAG, "syncStepSensorData add Table entry=" + entry);
                    String record = entry.getString("entry");
                    if (record != null) {
                        Log.d(TAG, "syncStepSensorData add Table record=" + record);
                        StepCounter data = gson.fromJson(record, StepCounter.class);
                        if (data != null) {
                            timeOfLastEntry = (long) data.timestamp + 1;
                            Log.d(TAG, "syncStepSensorData add Entry Wear=" + data.toString());
                            Log.d(TAG, "syncStepSensorData WATCH data.metric=" + data.metric + " timestamp=" + JoH.dateTimeText((long) data.timestamp));
                            if (!bBenchmark)
                                data.saveit();
                        }
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_LOGS count=" + entries.size(), timeOfLastEntry, bBenchmark ? "BM" : "STEP", -1);
        }
    }

    private synchronized void syncHeartSensorData(DataMap dataMap, boolean bBenchmark) {
        Log.d(TAG, "syncHeartSensorData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastEntry = 0;
        Log.d(TAG, "syncHeartSensorData add to Table");
        if (entries != null) {

            final Gson gson = JoH.defaultGsonInstance();

            //final HeartRate pm = HeartRate.last();
            Log.d(TAG, "syncHeartSensorData add Table entries count=" + entries.size());
            for (DataMap entry : entries) {
                if (entry != null) {
                    Log.d(TAG, "syncHeartSensorData add Table entry=" + entry);
                    String record = entry.getString("entry");
                    if (record != null) {
                        Log.d(TAG, "syncHeartSensorData add Table record=" + record);
                        final HeartRate data = gson.fromJson(record, HeartRate.class);
                        if (data != null) {
                            timeOfLastEntry = (long) data.timestamp + 1;
                            Log.d(TAG, "syncHeartSensorData add Entry Wear=" + data.toString() + " " + record);
                            Log.d(TAG, "syncHeartSensorData WATCH data.metric=" + data.bpm + " timestamp=" + JoH.dateTimeText((long) data.timestamp));
                            if (!bBenchmark)
                                data.saveit();
                        }
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_LOGS count=" + entries.size(), timeOfLastEntry, bBenchmark ? "BM" : "HEART", -1);
        }
    }


    private synchronized void syncTreatmentsData(DataMap dataMap, boolean bBenchmark) {
        Log.d(TAG, "syncTreatmentsData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        long timeOfLastEntry = 0;
        if (entries != null) {
            Log.d(TAG, "syncTreatmentsData count=" + entries.size());
            for (DataMap entry : entries) {
                if (entry != null) {
                    Log.d(TAG, "syncTreatmentsData entry=" + entry);
                    String record = entry.getString("entry");
                    if (record != null && record.length() > 1) {
                        Log.d(TAG, "Received wearable 2: voice payload: " + record);
                        long timestamp = entry.getLong("timestamp");
                        if (timestamp <= PersistentStore.getLong(LAST_RECORD_TIMESTAMP)) {
                            Log.e(TAG, "Ignoring repeated or older sync timestamp");
                            continue;
                        }
                        final long since = JoH.msSince(timestamp);
                        if ((since < -(Constants.SECOND_IN_MS * 5)) || (since > Constants.HOUR_IN_MS * 72)) {
                            JoH.static_toast_long("Rejecting wear treatment as time out of range!");
                            UserError.Log.e(TAG, "Rejecting wear treatment due to time: " + record + " since: " + since);
                        } else {
                            if (record.contains("uuid null")) {
                                Log.e(TAG, "Skipping xx uuid null record!");
                                continue;
                            }
                            receivedText(getApplicationContext(), record);
                            PersistentStore.setLong(LAST_RECORD_TIMESTAMP, timestamp);
                        }
                        Log.d(TAG, "syncTreatmentsData add Table record=" + record);
                        timeOfLastEntry = (long) timestamp + 1;
                        Log.d(TAG, "syncTreatmentsData WATCH treatments timestamp=" + JoH.dateTimeText(timestamp));
                    }
                }
            }
            sendDataReceived(DATA_ITEM_RECEIVED_PATH, "DATA_RECEIVED_LOGS count=" + entries.size(), timeOfLastEntry, bBenchmark ? "BM" : "TREATMENTS", -1);
        }
    }

    public static void sendWearToast(String msg, int length) {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TOAST_NOTIFICATON);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putInt("length", length);
            dataMapRequest.getDataMap().putString("msg", msg);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for toast! " + msg);
        }
    }

    public static void sendWearLocalToast(String msg, int length) {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TOAST_LOCAL_NOTIFICATON);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putInt("length", length);
            dataMapRequest.getDataMap().putString("msg", msg);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for toast! " + msg);
        }
    }

    public static void sendTreatment(double carbs, double insulin, double bloodtest, String injectionJSON, double timeoffset, String timestring) {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TREATMENT_PAYLOAD);
            //unique content
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putDouble("carbs", carbs);
            dataMapRequest.getDataMap().putDouble("insulin", insulin);
            dataMapRequest.getDataMap().putDouble("bloodtest", bloodtest);
            dataMapRequest.getDataMap().putDouble("timeoffset", timeoffset);
            dataMapRequest.getDataMap().putString("timestring", timestring);
            dataMapRequest.getDataMap().putString("injectionJSON", injectionJSON);
            dataMapRequest.getDataMap().putBoolean("ismgdl", doMgdl(PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())));
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for send treatment!");
        }
    }

    private static boolean doMgdl(SharedPreferences sPrefs) {
        String unit = sPrefs.getString("units", "mgdl");
        if (unit.compareTo("mgdl") == 0) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        //is_using_g5 = (getDexCollectionType() == DexCollectionType.DexcomG5);
        is_using_bt = DexCollectionType.hasBluetooth();
        if (wear_integration) {
            googleApiConnect();
        }
        setSettings();
        listenForChangeInSettings();
    }

    private void listenForChangeInSettings() {
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                pebble_integration = mPrefs.getBoolean("pebble_sync", false);
                if (key.compareTo("bridge_battery") != 0 && key.compareTo("nfc_sensor_age") != 0 &&
                        key.compareTo("bg_notifications_watch") != 0 && key.compareTo("persistent_high_alert_enabled_watch") != 0) {

                    Log.d(TAG, "Triggering Wear Settings Update due to key=" + key);

                    Inevitable.task("wear-update-settings", 2000, () -> {
                        sendPrefSettings();
                        processConnect();
                    });

                }
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }

    private void setSettings() {
        Log.d(TAG, "setSettings enter");
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        processConnect();
        if (wear_integration) {
            if (googleApiClient == null) googleApiConnect();
            Log.d(TAG, "setSettings wear_sync changed to True.");
            sendPrefSettings();
        }
    }

    private void googleApiConnect() {
        if (googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.disconnect();
        }
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
        if (googleApiClient.isConnected()) {
            Log.d("WatchUpdater", "API client is connected");
        } else {
            googleApiClient.connect();
        }
    }

    private void forceGoogleApiConnect() {
        if ((googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) || googleApiClient == null) {
            try {
                Log.d(TAG, "forceGoogleApiConnect: forcing google api reconnection");
                googleApiConnect();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Log.d(TAG, "forceGoogleApiConnect: exception:" + e);
            }
        }
    }

    @Override
    public void onPeerConnected(com.google.android.gms.wearable.Node peer) {//KS onPeerConnected and onPeerDisconnected deprecated at the same time as BIND_LISTENER

        super.onPeerConnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerConnected peer name & ID: " + name + "|" + id);
        sendPrefSettings();
        if (mPrefs.getBoolean("enable_wearG5", false)) {//watch_integration
            Log.d(TAG, "onPeerConnected call initWearData for node=" + peer.getDisplayName());
            initWearData();
            //Only stop service if Phone will rely on Wear Collection Service
            if (mPrefs.getBoolean("force_wearG5", false)) {
                Log.d(TAG, "onPeerConnected force_wearG5=true Phone stopBtService and continue to use Wear G5 BT Collector");
                stopBtService();
            } else {
                Log.d(TAG, "onPeerConnected onPeerConnected force_wearG5=false Phone startBtService");
                startBtService();
            }
        }
    }

    @Override
    public void onPeerDisconnected(com.google.android.gms.wearable.Node peer) {//KS onPeerConnected and onPeerDisconnected deprecated at the same time as BIND_LISTENER
        super.onPeerDisconnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerDisconnected peer name & ID: " + name + "|" + id);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (sharedPrefs.getBoolean("watch_integration", false)) {
            Log.d(TAG, "onPeerDisconnected watch_integration=true Phone startBtService");
            startBtService();
        }
    }

    // Custom method to determine whether a service is running
    private boolean isServiceRunning(Class<?> serviceClass) {//Class<?> serviceClass
        if (serviceClass != null) {
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            // Loop through the running services
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                //Log.d(TAG, "isServiceRunning: getClassName=" + service.service.getClassName() + " getShortClassName=" + service.service.getShortClassName());
                if (serviceClass.getName().equals(service.service.getClassName())) return true;
            }
        }
        return false;
    }

    private boolean isCollectorRunning() {
        Class<?> serviceClass = DexCollectionType.getCollectorServiceClass();
        if (serviceClass != null) {
            Log.d(TAG, "DexCollectionType.getCollectorServiceClass(): " + serviceClass.getName());
            return isServiceRunning(serviceClass);
        }
        return false;
    }

    private void startBtService() {//KS
        Log.d(TAG, "startBtService");
        is_using_bt = DexCollectionType.hasBluetooth();//(getDexCollectionType() == DexCollectionType.DexcomG5)
        if (is_using_bt) {
            if (!isCollectorRunning()) {
                CollectionServiceStarter.startBtService(getApplicationContext());
                Log.d(TAG, "startBtService startService");
            } else {
                Log.d(TAG, "startBtService collector already running!");
            }
        } else {
            Log.d(TAG, "Not starting any BT Collector service as it is not our data source");
        }
    }

    private void stopBtService() {
        Log.d(TAG, "stopService call stopService");
        CollectionServiceStarter.stopBtService(getApplicationContext());
        Log.d(TAG, "stopBtService should have called onDestroy");
    }

    private void startBtG5Service() {//KS
        Log.d(TAG, "startBtG5Service");
        //is_using_g5 = (getDexCollectionType() == DexCollectionType.DexcomG5);
        is_using_bt = DexCollectionType.hasBluetooth();
        if (is_using_bt) {
            Context myContext = getApplicationContext();
            Log.d(TAG, "startBtG5Service start G5CollectionService");
            myContext.startService(new Intent(myContext, G5CollectionService.class));
            Log.d(TAG, "startBtG5Service AFTER startService G5CollectionService");
        } else {
            Log.d(TAG, "Not starting any G5 service as it is not our data source");
        }
    }

    private void stopBtG5Service() {//KS
        Log.d(TAG, "stopBtG5Service");
        Context myContext = getApplicationContext();
        myContext.stopService(new Intent(myContext, G5CollectionService.class));
    }

    public static void startSelf() {
        Inevitable.task("wear-startself", 2000, () -> {
            if (JoH.ratelimit("start-wear", 5)) {
                startServiceAndResendData(0);
            }
        });
    }

    public static void startServiceAndResendData(long since) {
        UserError.Log.d(TAG, "Requesting to resend data");
        xdrip.getAppContext().startService(new Intent(xdrip.getAppContext(), WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_RESEND).putExtra("resend-since", since));
    }

    public static void startServiceAndResendDataIfNeeded(final long since) {
        if (isEnabled()) {
            if (JoH.ratelimit("wear-resend-data", 60)) {
                startServiceAndResendData(since);
            }
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-onstart", 60000);
        wear_integration = mPrefs.getBoolean("wear_sync", false);

        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (wear_integration) {
            is_using_bt = DexCollectionType.hasBluetooth();//(getDexCollectionType() == DexCollectionType.DexcomG5)
            if (googleApiClient != null) {
                if (googleApiClient.isConnected()) {
                    if (ACTION_RESEND.equals(action)) {
                        resendData(intent.getLongExtra("resend-since", 0));
                    } else if (ACTION_OPEN_SETTINGS.equals(action)) {
                        Log.d(TAG, "onStartCommand Action=ACTION_OPEN_SETTINGS");
                        sendNotification(OPEN_SETTINGS, "openSettings");
                    } else if (ACTION_SEND_TOAST.equals(action)) {
                        Log.d(TAG, "onStartCommand Action=ACTION_SEND_TOAST msg=" + intent.getStringExtra("msg"));
                        sendWearLocalToast(intent.getStringExtra("msg"), Toast.LENGTH_LONG);
                    } else if (ACTION_SEND_STATUS.equals(action)) {//KS added for HAPP
                        //https://github.com/StephenBlackWasAlreadyTaken/xDrip-Experimental
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SEND_STATUS + " externalStatusString=" + intent.getStringExtra("externalStatusString"));
                        sendRequestExtra(NEW_STATUS_PATH, "externalStatusString", intent.getStringExtra("externalStatusString"));
                    } else if (ACTION_SNOOZE_ALERT.equals(action)) {
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SNOOZE_ALERT + " repeatTime=" + intent.getStringExtra("repeatTime"));
                        sendRequestExtra(WEARABLE_SNOOZE_ALERT, "repeatTime", intent.getStringExtra("repeatTime"));
                    } else if (ACTION_SYNC_DB.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_DB + " Path=" + SYNC_DB_PATH);
                        sendNotification(SYNC_DB_PATH, "syncDB");
                        initWearData();
                    } else if (ACTION_RESET_DB.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_RESET_DB + " Path=" + RESET_DB_PATH);
                        sendNotification(RESET_DB_PATH, "resetDB");
                        //TODO Rm!
                        //UserError.cleanup(JoH.tsl());//TODO Rm!
                        //Log.d(TAG, "onStartCommand RESET_DB_PATH cleanup timestamp=" + JoH.dateTimeText(JoH.tsl()));
                        //TODO Rm!
                        initWearData();
                    } else if (ACTION_DISABLE_FORCE_WEAR.equals(action)) {//KS
                        int bg_wear_missed_minutes = readPrefsInt(mPrefs, "disable_wearG5_on_missedreadings_level", 30);
                        Log.d(TAG, "onStartCommand Action=ACTION_DISABLE_FORCE_WEAR");
                        Pref.setBoolean("force_wearG5", false);
                        final String msgDisableWear = getResources().getString(R.string.notify_disable_wearG5_on_missedreadings, bg_wear_missed_minutes);
                        JoH.static_toast_long(msgDisableWear);
                        Log.e(TAG, "wearIsConnected disable force_wearG5:" + Pref.getBooleanDefaultFalse("force_wearG5") + " msg=" + msgDisableWear);
                        sendWearLocalToast(msgDisableWear, Toast.LENGTH_LONG);
                    } else if (ACTION_START_COLLECTOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_START_COLLECTOR + " Path=" + START_COLLECTOR_PATH);
                        sendNotification(START_COLLECTOR_PATH, "startCOLLECTOR");
                    } else if (ACTION_STATUS_COLLECTOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=ACTION_STATUS_COLLECTOR Path=STATUS_COLLECTOR_PATH getBatteryStatusNow=" + intent.getBooleanExtra("getBatteryStatusNow", false));
                        //sendNotification(STATUS_COLLECTOR_PATH, "statusCOLLECTOR");
                        sendRequestExtra(STATUS_COLLECTOR_PATH, "getBatteryStatusNow", intent.getBooleanExtra("getBatteryStatusNow", false));
                    } else if (ACTION_SYNC_LOGS.equals(action)) {
                        //sendNotification(SYNC_LOGS_PATH, "syncLOG");
                        long rate = (syncLogsRequested == 0 ? 2 : syncLogsRequested * 10);//in seconds
                        Log.d(TAG, "onStartCommand Action ACTION_SYNC_LOGS=ACTION_SYNC_LOGS SYNC_LOGS_PATH syncLogsRequested=" + syncLogsRequested);
                        if (JoH.ratelimit("sync-logs-requested", (int) rate)) {
                            syncLogsRequested++;
                            sendRequestExtra(SYNC_LOGS_PATH, "syncLogsRequested", String.valueOf(syncLogsRequested));
                        }
                        Log.d(TAG, "onStartCommand Action ACTION_SYNC_LOGS=ACTION_SYNC_LOGS SYNC_LOGS_PATH syncLogsRequested=" + syncLogsRequested + " ratelimit=" + rate);
                    } else if (ACTION_CLEAR_LOGS.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_CLEAR_LOGS + " Path=" + CLEAR_LOGS_PATH);
                        sendNotification(CLEAR_LOGS_PATH, "clearLOG");
                    } else if (ACTION_SYNC_SENSOR.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_SENSOR + " Path=" + WEARABLE_SENSOR_DATA_PATH);
                        sendSensorData();
                    } else if (ACTION_SYNC_ACTIVEBTDEVICE.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_ACTIVEBTDEVICE + " Path=" + WEARABLE_ACTIVEBTDEVICE_DATA_PATH);
                        sendActiveBtDeviceData();
                    } else if (ACTION_SYNC_ALERTTYPE.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_ALERTTYPE + " Path=" + WEARABLE_ALERTTYPE_DATA_PATH);
                        sendAlertTypeData();
                    } else if (ACTION_SYNC_CALIBRATION.equals(action)) {//KS
                        Log.d(TAG, "onStartCommand Action=" + ACTION_SYNC_CALIBRATION + " Path=" + WEARABLE_CALIBRATION_DATA_PATH);

                        sendWearCalibrationData(sendCalibrationCount);
                        final boolean adjustPast = mPrefs.getBoolean("rewrite_history", false);
                        Log.d(TAG, "onStartCommand adjustRecentBgReadings for rewrite_history=" + adjustPast);
                        sendWearBgData(adjustPast ? 30 : 2);//wear may not have all BGs if force_wearG5=false, so send BGs from phone
                        sendData();//ensure BgReading.Last is displayed on watch
                    } else if (ACTION_SEND_G5_QUEUE.equals(action)) {
                        Log.d(TAG, "onStartCommand Action = " + ACTION_SEND_G5_QUEUE + " PAth= " + WEARABLE_G5_QUEUE_PATH);
                        sendG5QueueData(intent.getStringExtra("queueData"));

                    } else {
                        // default
                        /*if (!mPrefs.getBoolean("force_wearG5", false)//handled by UploaderQueue
                                && mPrefs.getBoolean("enable_wearG5", false)
                                && (is_using_bt)) { //KS only send BGs if using Phone's G5 Collector Server
                            sendWearBgData(1);
                        }*/
                        Log.d(TAG, "onStartCommand Action=" + " Path=" + WEARABLE_DATA_PATH);
                        sendData();//ensure BgReading.Last is displayed on watch
                    }
                } else {
                    googleApiClient.connect();
                }
            } else {
                Log.wtf(TAG, "GoogleAPI client is null!");
            }
        }

        if (pebble_integration) {
            sendData();
        }

        //if ((!wear_integration)&&(!pebble_integration))
        if (!wear_integration)    // only wear sync starts this service, pebble features are not used?
        {
            Log.i(TAG, "Stopping service");
            startBtService();
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }

        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    private void updateWearSyncBgsCapability() {
        CapabilityApi.GetCapabilityResult capabilityResult =
                Wearable.CapabilityApi.getCapability(
                        googleApiClient, CAPABILITY_WEAR_APP,
                        CapabilityApi.FILTER_REACHABLE).await(GET_CAPABILITIES_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        CapabilityInfo nodes;
        if (!capabilityResult.getStatus().isSuccess()) {
            Log.e(TAG, "updateWearSyncBgsCapability Failed to get capabilities, status: " + capabilityResult.getStatus().getStatusMessage());
            nodes = null;
        } else {
            nodes = capabilityResult.getCapability();
        }

        if (nodes != null && nodes.getNodes().size() > 0) {
            Log.d(TAG, "Updating wear sync nodes");
            updateWearSyncBgsCapability(nodes);
        }
    }


    private void updateWearSyncBgsCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        mWearNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void setLocalNodeName() {
        forceGoogleApiConnect();
        NodeApi.GetLocalNodeResult localnodes = Wearable.NodeApi.getLocalNode(googleApiClient).await(60, TimeUnit.SECONDS);
        Node getnode = localnodes.getNode();
        localnode = getnode != null ? getnode.getDisplayName() + "|" + getnode.getId() : "";
        UserError.Log.d(TAG, "setLocalNodeName.  localnode=" + localnode);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected entered");//KS
        /*CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updateWearSyncBgsCapability(capabilityInfo);
                        Log.d(TAG, "onConnected onCapabilityChanged mWearNodeID:" + mWearNodeId);
                        new CheckWearableConnected().execute();
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                googleApiClient,
                capabilityListener,
                CAPABILITY_WEAR_APP);*/
        sendData();
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateWearSyncBgsCapability(capabilityInfo);
        Log.d(TAG, "onConnected onCapabilityChanged mWearNodeID:" + mWearNodeId);
        new CheckWearableConnected().execute();
    }

    private class CheckWearableConnected extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (googleApiClient.isConnected()) {
                if (System.currentTimeMillis() - lastRequest > 20 * 1000) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();
                    //NodeApi.GetConnectedNodesResult nodes =
                    //        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    if (localnode == null || (localnode != null && localnode.isEmpty()))
                        setLocalNodeName();
                    CapabilityApi.GetCapabilityResult capabilityResult =
                            Wearable.CapabilityApi.getCapability(
                                    googleApiClient, CAPABILITY_WEAR_APP,
                                    CapabilityApi.FILTER_REACHABLE).await(GET_CAPABILITIES_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                    CapabilityInfo nodes;
                    if (!capabilityResult.getStatus().isSuccess()) {
                        Log.e(TAG, "doInBackground Failed to get capabilities, status: " + capabilityResult.getStatus().getStatusMessage());
                        nodes = null;
                    } else {
                        nodes = capabilityResult.getCapability();
                    }
                    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
                    boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false);
                    boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false);
                    String node_wearG5 = mPrefs.getString("node_wearG5", "");

                    if (nodes != null && nodes.getNodes().size() > 0) {
                        updateWearSyncBgsCapability(nodes);
                        int count = nodes.getNodes().size();
                        Log.d(TAG, "doInBackground connected.  CapabilityApi.GetCapabilityResult mWearNodeID=" + (mWearNodeId != null ? mWearNodeId : "") + " count=" + count);//KS
                        boolean isConnectedToWearable = false;
                        for (Node peer : nodes.getNodes()) {

                            //onPeerConnected
                            String wearNode = peer.getDisplayName() + "|" + peer.getId();
                            Log.d(TAG, "CheckWearableConnected onPeerConnected peer name & ID: " + wearNode);
                            if (wearNode.equals(node_wearG5)) {
                                isConnectedToWearable = true;
                                sendPrefSettings();
                                break;
                            } else if (node_wearG5.equals("")) {
                                isConnectedToWearable = true;
                                prefs.putString("node_wearG5", wearNode);
                                prefs.apply();
                                break;
                            }

                        }
                        sendPrefSettings();
                        initWearData();
                        if (enable_wearG5) {
                            //Only stop service if Phone will rely on Wear Collection Service
                            if (force_wearG5 && isConnectedToWearable) {
                                Log.d(TAG, "CheckWearableConnected onPeerConnected force_wearG5=true Phone stopBtService and continue to use Wear BT Collector");
                                stopBtService();
                            } else {
                                Log.d(TAG, "CheckWearableConnected onPeerConnected force_wearG5=false Phone startBtService");
                                startBtService();
                            }
                        }
                    } else {
                        //onPeerDisconnected
                        Log.d(TAG, "CheckWearableConnected onPeerDisconnected");
                        if (sharedPrefs.getBoolean("wear_sync", false)) {
                            Log.d(TAG, "CheckWearableConnected onPeerDisconnected wear_sync=true Phone startBtService");
                            startBtService();
                        }
                    }
                } else {
                    Log.d(TAG, "Debounce limit hit - not sending");
                }
            } else {
                Log.d(TAG, "Not connected for sending");
                googleApiClient.connect();
            }
            return null;
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {//KS does not seem to get triggered; therefore use OnMessageReceived instead

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {

                String path = event.getDataItem().getUri().getPath();

                switch (path) {
                    case WEARABLE_PREF_DATA_PATH:
                        dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                        if (dataMap != null) {
                            Log.d(TAG, "onDataChanged WEARABLE_PREF_DATA_PATH dataMap=" + dataMap);
                            syncPrefData(dataMap);
                        }
                        break;
                    default:
                        Log.d(TAG, "Unknown wearable path: " + path);
                        break;
                }
            }
        }
    }

    // incoming messages from wear device
    @Override
    public void onMessageReceived(MessageEvent event) {
        DataMap dataMap;
        byte[] decomprBytes;
        Log.d(TAG, "onMessageReceived enter");
        if (wear_integration) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-msgrec", 60000);//KS test with 120000
            if (event != null) {
                Log.d(TAG, "onMessageReceived wearable event path: " + event.getPath());
                switch (event.getPath()) {
                    case WEARABLE_RESEND_PATH:
                        resendData(0);
                        break;
                    case WEARABLE_VOICE_PAYLOAD:
                        String eventData = "";
                        try {
                            eventData = new String(event.getData(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            eventData = "error";
                        }
                        Log.d(TAG, "Received wearable: voice payload: " + eventData);
                        if (eventData.length() > 1)
                            receivedText(getApplicationContext(), eventData);
                        break;
                    case WEARABLE_APPROVE_TREATMENT:
                        approveTreatment(getApplicationContext(), "");
                        break;
                    case WEARABLE_CANCEL_TREATMENT:
                        cancelTreatment(getApplicationContext(), "");
                        break;
                    case WEARABLE_SNOOZE_ALERT:
                        try {
                            eventData = new String(event.getData(), "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            eventData = "30";
                        }
                        int snooze;
                        try {
                            snooze = Integer.parseInt(eventData);
                        } catch (NumberFormatException e) {
                            snooze = 30;
                        }
                        Log.d(TAG, "Received wearable: snooze payload: " + snooze);
                        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), snooze, true);
                        JoH.static_toast_long(getResources().getString(R.string.alert_snoozed_by_watch));
                        break;
                    case SYNC_BGS_PATH + "_BM"://TEST ignore only for benchmark
                    case SYNC_LOGS_PATH + "_BM":
                    case SYNC_BGS_PATH + "_BM_DUP":
                    case SYNC_LOGS_PATH + "_BM_DUP":
                    case SYNC_BGS_PATH + "_BM_RAND":
                    case SYNC_LOGS_PATH + "_BM_RAND":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = event.getData();
                        if (decomprBytes != null) {
                            //Log.d(TAG, "Benchmark: " + event.getPath() + "event.getData().length=" + decomprBytes.length);
                        }
                        break;
                    case SYNC_BGS_PATH + "_BM_COMPRESS"://TEST ignore only for benchmark
                    case SYNC_BGS_PATH + "_BM_DUP_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);//bBenchmark
                        dataMap = DataMap.fromByteArray(decomprBytes);
                        if (dataMap != null) {
                            syncTransmitterData(dataMap, true);//bBenchmark=true
                        }
                        break;
                    case SYNC_LOGS_PATH + "_BM_COMPRESS":
                    case SYNC_LOGS_PATH + "_BM_DUP_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);
                        dataMap = DataMap.fromByteArray(decomprBytes);
                        if (dataMap != null) {
                            syncLogData(dataMap, true);//bBenchmark=true
                        }
                        break;
                    case SYNC_BGS_PATH + "_BM_RAND_COMPRESS":
                    case SYNC_LOGS_PATH + "_BM_RAND_COMPRESS":
                        Log.d(TAG, "onMessageReceived Ignore, just for test!");
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), true);
                        break;
                    case SYNC_BGS_PATH://KS
                        Log.d(TAG, "onMessageReceived SYNC_BGS_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_BGS_PATH dataMap=" + dataMap);
                                syncTransmitterData(dataMap, false);
                            }
                        }
                        break;

                    case SYNC_BGS_PRECALCULATED_PATH:
                        Log.d(TAG, "onMessageReceived " + SYNC_BGS_PRECALCULATED_PATH);
                        decomprBytes = decompressBytes(event.getPath(), event.getData(), false);
                        dataMap = DataMap.fromByteArray(decomprBytes);
                        if (dataMap != null) {
                            final DataMap fdataMap = dataMap;
                            new LowPriorityThread(() -> {
                                syncBgReadingsData(fdataMap);
                                Home.staticRefreshBGCharts();
                            }, "inbound-precalculated-bg").start();
                        }
                        break;

                    case SYNC_LOGS_PATH:
                        Log.d(TAG, "onMessageReceived SYNC_LOGS_PATH");
                        if (event.getData() != null) {
                            decomprBytes = decompressBytes(event.getPath(), event.getData(), false);
                            dataMap = DataMap.fromByteArray(decomprBytes);//event.getData()
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_LOGS_PATH");
                                syncLogData(dataMap, false);
                            }
                        }
                        break;
                    case SYNC_LOGS_REQUESTED_PATH:
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            syncLogsRequested = dataMap.getLong("syncLogsRequested", -1);
                            Log.d(TAG, "onMessageReceived SYNC_LOGS_REQUESTED_PATH syncLogsRequested=" + syncLogsRequested);
                        }
                        break;
                    case SYNC_STEP_SENSOR_PATH:
                        Log.d(TAG, "onMessageReceived SYNC_STEP_SENSOR_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_STEP_SENSOR_PATH dataMap=" + dataMap);
                                syncStepSensorData(dataMap, false);
                            }
                        }
                        break;
                    case SYNC_HEART_SENSOR_PATH:
                        Log.d(TAG, "onMessageReceived SYNC_HEART_SENSOR_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_HEART_SENSOR_PATH dataMap=" + dataMap);
                                syncHeartSensorData(dataMap, false);
                            }
                        }
                        break;
                    case SYNC_TREATMENTS_PATH:
                        Log.d(TAG, "onMessageReceived SYNC_TREATMENTS_PATH");
                        if (event.getData() != null) {
                            dataMap = DataMap.fromByteArray(event.getData());
                            if (dataMap != null) {
                                Log.d(TAG, "onMessageReceived SYNC_TREATMENTS_PATH dataMap=" + dataMap);
                                syncTreatmentsData(dataMap, false);
                            }
                        }
                        break;
                    case WEARABLE_INITDB_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_INITDB_PATH");
                        initWearData();
                        break;
                    case WEARABLE_INITTREATMENTS_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_INITTREATMENTS_PATH");
                        initWearTreatments();
                        break;
                    case WEARABLE_REPLYMSG_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH");
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH dataMap=" + dataMap);
                            String action_path = dataMap.getString("action_path", "");
                            if (action_path != null && !action_path.isEmpty()) {
                                switch (action_path) {
                                    case START_COLLECTOR_PATH:
                                        String msg = dataMap.getString("msg", "");
                                        JoH.static_toast_short(msg);
                                        break;
                                    case STATUS_COLLECTOR_PATH:
                                        Log.d(TAG, "onMessageReceived WEARABLE_REPLYMSG_PATH send LocalBroadcastManager ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE=" + ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE);
                                        final Intent intent = new Intent(ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE);
                                        intent.putExtra("data", dataMap.toBundle());//msg
                                        LocalBroadcastManager.getInstance(xdrip.getAppContext()).sendBroadcast(intent);
                                        break;
                                }
                            }
                        }
                        break;
                    case WEARABLE_G5BATTERY_PAYLOAD:
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_FIELD_SENDPATH dataMap=" + dataMap);
                            syncFieldData(dataMap);
                        }
                        break;
                    case WEARABLE_INITPREFS_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_INITPREFS_PATH");
                        sendPrefSettings();
                        break;
                    case WEARABLE_LOCALE_CHANGED_PATH:
                        Log.d(TAG, "onMessageReceived WEARABLE_LOCALE_CHANGED_PATH");
                        sendLocale();
                        break;
                    case WEARABLE_PREF_DATA_PATH:
                        dataMap = DataMap.fromByteArray(event.getData());
                        if (dataMap != null) {
                            Log.d(TAG, "onMessageReceived WEARABLE_PREF_DATA_PATH dataMap=" + dataMap);
                            syncPrefData(dataMap);
                        }
                        break;


                    default:

                        if (event.getPath().startsWith(WEARABLE_REQUEST_APK)) {
                            // rate limit at this end just needs to de-bounce but allow retries
                            if (JoH.ratelimit(WEARABLE_REQUEST_APK, 15)) {
                                JoH.static_toast_short("Updating wear app");
                                int startAt = 0;
                                final String[] split = event.getPath().split("\\^");
                                if (split.length == 2) {
                                    startAt = Integer.parseInt(split[1]);
                                }
                                if (startAt == 0) {
                                    UserError.Log.uel(TAG, "VUP: Sending latest apk version to watch");
                                    JoH.static_toast_long("Sending latest version to watch");
                                }
                                final int finalStartAt = startAt;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mWearNodeId == null) {
                                            UserError.Log.d(TAG, "VUP: nodeid is null");
                                            updateWearSyncBgsCapability(); // try to populate
                                        }

                                        if (mWearNodeId != null) {
                                            // TODO limit to 120
                                            UserError.Log.d(TAG, "VUP: nodeid is now not null");
                                            if (apkBytes == null) {
                                                UserError.Log.d(TAG, "VUP: getting bytes");
                                                apkBytes = GetWearApk.getBytes();
                                            }
                                            if (apkBytes != null) {
                                                UserError.Log.d(TAG, "VUP: Trying to open channel to send apk");
                                                ChannelApi.OpenChannelResult result = Wearable.ChannelApi.openChannel(googleApiClient, mWearNodeId, "/updated-apk").await();

                                                final Channel channel = result.getChannel();
                                                if (channel != null) {

                                                    channel.getOutputStream(googleApiClient).setResultCallback(new ResultCallback<Channel.GetOutputStreamResult>() {
                                                        @Override
                                                        public void onResult(final Channel.GetOutputStreamResult getOutputStreamResult) {
                                                            Log.d(TAG, "VUP: channel get outputstream onResult:");


                                                            // TODO recurse/retry a few times if we haven't sent anything?
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {

                                                                    OutputStream output = null;
                                                                    try {
                                                                        output = getOutputStreamResult.getOutputStream();
                                                                        Log.d(TAG, "VUP: output stream opened");
                                                                        // this protocol can never be changed
                                                                        output.write((BuildConfig.VERSION_NAME + "\n").getBytes("UTF-8")); // version name
                                                                        output.write((apkBytes.length + "\n").getBytes("UTF-8")); // total length
                                                                        output.write((finalStartAt + "\n").getBytes("UTF-8")); // data starting from position
                                                                        // send data
                                                                        JoH.threadSleep(5000);
                                                                        Log.d(TAG, "VUP: sending data");
                                                                        // TODO stagger write?  await confirmation from far end to start xmit??
                                                                        output.write(apkBytes, finalStartAt, apkBytes.length - finalStartAt);
                                                                        output.flush();
                                                                        output.write(new byte[64000]); // seems to need some kind of padding
                                                                        JoH.threadSleep(5000);
                                                                        Log.d(TAG, "VUP: sent bytes: " + (apkBytes.length - finalStartAt));
                                                                    } catch (final IOException e) {
                                                                        Log.w(TAG, "VUP: could not send message: " + "Node: " + channel.getNodeId() + "Path: " + channel.getPath() + " Error message: " + e.getMessage() + " Error cause: " + e.getCause());
                                                                    } finally {
                                                                        try {
                                                                            Log.w(TAG, "VUP: Closing output stream");
                                                                            if (output != null) {
                                                                                output.close();
                                                                            }
                                                                        } catch (final IOException e) {
                                                                            Log.w(TAG, "VUP: could not close Output Stream: " + "Node ID: " + channel.getNodeId() + " Path: " + channel.getPath() + " Error message: " + e.getMessage() + " Error cause: " + e.getCause());
                                                                        } finally {
                                                                            channel.close(googleApiClient);
                                                                        }
                                                                    }

                                                                }
                                                            }).start();

                                                        }
                                                    });
                                                } else {
                                                    UserError.Log.d(TAG, "VUP: Could not send wearable apk as Channel result was null!");
                                                }
                                            }
                                        } else {
                                            Log.d(TAG, "VUP: Could not send wearable apk as nodeid is currently null");
                                        }
                                    }
                                }).start();
                            }
                        } else {
                            Log.d(TAG, "Unknown wearable path: " + event.getPath());
                            super.onMessageReceived(event);
                        }
                }
            }
            JoH.releaseWakeLock(wl);
        } else {
            super.onMessageReceived(event);
        }
    }

    private byte[] decompressBytes(String pathdesc, byte[] bytes, boolean bBenchmark) {
        byte[] decomprBytes;
        if ((bytes.length > 8)
                && (bytes[0] == (byte) 0x1F)
                && (bytes[1] == (byte) 0x8B)
                && (bytes[2] == (byte) 0x08)
                && (bytes[3] == (byte) 0x00)) {
            if (bBenchmark) {
                double benchmark_time = ts();
                JoH.benchmark(null);
                decomprBytes = JoH.decompressBytesToBytes(bytes);
                String msg = pathdesc + " JoH.decompressBytesToBytes from length=" + bytes.length + " to length=" + decomprBytes.length;
                JoH.benchmark(msg);
                msg = msg + " " + (ts() - benchmark_time) + " ms";
                sendDataReceived(DATA_ITEM_RECEIVED_PATH, msg, 1, "BM", -1);//"DATA_RECEIVED"
                return decomprBytes;
            } else {
                decomprBytes = JoH.decompressBytesToBytes(bytes);
                Log.d(TAG, pathdesc + " JoH.decompressBytesToBytes from length=" + bytes.length + " to length=" + decomprBytes.length);
                return decomprBytes;
            }
        } else {
            Log.d(TAG, "Benchmark: decompressBytesToBytes DataMap is not compressed!  Process as normal. length=" + bytes.length);
            return bytes;
        }
    }

    private void sendG5QueueData(String queueData) {
        if ((wear_integration) && (queueData != null)) {
            forceGoogleApiConnect();
            new SendToDataLayerThread(WEARABLE_G5_QUEUE_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap("queueData", queueData));
        }
    }

    private void sendData() {
        BgReading bg = BgReading.last();
        if (bg != null) {
            forceGoogleApiConnect();
            if (wear_integration) {
                final int battery = PowerStateReceiver.getBatteryLevel(getApplicationContext());
                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap(bg, mPrefs, new BgGraphBuilder(getApplicationContext()), battery));
            }
        }
    }

    private void resendData(long since) {
        Log.d(TAG, "resendData ENTER");
        forceGoogleApiConnect();
        final long startTime = since == 0 ? new Date().getTime() - (60000 * 60 * 24) : since;
        Log.d(TAG, "resendData googleApiClient connected ENTER, sending since: " + JoH.dateTimeText(startTime));
        final BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            List<BgReading> graph_bgs = BgReading.latestForGraph(60, startTime);
            BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(getApplicationContext());
            if (!graph_bgs.isEmpty()) {
                final int battery = PowerStateReceiver.getBatteryLevel(getApplicationContext());
                DataMap entries = dataMap(last_bg, mPrefs, bgGraphBuilder, battery);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (BgReading bg : graph_bgs) {
                    dataMaps.add(dataMap(bg, mPrefs, bgGraphBuilder, battery));
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                if (mPrefs.getBoolean("extra_status_line", false)) {
                    entries.putString("extra_status_line", StatusLine.extraStatusLine());
                }

                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            }
        }
    }

    private void sendNotification(String path, String notification) {//KS add args
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            Log.d(TAG, "sendNotification Notification=" + notification + " Path=" + path);
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            //unique content
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString(notification, notification);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "sendNotification No connection to wearable available!");
        }
    }

    private void sendRequestExtra(String path, String key, String value) {
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);//NEW_STATUS_PATH
            //unique content
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString(key, value);//"externalStatusString"
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e("sendRequestExtra", "No connection to wearable available!");
        }
    }

    private void sendRequestExtra(String path, String key, boolean value) {
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);//NEW_STATUS_PATH
            //unique content
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putBoolean(key, value);//"externalStatusString"
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e("sendRequestExtra", "No connection to wearable available!");
        }
    }

    private void sendRequestExtra(String path, String key, byte[] value) {
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putByteArray(key, value);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            dataMapRequest.setUrgent();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
            Log.d(TAG, "Sending bytes path: " + path + " " + value.length);

        } else {
            Log.e("sendRequestExtra", "No connection to wearable available!");
        }
    }

    private void sendBlob(String path, final byte[] blob) {
        forceGoogleApiConnect();
        if (googleApiClient.isConnected()) {
            final Asset asset = Asset.createFromBytes(blob);
            Log.d(TAG, "sendBlob asset size: " + asset.getData().length);
            final PutDataMapRequest request = PutDataMapRequest.create(path);
            request.getDataMap().putLong("time", new Date().getTime());
            request.getDataMap().putByteArray("asset", blob);
            request.setUrgent();

            final PendingResult result = Wearable.DataApi.putDataItem(googleApiClient, request.asPutDataRequest());

            result.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult sendMessageResult) {
                    if (!sendMessageResult.getStatus().isSuccess()) {
                        UserError.Log.e(TAG, "ERROR: failed to sendblob Status=" + sendMessageResult.getStatus().getStatusMessage());
                    } else {
                        UserError.Log.i(TAG, "Sendblob  Status=: " + sendMessageResult.getStatus().getStatusMessage());
                    }
                }
            });

            Log.d(TAG, "sendBlob: Sending asset of size " + blob.length);

        } else {
            Log.e(TAG, "sendBlob: No connection to wearable available!");
        }
    }


    // sending to watch - beware we munge the calculated value and replace with display glucose
    private DataMap dataMap(BgReading bg, SharedPreferences sPrefs, BgGraphBuilder bgGraphBuilder, int battery) {
        Double highMark = Double.parseDouble(sPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "70"));
        DataMap dataMap = new DataMap();

        //int battery = BgSendQueue.getBatteryLevel(getApplicationContext());

        // TODO this is inefficent when we are called in a loop instead should be passed in or already stored in bgreading
        final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose(); // current best


        dataMap.putString("sgvString", dg != null && bg.dg_mgdl > 0 ? dg.unitized : bgGraphBuilder.unitized_string(bg.calculated_value));

        dataMap.putString("slopeArrow", bg.slopeArrow());
        dataMap.putDouble("timestamp", bg.timestamp); //TODO: change that to long (was like that in NW)

        // This delta string only applies to the last reading even if we are processing historical data here
        if (dg != null) {
            dataMap.putString("delta", dg.unitized_delta);
        } else {
            dataMap.putString("delta", bgGraphBuilder.unitizedDeltaString(true, true, true));
        }
        dataMap.putString("battery", "" + battery);
        dataMap.putLong("sgvLevel", sgvLevel(bg.dg_mgdl > 0 ? bg.dg_mgdl : bg.calculated_value, sPrefs, bgGraphBuilder));
        dataMap.putInt("batteryLevel", (battery >= 30) ? 1 : 0);
        dataMap.putDouble("sgvDouble", bg.dg_mgdl > 0 ? bg.dg_mgdl : bg.calculated_value);
        dataMap.putDouble("high", inMgdl(highMark, sPrefs));
        dataMap.putDouble("low", inMgdl(lowMark, sPrefs));
        dataMap.putInt("bridge_battery", mPrefs.getInt("bridge_battery", -1));//Used in DexCollectionService
        //if (sPrefs.getBoolean("extra_status_line", false)) {
        //    dataMap.putString("extra_status_line", Home.extraStatusLine());
        //}

        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
        return dataMap;
    }

    private void sendLocale() {
        final Locale locale = Locale.getDefault();
        Log.d(TAG, "ACTION_LOCALE_CHANGED Locale changed to " + locale);
        String country = locale.getCountry();
        sendRequestExtra(WEARABLE_LOCALE_CHANGED_PATH, "locale", locale.getLanguage() + (country != null && !country.isEmpty() ? "_" + country : ""));
    }

    // These are the settings which get sent to Wear device
    private void sendPrefSettings() {//KS
        forceGoogleApiConnect();
        DataMap dataMap = new DataMap();
        String dexCollector = "None";
        boolean enable_wearG5 = false;
        boolean force_wearG5 = false;
        String node_wearG5 = "";

        // add booleans that default to false to this list
        final List<String> defaultFalseBooleansToSend = WearSyncBooleans.getBooleansToSync();
        // add persistent store strings that default to ""
        final List<String> defaultBlankPersistentStringsToSend = WearSyncPersistentStrings.getPersistentStrings();

        wear_integration = mPrefs.getBoolean("wear_sync", false);
        if (wear_integration) {
            Log.d(TAG, "sendPrefSettings wear_sync=true");
            dexCollector = mPrefs.getString(DexCollectionType.DEX_COLLECTION_METHOD, "DexcomG5");
            enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
            force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
            node_wearG5 = mPrefs.getString("node_wearG5", "");
            dataMap.putString("dex_collection_method", dexCollector);
            dataMap.putBoolean("rewrite_history", mPrefs.getBoolean("rewrite_history", false));
            dataMap.putBoolean("enable_wearG5", enable_wearG5);
            dataMap.putBoolean("force_wearG5", force_wearG5);
            dataMap.putString("node_wearG5", node_wearG5);
            dataMap.putString("share_key", mPrefs.getString("share_key", "SM00000000"));//Used by DexShareCollectionService
            //Advanced Bluetooth Settings used by G4+xBridge DexCollectionService - temporarily just use the Phone's settings
            dataMap.putBoolean("use_transmiter_pl_bluetooth", mPrefs.getBoolean("use_transmiter_pl_bluetooth", false));
            dataMap.putBoolean("use_rfduino_bluetooth", mPrefs.getBoolean("use_rfduino_bluetooth", false));
            dataMap.putBoolean("automatically_turn_bluetooth_on", mPrefs.getBoolean("automatically_turn_bluetooth_on", true));
            dataMap.putBoolean("bluetooth_excessive_wakelocks", mPrefs.getBoolean("bluetooth_excessive_wakelocks", true));
            dataMap.putBoolean("close_gatt_on_ble_disconnect", mPrefs.getBoolean("close_gatt_on_ble_disconnect", true));
            dataMap.putBoolean("bluetooth_frequent_reset", mPrefs.getBoolean("bluetooth_frequent_reset", false));
            dataMap.putBoolean("bluetooth_watchdog", mPrefs.getBoolean("bluetooth_watchdog", false));
            dataMap.putString("bluetooth_watchdog_timer", mPrefs.getString("bluetooth_watchdog_timer", "20"));
            dataMap.putInt("bridge_battery", mPrefs.getInt("bridge_battery", -1));
            dataMap.putInt("nfc_sensor_age", mPrefs.getInt("nfc_sensor_age", -1));

            dataMap.putBoolean("sync_wear_logs", mPrefs.getBoolean("sync_wear_logs", false));
            //Alerts:
            dataMap.putString("persistent_high_repeat_mins", mPrefs.getString("persistent_high_repeat_mins", "20"));
            dataMap.putString("persistent_high_threshold_mins", mPrefs.getString("persistent_high_threshold_mins", "60"));
            dataMap.putBoolean("falling_alert", mPrefs.getBoolean("falling_alert", false));
            dataMap.putString("falling_bg_val", mPrefs.getString("falling_bg_val", "2"));
            dataMap.putBoolean("rising_alert", mPrefs.getBoolean("rising_alert", false));
            dataMap.putString("rising_bg_val", mPrefs.getString("rising_bg_val", "2"));
            dataMap.putBoolean("aggressive_service_restart", mPrefs.getBoolean("aggressive_service_restart", false));

            //Extra Status Line
            dataMap.putBoolean("extra_status_line", mPrefs.getBoolean("extra_status_line", false));
            dataMap.putBoolean("extra_status_stats_24h", Pref.getBooleanDefaultFalse("extra_status_stats_24h"));
            dataMap.putBoolean("status_line_calibration_long", mPrefs.getBoolean("status_line_calibration_long", false));
            dataMap.putBoolean("status_line_calibration_short", mPrefs.getBoolean("status_line_calibration_short", false));
            dataMap.putBoolean("status_line_avg", mPrefs.getBoolean("status_line_avg", false));
            dataMap.putBoolean("status_line_a1c_dcct", mPrefs.getBoolean("status_line_a1c_dcct", false));
            dataMap.putBoolean("status_line_a1c_ifcc", mPrefs.getBoolean("status_line_a1c_ifcc", false));
            dataMap.putBoolean("status_line_in", mPrefs.getBoolean("status_line_in", false));
            dataMap.putBoolean("status_line_high", mPrefs.getBoolean("status_line_high", false));
            dataMap.putBoolean("status_line_low", mPrefs.getBoolean("status_line_low", false));
            dataMap.putBoolean("status_line_carbs", mPrefs.getBoolean("status_line_carbs", false));
            dataMap.putBoolean("status_line_insulin", mPrefs.getBoolean("status_line_insulin", false));
            dataMap.putBoolean("status_line_stdev", mPrefs.getBoolean("status_line_stdev", false));
            dataMap.putBoolean("status_line_royce_ratio", mPrefs.getBoolean("status_line_royce_ratio", false));
            dataMap.putBoolean("status_line_capture_percentage", mPrefs.getBoolean("status_line_capture_percentage", false));
            dataMap.putBoolean("status_line_realtime_capture_percentage", mPrefs.getBoolean("status_line_realtime_capture_percentage", false));

            //Calibration plugin
            dataMap.putBoolean("extra_status_calibration_plugin", mPrefs.getBoolean("extra_status_calibration_plugin", false));
            dataMap.putBoolean("display_glucose_from_plugin", Pref.getBooleanDefaultFalse("display_glucose_from_plugin"));
            dataMap.putBoolean("use_pluggable_alg_as_primary", Pref.getBooleanDefaultFalse("use_pluggable_alg_as_primary"));
            if (Pref.getBooleanDefaultFalse("engineering_mode")) {
                dataMap.putBoolean("old_school_calibration_mode", Pref.getBooleanDefaultFalse("old_school_calibration_mode"));
            }

            dataMap.putBoolean("show_wear_treatments", Pref.getBooleanDefaultFalse("show_wear_treatments"));
            dataMap.putBoolean("use_ob1_g5_collector_service", Pref.getBooleanDefaultFalse("use_ob1_g5_collector_service"));
            dataMap.putString(Blukon.BLUKON_PIN_PREF, Pref.getStringDefaultBlank(Blukon.BLUKON_PIN_PREF));

            final String dex_time_keeper = Ob1G5StateMachine.extractDexTime();
            if (dex_time_keeper != null) {
                dataMap.putString("dex-timekeeping", dex_time_keeper);
            }

        }
        //Step Counter
        // note transmutes use_pebble_health -> use_wear_health
        dataMap.putBoolean("use_wear_health", mPrefs.getBoolean("use_pebble_health", true));
        is_using_bt = DexCollectionType.hasBluetooth();

        Double highMark = Double.parseDouble(mPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(mPrefs.getString("lowValue", "70"));
        Log.d(TAG, "sendPrefSettings enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " dex_collection_method:" + dexCollector);
        dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
        dataMap.putString("dex_txid", mPrefs.getString("dex_txid", "ABCDEF"));
        dataMap.putString("units", mPrefs.getString("units", "mgdl"));
        dataMap.putDouble("high", highMark);//inMgdl(highMark, mPrefs));//KS Fix for mmol on graph Y-axis in wear standalone mode
        dataMap.putDouble("low", lowMark);//inMgdl(lowMark, mPrefs));//KS Fix for mmol on graph Y-axis in wear standalone mode
        dataMap.putBoolean("g5_non_raw_method", mPrefs.getBoolean("g5_non_raw_method", false));
        dataMap.putString("extra_tags_for_logging", Pref.getStringDefaultBlank("extra_tags_for_logging"));
        //dataMap.putBoolean("engineering_mode",  Pref.getBooleanDefaultFalse("engineering_mode"));
        dataMap.putBoolean("bridge_battery_alerts", Pref.getBooleanDefaultFalse("bridge_battery_alerts"));
        dataMap.putString("bridge_battery_alert_level", Pref.getString("bridge_battery_alert_level", "30"));
        final Locale locale = Locale.getDefault();
        String country = locale.getCountry();
        dataMap.putString("locale", locale.getLanguage() + (country != null && !country.isEmpty() ? "_" + country : ""));

        dataMap.putString("build-version-name", getVersionID());

        for (String pref : defaultFalseBooleansToSend) {
            dataMap.putBoolean(pref, Pref.getBooleanDefaultFalse(pref));
        }
        for (String str : defaultBlankPersistentStringsToSend) {
            dataMap.putString(str, PersistentStore.getString(str));
        }

        new SendToDataLayerThread(WEARABLE_PREF_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
    }

    private boolean sendSensorData() {//KS
        try {

            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
            if (googleApiClient != null) {
                Sensor sensor = Sensor.currentSensor();
                if (sensor != null) {
                    if (wear_integration) {
                        DataMap dataMap = new DataMap();
                        Log.d(TAG, "Sensor sendSensorData uuid=" + sensor.uuid + " started_at=" + sensor.started_at + " active=" + Sensor.isActive() + " battery=" + sensor.latest_battery_level + " location=" + sensor.sensor_location + " stopped_at=" + sensor.stopped_at);
                        String json = sensor.toS();
                        Log.d(TAG, "dataMap sendSensorData GSON: " + json);

                        dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP

                        dataMap.putString("dex_txid", mPrefs.getString("dex_txid", "ABCDEF"));//KS
                        dataMap.putLong("started_at", sensor.started_at);
                        dataMap.putString("uuid", sensor.uuid);
                        dataMap.putInt("latest_battery_level", sensor.latest_battery_level);
                        dataMap.putString("sensor_location", sensor.sensor_location);

                        new SendToDataLayerThread(WEARABLE_SENSOR_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
                        return true;
                    }
                } else
                    Log.e(TAG, "sendSensorData current sensor is null!");
            } else {
                Log.e(TAG, "sendSensorData No connection to wearable available for send Sensor!");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearCalibrationData: " + e);
            return false;
        }
        return true;
    }

    private void sendActiveBtDeviceData() {//KS
        if (is_using_bt) {//only required for Collector running on watch
            forceGoogleApiConnect();
            ActiveBluetoothDevice btDevice = ActiveBluetoothDevice.first();
            if (btDevice != null) {
                if (wear_integration) {
                    DataMap dataMap = new DataMap();
                    Log.d(TAG, "sendActiveBtDeviceData name=" + btDevice.name + " address=" + btDevice.address + " connected=" + btDevice.connected);

                    dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP

                    dataMap.putString("name", btDevice.name);
                    dataMap.putString("address", btDevice.address);
                    dataMap.putBoolean("connected", btDevice.connected);

                    new SendToDataLayerThread(WEARABLE_ACTIVEBTDEVICE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap);
                }
            }
        } else {
            Log.d(TAG, "Not sending activebluetoothdevice data as we are not using bt");
        }
    }

    private void sendAlertTypeData() {//KS
        try {
            forceGoogleApiConnect();
            List<AlertType> alerts = AlertType.getAllActive();
            if (alerts != null) {
                if (wear_integration) {
                    Log.d(TAG, "sendAlertTypeData latest count = " + alerts.size());
                    final DataMap entries = new DataMap();
                    final ArrayList<DataMap> dataMaps = new ArrayList<>(alerts.size());
                    for (AlertType alert : alerts) {
                        if (alert != null) {
                            dataMaps.add(dataMap(alert, "alert"));
                        }
                    }
                    entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                    entries.putDataMapArrayList("entries", dataMaps);
                    new SendToDataLayerThread(WEARABLE_ALERTTYPE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
                } else
                    Log.d(TAG, "sendAlertTypeData latest count = 0");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendAlertTypeData: " + e);
        }
    }

    private DataMap dataMap(String key, String value) {
        final DataMap dataMap = new DataMap();
        dataMap.putString(key, value);
        return dataMap;
    }

    private DataMap dataMap(AlertType alert, String type) {//KS
        DataMap dataMap = new DataMap();
        String json = alert.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString(type, json);
        return dataMap;
    }

    public static boolean sendWearUpload(List<BgReading> bgs, List<Calibration> cals, List<BloodTest> bts, List<Treatments> treatsAdd, List<String> treatsDel) {
        boolean statusCals = sendWearCalibrationData(0, 0, cals);
        boolean statusBgs = sendWearBgData(0, 0, bgs);
        boolean statusBts = sendWearBloodTestData(0, 0, bts);
        boolean statusTreats = sendWearTreatmentsData(0, 0, treatsAdd);
        boolean statusTreatsDel = sendWearTreatmentsDataDelete(treatsDel);
        return (statusCals && statusBts && statusTreats && statusTreatsDel && statusBgs);
    }

    public static boolean sendWearTreatmentsDataDelete(List<String> list) {
        if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiClient.connect();
        }
        if (googleApiClient != null) {
            if (!list.isEmpty()) {
                Log.d(TAG, "sendWearTreatmentsDataDelete graph size=" + list.size());
                DataMap entries = new DataMap();
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putString("action", "delete");
                entries.putStringArrayList("entries", (new ArrayList<String>(list)));
                new SendToDataLayerThread(WEARABLE_TREATMENTS_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            } else
                Log.d(TAG, "sendWearTreatmentsDataDelete treatments count = 0");
        } else {
            Log.e(TAG, "sendWearTreatmentsData No connection to wearable available for send treatment!");
            return false;
        }
        return true;
    }

    public static boolean sendWearTreatmentsData(Integer count, long startTime) {
        return sendWearTreatmentsData(count, startTime, null);
    }

    public static boolean sendWearTreatmentsData(Integer count, long startTime, List<Treatments> list) {
        try {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
            if (googleApiClient != null) {
                Treatments last = list != null && list.size() > 0 ? list.get(0) : Treatments.last();
                if (last != null) {
                    Log.d(TAG, "sendWearTreatmentsData last.timestamp:" + JoH.dateTimeText(last.timestamp));
                } else {
                    Log.d(TAG, "sendWearTreatmentsData no treatments exist");
                    return true;
                }
                List<Treatments> graph;
                if (list != null)
                    graph = list;
                else if (startTime == 0)
                    graph = Treatments.latest(count);
                else
                    graph = Treatments.latestForGraph(count, startTime);
                if (!graph.isEmpty()) {
                    Log.d(TAG, "sendWearTreatmentsData graph size=" + graph.size());
                    final ArrayList<DataMap> dataMaps = new ArrayList<>(graph.size());
                    DataMap entries = dataMap(last);
                    for (Treatments data : graph) {
                        dataMaps.add(dataMap(data));
                    }
                    Log.d(TAG, "sendWearTreatmentsData entries=" + entries);
                    entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                    entries.putString("action", "insert");
                    entries.putDataMapArrayList("entries", dataMaps);
                    new SendToDataLayerThread(WEARABLE_TREATMENTS_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
                } else
                    Log.d(TAG, "sendWearTreatmentsData treatments count = 0");
            } else {
                Log.e(TAG, "sendWearTreatmentsData No connection to wearable available for send treatment!");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearTreatmentsData: " + e);
            return false;
        }
        return true;
    }

    private static DataMap dataMap(Treatments data) {
        DataMap dataMap = new DataMap();
        String json = data.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("data", json);
        return dataMap;
    }

    public static boolean sendWearBloodTestData(Integer count, long startTime) {
        return sendWearBloodTestData(count, startTime, null);
    }

    public static boolean sendWearBloodTestData(Integer count, long startTime, List<BloodTest> list) {//DataMap
        try {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
            if (googleApiClient != null) {
                BloodTest last = list != null && list.size() > 0 ? list.get(0) : BloodTest.last();
                if (last != null) {
                    Log.d(TAG, "sendWearBloodTestData last.timestamp:" + JoH.dateTimeText(last.timestamp));
                } else {
                    Log.d(TAG, "sendWearBloodTestData no BloodTest exist");
                    return true;
                }
                List<BloodTest> graph;
                if (list != null)
                    graph = list;
                else if (startTime == 0)
                    graph = BloodTest.last(count);
                else
                    graph = BloodTest.latestForGraph(count, startTime);
                if (!graph.isEmpty()) {
                    Log.d(TAG, "sendWearBloodTestData graph size=" + graph.size());
                    final ArrayList<DataMap> dataMaps = new ArrayList<>(graph.size());
                    DataMap entries = dataMap(last);
                    for (BloodTest data : graph) {
                        dataMaps.add(dataMap(data));
                    }
                    Log.d(TAG, "sendWearBloodTestData entries=" + entries);
                    entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                    entries.putDataMapArrayList("entries", dataMaps);
                    new SendToDataLayerThread(WEARABLE_BLOODTEST_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
                } else
                    Log.d(TAG, "sendWearBloodTestData BloodTest count = 0");
            } else {
                Log.e(TAG, "sendWearBloodTestData No connection to wearable available for send BloodTest!");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearBloodTestData: " + e);
            return false;
        }
        return true;
    }

    private static DataMap dataMap(BloodTest data) {
        DataMap dataMap = new DataMap();
        String json = data.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("data", json);
        return dataMap;
    }

    private boolean sendWearCalibrationData(Integer count) {
        return sendWearCalibrationData(count, 0);
    }

    private boolean sendWearCalibrationData(Integer count, long startTime) {
        return sendWearCalibrationData(count, startTime, null);
    }

    private static boolean sendWearCalibrationData(Integer count, long startTime, List<Calibration> list) {
        try {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiClient.connect();
            }
            //if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            if (googleApiClient != null) {
                Log.d(TAG, "sendWearCalibrationData");
                final Sensor sensor = Sensor.currentSensor();
                final Calibration last = list != null && list.size() > 0 ? list.get(0) : Calibration.last();

                List<Calibration> latest;
                BgReading lastBgReading = BgReading.last();
                //From BgReading: if (lastBgReading.calibration_flag == true && ((lastBgReading.timestamp + (60000 * 20)) > bgReading.timestamp) && ((lastBgReading.calibration.timestamp + (60000 * 20)) > bgReading.timestamp))
                //From BgReading:     lastBgReading.calibration.rawValueOverride()
                if (list != null)
                    latest = list;
                else if (startTime != 0)
                    latest = Calibration.latestForGraphSensor(count, startTime, Long.MAX_VALUE);
                else if (lastBgReading != null && lastBgReading.calibration != null && lastBgReading.calibration_flag == true) {
                    Log.d(TAG, "sendWearCalibrationData lastBgReading.calibration_flag=" + lastBgReading.calibration_flag + " lastBgReading.timestamp: " + lastBgReading.timestamp + " lastBgReading.calibration.timestamp: " + lastBgReading.calibration.timestamp);
                    latest = Calibration.allForSensor();
                } else {
                    latest = Calibration.latest(count);
                }

                if ((sensor != null) && (last != null) && (latest != null && !latest.isEmpty())) {
                    Log.d(TAG, "sendWearCalibrationData latest count = " + latest.size());
                    final DataMap entries = dataMap(last);
                    final ArrayList<DataMap> dataMaps = new ArrayList<>(latest.size());
                    if (sensor.uuid != null) {
                        for (Calibration calibration : latest) {
                            if ((calibration != null) && (calibration.sensor_uuid != null) && (calibration.sensor_uuid.equals(sensor.uuid))) {
                                dataMaps.add(dataMap(calibration));
                            }
                        }
                    }
                    entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                    entries.putDataMapArrayList("entries", dataMaps);
                    new SendToDataLayerThread(WEARABLE_CALIBRATION_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
                } else
                    Log.d(TAG, "sendWearCalibrationData latest count = 0");
            } else {
                Log.e(TAG, "sendWearCalibrationData No connection to wearable available for send treatment!");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearCalibrationData: " + e);
            return false;
        }
        return true;
    }

    private static DataMap dataMap(Calibration calibration) {//KS
        DataMap dataMap = new DataMap();
        String json = calibration.toS();
        Log.d(TAG, "dataMap Calibration GSON: " + json);
        dataMap.putString("bgs", json); // should be refactored to avoid confusion!
        return dataMap;
    }

    private boolean sendWearBgData(Integer count) {
        return sendWearBgData(count, 0);
    }

    private boolean sendWearBgData(Integer count, long startTime) {
        return sendWearBgData(count, startTime, null);
    }


    private static boolean sendWearBgData(Integer count, long startTime, List<BgReading> list) {
        try {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                //googleApiConnect();
                googleApiClient.connect();
            }
            if (googleApiClient != null) {
                Log.d(TAG, "sendWearBgData");
                final BgReading last = BgReading.last();
                List<BgReading> latest;
                if (list != null)
                    latest = list;
                else if (startTime != 0)
                    latest = BgReading.latestForGraphSensor(count, startTime, Long.MAX_VALUE);
                else
                    latest = BgReading.latest(count);
                if ((last != null) && (latest != null && !latest.isEmpty())) {
                    final int battery = PowerStateReceiver.getBatteryLevel(xdrip.getAppContext());
                    Log.d(TAG, "sendWearBgData latest count = " + latest.size() + " battery=" + battery);
                    final DataMap entries = dataMap(last);
                    final ArrayList<DataMap> dataMaps = new ArrayList<>(latest.size());
                    final Sensor sensor = Sensor.currentSensor();
                    if ((sensor != null) && (sensor.uuid != null)) {
                        for (BgReading bg : latest) {
                            // if we have no sensor data, typically follower then add one in to pass tests.
                            if (bg != null && bg.sensor_uuid == null) {
                                bg.sensor_uuid = sensor.uuid;
                            }
                            if ((bg != null) && (bg.sensor_uuid != null) && (bg.sensor_uuid.equals(sensor.uuid) && (bg.calibration_uuid != null))) {
                                dataMaps.add(dataMap(bg));
                            } else {
                                if (bg.sensor_uuid == null) {
                                    Log.d(TAG, "sendWearBgData: sensor uuid is null on record to send");
                                }
                                if (bg.calibration_uuid == null) {
                                    Log.d(TAG, "sendWearBgData: calibration uuid is null on record to send");
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "sendWearBgData Not queueing data due to sensor: " + (sensor != null ? sensor.uuid : "null sensor object"));
                    }
                    entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                    entries.putInt("battery", battery);
                    entries.putDataMapArrayList("entries", dataMaps);
                    new SendToDataLayerThread(WEARABLE_BG_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
                } else
                    Log.d(TAG, "sendWearBgData lastest count = 0");
            } else {
                Log.e(TAG, "sendWearBgData No connection to wearable available for send BG!");
                return false;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Nullpointer exception in sendWearBgData: " + e);
            return false;
        }
        return true;
    }

    private static DataMap dataMap(BgReading bg) {//KS
        DataMap dataMap = new DataMap();
        //KS Fix for calibration_uuid not being set in Calibration.create which updates bgReading to new calibration ln 497
        //if (bg.calibration_flag == true) {
        //    bg.calibration_uuid = bg.calibration.uuid;
        //}
        try {
            dataMap.putString("calibrationUuid", bg.calibration.uuid);
        } catch (NullPointerException e) {
            Log.d(TAG, "Calibration uuid is not set in dataMap(BgReading)");
        }
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private void initWearData() {
        if (JoH.ratelimit("watch_init_wear_data", 120)) {
            wear_integration = mPrefs.getBoolean("wear_sync", false);
            if (wear_integration) {//is_using_bt
                Log.d(TAG, "***initWearData***");
                sendSensorData();
                sendActiveBtDeviceData();
                sendAlertTypeData();
                if (mPrefs.getBoolean("show_wear_treatments", false)) {
                    initWearTreatments();
                } else {
                    sendWearCalibrationData(sendCalibrationCount);
                    sendWearBgData(sendBgCount);
                }
                sendData();//ensure BgReading.Last is displayed on watch
            } else {
                Log.d(TAG, "Skip initWearData as wear integration is disabled");
            }
        } else
            Log.d(TAG, "Skip initWearData due to exceeding ratelimit");
    }

    private void initWearTreatments() {
        long startTime = new Date().getTime() - (60000 * 60 * 24 * 3);//3 days
        if (JoH.ratelimit("watch_init_wear_treatments_data", 60)) {
            Log.d(TAG, "initWearTreatments clear treatments and re-init from startTime=" + JoH.dateTimeText(startTime));
            sendNotification(CLEAR_TREATMENTS_PATH, "clearTreatments");//this necessary to ensure deleted treatments are cleared
            sendWearTreatmentsData(sendTreatmentsCount, startTime);
            sendWearBloodTestData(sendTreatmentsCount, startTime);
            sendWearCalibrationData(sendTreatmentsCount, startTime);
            sendWearBgData(sendTreatmentsCount, startTime);
        } else
            Log.d(TAG, "Skip initWearTreatments due to exceeding ratelimit");
    }

    private long sgvLevel(double sgv_double, SharedPreferences prefs, BgGraphBuilder bgGB) {
        Double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));
        if (bgGB.unitized(sgv_double) >= highMark) {
            return 1;
        } else if (bgGB.unitized(sgv_double) >= lowMark) {
            return 0;
        } else {
            return -1;
        }
    }

    private double inMgdl(double value, SharedPreferences sPrefs) {
        if (!doMgdl(sPrefs)) {
            return value * Constants.MMOLL_TO_MGDL;
        } else {
            return value;
        }

    }

    static public int readPrefsInt(SharedPreferences prefs, String name, int defaultValue) {
        try {
            return Integer.parseInt(prefs.getString(name, "" + defaultValue));

        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public void onDestroy() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (mPrefs != null && mPreferencesListener != null) {
            mPrefs.unregisterOnSharedPreferenceChangeListener(mPreferencesListener);
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse("wear_sync");
    }

    // any change must be replicated on wear
    private static String getVersionID() {
        return BuildConfig.VERSION_NAME;
    }


}
