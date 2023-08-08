package com.eveningoutpost.dexdrip.watch.thinjam.io;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import lombok.val;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.enableTls12OnPreLollipop;

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
            httpClient = enableTls12OnPreLollipop(new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS))
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
