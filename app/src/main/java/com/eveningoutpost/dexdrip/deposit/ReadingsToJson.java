package com.eveningoutpost.dexdrip.deposit;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.UserError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.List;

import lombok.val;

// jamorham

class ReadingsToJson {

    private static final String TAG = ReadingsToJson.class.getSimpleName();

    static JSONArray getJsonForStartEnd(final long start, final long end) {
        val readings = BgReading.latestForGraph(50000, start, end);
        return getJsonForReadings(readings);
    }

    private static JSONArray getJsonForReadings(final List<BgReading> readings) {
        val reply = new JSONArray();

        if (readings != null) {
            // populate json structures
            try {
                // for each reading produce a json record
                for (val reading : readings) {
                    val item = new JSONObject();

                    item.put("date", reading.timestamp);
                    item.put("sgv", (int) reading.getDg_mgdl());
                    try {
                        item.put("delta", new BigDecimal(reading.getDg_slope() * 5 * 60 * 1000).setScale(3, BigDecimal.ROUND_HALF_UP));
                    } catch (NumberFormatException e) {
                        UserError.Log.e(TAG, "Could not pass delta to webservice as was invalid number");
                    }
                    item.put("direction", reading.getDg_deltaName());
                    item.put("noise", reading.noiseValue());

                    reply.put(item);
                }

                UserError.Log.d(TAG, "Output: " + reply.toString());
            } catch (JSONException e) {
                UserError.Log.wtf(TAG, "Got json exception: " + e);
            }
        }
        return reply;
    }
}
