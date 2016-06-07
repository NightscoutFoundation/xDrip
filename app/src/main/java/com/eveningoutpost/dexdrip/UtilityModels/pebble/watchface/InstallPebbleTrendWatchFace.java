
package com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface;

import com.eveningoutpost.dexdrip.R;

import java.io.InputStream;

/**
 * Created by jamorham on 22/04/2016.
 */
public class InstallPebbleTrendWatchFace extends InstallPebbleWatchFace {

    private static String TAG = "InstallPebbleTrendWatchFace";


    @Override
    protected String getTag() {
        return TAG;
    }


    protected InputStream openRawResource() {
        return getResources().openRawResource(R.raw.xdrip_pebble_trend);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-trend-auto-install.pbw";
    }


}
