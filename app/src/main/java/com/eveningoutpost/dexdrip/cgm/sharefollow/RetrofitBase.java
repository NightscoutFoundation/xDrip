package com.eveningoutpost.dexdrip.cgm.sharefollow;

import androidx.annotation.NonNull;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.cgm.nsfollow.GzipRequestInterceptor;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper.enableTls12OnPreLollipop;

/**
 * jamorham
 *
 * Retrofit instance cache with lazy instantiation, url change detection and common interceptors
 *
 * Moving towards being fully reusable from anywhere
 *
 */

public class RetrofitBase {

    private static final ConcurrentHashMap<String, Retrofit> instances = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> urls = new ConcurrentHashMap<>();

    private static Converter.Factory createGsonConverter(Type type, Object typeAdapter) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(type, typeAdapter);
        Gson gson = gsonBuilder.create();
        return GsonConverterFactory.create(gson);
    }

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
                final OkHttpClient.Builder httpClient = enableTls12OnPreLollipop(new OkHttpClient.Builder())
                        .addInterceptor(new InfoInterceptor(TAG))
                        .addInterceptor(useGzip ? new GzipRequestInterceptor() : new NullInterceptor());

                if (UserError.ExtraLogTags.shouldLogTag(TAG, android.util.Log.VERBOSE)) {
                    UserError.Log.v(TAG, "Enable logging of request and response lines and their respective headers and bodies.");
                    final HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
                    httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                    httpLoggingInterceptor.redactHeader("Authorization");
                    httpLoggingInterceptor.redactHeader("Cookie");
                    httpClient.addInterceptor(httpLoggingInterceptor);
                }

                instances.put(TAG, instance = new retrofit2.Retrofit.Builder()
                        .baseUrl(url)
                        .client(httpClient.build())
                        .addConverterFactory(createGsonConverter(Dex_Constants.TREND_ARROW_VALUES.class, new ShareTrendDeserializer()))
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
