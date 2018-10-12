package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.dagger.Singleton;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLine;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLineTime;


/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * emulates the Nightscout /api/v1/entries/sgv.json endpoint at sgv.json
 * <p>
 * Always outputs 24 items and ignores any parameters
 * Always uses display glucose values
 * <p>
 */

public class WebServiceSgv extends BaseWebService {

    private static String TAG = "WebServiceSgv";


    // process the request and produce a response object
    public WebResponse request(String query) {

        int steps_result_code = 0; // result code for any steps cgi parameters, 200 = good
        int heart_result_code = 0; // result code for any heart cgi parameters, 200 = good
        int tasker_result_code = 0; // result code for any heart cgi parameters, 200 = good
        int units_indicator = 1; // show the units we are using
        boolean brief = false; // whether to cut out portions of the data

        final Map<String, String> cgi = getQueryParameters(query);

        int count = 24;

        if (cgi.containsKey("count")) {
            try {
                count = Integer.valueOf(cgi.get("count"));
                count = Math.min(count, 1000);
                count = Math.max(count, 1);
                UserError.Log.d(TAG, "SGV count request for: " + count + " entries");
            } catch (Exception e) {
                // meh
            }
        }

        if (cgi.containsKey("steps")) {
            UserError.Log.d(TAG, "Received steps request: " + cgi.get("steps"));
            // forward steps request to steps route
            final WebResponse steps_reply_wr = ((RouteFinder) Singleton.get("RouteFinder")).handleRoute("steps/set/" + cgi.get("steps"));
            steps_result_code = steps_reply_wr.resultCode;
        }

        if (cgi.containsKey("heart")) {
            UserError.Log.d(TAG, "Received heart request: " + cgi.get("heart"));
            // forward steps request to heart route
            final WebResponse heart_reply_wr = ((RouteFinder) Singleton.get("RouteFinder")).handleRoute("heart/set/" + cgi.get("heart") + "/1"); // accuracy currently ignored (always 1) - TODO review
            heart_result_code = heart_reply_wr.resultCode;
        }

        if (cgi.containsKey("tasker")) {
            UserError.Log.d(TAG, "Received tasker request: " + cgi.get("tasker"));
            // forward steps request to heart route
            final WebResponse tasker_reply_wr = ((RouteFinder) Singleton.get("RouteFinder")).handleRoute("tasker/" + cgi.get("tasker")); // send single word command to tasker, eg snooze or osnooze
            tasker_result_code = tasker_reply_wr.resultCode;
        }

        if (cgi.containsKey("brief_mode")) {
            brief = true;
        }


        final JSONArray reply = new JSONArray();

        // whether to include data which doesn't match the current sensor
        final boolean ignore_sensor = Home.get_follower() || cgi.containsKey("all_data");

        final List<BgReading> readings = BgReading.latest(count, ignore_sensor);
        if (readings != null) {
            // populate json structures
            try {

                final String collector_device = DexCollectionType.getBestCollectorHardwareName();
                String external_status_line = getLastStatusLine();

                // for each reading produce a json record
                for (BgReading reading : readings) {
                    final JSONObject item = new JSONObject();
                    if (!brief) {
                        item.put("_id", reading.uuid);
                        item.put("device", collector_device);
                        item.put("dateString", DateUtil.toNightscoutFormat(reading.timestamp));
                        item.put("sysTime", DateUtil.toNightscoutFormat(reading.timestamp));
                    }

                    item.put("date", reading.timestamp);
                    item.put("sgv", (int) reading.getDg_mgdl());
                    try {
                        item.put("delta", new BigDecimal(reading.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
                    } catch (NumberFormatException e) {
                        UserError.Log.e(TAG, "Could not pass delta to webservice as was invalid number");
                    }
                    item.put("direction", reading.getDg_deltaName());
                    item.put("noise", reading.noiseValue());

                    if (!brief) {
                        item.put("filtered", (long) (reading.filtered_data * 1000));
                        item.put("unfiltered", (long) (reading.raw_data * 1000));
                        item.put("rssi", 100);
                        item.put("type", "sgv");
                    }
                    if (units_indicator > 0) {
                        item.put("units_hint", Pref.getString("units", "mgdl").equals("mgdl") ? "mgdl" : "mmol");
                        units_indicator = 0;
                    }

                    // emit the external status line once if present
                    if (external_status_line.length() > 0) {
                        item.put("aaps", external_status_line);
                        item.put("aaps-ts", getLastStatusLineTime());
                        external_status_line = "";
                    }

                    // emit result code from steps if present
                    if (steps_result_code > 0) {
                        item.put("steps_result", steps_result_code);
                        steps_result_code = 0;
                    }

                    // emit result code from heart if present
                    if (heart_result_code > 0) {
                        item.put("heart_result", heart_result_code);
                        heart_result_code = 0;
                    }

                    // emit result code from tasker if present
                    if (tasker_result_code > 0) {
                        item.put("tasker_result", tasker_result_code);
                        tasker_result_code = 0;
                    }

                    reply.put(item);
                }

                Log.d(TAG, "Output: " + reply.toString());
            } catch (JSONException e) {
                UserError.Log.wtf(TAG, "Got json exception: " + e);
            }
        }

        // whether to send empty string instead of empty json array
        if (cgi.containsKey("no_empty") && reply.length() == 0) {
            return new WebResponse("");
        } else {
            return new WebResponse(reply.toString());
        }
    }


}
