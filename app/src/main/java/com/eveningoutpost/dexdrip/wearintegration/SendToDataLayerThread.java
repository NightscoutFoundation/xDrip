package com.eveningoutpost.dexdrip.wearintegration;

import android.os.AsyncTask;
import android.util.Log;

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
    String path;

    SendToDataLayerThread(String path, GoogleApiClient pGoogleApiClient) {
        this.path = path;
        googleApiClient = pGoogleApiClient;
    }

    @Override
    protected Void doInBackground(DataMap... params) {
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

        return null;
    }
}
