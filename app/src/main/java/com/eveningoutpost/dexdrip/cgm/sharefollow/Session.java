package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import java.io.IOException;
import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import okhttp3.ResponseBody;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.SESSION_ID_VALIDITY_TIME;

/**
 * jamorham
 *
 * Manage session data for Share Follow
 *
 * Handle persistence of server session id and storage of asynchronous replies
 */

@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public class Session {

    private static final String SHARE_FOLLOW_SESSION_ID = "SHARE_FOLLOW_SESSION_ID";
    private static final String SHARE_FOLLOW_SESSION_ID_TIMESTAMP = "SHARE_FOLLOW_SESSION_ID_TIMESTAMP";

    private final String TAG;
    public List<ShareGlucoseRecord> results;
    public ShareErrorResponse lastError;
    public volatile String accountId;
    public volatile String sessionId;

    @Setter
    @Getter
    private int lastResponseCode = 0;
    private volatile long sessionId_timestamp = 0;

    {
        loadSessionId();
    }

    // populate session data from a response object which could be any supported type
    public void populate(final Object object) {
        lastError = null; // clear any error
        // is this a list entries?
        if (isListOfShareGlucoseRecords(object)) {
            try {
                results = (List<ShareGlucoseRecord>) object;

            } catch (ClassCastException e) {
                UserError.Log.e(TAG, "Couldn't parse results: " + e);
            }
            // is this a uuid string reply?
        } else if (object instanceof String) {
            sessionId = (String) object;
            if (!isIdValid(sessionId)) {
                UserError.Log.d(TAG, "Got invalid session id: " + sessionId);
                sessionId = "";
            } else {
                UserError.Log.v(TAG, "Got valid looking session id: " + sessionId);
                sessionId_timestamp = JoH.tsl();
            }
            saveSessionId();
        }
    }

    public void extractAccountId(final Object object) {
        if (object instanceof String) {
            accountId = (String) object;
            if (!isIdValid(accountId)) {
                UserError.Log.d(TAG, "Got invalid account ID: " + accountId);
                accountId = "";
            } else {
                UserError.Log.v(TAG, "Got valid looking account ID: " + accountId);
            }
        }
    }

    public void errorHandler(ResponseBody x) {
        try {
            lastError = ShareErrorResponse.fromJson(x.string());
        } catch (IOException e) {
            //
        }
    }

    public String getErrorString() {
        return lastError != null ? lastError.getNicerMessage() : "";
    }

    boolean isIdValid(final String id) {
        return !emptyString(id) && id.length() == 36 && !id.equals("00000000-0000-0000-0000-000000000000");
    }

    boolean sessionIdValid() {
        return isIdValid(sessionId) && JoH.msSince(sessionId_timestamp) < SESSION_ID_VALIDITY_TIME;
    }

    boolean accountIdValid() {
        return isIdValid(accountId);
    }

    void invalidateSessionId() {
        sessionId = "";
        sessionId_timestamp = 0;
        saveSessionId();
    }

    synchronized void saveSessionId() {
        PersistentStore.setLong(SHARE_FOLLOW_SESSION_ID_TIMESTAMP, sessionId_timestamp);
        PersistentStore.setString(SHARE_FOLLOW_SESSION_ID, sessionId);
    }

    void loadSessionId() {
        sessionId_timestamp = PersistentStore.getLong(SHARE_FOLLOW_SESSION_ID_TIMESTAMP);
        sessionId = PersistentStore.getString(SHARE_FOLLOW_SESSION_ID);
        if (emptyString(sessionId)) {
            sessionId = null;
        } else {
            UserError.Log.d(TAG, "Loaded session id: " + sessionId + " age: " + JoH.msSince(sessionId_timestamp) + " valid: " + sessionIdValid());
        }
    }

    // TODO is there some way to do this generically for any class?
    private static boolean isListOfShareGlucoseRecords(final Object object) {
        if (object instanceof List) {
            final List<Object> someList = (List<Object>) object;
            return !someList.isEmpty() && someList.get(0) instanceof ShareGlucoseRecord;
        }
        return false;
    }

}
