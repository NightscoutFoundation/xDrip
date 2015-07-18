package com.eveningoutpost.dexdrip.ShareModels;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
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

/**
 * Created by stephenblack on 12/26/14.
 */
public class ShareRest extends Service {
    private final static String TAG = ShareRest.class.getSimpleName();
    private Context mContext;
    private String login;
    private String password;
    private String receiverSn;
    private String sessionId = null;
    private SharedPreferences prefs;
    private boolean retrying = false;
    private BgReading bg = null;
    OkClient client;

    public static Gson gson = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");
        client = getOkClient();
        mContext = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        login = prefs.getString("dexcom_account_name", "");
        password = prefs.getString("dexcom_account_password", "");
        receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
     }

    @Override
    public int onStartCommand (Intent intent,int flags, int startId) {
        retrying = false;
        bg = null;
        login = prefs.getString("dexcom_account_name", "");
        password = prefs.getString("dexcom_account_password", "");
        receiverSn = prefs.getString("share_key", "SM00000000").toUpperCase();
        Log.d(TAG, "Starting service");
        if (prefs.getBoolean("share_upload", false) && login.compareTo("") != 0 && password.compareTo("") != 0 && receiverSn.compareTo("SM00000000") != 0) {
            if(intent != null ) {
                String uuid = intent.getStringExtra("BgUuid");

                Log.d(TAG, "UUID: " + uuid);
                bg = BgReading.findByUuid(uuid);
                if(uuid != null && !uuid.contentEquals("")) {
                    if(sessionId != null && !sessionId.contentEquals("")) {
                        Log.d(TAG, "New BG reading found and session exists");
                        continueUpload();
                    } else {
                        Log.d(TAG, "New BG reading found but session does not exist");
                        getValidSessionId();
                    }
                }
            }
        } else {
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    public void getValidSessionId() {
        if (sessionId != null && !sessionId.equalsIgnoreCase("")) {
            try {
                Log.d(TAG, "Session ID not null, checking if active");
                emptyBodyInterface().checkSessionActive(querySessionMap(sessionId), new Callback() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.d(TAG, "Success!! got a response checking if session is active");
                        if (response.getBody() != null) {
                            if(new String(((TypedByteArray) response.getBody()).getBytes()).toLowerCase().contains("true")) {
                                Log.d(TAG, "Session is active :-)");
                                continueUpload();
                            } else {
                                Log.d(TAG, "Session is apparently not active :-(");
                                Log.d(TAG, new String(((TypedByteArray) response.getBody()).getBytes()));
                                StartRemoteMonitoringSession();
                            }
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        sessionId = null;
                        Log.e("RETROFIT ERROR: ", "" + retrofitError.toString());
                        getValidSessionId();
                    }
                });
            } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "BOOOO");
            }
        } else {
            try {
                Log.d(TAG, "Session ID is null, Getting a new one");
                jsonBodyInterface().getSessionId(new ShareAuthenticationBody(password, login), new Callback() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.d(TAG, "Success!! got a response on auth.");
                        Log.e("RETROFIT ERROR: ", "Auth succesfull");
                        sessionId = new String(((TypedByteArray) response.getBody()).getBytes()).replace("\"", "");
                        sendUserAgentData();
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        sessionId = null;
                        Log.e("RETROFIT ERROR: ", "" + retrofitError.toString());
                        Log.e("RETROFIT ERROR: ", "Unable to auth");
                    }
                });
            } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "BOOOO");
            }
        }
    }

    public void sendUserAgentData() {
        try {
            jsonBodyInterface().updatePublisherAccountInfo(new UserAgent(sessionId), new Callback() {
                @Override
                public void success(Object o, Response response) {
                    Log.d(TAG, "User Agent Data Updated!!");
                    checkAndSetRecieverAssignment();
                }

                @Override
                public void failure(RetrofitError retrofitError) {
                    Log.e("RETROFIT ERROR: ", ""+retrofitError.toString());
                    Log.e("RETROFIT ERROR: ", "Error updating user agent data");

                }
            });
        }
        catch (RetrofitError e) { Log.d("Retrofit Error: ", "BOOOO"); }
        catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
    }

    public void StartRemoteMonitoringSession() {
        if (sessionId != null && !sessionId.equalsIgnoreCase("")) {
            try {
                jsonBodyInterface().authenticatePublisherAccount(new ShareAuthenticationBody(password, login), queryActivateSessionMap(), new Callback() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.d(TAG, "Success!! Authenticated Publisher account!!!");

                        try {
                            emptyBodyInterface().StartRemoteMonitoringSession(queryActivateSessionMap(), new Callback() {
                                @Override
                                public void success(Object o, Response response) {
                                    Log.d(TAG, "Success!! Our remote monitoring session is up!");
                                    if (response.getBody() != null) {
                                        continueUpload();
                                    }
                                }

                                @Override
                                public void failure(RetrofitError retrofitError) {
                                    sessionId = null;
                                    Log.e("RETROFIT ERROR: ", "Unable to start a remote monitoring session");
                                }
                            });
                        } catch (Exception e) {
                            Log.e("REST CALL ERROR: ", "BOOOO");
                        }
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        sessionId = null;
                        Log.e("RETROFIT ERROR: ", "Unable to authenticate publisher account");
                    }
                });
            } catch (Exception e) {
                Log.e("REST CALL ERROR: ", "BOOOO");
            }


        }
    }

    public void checkAndSetRecieverAssignment() {
        emptyBodyInterface().checkMonitorAssignment(queryActivateSessionMap(), new Callback() {
            @Override
            public void success(Object o, Response response) {
                Log.d(TAG, "Success!! Our remote monitoring session is up!");
                if (response.getBody() != null) {
                    if (!(new String(((TypedByteArray) response.getBody()).getBytes()).contains("AssignedToYou"))) {

                        Log.e("Receiver trouble: ", "That receiver is not assigned to your account, trying to re-assign");
                        emptyBodyInterface().updateMonitorAssignment(queryActivateSessionMap(), new Callback() {
                            @Override
                            public void success(Object o, Response response) {
                                getValidSessionId();

                            }
                            @Override
                            public void failure(RetrofitError retrofitError) {
                                Log.e("RETROFIT ERROR: ", "Unable to set yourself as the publisher for that receiver");
                            }
                        });
                    } else {
                        getValidSessionId();

                    }
                }
            }

            @Override
            public void failure(RetrofitError retrofitError) {
                sessionId = null;
                Log.e("RETROFIT ERROR: ", "Unable to check receiver ownership");
            }
        });
    }

    public void continueUpload() {
        if(bg != null) {
            sendBgData(sessionId, bg);
        } else {
            Log.d(TAG, "No BG, cannot continue");
        }
    }

    private void sendBgData(String sessionId, BgReading bg) {
        DataSender dataSender = new DataSender(mContext, sessionId, bg);
        dataSender.execute((Void) null);
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

    public OkClient getOkClient (){
        OkHttpClient client1 = getOkHttpClient();
        OkClient _client = new OkClient(client1);
        return _client;
    }

    public class DataSender extends AsyncTask<Void, Void, Boolean> {
        Context mContext;
        String mSessionId;
        BgReading mBg;
        DataSender(Context context, String sessionId, BgReading bg) {
            mContext = context;
            mSessionId = sessionId;
            mBg = bg;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                String receiverSn = preferences.getString("share_key", "SM00000000").toUpperCase();
                jsonBodyInterface().uploadBGRecords(querySessionMap(mSessionId), new ShareUploadPayload(receiverSn, mBg), new Callback() {
                    @Override
                    public void success(Object o, Response response) {
                        Log.d(TAG, "Success!! Uploaded!!");
                        bg = null;
                    }

                    @Override
                    public void failure(RetrofitError retrofitError) {
                        Log.e("RETROFIT ERROR: ", ""+retrofitError.toString());
                        if((retrofitError.toString().contains("EvgPost is only allowed when monitoring session is active") && retrying == false) ||
                                (retrofitError.toString().contains("SessionNotValid")  && retrying == false)) {
                            sessionId = null;
                            retrying = true;
                            getValidSessionId();
                        } else {
                            bg = null;
                        }
                    }
                });
            }
            catch (RetrofitError e) { Log.d("Retrofit Error: ", "BOOOO"); }
            catch (Exception ex) { Log.d("Unrecognized Error: ", "BOOOO"); }
            return false;
        }
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
}
