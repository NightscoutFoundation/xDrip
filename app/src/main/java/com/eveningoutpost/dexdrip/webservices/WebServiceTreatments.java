package com.eveningoutpost.dexdrip.webservices;

import android.util.Log;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.DateUtil;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.NanoStatus;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.SensorStatus;
import com.eveningoutpost.dexdrip.dagger.Singleton;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLine;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLineTime;


/**
 * Created by jwoglom
 * <p>
 * emulates the Nightscout /api/v1/treatments.json endpoint at treatments.json
 * <p>
 */

public class WebServiceTreatments extends BaseWebService {

    private static String TAG = "WebServiceTreatments";

    private static List<Treatments> cachedTreatments = null;


    public WebResponse request(String query) {

        final Map<String, String> cgi = getQueryParameters(query);

        int count = 24;

        if (cgi.containsKey("count")) {
            try {
                count = Integer.valueOf(cgi.get("count"));
                count = Math.min(count, 100);
                count = Math.max(count, 1);
                UserError.Log.d(TAG, "Treatment count request for: " + count + " entries");
            } catch (Exception e) {
                // meh
            }
        }

        final JSONArray reply = new JSONArray();


        // Store a cache of the last Treatments.latest() query for the duration in which there is no
        // new latest treatment. Since obtaining the latest treatment is fast, but a larger number of
        // treatments is significantly slower, this optimizes the most often use case.
        Iterator<Treatments> it = Treatments.latest(1).iterator();
        Treatments latestTreatment = it.hasNext() ? it.next() : null;

        List<Treatments> treatments;
        if (this.cachedTreatments != null && latestTreatment != null &&
            this.cachedTreatments.size() > 0 && latestTreatment.uuid.equals(this.cachedTreatments.iterator().next().uuid) &&
            count <= this.cachedTreatments.size())
        {
            if (count == this.cachedTreatments.size()) {
                UserError.Log.d(TAG, "Using cached treatments");
                treatments = this.cachedTreatments;
            } else {
                UserError.Log.d(TAG, "Copying "+count+" of "+this.cachedTreatments.size()+" cached treatments");
                treatments = new ArrayList<>();
                it = this.cachedTreatments.iterator();
                for (int i=0; i<count; i++) {
                    treatments.add(it.next());
                }
            }
        } else {
            UserError.Log.d(TAG, "Fetching latest "+count+" entries from Treatments");
            treatments = Treatments.latest(count);
            this.cachedTreatments = treatments;
        }

        if (treatments != null) {
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
