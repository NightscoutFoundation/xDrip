package com.eveningoutpost.dexdrip.receivers.aidex;

import androidx.annotation.StringRes;

import com.eveningoutpost.dexdrip.R;

import java.util.HashMap;
import java.util.Map;

public enum AidexMessageType {
    SENSOR_ERROR(null),
    TRANSMITTER_ERROR(null),
    CALIBRATION_REQUESTED(null),
    GLUCOSE_INVALID(null),
    BATTERY_LOW(null),
    BATTERY_EMPTY(null),
    SENSOR_EXPIRED(null),
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
