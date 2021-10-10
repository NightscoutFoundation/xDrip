package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.G5Model.CalibrationState;
import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.PebbleMovement;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.CustomComplicationProviderService;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.Services.HeartRateService;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.WearSyncBooleans;
import com.eveningoutpost.dexdrip.UtilityModels.WearSyncPersistentStrings;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.utils.CheckBridgeBattery;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.VersionFixer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine.PREF_QUEUE_DRAINED;
import static com.eveningoutpost.dexdrip.Models.JoH.ts;
import static com.eveningoutpost.dexdrip.Services.G5CollectionService.G5_BATTERY_FROM_MARKER;
import static com.eveningoutpost.dexdrip.Services.G5CollectionService.G5_BATTERY_MARKER;
import static com.eveningoutpost.dexdrip.Services.G5CollectionService.G5_BATTERY_WEARABLE_SEND;
import static com.eveningoutpost.dexdrip.Services.G5CollectionService.G5_FIRMWARE_MARKER;
import static com.eveningoutpost.dexdrip.Services.HeartRateService.getWearHeartSensorData;
import static com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue.doMgdl;
import static com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue.extraStatusLine;
import static com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue.resendData;
import static com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue.sgvLevel;

/**
 * Created by Emma Black on 12/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ChannelApi.ChannelListener {
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
    public static final String SYNC_ALL_DATA = "/xdrip_plus_syncalldata";//KS
    private static final String CLEAR_LOGS_PATH = "/xdrip_plus_clearwearlogs";
    private static final String CLEAR_TREATMENTS_PATH = "/xdrip_plus_clearweartreatments";
    private static final String STATUS_COLLECTOR_PATH = "/xdrip_plus_statuscollector";
    private static final String START_COLLECTOR_PATH = "/xdrip_plus_startcollector";
    private static final String WEARABLE_REPLYMSG_PATH = "/xdrip_plus_watch_data_replymsg";
    public static final String WEARABLE_INITDB_PATH = "/xdrip_plus_watch_data_initdb";
    public static final String WEARABLE_INITTREATMENTS_PATH = "/xdrip_plus_watch_data_inittreatments";
    private static final String WEARABLE_TREATMENTS_DATA_PATH = "/xdrip_plus_watch_treatments_data";//KS
    private static final String WEARABLE_BLOODTEST_DATA_PATH = "/xdrip_plus_watch_bloodtest_data";//KS
    private static final String WEARABLE_INITPREFS_PATH = "/xdrip_plus_watch_data_initprefs";
    private static final String WEARABLE_LOCALE_CHANGED_PATH = "/xdrip_plus_locale_changed_data";//KS
    private static final String WEARABLE_BG_DATA_PATH = "/xdrip_plus_watch_bg_data";//KS
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/xdrip_plus_watch_cal_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/xdrip_plus_watch_sensor_data";//KS
    private static final String WEARABLE_PREF_DATA_PATH = "/xdrip_plus_watch_pref_data";//KS
    private static final String WEARABLE_ACTIVEBTDEVICE_DATA_PATH = "/xdrip_plus_watch_activebtdevice_data";//KS
    private static final String WEARABLE_ALERTTYPE_DATA_PATH = "/xdrip_plus_watch_alerttype_data";//KS
    public static final String WEARABLE_SNOOZE_ALERT = "/xdrip_plus_snooze_payload";
    private static final String DATA_ITEM_RECEIVED_PATH = "/xdrip_plus_data-item-received";//KS
    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_SENDDATA = "com.dexdrip.stephenblack.nightwatch.SEND_DATA";
    public static final String WEARABLE_FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    public static final String WEARABLE_FIELD_PAYLOAD = "field_xdrip_plus_payload";
    public static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    public static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String WEARABLE_TOAST_LOCAL_NOTIFICATON = "/xdrip_plus_local_toast";
    private static final String WEARABLE_REQUEST_APK = "/xdrip_plus_can_i_has_apk";
    private static final String WEARABLE_APK_DELIVERY = "/xdrip_plus_here_is_apk";
    public static final String WEARABLE_G5BATTERY_PAYLOAD = "/xdrip_plus_battery_payload";
    public final static String ACTION_BLUETOOTH_COLLECTION_SERVICE_UPDATE
            = "com.eveningoutpost.dexdrip.BLUETOOTH_COLLECTION_SERVICE_UPDATE";
    private static final String WEARABLE_G5_QUEUE_PATH = "/xdrip_plus_watch_g5_queue";

    // Phone
    private static final String CAPABILITY_PHONE_APP = "phone_app_sync_bgs";
    private static String localnode = "";

    private static final String TAG = "jamorham listener";
    private static SharedPreferences mPrefs;//KS
    private static boolean mLocationPermissionApproved;//KS
    private static long last_send_previous = 0;//KS
    final private static String pref_last_send_previous = "last_send_previous";
    private static long last_send_previous_log = 0;
    final private static String pref_last_send_previous_log = "last_send_previous_log";
    private static long last_send_previous_step_sensor = 0;
    private static long last_send_previous_heart_sensor = 0;
    final private static String pref_last_send_previous_step_sensor = "last_send_step_sensor";
    final private static String pref_last_send_previous_heart_sensor = "last_send_heart_sensor";
    private static long last_send_previous_treatments = 0;
    final private static String pref_last_send_previous_treatments = "last_send_previous_treatments";
    final private static int send_bg_count = 300;//288 equals full day of transmitter readings
    final private static int send_log_count = 600;//1000 records equals @160K non-compressed, 23K compressed, max transfer 2.7 seconds
    final private static int send_step_count = 600;
    final private static int send_heart_count = 600;
    final private static int send_treatments_count = 100;
    final private static int three_days_ms = 3*24*60*60*1000;
    //private static boolean doDeleteDB = false;//TODO remove once confirm not needed
    private boolean is_using_bt = false;
    private static int aggressive_backoff_timer = 120;
    private static volatile int reRequestDownloadApkCounter = 0;
    private volatile GoogleApiClient googleApiClient;
    private static long lastRequest = 0;
    private DataRequester mDataRequester = null;
    private static final int GET_CAPABILITIES_TIMEOUT_MS = 5000;
    private AsyncBenchmarkTester mAsyncBenchmarkTester = null;
    private static boolean bBenchmarkBgs = false;
    private static boolean bBenchmarkLogs = false;
    private static boolean bBenchmarkRandom = false;
    private static boolean bBenchmarkDup = false;
    private static boolean bInitPrefs = true;
    //Restart collector for change in the following received from phone in syncPrefData():
    private static final Set<String> restartCollectorPrefs = new HashSet<String>(Arrays.asList(
            new String[]{
                    "dex_collection_method", "share_key", "dex_txid", "use_transmiter_pl_bluetooth",
                    "use_rfduino_bluetooth", "automatically_turn_bluetooth_on", "bluetooth_excessive_wakelocks", "close_gatt_on_ble_disconnect", "bluetooth_frequent_reset", "bluetooth_watchdog"}
    ));

    //Sensor Step Counter variables
    private final static int SENS_STEP_COUNTER = android.hardware.Sensor.TYPE_STEP_COUNTER;
    //max batch latency is specified in microseconds
    private static final int BATCH_LATENCY_1s = 1000000;
    private static final int BATCH_LATENCY_400s = 400000000;
    //Steps counted in current session
    private int mSteps = 0;
    //Value of the step counter sensor when the listener was registered.
    //(Total steps are calculated from this value.)
    private int mCounterSteps = 0;
    //Steps counted by the step counter previously. Used to keep counter consistent across rotation
    //changes
    private int mPreviousCounterSteps = 0;
    private SensorManager mSensorManager;
    private static long last_movement_timestamp = 0;
    final private static String pref_last_movement_timestamp = "last_movement_timestamp";
    final private static String pref_msteps = "msteps";


    //@Getter
    public static volatile int apkBytesRead = -1;
    //@Getter
    public static volatile String apkBytesVersion = "";

    public class DataRequester extends AsyncTask<Void, Void, Void> {
        final String path;
        final byte[] payload;

        DataRequester(Context context, String thispath, byte[] thispayload) {
            path = thispath;
            payload = thispayload;
            if (JoH.quietratelimit("db-init",10)) {
                Sensor.InitDb(context);//ensure database has already been initialized
            }
            Log.d(TAG, "DataRequester DataRequester: " + thispath + " lastRequest:" + JoH.dateTimeText(lastRequest));
        }

        @Override
        protected Void doInBackground(Void... params) {
            final PowerManager.WakeLock wl = JoH.getWakeLock(getApplicationContext(), "data-requestor-background", 120000);
            try {
                // force reconnection if it is not present
                forceGoogleApiConnect();
                DataMap datamap;
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//KS
                boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false); //KS
                boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false); //KS
                String node_wearG5 = sharedPrefs.getString("node_wearG5", ""); //KS
                boolean sync_wear_logs = sharedPrefs.getBoolean("sync_wear_logs", false); //KS
                boolean sync_step_counter = sharedPrefs.getBoolean("use_wear_health", false); //KS
                boolean showSteps = sharedPrefs.getBoolean("showSteps", false);
                Log.d(TAG, "doInBackground enter enable_wearG5=" + enable_wearG5 + " force_wearG5=" + force_wearG5 + " node_wearG5=" + node_wearG5);//KS

                if (isCancelled()) {
                    Log.d(TAG, "doInBackground CANCELLED programmatically");
                    return null;
                }

                if (googleApiClient != null) {
                    final long timeout = JoH.tsl() + Constants.SECOND_IN_MS * 15;
                    while (!googleApiClient.isConnected() && JoH.tsl() < timeout) {
                        if (JoH.quietratelimit("gapi-reconnect", 15)) {
                            googleApiClient.connect();
                        }
                        Log.d(TAG, "Sleeping for connect, remaining: " + JoH.niceTimeScalar(JoH.msTill(timeout)));
                        JoH.threadSleep(1000);
                    }
                }

                if ((googleApiClient != null) && (googleApiClient.isConnected())) {
                    if (!path.equals(ACTION_RESEND) || (System.currentTimeMillis() - lastRequest > 20 * 1000)) { // enforce 20-second debounce period
                        lastRequest = System.currentTimeMillis();

                        //NodeApi.GetConnectedNodesResult nodes =
                        //        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                        if (localnode == null || (localnode != null && localnode.isEmpty())) setLocalNodeName();
                        CapabilityApi.GetCapabilityResult capabilityResult =
                                Wearable.CapabilityApi.getCapability(
                                        googleApiClient, CAPABILITY_PHONE_APP,
                                        CapabilityApi.FILTER_REACHABLE).await(GET_CAPABILITIES_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                        if (!capabilityResult.getStatus().isSuccess()) {
                            Log.e(TAG, "doInBackground Failed to get capabilities, status: " + capabilityResult.getStatus().getStatusMessage());
                            return null;
                        }
                        CapabilityInfo capabilityInfo = capabilityResult.getCapability();
                        int count = 0;
                        Node phoneNode = null;
                        if (capabilityInfo != null) {
                            phoneNode = updatePhoneSyncBgsCapability(capabilityInfo);
                            count = capabilityInfo.getNodes().size();
                        }
                        Log.d(TAG, "doInBackground connected.  CapabilityApi.GetCapabilityResult mPhoneNodeID=" + (phoneNode != null ? phoneNode.getId() : "") + " count=" + count + " localnode=" + localnode);//KS
                        if (count > 0) {
                            if (enable_wearG5) {
                                if (force_wearG5) {
                                    startBtService();
                                } else {
                                    stopBtService();
                                }
                            }

                            for (Node node : capabilityInfo.getNodes()) {

                                if (bInitPrefs) {
                                    Log.d(TAG, "doInBackground Request Phone's Preferences: WEARABLE_INITPREFS_PATH");
                                    sendMessagePayload(node, "WEARABLE_INITPREFS_PATH", WEARABLE_INITPREFS_PATH, null);
                                    sendMessagePayload(node, "WEARABLE_INITDB_PATH", WEARABLE_INITDB_PATH, null);
                                    bInitPrefs = false;
                                }
                                Log.d(TAG, "doInBackground path: " + path);
                                switch (path) {
                                    // simple send as is payloads
                                    case WEARABLE_REQUEST_APK:
                                    case WEARABLE_INITDB_PATH:
                                    case WEARABLE_INITTREATMENTS_PATH:
                                    case WEARABLE_REPLYMSG_PATH:
                                    case WEARABLE_G5BATTERY_PAYLOAD:
                                    case WEARABLE_SNOOZE_ALERT:
                                    case WEARABLE_PREF_DATA_PATH:
                                    case WEARABLE_LOCALE_CHANGED_PATH:
                                    case SYNC_BGS_PATH:
                                    case SYNC_LOGS_PATH:
                                    case SYNC_LOGS_REQUESTED_PATH:
                                    case SYNC_STEP_SENSOR_PATH:
                                        sendMessagePayload(node, path, path, payload);
                                        break;
                                    case SYNC_TREATMENTS_PATH:
                                        datamap = getWearTreatmentsData(send_treatments_count, last_send_previous_treatments, 0);
                                        if (datamap != null) {
                                            sendMessagePayload(node, "SYNC_TREATMENTS_PATH", SYNC_TREATMENTS_PATH, datamap.toByteArray());
                                        }
                                        break;
                                    case WEARABLE_RESEND_PATH:
                                        Log.d(TAG, "doInBackground WEARABLE_RESEND_PATH");
                                        sendMessagePayload(node, "WEARABLE_RESEND_PATH", path, payload);
                                        break;
                                    default://SYNC_ALL_DATA
                                        // this fall through is messy and non-deterministic for new paths
                                        if (path.startsWith(WEARABLE_REQUEST_APK)) {
                                            sendMessagePayload(node, path, path, payload);
                                        } else {

                                            Log.d(TAG, "doInBackground SYNC_ALL_DATA - or unknown path: " + path);
                                            if (sync_step_counter) {
                                                datamap = getWearStepSensorData(send_step_count, last_send_previous_step_sensor, 0);
                                                if (datamap != null) {
                                                    sendMessagePayload(node, "SYNC_STEP_SENSOR_PATH", SYNC_STEP_SENSOR_PATH, datamap.toByteArray());
                                                }
                                                datamap = getWearHeartSensorData(send_heart_count, last_send_previous_heart_sensor, 0);
                                                if (datamap != null) {
                                                    sendMessagePayload(node, "SYNC_HEART_SENSOR_PATH", SYNC_HEART_SENSOR_PATH, datamap.toByteArray());
                                                }
                                            }
                                            datamap = getWearTreatmentsData(send_treatments_count, last_send_previous_treatments, 0);
                                            if (datamap != null) {
                                                sendMessagePayload(node, "SYNC_TREATMENTS_PATH", SYNC_TREATMENTS_PATH, datamap.toByteArray());
                                            }
                                            if (enable_wearG5) {//KS
                                                if (!Ob1G5CollectionService.usingNativeMode()) {
                                                    datamap = getWearTransmitterData(send_bg_count, last_send_previous, 0);//KS 36 data for last 3 hours; 288 for 1 day
                                                }
                                                // fallback to using precalculated if our collection method doesn't appear to provide transmitter data or we know it doesn't
                                                if (datamap == null) {
                                                    datamap = getWearBgReadingData(send_bg_count, last_send_previous, 0);//KS 36 data for last 3 hours; 288 for 1 day
                                                    if (datamap == null) {
                                                        datamap = new DataMap(); // no readings but we need to update status in native mode
                                                    }
                                                    final boolean queue_drained = PersistentStore.getBoolean(PREF_QUEUE_DRAINED);
                                                    if (queue_drained)
                                                        PersistentStore.setBoolean(PREF_QUEUE_DRAINED, false); // TODO only set this when we get ack
                                                    datamap.putBoolean(PREF_QUEUE_DRAINED, queue_drained);
                                                    datamap.putString("dextime", Ob1G5StateMachine.extractDexTime());
                                                    final CalibrationState lastState = Ob1G5CollectionService.lastSensorState;
                                                    datamap.putInt("native_calibration_state", lastState != null ? lastState.getValue() : 0);
                                                    sendMessagePayload(node, "SYNC_BGS_PRECALCULATED_PATH", SYNC_BGS_PRECALCULATED_PATH, datamap.toByteArray());
                                                } else {
                                                    Log.d(TAG, "Sending transmitter data: " + datamap.size());
                                                    sendMessagePayload(node, "SYNC_BGS_PATH", SYNC_BGS_PATH, datamap.toByteArray());
                                                }
                                            }
                                            if (sync_wear_logs) {
                                                datamap = getWearLogData(send_log_count, last_send_previous_log, 0, -1);//UserError gen @10K messages just for G5CollectionService; UserError 2300 recs = @150K over @4 hrs when "scan cycle start" was logging / 5 secs.
                                                if (datamap != null) {
                                                    byte[] compressPayload = JoH.compressBytesToBytesGzip((datamap.toByteArray()));
                                                    sendMessagePayload(node, "SYNC_LOGS_PATH", SYNC_LOGS_PATH, compressPayload);
                                                }
                                            }
                                            if (PersistentStore.getBoolean(G5_BATTERY_WEARABLE_SEND)) {
                                                PersistentStore.setBoolean(G5_BATTERY_WEARABLE_SEND, false);
                                                sendPersistentStore();
                                            }
                                            if (PersistentStore.getBoolean(WEARABLE_RESEND_PATH)) {
                                                Log.d(TAG, "doInBackground WEARABLE_RESEND_PATH");
                                                sendMessagePayload(node, "WEARABLE_RESEND_PATH", path, payload);
                                            }
                                            break;
                                        // end default case
                                        }
                                }
                            }
                        } else {
                            if (enable_wearG5) {//KS
                                Log.d(TAG, "doInBackground connected but getConnectedNodes returns 0.  Start BT service");
                                startBtService();
                            }
                        }
                    } else {
                        Log.d(TAG, "Debounce limit hit - not sending");
                    }
                } else {
                    Log.d(TAG, "Not connected for sending: api " + ((googleApiClient == null) ? "is NULL!" : "not null"));
                    if (googleApiClient != null) {
                        googleApiClient.connect();
                    } else {
                        googleApiConnect();
                    }
                }
                if (showSteps || sync_step_counter) {
                    if (JoH.ratelimit("step-sensor-restart", 60)) {
                        restartMeasurement();//restart measurements on new day
                    }
                }
                return null;
            } finally {
                JoH.releaseWakeLock(wl);
            }
        }

        private boolean runBenchmarkTest(Node node, String pathdesc, String path, byte[] payload, boolean bDuplicateTest) {
            if (mAsyncBenchmarkTester != null) {
                Log.d(TAG, "Benchmark: runAsyncBenchmarkTester mAsyncBenchmarkTester != null lastRequest:" + JoH.dateTimeText(lastRequest));
                if (mAsyncBenchmarkTester.getStatus() != AsyncTask.Status.FINISHED) {
                    Log.d(TAG, "Benchmark: mAsyncBenchmarkTester let process complete, do not start new process.");
                    //mAsyncBenchmarkTester.cancel(true);
                }
                //mAsyncBenchmarkTester = null;
            } else {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    Log.d(TAG, "Benchmark: runAsyncBenchmarkTester SDK < M call execute lastRequest:" + JoH.dateTimeText(lastRequest));
                    mAsyncBenchmarkTester = (AsyncBenchmarkTester) new AsyncBenchmarkTester(Home.getAppContext(), node, pathdesc, path, payload, bDuplicateTest).execute();
                } else {
                    Log.d(TAG, "Benchmark: runAsyncBenchmarkTester SDK >= M call executeOnExecutor lastRequest:" + JoH.dateTimeText(lastRequest));
                    mAsyncBenchmarkTester = (AsyncBenchmarkTester) new AsyncBenchmarkTester(Home.getAppContext(), node, pathdesc, path, payload, bDuplicateTest).executeOnExecutor(xdrip.executor);
                }
            }
            return false;
        }

        private void sendMessagePayload(Node node, String pathdesc, final String path, byte[] payload) {
            Log.d(TAG, "Benchmark: doInBackground sendMessagePayload " + pathdesc + "=" + path + " nodeID=" + node.getId() + " nodeName=" + node.getDisplayName() + ((payload != null) ? (" payload.length=" + payload.length) : ""));

            //ORIGINAL ASYNC METHOD
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, payload);
            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    if (!sendMessageResult.getStatus().isSuccess()) {
                        Log.e(TAG, "sendMessagePayload ERROR: failed to send request " + path + " Status=" + sendMessageResult.getStatus().getStatusMessage());
                    } else {
                        Log.d(TAG, "sendMessagePayload Sent request " + node.getDisplayName() + " " + path + " Status=: " + sendMessageResult.getStatus().getStatusMessage());
                    }
                }
            });

            //TEST**************************************************************************
            DataMap datamap;
            if (bBenchmarkBgs && path.equals(SYNC_BGS_PATH)) {
                //bBenchmarkBgs = runBenchmarkTest(node, pathdesc+"_BM", path+"_BM", payload, bBenchmarkDup);
                datamap = getWearTransmitterData(1000, 0, 0);//generate 1000 records of test data
                if (datamap != null) {
                    bBenchmarkBgs = runBenchmarkTest(node, pathdesc + "_BM", path + "_BM", datamap.toByteArray(), false);
                }
            } else if (bBenchmarkLogs && path.equals(SYNC_LOGS_PATH)) {
                //bBenchmarkLogs = runBenchmarkTest(node, pathdesc+"_BM", path+"_BM", payload, bBenchmarkDup);
                datamap = getWearLogData(1000, 0, 0, -1);//generate 1000 records of test data
                if (datamap != null) {
                    bBenchmarkLogs = runBenchmarkTest(node, pathdesc + "_BM", path + "_BM", datamap.toByteArray(), false);
                }
            }
            //Random Test
            if (bBenchmarkRandom) {
                final byte[] randomBytes = new byte[200000];
                ThreadLocalRandom.current().nextBytes(randomBytes);
                bBenchmarkRandom = runBenchmarkTest(node, pathdesc + "_BM_RAND", path + "_BM_RAND", randomBytes, false);
                Log.i(TAG, "Benchmark: DONE!");
            }
            //******************************************************************************
        }

        @Override
        protected void onCancelled() {
            Log.d(TAG, "DataRequester AsyncTask doInBackground was cancelled");
        }
    }


    //******************************************************************************
    // Run Test in background
    //******************************************************************************
    public class AsyncBenchmarkTester extends AsyncTask<Void, Void, Void> {
        final Node node;
        final String pathdesc;
        final String path;
        final byte[] payload;
        final boolean bDuplicate;

        AsyncBenchmarkTester(Context context, Node thisnode, String thispathdesc, String thispath, byte[] thispayload, boolean thisbduplicate) {
            node = thisnode;
            pathdesc = thispathdesc;
            path = thispath;
            payload = thispayload;
            bDuplicate = thisbduplicate;
            Log.d(TAG, "AsyncBenchmarkTester: " + thispath);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (payload != null) {
                byte[] payloadTest;

                //Send Original Payload
                sendMessagePayloadTest(node, pathdesc, path, payload);
                payloadTest = compressPayload(pathdesc, payload);
                sendMessagePayloadTest(node, pathdesc + "_COMPRESS ", path + "_COMPRESS", payloadTest);
                //Send Original Duplicate
                if (bDuplicate) {
                    payloadTest = makeDuplucateByteArray(payload);
                    sendMessagePayloadTest(node, pathdesc + "_DUP ", path + "_DUP", payloadTest);
                    payloadTest = compressPayload(pathdesc, payloadTest);
                    sendMessagePayloadTest(node, pathdesc + "_DUP_COMPRESS ", path + "_DUP_COMPRESS", payloadTest);
                }

                //Test to be sure message can be delivered of this size.  It should show up in the phone log.
                /*
                Log.i(TAG, "Benchmark: " + pathdesc + "_COMPRESS" + " sendMessage Async check msg is delivered with len=" + comprPayload.length * 50);
                PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path + "_DUP", comprPayload);//Async
                result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Benchmark: ERROR: failed to ASYNC sendMessage " + pathdesc + "_COMPRESS" + " Status=" + sendMessageResult.getStatus().getStatusMessage());
                        } else {
                            Log.i(TAG, "Benchmark: Sent request " + pathdesc + "_COMPRESS" + " Status=" + sendMessageResult.getStatus().getStatusMessage());
                        }
                    }
                });
                */
            }
            return null;
        }

        private void sendMessagePayloadTest(Node node, String pathdesc, String path, byte[] payload) {
            double benchmark_time = 0;

            //JoH.benchmark(null);
            benchmark_time = ts();
            for (int i = 0; i < 50; i++) {
                sendMessageTest(node, pathdesc + " " + i + " ", path, payload);
            }
            //JoH.benchmark(pathdesc + " Send 50x len=" + payload.length*50);
            Log.i(TAG, "Benchmark: " + pathdesc + " Send 50x len=" + payload.length * 50 + " " + (ts() - benchmark_time) + " ms");
        }

        private void sendMessageTest(Node node, String pathdesc, String path, byte[] payload) {
            JoH.benchmark(null);
            Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, payload).await();//Synchronous
            JoH.benchmark(pathdesc + " sendMessage len=" + payload.length);
        }

        private byte[] compressPayload(String pathdesc, byte[] payload) {
            byte[] comprPayload;
            JoH.benchmark(null);
            comprPayload = JoH.compressBytesToBytesGzip((payload));
            JoH.benchmark(pathdesc + " JoH.compressBytesToBytes from length=" + payload.length + " to length=" + comprPayload.length);
            return comprPayload;
        }

        private byte[] concatPayload(byte[] a, byte[] b) {
            int aLen = a.length;
            int bLen = b.length;
            byte[] c = new byte[aLen + bLen];
            System.arraycopy(a, 0, c, 0, aLen);
            System.arraycopy(b, 0, c, aLen, bLen);
            return c;
        }

        private byte[] makeDuplucateByteArray(byte[] payload) {
            byte[] payloadTest;
            byte[] payloadTestSend;

            //Dup payload to create large array of 200k
            payloadTest = payload;
            payloadTestSend = payloadTest;
            while (payloadTest.length < 200000) {
                payloadTest = concatPayload(payloadTest, payloadTest);
                if (payloadTest.length > 200000) {
                    System.arraycopy(payloadTestSend, 0, payloadTest, 0, 100000);
                    break;
                }
                payloadTestSend = payloadTest;
            }

            return payloadTestSend;
        }
    }

    private synchronized DataMap getWearBgReadingData(int count, long last_send_time, int min_count) {
        forceGoogleApiConnect();

        Log.d(TAG, "getWearBgReadingData last_send_time:" + JoH.dateTimeText(last_send_time));

        BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            Log.d(TAG, "getWearBgReadingData last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
        }

        if (last_bg != null && last_send_time <= last_bg.timestamp) {//startTime
            long last_send_success = last_send_time;
            Log.d(TAG, "getWearBgData last_send_time < last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
            final List<BgReading> graph_bgs = BgReading.latestForGraphAsc(count, last_send_time);
            if (!graph_bgs.isEmpty() && graph_bgs.size() > min_count) {
                //Log.d(TAG, "getWearBgData count = " + graph_bgs.size());
                final DataMap entries = dataMap(last_bg);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (BgReading bg : graph_bgs) {
                    dataMaps.add(dataMap(bg));
                    last_send_success = bg.timestamp;
                    //Log.d(TAG, "getWearBgData bg getId:" + bg.getId() + " raw_data:" + bg.raw_data + " filtered_data:" + bg.filtered_data + " timestamp:" + bg.timestamp + " uuid:" + bg.uuid);
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                Log.i(TAG, "getWearBgReadingData SYNCED BGs up to " + JoH.dateTimeText(last_send_success) + " count = " + graph_bgs.size());
                return entries;
            } else
                Log.i(TAG, "getWearBgReading SYNCED BGs up to " + JoH.dateTimeText(last_send_success) + " count = 0");
        }
        return null;
    }


    private synchronized DataMap getWearTransmitterData(int count, long last_send_time, int min_count) {//KS
        forceGoogleApiConnect();

        Log.d(TAG, "getWearTransmitterData last_send_time:" + JoH.dateTimeText(last_send_time));

        TransmitterData last_bg = TransmitterData.last();
        if (last_bg != null) {
            Log.d(TAG, "getWearTransmitterData last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
        }

        if (last_bg != null && last_send_time <= last_bg.timestamp) {//startTime
            long last_send_success = last_send_time;
            Log.d(TAG, "getWearTransmitterData last_send_time < last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
            List<TransmitterData> graph_bgs = TransmitterData.latestForGraphAsc(count, last_send_time);
            if (!graph_bgs.isEmpty() && graph_bgs.size() > min_count) {
                //Log.d(TAG, "getWearTransmitterData count = " + graph_bgs.size());
                DataMap entries = dataMap(last_bg);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (TransmitterData bg : graph_bgs) {
                    dataMaps.add(dataMap(bg));
                    last_send_success = bg.timestamp;
                    //Log.d(TAG, "getWearTransmitterData bg getId:" + bg.getId() + " raw_data:" + bg.raw_data + " filtered_data:" + bg.filtered_data + " timestamp:" + bg.timestamp + " uuid:" + bg.uuid);
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                Log.i(TAG, "getWearTransmitterData SYNCED BGs up to " + JoH.dateTimeText(last_send_success) + " count = " + graph_bgs.size());
                return entries;
            } else
                Log.i(TAG, "getWearTransmitterData SYNCED BGs up to " + JoH.dateTimeText(last_send_success) + " count = 0");
        }
        return null;
    }

    private synchronized DataMap getWearLogData(int count, long last_send_time, int min_count, long syncLogsRequested) {
        forceGoogleApiConnect();

        min_count = 0; // FORCE ALWAYS SEND // TODO revisit this
        Log.d(TAG, "getWearLogData last_send_time:" + JoH.dateTimeText(last_send_time) + " max count=" + count + " min_count=" + min_count + " syncLogsRequested=" + syncLogsRequested);

        UserError last_log = UserError.last();
        if (last_log != null) {
            Log.d(TAG, "getWearLogData last_log.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
        }

        if (last_log != null && last_send_time <= last_log.timestamp) {//startTime
            long last_send_success = last_send_time;
            Log.d(TAG, "getWearLogData last_send_time < last_bg.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
            List<UserError> logs = UserError.latestAsc(count, last_send_time);
            if (!logs.isEmpty() && logs.size() > min_count) {
                //Log.d(TAG, "getWearLogData count = " + logs.size());
                DataMap entries = dataMap(last_log);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(logs.size());
                for (UserError log : logs) {
                    dataMaps.add(dataMap(log));
                    last_send_success = (long)log.timestamp;
                    //Log.d(TAG, "getWearLogData set last_send_sucess:" + JoH.dateTimeText(last_send_sucess) + " Log:" + log.toString());
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putLong("syncLogsRequested", syncLogsRequested);
                entries.putDataMapArrayList("entries", dataMaps);
                Log.i(TAG, "getWearLogData SYNCED logs up to " + JoH.dateTimeText(last_send_success) + " count = " + logs.size() + " syncLogsRequested=" + syncLogsRequested);
                return entries;
            } else
                Log.i(TAG, "getWearLogData SYNCED logs up to " + JoH.dateTimeText(last_send_success) + " count = 0" + " syncLogsRequested=" + syncLogsRequested);
        }
        return null;
    }


    private synchronized DataMap getWearStepSensorData(int count, long last_send_time, int min_count) {//final int sensorType, final int accuracy, final long timestamp, final float[] values) {
        forceGoogleApiConnect();

        Log.d(TAG, "getWearStepSensorData last_send_time:" + JoH.dateTimeText(last_send_time));

        PebbleMovement last_log = PebbleMovement.last();
        if (last_log != null) {
            Log.d(TAG, "getWearStepSensorData last_log.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
        }
        else {
            Log.d(TAG, "getWearStepSensorData PebbleMovement.last() = null:");
        }

        if (last_log != null && last_send_time <= last_log.timestamp) {//startTime
            long last_send_success = last_send_time;
            Log.d(TAG, "getWearStepSensorData last_send_time < last_bg.timestamp:" + JoH.dateTimeText((long) last_log.timestamp));
            List<PebbleMovement> logs = PebbleMovement.latestForGraph(count, last_send_time);
            if (!logs.isEmpty() && logs.size() > min_count) {
                //Log.d(TAG, "getWearStepSensorData count = " + logs.size());
                DataMap entries = dataMap(last_log);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(logs.size());
                for (PebbleMovement log : logs) {
                    dataMaps.add(dataMap(log));
                    last_send_success = (long)log.timestamp;
                    //Log.d(TAG, "getWearStepSensorData set last_send_sucess:" + JoH.dateTimeText(last_send_success) + " pw.metric: " + log.metric + " pw.timestamp: " + JoH.dateTimeText(log.timestamp));
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                //Log.d(TAG, "getWearStepSensorData  entries:" + entries);
                Log.i(TAG, "getWearStepSensorData SYNCED steps up to " + JoH.dateTimeText(last_send_success) + " count = " + logs.size());
                return entries;
            } else
                Log.i(TAG, "getWearStepSensorData SYNCED steps up to " + JoH.dateTimeText(last_send_success) + " count = 0");
        }
        return null;
    }

    private synchronized DataMap getWearTreatmentsData(int count, long last_send_time, int min_count) {
        forceGoogleApiConnect();

        Log.d(TAG, "getWearTreatmentsData last_send_time:" + JoH.dateTimeText(last_send_time) + " max count=" + count + " min_count=" + min_count);

        Treatments last_log = Treatments.lastSystime();
        if (last_log != null) {
            Log.d(TAG, "getWearTreatmentsData last systimestamp: " + last_log.systimestamp + " " + JoH.dateTimeText((long) last_log.systimestamp));
        }

        if (last_log != null && last_log.systimestamp > 0 && last_send_time <= last_log.systimestamp) {//startTime
            long last_send_success = last_send_time;
            Log.d(TAG, "getWearTreatmentsData last_send_time < last_log.timestamp:" + JoH.dateTimeText((long) last_log.systimestamp));
            List<Treatments> logs = Treatments.latestForGraphSystime(count, last_send_time);
            if (!logs.isEmpty() && logs.size() > min_count) {
                //Log.d(TAG, "getWearLogData count = " + logs.size());
                DataMap entries = dataMap(last_log);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(logs.size());
                for (Treatments log : logs) {
                    dataMaps.add(dataMap(log));
                    last_send_success = (long)log.systimestamp;
                    //Log.d(TAG, "getWearTreatmentsData set last_send_sucess:" + JoH.dateTimeText(last_send_sucess) + " Log:" + log.toString());
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                Log.i(TAG, "getWearTreatmentsData SYNCED treatments up to " + JoH.dateTimeText(last_send_success) + " count = " + logs.size());
                return entries;
            } else
                Log.i(TAG, "getWearTreatmentsData SYNCED treatments up to " + JoH.dateTimeText(last_send_success) + " count = 0");
        }
        return null;
    }

    private void sendPrefSettings() {//KS

        Log.d(TAG, "sendPrefSettings enter");
        forceGoogleApiConnect();
        DataMap dataMap = new DataMap();
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
        String node_wearG5 = mPrefs.getString("node_wearG5", "");
        String dex_txid = mPrefs.getString("dex_txid", "ABCDEF");
        boolean show_wear_treatments = mPrefs.getBoolean("show_wear_treatments", false);

        if (localnode == null || (localnode != null && localnode.isEmpty())) setLocalNodeName();
        Log.d(TAG, "sendPrefSettings enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " localnode:" + localnode + " dex_txid:" + dex_txid + " show_wear_treatments:" + show_wear_treatments);
        dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
        dataMap.putBoolean("enable_wearG5", enable_wearG5);
        dataMap.putBoolean("force_wearG5", force_wearG5);
        if (force_wearG5) {
            dataMap.putString("node_wearG5", localnode);
        } else {
            if (node_wearG5.equals(localnode)) {
                dataMap.putString("node_wearG5", "");
            } else {
                dataMap.putString("node_wearG5", node_wearG5);
            }
        }
        dataMap.putString("dex_txid", dex_txid);
        dataMap.putInt("bridge_battery", mPrefs.getInt("bridge_battery", -1));//Used in DexCollectionService
        dataMap.putInt("nfc_sensor_age", mPrefs.getInt("nfc_sensor_age", -1));//Used in DexCollectionService for LimiTTer
        dataMap.putBoolean("bg_notifications_watch", mPrefs.getBoolean("bg_notifications", false));
        dataMap.putBoolean("persistent_high_alert_enabled_watch", mPrefs.getBoolean("persistent_high_alert_enabled", false));
        dataMap.putBoolean("show_wear_treatments", show_wear_treatments);
        sendData(WEARABLE_PREF_DATA_PATH, dataMap.toByteArray());

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (!node_wearG5.equals(dataMap.getString("node_wearG5", ""))) {
            Log.d(TAG, "sendPrefSettings save to SharedPreferences - node_wearG5:" + dataMap.getString("node_wearG5", ""));
            prefs.putString("node_wearG5", dataMap.getString("node_wearG5", ""));
            prefs.apply();
        }
    }

    private DataMap dataMap(Treatments log) {
        DataMap dataMap = new DataMap();
        //String json = log.toS();
        //Log.d(TAG, "dataMap PebbleMovement GSON: " + json);
        //dataMap.putString("entry", json);
        //dataMap.putLong("timestamp", log.timestamp);
        String notes = log.uuid + " uuid " + log.notes;
        Log.d(TAG, "dataMap Treatments notes:" + notes);
        dataMap.putString("entry", notes);
        dataMap.putLong("timestamp", log.systimestamp);
        return dataMap;
    }

    private DataMap dataMap(PebbleMovement log) {
        DataMap dataMap = new DataMap();
        String json = log.toS();
        //Log.d(TAG, "dataMap PebbleMovement GSON: " + json);
        dataMap.putString("entry", json);
        return dataMap;
    }

    private DataMap dataMap(UserError bg) {
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        //Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("entry", json);
        return dataMap;
    }

    private DataMap dataMap(TransmitterData bg) {//KS
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private DataMap dataMap(BgReading bg) {
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private void requestData() {
        if (JoH.ratelimit("resend-request",60)) {
            sendData(WEARABLE_RESEND_PATH, null);
        }
    }

    private synchronized void sendData(String path, byte[] payload) {
        if (path == null) return;
        if (mDataRequester != null) {
            Log.d(TAG, "sendData DataRequester != null lastRequest:" + JoH.dateTimeText(lastRequest));
            if (mDataRequester.getStatus() != AsyncTask.Status.FINISHED) {
                Log.d(TAG, "sendData Should be canceled?  Let run 'til finished.");
                //mDataRequester.cancel(true);
            }
            //mDataRequester = null;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            Log.d(TAG, "sendData SDK < M call execute lastRequest:" + JoH.dateTimeText(lastRequest));
            mDataRequester = (DataRequester) new DataRequester(this, path, payload).execute();
        } else {
            Log.d(TAG, "sendData SDK >= M call executeOnExecutor lastRequest:" + JoH.dateTimeText(lastRequest));
            // TODO xdrip executor
            mDataRequester = (DataRequester) new DataRequester(this, path, payload).executeOnExecutor(xdrip.executor);
        }
    }

    private void googleApiConnect() {
        if (googleApiClient != null) {
            // Remove old listener(s)
            try {
                Wearable.ChannelApi.removeListener(googleApiClient, this);
            } catch (Exception e) {
                //
            }
            try {
                Wearable.MessageApi.removeListener(googleApiClient, this);
            } catch (Exception e) {
                //
            }
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    private void forceGoogleApiConnect() {
        if ((googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) || googleApiClient == null) {
            try {
                Log.d(TAG, "forceGoogleApiConnect: forcing google api reconnection");
                googleApiConnect();
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                //
            }
        }
    }

    @Override
    public void onPeerConnected(Node peer) {//Deprecated with BIND_LISTENER
        super.onPeerConnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerConnected peer name & ID: " + name + "|" + id);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sendPrefSettings();
        if (mPrefs.getBoolean("enable_wearG5", false) && !mPrefs.getBoolean("force_wearG5", false)) {
            stopBtService();
            ListenerService.requestData(this);
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {//Deprecated with BIND_LISTENER
        super.onPeerDisconnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerDisconnected peer name & ID: " + name + "|" + id);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (mPrefs.getBoolean("enable_wearG5", false)) {
            startBtService();
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate entered");
        Context context = getApplicationContext();
        Home.setAppContext(context);
        xdrip.checkAppContext(context);
        Sensor.InitDb(context);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        listenForChangeInSettings();
        setupStepSensor();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand entered");
        final PowerManager.WakeLock wl = JoH.getWakeLock("watchlistener-onstart",60000);
        last_send_previous = PersistentStore.getLong(pref_last_send_previous); // 0 if undef
        last_send_previous_log = PersistentStore.getLong(pref_last_send_previous_log); // 0 if undef
        last_send_previous_step_sensor = PersistentStore.getLong(pref_last_send_previous_step_sensor); // 0 if undef
        last_send_previous_heart_sensor = PersistentStore.getLong(pref_last_send_previous_heart_sensor); // 0 if undef
        last_send_previous_treatments = PersistentStore.getLong(pref_last_send_previous_treatments); // 0 if undef
        is_using_bt = DexCollectionType.hasBluetooth();
        if (intent != null && ACTION_RESEND.equals(intent.getAction())) {
            googleApiConnect();
            requestData();
        } else if (intent != null && ACTION_SENDDATA.equals(intent.getAction())) {
            final Bundle bundle = intent.getExtras();
            sendData(bundle.getString(WEARABLE_FIELD_SENDPATH), bundle.getByteArray(WEARABLE_FIELD_PAYLOAD));
        }
        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    final private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {//KS
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            // TODO can use equals() instead of compareTo()
            Log.d(TAG, "OnSharedPreferenceChangeListener entered key=" + ((key != null && !key.isEmpty()) ? key : ""));
            if(key.compareTo("enable_wearG5") == 0 || key.compareTo("force_wearG5") == 0 || key.compareTo("node_wearG5") == 0) {
                Log.i(TAG, "OnSharedPreferenceChangeListener sendPrefSettings and processConnect for key=" + key);
                sendPrefSettings();
                processConnect();
            }
            else if(key.compareTo("bridge_battery") == 0 || key.compareTo("nfc_sensor_age") == 0 ||
                    key.compareTo("bg_notifications") == 0 || key.compareTo("persistent_high_alert_enabled") == 0 ||
                    key.compareTo("show_wear_treatments") == 0){
                Log.d(TAG, "OnSharedPreferenceChangeListener sendPrefSettings for key=" + key);
                sendPrefSettings();
            }
            else if (key.compareTo("use_wear_health") == 0 || key.compareTo("showSteps") == 0 || key.compareTo("step_delay_time") == 0
                    || key.equals("use_wear_heartrate")) {
                setupStepSensor();
            }
            else if (key.compareTo("sync_wear_logs") == 0) {
                last_send_previous_log = JoH.tsl();
                PersistentStore.setLong(pref_last_send_previous_log, last_send_previous_log);
            }
            /*else if (key.compareTo("show_wear_treatments") == 0) {
                Log.d(TAG, "OnSharedPreferenceChangeListener sendPrefSettings for key=" + key);
                Context context = xdrip.getAppContext();
                showTreatments(context, "all");
            }*/
            else if (key.compareTo("overrideLocale") == 0) {
                if (prefs.getBoolean("overrideLocale", false)) {
                    Log.d(TAG, "overrideLocale true; Request phone locale");
                    sendData(WEARABLE_LOCALE_CHANGED_PATH, null);
                }
            }
            else {//if(key.compareTo("dex_txid") == 0 || key.compareTo(DexCollectionType.DEX_COLLECTION_METHOD) == 0){
                //Restart collector for change in the following received from phone in syncPrefData():
                //DexCollectionType.DEX_COLLECTION_METHOD - dex_collection_method, share_key or dex_txid
                //Advanced BT Settings:
                //use_transmiter_pl_bluetooth
                //use_rfduino_bluetooth, ...
                if (restartCollectorPrefs.contains(key)) {
                    Log.d(TAG, "OnSharedPreferenceChangeListener restartCollectorPrefs requires collector restart key=" + key);
                    stopBtService();
                }
                processConnect();
            }
        }
    };

    private void listenForChangeInSettings() {//KS
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    public static void resetDatabase() {
        Sensor.DeleteAndInitDb(xdrip.getAppContext());
        PersistentStore.setLong(pref_last_send_previous, 0);
        PersistentStore.setLong(pref_last_send_previous_log, 0);
        PersistentStore.setLong(pref_last_send_previous_step_sensor, 0);
        PersistentStore.setLong(pref_last_send_previous_heart_sensor, 0);
        PersistentStore.setLong(pref_last_send_previous_treatments, 0);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {



        final Context context = getApplicationContext();
        if (mPrefs == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(context);//KS
        }
        String msg;
        DataMap dataMap = null;

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                final String path = event.getDataItem().getUri().getPath();

                if (!path.startsWith(WEARABLE_APK_DELIVERY)) {
                    try {
                        dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    } catch (Exception e) {
                        //
                    }
                }

                Log.d(TAG, "onDataChanged top path=" + path + " DataMap=" + dataMap);
                if (path.equals(OPEN_SETTINGS)) {
                    //TODO: OpenSettings
                    JoH.startActivity(NWPreferences.class);

                } else if (path.equals(NEW_STATUS_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    boolean showExternalStatus = mPrefs.getBoolean("showExternalStatus", true);
                    Log.d(TAG, "onDataChanged NEW_STATUS_PATH=" + path + " showExternalStatus=" + showExternalStatus);

                    if (showExternalStatus) {
                        sendLocalMessage("status", dataMap);
                        String externalStatusString = dataMap.getString("externalStatusString");
                        PersistentStore.setString("remote-status-string",externalStatusString);
                        CustomComplicationProviderService.refresh();
                    }
                } else if (path.equals(WEARABLE_TOAST_LOCAL_NOTIFICATON)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    sendLocalMessage("msg", dataMap);
                    Log.d(TAG, "onDataChanged WEARABLE_TOAST_LOCAL_NOTIFICATON=" + path);
                } else if (path.equals(WEARABLE_DATA_PATH)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged WEARABLE_DATA_PATH=" + path);
                    if (resetDataToLatest(dataMap, getApplicationContext())) {
                        Log.d(TAG, "onDataChanged dataMap reset to watch BgReading.Last()");
                    }
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    DataMap stepsDataMap = BgSendQueue.getSensorSteps(mPrefs);
                    if (stepsDataMap != null) {
                        messageIntent.putExtra("steps", stepsDataMap.toBundle());
                    }
                    messageIntent.putExtra("data", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
                    if (!mPrefs.getBoolean("enable_wearG5", false) || !mPrefs.getBoolean("force_wearG5", false)) {
                        Inevitable.task("sync-all-data", 2000, () -> ListenerService.SendData(context, ListenerService.SYNC_ALL_DATA, null));
                    }
                } else if (path.equals(WEARABLE_TREATMENT_PAYLOAD)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent intent = new Intent(getApplicationContext(), Simulation.class);
                    intent.putExtra(WEARABLE_TREATMENT_PAYLOAD, dataMap.toBundle());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                } else if (path.equals(WEARABLE_TOAST_NOTIFICATON)) {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent intent = new Intent(getApplicationContext(), Simulation.class);
                    intent.putExtra(path, dataMap.toBundle());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                } else if (path.equals(WEARABLE_SNOOZE_ALERT)) {
                    Log.d(TAG, "onDataChanged WEARABLE_SNOOZE_ALERT=" + path);
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    if (dataMap != null) {
                        msg = dataMap.getString("repeatTime", "");
                        int snooze;
                        try {
                            snooze = Integer.parseInt(msg);
                        } catch (NumberFormatException e) {
                            snooze = 30;
                        }
                        Log.d(TAG, "Received wearable: snooze payload: " + snooze);
                        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), snooze, true);
                        sendLocalToast(getResources().getString(R.string.alert_snoozed_by_phone), Toast.LENGTH_SHORT);
                    }
                } else if (path.equals(SYNC_DB_PATH)) {//KS  || path.equals(RESET_DB_PATH)
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH=" + path);
                    final PowerManager.WakeLock wl = JoH.getWakeLock(getApplicationContext(), "watchlistener-SYNC_DB_PATH",120000);
                    //BgReading.deleteALL();
                    //Calibration.deleteALL();
                    long retainFrom = Pref.getBooleanDefaultFalse("extra_status_stats_24h")?last_send_previous-three_days_ms: StatsResult.getTodayTimestamp();//retain 3 days for Table Views
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH delete BgReading and Calibration < retainFrom=" + JoH.dateTimeText(retainFrom));
                    BgReading.cleanup(retainFrom);
                    Calibration.cleanup(retainFrom);
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH delete UserError < last_send_previous_log=" + JoH.dateTimeText(last_send_previous_log));
                    UserError.cleanup(last_send_previous_log);
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH delete TransmitterData < last_send_previous=" + JoH.dateTimeText(last_send_previous));
                    TransmitterData.cleanup(last_send_previous);
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH delete PebbleMovement < last_send_previous=" + JoH.dateTimeText(last_send_previous_step_sensor));
                    PebbleMovement.cleanup(2);//retain 2 days
                    HeartRate.cleanup(2);//retain 2 days
                    Treatments.cleanup(last_send_previous_treatments-three_days_ms);//retain 3 day for Table Views
                    BloodTest.cleanup(3);
                    JoH.releaseWakeLock(wl);
                } else if (path.equals(RESET_DB_PATH)) {//KS
                    Log.d(TAG, "onDataChanged RESET_DB_PATH=" + path);
                    final PowerManager.WakeLock wl = JoH.getWakeLock(getApplicationContext(), "watchlistener-RESET_DB_PATH",120000);
                   /* Sensor.DeleteAndInitDb(getApplicationContext());
                    PersistentStore.setLong(pref_last_send_previous, 0);
                    PersistentStore.setLong(pref_last_send_previous_log, 0);
                    PersistentStore.setLong(pref_last_send_previous_step_sensor, 0);*/
                    resetDatabase(); // remotely callabale method to do the above
                    /* TODO remove once confirm not needed
                    if (isSafeToDeleteDB()) {
                        doDeleteDB = false;
                        Sensor.DeleteAndInitDb(getApplicationContext());
                        PersistentStore.setLong(pref_last_send_previous, 0);
                        PersistentStore.setLong(pref_last_send_previous_log, 0);
                    }
                    else {
                        doDeleteDB = true;
                        Log.d(TAG, "onDataChanged RESET_DB_PATH=" + path + " Unable to delete wear DB; wear data needs syncing.");
                    }*/
                    JoH.releaseWakeLock(wl);
                } else if (path.equals(SYNC_LOGS_PATH)) {
                    Log.d(TAG, "onDataChanged SYNC_LOGS_PATH=" + path);
                    //requestData();
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    if (dataMap != null) {
                        msg = dataMap.getString("syncLogsRequested", "");
                        int syncLogsRequested;
                        try {
                            syncLogsRequested = Integer.parseInt(msg);
                        } catch (NumberFormatException e) {
                            syncLogsRequested = 0;
                        }
                        Log.d(TAG, "onDataChanged Received SYNC_LOGS_PATH request syncLogsRequested=" + syncLogsRequested);
                        dataMap = getWearLogData(send_log_count, last_send_previous_log, (send_log_count / 3), syncLogsRequested);
                        if (dataMap != null) {
                            Log.i(TAG, "onDataChanged SYNC_LOGS_PATH Request from last log processed " + JoH.dateTimeText(last_send_previous_log));
                            sendData(SYNC_LOGS_PATH, dataMap.toByteArray());
                        }
                        else {
                            Log.d(TAG, "SYNC_LOGS_PATH received! No outstanding logs! ACTION_SYNC_LOGS request completed!! Ongoing requests syncLogsRequested=" + syncLogsRequested);
                            if (syncLogsRequested > 0) {
                                sendSyncRequested(SYNC_LOGS_REQUESTED_PATH, syncLogsRequested);
                            }
                        }
                    }
                } else if (path.equals(CLEAR_LOGS_PATH)) {
                    Log.d(TAG, "onDataChanged CLEAR_LOGS_PATH=" + path);
                    try {
                        UserError.cleanup();
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChanged CLEAR_LOGS_PATH exception on UserError ", e);
                    }
                } else if (path.equals(CLEAR_TREATMENTS_PATH)) {
                    Log.d(TAG, "onDataChanged CLEAR_TREATMENTS_PATH=" + path + " last_send_previous_treatments=" + JoH.dateTimeText(last_send_previous_treatments));
                    try {
                        Treatments.cleanup(last_send_previous_treatments);
                    } catch (Exception e) {
                        Log.e(TAG, "onDataChanged CLEAR_TREATMENTS_PATH exception on Treatments ", e);
                    }
                } else if (path.equals(START_COLLECTOR_PATH)) {
                    Log.d(TAG, "onDataChanged START_COLLECTOR_PATH=" + path);
                    stopBtService();
                    if (processConnect()) {
                        msg = getResources().getString(R.string.notify_collector_started, DexCollectionType.getDexCollectionType());
                        sendReplyMsg (msg, 0, path, true, Toast.LENGTH_SHORT);
                    }
                } else if (path.equals(STATUS_COLLECTOR_PATH)) {
                    Log.d(TAG, "onDataChanged path=" + path);
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    G5CollectionService.getBatteryStatusNow = dataMap.getBoolean("getBatteryStatusNow", false);
                    Ob1G5CollectionService.getBatteryStatusNow = dataMap.getBoolean("getBatteryStatusNow", false);
                    sendCollectorStatus(getApplicationContext(), path);
                    sendPersistentStore();
                } else if (path.equals(WEARABLE_SENSOR_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncSensorData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_ACTIVEBTDEVICE_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncActiveBtDeviceData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_ALERTTYPE_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncAlertTypeData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_TREATMENTS_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncTreatmentsData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_BLOODTEST_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncBloodTestData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_CALIBRATION_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncCalibrationData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_BG_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncBgData(dataMap, getApplicationContext());
                } else if (path.equals(WEARABLE_PREF_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncPrefData(dataMap);
                } else if (path.equals(WEARABLE_G5_QUEUE_PATH)) {
                    // TODO clean up other duplication in conditionals above
                    receiveG5QueueData(dataMap);
                } else if (path.equals(WEARABLE_LOCALE_CHANGED_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    overrideLocale(dataMap);
                } else if (path.equals(DATA_ITEM_RECEIVED_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    String type = dataMap.getString("type", "");
                    long timeOfLastEntry = dataMap.getLong("timeOfLastEntry", 0);
                    long syncLogsRequested = dataMap.getLong("syncLogsRequested", -1);
                    msg = dataMap.getString("msg", "");
                    if (type != null && !type.isEmpty() && timeOfLastEntry > 0) {
                        // TODO duplicated sync tracking routines could be functionalized
                        switch (type) {
                            case "BG":
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Current last_send_previous=" + JoH.dateTimeText(last_send_previous));
                                Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! Received BGs confirmed up to " + JoH.dateTimeText(timeOfLastEntry));
                                if (timeOfLastEntry >= last_send_previous) {
                                    last_send_previous = timeOfLastEntry;
                                    PersistentStore.setLong(pref_last_send_previous, last_send_previous);
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received!  Updated last_send_previous=" + JoH.dateTimeText(last_send_previous));
                                }
                                else {
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Duplicate confirmation! Ignore timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry));
                                }
                                if (mPrefs.getBoolean("enable_wearG5", false)) {
                                    if (!Ob1G5CollectionService.usingNativeMode()) {
                                        dataMap = getWearTransmitterData(send_bg_count, last_send_previous, (send_bg_count / 3));
                                    }
                                    if (dataMap != null) {
                                        Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync BGs from " + JoH.dateTimeText(last_send_previous));
                                        sendData(SYNC_BGS_PATH, dataMap.toByteArray());
                                    }
                                    if (dataMap == null) {
                                        dataMap = getWearBgReadingData(send_bg_count, last_send_previous, (send_bg_count / 3));
                                        if (dataMap != null) {
                                            Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync BGs from " + JoH.dateTimeText(last_send_previous));
                                            sendData(SYNC_BGS_PRECALCULATED_PATH, dataMap.toByteArray());
                                        }
                                    }
                                }
                                break;
                            case "LOG":
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Current last_send_previous_log=" + JoH.dateTimeText(last_send_previous_log));
                                Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! Received LOGS confirmed up to " + JoH.dateTimeText(timeOfLastEntry));
                                if (timeOfLastEntry >= last_send_previous_log) {
                                    last_send_previous_log = timeOfLastEntry;
                                    PersistentStore.setLong(pref_last_send_previous_log, last_send_previous_log);
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received!  Updated last_send_previous_log=" + JoH.dateTimeText(last_send_previous_log));
                                }
                                else {
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Duplicate confirmation! Ignore timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry));
                                }
                                if (mPrefs.getBoolean("sync_wear_logs", false)) {
                                    dataMap = getWearLogData(send_log_count, last_send_previous_log, (send_log_count / 3), syncLogsRequested);
                                    if (dataMap != null) {
                                        Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync LOGS from " + JoH.dateTimeText(last_send_previous_log));
                                        sendData(SYNC_LOGS_PATH, dataMap.toByteArray());
                                    }
                                    //Indicates this request was triggered by phone ACTION_SYNC_LOGS request; -1 indicates request was triggered by watch in doBackground()
                                    else if (syncLogsRequested > -1) {
                                        Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! No outstanding logs! ACTION_SYNC_LOGS request completed!! Ongoing requests syncLogsRequested=" + syncLogsRequested);
                                        sendSyncRequested(SYNC_LOGS_REQUESTED_PATH, syncLogsRequested);
                                    }
                                    else
                                        Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! No outstanding logs! SYNC_LOGS_PATH request triggered by watch doBackground.  syncLogsRequested=" + syncLogsRequested);
                                }
                                break;
                            case "STEP":
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Current last_send_previous_step_sensor=" + JoH.dateTimeText(last_send_previous_step_sensor));
                                Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! Received Steps confirmed up to " + JoH.dateTimeText(timeOfLastEntry));
                                last_send_previous_step_sensor = timeOfLastEntry;
                                PersistentStore.setLong(pref_last_send_previous_step_sensor, last_send_previous_step_sensor);
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received!  Updated last_send_previous_step_sensor=" + JoH.dateTimeText(last_send_previous_step_sensor));
                                if (mPrefs.getBoolean("use_wear_health", false)) {
                                    dataMap = getWearStepSensorData(send_step_count, last_send_previous_step_sensor, (send_step_count / 3));
                                    if (dataMap != null) {
                                        Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync Steps from " + JoH.dateTimeText(last_send_previous_step_sensor));
                                        sendData(SYNC_STEP_SENSOR_PATH, dataMap.toByteArray());
                                    }
                                }
                                break;
                            case "HEART":
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Current last_send_previous_heart_sensor=" + JoH.dateTimeText(last_send_previous_heart_sensor));
                                Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! Received Heart confirmed up to " + JoH.dateTimeText(timeOfLastEntry));
                                last_send_previous_heart_sensor = timeOfLastEntry;
                                PersistentStore.setLong(pref_last_send_previous_heart_sensor, last_send_previous_heart_sensor);
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received!  Updated last_send_previous_heart_sensor=" + JoH.dateTimeText(last_send_previous_heart_sensor));
                                if (mPrefs.getBoolean("use_wear_health", false)) {
                                    dataMap = getWearHeartSensorData(send_heart_count, last_send_previous_heart_sensor, (send_heart_count / 3));
                                    if (dataMap != null) {
                                        Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync heart from " + JoH.dateTimeText(last_send_previous_heart_sensor));
                                        sendData(SYNC_HEART_SENSOR_PATH, dataMap.toByteArray());
                                    }
                                }
                                break;
                            case "TREATMENTS":
                                Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Current last_send_previous_treatments=" + JoH.dateTimeText(last_send_previous_treatments));
                                Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! Received treatments confirmed up to " + JoH.dateTimeText(timeOfLastEntry));
                                if (timeOfLastEntry >= last_send_previous_treatments) {
                                    last_send_previous_treatments = timeOfLastEntry;
                                    PersistentStore.setLong(pref_last_send_previous_treatments, last_send_previous_treatments);
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received!  Updated last_send_previous_treatments=" + JoH.dateTimeText(last_send_previous_treatments));
                                    Treatments.cleanupBloodTest(last_send_previous_treatments);//delete BloodTest entered via keypad
                                }
                                else {
                                    Log.d(TAG, "DATA_ITEM_RECEIVED_PATH received! Duplicate confirmation! Ignore timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry));
                                }
                                dataMap = getWearTreatmentsData(send_treatments_count, last_send_previous_treatments, (send_treatments_count / 4));
                                if (dataMap != null) {
                                    Log.i(TAG, "DATA_ITEM_RECEIVED_PATH received! New Request to sync treatments from " + JoH.dateTimeText(last_send_previous_treatments));
                                    sendData(SYNC_TREATMENTS_PATH, dataMap.toByteArray());
                                }
                                break;
                            case "BM":
                                Log.d(TAG, "Benchmark: onDataChanged received from sendDataReceived timeOfLastEntry=" + JoH.dateTimeText(timeOfLastEntry) + " Path=" + path);
                                Log.d(TAG, "Benchmark: onDataChanged DATA_ITEM_RECEIVED_PATH msg=" + msg);
                                break;
                        }
                    }
                    /* TODO remove once confirm not needed
                    if (doDeleteDB && isSafeToDeleteDB()) {
                        doDeleteDB = false;
                        Sensor.DeleteAndInitDb(getApplicationContext());
                        PersistentStore.setLong(pref_last_send_previous, 0);
                        PersistentStore.setLong(pref_last_send_previous_log, 0);
                        sendData(WEARABLE_INITDB_PATH, null);
                    }*/
                }
            }
        }
    }

    public synchronized static void createTreatment(DataMap dataMap, Context context) {
        Log.d(TAG, "createTreatment dataMap=" + dataMap);
        double timeoffset = dataMap.getDouble("timeoffset", 0);
        double carbs = dataMap.getDouble("carbs", 0);
        double insulin = dataMap.getDouble("insulin", 0);
        double bloodtest = dataMap.getDouble("bloodtest", 0);
        String notes = dataMap.getString("notes", "");

        long timestamp_ms = Treatments.getTimeStampWithOffset(timeoffset);
        Treatments treatment = Treatments.create(carbs, insulin, notes, timestamp_ms);
        if (bloodtest > 0) {
            Log.d(TAG, "createTreatment bloodtest=" + bloodtest);
            BloodTest.createFromCal(bloodtest, timeoffset, "Manual Entry", treatment.uuid);
        }
        else Log.d(TAG, "createTreatment bloodtest=0 " + bloodtest);
        showTreatments(context, "all");
        SendData(context, SYNC_TREATMENTS_PATH, null);
        //requestData(context);//send to phone if connected
    }

    public synchronized static void sendTreatment(String notes) {
        Log.d(TAG, "sendTreatment WEARABLE_TREATMENT_PAYLOAD notes=" + notes);
        DataMap dataMap = new DataMap();
        dataMap.putDouble("timestamp", System.currentTimeMillis());
        dataMap.putBoolean("watchkeypad", true);
        dataMap.putString("notes", notes);
        dataMap.putBoolean("ismgdl", doMgdl(PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())));
        Intent intent = new Intent(xdrip.getAppContext(), Simulation.class);
        intent.putExtra(WEARABLE_TREATMENT_PAYLOAD, dataMap.toBundle());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        xdrip.getAppContext().startActivity(intent);
    }

    private void sendCollectorStatus (Context context, String path) {
        String msg;
        //long last_timestamp = 0;
        DataMap dataMap = new DataMap();
        switch (DexCollectionType.getDexCollectionType()) {
            case DexcomG5:

                if (DexCollectionType.getCollectorServiceClass() == G5CollectionService.class) {
                    dataMap = G5CollectionService.getWatchStatus();//msg, last_timestamp
                } else {
                    dataMap = Ob1G5CollectionService.getWatchStatus();//msg, last_timestamp
                }
                break;
            case DexcomShare://TODO getLastState() in non-G5 Services
                BluetoothManager mBluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
                ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
                boolean connected = false;
                if (mBluetoothManager != null && activeBluetoothDevice != null) {
                    for (BluetoothDevice bluetoothDevice : mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                        if (bluetoothDevice.getAddress().compareTo(activeBluetoothDevice.address) == 0) {
                            connected = true;
                        }
                    }
                }
                if (connected) {
                    msg = "Connected on watch";
                } else {
                    msg = "Not Connected";
                }
                dataMap.putString("lastState", msg);
                break;
            default:
                dataMap = DexCollectionService.getWatchStatus();
                break;
        }
        if (dataMap != null) {
            dataMap.putString("action_path", path);
        }

        //sendReplyMsg (msg, last_timestamp, path, false);
        sendData(WEARABLE_REPLYMSG_PATH, dataMap.toByteArray());
    }

    private void sendLocalMessage(String tag, DataMap dataMap) {
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra(tag, dataMap.toBundle());
        LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
    }

    private void sendLocalToast(String msg, int length) {
        DataMap dataMap = new DataMap();
        dataMap.putString("msg", msg);
        dataMap.putInt("length", length);
        sendLocalMessage("msg", dataMap);
    }

    private synchronized void sendReplyMsg (String msg, long last_timestamp, String path, boolean showToast, int length) {
        Log.d(TAG, "sendReplyMsg msg=" + msg);
        DataMap dataMap = new DataMap();
        dataMap.putString("msg", msg);
        dataMap.putLong("last_timestamp", last_timestamp);
        dataMap.putString("action_path", path);//eg. START_COLLECTOR_PATH
        Log.d(TAG, "sendReplyMsg dataMap=" + dataMap);
        if (showToast) {
            sendLocalToast(msg, length);
        }
        sendData(WEARABLE_REPLYMSG_PATH, dataMap.toByteArray());
    }

    private synchronized void sendSyncRequested (String path, long syncLogsRequested) {
        if (syncLogsRequested > 0) syncLogsRequested--;
        Log.d(TAG, "sendSyncRequested syncLogsRequested=" + syncLogsRequested);
        DataMap dataMap = new DataMap();
        dataMap.putLong("timestamp", System.currentTimeMillis());
        dataMap.putLong("syncLogsRequested", syncLogsRequested);
        sendData(path, dataMap.toByteArray());
    }

    private synchronized void sendPersistentStore() {
        if (DexCollectionType.getDexCollectionType().equals(DexCollectionType.DexcomG5)) {
            DataMap dataMap = new DataMap();
            String dex_txid = mPrefs.getString("dex_txid", "ABCDEF");
            dataMap.putByteArray(G5_BATTERY_MARKER, PersistentStore.getBytes(G5_BATTERY_MARKER + dex_txid));
            dataMap.putLong(G5_BATTERY_FROM_MARKER, PersistentStore.getLong(G5_BATTERY_FROM_MARKER + dex_txid));
            dataMap.putString("dex_txid", dex_txid);

            dataMap.putByteArray(G5_FIRMWARE_MARKER, PersistentStore.getBytes(G5_FIRMWARE_MARKER + dex_txid));
            dataMap.putString("dex_txid", dex_txid);
            sendData(WEARABLE_G5BATTERY_PAYLOAD, dataMap.toByteArray());
        }
    }

    private boolean isSafeToDeleteDB() {//TODO remove once confirm not needed
        TransmitterData last_bg = TransmitterData.last();
        if (last_bg != null && last_send_previous <= last_bg.timestamp) {
            Log.d(TAG, "onDataChanged SYNC_DB_PATH requestData for last_send_previous < last_bg.timestamp:" + JoH.dateTimeText(last_send_previous) + "<="+ JoH.dateTimeText(last_bg.timestamp));
            requestData();
            return false;
        }
        if (mPrefs.getBoolean("sync_wear_logs", false)) {
            UserError last_log = UserError.last();
            if (last_log != null && last_send_previous_log <= last_log.timestamp) {
                Log.d(TAG, "onDataChanged SYNC_DB_PATH requestData for last_send_previous_log < last_log.timestamp:" + JoH.dateTimeText(last_send_previous_log) + "<=" + JoH.dateTimeText((long) last_log.timestamp));
                return false;
            }
        }
        return true;
    }

    private boolean resetDataToLatest(DataMap dataMap, Context context) {//KS
        if (dataMap != null) {
            Double dmTimestamp = dataMap.getDouble("timestamp");
            Log.d(TAG, "resetDataToLatest dataMap.datetime=" + JoH.dateTimeText(dmTimestamp.longValue()) + " dataMap.sgvDouble=" + dataMap.getDouble("sgvDouble"));
            // todo ratelimit
            Sensor.InitDb(context);//ensure database has already been initialized
            final BgReading last = BgReading.last();
            if (last != null) {
                long bgTimestamp = last.timestamp;
                Log.d(TAG, "resetDataToLatest last.timestamp=" + JoH.dateTimeText(bgTimestamp) + " last.calculated_value=" + last.calculated_value);
                if (bgTimestamp > dmTimestamp) {
                    dataMap(dataMap, last, mPrefs, new com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder(context));
                    return true;
                }
            }
        }
        return false;
    }

    private static void dataMap(DataMap dataMap, BgReading bg, SharedPreferences sPrefs, com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder bgGraphBuilder) {//KS
        Log.d(TAG, "dataMap bgTimestamp=" + JoH.dateTimeText(bg.timestamp) + " calculated_value=" + bg.calculated_value);
        //Double highMark = Double.parseDouble(sPrefs.getString("highValue", "140"));
        //Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "60"));
        //int battery = BgSendQueue.getBatteryLevel(context.getApplicationContext());
        dataMap.putString("sgvString", bgGraphBuilder.unitized_string(bg.calculated_value));
        dataMap.putString("slopeArrow", bg.slopeArrow());
        dataMap.putDouble("timestamp", bg.timestamp); //TODO: change that to long (was like that in NW)
        dataMap.putString("delta", bgGraphBuilder.unitizedDeltaString(true, true));
        //dataMap.putString("battery", "" + battery);
        dataMap.putLong("sgvLevel", sgvLevel(bg.calculated_value, sPrefs, bgGraphBuilder));
        //dataMap.putInt("batteryLevel", (battery>=30)?1:0);
        dataMap.putDouble("sgvDouble", bg.calculated_value);
        //dataMap.putDouble("high", inMgdl(highMark, sPrefs));
        //dataMap.putDouble("low", inMgdl(lowMark, sPrefs));
        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
    }

    private boolean fuzzyNodeCompare(String left, String right) {
        try {
            final String regex = "\\|\\S+$";
            return left.replaceFirst(regex, "").equals(right.replaceFirst(regex, ""));
        } catch (NullPointerException e) {
            Log.e(TAG, "fuzzyNodeCompare NullPointerException ", e);
            return false;
        }
    }

    public boolean overrideLocale(DataMap dataMap) {
        if (mPrefs.getBoolean("overrideLocale", false)) {
            String localeStr = dataMap.getString("locale", "");
            String locale[] = localeStr.split("_");
            final Locale newLocale = locale == null ? new Locale(localeStr) : locale.length > 1 ? new Locale(locale[0], locale[1]) : new Locale(locale[0]);
            final Locale oldLocale = Locale.getDefault();
            if (newLocale != null && !oldLocale.equals(newLocale)) {
                try {
                    Log.d(TAG, "overrideLocale locale from " + oldLocale + " to " + newLocale);
                    Context context = getApplicationContext();
                    final Resources resources = context.getResources();
                    final DisplayMetrics metrics = resources.getDisplayMetrics();
                    final Configuration config = resources.getConfiguration();
                    config.locale = newLocale;
                    resources.updateConfiguration(config, metrics);
                    Locale.setDefault(newLocale);
                    Log.d(TAG, "overrideLocale default locale " + Locale.getDefault() + " resource locale " + context.getResources().getConfiguration().locale);
                    DataMap dm = new DataMap();
                    dm.putString("locale", localeStr);
                    sendLocalMessage("locale", dm);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "overrideLocale Exception e: " + e);
                }
            }
        }
        return false;
    }

    private static void receiveG5QueueData(DataMap dataMap) {
        final String queueData = dataMap.getString("queueData");
        Ob1G5StateMachine.injectQueueJson(queueData);
    }

    private synchronized void syncPrefData(DataMap dataMap) {//KS
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        final PowerManager.WakeLock wl = JoH.getWakeLock(getApplicationContext(), "watchlistener-SYNC_PREF_DATA",120000);
        try {
            Log.d(TAG, "syncPrefData dataMap=" + dataMap);
            if (localnode == null || (localnode != null && localnode.isEmpty())) setLocalNodeName();

            String dexCollector = dataMap.getString(DexCollectionType.DEX_COLLECTION_METHOD, "None");//BluetoothWixel "DexcomG5"
            Log.d(TAG, "syncPrefData dataMap dexCollector=" + dexCollector + " mPrefs DexCollectionType.DEX_COLLECTION_METHOD:" + mPrefs.getString(DexCollectionType.DEX_COLLECTION_METHOD, "xxxxxxxx"));
            DexCollectionType collectionType = DexCollectionType.getType(dexCollector);

            Log.d(TAG, "syncPrefData dataMap dexCollector:" + dexCollector + " mPrefs dex_collection_method=" + mPrefs.getString(DexCollectionType.DEX_COLLECTION_METHOD, "xxxxxxxx"));

            if (!dexCollector.equals(mPrefs.getString(DexCollectionType.DEX_COLLECTION_METHOD, "xxxxxxxx"))) {
                Log.d(TAG, "syncPrefData dexCollector:" + dexCollector + " collectionType.name=" + collectionType.name());
                DexCollectionType.setDexCollectionType(collectionType);
                stopBtService();//Change requires collector restart
            }

            is_using_bt = DexCollectionType.hasBluetooth();//(collectionType == DexCollectionType.DexcomG5);
            Log.d(TAG, "syncPrefData is_using_bt:" + is_using_bt);
            //prefs.putBoolean("g5_collection_method", is_using_g5);

            boolean enable_wearG5 = is_using_bt && dataMap.getBoolean("enable_wearG5", false);
            boolean force_wearG5 = is_using_bt && dataMap.getBoolean("force_wearG5", false);
            String node_wearG5 = dataMap.getString("node_wearG5", "");
            String prefs_node_wearG5 = mPrefs.getString("node_wearG5", "");
            Log.d(TAG, "syncPrefData enter enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " prefs_node_wearG5:" + prefs_node_wearG5 + " localnode:" + localnode);

            if (!node_wearG5.equals(prefs_node_wearG5)) {
                prefs.putString("node_wearG5", node_wearG5);
                Log.d(TAG, "syncPrefData node_wearG5 pref set to dataMap:" + node_wearG5);
            }
            if (force_wearG5 && node_wearG5.equals("")) {
                prefs.putString("node_wearG5", localnode);
                node_wearG5 = localnode;
                Log.d(TAG, "syncPrefData node_wearG5 set empty string to localnode:" + localnode);
            }
            //if (!node_wearG5.equals(localnode)) {
            if (force_wearG5 && !fuzzyNodeCompare(localnode,node_wearG5)) {
                Log.d(TAG, "syncPrefData localnode != node_wearG5 disable force_wearG5 for this watch device! \nlocalnode: " + localnode + "\nnode_wearG5:" + node_wearG5);
                force_wearG5 = false;
            }

            if (force_wearG5 != mPrefs.getBoolean("force_wearG5", false)) {
                Log.d(TAG, "syncPrefData force_wearG5:" + force_wearG5);
                prefs.putBoolean("force_wearG5", force_wearG5);
            }
            if (enable_wearG5 != mPrefs.getBoolean("enable_wearG5", false)) {
                Log.d(TAG, "syncPrefData enable_wearG5:" + enable_wearG5);
                prefs.putBoolean("enable_wearG5", enable_wearG5);
            }

            String dex_txid = dataMap.getString("dex_txid", "ABCDEF");
            Log.d(TAG, "syncPrefData dataMap dex_txid=" + dex_txid);
            if (!dex_txid.equals(mPrefs.getString("dex_txid", ""))) {
                Log.d(TAG, "syncPrefData dex_txid:" + dex_txid);
                prefs.putString("dex_txid", dex_txid);
                stopBtService();//Change requires collector restart
            }

            String share_key = dataMap.getString("share_key", "SM00000000");
            Log.d(TAG, "syncPrefData dataMap share_key=" + share_key);
            if (!share_key.equals(mPrefs.getString("share_key", "SM00000000"))) {//change requires collector restart
                Log.d(TAG, "syncPrefData share_key:" + share_key);
                prefs.putString("share_key", share_key);
                stopBtService();//Change requires collector restart
            }

            final boolean adjustPast = dataMap.getBoolean("rewrite_history", true);
            prefs.putBoolean("rewrite_history", adjustPast);

            String units = dataMap.getString("units", "mgdl");
            Log.d(TAG, "syncPrefData dataMap units=" + units);
            prefs.putString("units", units);
            Log.d(TAG, "syncPrefData prefs units=" + mPrefs.getString("units", "mgdl"));

            Double high = dataMap.getDouble("high", 170.0);
            Double low = dataMap.getDouble("low", 70.0);
            Log.d(TAG, "syncPrefData dataMap highMark=" + high + " highMark=" + low);
            prefs.putString("highValue", high.toString());
            prefs.putString("lowValue", low.toString());

            final boolean g5_non_raw_method = dataMap.getBoolean("g5_non_raw_method", false);
            prefs.putBoolean("g5_non_raw_method", g5_non_raw_method);
            final String extra_tags_for_logging = dataMap.getString("extra_tags_for_logging", "");
            prefs.putString("extra_tags_for_logging", extra_tags_for_logging);

            prefs.putBoolean("use_ob1_g5_collector_service", dataMap.getBoolean("use_ob1_g5_collector_service", false));


            final String blukon_pin = dataMap.getString(Blukon.BLUKON_PIN_PREF, "");
            prefs.putString(Blukon.BLUKON_PIN_PREF, blukon_pin);


            // just add to this list to sync booleans with the same name
            final List<String> defaultFalseBooleansToReceive = WearSyncBooleans.getBooleansToSync();
            final List<String> defaultBlankPersistentStringsToReceive = WearSyncPersistentStrings.getPersistentStrings();

            for (String preference_name : defaultFalseBooleansToReceive) {
                prefs.putBoolean(preference_name, dataMap.getBoolean(preference_name, false));
            }
            for (String store_name : defaultBlankPersistentStringsToReceive) {
                PersistentStore.setString(store_name, dataMap.getString(store_name, ""));
            }

            //Advanced Bluetooth Settings used by G4+xBridge DexCollectionService - temporarily just use the Phone's settings
            //Therefore, change requires collector restart
            prefs.putBoolean("use_transmiter_pl_bluetooth", dataMap.getBoolean("use_transmiter_pl_bluetooth", false));
            prefs.putBoolean("use_rfduino_bluetooth", dataMap.getBoolean("use_rfduino_bluetooth", false));
            prefs.putBoolean("automatically_turn_bluetooth_on", dataMap.getBoolean("automatically_turn_bluetooth_on", true));
            prefs.putBoolean("bluetooth_excessive_wakelocks", dataMap.getBoolean("bluetooth_excessive_wakelocks", true));
            prefs.putBoolean("close_gatt_on_ble_disconnect", dataMap.getBoolean("close_gatt_on_ble_disconnect", true));
            prefs.putBoolean("bluetooth_frequent_reset", dataMap.getBoolean("bluetooth_frequent_reset", false));
            prefs.putBoolean("bluetooth_watchdog", dataMap.getBoolean("bluetooth_watchdog", false));
            prefs.putString("bluetooth_watchdog_timer", dataMap.getString("bluetooth_watchdog_timer", "20"));

            prefs.putBoolean("sync_wear_logs", dataMap.getBoolean("sync_wear_logs", false));

            //prefs.putBoolean("engineering_mode", dataMap.getBoolean("engineering_mode", false));

            prefs.putInt("nfc_sensor_age", dataMap.getInt("nfc_sensor_age", -1));
            prefs.putInt("bridge_battery", dataMap.getInt("bridge_battery", -1));
            prefs.putBoolean("bridge_battery_alerts", dataMap.getBoolean("bridge_battery_alerts", false));
            prefs.putString("bridge_battery_alert_level", dataMap.getString("bridge_battery_alert_level", "30"));

            if ((DexCollectionType.getDexCollectionType().equals(DexCollectionType.DexcomG5) ||
                    DexCollectionType.getDexCollectionType().equals(DexCollectionType.DexcomShare)) && enable_wearG5) {///TODO confirm wear battery should be used as bridge
                int wearBatteryLevel = CheckBridgeBattery.getBatteryLevel(Home.getAppContext());
                Log.i(TAG, "syncPrefData wearBatteryLevel=" + wearBatteryLevel);
                prefs.putInt("bridge_battery", wearBatteryLevel);//TODO confirm wear battery should be used as bridge
            }

            //Alerts:
            prefs.putString("persistent_high_repeat_mins", dataMap.getString("persistent_high_repeat_mins", "20"));
            prefs.putString("persistent_high_threshold_mins", dataMap.getString("persistent_high_threshold_mins", "60"));
            prefs.putBoolean("falling_alert", dataMap.getBoolean("falling_alert", false));
            prefs.putString("falling_bg_val", dataMap.getString("falling_bg_val", "2"));
            prefs.putBoolean("rising_alert", dataMap.getBoolean("rising_alert", false));
            prefs.putString("rising_bg_val", dataMap.getString("rising_bg_val", "2"));
            prefs.putBoolean("aggressive_service_restart", dataMap.getBoolean("aggressive_service_restart", false));
            //Step Counter
            prefs.putBoolean("use_wear_health", dataMap.getBoolean("use_wear_health", true));
            //Extra Status Line
            prefs.putBoolean("extra_status_stats_24h", dataMap.getBoolean("extra_status_stats_24h", false));
            prefs.putBoolean("extra_status_line", dataMap.getBoolean("extra_status_line", false));
            prefs.putBoolean("status_line_calibration_long", dataMap.getBoolean("status_line_calibration_long", false));
            prefs.putBoolean("status_line_calibration_short", dataMap.getBoolean("status_line_calibration_short", false));
            prefs.putBoolean("status_line_avg", dataMap.getBoolean("status_line_avg", false));
            prefs.putBoolean("status_line_a1c_dcct", dataMap.getBoolean("status_line_a1c_dcct", false));
            prefs.putBoolean("status_line_a1c_ifcc", dataMap.getBoolean("status_line_a1c_ifcc", false));
            prefs.putBoolean("status_line_in", dataMap.getBoolean("status_line_in", false));
            prefs.putBoolean("status_line_high", dataMap.getBoolean("status_line_high", false));
            prefs.putBoolean("status_line_low", dataMap.getBoolean("status_line_low", false));
            prefs.putBoolean("status_line_carbs", dataMap.getBoolean("status_line_carbs", false));
            prefs.putBoolean("status_line_insulin", dataMap.getBoolean("status_line_insulin", false));
            prefs.putBoolean("status_line_stdev", dataMap.getBoolean("status_line_stdev", false));
            prefs.putBoolean("status_line_royce_ratio", dataMap.getBoolean("status_line_royce_ratio", false));
            prefs.putBoolean("status_line_capture_percentage", dataMap.getBoolean("status_line_capture_percentage", false));
            //Calibration plugin
            prefs.putBoolean("extra_status_calibration_plugin", dataMap.getBoolean("extra_status_calibration_plugin", false));
            prefs.putBoolean("display_glucose_from_plugin", dataMap.getBoolean("display_glucose_from_plugin", false));
            prefs.putBoolean("use_pluggable_alg_as_primary", dataMap.getBoolean("use_pluggable_alg_as_primary", false));
            prefs.putBoolean("old_school_calibration_mode", dataMap.getBoolean("old_school_calibration_mode", false));

            prefs.putBoolean("show_wear_treatments", dataMap.getBoolean("show_wear_treatments", false));
            overrideLocale(dataMap);


            Ob1G5StateMachine.injectDexTime(dataMap.getString("dex-timekeeping"));

            prefs.apply();

            CheckBridgeBattery.checkBridgeBattery();

            enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
            force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
            node_wearG5 = mPrefs.getString("node_wearG5", "");
            Log.d(TAG, "syncPrefData exit enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5);

            if (!Sensor.isActive()) {
                Log.d(TAG, "syncPrefData No Active Sensor!! Request WEARABLE_INITDB_PATH before starting BT Collection Service: " + DexCollectionType.getDexCollectionType());
                sendData(WEARABLE_INITDB_PATH, null);
            }

            VersionFixer.updateAndCheck(dataMap.getString("build-version-name", null), apkBytesVersion);

            } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    //Assumes Wear is connected to phone
    private boolean processConnect() {//KS
        Log.d(TAG, "processConnect enter");
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
        boolean bStarted = false;
        if (enable_wearG5) {
            Log.d(TAG, "processConnect enable_wearG5=true");
            if (!force_wearG5){
                Log.d(TAG, "processConnect force_wearG5=false - stopBtService and requestData");
                stopBtService();
                ListenerService.requestData(this);
            }
            else {
                Log.d(TAG, "processConnect force_wearG5=true - startBtService");
                bStarted = true;
                startBtService();
            }
        }
        else {
            Log.d(TAG, "processConnect enable_wearG5=false - stopBtService and requestData");
            stopBtService();
            ListenerService.requestData(this);
        }
        return bStarted;
    }

    private void syncSensorData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncSensorData");
        if (dataMap != null) {
            String uuid = dataMap.getString("uuid");
            Log.d(TAG, "syncSensorData add Sensor for uuid=" + uuid);
            long started_at = dataMap.getLong("started_at");
            Integer latest_battery_level = dataMap.getInt("latest_battery_level");
            String sensor_location = dataMap.getString("sensor_location");
            Sensor.InitDb(context);//ensure database has already been initialized
            if (uuid != null && !uuid.isEmpty()) {
                Log.d(TAG, "syncSensorData add Sensor for uuid=" + uuid + " timestamp=" + started_at + " timeString=" +  JoH.dateTimeText(started_at));
                Sensor sensor = Sensor.getByUuid(uuid);
                if (sensor == null) {
                    Log.d(TAG, "syncSensorData createUpdate new Sensor...");
                    Sensor.createUpdate(started_at, 0, latest_battery_level, sensor_location, uuid);
                    Sensor newsensor = Sensor.currentSensor();
                    if (newsensor != null) {
                        Log.d(TAG, "syncSensorData createUpdate Sensor with uuid=" + uuid + " started at=" + started_at);
                    } else
                        Log.d(TAG, "syncSensorData Failed to createUpdate new Sensor for uuid=" + uuid);
                } else
                    Log.d(TAG, "syncSensorData Sensor already exists with uuid=" + uuid);
            }
        }
    }

    private void syncActiveBtDeviceData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncActiveBtDeviceData");
        if (dataMap != null) {
            String name = dataMap.getString("name", "");
            String address = dataMap.getString("address", "");
            Boolean connected = dataMap.getBoolean("connected", false);
            Log.d(TAG, "syncActiveBtDeviceData add ActiveBluetoothDevice for name=" + name + " address=" + address + " connected=" + connected);
            Sensor.InitDb(context);//ensure database has already been initialized
            if (name != null && !name.isEmpty() && address != null && !address.isEmpty()) {
                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                synchronized (ActiveBluetoothDevice.table_lock) {
                    ActiveBluetoothDevice btDevice = new Select().from(ActiveBluetoothDevice.class)
                            .orderBy("_ID desc")
                            .executeSingle();

                    prefs.edit().putString("last_connected_device_address", address).apply();
                    if (btDevice == null) {
                        ActiveBluetoothDevice newBtDevice = new ActiveBluetoothDevice();
                        newBtDevice.name = name;
                        newBtDevice.address = address;
                        newBtDevice.connected = connected;
                        newBtDevice.save();
                    } else {
                        btDevice.name = name;
                        btDevice.address = address;
                        btDevice.connected = connected;
                        btDevice.save();
                    }
                }
            }
        }
    }

    private void syncAlertTypeData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncAlertTypeData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncAlertTypeData add AlertType Table entries count=" + entries.size());
            Sensor.InitDb(context);//ensure database has already been initialized
            AlertType.remove_all();
            for (DataMap entry : entries) {
                if (entry != null) {
                    String alertrecord = entry.getString("alert");
                    if (alertrecord != null) {
                        AlertType data = gson.fromJson(alertrecord, AlertType.class);
                        AlertType exists = AlertType.get_alert(data.uuid);
                        if (exists != null) {
                            Log.d(TAG, "syncAlertTypeData AlertType exists for uuid=" + data.uuid + " name=" + data.name);
                            exists.name = data.name;
                            exists.active = data.active;
                            exists.volume = data.volume;
                            exists.vibrate = data.vibrate;
                            exists.light = data.light;
                            exists.override_silent_mode = data.override_silent_mode;
                            exists.predictive = data.predictive;
                            exists.time_until_threshold_crossed = data.time_until_threshold_crossed;
                            exists.above= data.above;
                            exists.threshold = data.threshold;
                            exists.all_day = data.all_day;
                            exists.start_time_minutes = data.start_time_minutes;
                            exists.end_time_minutes = data.end_time_minutes;
                            exists.minutes_between = data.minutes_between;
                            exists.default_snooze = data.default_snooze;
                            exists.text = data.text;
                            exists.mp3_file = data.mp3_file;
                            exists.save();
                        }
                        else {
                            data.save();
                            Log.d(TAG, "syncAlertTypeData AlertType does not exist for uuid=" + data.uuid);
                        }
                        exists = AlertType.get_alert(data.uuid);
                        if (exists != null)
                            Log.d(TAG, "syncAlertTypeData AlertType GSON saved BG: " + exists.toS());
                        else
                            Log.d(TAG, "syncAlertTypeData AlertType GSON NOT saved");
                    }
                }
            }
        }
    }

    private synchronized void syncCalibrationData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncCalibrationData");

        boolean changed = false;
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncCalibrationData add Calibration Table entries count=" + entries.size());
            Sensor.InitDb(context);//ensure database has already been initialized
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                for (DataMap entry : entries) {
                    if (entry != null) {
                        String calibration = entry.getString("bgs"); // bgs should be refactored to avoid confusion
                        if (calibration != null) {
                            Calibration bgData = gson.fromJson(calibration, Calibration.class);
                            Calibration exists = Calibration.findByUuid(bgData.uuid);
                            bgData.sensor = sensor;
                            if (exists != null) {
                                Log.d(TAG, "syncCalibrationData Calibration exists for uuid=" + bgData.uuid + " bg=" + bgData.bg + " timestamp=" + bgData.timestamp + " timeString=" +  JoH.dateTimeText(bgData.timestamp));
                                if (exists.slope != bgData.slope || exists.slope_confidence != bgData.slope_confidence || exists.timestamp != bgData.timestamp || exists.bg != bgData.bg) {//slope* indicates if shown on graph
                                    changed = true;
                                }
                                exists.adjusted_raw_value = bgData.adjusted_raw_value;
                                exists.bg = bgData.bg;
                                exists.check_in = bgData.check_in;
                                exists.distance_from_estimate = bgData.distance_from_estimate;
                                exists.estimate_bg_at_time_of_calibration = bgData.estimate_bg_at_time_of_calibration;
                                exists.estimate_raw_at_time_of_calibration = bgData.estimate_raw_at_time_of_calibration;
                                exists.first_decay = bgData.first_decay;
                                exists.first_intercept = bgData.first_intercept;
                                exists.first_scale = bgData.first_scale;
                                exists.first_slope = bgData.first_slope;
                                exists.intercept = bgData.intercept;
                                exists.possible_bad = bgData.possible_bad;
                                exists.raw_timestamp = bgData.raw_timestamp;
                                exists.raw_value = bgData.raw_value;
                                exists.second_decay = bgData.second_decay;
                                exists.second_intercept = bgData.second_intercept;
                                exists.second_scale = bgData.second_scale;
                                exists.second_slope = bgData.second_slope;
                                exists.sensor = sensor;
                                exists.sensor_age_at_time_of_estimation = bgData.sensor_age_at_time_of_estimation;
                                exists.sensor_confidence = bgData.sensor_confidence;
                                exists.sensor_uuid = bgData.sensor_uuid;
                                exists.slope = bgData.slope;
                                exists.slope_confidence = bgData.slope_confidence;
                                exists.timestamp = bgData.timestamp;
                                exists.save();
                            }
                            else {
                                changed = true;
                                bgData.save();
                                //final boolean adjustPast = mPrefs.getBoolean("rewrite_history", true);
                                Log.d(TAG, "syncCalibrationData Calibration does not exist for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" +  JoH.dateTimeText(bgData.timestamp));
                                //Calibration.adjustRecentBgReadings(adjustPast ? 30 : 2);
                            }
                            exists = Calibration.findByUuid(bgData.uuid);
                            if (exists != null)
                                Log.d(TAG, "syncCalibrationData Calibration GSON saved BG: " + exists.toS());
                            else
                                Log.d(TAG, "syncCalibrationData Calibration GSON NOT saved");
                        }
                    }
                }
            }
            else {
                Log.d(TAG, "syncCalibrationData No Active Sensor!! Request WEARABLE_INITDB_PATH");
                sendData(WEARABLE_INITDB_PATH, null);
            }
            if (changed) {
                showTreatments(context, "cals");
            }
        }
    }

    private synchronized void deleteTreatment(DataMap dataMap) {
        ArrayList<String> entries = dataMap.getStringArrayList("entries");
        for (String uuid : entries) {
            Log.d(TAG, "syncTreatmentsData deleteTreatment for uuid=" + uuid);
            Treatments.delete_by_uuid(uuid);
        }
    }

    private synchronized void syncTreatmentsData(DataMap dataMap, Context context) {
        Log.d(TAG, "syncTreatmentsData");

        boolean changed = false;
        String action = dataMap.getString("action");
        if (action.equals("delete")) {
            Log.d(TAG, "syncTreatmentsData Delete Treatments");
            deleteTreatment(dataMap);
            showTreatments(context, "treats");
        }
        else {
            ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
            if (entries != null) {
                Gson gson = new GsonBuilder()
                            .excludeFieldsWithoutExposeAnnotation()
                            .registerTypeAdapter(Date.class, new DateTypeAdapter())
                            .serializeSpecialFloatingPointValues()
                            .create();
                Log.d(TAG, "syncTreatmentsData add Treatments Table entries count=" + entries.size());
                Sensor.InitDb(context);//ensure database has already been initialized
                for (DataMap entry : entries) {
                    if (entry != null) {
                        String record = entry.getString("data");
                        if (record != null) {
                            Treatments data = gson.fromJson(record, Treatments.class);
                            Treatments exists = Treatments.byuuid(data.uuid);
                            if (exists != null) {
                                Log.d(TAG, "syncTreatmentsData save existing Treatments for action insert uuid=" + data.uuid + " timestamp=" + data.timestamp + " timeString=" + JoH.dateTimeText(data.timestamp) + " carbs=" + data.carbs + " insulin=" + data.insulin + " exists.systime=" + JoH.dateTimeText(exists.systimestamp));
                                if (exists.timestamp != data.timestamp) {//currently only tracking timestamp on watch
                                    changed = true;
                                }
                                exists.enteredBy = data.enteredBy;
                                exists.eventType = data.eventType;
                                exists.insulin = data.insulin;
                                exists.carbs = data.carbs;
                                exists.created_at = data.created_at;
                                exists.notes = data.notes;
                                exists.timestamp = data.timestamp;
                                exists.systimestamp = exists.systimestamp > 0 ? exists.systimestamp : data.timestamp < last_send_previous_treatments ? data.timestamp : last_send_previous_treatments > 0 ? last_send_previous_treatments - 1 : JoH.tsl();
                                exists.save();
                            } else {
                                changed = true;
                                data.systimestamp = data.timestamp < last_send_previous_treatments ? data.timestamp : last_send_previous_treatments > 0 ? last_send_previous_treatments - 1 : JoH.tsl();
                                data.save();
                                Log.d(TAG, "syncTreatmentsData create new treatment for action insert uuid=" + data.uuid + " timestamp=" + data.timestamp + " timeString=" + JoH.dateTimeText(data.timestamp) + " carbs=" + data.carbs + " insulin=" + data.insulin + " systime=" + JoH.dateTimeText(data.systimestamp));
                            }
                        }
                    }
                }
                if (changed) {
                    showTreatments(context, "treats");
                }
            }
        }
    }

    private synchronized void syncBloodTestData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncBloodTestData");

        boolean changed = false;
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncBloodTestData add BloodTest Table entries count=" + entries.size());
            Sensor.InitDb(context);//ensure database has already been initialized
            for (DataMap entry : entries) {
                if (entry != null) {
                    String record = entry.getString("data");
                    if (record != null) {
                        BloodTest data = gson.fromJson(record, BloodTest.class);
                        BloodTest exists = BloodTest.byUUID(data.uuid);
                        if (exists != null) {
                            Log.d(TAG, "syncBloodTestData save existing BloodTest for uuid=" + data.uuid + " timestamp=" + data.timestamp + " timeString=" +  JoH.dateTimeText(data.timestamp) + " mgdl=" + data.mgdl + " state=" + data.state);
                            if (exists.mgdl != data.mgdl || exists.state != data.state || exists.timestamp != data.timestamp) {//state indicates if deleted
                                changed = true;
                            }
                            exists.mgdl = data.mgdl;
                            exists.created_timestamp = data.created_timestamp;
                            exists.source = data.source;
                            exists.state = data.state;
                            exists.timestamp = data.timestamp;
                            exists.save();
                        }
                        else {
                            changed = true;
                            data.save();
                            Log.d(TAG, "syncBloodTestData create new BloodTest for uuid=" + data.uuid + " timestamp=" + data.timestamp + " timeString=" +  JoH.dateTimeText(data.timestamp) + " mgdl=" + data.mgdl + " state=" + data.state);
                        }
                    }
                }
            }
            if (changed) {
                showTreatments(context, "bts");
            }
        }
    }

    public static void showTreatments(Context context, String extra) {
        Log.d(TAG, "showTreatments enter");
        long startTime = new Date().getTime() - (60000 * 60 * 5);//Max Chart time?
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("message", "ACTION_G5BG");
        DataMap treatsDataMap = null;
        if (Pref.getBooleanDefaultFalse("show_wear_treatments")) {
            switch (extra) {
                case "all":
                    treatsDataMap = getTreatments(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("treats", treatsDataMap.toBundle());
                    }
                    treatsDataMap = getCalibrations(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("cals", treatsDataMap.toBundle());
                    }
                    treatsDataMap = getBloodTests(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("bts", treatsDataMap.toBundle());
                    }
                    break;
                case "treats":
                    treatsDataMap = getTreatments(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("treats", treatsDataMap.toBundle());
                    }
                    break;
                case "cals":
                    treatsDataMap = getCalibrations(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("cals", treatsDataMap.toBundle());
                    }
                    break;
                case "bts":
                    treatsDataMap = getBloodTests(startTime);
                    if (treatsDataMap != null) {
                        messageIntent.putExtra("bts", treatsDataMap.toBundle());
                    }
                    break;
            }
            mPrefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
            if (mPrefs.getBoolean("extra_status_line", false)) {
                messageIntent.putExtra("extra_status_line", extraStatusLine(mPrefs));
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(messageIntent);
        }
    }

    public static DataMap getTreatments(long startTime) {
        Treatments last = Treatments.last();
        if (last != null) {
            Log.d(TAG, "getTreatments last.timestamp:" +  JoH.dateTimeText(last.timestamp));
        }
        List<Treatments> graph = Treatments.latestForGraph(60, startTime);
        if (!graph.isEmpty()) {
            Log.d(TAG, "getTreatments graph size=" + graph.size());
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph.size());
            DataMap entries = null;
            //if (includeTreatment(last)) entries = dataMapForWatchface(last);
            for (Treatments data : graph) {
                if (includeTreatment(data)) {
                    if (entries == null) {
                        entries = dataMapForWatchface(data);
                        dataMaps.add(dataMapForWatchface(data));
                    }
                    else
                        dataMaps.add(dataMapForWatchface(data));
                }
            }
            if (entries != null) {
                entries.putDataMapArrayList("entries", dataMaps);
                Log.d(TAG, "getTreatments entries=" + entries);
            }
            return entries;
        }
        else return null;
    }
    private static boolean includeTreatment(Treatments data) {
        if (data.carbs > 0 || data.insulin > 0)
            return true;
        else if (data.notes != null && !data.notes.isEmpty() && data.notes.indexOf("watchkeypad") < 0)//generated by Simulation
            return true;
        else
            return false;
    }
    private static DataMap dataMapForWatchface(Treatments data) {
        DataMap dataMap = new DataMap();
        //dataMap.putString("notes", data.notes);//TODO
        dataMap.putDouble("timestamp", data.timestamp);
        dataMap.putDouble("high", data.carbs);
        dataMap.putDouble("low", data.insulin);
        return dataMap;
    }

    public static DataMap getCalibrations(long startTime) {
        Calibration last = Calibration.last();
        if (last != null) {
            Log.d(TAG, "getCalibrations last.timestamp:" +  JoH.dateTimeText(last.timestamp));
        }
        List<Calibration> graph = Calibration.latestForGraph(60, startTime, Long.MAX_VALUE);
        //calibrations = Calibration.latestForGraph(numValues, start - (3 * Constants.DAY_IN_MS), end);
        if (!graph.isEmpty()) {
            Log.d(TAG, "getCalibrations graph size=" + graph.size());
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph.size());
            DataMap entries = null;
            //if (last.slope_confidence != 0) entries = dataMapForWatchface(last);
            for (Calibration data : graph) {
                if (data.slope_confidence != 0) {
                    if (entries == null) {
                        entries = dataMapForWatchface(data);
                        dataMaps.add(dataMapForWatchface(data));
                    }
                    else
                        dataMaps.add(dataMapForWatchface(data));
                }
            }
            if (entries != null) {
                entries.putDataMapArrayList("entries", dataMaps);
                Log.d(TAG, "getCalibrations entries=" + entries);
            }
            return entries;
        }
        else return null;
    }
    private static DataMap dataMapForWatchface(Calibration data) {
        DataMap dataMap = new DataMap();
        dataMap.putDouble("timestamp", data.timestamp);
        dataMap.putDouble("sgvDouble", data.bg);
        return dataMap;
    }
    public static DataMap getBloodTests(long startTime) {
        BloodTest last = BloodTest.last();
        if (last != null) {
            Log.d(TAG, "getBloodTests last.timestamp:" +  JoH.dateTimeText(last.timestamp));
        }
        List<BloodTest> graph = BloodTest.latestForGraph(60, startTime);
        if (!graph.isEmpty()) {
            Log.d(TAG, "getBloodTests graph size=" + graph.size());
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph.size());
            DataMap entries = dataMapForWatchface(graph.get(0));
            for (BloodTest data : graph) {
                dataMaps.add(dataMapForWatchface(data));
            }
            entries.putDataMapArrayList("entries", dataMaps);
            Log.d(TAG, "getBloodTests entries=" + entries);
            return entries;
        }
        else {
            Log.d(TAG, "getBloodTests no entries for startTime=" + JoH.dateTimeText(startTime));
            return null;
        }
    }
    private static DataMap dataMapForWatchface(BloodTest data) {
        DataMap dataMap = new DataMap();
        dataMap.putDouble("timestamp", data.timestamp);
        dataMap.putDouble("sgvDouble", data.mgdl);
        return dataMap;
    }

    private synchronized void syncBgData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncBGData");

        boolean changed = false;
        int battery = dataMap.getInt("battery");
        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        Log.d(TAG, "syncBGData add BgReading Table battery=" + battery );
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncBGData add BgReading Table entries count=" + entries.size());

            if (entries.size() == 0) {
                final String bgs = dataMap.getString("bgs");
                if (bgs != null) {
                   // entries = new ArrayList<>();
                    entries.add(dataMap);
                    UserError.Log.d(TAG, "Reformulating empty entries with recursive solo bgs record");
                }
            }

            if (JoH.ratelimit("init-db2", 15)) {
                Sensor.InitDb(context);//ensure database has already been initialized
            }
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                for (DataMap entry : entries) {
                    if (entry != null) {
                        String bgrecord = entry.getString("bgs");
                        if (bgrecord != null) {
                            final BgReading bgData = gson.fromJson(bgrecord, BgReading.class);

                        /*    // TODO this is a hack to use display glucose but it is incomplete regarding delta
                            if (bgData.dg_mgdl > 0) {
                                bgData.calculated_value = bgData.dg_mgdl;
                                bgData.calculated_value_slope = bgData.dg_slope;
                                // TODO delta missing???
                            }
*/
                            BgReading exists = BgReading.getForTimestampExists(bgData.timestamp);
                            exists = exists != null ? exists : BgReading.findByUuid(bgData.uuid);
                            String calibrationUuid = entry.getString("calibrationUuid");
                            if (exists != null) {
                                Log.d(TAG, "syncBGData BG already exists for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp));
                                try {
                                    Log.d(TAG, "syncBGData exists timeString=" + JoH.dateTimeText(exists.timestamp) + "  exists.calibration.uuid=" + exists.calibration.uuid + " exists=" + exists.toS());
                                } catch (NullPointerException e) {
                                    Log.d(TAG, "" + e); // usually when calibration.uuid is null because data is from g5 native
                                }

                                exists.filtered_calculated_value = bgData.filtered_calculated_value;
                                exists.calculated_value = bgData.calculated_value;
                                exists.hide_slope = bgData.hide_slope;

                                exists.filtered_data = bgData.filtered_data;
                                exists.raw_data = bgData.raw_data;
                                exists.raw_calculated = bgData.raw_calculated;
                                exists.calculated_value_slope = bgData.calculated_value_slope;
                                exists.age_adjusted_raw_value = bgData.age_adjusted_raw_value;
                                exists.calibration_flag = bgData.calibration_flag;
                                exists.ignoreForStats = bgData.ignoreForStats;
                                exists.time_since_sensor_started = bgData.time_since_sensor_started;
                                exists.ra = bgData.ra;
                                exists.rb = bgData.rb;
                                exists.rc = bgData.rc;
                                exists.a = bgData.a;
                                exists.b = bgData.b;
                                exists.c = bgData.c;
                                exists.noise = bgData.noise;
                                exists.time_since_sensor_started = bgData.time_since_sensor_started;

                                Calibration calibration = Calibration.byuuid(calibrationUuid);
                                calibration = calibration != null ? calibration : Calibration.byuuid(exists.calibration_uuid);
                                if (calibration != null) {
                                    exists.calibration = calibration;
                                    exists.calibration_uuid = calibration.uuid;
                                    exists.sensor = sensor;
                                    exists.sensor_uuid = sensor.uuid;
                                    if (exists.calculated_value != bgData.calculated_value) {
                                        changed = true;
                                    }
                                    exists.save();
                                }
                                else {
                                    Log.e(TAG, "syncBGData existing BgReading calibrationUuid not found by byuuid; calibrationUuid=" + calibrationUuid + " bgData.calibration_uuid=" + bgData.calibration_uuid + " bgData.uuid=" + bgData.uuid + " timeString=" +  JoH.dateTimeText(bgData.timestamp));
                                }
                            } else {
                                Calibration calibration = Calibration.byuuid(calibrationUuid);
                                calibration = calibration != null ? calibration : Calibration.byuuid(bgData.calibration_uuid);
                                if (calibration != null) {
                                    Log.d(TAG, "syncBGData add BG; calibration does exist for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp));
                                    bgData.calibration = calibration;
                                    bgData.calibration_uuid = calibration.uuid;
                                    bgData.sensor = sensor;
                                    bgData.sensor_uuid = sensor.uuid;
                                    changed = true;
                                    bgData.save();
                                } else {
                                    if (bgData.source_info != null && (bgData.source_info.contains("Native") || bgData.source_info.contains("Follow"))) {
                                        UserError.Log.d(TAG, "Saving BgData without calibration as source info is native or follow");
                                        bgData.sensor = sensor;
                                        bgData.sensor_uuid = sensor.uuid;
                                        changed = true;
                                        bgData.save();
                                    } else {
                                        Log.e(TAG, "syncBGData new BgReading calibrationUuid not found by byuuid; cannot save! calibrationUuid=" + calibrationUuid + " bgData.calibration_uuid=" + bgData.calibration_uuid + " bgData.uuid=" + bgData.uuid + " timeString=" + JoH.dateTimeText(bgData.timestamp));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else {
                Log.d(TAG, "syncBGData No Active Sensor!! Request WEARABLE_INITDB_PATH");
                sendData(WEARABLE_INITDB_PATH, null);
            }
        }
        if (changed) {//otherwise, wait for doBackground ACTION_RESEND
            Log.d(TAG, "syncBGData BG data has changed, refresh watchface, phone battery=" + battery );
            resendData(getApplicationContext(), battery);
            CustomComplicationProviderService.refresh();
        }
        else
            Log.d(TAG, "syncBGData BG data has NOT changed, do not refresh watchface, phone battery=" + battery );
    }

    // Custom method to determine whether a service is running
    private boolean isServiceRunning(Class<?> serviceClass){//Class<?> serviceClass
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
            final boolean result = isServiceRunning(serviceClass);
            Log.d(TAG, "DexCollectionType.getCollectorServiceClass(): " + serviceClass.getName() + " Running: " + true);
            return result;
        }
        return false;
    }

    private synchronized void startBtService() {//KS
        Log.d(TAG, "startBtService");
        if (is_using_bt) {
            if (checkLocationPermissions()) {
                Log.d(TAG, "startBtService start BT Collection Service: " + DexCollectionType.getDexCollectionType());
                //if (restartWatchDog()) {
                //    stopBtService();
                //}
                if (!isCollectorRunning()) {
                    if (JoH.ratelimit("start-collector", 2)) {
                        CollectionServiceStarter.startBtService(getApplicationContext());
                    }
                    Log.d(TAG, "startBtService AFTER startService mLocationPermissionApproved " + mLocationPermissionApproved);
                } else {
                    Log.d(TAG, "startBtService collector already running!");
                }
            }
        }
    }

    private boolean checkLocationPermissions() {//KS
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        Context myContext = getApplicationContext();
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        myContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions  mLocationPermissionApproved:" + mLocationPermissionApproved);

        // Display Activity to get user permission
        if (!mLocationPermissionApproved) {
            if (JoH.ratelimit("location_permission", 20)) {
                Intent permissionIntent = new Intent(getApplicationContext(), LocationPermissionActivity.class);
                permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(permissionIntent);
            }
        }
        // Enables app to handle 23+ (M+) style permissions.
        // TODO isn't this a duplicate as permission intent new task is asynchronous?
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions mLocationPermissionApproved:" + mLocationPermissionApproved);
        return mLocationPermissionApproved;
    }

    private boolean restartWatchDog() {//KS from app/MissedReadingService.java
        final long stale_millis = Home.stale_data_millis();
        if (is_using_bt) {//(prefs.getBoolean("aggressive_service_restart", false) || DexCollectionType.isFlakey()) {
            if (!BgReading.last_within_millis(stale_millis)) {
                if (JoH.ratelimit("aggressive-restart", aggressive_backoff_timer)) {
                    Log.e(TAG, "Aggressively restarting wear collector service due to lack of reception: backoff: "+aggressive_backoff_timer);
                    if (aggressive_backoff_timer < 1200) aggressive_backoff_timer+=60;
                    return true;//CollectionServiceStarter.restartCollectionService
                } else {
                    aggressive_backoff_timer = 120; // reset
                }
            }
        }
        return false;
    }

    private void stopBtService() {
        Log.d(TAG, "stopService call stopService");
        CollectionServiceStarter.stopBtService(getApplicationContext());
        Log.d(TAG, "stopBtService should have called onDestroy");
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent wakeIntent = PendingIntent.getService(this, 0, new Intent(this, Notifications.class), PendingIntent.FLAG_UPDATE_CURRENT);
        wakeIntent.cancel();
        alarmManager.cancel(wakeIntent);
        Log.d(TAG, "stopBtService cancel Notifications wakeIntent");
    }

    public static void requestData(Context context) {
        Log.d(TAG, "requestData (Context context) ENTER");
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_RESEND);
        context.startService(intent);
    }

    // generic send data
    public static void SendData(Context context, String path, byte[] payload) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_SENDDATA);
        intent.putExtra(WEARABLE_FIELD_SENDPATH, path);
        intent.putExtra(WEARABLE_FIELD_PAYLOAD, payload);
        context.startService(intent);
    }

    private Node updatePhoneSyncBgsCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        return pickBestNode(connectedNodes);
        //mPhoneNodeId = pickBestNodeId(connectedNodes);
    }

    private String pickBestNodeId(Set<Node> nodes) {//TODO remove once confirm not needed
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

    private Node pickBestNode(Set<Node> nodes) {
        Node bestNode = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node;
            }
            bestNode = node;
        }
        return bestNode;
    }

    private void setupStepSensor() {
        if (mPrefs.getBoolean("use_wear_health", false) || mPrefs.getBoolean("showSteps", false)) {
            Log.d(TAG, "Start Step Counter Sensor");
            //startService(new Intent(this, SensorService.class));
            resetCounters();
            stopMeasurement();
            startMeasurement();

            HeartRateService.scheduleWakeUp(25 * Constants.SECOND_IN_MS, "initial start");

        }
        else {
            Log.d(TAG, "Stop Step Counter Sensor");
            //stopService(new Intent(this, SensorService.class));
            stopMeasurement();
        }
    }

    // step counter
    private final SensorEventListener mListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            long t = System.currentTimeMillis();

            int type = event.sensor.getType();

            if (type == SENS_STEP_COUNTER) {

                Log.d(TAG, "onSensorChanged Sensor " + type + " name = " + event.sensor.getStringType());
                Log.d(TAG, "onSensorChanged accuracy = " + event.accuracy);
                Log.d(TAG, "onSensorChanged MaxDelay = " + event.sensor.getMaxDelay());
                Log.d(TAG, "onSensorChanged t = " + t + " text = " + JoH.dateTimeText(t));
                Log.d(TAG, "onSensorChanged last_movement_timestamp = " + last_movement_timestamp + " text = " + JoH.dateTimeText(last_movement_timestamp));

                // Calculate the delay from when event was recorded until it was received here in ms
                // Event timestamp is recorded in us accuracy, but ms accuracy is sufficient here
                long delay = System.currentTimeMillis() - (event.timestamp / 1000000L);//Timestamp when sensor was registered
                Log.d(TAG, "onSensorChanged delay = " + delay + " JoH.DateTimeText(delay) = " + JoH.dateTimeText(delay) + " (delay + (event.timestamp / 1000000L)) = " + delay + (event.timestamp / 1000000L) + " text= " + JoH.dateTimeText(delay + (event.timestamp / 1000000L)));

                PebbleMovement last = PebbleMovement.last();
                boolean sameDay = last != null ? isSameDay(t, last.timestamp) : false;
                if (!sameDay) {
                    initCounters();
                    Log.d(TAG, "onSensorChanged initCounters initCounters mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + last_movement_timestamp);
                }
                if (mCounterSteps < 1) {
                    // initial value
                    mCounterSteps = (int) event.values[0];
                }
                // Calculate steps taken based on first counter value received.
                mSteps = (int) event.values[0] - mCounterSteps;
                // Add the number of steps previously taken, otherwise the counter would start at 0.
                // This is needed to keep the counter consistent across rotation changes.
                mSteps = mSteps + mPreviousCounterSteps;
                PersistentStore.setLong(pref_msteps, (long) mSteps);
                Log.d(TAG, "onSensorChanged Total step count: " + mSteps + " mCounterSteps: " + mCounterSteps + " mPreviousCounterSteps: " + mPreviousCounterSteps + " event.values[0]: " + event.values[0]);

                if (last_movement_timestamp < t) {//KS BUG SW3 seems to set event.timestamp to time when sensor listener is registered
                    Log.d(TAG, "onSensorChanged Movement for mSteps: " + mSteps + " event.values[0]: " + event.values[0] +
                            " recorded: " + JoH.dateTimeText(System.currentTimeMillis() - (event.timestamp / 1000000L)) +
                            " received: " + JoH.dateTimeText(t) + " last_movement_timestamp: " + JoH.dateTimeText(last_movement_timestamp)
                    );
                    if (last_movement_timestamp == 0 || (sameDay && last != null && last.metric == mSteps)) {//skip initial movement or duplicate steps
                        Log.d(TAG, "onSensorChanged Initial sensor movement! Skip initial movement record, or duplicate record. last.metric=" + (last != null ? last.metric : "null"));
                    } else {
                        final PebbleMovement pm = PebbleMovement.createEfficientRecord(t, mSteps);//event.timestamp * 1000, (int) event.values[0]
                        Log.d(TAG, "Saving Movement: " + pm.toS());
                    }
                    last_movement_timestamp = t;
                    PersistentStore.setLong(pref_last_movement_timestamp, last_movement_timestamp);
                    Log.d(TAG, "onSensorChanged sendLocalMessage mSteps: " + mSteps + " t: " + JoH.dateTimeText(t) + " last_movement_timestamp: " + JoH.dateTimeText(last_movement_timestamp));
                    sendSensorLocalMessage(mSteps, t);
                } else {
                    Log.e(TAG, "onSensorChanged last_movement_timestamp > t! Reset last_movement_timestamp to current time.");
                    last_movement_timestamp = t;
                    PersistentStore.setLong(pref_last_movement_timestamp, last_movement_timestamp);
                }
            } else {
                Log.e(TAG,"onSensorChanged: unknown sensor type! "+type);
            }
        }

        @Override
        public void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        }
    };

    public static boolean isSameDay(long t, long last) {
        Calendar curCal = Calendar.getInstance();
        curCal.setTimeInMillis(t);
        Calendar lastCal = Calendar.getInstance();
        lastCal.setTimeInMillis(last);
        Log.d(TAG, "isSameDay Sensor curCal.DAY_OF_MONTH=" + curCal.get(Calendar.DAY_OF_MONTH) + " lastCal.DAY_OF_MONTH=" + lastCal.get(Calendar.DAY_OF_MONTH) + " t=" + JoH.dateTimeText(t) + " last.timestamp=" + JoH.dateTimeText(last) + " " + last);
        if (curCal.get(Calendar.DAY_OF_MONTH) == lastCal.get(Calendar.DAY_OF_MONTH) &&
                curCal.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) &&
                curCal.get(Calendar.MONTH) == lastCal.get(Calendar.MONTH) ) {
            return true;
        }
        return false;

    }

    private synchronized void startMeasurement() {
        if (mSensorManager == null) mSensorManager = ((SensorManager) getSystemService(SENSOR_SERVICE));

        //if (BuildConfig.DEBUG) {
        logAvailableSensors();
        //}
        mCounterSteps = 0;
        Log.i(TAG, "startMeasurement SensorService Event listener for step counter sensor register");

        android.hardware.Sensor stepCounterSensor = mSensorManager.getDefaultSensor(SENS_STEP_COUNTER);

        // Register the listener
        if (mSensorManager != null) {
            if (stepCounterSensor != null) {
                if (mPrefs.getBoolean("showSteps", false)) {
                    int delay = Integer.parseInt(mPrefs.getString("step_delay_time", "10"));
                    Log.d(TAG, "startMeasurement delay " + delay + " seconds.");
                    Log.d(TAG, "startMeasurement Event listener for step counter sensor registered with a max delay of " + delay + " seconds.");
                    mSensorManager.registerListener(mListener, stepCounterSensor, SensorManager.SENSOR_DELAY_UI, delay * BATCH_LATENCY_1s);
                }
                else {
                    Log.d(TAG, "startMeasurement Event listener for step counter sensor registered with a max delay of " + BATCH_LATENCY_400s);
                    mSensorManager.registerListener(mListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL, BATCH_LATENCY_400s);
                }
            } else {
                Log.d(TAG, "startMeasurement No Step Counter Sensor found");
            }
        }
    }

    private synchronized void stopMeasurement() {
        Log.i(TAG, "stopMeasurement");
        try {
            if (mSensorManager != null) {
                Log.i(TAG, "stopMeasurement STOP Event listener for step counter sensor register");
                mSensorManager.unregisterListener(mListener);
            }
        } catch (Exception e) {
            Log.i(TAG, "StopStepMeasurement exception: " + e);
        }
    }


    private void restartMeasurement() {
        PebbleMovement last = PebbleMovement.last();
        boolean sameDay = last != null ? ListenerService.isSameDay(System.currentTimeMillis(), last.timestamp) : false;
        if (!sameDay) {
            initCounters();
            Log.d(TAG, "restartMeasurement Sensor isSameDay=false initCounters mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + JoH.dateTimeText(last_movement_timestamp));
            stopMeasurement();
            startMeasurement();
        } else {
            Log.d(TAG, "restartMeasurement Sensor isSameDay=true PersistentStore mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + JoH.dateTimeText(last_movement_timestamp));
        }
    }

    private void resetCounters() {
        //initCounters();
        mSteps = (int) PersistentStore.getLong(pref_msteps);
        last_movement_timestamp = (int) PersistentStore.getLong(pref_last_movement_timestamp);
        Log.d(TAG, "resetCounters Sensor Enter PersistentStore mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + JoH.dateTimeText(last_movement_timestamp));

        PebbleMovement last = PebbleMovement.last();
        boolean sameDay = last != null ? ListenerService.isSameDay(System.currentTimeMillis(), last.timestamp) : false;
        if (!sameDay) {
            initCounters();
            Log.d(TAG, "resetCounters Sensor isSameDay=false initCounters mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + JoH.dateTimeText(last_movement_timestamp));
        } else {
            mCounterSteps = 0;
            mPreviousCounterSteps = mSteps;
            Log.d(TAG, "resetCounters Sensor isSameDay=true PersistentStore mSteps = " + mSteps + " mCounterSteps = " + mCounterSteps + " mPreviousCounterSteps = " + mPreviousCounterSteps + " last_movement_timestamp = " + JoH.dateTimeText(last_movement_timestamp));
        }
    }

    private synchronized void initCounters() {
        long t = System.currentTimeMillis();
        final PebbleMovement pm = PebbleMovement.createEfficientRecord(t, 0);
        Log.d(TAG, "initCounters Saving First Movement: " + pm.toS() + " at midnight t=" + JoH.dateTimeText(t));
        mSteps = 0;
        mCounterSteps = 0;
        mPreviousCounterSteps = 0;
    }

    private void sendSensorLocalMessage(int steps, long timestamp) {
        DataMap dataMap = new DataMap();
        dataMap.putInt("steps", steps);
        dataMap.putLong("steps_timestamp", timestamp);
        sendLocalMessage("steps", dataMap);
    }

    //Log all available sensors to logcat
    private void logAvailableSensors() {
        final List<android.hardware.Sensor> sensors = mSensorManager.getSensorList(android.hardware.Sensor.TYPE_ALL);
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
        Log.d(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", "SensorName", "StringType", "Type"));
        for (android.hardware.Sensor sensor : sensors) {
            Log.v(TAG, String.format(Locale.getDefault(), "|%-35s|%-38s|%-6s|", sensor.getName(), sensor.getStringType(), sensor.getType()));
        }
        Log.d(TAG, "=== LIST AVAILABLE SENSORS ===");
    }

    private void setLocalNodeName () {
        forceGoogleApiConnect();
        PendingResult<NodeApi.GetLocalNodeResult> result = Wearable.NodeApi.getLocalNode(googleApiClient);
        result.setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
            @Override
            public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                if (!getLocalNodeResult.getStatus().isSuccess()) {
                    Log.e(TAG, "ERROR: failed to getLocalNode Status=" + getLocalNodeResult.getStatus().getStatusMessage());
                } else {
                    Log.d(TAG, "getLocalNode Status=: " + getLocalNodeResult.getStatus().getStatusMessage());
                    Node getnode = getLocalNodeResult.getNode();
                    localnode = getnode != null ? getnode.getDisplayName() + "|" + getnode.getId() : "";
                    Log.d(TAG, "setLocalNodeName.  localnode=" + localnode);
                }
            }
        });
    }

    public static void requestAPK(String peerVersion) {
        if (peerVersion == null) return;
        if (!peerVersion.equals(apkBytesVersion)) {
            UserError.Log.d(TAG, "Requesting apk from start");
            requestAPK(0);
        } else {
            UserError.Log.d(TAG, "Resuming request apk from: " + apkBytesRead + " for matching: " + peerVersion);
            requestAPK(apkBytesRead);
        }
    }

    public static void requestAPK(int position) {
        Log.d(TAG, "Requesting APK from phone: position: " + position);
        SendData(xdrip.getAppContext(), WEARABLE_REQUEST_APK + (position > 0 ? "^" + position : ""), null);
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        Node phoneNode = updatePhoneSyncBgsCapability(capabilityInfo);
        Log.d(TAG, "onCapabilityChanged mPhoneNodeID:" + (phoneNode != null ? phoneNode.getId() : ""));//mPhoneNodeId
        //onPeerConnected and onPeerDisconnected deprecated at the same time as BIND_LISTENER

        if (phoneNode != null && phoneNode.getId().length() > 0) {
            if (JoH.ratelimit("on-connected-nodes-sync", 1200)) {
                Log.d(TAG, "onCapabilityChanged event - attempting resync");
                requestData();
            } else {
                Log.d(TAG, "onCapabilityChanged event - ratelimited");
            }
            sendPrefSettings();//from onPeerConnected
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected call requestData");

        Wearable.ChannelApi.addListener(googleApiClient, this);
        requestData();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (googleApiClient != null && googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
        if (googleApiClient != null) {
            Wearable.MessageApi.removeListener(googleApiClient, this);
            Wearable.ChannelApi.removeListener(googleApiClient, this);
        }

        Log.d(TAG, "Stop Sensors");
        stopMeasurement();


        if (mPrefs.getBoolean("enable_wearG5", true)) {
            Log.d(TAG, "Start BT Collection Service");
            stopBtService();
        }
    }

    @Override
    public void onChannelOpened(final Channel channel) {
        UserError.Log.d(TAG, "onChannelOpened: A new channel opened. From Node ID: " + channel.getNodeId() + " Path: " + channel.getPath());

        switch (channel.getPath()) {

            case "/updated-apk":
                ProcessAPKChannelDownload.enqueueWork(googleApiClient, channel);
                break;
            default:
                UserError.Log.e(TAG, "Unknown channel: " + channel.getPath());
        }
    }

    @Override
    public void onChannelClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {
        logChannelCloseReason("Whole Channel", channel, closeReason, appSpecificErrorCode);
        // TODO counter for failures??
        if ((closeReason == CLOSE_REASON_LOCAL_CLOSE || closeReason == CLOSE_REASON_REMOTE_CLOSE) && reRequestDownloadApkCounter < 30 && apkBytesRead < 1 && appSpecificErrorCode == 0) {
            UserError.Log.d(TAG,"Requesting to download again");
            reRequestDownloadApkCounter++;
            Inevitable.task("re-request apk download on close", 16000, VersionFixer::downloadApk);
        }
    }

    @Override
    public void onInputClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {
        logChannelCloseReason("Channel input", channel, closeReason, appSpecificErrorCode);
    }

    @Override
    public void onOutputClosed(final Channel channel, final int closeReason, final int appSpecificErrorCode) {
        logChannelCloseReason("Channel output", channel, closeReason, appSpecificErrorCode);
    }

    private void logChannelCloseReason(String source, final Channel channel, final int closeReason, final int appSpecificErrorCode) {
        UserError.Log.d(TAG, source + " closed. Reason: " + getCloseReason(closeReason) + " (" + closeReason + ") Error code: " + appSpecificErrorCode + " From Node ID " + channel.getNodeId() + " Path: " + channel.getPath());
    }

    private static String getCloseReason(final int closeReason) {
        switch (closeReason) {
            case CLOSE_REASON_NORMAL:
                return "normal close";
            case CLOSE_REASON_DISCONNECTED:
                return "disconnected";
            case CLOSE_REASON_REMOTE_CLOSE:
                return "closed by remote";
            case CLOSE_REASON_LOCAL_CLOSE:
                return "closed locally";
            default:
                return "UNKNOWN";
        }

    }
}
