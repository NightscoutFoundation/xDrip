package com.eveningoutpost.dexdrip.utils.framework;

// jamorham

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.watch.lefun.LeFun;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.Getter;

public class IncomingCallsReceiver extends BroadcastReceiver {

    private static final String TAG = "IncomingCalls";
    @Getter
    private static volatile boolean ringingNow = false;

    public static void checkPermission(final Activity activity) {

        // TODO call log permission - especially for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (xdrip.getAppContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        Constants.GET_PHONE_READ_PERMISSION);
            }
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        final String stateExtra = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        final String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

        // TODO lookup contacts
        UserError.Log.d(TAG, "Call State: " + stateExtra + " " + number);
        if (stateExtra != null && stateExtra.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            ringingNow = true;
            if (JoH.quietratelimit("lefun-call-debounce", 10)) {
                if (LeFunEntry.areCallAlertsEnabled()) {
                    UserError.Log.d(TAG, "Sending call alert: " + number);
                    // TODO extract to generic notifier
                    final String caller = number != null ? number.substring(Math.max(0, number.length() - 8)) : "CALL";
                    LeFun.sendAlert(true, caller);
                }
            }
        } else {
            if (ringingNow) {
                ringingNow = false;
                UserError.Log.d(TAG, "Ringing stopped: " + stateExtra);
            }
        }
    }
}

