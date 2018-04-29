package com.eveningoutpost.dexdrip.UtilityModels;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jamorham on 10/10/2017.
 * <p>
 * Fake Datasource emulating WifiWixel
 * Produces sine wave "bouncing" raw+filtered data
 * Designed to look unrealistic to reduce chance of not realising the data is fake
 */

public class MockDataSource {

    private static final String TAG = "MockDataSource";

    private static final String PREF_BROKEN_RAW = "MockDataSource-broken-raw";

    public static String getFakeWifiData() {

        long time = JoH.tsl();
        double divisor_scale = 5000000;
        double mod_raw = (time / divisor_scale) % Math.PI;
        double mod_filtered = ((time - 500000) / divisor_scale) % Math.PI;
        double raw_value = (Math.sin(mod_raw) * 100000) + 50000;
        double filtered_value = (Math.sin(mod_filtered) * 100000) + 50000;

        if (Pref.getBooleanDefaultFalse(PREF_BROKEN_RAW)) {
            raw_value = Math.sin(mod_raw) * 1000;
        }

        final JSONObject json = new JSONObject();
        try {
            json.put("CaptureDateTime", time);
            json.put("RelativeTime", 0L);
            json.put("TransmitterId", "123456");
            json.put("RawValue", (long) raw_value);
            json.put("FilteredValue", (long) filtered_value);
            json.put("BatteryLife", "100");
            json.put("ReceivedSignalStrength", 1);
            json.put("TransmissionId", 1);

        } catch (JSONException e) {
            UserError.Log.e(TAG, "Got weird Json exception: ", e);
        }
        return json.toString();
    }

    public static void breakRaw() {
        Pref.setBoolean(PREF_BROKEN_RAW, true);
    }

    public static void fixRaw() {
        Pref.setBoolean(PREF_BROKEN_RAW, false);
    }
}




