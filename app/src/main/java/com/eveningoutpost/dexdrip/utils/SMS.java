package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;

import static android.telephony.TelephonyManager.SIM_STATE_READY;

/**
 * Created by jamorham on 18/01/2017.
 */

public class SMS {

    private static final String TAG = "xDrip-SMS";


    public static boolean hasSMScapability() {

        final TelephonyManager manager = (TelephonyManager) xdrip.getAppContext()
                .getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            UserError.Log.d(TAG, "Phone type: " + manager.getPhoneType());

            UserError.Log.d(TAG, "Sim State: " + manager.getSimState());

            if (manager.getSimState() == SIM_STATE_READY) {
                return true;
            }


        } else {
            UserError.Log.e(TAG, "Could not get telephony manager");
        }
        //  if (manager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
        //
        //  } else {
        //       // has SMS
        //  }
        return false;
    }


    public static boolean sendSMS(String destination, String message) {
        try {

            // TODO TRACK PARTS ON SEND INTENTS
            final SmsManager smsManager = SmsManager.getDefault();
            if (message.length() > 160) {
                final ArrayList<String> messageParts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(destination, null, messageParts, null, null);
            } else {
                smsManager.sendTextMessage(destination, null, message, null, null);
            }
            return true;
        } catch (SecurityException e) {
            UserError.Log.wtf(TAG, "Error sending SMS! no permission? " + e);
            // warn user? disable feature?
        }
        return false;
    }


}
