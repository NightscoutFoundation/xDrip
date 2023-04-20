package com.eveningoutpost.dexdrip.services.broadcastservice;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

public class BroadcastEntry {
    //a tiny class created to make sure the service class would not be loaded if service disabled
    public static final String PREF_ENABLED = "broadcast_service_enabled";

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("broadcast-service-initial-start", 500, () -> JoH.startService(BroadcastService.class));
        }
    }

    public static void sendLatestBG() {
        if (isEnabled()) {
            JoH.startService(BroadcastService.class, Const.INTENT_FUNCTION_KEY, Const.CMD_UPDATE_BG);
        }
    }

    public static void cancelAlert() {
        if (isEnabled()) {
             JoH.startService(BroadcastService.class, Const.INTENT_FUNCTION_KEY, Const.CMD_CANCEL_ALERT);
        }
    }

    public static void sendAlert(String type, String message) {
        if (isEnabled()) {
            Inevitable.task("broadcast-service-send-alert", 100, () -> JoH.startService(BroadcastService.class,
                    Const.INTENT_FUNCTION_KEY, Const.CMD_ALERT,
                    "message", message,
                    "type", type));
        }
    }
}
