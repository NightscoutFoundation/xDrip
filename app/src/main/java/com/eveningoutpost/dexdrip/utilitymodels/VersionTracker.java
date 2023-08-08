package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;

/**
 * jamorham
 *
 * Track and log version changes
 *
 */

public class VersionTracker {

    private static final String TAG = VersionTracker.class.getSimpleName();
    private static final String STORE_PHONE_VERSION_SHORT = "PHONE_VERSION_SHORT";
    private static final String STORE_PHONE_VERSION_LONG = "PHONE_VERSION_LONG";

    private static final String LONG_TRACKED = "" + BuildConfig.buildTimestamp;
    private static final String SHORT_TRACKED = BuildConfig.VERSION_NAME;

    private static boolean checked = false;

    // uses the same storage names on wear as on phone

    public static void updateDevice() {
        try {
            if (!checked) {
                checked = true;

                final String oldShort = PersistentStore.getString(STORE_PHONE_VERSION_SHORT);
                if (!checkChanges(oldShort, SHORT_TRACKED, false)) {
                    final String oldLong = PersistentStore.getString(STORE_PHONE_VERSION_LONG);
                    checkChanges(oldLong, LONG_TRACKED, true);
                }
            }
        } catch (Exception e) {
            // never crash
        }
    }

    private static boolean checkChanges(String old, String nu, boolean numeric) {
        if (!old.equals(nu)) {
            savePhoneVersions();
            if (numeric) {
                try {
                    UserError.Log.ueh(TAG, xdrip.getAppContext().getString(R.string.xdrip_software_changed_format, dateTimeText(Long.parseLong(old)), dateTimeText(Long.parseLong(nu))));
                } catch (Exception e) {
                    UserError.Log.ueh(TAG, xdrip.getAppContext().getString(R.string.xdrip_software_changed_format, old, nu + " (parse failure)"));
                }
            } else {
                UserError.Log.ueh(TAG, xdrip.getAppContext().getString(R.string.xdrip_software_changed_format, old, nu));
            }
            return true;
        }
        return false;
    }

    private static void savePhoneVersions() {
        PersistentStore.setString(STORE_PHONE_VERSION_SHORT, SHORT_TRACKED);
        PersistentStore.setString(STORE_PHONE_VERSION_LONG, LONG_TRACKED);
    }

}
