package com.eveningoutpost.dexdrip.utilitymodels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.primitives.Bytes;

/**
 * Created by jamorham on 23/09/2016.
 * <p>
 * This is for internal data which is never backed up,
 * separate file means it doesn't clutter prefs
 * we can afford to lose it, it is for internal states
 * and is alternative to static variables which get
 * flushed when classes are destroyed by garbage collection
 * <p>
 * It is suitable for cache type variables where losing
 * state will cause problems. Obviously it will be slower than
 * pure in-memory state variables.
 */


public class PersistentStore {

    private static final String DATA_STORE_INTERNAL = "persist_internal_store";
    private static SharedPreferences prefs;
    private static final boolean d = false; // debug flag

    public static String getString(final String name) {
        return prefs.getString(name, "");
    }
    
    public static String getString(final String name, String defaultValue) {
        return prefs.getString(name, defaultValue);
    }
    
    public static int getStringToInt(final String name, final int defaultValue) {
        try {
            return Integer.parseInt(getString(name, Integer.toString(defaultValue)));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static boolean removeItem(final String pref) {
        if (prefs != null) {
            prefs.edit().remove(pref).apply();
            return true;
        }
        return false;
    }

    static {
        try {
            prefs = xdrip.getAppContext()
                    .getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);
        } catch (NullPointerException e) {
            android.util.Log.e("PersistentStore", "Failed to get context on init!!! nothing will work");
        }
    }

    public static void setString(final String name, String value) {
        prefs.edit().putString(name, value).apply();
    }

    // if string is different to what we have stored then update and return true
    public static boolean updateStringIfDifferent(final String name, final String current) {
        if (current == null) return false; // can't handle nulls
        if (PersistentStore.getString(name).equals(current)) return false;
        PersistentStore.setString(name, current);
        return true;
    }

    public static void appendString(String name, String value) {
        setString(name, getString(name) + value);
    }

    public static void appendString(String name, String value, String delimiter) {
        String current = getString(name);
        if (current.length() > 0) current += delimiter;
        setString(name, current + value);
    }

    public static void appendBytes(String name, byte[] value) {
        setBytes(name, Bytes.concat(getBytes(name), value));
    }

    public static byte[] getBytes(String name) {
        return JoH.base64decodeBytes(getString(name));
    }

    public static byte getByte(String name) {
        return (byte)getLong(name);
    }

    public static void setBytes(String name, byte[] value) {
        setString(name, JoH.base64encodeBytes(value));
    }

    public static void setByte(String name, byte value) {
        setLong(name, value);
    }

    public static long getLong(String name) {
        return prefs.getLong(name, 0);
    }

    public static float getFloat(String name) {
        return prefs.getFloat(name, 0);
    }

    public static void setLong(String name, long value) {
        prefs.edit().putLong(name, value).apply();
    }

    public static void setFloat(String name, float value) {
        prefs.edit().putFloat(name, value).apply();
    }

    public static void setDouble(String name, double value) {
        setLong(name, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(String name) {
        return Double.longBitsToDouble(getLong(name));
    }

    public static boolean getBoolean(String name) {
        return prefs.getBoolean(name, false);
    }

    public static boolean getBoolean(String name, boolean value) {
        return prefs.getBoolean(name, value);
    }

    public static void setBoolean(String name, boolean value) {
        prefs.edit().putBoolean(name, value).apply();
    }

    public static long incrementLong(String name) {
        final long val = getLong(name) + 1;
        setLong(name, val);
        return val;
    }

    public static void setLongZeroIfSet(String name) {
        if (getLong(name) > 0) setLong(name, 0);
    }

    @SuppressLint("ApplySharedPref")
    public static void commit() {
        prefs.edit().commit();
    }
}
