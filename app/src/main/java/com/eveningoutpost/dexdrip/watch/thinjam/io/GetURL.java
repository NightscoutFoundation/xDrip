package com.eveningoutpost.dexdrip.watch.thinjam.io;

import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;

import java.io.IOException;

import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// jamorham

// Just some utility methods to retrieve urls

public class GetURL {

    private static OkHttpClient httpClient = null;

    public static String getURL(final String URL) {
        try {
            val response = getURLresponse(URL);
            if (response.isSuccessful()) {
                return response.body().string();
            }
        } catch (NullPointerException | IOException e) {
            //
        }
        return null;
    }

    public static byte[] getURLbytes(final String URL) {
        try {
            val response = getURLresponse(URL);
            if (response.isSuccessful()) {
                return response.body().bytes();
            }
        } catch (NullPointerException | IOException e) {
            //
        }
        return null;
    }

    // must be on background thread
    private static Response getURLresponse(final String URL) {

        if (httpClient == null) {
            httpClient = OkHttpWrapper.getClient().newBuilder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                    .build();
        }


        final Request request = new Request.Builder()
                // Mozilla header facilitates compression
                .header("User-Agent", "Mozilla/5.0")
                .header("Connection", "close")
                .url(URL)
                .build();

        try {
            return httpClient.newCall(request).execute();
        } catch (NullPointerException | IOException e) {
            // meh fall through to default failure
        }

        return null;
    }

}
