package com.eveningoutpost.dexdrip.wearintegration;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import com.eveningoutpost.dexdrip.Models.UserError;

/**
 * Created by adrian on 14/02/16.
 */
public class ExternalStatusBroadcastReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        startWakefulService(context, new Intent(context, ExternalStatusService.class)
                .setAction(ExternalStatusService.ACTION_NEW_EXTERNAL_STATUSLINE)
                .putExtras(intent));
    }
}