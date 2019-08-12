package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.UUID;
import static com.eveningoutpost.dexdrip.xdrip.gs;


/**
 * Created by jamorham on 23/02/2016.
 */
public class NSClientReceiver extends BroadcastReceiver {

    private static final String TAG = "jamorham nsreceiver";
    private static final boolean debug = false;
    private static SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "NSRECEIVER onReceiver: " + intent.getAction());

        // check source
        if (prefs == null) prefs = PreferenceManager.getDefaultSharedPreferences(context);

        final Bundle bundle = intent.getExtras();
        //  BundleScrubber.scrub(bundle);
        final String action = intent.getAction();

        if ((bundle != null) && (debug)) {
            UserError.Log.d(TAG, "Action: " + action);
            JoH.dumpBundle(bundle, TAG);
        }

        if (action == null) return;

        switch (action) {
            case Intents.ACTION_NEW_SGV:
                if (Home.get_follower() && prefs.getBoolean("accept_nsclient_sgv", true)) {
                    if (bundle == null) break;
                    final String sgvs_json = bundle.getString("sgvs", "");
                    if (sgvs_json.length() > 0) {
                        try {
                            final JSONArray jsonArray = new JSONArray(sgvs_json);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                process_SGV_json(jsonArray.getString(i));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Json exception with sgvs: " + e.toString());
                        }
                    }
                    final String sgv_json = bundle.getString("sgv", "");
                    if (sgv_json.length() > 0) {
                        process_SGV_json(sgv_json);
                    }
                } else {
                    Log.d(TAG, "Ignoring SGV data as we are not a follower");
                }
                break;

            case Intents.ACTION_NEW_TREATMENT:
                if (bundle == null) break;
                if (prefs.getBoolean("accept_nsclient_treatments", true)) {
                    final String treatment_json = bundle.getString("treatment", "");
                    if (treatment_json.length() > 0) {
                        process_TREATMENT_json(treatment_json);
                    }
                    final String treatments_json = bundle.getString("treatments", "");
                    if (treatments_json.length() > 0) {
                        try {
                            final JSONArray jsonArray = new JSONArray(treatments_json);
                            for (int i = 0; i < jsonArray.length(); i++) {
                                process_TREATMENT_json(jsonArray.getString(i));
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "Json exception with sgvs: " + e.toString());
                        }
                    }
                } else {
                    Log.d(TAG, "Ignoring nsclient treatment data due to preference");
                }
                break;


            case Intents.ACTION_REMOTE_CALIBRATION:
                if (bundle == null) break;

                // calibration value is in local units, whether you're using mmol or mgdl then it should be as the user would type it
                //
                // calibration timestamp is ms since epoch standard

                // if you specify the units field as either mgdl or mmol then conversion will be done if local units don't match

                if (Pref.getBooleanDefaultFalse("accept_broadcast_calibrations")) {

                    final long calibration_timestamp = bundle.getLong("timestamp", -1);
                    double glucose_number = bundle.getDouble("glucose_number", -1);
                    final String units = bundle.getString("units","");

                    final long timeoffset = JoH.tsl() - calibration_timestamp;

                    if (glucose_number > 0) {

                        if (timeoffset < 0) {
                            Home.toaststaticnext(gs(R.string.got_calibration_in_the_future__cannot_process));
                            break;
                        }

                        final String local_units = Pref.getString("units", "mgdl");
                        if (units.equals("mgdl") && (!local_units.equals("mgdl"))) {
                            glucose_number = glucose_number * Constants.MGDL_TO_MMOLL;
                            Log.d(TAG, "Converting from mgdl to mmol: " + JoH.qs(glucose_number, 2));
                        } else if (units.equals("mmol") && (!local_units.equals("mmol"))) {
                            glucose_number = glucose_number * Constants.MMOLL_TO_MGDL;
                            Log.d(TAG, "Converting from mmol to mgdl: " + JoH.qs(glucose_number, 2));
                        }

                        UserError.Log.ueh(TAG, "Processing broadcasted calibration: " + JoH.qs(glucose_number, 2) + " offset ms: " + JoH.qs(timeoffset, 0));
                        final Intent calintent = new Intent(xdrip.getAppContext(), AddCalibration.class);
                        calintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        calintent.putExtra("timestamp",JoH.tsl());
                        calintent.putExtra("bg_string", JoH.qs(glucose_number));
                        calintent.putExtra("bg_age", Long.toString(timeoffset / 1000));
                        calintent.putExtra("allow_undo", "true");
                        calintent.putExtra("note_only", "false");
                        calintent.putExtra("cal_source", "NSClientReceiver");
                        Home.startIntentThreadWithDelayedRefresh(calintent);
                    } else {
                        Log.e(TAG,"Received broadcast calibration without glucose number");
                    }

                } else {
                    Log.e(TAG, "Received broadcast calibration, but inter-app preference is set to ignore");
                }
                break;

            default:
                Log.e(TAG, "Unknown action! " + action);
                break;
        }
    }

    public static void testCalibration() {
        Bundle bundle = new Bundle();
        bundle.putDouble("glucose_number", 5.5d+ (JoH.ts() % 100)/100d); // format is local format, in this example 5.5 mmol/l or specify the units as shown below
        bundle.putString("units", "mmol"); // mgdl or mmol
        bundle.putLong("timestamp", JoH.tsl()-10000);
        Intent intent = new Intent(Intents.ACTION_REMOTE_CALIBRATION);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }

    private void process_TREATMENT_json(String treatment_json) {
        try {
            Log.i(TAG, "Processing treatment from NS: "+treatment_json);
            Treatments.pushTreatmentFromJson(toTreatmentJSON(JoH.JsonStringtoMap(treatment_json)), true); // warning marked as from interactive - watch out for feedback loops
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing treatment from NS client " + e.toString());
        }
    }

    private void process_SGV_json(String sgv_json) {
        if (sgv_json == null) {
            Log.e(TAG, "SGV json is null!");
            return;
        }
        final HashMap<String, Object> sgv_map = JoH.JsonStringtoMap(sgv_json);
        //  if (prefs.getString("dex_collection_method", "").equals("Follower")) {
        if (sgv_map == null) {
            Log.e(TAG, "SGV map results in null!");
            return;
        }
        BgReading.bgReadingInsertFromJson(toBgReadingJSON(sgv_map));
        //  } else {
        //      Log.i(TAG, "Received nightscout SGV intent but we are not a follower");
        //  }
    }

    private String toBgReadingJSON(HashMap<String, Object> sgv_map) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("uuid", UUID.randomUUID().toString());

            jsonObject.put("timestamp", sgv_map.get("mills"));
            jsonObject.put("calculated_value", sgv_map.get("mgdl"));

            try {
                double myslope = (double) sgv_map.get("unfiltered") / (double) sgv_map.get("mgdl");
                double filtered_calculated_value = (double) sgv_map.get("filtered") / myslope;
                jsonObject.put("filtered_calculated_value", filtered_calculated_value);
            } catch (NullPointerException e) {
                Log.i(TAG, "Cannot calculate raw slope due to null pointer on unfiltered?");
                jsonObject.put("filtered_calculated_value", sgv_map.get("mgdl")); // use mgdl
            }

            //jsonObject.put("calibration_flag", calibration_flag);
            jsonObject.put("filtered_data", (sgv_map.get("filtered") != null) ? (((double) sgv_map.get("filtered")) / 1000) : 1);
            //jsonObject.put("raw_calculated", raw_calculated);
            jsonObject.put("raw_data", (sgv_map.get("unfiltered") != null) ? (((double) sgv_map.get("unfiltered")) / 1000) : 1);
            Double slope = BgReading.slopefromName(sgv_map.get("direction").toString());
            jsonObject.put("calculated_value_slope", slope);
            if (BgReading.isSlopeNameInvalid(sgv_map.get("direction").toString())) {
                jsonObject.put("hide_slope", true);
            }
            jsonObject.put("noise", sgv_map.get("noise"));
            //if (d) Log.d(TAG, "deebug: " + jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException | NullPointerException e) {
            Log.e(TAG, "JSON or Null Exception in toBgReadingJSON: " + e);
            return "";
        }
    }


    private String toTreatmentJSON(HashMap<String, Object> trt_map) {
        JSONObject jsonObject = new JSONObject();
        try {
            // jsonObject.put("uuid", UUID.fromString(trt_map.get("_id").toString()).toString());

            jsonObject.put("timestamp", trt_map.get("mills"));
            jsonObject.put("eventType", trt_map.get("eventType"));
            jsonObject.put("enteredBy", trt_map.get("enteredBy"));
            if (trt_map.containsKey("carbs")) {
                jsonObject.put("carbs", trt_map.get("carbs"));
            }
            if (trt_map.containsKey("insulin")) {
                jsonObject.put("insulin", trt_map.get("insulin"));
            }
            if (trt_map.containsKey("notes")) {
                jsonObject.put("notes", trt_map.get("notes"));
            }
            //double mgdl = trt_map.get("mgdl"); // bg reading

            jsonObject.put("created_at", trt_map.get("created_at"));
            return jsonObject.toString();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return "";
        }
    }
}


