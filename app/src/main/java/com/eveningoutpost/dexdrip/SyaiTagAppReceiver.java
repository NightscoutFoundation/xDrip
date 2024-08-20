
package com.eveningoutpost.dexdrip;

import static com.eveningoutpost.dexdrip.models.BgReading.bgReadingInsertFromJson;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by jamorham on 14/11/2016.
 */

public class SyaiTagAppReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham syai tag";
    private static final boolean debug = false;
    private static final boolean d = false;
    private static SharedPreferences prefs;
    private static final Object lock = new Object();

    @Override
    public void onReceive(final Context context, final Intent intent) {
        new Thread() {
            @Override
            public void run() {
                PowerManager.WakeLock wl = JoH.getWakeLock("SyaiTag-receiver", 60000);
                synchronized (lock) {
                    try {
                        Log.d(TAG, "SyaiTagApp onReceiver: " + intent.getAction());
                        JoH.benchmark(null);
                        // check source
                        if (prefs == null)
                            prefs = PreferenceManager.getDefaultSharedPreferences(context);

                        final Bundle bundle = intent.getExtras();
                        //  BundleScrubber.scrub(bundle);
                        final String action = intent.getAction();

                        if ((bundle != null) && (debug)) {
                            Log.d(TAG, "Action: " + action);
                            JoH.dumpBundle(bundle, TAG);
                        }
                        if (action == null) return;
                        switch (action) {
                            case Intents.XDRIP_PLUS_SYAI_TAG_APP:
                                // in future this could have its own data source perhaps instead of follower
                                if (!Home.get_follower() && DexCollectionType.getDexCollectionType() != DexCollectionType.SyaiTag &&
                                        !Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
                                    Log.e(TAG, "Received Syai Tag data but we are not a follower or emulator receiver");
                                    return;
                                }
                                if (!Home.get_follower()) {
                                    // must be syai tag here ???? Not true anymore.
                                    if (!Sensor.isActive()) {
                                        // warn about problems running without a sensor record
                                        Home.toaststaticnext("Please use: Start Sensor from the menu for best results!");
                                    }
                                }
                                if (bundle == null) break;
                                Log.d(TAG, "Receiving syai tag broadcast");
                                final String collection = bundle.getString("collection");
                                if (collection == null) return;
                                switch (collection) {
                                    case "entries":
                                        final String data = bundle.getString("data");
                                        if ((data != null) && (data.length() > 0)) {
                                            try {
                                                final JSONArray json_array = new JSONArray(data);
                                                // if this array is >1 in length then it is from OOP otherwise something like AAPS
                                                if (json_array.length() > 1) {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    int process_id = -1;
                                                    try {
                                                        process_id = json_object.getInt("ROW_ID");
                                                    } catch (JSONException e) {
                                                        // Intentionly ignoring ecxeption.
                                                    }
                                                    if (process_id == -1 || process_id == android.os.Process.myPid()) {
                                                        LibreOOPAlgorithm.handleData(json_array.getString(1));
                                                    } else {
                                                        Log.d(TAG, "Ignoring OOP result since process id is wrong " + process_id);
                                                    }

                                                } else {
                                                    final JSONObject json_object = json_array.getJSONObject(0);
                                                    final String type = json_object.getString("type");
                                                    switch (type) {
                                                        case "sgv":
                                                            double slope = 0;
                                                            try {
                                                                slope = BgReading.slopefromName(json_object.getString("direction"));
                                                            } catch (JSONException e) {
                                                                //
                                                            }
                                                            bgReadingInsertFromData(json_object.getLong("date"),
                                                                    json_object.getDouble("sgv"), slope, true);

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
                                    case "devicestatus":
                                        final String ddata = bundle.getString("data");

                                        if ((ddata != null) && (ddata.length() > 0)) {
                                            try {
                                                Log.d(TAG, "Got device status data: " + ddata);
                                                final JSONArray json_array = new JSONArray(ddata);
                                                final JSONObject json_object = json_array.getJSONObject(0);
                                                final JSONObject json_pump_object = json_object.getJSONObject("pump");

                                                try {
                                                    final double reservoir = json_pump_object.getDouble("reservoir");
                                                    PumpStatus.setReservoir(reservoir);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing reservoir: " + e);
                                                }

                                                try {
                                                    final JSONObject battery_object = json_pump_object.getJSONObject("battery");
                                                    final double battery_percent = battery_object.getDouble("percent");
                                                    PumpStatus.setBattery(battery_percent);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing battery: " + e);
                                                }

                                                try {
                                                    final JSONObject iob_object = json_pump_object.getJSONObject("iob");
                                                    final double bolus_iob = iob_object.getDouble("bolusiob");
                                                    PumpStatus.setBolusIoB(bolus_iob);

                                                } catch (JSONException e) {
                                                    Log.d(TAG, "Got exception when processing iob: " + e);
                                                }

                                            } catch (JSONException e) {
                                                Log.e(TAG, "Got JSON exception: " + e);
                                            } catch (Exception e) {
                                                Log.e(TAG, "Got processing exception: " + e);
                                            }
                                            PumpStatus.syncUpdate();
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

                    } catch (Exception e) {
                        Log.e(TAG, "Caught Exception handling intent", e );
                    }finally {
                        JoH.benchmark("syai tag process");
                        JoH.releaseWakeLock(wl);
                    }
                } // lock
            }
        }.start();
    }
    public static BgReading bgReadingInsertFromData(long timestamp, double sgv, double slope, boolean do_notification) {
        Log.d(TAG, "bgReadingInsertFromData called timestamp = " + timestamp + " bg = " + sgv + " time =" + JoH.dateTimeText(timestamp));
        final JSONObject faux_bgr = new JSONObject();
        try {
            faux_bgr.put("timestamp", timestamp);
            faux_bgr.put("calculated_value", sgv);
            faux_bgr.put("filtered_calculated_value", sgv);
            faux_bgr.put("calculated_value_slope", slope);
            faux_bgr.put("source_info", "Syai Tag Follow");
            // sanity checking???
            // fake up some extra data
            faux_bgr.put("raw_data", sgv);
            faux_bgr.put("age_adjusted_raw_value", sgv);
            faux_bgr.put("filtered_data", sgv);

            faux_bgr.put("uuid", UUID.randomUUID().toString());
        } catch (JSONException e) {
            Log.e(TAG, "bgReadingInsertFromData Got JSON exception: " + e);
            return null;
        }

        Log.d(TAG, "Received Syai Tag SGV: " + faux_bgr);
        Sensor.createDefaultIfMissing();
        return bgReadingInsertFromJson(faux_bgr.toString(), do_notification, true); // notify and force sensor
    }
}
