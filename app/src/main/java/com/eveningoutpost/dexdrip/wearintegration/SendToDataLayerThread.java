package com.eveningoutpost.dexdrip.wearintegration;

import android.os.AsyncTask;
import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Emma Black on 12/26/14.
 */
class SendToDataLayerThread extends AsyncTask<DataMap,Void,Void> {
    private GoogleApiClient googleApiClient;
    private static int concurrency = 0;
    private static int state = 0;
    private static final String TAG = "jamorham wear";
    private static final ReentrantLock lock = new ReentrantLock();
    private static long lastlock = 0;
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
        if ((concurrency > 12) || ((concurrency > 3 && (lastlock != 0) && (JoH.tsl() - lastlock) > 300000))) {//KS increase from 8 to 12
            // error if 9 concurrent threads or lock held for >5 minutes with concurrency of 4
            final String err = "Wear Integration deadlock detected!! "+((lastlock !=0) ? "locked" : "")+" state:"+state+" @"+ JoH.hourMinuteString();
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
        if (!lock.tryLock()) {
            Log.d(TAG, "Concurrent access - waiting for thread unlock");
            lock.lock(); // enforce single threading
            Log.d(TAG, "Thread unlocked - proceeding");
        }
        lastlock=JoH.tsl();
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
                    putDMR.setUrgent();
                    state = 6;
                    PutDataRequest request = putDMR.asPutDataRequest();
                    state = 7;
                    DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, request).await(15, TimeUnit.SECONDS);
                    state = 8;
                    if (result.getStatus().isSuccess()) {
                        UserError.Log.d(TAG, "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                    } else {
                        UserError.Log.e(TAG, "ERROR: failed to send DataMap");
                        result = Wearable.DataApi.putDataItem(googleApiClient, request).await(30, TimeUnit.SECONDS);
                        if (result.getStatus().isSuccess()) {
                            UserError.Log.d(TAG, "DataMap retry: " + dataMap + " sent to: " + node.getDisplayName());
                        } else {
                            UserError.Log.e(TAG, "ERROR on retry: failed to send DataMap: " + result.getStatus().toString());
                        }
                    }
                    state = 9;
                }
            }
            state = 0;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in sendToWear: " + e.toString());
        } finally {
            lastlock=0;
            lock.unlock();
        }
    }
}
