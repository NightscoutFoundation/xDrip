package com.eveningoutpost.dexdrip.ShareModels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.OkHttpClient;

import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ShareRest {
    public final static String TAG = ShareRest.class.getSimpleName();

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public static DexcomShareInterface jsonBodyInterface() {
        RestAdapter adapter = authoirizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    public static DexcomShareInterface emptyBodyInterface() {
        RestAdapter adapter = getBgAdapterBuilder().build();
        DexcomShareInterface checkSessionActive =
                adapter.create(DexcomShareInterface.class);
        return checkSessionActive;
    }

    private static RestAdapter.Builder authoirizeAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(getOkClient())
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog(TAG))
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(authorizationRequestInterceptor())
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    private static RestAdapter.Builder getBgAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(getOkClient())
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog(TAG))
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(getBgRequestInterceptor())
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    public static RequestInterceptor authorizationRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestInterceptor.RequestFacade request) {
                request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
                request.addHeader("Content-Type", "application/json");
                request.addHeader("Accept", "application/json");
            }
        };
    }
    public static RequestInterceptor getBgRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void intercept(RequestInterceptor.RequestFacade request) {
                request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
                request.addHeader("Content-Type", "application/json");
                request.addHeader("Content-Length", "0");
                request.addHeader("Accept", "application/json");
            }
        };
    }

    public static OkHttpClient getOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            } };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {

                @Override
                public boolean verify(String hostname, SSLSession session) {
                        return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static OkClient getOkClient (){
        OkHttpClient client1 = getOkHttpClient();
        OkClient _client = new OkClient(client1);
        return _client;
    }

    public static Map<String, String> querySessionMap(String sessionId) {
        Map map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        return map;
    }
}
