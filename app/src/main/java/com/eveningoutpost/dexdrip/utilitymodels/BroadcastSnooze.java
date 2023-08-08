package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Intent;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import static com.eveningoutpost.dexdrip.xdrip.getAppContext;

// jamorham

// This could include more detail

public class BroadcastSnooze {

    private static final String TAG = "BroadcastSnooze";

    public static void send() {

        if (Pref.getBooleanDefaultFalse("broadcast_snooze")) {
            if (JoH.ratelimit("broadcast-snooze-send", 2)) {
                final Intent intent = new Intent(Intents.ACTION_SNOOZE);
                intent.putExtra(Intents.EXTRA_SENDER, BuildConfig.APPLICATION_ID);
                getAppContext().sendBroadcast(intent);
                UserError.Log.d(TAG, "Sent snooze");
            }
        }
    }

}
