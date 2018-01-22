package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.google.common.collect.ImmutableSet;

import java.io.File;
import java.util.Locale;

/**
 * Created by jamorham on 22/01/2017.
 * <p>
 * Gauge a user's experience level
 */

public class Experience {

    private static final String TAG = "xdrip-Experience";
    private static final String marker = "xdrip-plus-installed-time";

    // caches
    private static boolean got_data = false;
    private static boolean not_newbie = false;

    public static boolean isNewbie() {
        if (not_newbie) return false;
        final long installed_time = Pref.getLong(marker, -1);
        if (installed_time > 0) {
            UserError.Log.d(TAG, "First Installed " + JoH.niceTimeSince(installed_time) + " ago");
            not_newbie = true;
            return false;
        } else {
            // probably newbie
            Pref.setLong(marker, JoH.tsl());
            if (gotData()) return false;
            UserError.Log.d(TAG, "Looks like a Newbie");
            return true;
        }
    }

    public static boolean gotData() {
        if (got_data) return true;
        if (BgReading.last(true) != null) {
            got_data = true;
            return true;
        } else {
            return false;
        }
    }

    public static boolean backupAvailable() {
        final String backup_file = Pref.getString("last-saved-database-zip", "");
        if (backup_file.length() > 0) {
            if (new File(backup_file).exists()) {
                return true;
            }
        }
        return false;
    }


    private static final ImmutableSet<String> mmol_countries = ImmutableSet.of("AU", "CA", "CN", "HK", "MO", "TW", "HR", "CZ", "DE", "DK", "FI", "HK", "HU", "IS", "IE", "JM", "KZ", "YK", "LV", "LT", "MY", "MT", "NL", "AN", "NZ", "NO", "RU", "SK", "SI", "ZA", "SE", "CH", "GB");

    private static String defaultUnits() {
        try {
            final String country = Locale.getDefault().getCountry();
            final String units = mmol_countries.contains(country) ? "mmol/l" : "mg/dl";
            UserError.Log.d(TAG, "Country: " + country + " default units: " + units);
            return units;
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception trying to determine locale units: " + e);
            return "mg/dl";
        }
    }

    public static boolean defaultUnitsAreMmol() {
        return defaultUnits().equals("mmol/l");
    }

}
