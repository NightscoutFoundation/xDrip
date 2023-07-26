package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Created by jamorham on 04/03/2018.
 */

// TODO future move noise trigger constants here

public class Noise {

    private static final String TAG = "xDripNoise";

    public static int getNoiseBlockLevel() {
        int value = 200;
        try {
            value = Integer.parseInt(Pref.getString("noise_block_level", "200"));
        } catch (NumberFormatException e) {
            UserError.Log.e(TAG, "Cannot process noise block level: " + e);
        }
        return value;

    }

}
