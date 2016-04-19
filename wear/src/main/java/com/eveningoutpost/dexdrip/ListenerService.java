package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ListenerService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final String WEARABLE_DATA_PATH = "/nightscout_watch_data";
    private static final String WEARABLE_RESEND_PATH = "/nightscout_watch_data_resend";
    private static final String OPEN_SETTINGS = "/openwearsettings";
    private static final String ACTION_RESEND = "com.dexdrip.stephenblack.nightwatch.RESEND_DATA";
    private static final String ACTION_SENDDATA = "com.dexdrip.stephenblack.nightwatch.SEND_DATA";
    private static final String ACTION_RESEND_BULK = "com.dexdrip.stephenblack.nightwatch.RESEND_BULK_DATA";
    private static final String FIELD_SENDPATH = "field_xdrip_plus_sendpath";
    private static final String FIELD_PAYLOAD = "field_xdrip_plus_payload";
    private static final String WEARABLE_TREATMENT_PAYLOAD = "/xdrip_plus_treatment_payload";
    private static final String WEARABLE_TOAST_NOTIFICATON = "/xdrip_plus_toast";
    private static final String TAG = "jamorham listener";

    GoogleApiClient googleApiClient;
    private static long lastRequest = 0;

    public class DataRequester extends AsyncTask<Void, Void, Void> {
        Context mContext;
        final String path;
        final byte[] payload;

        DataRequester(Context context, String thispath, byte[] thispayload) {
            mContext = context;
            path = thispath;
            payload = thispayload;
            Log.d(TAG, "DataRequester: " + thispath);
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (googleApiClient.isConnected()) {
                if (!path.equals(ACTION_RESEND) || (System.currentTimeMillis() - lastRequest > 20 * 1000)) { // enforce 20-second debounce period
                    lastRequest = System.currentTimeMillis();

                    NodeApi.GetConnectedNodesResult nodes =
                            Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
                    for (Node node : nodes.getNodes()) {
                        Wearable.MessageApi.sendMessage(googleApiClient, node.getId(), path, payload);
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

    public void requestData() {
        sendData(WEARABLE_RESEND_PATH, null);
    }

    public void sendData(String path, byte[] payload) {
        if (path == null) return;
        new DataRequester(this, path, payload).execute();
    }

    public void googleApiConnect() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        Wearable.MessageApi.addListener(googleApiClient, this);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_RESEND.equals(intent.getAction())) {
            googleApiConnect();
            requestData();
        } else if (intent != null && ACTION_SENDDATA.equals(intent.getAction())) {
            final Bundle bundle = intent.getExtras();
            sendData(bundle.getString(FIELD_SENDPATH), bundle.getByteArray(FIELD_PAYLOAD));
        }
        return START_STICKY;
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
                } else if (path.equals(WEARABLE_TOAST_NOTIFICATON))
                {
                    dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    Intent intent = new Intent(getApplicationContext(), Simulation.class);
                    intent.putExtra(path, dataMap.toBundle());
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(intent);
                }
            }
        }
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

    @Override
    public void onConnected(Bundle bundle) {
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
