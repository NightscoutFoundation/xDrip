package com.eveningoutpost.dexdrip.receiver;

import android.content.BroadcastReceiver;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.Reminders;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String TAG = ReminderReceiver.class.getSimpleName();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final String action = intent.getAction();
        if (action == null) {
            UserError.Log.e(TAG, "Null action intent received");
            return;
        }
        if (action.equals(Reminders.REMINDER_ACTION)) {
            final Bundle bundle = intent.getExtras();
            new Reminders().processIncomingBundle(bundle);
        }
    }
}
