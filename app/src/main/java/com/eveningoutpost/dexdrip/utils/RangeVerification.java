package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.EditAlertActivity.unitsConvert2Disp;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.OUT_OF_RANGE_GLUCOSE_ENTRY_ID;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SENSORY_EXPIRY_NOTIFICATION_ID;
import static java.lang.Math.abs;

import android.preference.PreferenceScreen;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import lombok.val;

/**
 * Navid200
 * <p>
 * Utility for verifying that the glucose value entered is in the expected range.
 * If it is not, the setting is corrected back to what it was and the user is notified by a log, a toast and a notification.
 */

public class RangeVerification {
    private static final String TAG = RangeVerification.class.getSimpleName();
    // We may some day parameterize the following two.
    private static double MIN = 40; // The smallest blood glucose value xDrip accepts as an input
    private static double MAX = 400; // The greatest blood glucose value xDrip accepts as an input
    private static boolean doMgdl;
    private static String msg = "";
    private static String unit = "";
    public static boolean verifyRange(String prefKey) {

        doMgdl = (Pref.getString("units", "mgdl").compareTo("mgdl") == 0); // True if we are using mg/dL - False if we are using mmol/L
        final Double valueUiMgdl = Home.convertToMgDlIfMmol(JoH.tolerantParseDouble(Pref.getString(prefKey, ""))); // The value entered by the user shown in mg/dL
        final String prefVerifiedKey = prefKey + "_verified"; // The key for the shadow setting representing the verified value
        final Double valueVerMgdl = Home.convertToMgDlIfMmol(JoH.tolerantParseDouble(Pref.getString(prefVerifiedKey, ""))); // Verified entered value in mg/dL

        msg = "Value needs to be between " + unitsConvert2Disp(doMgdl, MIN) + " and " + unitsConvert2Disp(doMgdl, MAX); // Toast and notification content
        unit = "mmol/l";
        if (doMgdl) { // Correct the unit if needed
            unit = "mg/dl";
        }
        final String msgLongLow = "Value entered is less than " + unitsConvert2Disp(doMgdl, MIN) + " " + unit + " and cannot be accepted";
        final String msgLongHigh = "Value entered is greater than " + unitsConvert2Disp(doMgdl, MAX) + " " + unit + " and cannot be accepted";

        if (valueUiMgdl < MIN) { // The value entered is less than the allowed minimum
            if (doMgdl) { // We are using mg/dL
                Pref.setString(prefKey, Long.toString(Math.round(valueVerMgdl))); // Correct the UI setting
            } else { // We are using mmol/L
                Pref.setString(prefKey, JoH.qs(Math.round(valueVerMgdl * Constants.MGDL_TO_MMOLL), 1)); // Correct the UI setting
            }
            JoH.static_toast_long(msg);
            issueNotification(prefKey); // Unfortunately, the toast message comes up with a delay of a few seconds.  For that reason, a silent notification is also added.
            UserError.Log.e(TAG, msgLongLow);
            return false;
        } else if (valueUiMgdl > MAX) { // The value entered is greater than the allowed maximum
            if (doMgdl) { // We are using mg/dL
                Pref.setString(prefKey, Long.toString(Math.round(valueVerMgdl))); // Correct the UI setting
            } else { // We are using mmol/L
                Pref.setString(prefKey, JoH.qs(Math.round(valueVerMgdl * Constants.MGDL_TO_MMOLL), 1)); // Correct the UI setting
            }
            JoH.static_toast_long(msg);
            issueNotification(prefKey); // Unfortunately, the toast message comes up with a delay of a few seconds.  For that reason, a silent notification is also added.
            UserError.Log.e(TAG, msgLongHigh);
            return false;

        } else { // The value entered is in range.
            if (abs(valueUiMgdl - valueVerMgdl) > 0.01) { // Ignore values too close to the current value.
                if (doMgdl) { // We are using mg/dL
                    Pref.setString(prefVerifiedKey, Long.toString(Math.round(valueUiMgdl))); // Update the verified setting
                } else { // We are using mmol/L
                    Pref.setString(prefVerifiedKey, JoH.qs(Math.round(valueUiMgdl * Constants.MGDL_TO_MMOLL), 1)); // Update the verified setting
                }
            }
            return true;
        }
    }

    private static void issueNotification(String title) { // Show a silent notification
        val notificationId = OUT_OF_RANGE_GLUCOSE_ENTRY_ID; // Notification ID
        showNotification(title, msg, null, notificationId, null, false, false, null, null, null, true);
    }
}
