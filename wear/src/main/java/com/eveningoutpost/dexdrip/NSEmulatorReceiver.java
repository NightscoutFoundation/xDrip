
package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertFromJson;

/**
 * Created by jamorham on 14/11/2016.
 *
 * NOTE THIS IS THE REDUCED WEAR OS VERSION
 *
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



                        if (action == null) return;

                        switch (action) {
                            case Intents.XDRIP_PLUS_NS_EMULATOR:

                                // in future this could have its own data source perhaps instead of follower
                                if (!Home.get_follower() && DexCollectionType.getDexCollectionType() != DexCollectionType.NSEmulator &&
                                    !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) { //???DexCollectionType
                                    Log.e(TAG, "Received NSEmulator data but we are not a follower or emulator receiver");
                                    return;
                                }

                                if (!Home.get_follower()) {
                                    // must be NSEmulator here ???? Not true anymore.
                                    if (!Sensor.isActive()) {
                                        // warn about problems running without a sensor record
                                        Home.toaststaticnext("Please use: Start Sensor from the menu for best results!");
                                    }
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
                                                if(json_array.length() >= 1) {
                                                    LibreOOPAlgorithm.HandleData(json_array.getString(1));
                                                } else {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    final String type = json_object.getString("type");
                                                    switch (type) {
                                                        case "sgv":
                                                            bgReadingInsertFromData(json_object.getLong("date"), json_object.getDouble("sgv"), true);

                                                            break;
                                                        default:
                                                            Log.e(TAG, "Unknown entries type: " + type);
                                                    }
                                                }


                                            } catch (JSONException e) {
                                                Log.e(TAG, "Got JSON exception: " + e);
                                            }

                                        }
                                        break;

                                    // device status removed on wear

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
    static public void bgReadingInsertFromData(long timestamp, double sgv, boolean do_notification) {
        Log.e(TAG, "bgReadingInsertFromData called timestamp = " + timestamp+ " bg = " + sgv + " time =" +  JoH.dateTimeText(timestamp));
        JSONObject faux_bgr = new JSONObject();
        try {
        faux_bgr.put("timestamp", timestamp);
        faux_bgr.put("calculated_value", sgv);
        faux_bgr.put("filtered_calculated_value", sgv);
        faux_bgr.put("source_info", "NSEmulator Follow");
        // sanity checking???
        // fake up some extra data
        faux_bgr.put("raw_data", sgv);
        faux_bgr.put("age_adjusted_raw_value", sgv);
        faux_bgr.put("filtered_data", sgv);
        
            faux_bgr.put("uuid", UUID.randomUUID().toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            Log.e(TAG, "Got JSON exception: " + e);
            return;
        }
         
        Log.d(TAG, "Received NSEmulator SGV: " + faux_bgr);
        bgReadingInsertFromJson(faux_bgr.toString(), do_notification, true); // notify and force sensor
    }
}
