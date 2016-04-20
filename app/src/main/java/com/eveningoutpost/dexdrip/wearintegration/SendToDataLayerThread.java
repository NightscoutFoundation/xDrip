package com.eveningoutpost.dexdrip.wearintegration;

import android.os.AsyncTask;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by stephenblack on 12/26/14.
 */
class SendToDataLayerThread extends AsyncTask<DataMap,Void,Void> {
    private GoogleApiClient googleApiClient;
    private static int concurrency = 0;
    private static final String TAG = "jamorham wear";
    String path;

    SendToDataLayerThread(String path, GoogleApiClient pGoogleApiClient) {
        this.path = path;
        googleApiClient = pGoogleApiClient;
    }

    @Override
    protected void onPreExecute()
    {
        concurrency++;
        if (concurrency>2) Home.toaststaticnext("Wear Integration deadlock detected!!");
        if (concurrency<0) Home.toaststaticnext("Wear Integration impossible concurrency!!");
        Log.d(TAG,"SendDataToLayerThread pre-execute concurrency: "+concurrency);
    }

    @Override
    protected Void doInBackground(DataMap... params) {
       // try {
       //    Thread.sleep(1000000); // DEEEBBUUGGGG
       // } catch (Exception e)
       // {
       // }
        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await();
        for (Node node : nodes.getNodes()) {
            for (DataMap dataMap : params) {
                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient,request).await();
                if (result.getStatus().isSuccess()) {
                    Log.d("SendDataThread", "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    Log.d("SendDataThread", "ERROR: failed to send DataMap");
                }
            }
        }
        concurrency--;
        Log.d(TAG,"SendDataToLayerThread post-execute concurrency: "+concurrency);
        return null;
    }
}
