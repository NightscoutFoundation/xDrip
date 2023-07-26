package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

// jamorham

public class ConfigReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        log("OnReceive enter");
        if ((intent == null) || (intent.getAction() == null)) {
            log("Intent or action null");
            return;
        }
        switch (intent.getAction()) {
            case "com.gmail.jamorham.NEW_CONFIG":

                // strings
                String key = intent.getStringExtra("prefstr");
                if (key != null) {
                    final String value = intent.getStringExtra("prefval");
                    if (value != null) {
                        log("Setting string: " + key + " to " + value);
                        Pref.setString(key, value);
                    }
                }

                // boolean
                key = intent.getStringExtra("prefbool");
                if (key != null) {
                    final String value = intent.getStringExtra("prefval");
                    if (value != null) {
                        log("Setting boolean: " + key + " to " + value);
                        Pref.setBoolean(key, value.equals("true"));
                    }
                }

                Sensor.createDefaultIfMissing();
                CollectionServiceStarter.restartCollectionServiceBackground();
                break;
            case "android.intent.action.BOOT_COMPLETED":
                log("Received boot complete");
                CollectionServiceStarter.restartCollectionServiceBackground();
                break;
            default:
                log("Wrong action");
                break;
        }
    }

    private static void log(String msg) {
        //android.util.Log.d("ConfigReceiver", msg);
    }
}
