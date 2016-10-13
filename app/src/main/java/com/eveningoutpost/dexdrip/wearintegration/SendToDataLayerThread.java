package com.eveningoutpost.dexdrip.wearintegration;

import android.os.AsyncTask;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Created by stephenblack on 12/26/14.
 */
class SendToDataLayerThread extends AsyncTask<DataMap,Void,Void> {
    private GoogleApiClient googleApiClient;
    private static int concurrency = 0;
    private static int state = 0;
    private static final String TAG = "jamorham wear";
    private static final boolean testlockup = false; // always false in production
    String path;

    SendToDataLayerThread(String path, GoogleApiClient pGoogleApiClient) {
        this.path = path;
        googleApiClient = pGoogleApiClient;
    }

    @Override
    protected void onPreExecute()
    {
        concurrency++;
        if (concurrency>3) {
            final String err = "Wear Integration deadlock detected!! state:"+state+" @"+ JoH.hourMinuteString();
            Home.toaststaticnext(err);
            UserError.Log.e(TAG,err);
        }
        if (concurrency<0) Home.toaststaticnext("Wear Integration impossible concurrency!!");
        UserError.Log.d(TAG, "SendDataToLayerThread pre-execute concurrency: " + concurrency);
    }

    @Override
    protected Void doInBackground(DataMap... params) {
        if (testlockup) {
            try {
                UserError.Log.e(TAG,"WARNING RUNNING TEST LOCK UP CODE - NEVER FOR PRODUCTION");
                Thread.sleep(1000000); // DEEEBBUUGGGG
            } catch (Exception e) {
            }
        }
        sendToWear(params);
        concurrency--;
        UserError.Log.d(TAG, "SendDataToLayerThread post-execute concurrency: " + concurrency);
        return null;
    }

    // Debug function to expose where it might be locking up
    private synchronized void sendToWear(final DataMap... params) {
        try {
            if (state != 0) {
                UserError.Log.e(TAG, "WEAR STATE ERROR: state=" + state);
            }
            state = 1;
            final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await(15, TimeUnit.SECONDS);

            state = 2;
            for (Node node : nodes.getNodes()) {
                state = 3;
                for (DataMap dataMap : params) {
                    state = 4;
                    PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                    state = 5;
                    putDMR.getDataMap().putAll(dataMap);
                    state = 6;
                    PutDataRequest request = putDMR.asPutDataRequest();
                    state = 7;
                    DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, request).await(15, TimeUnit.SECONDS);
                    state = 8;
                    if (result.getStatus().isSuccess()) {
                        UserError.Log.d(TAG, "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                    } else {
                        UserError.Log.e(TAG, "ERROR: failed to send DataMap");
                        result = Wearable.DataApi.putDataItem(googleApiClient, request).await(25, TimeUnit.SECONDS);
                        if (result.getStatus().isSuccess()) {
                            UserError.Log.d(TAG, "DataMap retry: " + dataMap + " sent to: " + node.getDisplayName());
                        } else {
                            UserError.Log.e(TAG, "ERROR on retry: failed to send DataMap: "+result.getStatus().toString());
                        }
                    }
                    state = 9;
                }
            }
            state = 0;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in sendToWear: " + e.toString());
        }
    }
}
