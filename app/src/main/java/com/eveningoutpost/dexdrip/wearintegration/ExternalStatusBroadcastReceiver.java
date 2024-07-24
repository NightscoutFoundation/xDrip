package com.eveningoutpost.dexdrip.wearintegration;

import android.content.Context;
import android.content.Intent;


import androidx.legacy.content.WakefulBroadcastReceiver;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Created by adrian on 14/02/16.
 */
public class ExternalStatusBroadcastReceiver extends WakefulBroadcastReceiver {


    private static final String TAG = ExternalStatusBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Pref.getBoolean("accept_external_status", true)) {
            startWakefulService(context, new Intent(context, ExternalStatusService.class)
                    .setAction(ExternalStatusService.ACTION_NEW_EXTERNAL_STATUSLINE)
                    .putExtras(intent));
        } else {
            UserError.Log.d(TAG, "Not accepting external status line due to preference switch");
        }
    }
}