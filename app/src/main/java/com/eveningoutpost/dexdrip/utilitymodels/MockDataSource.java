package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

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

    private static final String PREF_SPEED_UP = "MockDataSource-speed-up";

    private static final String PREF_AMPLIFY = "MockDataSource-amplify";

    public static double divisor_scale = 5000000;
    public static double amplify_cnst = 100000;

    public static String getFakeWifiData() {

        long time = JoH.tsl();
        if (Pref.getBooleanDefaultFalse(PREF_SPEED_UP)) {
            divisor_scale = 1500000;
        }
        double mod_raw = (time / divisor_scale) % Math.PI;
        double mod_filtered = ((time - 500000) / divisor_scale) % Math.PI;
        if (Pref.getBooleanDefaultFalse(PREF_AMPLIFY)) {
            amplify_cnst = 330000;
        }
        double raw_value = (Math.sin(mod_raw) * amplify_cnst) + 50000;
        double filtered_value = (Math.sin(mod_filtered) * amplify_cnst) + 50000;

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

    public static void defaults() {
        Pref.setBoolean(PREF_SPEED_UP, false);
        Pref.setBoolean(PREF_AMPLIFY, false);
    }

    public static void breakRaw() {
        Pref.setBoolean(PREF_BROKEN_RAW, true);
    }

    public static void fixRaw() {
        Pref.setBoolean(PREF_BROKEN_RAW, false);
    }

    public static void speedup() {
        Pref.setBoolean(PREF_SPEED_UP, true);
    }

    public static void amplify() {
        Pref.setBoolean(PREF_AMPLIFY, true);
    }
}




