package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SETTINGS_INADVISABLE_BASE_ID;

// Navid200

public class SettingsValidation {
    private static final String NOTIFY_MARKER = "-NOTIFY";
    private static final int RENOTIFY_TIME = 86400 * 30;
    public static void notifyAboutInadvisableSettings() {
        int id = SETTINGS_INADVISABLE_BASE_ID;
        String setting_name;

        setting_name = "engineering_mode";
        if (Pref.getBooleanDefaultFalse("engineering_mode")) {
            if (JoH.pratelimit(setting_name + NOTIFY_MARKER, RENOTIFY_TIME)) {
                id = notifyDis("Engineering Mode", setting_name, "" + xdrip.getAppContext().getString(R.string.eng_mode_is_on), id);
            }
        }

        // Todo Add the following items as well
        // A different method than notifyDis should be created (perhaps notifyEn) for settings that are disabled and are advised to be enabled e.g. OB1.

        // OB1 is disabled

        // Wake workaround is disabled

        // Bluetooth watchdog is disabled

        // Battery optimization prompt is enabled

        // Fallback is enabled

        // Allow initial bonding is disabled

        // Authenticate G5 b4 each read is disabled

        // Aggressive service restarts is disabled
    }

    private static int notifyDis(String short_name, String setting_string, String msg, int id) {

        JoH.showNotification("Inadvisable setting ", "Please disable " + short_name, null, id, false, false, null, null, ((msg.length() > 0) ? msg : ""));
        return id + 1;
    }
}