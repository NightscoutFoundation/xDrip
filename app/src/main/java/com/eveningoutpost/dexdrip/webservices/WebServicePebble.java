package com.eveningoutpost.dexdrip.webservices;

import android.content.Context;
import android.app.Activity;
import android.util.Log;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.dagger.Injectors;
import com.eveningoutpost.dexdrip.ui.MicroStatus;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

/**
 * Created by jamorham on 06/01/2018.
 * <p>
 * emulates the Nightscout /pebble endpoint
 * <p>
 * we use the mgdl/mmol setting from preferences and ignore any query string
 * <p>
 * Set the data endpoint on Pebble Nightscout watchface to: http://127.0.0.1:17580/pebble
 */

public class WebServicePebble extends BaseWebService {

    private static String TAG = "WebServicePebble";

    @SuppressWarnings("WeakerAccess")
    @Inject
    MicroStatus microStatus;

    WebServicePebble() {
        Injectors.getMicroStatusComponent().inject(this);
    }

    // process the request and produce a response object
    public WebResponse request(String query) {

        final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        if (dg == null) return null; // TODO better error handling?

        final BgReading bgReading = BgReading.last();
        if (bgReading == null) return null; // TODO better error handling?

        final Calibration calibration = Calibration.lastValid(); // this can be null

        // prepare json objects
        final JSONObject reply = new JSONObject();

        final JSONObject status = new JSONObject();
        final JSONObject bgs = new JSONObject();
        final JSONObject cals = new JSONObject();

        final JSONArray status_array = new JSONArray();
        final JSONArray bgs_array = new JSONArray();
        final JSONArray cals_array = new JSONArray();

        // populate json structures
        try {

            status.put("now", JoH.tsl());

            bgs.put("sgv", dg.unitized);
            bgs.put("trend", bgReading.getSlopeOrdinal()); // beware not coming from Display Glucose
            bgs.put("direction", dg.delta_name);
            bgs.put("datetime", dg.timestamp);
            bgs.put("filtered", (long) (bgReading.filtered_data * 1000));
            bgs.put("unfiltered", (long) (bgReading.raw_data * 1000));

            bgs.put("noise", bgReading.noiseValue());

            // apparently curious way to differentiate between mgdl/mmol on the watch face
            if (dg.doMgDl) {
                bgs.put("bgdelta", (long) dg.delta_mgdl);
            } else {
                bgs.put("bgdelta", dg.unitized_delta_no_units.replaceFirst("\\+", ""));
            }

            bgs.put("battery", microStatus.gs("bestBridgeBattery"));
            if (!Pref.getBooleanDefaultFalse("enable_iob_in_api_endpoint")) {
                bgs.put("iob", 0.0);
            } else {
                Double iob = Treatments.getCurrentIoB();
                bgs.put("iob", (iob == null) ? "unknown" : String.format("%.02f", iob));
            }
            // TODO output bwp and bwpo

            status_array.put(status);
            bgs_array.put(bgs);

            reply.put("status", status_array);
            reply.put("bgs", bgs_array);

            // optional calibration
            if (calibration != null) {
                cals.put("scale", 1);
                cals.put("slope", calibration.slope * 1000);
                cals.put("intercept", calibration.intercept * 1000); // negated??
                cals_array.put(cals);
                reply.put("cals", cals_array);

            }
            Log.d(TAG, "Output: " + reply.toString());

        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }
        // {"status":[{"now":1515263236782}],"bgs":[{"sgv":"16.8","trend":3,"direction":"FortyFiveUp","datetime":1515263092650,"filtered":292672,"unfiltered":296384,"noise":1,"bgdelta":"0.3","battery":"72","iob":0,"bwp":"29.50","bwpo":303}],"cals":[{"slope":1000,"intercept":13056.86000000003,"scale":1}]}

        return new WebResponse(reply.toString());
    }


}
