package com.eveningoutpost.dexdrip.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by jamorham on 29/01/2016.
 */
public class WebAppHelper extends AsyncTask<String, Integer, Integer> {

    // TODO probably migrate uploader here as well

    private final String TAG = "jamorham webapphelper";
    private final OkHttpClient client = new OkHttpClient();
    private final Preferences.OnServiceTaskCompleted listener;
    private byte[] body = new byte[0];

    public WebAppHelper(Preferences.OnServiceTaskCompleted listener) {
        this.listener = listener;
    }

    protected void onPostExecute(Integer result) {
        Log.d(TAG, "OnPostExecute: body: " + result);
        if (listener != null) listener.onTaskCompleted(this.body);
    }

    @Override
    protected Integer doInBackground(String... url) {
        try {
            Log.d(TAG, "Processing URL: " + url[0]);
            Request request = new Request.Builder()
                    .header("User-Agent", "Mozilla/5.0 (jamorham)")
                    .header("Connection", "close")
                    .url(url[0])
                    .build();

            client.setConnectTimeout(15, TimeUnit.SECONDS);
            client.setReadTimeout(30, TimeUnit.SECONDS);
            client.setWriteTimeout(30, TimeUnit.SECONDS);

            final Response response = client.newCall(request).execute();
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            body = response.body().bytes();
        } catch (Exception e) {
            Log.d(TAG, "Exception in background task: " + e.toString());
        }
        return body.length;
    }
}
