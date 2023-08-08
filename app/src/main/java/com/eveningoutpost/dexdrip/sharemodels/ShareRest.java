package com.eveningoutpost.dexdrip.sharemodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.sharemodels.models.ExistingFollower;
import com.eveningoutpost.dexdrip.sharemodels.models.InvitationPayload;
import com.eveningoutpost.dexdrip.sharemodels.models.ShareAuthenticationBody;
import com.eveningoutpost.dexdrip.sharemodels.models.ShareUploadPayload;
import com.eveningoutpost.dexdrip.xdrip;
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
import java.util.concurrent.ExecutionException;

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
 * Created by Emma Black on 12/26/14.
 */
public class ShareRest {
    public static String TAG = ShareRest.class.getSimpleName();

    private String sessionId;

    private String username;
    private String password;
    private String serialNumber;
    private DexcomShare dexcomShareApi;

    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
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

    private static final String US_SHARE_BASE_URL = "https://share2.dexcom.com/ShareWebServices/Services/";
    private static final String NON_US_SHARE_BASE_URL = "https://shareous1.dexcom.com/ShareWebServices/Services/";
    private SharedPreferences sharedPreferences;

    public ShareRest(Context context, OkHttpClient okHttpClient) {

        try {
            OkHttpClient httpClient = okHttpClient != null ? okHttpClient : getOkHttpClient();

            if (httpClient == null) httpClient = getOkHttpClient(); // try again on failure
            // if fails second time we've got big problems

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(sharedPreferences.getBoolean("dex_share_us_acct", true) ? US_SHARE_BASE_URL : NON_US_SHARE_BASE_URL)
                    .client(httpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
            dexcomShareApi = retrofit.create(DexcomShare.class);
            sessionId = sharedPreferences.getString("dexcom_share_session_id", null);
            username = sharedPreferences.getString("dexcom_account_name", null);
            password = sharedPreferences.getString("dexcom_account_password", null);
            serialNumber = sharedPreferences.getString("share_key", null);
            if (sharedPreferences.getBoolean("engineering_mode", false)) {
                final String share_test_key = sharedPreferences.getString("share_test_key", "").trim();
                if (share_test_key.length() > 4) serialNumber = share_test_key;
            }
            sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
            if ("".equals(sessionId)) // migrate previous empty sessionIds to null;
                sessionId = null;
        } catch (IllegalStateException e) {
            Log.wtf(TAG, "Illegal state exception: " + e);
        }
    }

    private synchronized OkHttpClient getOkHttpClient() {
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

            final OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.networkInterceptors().add(new Interceptor() {
                @Override
                public synchronized Response intercept(Chain chain) throws IOException {
                    try {
                        // Add user-agent and relevant headers.
                        Request original = chain.request();
                        Request copy = original.newBuilder().build();
                        Request modifiedRequest = original.newBuilder()
                                .header("User-Agent", "CGM-Store-1.2/22 CFNetwork/711.5.6 Darwin/14.0.0")
                                .header("Content-Type", "application/json")
                                .header("Accept", "application/json")
                                .build();
                        Log.d(TAG, "Sending request: " + modifiedRequest.toString());
                        Buffer buffer = new Buffer();
                        copy.body().writeTo(buffer);
                        Log.d(TAG, "Request body: " + buffer.readUtf8());

                        final Response response = chain.proceed(modifiedRequest);
                        Log.d(TAG, "Received response: " + response.toString());
                        if (response.body() != null) {
                            MediaType contentType = response.body().contentType();
                            String bodyString = response.body().string();
                            Log.d(TAG, "Response body: " + bodyString);
                            return response.newBuilder().body(ResponseBody.create(contentType, bodyString)).build();
                        } else
                            return response;

                    } catch (NullPointerException e) {
                        Log.e(TAG, "Got null pointer exception: " + e);
                        return null;
                    } catch (IllegalStateException e) {
                        UserError.Log.wtf(TAG,"Got illegal state exception: " + e);
                        return null;
                    }
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
            throw new RuntimeException("Error occurred initializing OkHttp: ", e);
        }
    }

    private String getSessionId() {
        AsyncTask<String, Void, String> task = new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... params) {
                try {
                    Boolean isActive = null;
                    if (params[0] != null)
                        isActive = dexcomShareApi.checkSessionActive(params[0]).execute().body();
                    if (isActive == null || !isActive) {
                        return updateAuthenticationParams();
                    } else
                        return params[0];
                } catch (IOException e) {
                    return null;
                } catch (RuntimeException e) {
                    UserError.Log.wtf(TAG, "Painful exception processing response in updateAuthenticationParams " + e);
                    return null;
                }
            }

            private String updateAuthenticationParams() throws IOException {
                sessionId = dexcomShareApi.getSessionId(new ShareAuthenticationBody(password, username).toMap()).execute().body();
                dexcomShareApi.authenticatePublisherAccount(sessionId, serialNumber, new ShareAuthenticationBody(password, username).toMap()).execute().body();
                dexcomShareApi.StartRemoteMonitoringSession(sessionId, serialNumber).execute();
                String assignment = dexcomShareApi.checkMonitorAssignment(sessionId, serialNumber).execute().body();
                if ((assignment != null) && (!assignment.equals("AssignedToYou"))) {
                    dexcomShareApi.updateMonitorAssignment(sessionId, serialNumber).execute();
                }
                return sessionId;
            }

        };

        if (sessionId == null || sessionId.equals(""))
            try {
                sessionId = task.executeOnExecutor(xdrip.executor,sessionId).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        return sessionId;
    }

    public void getContacts(Callback<List<ExistingFollower>> existingFollowerListener) {
        dexcomShareApi.getContacts(getSessionId()).enqueue(new AuthenticatingCallback<List<ExistingFollower>>(existingFollowerListener) {
            @Override
            public void onRetry() {
                dexcomShareApi.getContacts(getSessionId()).enqueue(this);
            }
        });
    }

    public void uploadBGRecords(final ShareUploadPayload bg, Callback<ResponseBody> callback) {
        dexcomShareApi.uploadBGRecords(getSessionId(), bg).enqueue(new AuthenticatingCallback<ResponseBody>(callback) {
            @Override
            public void onRetry() {
                dexcomShareApi.uploadBGRecords(getSessionId(), bg).enqueue(this);
            }
        });
    }

    public void createContact(final String followerName, final String followerEmail, Callback<String> callback) {
        dexcomShareApi.createContact(getSessionId(), followerName, followerEmail).enqueue(new AuthenticatingCallback<String>(callback) {
            @Override
            public void onRetry() {
                dexcomShareApi.createContact(getSessionId(), followerName, followerEmail).enqueue(this);
            }
        });
    }

    public void createInvitationForContact(final String contactId, final InvitationPayload invitationPayload, Callback<String> callback) {
        dexcomShareApi.createInvitationForContact(getSessionId(), contactId, invitationPayload).enqueue(new AuthenticatingCallback<String>(callback) {
            @Override
            public void onRetry() {
                dexcomShareApi.createInvitationForContact(getSessionId(), contactId, invitationPayload).enqueue(this);
            }
        });
    }

    public void deleteContact(final String contactId, Callback<ResponseBody> deleteFollowerListener) {
        dexcomShareApi.deleteContact(getSessionId(), contactId).enqueue(new AuthenticatingCallback<ResponseBody>(deleteFollowerListener) {
            @Override
            public void onRetry() {
                dexcomShareApi.deleteContact(getSessionId(), contactId).enqueue(this);
            }
        });
    }

    public abstract class AuthenticatingCallback<T> implements Callback<T> {

        private int attempts = 0;
        private Callback<T> delegate;
        public AuthenticatingCallback (Callback<T> callback) {
            this.delegate = callback;
        }

        public abstract void onRetry();

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
