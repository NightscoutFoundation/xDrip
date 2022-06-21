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
import java.util.Arrays;
import java.util.List;

import lombok.val;

// jamorham

public class TreatmentsToJson {

    private static final String TAG = TreatmentsToJson.class.getSimpleName();

    static JSONArray getJsonForStartEnd(final long start, final long end) {
        List<Treatments> treatments = Treatments.latestForGraph(50000, start, end);
        List<BloodTest> tests = BloodTest.latestForGraph(50000, start, end);
        return getJsonForTreatments(treatments, tests,
                Pref.getString("saved_profile_list_json",""),
                BasalProfile.getAllProfilesAsJson());
    }

    private static JSONArray getJsonForTreatments(final List<Treatments> treatments, final List<BloodTest> tests, final String profile, final String basal) {
        JSONArray reply = new JSONArray();
        if (profile != null) {
            if (profile.length() > 5) {
                JSONObject item = new JSONObject();
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
                JSONObject item = new JSONObject();
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
                for ( Treatments treatment : treatments) {
                    JSONObject item = new JSONObject();
                    if (anyMatchGreaterThanZero(Arrays.asList(treatment.carbs, treatment.fats, treatment.proteins, treatment.insulin))) {
                        item.put("date", treatment.timestamp);
                        if (treatment.carbs > 0) {
                            item.put("carbs", treatment.carbs);
                        }
                        if (treatment.fats > 0) {
                            item.put("fats", treatment.fats);
                        }
                        if (treatment.proteins > 0) {
                            item.put("proteins", treatment.proteins);
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
                for (BloodTest test : tests) {
                    JSONObject item = new JSONObject();
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

    private static boolean anyMatchGreaterThanZero(List<Double> list) {
        for (Double element : list) {
            if (element > 0) return true;
        }
        return false;
    }
}

