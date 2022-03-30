package com.eveningoutpost.dexdrip.receivers.aidex;

import android.support.annotation.StringRes;

import com.eveningoutpost.dexdrip.R;

import java.util.HashMap;
import java.util.Map;

public enum AidexMessageType {
    SENSOR_ERROR(R.string.aidex_sensor_error),
    TRANSMITTER_ERROR(R.string.aidex_transmitter_error),
    CALIBRATION_REQUESTED(R.string.aidex_calibration_requested),
    GLUCOSE_INVALID(R.string.aidex_glucose_invalid),
    BATTERY_LOW(R.string.aidex_battery_low),
    BATTERY_EMPTY(R.string.aidex_battery_empty),
    SENSOR_EXPIRED(R.string.aidex_sensor_expired),
    OTHER(null);

    static Map<String,AidexMessageType> mapByKey;
    private @StringRes Integer resourceId;

    static {
        mapByKey = new HashMap<>();

        for (AidexMessageType value : values()) {
            mapByKey.put(value.name(), value);
        }
    }

    AidexMessageType(@StringRes Integer resourceId) {
        this.resourceId = resourceId;
    }

    public static AidexMessageType getByKey(String key) {
        if (mapByKey.containsKey(key))
            return mapByKey.get(key);
        else
            return OTHER;
    }

    public int getResourceId() {
        return resourceId;
    }
}
