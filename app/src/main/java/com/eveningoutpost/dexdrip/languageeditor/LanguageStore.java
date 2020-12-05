package com.eveningoutpost.dexdrip.languageeditor;

import android.content.Context;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.Locale;

/**
 * Created by jamorham on 04/07/2016.
 */


public class LanguageStore {

    private static final String JAMORHAM_LANGUAGE = "jamorham_language";

    public static String getString(String key) {
        if (key == null) {
            UserError.Log.e(JAMORHAM_LANGUAGE, "getString key is null!");
            return "";
        }
        return xdrip.getAppContext()
                .getSharedPreferences(JAMORHAM_LANGUAGE+"-"+ Locale.getDefault().toString(), Context.MODE_PRIVATE)
                .getString(key, "");
    }

    public static void putString(String key, String value) {
        if (key == null) {
            UserError.Log.e(JAMORHAM_LANGUAGE, "putString key is null!");
            return;
        }
        if (value == null) {
            UserError.Log.e(JAMORHAM_LANGUAGE, "putString key is null!");
            return;
        }
        xdrip.getAppContext()
                .getSharedPreferences(JAMORHAM_LANGUAGE+"-"+ Locale.getDefault().toString(), Context.MODE_PRIVATE)
                .edit().putString(key, value).apply();
    }

    public static void resetEverything() {
        xdrip.getAppContext().getSharedPreferences(JAMORHAM_LANGUAGE+"-"+ Locale.getDefault().toString(), Context.MODE_PRIVATE).edit().clear().commit();
    }
}
