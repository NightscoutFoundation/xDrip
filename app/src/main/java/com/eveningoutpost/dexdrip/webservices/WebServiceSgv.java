package com.eveningoutpost.dexdrip.webservices;

import android.support.annotation.NonNull;
import android.util.Log;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.eveningoutpost.dexdrip.Models.UserError;
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
    private static volatile WebServiceSgv instance;

    @NonNull
    public static WebServiceSgv getInstance() {
        if (instance == null) {
            instance = new WebServiceSgv();
        }
        return instance;
    }

    // process the request and produce a response object
    public WebResponse request(String query) {

        int steps_result_code = 0; // result code for any steps cgi parameters, 200 = good

        final Map<String, String> cgi = getQueryParameters(query);

        if (cgi.containsKey("steps")) {
            UserError.Log.d(TAG, "Received steps request: " + cgi.get("steps"));
            // forward steps request to steps route
            final WebResponse steps_reply_wr = RouteFinder.handleRoute("steps/set/" + cgi.get("steps"));
            steps_result_code = steps_reply_wr.resultCode;
        }

        final JSONArray reply = new JSONArray();
        final List<BgReading> readings = BgReading.latest(24);
        // populate json structures
        try {

            final String collector_device = DexCollectionType.getBestCollectorHardwareName();
            String external_status_line = getLastStatusLine();

            // for each reading produce a json record
            for (BgReading reading : readings) {
                final JSONObject item = new JSONObject();
                item.put("_id", reading.uuid);
                item.put("device", collector_device);
                item.put("date", reading.timestamp);
                item.put("dateString", DateUtil.toNightscoutFormat(reading.timestamp));
                item.put("sysTime", DateUtil.toNightscoutFormat(reading.timestamp));
                item.put("sgv", (int) reading.getDg_mgdl());
                item.put("delta", new BigDecimal(reading.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
                item.put("direction", reading.getDg_deltaName());
                item.put("noise", reading.noiseValue());
                item.put("filtered", (long) (reading.filtered_data * 1000));
                item.put("unfiltered", (long) (reading.raw_data * 1000));
                item.put("rssi", 100);
                item.put("type", "sgv");

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

                reply.put(item);
            }

            Log.d(TAG, "Output: " + reply.toString());
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }
        return new WebResponse(reply.toString());
    }


}
