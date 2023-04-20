package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.CompatibleApps;

import static com.eveningoutpost.dexdrip.utilitymodels.CompatibleApps.createActionIntent;
import static com.eveningoutpost.dexdrip.utilitymodels.CompatibleApps.createChoiceIntent;
import static com.eveningoutpost.dexdrip.utilitymodels.CompatibleApps.showNotification;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DEX_BASE_ID;

// jamorham

public class DexResetHelper {

    private static final String TAG = "DexResetHelper";

    public static void offer(String reason) {

        if (JoH.pratelimit("offer-hard-reset-dex", 1200)) {
            int id = DEX_BASE_ID;

            final String title = "Hard Reset Transmitter?";

            // piggybacking on the existing choice dialog system for compatible apps
            showNotification(title, reason,
                    createActionIntent(id, id + 1, CompatibleApps.Feature.HARD_RESET_TRANSMITTER),
                    createActionIntent(id, id + 2, CompatibleApps.Feature.CANCEL),
                    createChoiceIntent(id, id + 3, CompatibleApps.Feature.HARD_RESET_TRANSMITTER, title, reason),
                    id);
        } else {
            UserError.Log.d(TAG, "Not offering reset as within rate limit");
        }
    }

    public static void cancel() {
        JoH.cancelNotification(DEX_BASE_ID);
    }

}
