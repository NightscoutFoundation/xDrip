package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by jamorham on 01/01/2018.
 * <p>
 * Simplified, cached static access to default preferences store
 */

public class Pref {

    private static final String TAG = "Pref";
    private static SharedPreferences prefs;

    // TODO optimize initializePrefs()

    // cache instance
    private static void initializePrefs() {
        if (prefs == null) {
            if (xdrip.getAppContext() != null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
                if ((prefs == null) && (JoH.ratelimit("prefs-failure1", 20))) {
                    UserError.Log.wtf(TAG, "Could not initialize preferences due to init failure!!");
                }
            } else {
                if (JoH.ratelimit("prefs-failure2", 20)) {
                    UserError.Log.wtf(TAG, "Could not initialize preferences due to missing context!!");
                }
            }
        }
    }

    public static SharedPreferences getInstance() {
        initializePrefs();
        return prefs;
    }


    // booleans
    public static boolean getBooleanDefaultFalse(final String pref) {
        initializePrefs();
        return (prefs != null) && (prefs.getBoolean(pref, false));
    }

    public static boolean getBoolean(final String pref, boolean def) {
        initializePrefs();
        return (prefs != null) && (prefs.getBoolean(pref, def));
    }

    public static boolean setBoolean(final String pref, final boolean lng) {
        initializePrefs();
        if (prefs != null) {
            prefs.edit().putBoolean(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static void toggleBoolean(final String pref) {
        initializePrefs();
        if (prefs != null) prefs.edit().putBoolean(pref, !prefs.getBoolean(pref, false)).apply();
    }


    // strings
    public static String getStringDefaultBlank(final String pref) {
        initializePrefs();
        if (prefs != null) {
            return prefs.getString(pref, "");
        }
        return "";
    }

    public static String getString(final String pref, final String def) {
        initializePrefs();
        if (prefs != null) {
            return prefs.getString(pref, def);
        }
        return "";
    }

    public static boolean setString(final String pref, final String str) {
        initializePrefs();
        if (prefs != null) {
            prefs.edit().putString(pref, str).apply();
            return true;
        }
        return false;
    }


    // numbers
    public static long getLong(final String pref, final long def) {
        initializePrefs();
        if (prefs != null) {
            return prefs.getLong(pref, def);
        }
        return def;
    }

    public static boolean setLong(final String pref, final long lng) {
        initializePrefs();
        if (prefs != null) {
            prefs.edit().putLong(pref, lng).apply();
            return true;
        }
        return false;
    }

    public static int getInt(final String pref, final int def) {
        initializePrefs();
        if (prefs != null) {
            return prefs.getInt(pref, def);
        }
        return def;
    }

    public static int getStringToInt(final String pref, final int defaultValue) {
        try {
            return Integer.parseInt(getString(pref, Integer.toString(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static double getStringToDouble(final String pref, final double defaultValue) {
        try {
            return JoH.tolerantParseDouble(getString(pref, Double.toString(defaultValue)), defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean setInt(final String pref, final int num) {
        initializePrefs();
        if (prefs != null) {
            prefs.edit().putInt(pref, num).apply();
            return true;
        }
        return false;
    }


    // misc
    public static boolean removeItem(final String pref) {
        initializePrefs();
        if (prefs != null) {
            prefs.edit().remove(pref).apply();
            return true;
        }
        return false;
    }

}
