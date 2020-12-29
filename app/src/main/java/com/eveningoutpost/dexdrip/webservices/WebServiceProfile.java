package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import static com.eveningoutpost.dexdrip.Models.JoH.tolerantParseDouble;

/**
 * @author James Woglom (j@wogloms.net)
 */

public class WebServiceProfile extends BaseWebService {

    private static String TAG = "WebServiceProfile";

    public WebResponse request(String query) {
        final JSONObject reply = new JSONObject();
        final JSONArray wrapper = new JSONArray();

        // populate json structures
        try {
            reply.put("defaultProfile", "Default");
            final JSONObject basal = new JSONObject();
            basal.put("time", "00:00");
            basal.put("value", "0.0");
            basal.put("timeAsSeconds", "0");

            final JSONArray basalArr = new JSONArray();
            basalArr.put(basal);
            final JSONObject defaultObj = new JSONObject();
            defaultObj.put("basal", basalArr);
            reply.put("store", defaultObj);
            /*
            "defaultProfile": "Default",
            "store": {
              "Default": {
                "basal": [
                  {
                    "time": "00:00",
                    "value": "0.0",
                    "timeAsSeconds": "0"
                  }
                ],
              }
            },
             */

            wrapper.put(reply);
            Log.d(TAG, "Output: " + wrapper.toString());
        } catch (JSONException e) {
            UserError.Log.wtf(TAG, "Got json exception: " + e);
        }

        return new WebResponse(wrapper.toString());
    }


}
