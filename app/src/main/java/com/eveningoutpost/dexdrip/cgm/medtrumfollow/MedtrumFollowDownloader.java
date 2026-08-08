package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.os.Build;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.OkHttpWrapper;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.tidepool.InfoInterceptor;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

final class MedtrumFollowDownloader {

    private static final String TAG = "MedtrumFollowDL";
    private static final long MAX_BACKFILL = Constants.DAY_IN_MS;
    private static final String APP_VERSION = "1.2.70(112)";
    private static final String BUNDLE_ID = "com.medtrum.easyfollowforandroidmg";

    private final String username;
    private final String password;
    private final String patient;
    private final String baseUrl;
    private final MedtrumFollow service;
    private final AtomicBoolean inFlight = new AtomicBoolean(false);

    private volatile String cookie = "";
    @Getter private volatile String status = "";
    @Getter private volatile String selectedPatient = "";

    MedtrumFollowDownloader() {
        username = Pref.getString("medtrum_follow_user", "").trim();
        password = Pref.getString("medtrum_follow_password", "");
        patient = Pref.getString("medtrum_follow_patient", "").trim();
        baseUrl = serverUrl(Pref.getString("medtrum_follow_server", "eu"));
        service = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(OkHttpWrapper.getClient().newBuilder()
                        .addInterceptor(new InfoInterceptor(TAG))
                        .build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(MedtrumFollow.class);
    }

    static String serverUrl(final String topLevelDomain) {
        final String tld = "fr".equalsIgnoreCase(topLevelDomain) ? "fr" : "eu";
        return "https://easyview.medtrum." + tld + "/";
    }

    void download() {
        if (username.isEmpty() || password.isEmpty()) {
            setStatus(gs(R.string.please_enter_easyview_username_and_password));
            return;
        }
        if (!inFlight.compareAndSet(false, true)) {
            UserError.Log.d(TAG, gs(R.string.download_already_in_progress));
            return;
        }
        final BgReading last = BgReading.lastNoSenssor();
        final long notBefore = Math.max(last == null ? 0 : last.timestamp + 1, JoH.tsl() - MAX_BACKFILL);
        if (cookie.isEmpty()) {
            login(notBefore, false);
        } else {
            getMonitor(notBefore, false);
        }
    }

    void invalidateSession() {
        cookie = "";
    }

    private void login(final long notBefore, final boolean alreadyRetried) {
        setStatus(gs(R.string.logging_in_to_medtrum_easyview));
        service.login(deviceInfo(), "M", username, password, "xdrip", "google", "Follow",
                        "google", APP_VERSION, Build.MODEL, BUNDLE_ID)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(final Call<JsonObject> call, final Response<JsonObject> response) {
                        final String error = responseError(response);
                        if (error != null) {
                            finish(gs(R.string.easyview_login_failed) + error);
                            return;
                        }
                        cookie = extractCookie(response);
                        if (cookie.isEmpty()) {
                            finish(gs(R.string.easyview_login_did_not_return_a_session_cookie));
                            return;
                        }
                        getMonitor(notBefore, alreadyRetried);
                    }

                    @Override
                    public void onFailure(final Call<JsonObject> call, final Throwable throwable) {
                        finish(gs(R.string.easyview_login_connection_error) + throwable.getMessage());
                    }
                });
    }

    private void getMonitor(final long notBefore, final boolean alreadyRetried) {
        setStatus("Downloading glucose from EasyView");
        service.getMonitor(deviceInfo(), cookie).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(final Call<JsonObject> call, final Response<JsonObject> response) {
                final String error = responseError(response);
                if (error != null) {
                    retryLoginOrFinish(notBefore, alreadyRetried, error);
                    return;
                }
                final MedtrumFollowData.Result current = MedtrumFollowData.parseMonitor(response.body(), patient);
                if (!current.isSuccessful()) {
                    if (MedtrumFollowData.responseError(response.body()) != null) {
                        retryLoginOrFinish(notBefore, alreadyRetried, current.error);
                    } else {
                        finish(current.error);
                    }
                    return;
                }
                selectedPatient = current.patient;
                if (!selectedPatient.isEmpty() && notBefore < JoH.tsl() - (3 * Constants.MINUTE_IN_MS)) {
                    getHistory(notBefore, current.entries);
                } else {
                    complete(current.entries);
                }
            }

            @Override
            public void onFailure(final Call<JsonObject> call, final Throwable throwable) {
                finish(gs(R.string.easyview_download_connection_error) + throwable.getMessage());
            }
        });
    }

    private void getHistory(final long notBefore, final List<MedtrumFollowData.Entry> current) {
        final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        service.getGraph(deviceInfo(), cookie, "sg", format.format(new Date(notBefore)),
                        format.format(new Date()), selectedPatient)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(final Call<JsonObject> call, final Response<JsonObject> response) {
                        final List<MedtrumFollowData.Entry> combined = new ArrayList<>();
                        if (response.isSuccessful()) {
                            final MedtrumFollowData.Result history = MedtrumFollowData.parseGraph(response.body(), notBefore);
                            if (history.isSuccessful()) combined.addAll(history.entries);
                        } else {
                            UserError.Log.w(TAG, "History download failed with HTTP " + response.code());
                        }
                        combined.addAll(current);
                        complete(combined);
                    }

                    @Override
                    public void onFailure(final Call<JsonObject> call, final Throwable throwable) {
                        UserError.Log.w(TAG, "History download failed: " + throwable.getMessage());
                        complete(current);
                    }
                });
    }

    private void retryLoginOrFinish(final long notBefore, final boolean alreadyRetried, final String error) {
        cookie = "";
        if (!alreadyRetried) {
            login(notBefore, true);
        } else {
            finish(gs(R.string.easyview_download_failed) + error);
        }
    }

    private void complete(final List<MedtrumFollowData.Entry> entries) {
        EntryProcessor.processEntries(entries);
        MedtrumFollowService.updateBgReceiveDelay();
        MedtrumFollowService.scheduleWakeUp();
        finish("");
    }

    private void finish(final String message) {
        setStatus(message);
        inFlight.set(false);
    }

    private void setStatus(final String message) {
        status = message == null || message.isEmpty() ? "" : JoH.hourMinuteString() + ": " + message;
        if (!status.isEmpty()) UserError.Log.d(TAG, status);
    }

    private static String responseError(final Response<JsonObject> response) {
        if (!response.isSuccessful()) return "HTTP " + response.code();
        return MedtrumFollowData.responseError(response.body());
    }

    private static String extractCookie(final Response<?> response) {
        final List<String> values = response.headers().values("Set-Cookie");
        if (values.isEmpty()) return "";
        final String value = values.get(0);
        final int separator = value.indexOf(';');
        return (separator >= 0 ? value.substring(0, separator) : value).trim();
    }

    private static String deviceInfo() {
        return "Android " + Build.VERSION.RELEASE + ";" + Build.MANUFACTURER + " "
                + Build.DEVICE + ";Android " + Build.VERSION.RELEASE;
    }
}


