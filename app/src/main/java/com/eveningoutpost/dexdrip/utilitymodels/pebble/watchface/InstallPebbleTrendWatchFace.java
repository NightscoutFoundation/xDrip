
package com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface;

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
        // unified version for pebble classic and pebble time
        return getResources().openRawResource(R.raw.xdrip_pebble_classic_trend);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-trend-auto-install.pbw";
    }


}
