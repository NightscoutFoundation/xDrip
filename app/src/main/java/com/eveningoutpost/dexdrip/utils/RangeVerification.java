package com.eveningoutpost.dexdrip.utils;

import static com.eveningoutpost.dexdrip.EditAlertActivity.unitsConvert2Disp;
import static com.eveningoutpost.dexdrip.models.JoH.showNotification;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.OUT_OF_RANGE_GLUCOSE_ENTRY_ID;
import static java.lang.Math.abs;

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
    private static double MIN = 40; // The smallest blood glucose value xDrip accepts as an input
    private static double MAX = 400; // The greatest blood glucose value xDrip accepts as an input

    public static boolean verifyRange(String prefKey) {

        boolean doMgdl = (Pref.getString("units", "mgdl").compareTo("mgdl") == 0); // True if we are using mg/dL - False if we are using mmol/L
        double valueUiMgdl = Home.convertToMgDlIfMmol(JoH.tolerantParseDouble(Pref.getString(prefKey, ""))); // The value entered by the user shown in mg/dL
        final String prefVerifiedKey = prefKey + "_verified"; // The key for the shadow setting representing the verified value
        double valueVerMgdl = Home.convertToMgDlIfMmol(JoH.tolerantParseDouble(Pref.getString(prefVerifiedKey, ""))); // Verified entered value in mg/dL

        String unit = "mmol/l";
        if (doMgdl) { // Correct the unit if needed
            unit = "mg/dl";
        }
        final String msg = "Value needs to be between " + unitsConvert2Disp(doMgdl, MIN) + " and " + unitsConvert2Disp(doMgdl, MAX); // Toast and notification content
        final String msgLongLow = prefKey + " value entered is less than " + unitsConvert2Disp(doMgdl, MIN) + " " + unit + " and cannot be accepted"; // Log content
        final String msgLongHigh = prefKey + " value entered is greater than " + unitsConvert2Disp(doMgdl, MAX) + " " + unit + " and cannot be accepted"; // Log content

        if (valueUiMgdl < MIN) { // The value entered is less than the allowed minimum
            undoChange(prefKey, msg); // Restore the setting to the value of its shadow.
            UserError.Log.e(TAG, msgLongLow);
            return false;
        } else if (valueUiMgdl > MAX) { // The value entered is greater than the allowed maximum
            undoChange(prefKey, msg); // Restore the setting to the value of its shadow.
            UserError.Log.e(TAG, msgLongHigh);
            return false;
        } else { // The value entered is in range.
            if (abs(valueUiMgdl - valueVerMgdl) > 0.01) { // Ignore submitted values too close to the current value.
                approveChange(prefKey); // Update the shadow setting to the submitted value by the user
                UserError.Log.d(TAG, "Approving " + prefKey + " submission");
            }
            return true;
        }
    }

    public static void defaultCorrection(String prefKey) {
        value2Large4mmol(prefKey); // Correct the default value if needed.
        String shadowPrefKey = prefKey + "_verified"; // Shadow setting key
        value2Large4mmol(shadowPrefKey); // Correct the default shadow value if needed.
    }

    private static void value2Large4mmol(String prefKey) { // The 35/36 correction mechanism is only triggered when the user changes units.
        // But, the defaults are always in mg/dL.  If the unit is mmol/L, and the user updates from an older version of xDrip,
        // the default value of the new setting will be in mg/dL and will never be corrected unless the user changes units back and forth.
        // This method corrects the default value if needed even if the user does not change units.
        boolean doMgdl = (Pref.getString("units", "mgdl").compareTo("mgdl") == 0);
        if (!doMgdl) { // If we are using the mmol/L unit
            double value = JoH.tolerantParseDouble(Pref.getString(prefKey, ""));
            if (value > 35) { // If the value is greater than 35, it means the value corresponds to the mg/dL unit.
                Pref.setString(prefKey, JoH.qs(value * Constants.MGDL_TO_MMOLL, 1)); // Convert the value from mg/dL to mmol/L
                UserError.Log.d(TAG, "Converting " + prefKey + " to mmom/l " + value);
            }
        }
    }

    private static void undoChange(String prefKey, String msg) { // Restore the setting to the value of the shadow setting
        String shadowKey = prefKey + "_verified"; // The key of the shadow setting
        String old = Pref.getString(shadowKey, ""); // Shadow setting
        Pref.setString(prefKey, old); // Restore the setting to the value of its shadow.
        JoH.static_toast_long(msg);
        rejectionNotification(prefKey, msg); // Unfortunately, the toast message comes up with a delay of a few seconds if there is an active session.
        // If there is no active session, the toast will come up only when the user opens settings.
        // For that reason, a silent notification is also shown to provide more detail and serve as a reminder.
    }

    private static void approveChange(String prefKey) { // Update the shadow setting to the new value entered by the user.
        String shadowKey = prefKey + "_verified"; // The key of the shadow setting
        String submission = Pref.getString(prefKey, ""); // Submitted setting
        Pref.setString(shadowKey, submission); // Update the shadow setting
    }

    private static void rejectionNotification(String title, String msg) { // Show a silent notification
        val notificationId = OUT_OF_RANGE_GLUCOSE_ENTRY_ID; // Notification ID
        showNotification(title, msg, null, notificationId, null, false, false, null, null, null, true);
    }
}

