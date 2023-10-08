package com.eveningoutpost.dexdrip.eassist;

import static com.eveningoutpost.dexdrip.eassist.EmergencyAssist.Reason.DID_NOT_ACKNOWLEDGE_HIGH_ALERT;
import static com.eveningoutpost.dexdrip.eassist.EmergencyAssist.Reason.DID_NOT_ACKNOWLEDGE_LOWEST_ALERT;
import static com.eveningoutpost.dexdrip.eassist.EmergencyAssist.Reason.DID_NOT_ACKNOWLEDGE_LOW_ALERT;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import lombok.val;

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
                                // Determine if is lowest alert
                                val lowest = AlertType.getLowestAlert();
                                val thisAlertType = ActiveBgAlert.alertTypegetOnly(activeBgAlert);
                                if (lowest == null || thisAlertType == null
                                        || lowest.threshold == thisAlertType.threshold) {
                                    // Always flag as lowest unless we know it isn't
                                    EmergencyAssist.checkAndActivate(DID_NOT_ACKNOWLEDGE_LOWEST_ALERT, since, summary);
                                }

                            EmergencyAssist.checkAndActivate(DID_NOT_ACKNOWLEDGE_LOW_ALERT, since, summary);
                        } else {
                            EmergencyAssist.checkAndActivate(DID_NOT_ACKNOWLEDGE_HIGH_ALERT,
                                    since, summary);
                        }
                    }
                }
            }
        }
    }
}
