package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.models.UserError;

/**
 * jamorham
 *
 * Broadcast intent snooze receiver
 *
 * Sending application must place its package id in the EXTRA_SENDER
 *
 */

public class BroadcastSnoozeReceiver extends BroadcastReceiver {

    private static final String TAG = "BroadcastSnoozeReceiver";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        if (intent.getAction().equals(Intents.ACTION_SNOOZE)) {
            final String sender = intent.getStringExtra(Intents.EXTRA_SENDER);
            if (sender == null || sender.equals(BuildConfig.APPLICATION_ID)) {
                UserError.Log.d(TAG, "Ignoring broadcast we probably sent or is invalid");
                return;
            }
            if (Pref.getBooleanDefaultFalse("accept_broadcast_snooze")) {
                UserError.Log.uel(TAG, "Received snooze from: " + sender);
                AlertPlayer.getPlayer().OpportunisticSnooze();
            } else {
                UserError.Log.d(TAG, "Received a locally broadcast snooze but we don't accept it: " + sender);
            }
        }
    }
}
