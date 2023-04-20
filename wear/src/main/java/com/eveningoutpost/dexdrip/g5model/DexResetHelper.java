package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DEX_BASE_ID;

// jamorham

public class DexResetHelper {

    private static final String TAG = "DexResetHelper";

    public static void offer(String reason) {

        UserError.Log.wtf(TAG, "Cannot offer reset on wear: " + reason);
    }

    public static void cancel() {
        JoH.cancelNotification(DEX_BASE_ID);
    }

}
