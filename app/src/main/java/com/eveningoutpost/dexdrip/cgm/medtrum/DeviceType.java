package com.eveningoutpost.dexdrip.cgm.medtrum;

import android.util.SparseArray;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.DEVICE_TYPE_A6;

/**
 * jamorham
 *
 * Type of medtrum device
 */

public enum DeviceType {

    A6(DEVICE_TYPE_A6);

    private static final SparseArray<DeviceType> map = new SparseArray<>();

    final int id;

    DeviceType(int id) {
        this.id = id;
    }

    static {
        for (DeviceType dt : values()) {
            map.put(dt.id, dt);
        }
    }

    public static DeviceType get(int id) {
        return map.get(id);
    }
}

