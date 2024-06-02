package com.eveningoutpost.dexdrip.watch.thinjam;

import android.content.SharedPreferences;
import android.os.Build;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import lombok.val;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_CANCEL;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_HIGH_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_LOW_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_OTHER_ALERT;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXT_MESSAGE;

// jamorham

public class BlueJayEntry {

// very lightweight entry point class to avoid loader overhead when not in use

    // preference key groups to observe
    private static final String[] HUNT_PREFERENCES = {"bluejay", "units", "dex_txid"};

    private static final String PREF_ENABLED = "bluejay_enabled";
    private static final String PREF_REMOTE_ENABLED = "bluejay_send_to_another_xdrip";
    private static final String PREF_PHONE_COLLECTOR_ENABLED = "bluejay_run_phone_collector";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }

    public static boolean isRemoteEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_REMOTE_ENABLED);
    }

    public static boolean isPhoneCollectorDisabled() {
        return isEnabled() && BlueJay.isCollector() && !Pref.getBoolean(PREF_PHONE_COLLECTOR_ENABLED, true);
    }

    public static void setEnabled() {
        Pref.setBoolean(PREF_ENABLED, true);
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("bluejay_send_alarms");
    }

    public static boolean areCallAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse("bluejay_option_call_notifications");
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("bj-full-initial-start", 500, new Runnable() {
                @Override
                public void run() {
                    startWithRefresh();
                }
            });
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            for (val hunt : HUNT_PREFERENCES)
                if (key.startsWith(hunt)) {
                    android.util.Log.d("BlueJayPref", "Hit on Preference key: " + key);
                    if (key.equals(PREF_PHONE_COLLECTOR_ENABLED)) {
                        CollectionServiceStarter.restartCollectionServiceBackground();
                    }
                    startWithRefresh();
                    break;
                }
        }
    };

    public static void startWithRefreshIfEnabled() {
        if (isEnabled()) {
            startWithRefresh();
        }
    }

    public static void sendAlertIfEnabled(final String msg) {
        if (BlueJayEntry.areAlertsEnabled()) {
            final String alertType;
            if (msg.startsWith("High Alert")) {
                alertType = THINJAM_NOTIFY_TYPE_HIGH_ALERT;
            } else if (msg.startsWith("Low Alert")) {
                alertType = THINJAM_NOTIFY_TYPE_LOW_ALERT;
            } else {
                alertType = THINJAM_NOTIFY_TYPE_OTHER_ALERT;
            }
            Inevitable.task("send-bj-alert" + msg, 2000, () -> BlueJayEntry.sendNotifyIfEnabled(alertType, "A"));
        }
    }

    public static void sendNotifyIfEnabled(final String msg) {
        sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_TEXT_MESSAGE, msg);
    }

    public static void sendNotifyIfEnabled(final String message_type, final String msg) {
        if (isEnabled()) {
            final String fmsg = msg.replaceAll("^-", "").trim(); // TODO move
            if (!JoH.emptyString(msg)) {
                Inevitable.task("bluejay-send-notify-external" + message_type, 200, () -> JoH.startService(BlueJayService.class, "function", "message", "message", fmsg, "message_type", message_type));
            }
        }
    }

    public static void cancelNotifyIfEnabled() {
        if (isEnabled()) {
            if (BlueJayEntry.areAlertsEnabled() || BlueJayEntry.areCallAlertsEnabled()) {
                BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_CANCEL, "C");
            }
        }
    }

    public static void sendPngIfEnabled(final byte[] bytes, final String parameters, final String type) {
        if (isEnabled()) {
            Inevitable.task("bluejay-send-png-external", 200, () -> JoH.startService(BlueJayService.class, bytes, "function", "png", "params", parameters, "type", type));
        }
    }

    public static boolean isNative() {
        return Build.MODEL.startsWith("BlueJay U");
    }

    static void startWithRefresh() {
        Inevitable.task("bluejay-preference-changed", 1000, () -> JoH.startService(BlueJayService.class, "function", "refresh"));
    }

}



