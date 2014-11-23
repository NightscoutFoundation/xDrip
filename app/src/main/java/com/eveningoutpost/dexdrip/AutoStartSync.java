package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by stephenblack on 11/8/14.
 */
public class AutoStartSync extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, SyncService.class));
    }
}