package com.eveningoutpost.dexdrip.cgm.sharefollow;

import android.support.annotation.NonNull;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.UtilityModels.OkHttpWrapper.enableTls12OnPreLollipop;

/**
 * jamorham
 *
 * Retrofit instance cache with lazy instantiation, url change detection and common interceptors
 *
 * Moving towards being fully reusable from anywhere
 *
 */

public class RetrofitBase {

    private static final boolean D = false;

    private static final ConcurrentHashMap<String, Retrofit> instances = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> urls = new ConcurrentHashMap<>();

    // TODO make fully reusable
    public static Retrofit getRetrofitInstance(final String TAG, final String url, boolean useGzip) throws IllegalArgumentException {

        Retrofit instance = instances.get(TAG);
        if (instance == null || !urls.get(TAG).equals(url)) {
            synchronized (instances) {
                if (emptyString(url)) {
                    UserError.Log.d(TAG, "Empty url - cannot create instance");
                    return null;
                }
                UserError.Log.d(TAG, "Creating new instance for: " + url);
                final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
                if (D) {
                    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                }
                final OkHttpClient client = enableTls12OnPreLollipop(new OkHttpClient.Builder())
                        .addInterceptor(httpLoggingInterceptor)
                        .addInterceptor(new InfoInterceptor(TAG))
                        .addInterceptor(useGzip ? new GzipRequestInterceptor() : new NullInterceptor())
                        .build();

                instances.put(TAG, instance = new retrofit2.Retrofit.Builder()
                        .baseUrl(url)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build());
                urls.put(TAG, url); // save creation url for quick search
            }
        }
        return instance;
    }

    // does nothing, just so we can use builder with ternary
    public static class NullInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(@NonNull Chain chain) throws IOException {
            return chain.proceed(chain.request());
        }
    }

}
