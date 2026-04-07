package com.eveningoutpost.dexdrip.cloud.nightlite;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import java.util.List;

// JamOrHam

public class NightLiteEntry {

    public static final String TAG = "NightLiteEntry";
    public static final String API_PREF = "nightlite-api-url";
    public static final String NSLITE_ENABLED = "nightlite-enabled";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(NSLITE_ENABLED);
    }

    public static void setEnabled(boolean enabled) {
        Pref.setBoolean(NSLITE_ENABLED, enabled);
    }

    public static void uploadIfEnabled() {
        if (isEnabled()) {
            NightLiteClient.doUpload();
        }
    }

    public static boolean setApi(List<String> apiUris) {
        if (apiUris != null && !apiUris.isEmpty()) {
            Pref.setString(API_PREF, apiUris.get(0));
            setEnabled(true);
            return true;
        }
        return false;
    }

    public static String getApi() {
        if (isEnabled()) {
            return Pref.getString(API_PREF, "");
        }
        return "";
    }
}
