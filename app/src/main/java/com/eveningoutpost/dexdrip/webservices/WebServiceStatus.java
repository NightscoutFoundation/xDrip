package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONException;
import org.json.JSONObject;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantParseDouble;

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
            // "settings":{"units":"mmol"}
            final JSONObject settings = new JSONObject();
            final boolean using_mgdl = Pref.getString("units", "mgdl").equals("mgdl");
            settings.put("units", using_mgdl ? "mg/dl" : "mmol");

            // thresholds":{"bgHigh":260,"bgTargetTop":180,"bgTargetBottom":80,"bgLow":55}
            double highMark = tolerantParseDouble(Pref.getString("highValue", "170"), 170d);
            double lowMark = tolerantParseDouble(Pref.getString("lowValue", "70"), 70d);

            if (!using_mgdl) {
                // if we're using mmol then the marks will be in mmol but should be expressed in mgdl
                // to be in line with how Nightscout presents data
                highMark = JoH.roundDouble(highMark * Constants.MMOLL_TO_MGDL, 0);
                lowMark = JoH.roundDouble(lowMark * Constants.MMOLL_TO_MGDL, 0);
            }

            final JSONObject thresholds = new JSONObject();
            thresholds.put("bgHigh", highMark);
            thresholds.put("bgLow", lowMark);

            settings.put("thresholds", thresholds);

            reply.put("settings", settings);

            Log.d(TAG, "Output: " + reply.toString());
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }

        return new WebResponse(reply.toString());
    }


}
