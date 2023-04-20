package com.eveningoutpost.dexdrip.healthconnect;

import android.os.Build;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

// jamorham

public class HealthConnectEntry {

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("health_connect_enable");
    }

    public static boolean sendEnabled() {
        return enabled() && Pref.getBooleanDefaultFalse("health_connect_send");
    }

    public static boolean receiveEnabled() {
        return enabled() && Pref.getBooleanDefaultFalse("health_connect_receive");
    }

    public static void ping() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (receiveEnabled()) {
                if (JoH.ratelimit("health-connect-read", 290)) {
                    HealthGamut.ping();
                }
            }
        }
    }

}
