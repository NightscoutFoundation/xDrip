package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Emulates the Nightscout /api/v1/treatments.json endpoint at treatments.json
 *
 * @author James Woglom (j@wogloms.net)
 */

public class WebServiceTreatments extends BaseWebService {

    private static final String TAG = "WebServiceTreatments";

    @VisibleForTesting
    static List<Treatments> cachedTreatments = null;

    public WebResponse request(String query) {
        final Map<String, String> cgi = getQueryParameters(query);
        int count = 24;

        if (cgi.containsKey("count")) {
            try {
                count = Integer.valueOf(cgi.get("count"));
                count = Math.min(count, 100);
                count = Math.max(count, 1);
                UserError.Log.d(TAG, "Treatment count request for: " + count + " entries");
            } catch (NumberFormatException e) {
                UserError.Log.w(TAG, "Invalid treatment count request for: " + count + " entries");
            }
        }

        // Store a cache of the last Treatments.latest() query for the duration in which there is no
        // new latest treatment. Since obtaining the latest treatment is fast, but a larger number of
        // treatments is significantly slower, this optimizes the most often use case.
        Treatments latestTreatment = null;
        List<Treatments> latestList = Treatments.latest(1);
        if (latestList != null && !latestList.isEmpty()) {
            latestTreatment = latestList.iterator().next();
        }

        // Check whether there are any cached treatments, and if so whether the latest cached
        // treatment matches the UUID of the most recent treatment. Use the cache if we can,
        // otherwise fill cachedTreatments with new treatments from the database.
        List<Treatments> treatments;
        if (cachedTreatments != null && cachedTreatments.size() > 0 && count <= cachedTreatments.size() &&
            latestTreatment != null && latestTreatment.uuid.equals(cachedTreatments.iterator().next().uuid)) {
            if (count == cachedTreatments.size()) {
                UserError.Log.d(TAG, "Using cached treatments");
                treatments = cachedTreatments;
            } else {
                UserError.Log.d(TAG, "Copying " + count + " of " + cachedTreatments.size() + " cached treatments");
                treatments = new ArrayList<>();
                Iterator<Treatments> it = cachedTreatments.iterator();
                for (int i=0; i<count; i++) {
                    treatments.add(it.next());
                }
            }
        } else {
            UserError.Log.d(TAG, "Fetching latest " + count + " entries from Treatments");
            treatments = Treatments.latest(count);
            cachedTreatments = treatments;
        }

        final JSONArray reply = new JSONArray();

        // populate json structures
        try {
            for (Treatments treatment : treatments) {
                final JSONObject item = new JSONObject();

                item.put("_id", treatment.uuid);
                item.put("created_at", treatment.timestamp);
                item.put("eventType", treatment.eventType);
                item.put("enteredBy", treatment.enteredBy);
                item.put("notes", treatment.notes);
                item.put("carbs", treatment.carbs);
                item.put("insulin", treatment.insulin);

                reply.put(item);
            }

            Log.d(TAG, "Treatment output: " + reply.toString());
        } catch (JSONException e) {
            UserError.Log.w(TAG, "Got json exception: " + e);
        }

        // whether to send empty string instead of empty json array
        if (cgi.containsKey("no_empty") && reply.length() == 0) {
            return new WebResponse("");
        } else {
            return new WebResponse(reply.toString());
        }
    }
}
