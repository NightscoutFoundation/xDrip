package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.SETTINGS_INADVISABLE_BASE_ID;

// navid200

public class SettingsValidation {
    private static final String NOTIFY_MARKER = "-NOTIFY";
    private static final int RENOTIFY_TIME = 86400 * 30;
    public static void notifyAboutInadvisableSettings() {
        int id = SETTINGS_INADVISABLE_BASE_ID;
        String setting_name;

        setting_name = "engineering_mode";
        if (Pref.getBooleanDefaultFalse("engineering_mode")) {
            if (JoH.pratelimit(setting_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notify("Engineering Mode", setting_name, "" + xdrip.getAppContext().getString(R.string.eng_mode_is_on), id);
            }
        }

        // Todo Add the following items as well

        // OB1 is disabled

        // Samsung workaround is disabled

        // Bluetooth watchdog is disabled

        // Battery optimization prompt is enabled

        // Fallback is enabled

        // Allow initial bonding is disabled

        // Authenticate G5 b4 each read is disabled

        // Aggressive service restarts is disabled
    }

    private static int notify(String short_name, String setting_string, String msg, int id) {

        JoH.showNotification("Inadvisable settings - " + short_name, "Please enable or disable " + setting_string, null, id, true, true, null, null, ((msg.length() > 0) ? msg : ""));
        return id + 1;
    }
}
