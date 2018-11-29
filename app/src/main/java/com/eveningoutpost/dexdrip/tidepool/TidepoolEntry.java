package com.eveningoutpost.dexdrip.tidepool;

// jamorham

// lightweight class entry point

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

public class TidepoolEntry {


    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("cloud_storage_tidepool_enable");
    }

    public static void newData() {
        if (enabled() && JoH.pratelimit("tidepool-new-data-upload", 1200)) {
            TidepoolUploader.doLogin(false);
        }
    }
}
