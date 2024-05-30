package com.eveningoutpost.dexdrip.webservices;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.NanoStatus;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.SensorStatus;
import com.eveningoutpost.dexdrip.dagger.Singleton;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

    private static final String TAG = "WebServiceSgv";

    @VisibleForTesting
    static List<BgReading> cachedReadings = null;

    @VisibleForTesting
    static final Map<String, JSONObject> cachedJson = new LinkedHashMap<String, JSONObject>() {
        // Prevent the cache from growing too large. 1000 entries is about 3.5 days
        @Override
        protected boolean removeEldestEntry(Entry eldest) {
            return size() > 1000;
        }
    };

    // process the request and produce a response object
    public WebResponse request(String query) {
        int steps_result_code = 0; // result code for any steps cgi parameters, 200 = good
        int heart_result_code = 0; // result code for any heart cgi parameters, 200 = good
        int tasker_result_code = 0; // result code for any heart cgi parameters, 200 = good
        int units_indicator = 1; // show the units we are using
        String collector_status_string = null; // result of collector status
        String sensor_status_string = null; // result of collector status
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

        if (cgi.containsKey("collector")) {
            UserError.Log.d(TAG,"Received collector status request");
            collector_status_string = NanoStatus.nanoStatus("collector");
        }

        if (cgi.containsKey("sensor")) {
            UserError.Log.d(TAG,"Received sensor status request");
            sensor_status_string = SensorStatus.status();
        }

        if (cgi.containsKey("test_trigger_exception")) {
            throw new RuntimeException("This is a test exception");
        }

        if (cgi.containsKey("brief_mode")) {
            brief = true;
        }

        final JSONArray reply = new JSONArray();

        // whether to include data which doesn't match the current sensor
        final boolean ignore_sensor = Home.get_follower() || cgi.containsKey("all_data");

        // Store a cache of the last BgReading.latest() query for the duration in which there is no
        // new latest reading. Since obtaining the latest reading is fast, but a larger number of
        // readings is significantly slower, this optimizes the most often use case.
        List<BgReading> bgr = BgReading.latest(1, ignore_sensor);
        BgReading latestReading = null;
        if (bgr != null && bgr.size() > 0) {
            latestReading = bgr.iterator().next();
        }

        List<BgReading> readings;
        if (cachedReadings != null && cachedReadings.size() > 0 && count <= cachedReadings.size() &&
            latestReading != null && latestReading.uuid.equals(cachedReadings.iterator().next().uuid)) {
            if (count == cachedReadings.size()) {
                UserError.Log.d(TAG, "Using cached readings");
                readings = cachedReadings;
            } else {
                UserError.Log.d(TAG, "Copying " + count + " of " + cachedReadings.size() + " cached readings");
                readings = new ArrayList<>();
                Iterator<BgReading> it = cachedReadings.iterator();
                for (int i=0; i<count; i++) {
                    readings.add(it.next());
                }
            }
        } else {
            UserError.Log.d(TAG, "Fetching latest " + count + " readings from BgReading");
            if (brief) {
                // TODO this de-dupe period calculation should move in to DexCollectionType once a suitable method is available.
                readings = BgReading.latestDeduplicateToPeriod(count, ignore_sensor, BgGraphBuilder.DEXCOM_PERIOD - BgGraphBuilder.DEXCOM_PERIOD / 6);
            } else {
                readings = BgReading.latest(count, ignore_sensor);
            }
            cachedReadings = readings;
        }

        if (readings != null) {
            int withCache = 0;
            int withManual = 0;
            // populate json structures
            try {
                final String collector_device = DexCollectionType.getBestCollectorHardwareName();

                // for each reading produce a json record
                for (BgReading reading : readings) {
                    JSONObject item;
                    if (!brief && cachedJson.containsKey(reading.uuid)) {
                        reply.put(cachedJson.get(reading.uuid));
                        withCache++;
                        continue;
                    }

                    item = new JSONObject();
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

                    if (!brief) {
                        cachedJson.put(reading.uuid, item);
                    }
                    withManual++;
                    reply.put(item);
                }

                UserError.Log.d(TAG, "Processed "+withCache+" cached entries and "+withManual+" manual entries");
                UserError.Log.d(TAG, "Output: " + reply.toString());
            } catch (JSONException e) {
                UserError.Log.wtf(TAG, "Got json exception: " + e);
            }
        }

        // Add special fields for first SGV entry
        if (reply.length() > 0) {
            try {
                // Copy the first item only to a new JSONObject as to not
                // add these items to the cached json object
                JSONObject origItem = reply.getJSONObject(0);
                JSONObject item = new JSONObject(origItem.toString());
                if (units_indicator > 0) {
                    String unitsHint = Pref.getString("units", "mgdl").equals("mgdl") ? "mgdl" : "mmol";
                    item.put("units_hint", unitsHint);
                }

                // emit the external status line once if present
                String external_status_line = getLastStatusLine();
                if (external_status_line.length() > 0) {
                    item.put("aaps", external_status_line);
                    item.put("aaps-ts", getLastStatusLineTime());
                }

                // emit result code from steps if present
                if (steps_result_code > 0) {
                    item.put("steps_result", steps_result_code);
                }

                // emit result code from heart if present
                if (heart_result_code > 0) {
                    item.put("heart_result", heart_result_code);
                }

                // emit result code from tasker if present
                if (tasker_result_code > 0) {
                    item.put("tasker_result", tasker_result_code);
                }

                // emit nano-status string if present
                if (collector_status_string != null) {
                    item.put("collector_status", collector_status_string);
                }

                // TODO uploader battery

                // emit sensor status/age message if present
                if (sensor_status_string != null) {
                    item.put("sensor_status", sensor_status_string);
                }
                reply.put(0, item);
            } catch (JSONException e) {
                e.printStackTrace();
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
