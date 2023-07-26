package com.eveningoutpost.dexdrip.alert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.LinkedList;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Encapsulated poller providing external methods for trigger events
 * <p>
 * Handles its own trigger for screen on events
 */

public class Poller {

    private static final String TAG = "AlertSchedule";

    public static void chargerConnectedDisconnected() {
        poll(Pollable.When.ChargeChange);
    }

    public static void screenOn() {
        poll(Pollable.When.ScreenOn);
    }

    public static void reading() {
        poll(Pollable.When.Reading);
    }

    public static void hour() {
        poll(Pollable.When.Hour);
    }

    // call from application start
    public static void init() {
        try {
            val intentFilter = new IntentFilter();
            intentFilter.addAction(Intent.ACTION_SCREEN_ON);

            try {
                xdrip.getAppContext().unregisterReceiver(screenReceiver);
            } catch (Exception e) {
                //
            }
            xdrip.getAppContext().registerReceiver(screenReceiver, intentFilter);
        } catch (Exception e) {
            Log.wtf(TAG, "Exception in init: " + e);
        }
    }

    private static void poll(final Pollable.When event) {
        Log.d(TAG, "DEBUG POLL: " + event);
        val remove = new LinkedList<Pollable>();
        val triggeredGroups = new LinkedList<String>();
        val registry = Registry.getRegistry();
        for (val alert : registry) {
            if (triggeredGroups.contains(alert.group())) {
                Log.d(TAG, "Skipping due to group match: " + alert.group());
                continue;
            }
            val result = alert.poll(event);
            if (result.remove) {
                remove.add(alert); // this one asked to be removed
            }
            if (result.triggered) {
                triggeredGroups.add(alert.group());
            }
        }
        for (val alert : remove) {
            Registry.remove(alert);
        }
    }

    private static final BroadcastReceiver screenReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) return;
            switch (action) {
                case Intent.ACTION_SCREEN_ON:
                    Inevitable.task("screen-on-poll", 6000, () -> screenOn());
                    break;
            }
        }
    };

}
