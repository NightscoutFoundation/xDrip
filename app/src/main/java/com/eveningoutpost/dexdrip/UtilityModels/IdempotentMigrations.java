package com.eveningoutpost.dexdrip.UtilityModels;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.SnoozeActivity;
import com.eveningoutpost.dexdrip.Models.AlertType;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by stephenblack on 4/15/15.
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
    }

    private void migrateBGAlerts() {
        // Migrate away from old style notifications to Tzachis new Alert system
        AlertType.CreateStaticAlerts();
        if(prefs.getBoolean("bg_notifications", true)){
            double highMark = Double.parseDouble(prefs.getString("highValue", "170"));
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


            AlertType.add_alert(null, "High Alert", true, highMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, bg_high_snooze);
            AlertType.add_alert(null, "Low Alert", false, lowMark, true, 1, bg_notification_sound, 0, 0, bg_sound_in_silent, bg_low_snooze);
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

        prefs.edit().putString("cloud_storage_api_base", newUris.toString()).apply();
    }

}
