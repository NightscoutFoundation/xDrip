package com.eveningoutpost.dexdrip.UtilityModels;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.primitives.Bytes;

/**
 * Created by jamorham on 23/09/2016.
 *
 * This is for internal data which is never backed up,
 * separate file means it doesn't clutter prefs
 * we can afford to lose it, it is for internal states
 * and is alternative to static variables which get
 * flushed when classes are destroyed by garbage collection
 *
 * It is suitable for cache type variables where losing
 * state will cause problems. Obviously it will be slower than
 * pure in-memory state variables.
 *
 */


public class PersistentStore {

    private static final String DATA_STORE_INTERNAL = "persist_internal_store";
    private static SharedPreferences prefs;
    private static final boolean d = false; // debug flag

    public static void init_prefs() {
        if (prefs == null) prefs = xdrip.getAppContext()
                .getSharedPreferences(DATA_STORE_INTERNAL, Context.MODE_PRIVATE);


    }

    public static String getString(String name) {
        init_prefs();
        return prefs.getString(name, "");
    }

    public static void setString(String name, String value) {
        init_prefs();
        prefs.edit().putString(name, value).apply();
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

    public static void setBytes(String name, byte[] value) {
        setString(name, JoH.base64encodeBytes(value));
    }

    public static long getLong(String name) {
        init_prefs();
        return prefs.getLong(name, 0);
    }

    public static void setLong(String name, long value) {
        init_prefs();
        prefs.edit().putLong(name, value).apply();
    }

    public static boolean getBoolean(String name) {
        init_prefs();
        return prefs.getBoolean(name, false);
    }

    public static void setBoolean(String name, boolean value) {
        init_prefs();
        prefs.edit().putBoolean(name, value).apply();
    }

    public static long incrementLong(String name) {
        final long val = getLong(name) + 1;
        setLong(name, val);
        return val;
    }

    public static void setLongZeroIfSet(String name) {
        if (getLong(name)>0) setLong(name,0);
    }

    @SuppressLint("ApplySharedPref")
    public static void commit() {
        init_prefs();
        prefs.edit().commit();
    }
}
