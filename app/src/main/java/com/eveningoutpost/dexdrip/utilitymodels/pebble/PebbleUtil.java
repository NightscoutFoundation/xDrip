package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;


/**
 * Created by andy on 04/06/16.
 */
public class PebbleUtil {

    private static final String TAG = PebbleUtil.class.getSimpleName();

    public static PebbleDisplayType pebbleDisplayType = PebbleDisplayType.None;

    public static int getCurrentPebbleSyncType() {
        String value = "1";

        try {
            value = Pref.getString("broadcast_to_pebble_type", Pref.getBoolean("broadcast_to_pebble", false) ? "2" : "1");
        } catch (ClassCastException ex) {
          //  UserError.Log.w(TAG, "");
        }


//        if (value == null) {
//            try {
//                Boolean bvalue = sharedPreferences.getBoolean("broadcast_to_pebble", false);
//
//                value = bvalue ? "2" : "1";
//            } catch (ClassCastException ex) {
//
//            }
//        }

        return Integer.parseInt(value);
    }


    public static int getCurrentPebbleSyncType(Object value) {

        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        } else if (value instanceof Boolean) {
            return ((Boolean) value) ? 2 : 1;
        } else {
            UserError.Log.e(TAG, "Pebble Sync Type from configuration is in wrong type: " + value + ", type=" + value.getClass().getSimpleName());
        }

        return -1;
    }


    public static PebbleDisplayType getPebbleDisplayType(int displayTypeNumeric) {
        switch (displayTypeNumeric) {
            case 2:
                return PebbleDisplayType.Standard;

            case 3:
                return PebbleDisplayType.Trend;

            case 4:
                return PebbleDisplayType.TrendClassic;

            case 5:
                return PebbleDisplayType.TrendClay;

            default:
                return PebbleDisplayType.None;

        }
    }


}
