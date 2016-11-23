
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertFromJson;

/**
 * Created by jamorham on 14/11/2016.
 */

public class NSEmulatorReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham nsemulator";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static SharedPreferences prefs;
    private static final Object lock = new Object();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("nsemulator-receiver", 60000);
                synchronized (lock) {
                    try {

                        Log.d(TAG, "NSEmulator onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();


                        if ((bundle != null) && (debug)) {
                            for (String key : bundle.keySet()) {
                                Object value = bundle.get(key);
                                if (value != null) {
                                    Log.d(TAG, String.format("%s %s (%s)", key,
                                            value.toString(), value.getClass().getName()));
                                }
                            }
                        }

                        switch (action) {
                            case Intents.XDRIP_PLUS_NS_EMULATOR:

                                // in future this could have its own data source perhaps instead of follower
                                if (!Home.get_follower()) {
                                    Log.e(TAG, "Received NSEmulator data but we are not a follower");
                                    return;
                                }

                                if (bundle == null) break;

                                Log.d(TAG, "Receiving NSEmulator broadcast");

                                final String collection = bundle.getString("collection");
                                if (collection == null) return;

                                switch (collection) {

                                    case "entries":
                                        final String data = bundle.getString("data");

                                        if ((data != null) && (data.length() > 0)) {
                                            try {
                                                final JSONArray json_array = new JSONArray(data);
                                                final JSONObject json_object = json_array.getJSONObject(0);
                                                final String type = json_object.getString("type");
                                                switch (type) {
                                                    case "sgv":
                                                        JSONObject faux_bgr = new JSONObject();
                                                        faux_bgr.put("timestamp", json_object.getLong("date"));
                                                        faux_bgr.put("calculated_value", json_object.getDouble("sgv"));
                                                        faux_bgr.put("filtered_calculated_value", json_object.getDouble("sgv"));
                                                        // sanity checking???
                                                        // fake up some extra data
                                                        faux_bgr.put("raw_data", json_object.getDouble("sgv"));
                                                        faux_bgr.put("filtered_data", json_object.getDouble("sgv"));

                                                        Log.d(TAG, "Received NSEmulator SGV: " + faux_bgr);
                                                        bgReadingInsertFromJson(faux_bgr.toString());
                                                        break;
                                                    default:
                                                        Log.e(TAG, "Unknown entries type: " + type);
                                                }


                                            } catch (JSONException e) {
                                                Log.e(TAG, "Got JSON exception: " + e);
                                            }

                                        }
                                        break;
                                    default:
                                        Log.d(TAG, "Unprocessed collection: " + collection);

                                }

                                break;

                            default:
                                Log.e(TAG, "Unknown action! " + action);
                                break;
                        }
                    } finally {
                        JoH.benchmark("NSEmulator process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }

}
