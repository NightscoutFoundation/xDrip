package com.eveningoutpost.dexdrip.deposit;

import com.eveningoutpost.dexdrip.Models.BloodTest;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.profileeditor.BasalProfile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;

import lombok.val;

// jamorham

public class TreatmentsToJson {

    private static final String TAG = TreatmentsToJson.class.getSimpleName();

    static JSONArray getJsonForStartEnd(final long start, final long end) {
        val treatments = Treatments.latestForGraph(50000, start, end);
        val tests = BloodTest.latestForGraph(50000, start, end);
        return getJsonForTreatments(treatments, tests,
                Pref.getString("saved_profile_list_json",""),
                BasalProfile.getAllProfilesAsJson());
    }

    private static JSONArray getJsonForTreatments(final List<Treatments> treatments, final List<BloodTest> tests, final String profile, final String basal) {
        val reply = new JSONArray();
        if (profile != null) {
            if (profile.length() > 5) {
                val item = new JSONObject();
                try {
                    item.put("profile", profile);
                    reply.put(item);
                } catch (final JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (basal != null) {
            if (basal.length() > 5) {
                val item = new JSONObject();
                try {
                    item.put("basal", basal);
                    reply.put(item);
                } catch (final JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (treatments != null) {
            // populate json structures
            try {
                // for each treatment produce a json record
                for (val treatment : treatments) {
                    val item = new JSONObject();
                    if (treatment.insulin > 0 || treatment.carbs > 0) {
                        item.put("date", treatment.timestamp);
                        if (treatment.carbs > 0) {
                            item.put("carbs", treatment.carbs);
                        }
                        if (treatment.insulin > 0) {
                            item.put("units",  new BigDecimal(treatment.insulin).setScale(2, BigDecimal.ROUND_HALF_UP));
                        }
                        reply.put(item);
                    }

                }

                UserError.Log.d(TAG, "Output: " + reply.toString());
            } catch (JSONException e) {
                UserError.Log.wtf(TAG, "Got json exception: " + e);
            }
        }
        if (tests != null) {
            // populate json structures
            try {
                // for each treatment produce a json record
                for (val test : tests) {
                    val item = new JSONObject();
                    if (test.mgdl > 0) {
                        item.put("date", test.timestamp);
                        item.put("mgdl", new BigDecimal(test.mgdl).setScale(2, BigDecimal.ROUND_HALF_UP));
                        reply.put(item);
                    }

                }

                UserError.Log.d(TAG, "Output: " + reply.toString());
            } catch (JSONException e) {
                UserError.Log.wtf(TAG, "Got json exception: " + e);
            }
        }
        return reply;
    }
}

