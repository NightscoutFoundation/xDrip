package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

public class WatchBroadcastEntry {
    public static final String PREF_ENABLED = "watch_broadcast_enabled";
    public static final String PREF_SEND_ALARMS = "watch_broadcast_send_alarms";
    public static final String PREF_SEND_ALARMS_OTHER = "watch_broadcast_send_alarms_other";
    public static final String PREF_UPDATE_BG = "watch_broadcast_update_bg";
    public static final String PREF_COLLECT_HEARTRATE = "watch_broadcast_collect_heartrate";
    public static final String PREF_COLLECT_STEPS = "watch_broadcast_collect_steps";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }

    public static boolean areAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS);
    }

    public static boolean areOtherAlertsEnabled() {
        return isEnabled() && Pref.getBooleanDefaultFalse(PREF_SEND_ALARMS_OTHER);
    }

    public static boolean isNeedToCollectHR() {
        return Pref.getBooleanDefaultFalse(PREF_COLLECT_HEARTRATE);
    }

    public static boolean isNeedToCollectSteps() {
        return Pref.getBooleanDefaultFalse(PREF_COLLECT_STEPS);
    }

    public static void showLatestBG() {
        if (isEnabled()) {
            JoH.startService(WatchBroadcastService.class, "function", "update_bg");
        }
    }
}
