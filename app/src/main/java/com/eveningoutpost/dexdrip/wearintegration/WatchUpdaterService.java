package com.eveningoutpost.dexdrip.wearintegration;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WatchUpdaterService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    public static final String ACTION_RESEND = WatchUpdaterService.class.getName().concat(".Resend");
    public static final String ACTION_OPEN_SETTINGS = WatchUpdaterService.class.getName().concat(".OpenSettings");

    private GoogleApiClient googleApiClient;
    public static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    public static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS_PATH = "/openwearsettings";

    boolean wear_integration = false;
    boolean pebble_integration = false;
    SharedPreferences mPrefs;
    SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener;

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
        if (wear_integration) {
            googleApiConnect();
        }
    }

    public void googleApiConnect() {
        if(googleApiClient != null && (googleApiClient.isConnected() || googleApiClient.isConnecting())) { googleApiClient.disconnect(); }
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
                //TODO Adrian: ACTION_OPEN_MENU?
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
        return START_STICKY;
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        sendData();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (wear_integration) {
            if (event != null && event.getPath().equals(WEARABLE_RESEND_PATH))
                resendData();
        }
    }

    public void sendData() {
        BgReading bg = BgReading.last();
        if (bg != null) {
            if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }
            if (wear_integration) {
                new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).execute(dataMap(bg, mPrefs));
            }
        }
    }

    private void resendData() {
        if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }
        long startTime = new Date().getTime() - (60000 * 60 * 24);
        BgReading last_bg = BgReading.last();
        List<BgReading> graph_bgs = BgReading.latestForGraph(60, startTime);
        if (!graph_bgs.isEmpty()) {
            DataMap entries = dataMap(last_bg, mPrefs);
            final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
            for (BgReading bg : graph_bgs) {
                dataMaps.add(dataMap(bg, mPrefs));
            }
            entries.putDataMapArrayList("entries", dataMaps);

            new SendToDataLayerThread(WEARABLE_DATA_PATH, googleApiClient).execute(entries);
        }
    }


    private void sendNotification() {
        if (googleApiClient.isConnected()) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(OPEN_SETTINGS_PATH);
            //unique content
            dataMapRequest.getDataMap().putDouble("timestamp", System.currentTimeMillis());
            dataMapRequest.getDataMap().putString("openSettings", "openSettings");
            PutDataRequest putDataRequest = dataMapRequest.asPutDataRequest();
            Wearable.DataApi.putDataItem(googleApiClient, putDataRequest);
        } else {
            Log.e("OpenSettings", "No connection to wearable available!");
        }
    }

    private DataMap dataMap(BgReading bg, SharedPreferences sPrefs) {
        Double highMark = Double.parseDouble(sPrefs.getString("highValue", "170"));
        Double lowMark = Double.parseDouble(sPrefs.getString("lowValue", "70"));
        DataMap dataMap = new DataMap();
        //TODO Add data
        /*
        dataMap.putString("sgvString", unitized_string());
        dataMap.putString("slopeArrow", slopeArrow());
        dataMap.putDouble("timestamp", datetime);
        dataMap.putString("delta", unitizedDeltaString());
        dataMap.putString("battery", battery);
        dataMap.putLong("sgvLevel", sgvLevel(prefs));
        dataMap.putInt("batteryLevel", batteryLevel());
        dataMap.putDouble("sgvDouble", sgv_double());
        dataMap.putDouble("high", inMgdl(highMark));
        dataMap.putDouble("low", inMgdl(lowMark));
        */
        //TODO: Add raw again
        //dataMap.putString("rawString", threeRaw((prefs.getString("units", "mgdl").equals("mgdl"))));
        return dataMap;
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
