package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.Models.BgReading;//KS
import com.eveningoutpost.dexdrip.Models.Calibration;//KS
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;//KS
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;//KS
import com.eveningoutpost.dexdrip.UtilityModels.BgSendQueue;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
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

import java.text.SimpleDateFormat;//KS
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String SYNC_DB_PATH = "/syncweardb";//KS
    private static final String SYNC_BGS_PATH = "/syncwearbgs";//KS
    private static final String WEARABLE_BG_DATA_PATH = "/nightscout_watch_bg_data";//KS
    private static final String WEARABLE_CALIBRATION_DATA_PATH = "/nightscout_watch_cal_data";//KS
    private static final String WEARABLE_SENSOR_DATA_PATH = "/nightscout_watch_sensor_data";//KS
    private static final String WEARABLE_PREF_DATA_PATH = "/nightscout_watch_pref_data";//KS
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";//KS
    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_SENDDATA = "com.dexdrip.stephenblack.nightwatch.SEND_DATA";
    private static final String FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    private static final String FIELD_PAYLOAD = "field_xdrip_plus_payload";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";

    // Phone
    private static final String CAPABILITY_PHONE_APP = "phone_app_sync_bgs";
    private static final String MESSAGE_PATH_PHONE = "/phone_message_path";
    // Wear
    private static final String CAPABILITY_WEAR_APP = "wear_app_sync_bgs";
    private static final String MESSAGE_PATH_WEAR = "/wear_message_path";
    private String mPhoneNodeId = null;
    private String localnode= null;

    private static final String TAG = "jamorham listener";
    private SharedPreferences mPrefs;//KS
    private static boolean mLocationPermissionApproved;//KS
    private static long last_send_previous = 0;//KS
    final private static String pref_last_send_previous = "last_send_previous";
    private boolean is_using_g5 = false;
    private static int aggressive_backoff_timer = 120;

    private GoogleApiClient googleApiClient;
    private static long lastRequest = 0;

    public class DataRequester extends AsyncTask<Void, Void, Void> {
        final String path;
        final byte[] payload;

        DataRequester(Context context, String thispath, byte[] thispayload) {
            path = thispath;
            payload = thispayload;
            Sensor.InitDb(context);//ensure database has already been initialized
            Log.d(TAG, "DataRequester: " + thispath);
        }

        @Override
        protected Void doInBackground(Void... params) {
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//KS
            boolean enable_wearG5 = sharedPrefs.getBoolean("enable_wearG5", false); //KS
            boolean force_wearG5 = sharedPrefs.getBoolean("force_wearG5", false); //KS
            String node_wearG5 = sharedPrefs.getString("node_wearG5", ""); //KS
            Log.d(TAG, "doInBackground enter enable_wearG5=" + enable_wearG5 + " force_wearG5=" + force_wearG5 + " node_wearG5=" + node_wearG5);//KS

            if ((googleApiClient != null) && (googleApiClient.isConnected())) {
                if (!path.equals(ACTION_RESEND) || (System.currentTimeMillis() - lastRequest > 20 * 1000)) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();

                    //NodeApi.GetConnectedNodesResult nodes =
                    //        Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    NodeApi.GetLocalNodeResult localnodes = Wearable.NodeApi.getLocalNode(googleApiClient).await();
                    Node getnode = localnodes != null ? localnodes.getNode() : null;
                    localnode = getnode != null ?  getnode.getDisplayName() + "|" + getnode.getId() : "";
                    Log.d(TAG, "doInBackground.  getLocalNode name=" + localnode);
                    CapabilityApi.GetCapabilityResult capResult =
                            Wearable.CapabilityApi.getCapability(
                                    googleApiClient, CAPABILITY_PHONE_APP,
                                    CapabilityApi.FILTER_REACHABLE).await();
                    CapabilityInfo nodes = capResult.getCapability();
                    updatePhoneSyncBgsCapability(nodes);

                    int count = nodes.getNodes().size();//KS
                    Log.d(TAG, "doInBackground connected.  CapabilityApi.GetCapabilityResult mPhoneNodeID=" + (mPhoneNodeId != null ? mPhoneNodeId : "") + " count=" + count);//KS
                    if (count > 0) {//KS
                        if (enable_wearG5) {
                            if (force_wearG5) {
                                startBtG5Service();
                            }
                            else {
                                stopBtG5Service();
                            }
                        }

                        for (Node node : nodes.getNodes()) {

                            if (enable_wearG5) {//KS
                                DataMap datamap = getWearTransmitterData(288);//KS 36 data for last 3 hours; 288 for 1 day
                                if (datamap != null) {//while
                                    Log.d(TAG, "doInBackground send Wear Data BGs to phone path:" + SYNC_BGS_PATH + " and node:" + node.getId() + " and node:" + node.getDisplayName());
                                    Log.d(TAG, "doInBackground send Wear datamap:" + datamap);

                                    PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), SYNC_BGS_PATH, datamap.toByteArray());
                                    result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                                        @Override
                                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                            if (!sendMessageResult.getStatus().isSuccess()) {
                                                Log.e(TAG, "ERROR: failed to send Wear BGs to phone: " + sendMessageResult.getStatus().getStatusMessage());
                                            }
                                            else {
                                                Log.i(TAG, "Sent Wear BGs to phone: " + sendMessageResult.getStatus().getStatusMessage());
                                            }
                                        }
                                    });
                                }
                            }
                            Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, payload);
                        }
                    }
                    else {
                        if (enable_wearG5) {//KS
                            Log.d(TAG, "doInBackground connected but getConnectedNodes returns 0.  start G5 service");
                            startBtG5Service();
                        }
                    }
                } else {
                    Log.d(TAG, "Debounce limit hit - not sending");
                }
            } else {
                Log.d(TAG, "Not connected for sending");
                if (googleApiClient != null) {
                    googleApiClient.connect();
                }
            }
            return null;
        }
    }

    private DataMap getWearTransmitterData(int count) {//KS
        if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }

        Log.d(TAG, "getWearTransmitterData last_send_previous:" + JoH.dateTimeText(last_send_previous));

        TransmitterData last_bg = TransmitterData.last();
        if (last_bg != null) {
            Log.d(TAG, "getWearTransmitterData last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
        }

        if (last_bg != null && last_send_previous <= last_bg.timestamp) {//startTime
            Log.d(TAG, "getWearTransmitterData last_send_previous < last_bg.timestamp:" + JoH.dateTimeText(last_bg.timestamp));
            List<TransmitterData> graph_bgs = TransmitterData.latestForGraphAsc(count, last_send_previous);
            if (!graph_bgs.isEmpty()) {
                Log.d(TAG, "getWearTransmitterData graph_bgs count = " + graph_bgs.size());
                DataMap entries = dataMap(last_bg);
                final ArrayList<DataMap> dataMaps = new ArrayList<>(graph_bgs.size());
                for (TransmitterData bg : graph_bgs) {
                    dataMaps.add(dataMap(bg));
                    Log.d(TAG, "getWearTransmitterData bg.timestamp:" + JoH.dateTimeText(bg.timestamp));
                    long last_send_sucess = bg.timestamp + 1;
                    Log.d(TAG, "getWearTransmitterData set last_send_sucess:" + JoH.dateTimeText(last_send_sucess));
                    Log.d(TAG, "getWearTransmitterData bg getId:" + bg.getId() + " raw_data:" + bg.raw_data + " filtered_data:" + bg.filtered_data + " timestamp:" + bg.timestamp + " uuid:" + bg.uuid);
                }
                entries.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
                entries.putDataMapArrayList("entries", dataMaps);
                return entries;
            }
            else
                Log.d(TAG, "getWearTransmitterData graph_bgs count = 0");
        }
        return null;
    }

    private void sendPrefSettings() {//KS

        if(googleApiClient != null && !googleApiClient.isConnected() && !googleApiClient.isConnecting()) { googleApiConnect(); }
        DataMap dataMap = new DataMap();
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
        String node_wearG5 = mPrefs.getString("node_wearG5", "");
        String dex_txid = mPrefs.getString("dex_txid", "ABCDEF");

        Log.d(TAG, "sendPrefSettings enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " localnode:" + localnode + " dex_txid:" + dex_txid);
        dataMap.putLong("time", new Date().getTime()); // MOST IMPORTANT LINE FOR TIMESTAMP
        dataMap.putBoolean("enable_wearG5", enable_wearG5);
        dataMap.putBoolean("force_wearG5", force_wearG5);
        if (force_wearG5) {
            dataMap.putString("node_wearG5", localnode);
        }
        else {
            if (node_wearG5.equals(localnode)) {
                dataMap.putString("node_wearG5", "");
            }
            else {
                dataMap.putString("node_wearG5", node_wearG5);
            }
        }
        dataMap.putString("dex_txid", dex_txid);
        sendData(WEARABLE_PREF_DATA_PATH, dataMap.toByteArray());

        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
        if (!node_wearG5.equals(dataMap.getString("node_wearG5", ""))) {
            Log.d(TAG, "syncPrefData save to SharedPreferences - node_wearG5:" + dataMap.getString("node_wearG5", ""));
            prefs.putString("node_wearG5", node_wearG5);
            prefs.commit();
        }
    }

    private DataMap dataMap(TransmitterData bg) {//KS
        DataMap dataMap = new DataMap();
        String json = bg.toS();
        Log.d(TAG, "dataMap BG GSON: " + json);
        dataMap.putString("bgs", json);
        return dataMap;
    }

    private void requestData() {
        sendData(WEARABLE_RESEND_PATH, null);
    }

    private void sendData(String path, byte[] payload) {
        if (path == null) return;
        new DataRequester(this, path, payload).execute();
    }

    private void googleApiConnect() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
    }

    @Override
    public void onPeerConnected(Node peer) {//KS
        super.onPeerConnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerConnected peer name & ID: " + name + "|" + id);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sendPrefSettings();
        if (mPrefs.getBoolean("enable_wearG5", false) && !mPrefs.getBoolean("force_wearG5", false)) {
            stopBtG5Service();
            ListenerService.requestData(this);
        }
    }

    @Override
    public void onPeerDisconnected(Node peer) {//KS
        super.onPeerDisconnected(peer);
        String id = peer.getId();
        String name = peer.getDisplayName();
        Log.d(TAG, "onPeerDisconnected peer name & ID: " + name + "|" + id);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (mPrefs.getBoolean("enable_wearG5", false)) {
            startBtG5Service();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand entered");
        Home.setAppContext(getApplicationContext());
        xdrip.checkAppContext(getApplicationContext());
        final PowerManager.WakeLock wl = JoH.getWakeLock("watchlistener-onstart",60000);
        last_send_previous = PersistentStore.getLong(pref_last_send_previous); // 0 if undef
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//KS
        listenForChangeInSettings();//KS
        is_using_g5 = mPrefs.getBoolean("g5_collection_method", false);//DexCollectionType.DexcomG5
        if (intent != null && ACTION_RESEND.equals(intent.getAction())) {
            googleApiConnect();
            requestData();
        } else if (intent != null && ACTION_SENDDATA.equals(intent.getAction())) {
            final Bundle bundle = intent.getExtras();
            sendData(bundle.getString(FIELD_SENDPATH), bundle.getByteArray(FIELD_PAYLOAD));
        }
        JoH.releaseWakeLock(wl);
        return START_STICKY;
    }

    final private SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {//KS
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            Log.d(TAG, "OnSharedPreferenceChangeListener entered");
            if(key.compareTo("enable_wearG5") == 0 || key.compareTo("force_wearG5") == 0 || key.compareTo("node_wearG5") == 0) {
                Log.i(TAG, "OnSharedPreferenceChangeListener enable_wearG5 || force_wearG5 changed!");
                sendPrefSettings();
                processConnectG5();
            }
            else if(key.compareTo("dex_txid") == 0){
                processConnectG5();
            }
        }
    };

    private void listenForChangeInSettings() {//KS
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);
        // TODO do we need an unregister!?
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {

        DataMap dataMap;

        for (DataEvent event : dataEvents) {

            if (event.getType() == DataEvent.TYPE_CHANGED) {


                String path = event.getDataItem().getUri().getPath();
                if (path.equals(OPEN_SETTINGS)) {
                    //TODO: OpenSettings
                    Intent intent = new Intent(this, NWPreferences.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } else if (path.equals(WEARABLE_DATA_PATH)) {

                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent messageIntent = new Intent();
                    messageIntent.setAction(Intent.ACTION_SEND);
                    messageIntent.putExtra("data", dataMap.toBundle());
                    LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
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
                } else if (path.equals(SYNC_DB_PATH)) {//KS
                    Log.d(TAG, "onDataChanged SYNC_DB_PATH=" + path);
                    final PowerManager.WakeLock wl = JoH.getWakeLock("watchlistener-SYNC_DB_PATH",120000);
                    TransmitterData last_bg = TransmitterData.last();
                    if (last_bg != null && last_send_previous <= last_bg.timestamp) {
                        Log.d(TAG, "onDataChanged SYNC_DB_PATH requestData for last_send_previous < last_bg.timestamp:" + JoH.dateTimeText(last_send_previous) + "<="+ JoH.dateTimeText(last_bg.timestamp));
                        requestData();
                    }
                    JoH.releaseWakeLock(wl);
                    Sensor.DeleteAndInitDb(getApplicationContext());
                    PersistentStore.setLong(pref_last_send_previous, 0);
                } else if (path.equals(WEARABLE_SENSOR_DATA_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    syncSensorData(dataMap, getApplicationContext());
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
                } else if (path.equals(DATA_ITEM_RECEIVED_PATH)) {//KS
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Log.d(TAG, "onDataChanged path=" + path + " DataMap=" + dataMap);
                    long timeOfLastBG = dataMap.getLong("timeOfLastBG", 0);
                    if (timeOfLastBG > 0) {
                        Log.d(TAG, "onDataChanged received from sendDataReceived current last_send_previous=" + JoH.dateTimeText(last_send_previous));
                        Log.d(TAG, "onDataChanged received from sendDataReceived timeOfLastBG=" + JoH.dateTimeText(timeOfLastBG) + " Path=" + path);
                        last_send_previous = timeOfLastBG;
                        PersistentStore.setLong(pref_last_send_previous, last_send_previous);
                        Log.d(TAG, "onDataChanged received from sendDataReceived update last_send_previous=" + JoH.dateTimeText(last_send_previous));
                    }
                }
            }
        }
    }

    private void syncPrefData(DataMap dataMap) {//KS
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();

        Log.d(TAG, "syncPrefData dataMap=" + dataMap);

        String dexCollector = dataMap.getString("dex_collection_method", "DexcomG5");//BluetoothWixel
        DexCollectionType collectionType = DexCollectionType.getType( dexCollector);
        is_using_g5 = (collectionType == DexCollectionType.DexcomG5);
        Log.d(TAG, "syncPrefData is_using_g5:" + is_using_g5);
        prefs.putBoolean("g5_collection_method", is_using_g5);

        boolean enable_wearG5 = is_using_g5 && dataMap.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = is_using_g5 && dataMap.getBoolean("force_wearG5", false);
        String node_wearG5 = dataMap.getString("node_wearG5", "");
        String prefs_node_wearG5 = mPrefs.getString("node_wearG5", "");
        boolean change = false;
        Log.d(TAG, "syncPrefData enter enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5 + " prefs_node_wearG5:" + prefs_node_wearG5 + " localnode:" + localnode);

        if (!node_wearG5.equals(prefs_node_wearG5)) {
            change = true;
            prefs.putString("node_wearG5", node_wearG5);
            Log.d(TAG, "syncPrefData node_wearG5 pref set to dataMap:" + node_wearG5);
        }
        if (force_wearG5 && node_wearG5.equals("")) {
            change = true;
            prefs.putString("node_wearG5", localnode);
            node_wearG5 = localnode;
            Log.d(TAG, "syncPrefData node_wearG5 set empty string to localnode:" + localnode);
        }
        if (!node_wearG5.equals(localnode)) {
            //change = true;
            force_wearG5 = false;
        }

        if (force_wearG5 != mPrefs.getBoolean("force_wearG5", false)) {
            change = true;
            Log.d(TAG, "syncPrefData force_wearG5:" + force_wearG5);
            prefs.putBoolean("force_wearG5", force_wearG5);
        }
        if (enable_wearG5 != mPrefs.getBoolean("enable_wearG5", false)) {
            change = true;
            Log.d(TAG, "syncPrefData enable_wearG5:" + force_wearG5);
            prefs.putBoolean("enable_wearG5", enable_wearG5);
        }

        String dex_txid = dataMap.getString("dex_txid", "ABCDEF");
        Log.d(TAG, "syncPrefData dataMap dex_txid=" + dex_txid);
        if (!dex_txid.equals(mPrefs.getString("dex_txid", "ABCDEF"))) {
            Log.d(TAG, "syncPrefData dex_txid:" + dex_txid);
            prefs.putString("dex_txid", dex_txid);
            stopBtG5Service();
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

        if (change) {
            prefs.commit();
            //sendPrefSettings();
            //processConnectG5();
        }
        enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
        node_wearG5 = mPrefs.getString("node_wearG5", "");
        Log.d(TAG, "syncPrefData exit enable_wearG5: " + enable_wearG5 + " force_wearG5:" + force_wearG5 + " node_wearG5:" + node_wearG5);
    }

    //Assumes Wear is connected to phone
    private void processConnectG5() {//KS
        Log.d(TAG, "processConnectG5 enter");
        boolean enable_wearG5 = mPrefs.getBoolean("enable_wearG5", false);
        boolean force_wearG5 = mPrefs.getBoolean("force_wearG5", false);
        if (enable_wearG5) {
            Log.d(TAG, "processConnectG5 enable_wearG5=true");
            if (!force_wearG5){
                Log.d(TAG, "processConnectG5 force_wearG5=false - stopBtG5Service and requestData");
                stopBtG5Service();
                ListenerService.requestData(this);
            }
            else {
                Log.d(TAG, "processConnectG5 force_wearG5=true - startBtG5Service");
                startBtG5Service();
            }
        }
        else {
            Log.d(TAG, "processConnectG5 enable_wearG5=false - stopBtG5Service and requestData");
            stopBtG5Service();
            ListenerService.requestData(this);
        }
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
                        Log.i(TAG, "syncSensorData createUpdate Sensor with uuid=" + uuid + " started at=" + started_at);
                    } else
                        Log.e(TAG, "syncSensorData Failed to createUpdate new Sensor for uuid=" + uuid);
                } else
                    Log.d(TAG, "syncSensorData Sensor already exists with uuid=" + uuid);
            }
        }
    }

    private synchronized void syncCalibrationData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncCalibrationData");

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
                        String bgrecord = entry.getString("bgs");
                        if (bgrecord != null) {
                            Calibration bgData = gson.fromJson(bgrecord, Calibration.class);
                            Calibration exists = Calibration.findByUuid(bgData.uuid);
                            bgData.sensor = sensor;
                            if (exists != null) {
                                Log.d(TAG, "syncCalibrationData Calibration exists for uuid=" + bgData.uuid + " bg=" + bgData.bg + " timestamp=" + bgData.timestamp + " timeString=" +  JoH.dateTimeText(bgData.timestamp));
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
        }
    }

    private synchronized void syncBgData(DataMap dataMap, Context context) {//KS
        Log.d(TAG, "syncBGData");

        ArrayList<DataMap> entries = dataMap.getDataMapArrayList("entries");
        Log.d(TAG, "syncBGData add BgReading Table" );
        if (entries != null) {

            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .registerTypeAdapter(Date.class, new DateTypeAdapter())
                    .serializeSpecialFloatingPointValues()
                    .create();

            Log.d(TAG, "syncBGData add BgReading Table entries count=" + entries.size());
            Sensor.InitDb(context);//ensure database has already been initialized
            Sensor sensor = Sensor.currentSensor();
            if (sensor != null) {
                for (DataMap entry : entries) {
                    if (entry != null) {
                        String bgrecord = entry.getString("bgs");
                        if (bgrecord != null) {
                            BgReading bgData = gson.fromJson(bgrecord, BgReading.class);
                            BgReading exists = BgReading.getForTimestampExists(bgData.timestamp);
                            exists = exists != null ? exists : BgReading.findByUuid(bgData.uuid);
                            if (exists != null) {
                                Log.d(TAG, "syncBGData BG already exists for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" +  JoH.dateTimeText(bgData.timestamp));
                                Log.d(TAG, "syncBGData exists timeString=" +  JoH.dateTimeText(exists.timestamp) + "  exists.calibration.uuid=" + exists.calibration.uuid + " exists=" + exists.toS());

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

                                String calibrationUuid = entry.getString("calibrationUuid");
                                if (calibrationUuid != null && !calibrationUuid.isEmpty()) {
                                    Calibration calibration = Calibration.byuuid(calibrationUuid);
                                    if (calibration != null) {
                                        exists.calibration = calibration;
                                        exists.sensor = sensor;
                                        exists.save();
                                    }
                                    else {
                                        Log.e(TAG, "syncBGData calibrationUuid not found by byuuid; calibrationUuid=" + calibrationUuid + " bgData.calibration_uuid=" + bgData.calibration_uuid);
                                    }
                                }
                                else {
                                    Log.e(TAG, "syncBGData calibrationUuid not sent");
                                }
                            } else {
                                Calibration calibration = Calibration.byuuid(bgData.calibration_uuid);
                                if (calibration != null) {
                                    bgData.calibration = calibration;
                                    bgData.sensor = sensor;
                                    Log.d(TAG, "syncBGData add BG; does NOT exist for uuid=" + bgData.uuid + " timestamp=" + bgData.timestamp + " timeString=" + JoH.dateTimeText(bgData.timestamp));
                                    String calibrationUuid = entry.getString("calibrationUuid");
                                    if (calibrationUuid != null && !calibrationUuid.isEmpty()) {
                                        calibration = Calibration.byuuid(calibrationUuid);
                                        if (calibration != null) {
                                            bgData.calibration = calibration;
                                            bgData.sensor = sensor;
                                            bgData.save();
                                        }
                                        else {
                                            Log.e(TAG, "syncBGData calibrationUuid not found by byuuid; calibrationUuid=" + calibrationUuid + " bgData.calibration_uuid=" + bgData.calibration_uuid);
                                        }
                                    }
                                    else {
                                        Log.e(TAG, "syncBGData calibrationUuid not sent");
                                    }

                                    //BgSendQueue.handleNewBgReading(bgData, "create", getApplicationContext() );
                                    exists = BgReading.findByUuid(bgData.uuid);
                                    if (exists != null)
                                        Log.d(TAG, "syncBGData BG GSON saved BG: " + exists.toS());
                                    else
                                        Log.e(TAG, "syncBGData BG GSON NOT saved");
                                }
                                else {
                                    Log.e(TAG, "syncBGData bgData.calibration_uuid not found by byuuid; calibration_uuid=" + bgData.calibration_uuid);
                                }
                            }
                        }
                    }
                }
                BgSendQueue.resendData(getApplicationContext());
            }
        }
    }

    private void startBtG5Service() {//KS
        Log.d(TAG, "startBtG5Service");
        if (is_using_g5) {
            Context myContext = getApplicationContext();
            if (checkLocationPermissions()) {
                Log.d(TAG, "startBtG5Service start G5CollectionService");
                if (restartWatchDog())
                    stopBtG5Service();
                G5CollectionService.keep_running = true;
                myContext.startService(new Intent(myContext, G5CollectionService.class));
                Log.d(TAG, "startBtG5Service AFTER startService G5CollectionService mLocationPermissionApproved " + mLocationPermissionApproved);
            }
        }
    }

    private boolean checkLocationPermissions() {//KS
        Context myContext = getApplicationContext();
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        myContext,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions  mLocationPermissionApproved:" + mLocationPermissionApproved);

        // Display Activity to get user permission
        if (!mLocationPermissionApproved) {
            Intent permissionIntent = new Intent(getApplicationContext(), LocationPermissionActivity.class);
            permissionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissionIntent);
        }
        // Enables app to handle 23+ (M+) style permissions.
        mLocationPermissionApproved =
                ActivityCompat.checkSelfPermission(
                        getApplicationContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        Log.d(TAG, "checkLocationPermissions mLocationPermissionApproved:" + mLocationPermissionApproved);
        return mLocationPermissionApproved;
    }

    private boolean restartWatchDog() {//KS from app/MissedReadingService.java
        final long stale_millis = Home.stale_data_millis();
        if (is_using_g5) {//(prefs.getBoolean("aggressive_service_restart", false) || DexCollectionType.isFlakey()) {
            if (!BgReading.last_within_millis(stale_millis)) {
                if (JoH.ratelimit("aggressive-restart", aggressive_backoff_timer)) {
                    Log.e(TAG, "Aggressively restarting wear G5 collector service due to lack of reception: backoff: "+aggressive_backoff_timer);
                    if (aggressive_backoff_timer < 1200) aggressive_backoff_timer+=60;
                    return true;//CollectionServiceStarter.restartCollectionService
                } else {
                    aggressive_backoff_timer = 120; // reset
                }
            }
        }
        return false;
    }

    private void stopBtG5Service() {//KS
        Context myContext = getApplicationContext();
        Log.d(TAG, "stopBtG5Service call stopService");
        myContext.stopService(new Intent(myContext, G5CollectionService.class));
        Log.d(TAG, "stopBtG5Service should have called onDestroy");
    }

    public static void requestData(Context context) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_RESEND);
        context.startService(intent);
    }

    // generic send data
    public static void SendData(Context context, String path, byte[] payload) {
        Intent intent = new Intent(context, ListenerService.class);
        intent.setAction(ACTION_SENDDATA);
        intent.putExtra(FIELD_SENDPATH, path);
        intent.putExtra(FIELD_PAYLOAD, payload);
        context.startService(intent);
    }

    private void updatePhoneSyncBgsCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        mPhoneNodeId = pickBestNodeId(connectedNodes);
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

    @Override
    public void onConnected(Bundle bundle) {
        CapabilityApi.CapabilityListener capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        updatePhoneSyncBgsCapability(capabilityInfo);
                        Log.d(TAG, "onConnected onCapabilityChanged mPhoneNodeID:" + mPhoneNodeId);
                    }
                };

        Wearable.CapabilityApi.addCapabilityListener(
                googleApiClient,
                capabilityListener,
                CAPABILITY_PHONE_APP);

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
        }
    }
}
