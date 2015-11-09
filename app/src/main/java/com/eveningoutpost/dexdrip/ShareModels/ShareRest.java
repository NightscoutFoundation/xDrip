package com.eveningoutpost.dexdrip.ShareModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.ShareModels.Models.ExistingFollower;
import com.eveningoutpost.dexdrip.ShareModels.Models.InvitationPayload;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareAuthenticationBody;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareUploadPayload;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okio.Buffer;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

/**
 * Created by stephenblack on 12/26/14.
 */
public class ShareRest {
    public static String TAG = ShareRest.class.getSimpleName();

    private String sessionId;

    private String username;
    private String password;
    private String serialNumber;
    private DexcomShare dexcomShareApi;

    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if ("dexcom_account_name".equals(key)) {
                username = sharedPreferences.getString(key, null);
            } else if ("dexcom_account_password".equals(key)) {
                password = sharedPreferences.getString(key, null);
            } else if ("share_key".equals(key)) {
                serialNumber = sharedPreferences.getString(key, null);
            }

        }
    };

    private static final String SHARE_BASE_URL = "https://share1.dexcom.com/ShareWebServices/Services/";
    private SharedPreferences sharedPreferences;

    public ShareRest (Context context, OkHttpClient okHttpClient) {
        OkHttpClient httpClient = okHttpClient != null ? okHttpClient : getOkHttpClient();

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SHARE_BASE_URL)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        dexcomShareApi = retrofit.create(DexcomShare.class);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sessionId = sharedPreferences.getString("dexcom_share_session_id", null);
        username = sharedPreferences.getString("dexcom_account_name", null);
        password = sharedPreferences.getString("dexcom_account_password", null);
        serialNumber = sharedPreferences.getString("share_key", null);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        if ("".equals(sessionId)) // migrate previous empty sessionIds to null;
            sessionId = null;
    }

    public OkHttpClient getOkHttpClient() {
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
            okHttpClient.networkInterceptors().add(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    // Add user-agent and relevant headers.
                    Request original = chain.request();
                    Request copy = original.newBuilder().build();
                    Request modifiedRequest = original.newBuilder()
                            .header("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0")
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .build();
                    Log.d(TAG, "Sending request: " + modifiedRequest.toString());
                    Buffer buffer = new Buffer();
                    copy.body().writeTo(buffer);
                    Log.d(TAG, "Request body: " + buffer.readUtf8());

                    Response response = chain.proceed(modifiedRequest);
                    Log.d(TAG, "Received response: " + response.toString());
                    if (response.body() != null) {
                        MediaType contentType = response.body().contentType();
                        String bodyString = response.body().string();
                        Log.d(TAG, "Response body: " + bodyString);
                        return response.newBuilder().body(ResponseBody.create(contentType, bodyString)).build();
                    } else
                        return response;
                }
            });

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

    public String getSessionId() {
        // getValidSessionId:
        // cached session id?
        // y:  checkSessionActive
        //        true: exit
        //        false:  SRMS
        // n:  getSessionId()
        //        updatePublisherAccountInfo
        //

        // SRMS:
        //    authenticatePublisherAccount
        //        StartRemoteMonitoringSession
        //            checkMonitorAssignment
        //              !AssignedToYou: updateMonitorAssignment
        //                  getValidSessionId

        if (sessionId == null || "".equals(sessionId)) {

        }
        return sessionId;
    }

    public void getContacts(Callback<List<ExistingFollower>> existingFollowerListener) {
        dexcomShareApi.getContacts(getSessionId()).enqueue(new AuthenticatingCallback<List<ExistingFollower>>(existingFollowerListener) {
            @Override
            void onRetry() {
                dexcomShareApi.getContacts(getSessionId()).enqueue(this);
            }
        });
    }

    public void uploadBGRecords(final ShareUploadPayload bg, Callback<ResponseBody> callback) {
        dexcomShareApi.uploadBGRecords(getSessionId(), bg).enqueue(new AuthenticatingCallback<ResponseBody>(callback) {
            @Override
            void onRetry() {
                dexcomShareApi.uploadBGRecords(getSessionId(), bg).enqueue(this);
            }
        });
    }

    public void createContact(final String followerName, final String followerEmail, Callback<String> callback) {
        dexcomShareApi.createContact(getSessionId(), followerName, followerEmail).enqueue(new AuthenticatingCallback<String>(callback) {
            @Override
            void onRetry() {
                dexcomShareApi.createContact(getSessionId(), followerName, followerEmail).enqueue(this);
            }
        });
    }

    public void createInvitationForContact(final String contactId, final InvitationPayload invitationPayload, Callback<String> callback) {
        dexcomShareApi.createInvitationForContact(getSessionId(), contactId, invitationPayload).enqueue(new AuthenticatingCallback<String>(callback) {
            @Override
            void onRetry() {
                dexcomShareApi.createInvitationForContact(getSessionId(), contactId, invitationPayload).enqueue(this);
            }
        });
    }

    public void deleteContact(final String contactId, Callback<ResponseBody> deleteFollowerListener) {
        dexcomShareApi.deleteContact(getSessionId(), contactId).enqueue(new AuthenticatingCallback<ResponseBody>(deleteFollowerListener) {
            @Override
            void onRetry() {
                dexcomShareApi.deleteContact(getSessionId(), contactId).enqueue(this);
            }
        });
    }

    public abstract class AuthenticatingCallback<T> implements Callback<T> {

        int attempts = 0;
        Callback<T> delegate;
        public AuthenticatingCallback (Callback<T> callback) {
            this.delegate = callback;
        }

        abstract void onRetry();

        @Override
        public void onResponse(retrofit.Response<T> response, Retrofit retrofit) {
            if (response.code() == 500 && attempts == 0) {
                // retry with new session ID
                attempts += 1;
                dexcomShareApi.getSessionId(new ShareAuthenticationBody(password, username).toMap()).enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(retrofit.Response<String> response, Retrofit retrofit) {
                        if (response.isSuccess()) {
                            sessionId = response.body();
                            ShareRest.this.sharedPreferences.edit().putString("dexcom_share_session_id", sessionId).apply();
                            onRetry();
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        delegate.onFailure(t);
                    }
                });
            } else {
                delegate.onResponse(response, retrofit);
            }
        }

        @Override
        public void onFailure(Throwable t) {
            delegate.onFailure(t);
        }
    }
}
