package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import org.json.JSONException;
import org.json.JSONObject;

import static com.eveningoutpost.dexdrip.Models.JoH.tolerantParseDouble;

/**
 * Created by jamorham on 04/02/2018.
 */

public class WebServiceStatus extends BaseWebService {

    private static String TAG = "WebServiceStatus";

    // process the request and produce a response object
    public WebResponse request(String query) {
        final JSONObject reply = new JSONObject();

        // populate json structures
        try {
            // thresholds":{"bgHigh":260,"bgTargetTop":180,"bgTargetBottom":80,"bgLow":55}
            double highMark = tolerantParseDouble(Pref.getString("highValue", "170"));
            double lowMark = tolerantParseDouble(Pref.getString("lowValue", "70"));

            final JSONObject thresholds = new JSONObject();
            thresholds.put("bgHigh", highMark);
            thresholds.put("bgLow", lowMark);

            reply.put("thresholds", thresholds);

            Log.d(TAG, "Output: " + reply.toString());
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }

        return new WebResponse(reply.toString());
    }


}
