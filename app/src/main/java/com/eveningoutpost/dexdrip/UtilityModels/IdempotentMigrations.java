package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.Libre2RawValue;
import com.eveningoutpost.dexdrip.Models.Libre2Sensor;
import com.eveningoutpost.dexdrip.Models.APStatus;
import com.eveningoutpost.dexdrip.Models.BgReading;
//import com.eveningoutpost.dexdrip.Models.BgReadingArchive;
import com.eveningoutpost.dexdrip.Models.DesertSync;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.LibreData;
import com.eveningoutpost.dexdrip.Models.PenData;
import com.eveningoutpost.dexdrip.Models.Prediction;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.Models.AlertType;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Emma Black on 4/15/15.
 */
public class IdempotentMigrations {
    private Context mContext;
    private SharedPreferences prefs;

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
        JoH.clearCache();

        IncompatibleApps.notifyAboutIncompatibleApps();
        CompatibleApps.notifyAboutCompatibleApps();

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
            boolean bg_sound_in_silent = prefs.getBoolean("bg_sound_in_silent", false);
            String bg_notification_sound = prefs.getString("bg_notification_sound", "content://settings/system/notification_sound");

            int bg_high_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(true))));
            int bg_low_snooze = Integer.parseInt(prefs.getString("bg_snooze",  Integer.toString(SnoozeActivity.getDefaultSnooze(false))));


            AlertType.add_alert(null, mContext.getString(R.string.high_alert), true, highMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, false, bg_high_snooze, true, true);
            AlertType.add_alert(null, mContext.getString(R.string.low_alert), false, lowMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, false, bg_low_snooze, true, true);
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

}
