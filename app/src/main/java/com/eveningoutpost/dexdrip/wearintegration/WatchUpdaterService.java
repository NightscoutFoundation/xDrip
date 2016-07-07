package com.eveningoutpost.dexdrip.wearintegration;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WatchUpdaterService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_RESEND = WatchUpdaterService.class.getName().concat(".Resend");
    public static final String ACTION_OPEN_SETTINGS = WatchUpdaterService.class.getName().concat(".OpenSettings");
    public static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    public static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    public static final String WEARABLE_VOICE_PAYLOAD = "/xdrip_plus_voice_payload";
    public static final String WEARABLE_APPROVE_TREATMENT = "/xdrip_plus_approve_treatment";
    public static final String WEARABLE_CANCEL_TREATMENT = "/xdrip_plus_cancel_treatment";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String OPEN_SETTINGS_PATH = "/openwearsettings";
    private static final String TAG = "jamorham watchupdater";
    private static GoogleApiClient googleApiClient;
    boolean wear_integration = false;
    boolean pebble_integration = false;
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

    public static void receivedText(Context context, String text) {
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

    public static void sendWearToast(String msg, int length)
    {
        if ((googleApiClient != null) && (googleApiClient.isConnected())) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(WEARABLE_TOAST_NOTIFICATON);
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putInt("length", length);
            dataMapRequest.getDataMap().putString("msg", msg);
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for toast! "+msg);
        }
    }

    public static void sendTreatment(double carbs, double insulin, double bloodtest, double timeoffset, String timestring) {
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
            dataMapRequest.getDataMap().putBoolean("ismgdl", doMgdl(PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())));
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e(TAG, "No connection to wearable available for send treatment!");
        }
    }

    public static boolean doMgdl(SharedPreferences sPrefs) {
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
        listenForChangeInSettings();
        setSettings();
        if (wear_integration) {
            googleApiConnect();
        }
    }

    public void listenForChangeInSettings() {
        mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                setSettings();
            }
        };
        mPrefs.registerOnSharedPreferenceChangeListener(mPreferencesListener);
    }

    public void setSettings() {
        wear_integration = mPrefs.getBoolean("wear_sync", false);
        pebble_integration = mPrefs.getBoolean("pebble_sync", false);
        if (wear_integration) {
            googleApiConnect();
        } else {
            this.stopService(new Intent(this, WatchUpdaterService.class));
        }
    }

    public void googleApiConnect() {
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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-onstart",60000);
        double timestamp = 0;
        if (intent != null) {
            timestamp = intent.getDoubleExtra("timestamp", 0);
        }

        String action = null;
        if (intent != null) {
            action = intent.getAction();
        }

        if (wear_integration) {
            if (googleApiClient.isConnected()) {
                if (ACTION_RESEND.equals(action)) {
                    resendData();
                } else if (ACTION_OPEN_SETTINGS.equals(action)) {
                    sendNotification();
                } else {
                    sendData();
                }
            } else {
                googleApiClient.connect();
            }
        }

        if (pebble_integration) {
            sendData();
        }

        //if ((!wear_integration)&&(!pebble_integration))
        if (!wear_integration)    // only wear sync starts this service, pebble features are not used?
        {
            Log.i(TAG,"Stopping service");
            stopSelf();
            JoH.releaseWakeLock(wl);
            return START_NOT_STICKY;
        }

        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        sendData();
    }

    // incoming messages from wear device
    @Override
    public void onMessageReceived(MessageEvent event) {
        if (wear_integration) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("watchupdate-msgrec", 60000);
            if (event != null) {
                Log.d(TAG, "wearable event path: " + event.getPath());
                switch (event.getPath()) {
                    case WEARABLE_RESEND_PATH:
                        resendData();
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
                    default:
                        Log.d(TAG, "Unknown wearable path: " + event.getPath());
                        break;
                }
            }
            JoH.releaseWakeLock(wl);
        }
    }

    public void sendData() {
        BgReading bg = BgReading.last();
        if (bg != null) {
            if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
                googleApiConnect();
            }
            if (wear_integration) {
                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, dataMap(bg, mPrefs, new BgGraphBuilder(getApplicationContext())));
            }
        }
    }

    private void resendData() {
        if (googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
            googleApiConnect();
        }
        long startTime = new Date().getTime() - (60000 * 60 * 24);
        BgReading last_bg = BgReading.last();
        if (last_bg != null) {
            List<BgReading> graph_bgs = BgReading.latestForGraph(60, startTime);
            BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(getApplicationContext());
            if (!graph_bgs.isEmpty()) {
                DataMap entries = dataMap(last_bg, mPrefs, bgGraphBuilder);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (BgReading bg : graph_bgs) {
                    dataMaps.add(dataMap(bg, mPrefs, bgGraphBuilder));
                }
                entries.putDataMapArrayList("entries", dataMaps);

                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).executeOnExecutor(xdrip.executor, entries);
            }
        }
    }

    private void sendNotification() {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(OPEN_SETTINGS_PATH);
            //unique content
            dataMapRequest.setUrgent();
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString("openSettings", "openSettings");
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e("OpenSettings", "No connection to wearable available!");
        }
    }


    private DataMap dataMap(BgReading bg, SharedPreferences sPrefs, BgGraphBuilder bgGraphBuilder) {
        Double highMark = Double.parseDouble(sPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "70"));
        DataMap dataMap = new DataMap();

        int battery = BgSendQueue.getBatteryLevel(getApplicationContext());

        dataMap.putString("sgvString", bgGraphBuilder.unitized_string(bg.calculated_value));
        dataMap.putString("slopeArrow", bg.slopeArrow());
        dataMap.putDouble("timestamp", bg.timestamp); //TODO: change that to long (was like that in NW)
        dataMap.putString("delta", bgGraphBuilder.unitizedDeltaString(true, true, true));
        dataMap.putString("battery", "" + battery);
        dataMap.putLong("sgvLevel", sgvLevel(bg.calculated_value, sPrefs, bgGraphBuilder));
        dataMap.putInt("batteryLevel", (battery >= 30) ? 1 : 0);
        dataMap.putDouble("sgvDouble", bg.calculated_value);
        dataMap.putDouble("high", inMgdl(highMark, sPrefs));
        dataMap.putDouble("low", inMgdl(lowMark, sPrefs));
        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
        return dataMap;
    }

    public long sgvLevel(double sgv_double, SharedPreferences prefs, BgGraphBuilder bgGB) {
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

    public double inMgdl(double value, SharedPreferences sPrefs) {
        if (!doMgdl(sPrefs)) {
            return value * Constants.MMOLL_TO_MGDL;
        } else {
            return value;
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
}
