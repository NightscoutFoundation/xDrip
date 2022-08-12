package com.eveningoutpost.dexdrip.wearintegration;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.eveningoutpost.dexdrip.Models.APStatus;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.NewDataObserver;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.val;

/**
 * Created by adrian on 14/02/16.
 */
public class ExternalStatusService extends IntentService {
    //constants
    static final String EXTERNAL_STATUS_STORE = "external-status-store";
    static final String EXTERNAL_STATUS_STORE_TIME = "external-status-store-time";
    private static final String EXTRA_STATUSLINE = "com.eveningoutpost.dexdrip.Extras.Statusline";
    public static final String ACTION_NEW_EXTERNAL_STATUSLINE = "com.eveningoutpost.dexdrip.ExternalStatusline";
    //public static final String RECEIVER_PERMISSION = "com.eveningoutpost.dexdrip.permissions.RECEIVE_EXTERNAL_STATUSLINE";
    private static final int MAX_LEN = 40;
    private final static String TAG = ExternalStatusService.class.getSimpleName();

    public ExternalStatusService() {
        super("ExternalStatusService");
        setIntentRedelivery(true);
    }

    private static boolean isCurrent(long timestamp) {
        return JoH.msSince(timestamp) < Constants.HOUR_IN_MS * 5;
    }

    private static boolean isCurrent() {
        return isCurrent(getLastStatusLineTime());
    }

    @NonNull
    public static String getLastStatusLine() {
        if (isCurrent()) {
            return PersistentStore.getString(EXTERNAL_STATUS_STORE);
        } else {
            return ""; // ignore if more than 8 hours old
        }
    }

    public static long getLastStatusLineTime() {
        return PersistentStore.getLong(EXTERNAL_STATUS_STORE_TIME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (intent == null)
            return;

        try {
            final String action = intent.getAction();
            if (action == null) return;

            if (ACTION_NEW_EXTERNAL_STATUSLINE.equals(action)) {
                final String statusLine = intent.getStringExtra(EXTRA_STATUSLINE);
                update(JoH.tsl(), statusLine, true);
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }


    public static void update(long timestamp, String statusline, boolean receivedLocally) {
        if (statusline != null) {

            if (statusline.length() > MAX_LEN) {
                statusline = statusline.substring(0, MAX_LEN);
            }

            // store the data
            if (isCurrent(timestamp)) {
                PersistentStore.setString(EXTERNAL_STATUS_STORE, statusline);
                PersistentStore.setLong(EXTERNAL_STATUS_STORE_TIME, timestamp);
            }

            if (statusline.length() > 0) {
                final Double absolute = getAbsoluteBRDouble();
                if (absolute != null) {
                    APStatus.createEfficientRecord(timestamp, absolute);
                } else {
                    final Integer percent = getTBRInt();
                    if (percent != null) {
                        APStatus.createEfficientRecord(timestamp, percent);
                    } else {
                        UserError.Log.wtf(TAG, "Could not parse TBR from: " + statusline);
                    }
                }
            }

            // notify observers
            NewDataObserver.newExternalStatus(receivedLocally);

        }
    }

    // adapted from PR submission by JoernL
    public static String getIOB() {
        final String statusLine = getLastStatusLine();
        if (statusLine.length() == 0) return "";
        final String check = statusLine.replaceAll("[^%]", "");
        if (check.length() > 0) {
            UserError.Log.v(TAG, statusLine);
            return statusLine.substring((statusLine.lastIndexOf('%') + 2), (statusLine.lastIndexOf('%') + 6));
        } else if (check.length() == 0) {
            UserError.Log.v(TAG, statusLine);
            return statusLine.substring(0, 4);
        } else return "???";
    }

    // extract a TBR percentage from a status line string.
    public static String getTBR(final String statusLine) {
        if (JoH.emptyString(statusLine)) return "";
        val pattern = Pattern.compile(".*([^0-9]|^)([0-9]+%)", Pattern.DOTALL); // match last of any number followed by %
        val matcher = pattern.matcher(statusLine);
        val matches = matcher.find();       // was at least one found?

        if (matches) {
            return matcher.group(matcher.groupCount());    // return the last one
        } else {
            return "100%";      // if no value in status line return 100%
        }
    }

    public static String getAbsoluteBR(final String statusLine) {
        if (JoH.emptyString(statusLine)) return "";
        val pattern = Pattern.compile(".*(^|[^0-9.,])([0-9.,]+U/h)", Pattern.DOTALL); // match last of any number followed by units per hour
        val matcher = pattern.matcher(statusLine);
        val matches = matcher.find();       // was at least one found?

        if (matches) {
            return matcher.group(matcher.groupCount());    // return the last one
        } else {
            return null;      // if no value in status line return null
        }

    }


    // I don't have test data for what this matched exactly but it doesn't work with newer strings
    // so hopefully the replacement function works as this one was also intended.
    public static String getTBRold(final String statusLine) {
        if (statusLine.length() == 0) return "";
        final String check = statusLine.replaceAll("[^%]", "");
        if (check.length() > 0) {
            int index1 = 0, index2 = 4;
            UserError.Log.v(TAG, statusLine);
            if (statusLine.lastIndexOf('%') == 3) index2 = 4;
            else if (statusLine.lastIndexOf('%') == 2) index2 = 3;
            else if (statusLine.lastIndexOf('%') == 1) index2 = 2;
            return statusLine.substring(index1, index2);
        } else if (check.length() == 0)
            return "100%";
        else return "???";
    }

    // adapted from PR submission by JoernL
    public static String getTBR() {
        final String statusLine = getLastStatusLine();
        return getTBR(statusLine);
    }

    public static String getAbsoluteBR() {
        final String statusLine = getLastStatusLine();
        return getAbsoluteBR(statusLine);
    }

    public static Integer getTBRInt() {
        try {
            return Integer.parseInt(getTBR().replace("%", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double getAbsoluteBRDouble() {
        try {
            return JoH.tolerantParseDouble(getAbsoluteBR().replace("U/h", ""));
        } catch (NullPointerException | NumberFormatException e) {
            return null;
        }
    }

}