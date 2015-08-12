package com.eveningoutpost.dexdrip.ShareModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.ShareModels.Models.ShareAuthenticationBody;
import com.eveningoutpost.dexdrip.ShareModels.UserAgentInfo.UserAgent;
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

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.AndroidLog;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;
import retrofit.mime.TypedByteArray;
import rx.Observable;
import rx.functions.Action1;

/**
 * Created by stephenblack on 7/31/15.
 */
public class ShareAuthentication {
    private final static String TAG = ShareAuthentication.class.getSimpleName();
    private Context mContext;
    private String login;
    private String password;
    private String receiverSn;
    private String sessionId = null;
    private SharedPreferences prefs;
    private boolean retrying = false;
    private Action1<Boolean> authListener;
    private BgReading bg = null;
    OkClient client;

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    public ShareAuthentication(String login, String password, String receiverSerialNumber, Context context, final Action1<Boolean> authListener) {
        this.client = getOkClient();
        this.mContext = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.login = login;
        this.password = password;
        this.receiverSn = receiverSerialNumber.toUpperCase();
        this.authListener = authListener;
    }

    public static void invalidate(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putString("dexcom_share_session_id", "").apply();
    }

    public void authenticate() {
        if (login.length() == 0 || password.length() == 0 || receiverSn.length() == 0 || receiverSn.equals("SM00000000")) {
            authFailure();
        }

        sessionId = prefs.getString("dexcom_share_session_id", "");
        if(sessionId.length() != 0) {
            Log.d(TAG, "We already have a stored session ID");
            authSuccess();
        } else {
            Log.d(TAG, "There is no stored session id, we need to get a new one");
            getValidSessionId();
        }
    }

    private void authSuccess() {
        prefs.edit().putString("dexcom_share_session_id", sessionId).apply();
        Observable.just(true).subscribe(authListener);
    }

    private void authFailure() {
        prefs.edit().putString("dexcom_share_session_id", "").apply();
        Observable.just(false).subscribe(authListener);
    }

    public void getValidSessionId() {
        if (sessionId != null && !sessionId.equalsIgnoreCase("")) {
            Log.d(TAG, "Session ID not null, checking if active");
            emptyBodyInterface().checkSessionActive(querySessionMap(sessionId), new Callback<Response>() {
                @Override
                public void success(Response o, Response response) {
                    Log.d(TAG, "Success!! got a response checking if session is active");
                    if (response.getBody() != null) {
                        if(new String(((TypedByteArray) response.getBody()).getBytes()).toLowerCase().contains("true")) {
                            Log.d(TAG, "Session is active :-)");
                            authSuccess();
                        } else {
                            Log.w(TAG, "Session is apparently not active :-( " + new String(((TypedByteArray) response.getBody()).getBytes()));
                            StartRemoteMonitoringSession();
                        }
                    }
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    sessionId = null;
                    Log.e(TAG, "Failed to check if session is active", retrofitError);
                    getValidSessionId();
                }
            });
        } else {
            Log.d(TAG, "Session ID is null, Getting a new one");
            jsonBodyInterface().getSessionId(new ShareAuthenticationBody(password, login), new Callback<Response>() {
                @Override
                public void success(Response o, Response response) {
                    Log.d(TAG, "Success!! got a response on auth.");
                    sessionId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");
                    sendUserAgentData();
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e(TAG, "Unable to auth", retrofitError);
                    authFailure();
                }
            });
        }
    }

