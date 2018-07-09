package com.eveningoutpost.dexdrip.eassist;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.JoH;

// jamorham

// track active alerts and pass on to EmergencyAssist checker


public class AlertTracker {

    public static void evaluate() {

        final ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();

        if (activeBgAlert != null) {
            if (!activeBgAlert.is_snoozed) {
                if (JoH.ratelimit("alert-tracker-eval", 10)) {
                    final AlertType type = ActiveBgAlert.alertTypegetOnly(activeBgAlert);
                    if (type != null) {
                        final long since = JoH.msSince(activeBgAlert.alert_started_at);
                        String summary = "";
                        try {
                            summary = "(glucose " + BestGlucose.getDisplayGlucose().humanSummary() + ")";
                        } catch (Exception e) {
                            //
                        }
                        if (!type.above) {
                            EmergencyAssist.checkAndActivate(EmergencyAssist.Reason.DID_NOT_ACKNOWLEDGE_LOW_ALERT,
                                    since, summary);
                        } else {
                            EmergencyAssist.checkAndActivate(EmergencyAssist.Reason.DID_NOT_ACKNOWLEDGE_HIGH_ALERT,
                                    since, summary);
                        }
                    }
                }
            }
        }
    }
}
