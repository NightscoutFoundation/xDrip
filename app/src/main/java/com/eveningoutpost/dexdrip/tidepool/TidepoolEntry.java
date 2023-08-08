package com.eveningoutpost.dexdrip.tidepool;

// jamorham

// lightweight class entry point

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import static com.eveningoutpost.dexdrip.models.JoH.isLANConnected;
import static com.eveningoutpost.dexdrip.utils.PowerStateReceiver.is_power_connected;

public class TidepoolEntry {


    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("cloud_storage_tidepool_enable");
    }

    public static void newData() {
        if (enabled()
                && (!Pref.getBooleanDefaultFalse("tidepool_only_while_charging") || is_power_connected())
                && (!Pref.getBooleanDefaultFalse("tidepool_only_while_unmetered") || isLANConnected())
                && JoH.pratelimit("tidepool-new-data-upload", 1200)) {
            TidepoolUploader.doLogin(false);
        }
    }
}