    public void sendUserAgentData() {
        jsonBodyInterface().updatePublisherAccountInfo(new UserAgent(sessionId), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d(TAG, "User Agent Data Updated!!");
                checkAndSetRecieverAssignment();
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Error updating user agent data", retrofitError);
                authFailure();
            }
        });
    }

    public void StartRemoteMonitoringSession() {
        if (sessionId != null && !sessionId.equalsIgnoreCase("")) {
            jsonBodyInterface().authenticatePublisherAccount(new ShareAuthenticationBody(password, login), queryActivateSessionMap(), new Callback<Response>() {
                @Override
                public void success(Response o, Response response) {
                    Log.d(TAG, "Success!! Authenticated Publisher account!!!");
                    emptyBodyInterface().StartRemoteMonitoringSession(queryActivateSessionMap(), new Callback<Response>() {
                        @Override
                        public void success(Response o, Response response) {
                            Log.d(TAG, "Success!! Our remote monitoring session is up!");
                            if (response.getBody() != null) {
                                authSuccess();
                            }
                        }

                        @Override
                        public void failure(RetrofitError retrofitError) {
                            Log.e(TAG, "Unable to start a remote monitoring session", retrofitError);
                            authFailure();
                        }
                    });
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e(TAG, "Unable to authenticate publisher account", retrofitError);
                    authFailure();
                }
            });
        }
    }

    public void checkAndSetRecieverAssignment() {
        emptyBodyInterface().checkMonitorAssignment(queryActivateSessionMap(), new Callback<Response>() {
            @Override
            public void success(Response o, Response response) {
                Log.d(TAG, "Success!! Our remote monitoring session is up!");
                if (response.getBody() != null) {
                    if (!(new String(((TypedByteArray) response.getBody()).getBytes()).contains("AssignedToYou"))) {

                        Log.e("Receiver trouble: ", "That receiver is not assigned to your account, trying to re-assign");
                        emptyBodyInterface().updateMonitorAssignment(queryActivateSessionMap(), new Callback<Response>() {
                            @Override
                            public void success(Response o, Response response) {
                                getValidSessionId();
                            }
                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.e(TAG, "Unable to set yourself as the publisher for that receiver", retrofitError);
                                authFailure();
                            }
                        });
                    } else {
                        getValidSessionId();
                    }
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                Log.e(TAG, "Unable to check receiver ownership", retrofitError);
                authFailure();
            }
        });
    }

    public DexcomShareInterface jsonBodyInterface() {
        RestAdapter adapter = authoirizeAdapterBuilder().build();
        DexcomShareInterface dexcomShareInterface =
                adapter.create(DexcomShareInterface.class);
        return dexcomShareInterface;
    }

    public DexcomShareInterface emptyBodyInterface() {
        RestAdapter adapter = getBgAdapterBuilder().build();
        DexcomShareInterface checkSessionActive =
                adapter.create(DexcomShareInterface.class);
        return checkSessionActive;
    }

    private RestAdapter.Builder authoirizeAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog(TAG))
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(authorizationRequestInterceptor)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    private RestAdapter.Builder getBgAdapterBuilder() {
        RestAdapter.Builder adapterBuilder = new RestAdapter.Builder();
        adapterBuilder
                .setClient(client)
                .setLogLevel(RestAdapter.LogLevel.FULL).setLog(new AndroidLog(TAG))
                .setEndpoint("https://share1.dexcom.com/ShareWebServices/Services")
                .setRequestInterceptor(getBgRequestInterceptor)
                .setConverter(new GsonConverter(new GsonBuilder()
                        .excludeFieldsWithoutExposeAnnotation()
                        .create()));
        return adapterBuilder;
    }

    static RequestInterceptor authorizationRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestInterceptor.RequestFacade request) {
            request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
        }
    };
    RequestInterceptor getBgRequestInterceptor = new RequestInterceptor() {
        @Override
        public void intercept(RequestInterceptor.RequestFacade request) {
            request.addHeader("User-Agent", "Dexcom Share/3.0.2.11 CFNetwork/711.2.23 Darwin/14.0.0");
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Content-Length", "0");
            request.addHeader("Accept", "application/json");
        }
    };

    public OkHttpClient getOkHttpClient() {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] chain,
                        String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted( java.security.cert.X509Certificate[] chain,
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

    public OkClient getOkClient (){
        OkHttpClient client1 = getOkHttpClient();
        OkClient _client = new OkClient(client1);
        return _client;
    }

    public Map<String, String> querySessionMap(String sessionId) {
        Map map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        return map;
    }

    public Map<String, String> queryActivateSessionMap() {
        Map map = new HashMap<String, String>();
        map.put("sessionID", sessionId);
        map.put("serialNumber", receiverSn);
        return map;
    }

    public static boolean shouldReAuth(Context context, RetrofitError retrofitError, boolean retried) {
        if (retrofitError.toString().toLowerCase().contains("session") && !retried) {
            ShareAuthentication.invalidate(context);
            return true;
        }
        return false;
    }
}
