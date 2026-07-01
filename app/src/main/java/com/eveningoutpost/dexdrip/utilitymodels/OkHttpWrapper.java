package com.eveningoutpost.dexdrip.utilitymodels;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class OkHttpWrapper {

    private static volatile OkHttpClient instance;

    public static OkHttpClient getClient() {
        if (instance == null) {
            synchronized (OkHttpWrapper.class) {
                if (instance == null) {
                    instance = new OkHttpClient.Builder()
                            .connectTimeout(30, TimeUnit.SECONDS)
                            .readTimeout(60, TimeUnit.SECONDS)
                            .writeTimeout(20, TimeUnit.SECONDS)
                            .build();
                }
            }
        }
        return instance;
    }

}
