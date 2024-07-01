package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.APStatus;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.DesertSync;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Libre2RawValue;
import com.eveningoutpost.dexdrip.models.Libre2Sensor;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreData;
import com.eveningoutpost.dexdrip.models.PenData;
import com.eveningoutpost.dexdrip.models.Prediction;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.SnoozeActivity;

import java.util.ArrayList;
import java.util.Iterator;

import lombok.val;

/**
 * Created by Emma Black on 4/15/15.
 */
public class IdempotentMigrations {
    private final Context mContext;
    private final SharedPreferences prefs;

    private final static String TAG = IdempotentMigrations.class.getSimpleName();

    public IdempotentMigrations(Context context) {
        this.mContext = context;
        this.prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    }

    public void performAll() {
        migrateBGAlerts();
        migrateToNewStyleRestUris();
        BgReading.updateDB();
        LibreBlock.updateDB();
        LibreData.updateDB();
        APStatus.updateDB();
        Prediction.updateDB();
        DesertSync.updateDB();
        PenData.updateDB();
        Libre2RawValue.updateDB();
        Libre2Sensor.updateDB();
//        BgReadingArchive.updateDB();
        AlertType.fixUpTable();
        UserNotification.updateDB();
        JoH.clearCache();
        legacySettingsFix();
        IncompatibleApps.notifyAboutIncompatibleApps();
        CompatibleApps.notifyAboutCompatibleApps();
        legacySettingsMoveLanguageFromNoToNb();

    }

    private void migrateBGAlerts() {
        // Migrate away from old style notifications to Tzachis new Alert system
       // AlertType.CreateStaticAlerts(); // jamorham weird problem auto-calibrations
        if(prefs.getBoolean("bg_notifications", true)){
            double highMark = Double.parseDouble(prefs.getString("highValue", "170"))+54; // make default alert not too fatiguing
            double lowMark = Double.parseDouble(prefs.getString("lowValue", "70"));

            boolean doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

            if(!doMgdl) {
                highMark = highMark * Constants.MMOLL_TO_MGDL;
                lowMark = lowMark * Constants.MMOLL_TO_MGDL;
            }
            boolean bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", true);
            String bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");

            int bg_high_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(true))));
            int bg_low_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(false))));


            AlertType.add_alert(null, mContext.getString(R.string.high_alert), true, highMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, true, bg_high_snooze, true, true);
            AlertType.add_alert(null, mContext.getString(R.string.low_alert), false, lowMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, true, bg_low_snooze, true, true);
            prefs.edit().putBoolean("bg_notifications", false).apply();
        }
    }

    private void migrateToNewStyleRestUris() {

        String baseURLSettings = prefs.getString("cloud_storage_api_base", "");
        ArrayList<String> baseURIs = new ArrayList<String>();

        try {
            for (String baseURLSetting : baseURLSettings.split(" ")) {
                String baseURL = baseURLSetting.trim();
                if (baseURL.isEmpty()) continue;
                baseURIs.add(baseURL + (baseURL.endsWith("/") ? "" : "/"));
            }
        } catch (Exception e) {
            return;
        }

        StringBuilder newUris = new StringBuilder();

        for (Iterator<String> i = baseURIs.iterator(); i.hasNext();) {
            String uriString = i.next();
            if (uriString.contains("@http")) {
                String[] uriParts = uriString.split("@");
                Uri newUri;
                if (uriParts.length == 2) {
                    Uri oldUri = Uri.parse(uriParts[1]);
                    newUri = oldUri.buildUpon().encodedAuthority(uriParts[0] + "@" + oldUri.getEncodedAuthority()).build();
                } else {
                    newUri = Uri.parse(uriString);
                }
                newUris.append(newUri.toString());
            } else {
                newUris.append(uriString);
            }
            if (i.hasNext())
                newUris.append(" ");
        }
        if (newUris.length() != 0) {
            prefs.edit().putString("cloud_storage_api_base", newUris.toString()).apply();
        } else {
            // instead of an empty string: delete the setting to show (but later not read) default string
            prefs.edit().remove("cloud_storage_api_base").apply();
        }
    }

    // When adding a new setting to an existing function, this method lets us control what happens to an existing
    // xDrip install after update.  The new added setting should offer more flexibility to the user rather than modifying
    // the xDrip behavior without the user's knowledge.
    public static void startup() {
        if (!Pref.isPreferenceSet("warning_agreed_to")) { // This is the very first time xDrip is running after a fresh install
            try { // Everything here runs only once after a fresh install

                Pref.setBoolean("migrate_persistent_high_alert_threshold", false); // Never migrate; we will use the default

            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Initial startup settings failed! Please report.");
            }
        }
        try { // Everything here runs after startup, fresh install or not

            if (Pref.getBoolean("migrate_persistent_high_alert_threshold", true)) { // Only if not migrated yet
                final String vThreshold = Pref.getString("highValue", "170");
                UserError.Log.e(TAG, "You can now set a persistent high alert threshold. Until you do, we set it equal to your current High Value setting: " + vThreshold);
                Pref.setString("persistent_high_threshold", vThreshold); // Set the persistent high alert threshold equal to the high value
                Pref.setBoolean("migrate_persistent_high_alert_threshold", false); // Done - never migrate again

            }
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Startup settings failed! Please report");
        }
    }

    // This function moves us from calibrate_external_libre_2_algorithm which is a boolean to a
    // multi value list option
    public static void migrateOOP2CalibrationPreferences() {
        val oldPref = "calibrate_external_libre_2_algorithm";
        val newPref = "calibrate_external_libre_2_algorithm_type";
        if (Pref.isPreferenceSet(oldPref) && !Pref.isPreferenceSet(newPref)) {
            Log.e(TAG, oldPref + " found - updating to new style");
            Pref.setString(newPref, Pref.getBooleanDefaultFalse(oldPref) ? "calibrate_raw" : "no_calibration");
        }
    }

    // Force legacy settings to be at their recommended values
    private static void legacySettingsFix() {
        Pref.setBoolean("use_ob1_g5_collector_service", true);
        Pref.setBoolean("ob1_g5_fallback_to_xdrip", false);
        Pref.setBoolean("always_unbond_G5", false);
        Pref.setBoolean("always_get_new_keys", true);
        Pref.setBoolean("run_ble_scan_constantly", false);
        Pref.setBoolean("run_G5_ble_tasks_on_uithread", false);
    }
    private static void legacySettingsMoveLanguageFromNoToNb() {
        // Check if the user's language preference is set to "no"
        if ("no".equals(Pref.getString("forced_language", ""))) {
        // Update the language preference to "nb"
        Pref.setString("forced_language", "nb");
        }
    }
}
