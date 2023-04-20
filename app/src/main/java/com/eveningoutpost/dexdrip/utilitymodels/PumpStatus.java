package com.eveningoutpost.dexdrip.utilitymodels;

import android.util.Log;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jamorham on 18/08/2017.
 */

public class PumpStatus {

    private static final String TAG = "PumpStatus";
    private static final String PUMP_RESERVOIR = "pump-reservoir";
    private static final String PUMP_BOLUSIOB = "pump-bolusiob";
    private static final String PUMP_BATTERY = "pump-battery";
    private static final String TIME = "-time";

    private static String last_json = "";

    private static void setValue(String name, double value) {
        if (value < 0) value = -1;
        PersistentStore.setDouble(name, value);
        PersistentStore.setLong(name + TIME, JoH.tsl());
    }

    private static double getValue(String name) {
        final long ts = PersistentStore.getLong(name + TIME);
        if ((ts > 1503081681000L) && (JoH.msSince(ts) < Constants.MINUTE_IN_MS * 30)) {
            return PersistentStore.getDouble(name);
        } else {
            return -1;
        }
    }

    public static void setReservoir(double reservoir) {
        setValue(PUMP_RESERVOIR, reservoir);
    }

    private static double getReservoir() {
        return getValue(PUMP_RESERVOIR);
    }

    public static void setBolusIoB(double value) {
        setValue(PUMP_BOLUSIOB, value);
    }

    private static double getBolusIoB() {
        return getValue(PUMP_BOLUSIOB);
    }

    public static void setBattery(double value) {
        setValue(PUMP_BATTERY, value);
    }

    public static double getBattery() {
        return getValue(PUMP_BATTERY);
    }

    public static String getReservoirString() {
        final double reservoir = getReservoir();
        if (reservoir > -1) {
            return "\uD83D\uDCDF" + "" + JoH.qs(reservoir, 1) + "U ";
        } else {
            return "";
        }
    }

    public static String getBolusIoBString() {
        final double value = getBolusIoB();
        if (value > -1) {
            // return "\u231B" + " " + JoH.qs(value, 1) + "U ";
            return "\u23F3" + "" + JoH.qs(value, 2) + "U ";
        } else {
            return "";
        }
    }

    public static String getBatteryString() {
        final double value = getBattery();
        if (value > -1) {
            return "\uD83D\uDD0B" + "" + JoH.qs(value, 0) + "% ";
        } else {
            return "";
        }
    }

    public static String toJson() {
        final JSONObject json = new JSONObject();
        try {
            json.put("reservoir", getReservoir());
            json.put("bolusiob", getBolusIoB());
            json.put("battery", getBattery());
        } catch (JSONException e) {
            UserError.Log.e(TAG, "Got exception building PumpStatus " + e);
        }
        return json.toString();
    }

    public static void fromJson(String msg) {
        try {
            final JSONObject json = new JSONObject(msg);
            setReservoir(json.getDouble("reservoir"));
            setBolusIoB(json.getDouble("bolusiob"));
            setBattery(json.getDouble("battery"));
        } catch (Exception e) {
            Log.e(TAG, "Got exception processing json msg: " + e + " " + msg);
        }
    }

    public static synchronized void syncUpdate() {
        if (Home.get_master()) {
            final String current_json = toJson();
            if (current_json.equals(last_json)) {
                Log.d(TAG, "No sync as data is identical");
            } else {
                Log.d(TAG, "Sending update: " + current_json);
                GcmActivity.sendPumpStatus(current_json);
                last_json = current_json;
            }
        }
    }
}
