package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.DEX_BASE_ID;

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
