package com.eveningoutpost.dexdrip.cgm.sharefollow;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.evaluators.MissedReadingsEstimator;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.MAX_RECORDS_TO_ASK_FOR;

/**
 * jamorham
 *
 * Handle connection and downloading of data for Share Follow
 */

public class ShareFollowDownload extends RetrofitBase {

    private static final String TAG = "ShareFollowDL";
    private static final boolean D = false;

    private static PowerManager.WakeLock wl;

    private String login;
    private String password;
    private String serverUrl;
    @Getter
    private String status;
    private boolean loginDataLooksOkay;


    private int requestCount;
    private long loginBlockedTill = 0;
    private long loginBackoff = Constants.MINUTE_IN_MS;

    private DexcomShareInterface service;

    private Session session = new Session(TAG);

    ShareFollowDownload(final String server, final String login, final String password) {
        this.login = login;
        this.password = password;
        this.serverUrl = server;
        loginDataLooksOkay = !emptyString(server) && !emptyString(login) && !emptyString(password);
    }

    public boolean doEverything(final int record_request_size) {
        if (D) UserError.Log.e(TAG, "doEverything called");
        if (loginDataLooksOkay) {
            requestCount = Math.min(record_request_size, MAX_RECORDS_TO_ASK_FOR);
            return loginAndGetData();
        } else {
            final String invalid = xdrip.gs(R.string.share_login_data_isnt_valid);
            msg(invalid);
            UserError.Log.e(TAG, invalid);
            return false;
        }
    }

    public void invalidateSession() {
        session.invalidateSessionId();
    }

    // Login to service and proceed to get data if successful
    private boolean loginAndGetData() {
        if (!session.sessionIdValid()) {
            if (JoH.tsl() > loginBlockedTill) {
                extendWakeLock(30000);
                getService().getSessionId(new ShareAuthenticationBody(password, login))
                        .enqueue(new ShareFollowCallback<String>("Login", session, this::getData)
                                .setOnFailure(this::handleLoginFailure));
            } else {
                UserError.Log.e(TAG, "Not trying to login due to backoff timer for login failures until: " + JoH.dateTimeText(loginBlockedTill));
            }
        } else {
            UserError.Log.d(TAG, "Session id appears valid so going direct to get data");
            getData();
        }
        return true;
    }

    private void handleLoginFailure() {
        UserError.Log.d(TAG, "Login failure: " + session.getErrorString() + " code: " + session.getLastResponseCode());
        if (session.getLastResponseCode() == 0) {
            msg(xdrip.gs(R.string.connectivity_problem_reaching_share_servers));
        } else {
            msg("Share login error: " + session.getErrorString() + " code: " + session.getLastResponseCode());
            loginBackoff += Constants.MINUTE_IN_MS;
            loginBlockedTill = JoH.tsl() + loginBackoff;
        }
        releaseWakeLock();
    }

    // Get data from service
    private boolean getData() {
        loginBackoff = 0; // reset backoff timer due to login success
        try {
            if (session.sessionId != null) {
                extendWakeLock(30000);
                getService().getGlucoseRecords(getDataQueryParameters(session.sessionId))
                        .enqueue(new ShareFollowCallback<List<ShareGlucoseRecord>>("Get Share Data", session,
                                this::backgroundProcessGlucoseResults).setOnFailure(this::handleGetDataFailure));
            } else {
                UserError.Log.d(TAG, "Cannot get data as sessionID is null");
                return false;
            }
            return true;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception in getData() " + e);
            releaseWakeLock();
            return false;
        }
    }

    private void handleGetDataFailure() {
        UserError.Log.d(TAG, "Last response code: " + session.getLastResponseCode());
        if (session.getLastResponseCode() == 0) {
            msg(xdrip.gs(R.string.connectivity_problem_reaching_share_servers));
        } else {
            session.invalidateSessionId(); // could be due to invalid session handle so reset that
            msg("Share get data error: " + session.getErrorString() + " code: " + session.getLastResponseCode());
        }
        releaseWakeLock();
    }

    private void backgroundProcessGlucoseResults() {
        Inevitable.task("proc-share-follow", 100, this::processGlucoseResults);
        releaseWakeLock(); // handover to inevitable
    }

    // don't call this directly unless you are also handling the wakelock release
    private void processGlucoseResults() {
        if (session.results != null) {
            UserError.Log.d(TAG, "Success get data");
            EntryProcessor.processEntries(session.results, true);
            ShareFollowService.updateBgReceiveDelay();
            session.results = null;
            msg(null); // clear any error msg
        } else {
            UserError.Log.d(TAG, "Nothing to process");
        }
    }

    private DexcomShareInterface getService() {
        if (service == null) {
            try {
                service = getRetrofitInstance(TAG, serverUrl, false).create(DexcomShareInterface.class);
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Null pointer trying to getService()");
            }
        }
        return service;
    }

    private Map<String, String> getDataQueryParameters(String sessionId) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("sessionID", sessionId);
        map.put("minutes", String.valueOf(minutesToAskFor()));
        map.put("maxCount", String.valueOf(requestCount));
        return map;
    }

    private int minutesToAskFor() {
        final int count = MissedReadingsEstimator.estimate() + 1;
        return Math.min(count * 5, 1440); // 5= dexcom period minutes // day max minutes
    }

    private void msg(final String msg) {
        status = msg != null ? JoH.hourMinuteString() + ": " + msg : null;
        if (msg != null) UserError.Log.d(TAG, "Setting message: " + status);
    }

    private static synchronized void extendWakeLock(final long ms) {
        if (wl == null) {
            if (D) UserError.Log.d(TAG,"Creating wakelock");
            wl = JoH.getWakeLock("SHFollow-download", (int) ms);
        } else {
            JoH.releaseWakeLock(wl); // lets not get too messy
            wl.acquire(ms);
            if (D) UserError.Log.d(TAG,"Extending wakelock");
        }
    }

    protected static synchronized void releaseWakeLock() {
        if (D) UserError.Log.d(TAG, "Releasing wakelock");
        JoH.releaseWakeLock(wl);
    }


}
