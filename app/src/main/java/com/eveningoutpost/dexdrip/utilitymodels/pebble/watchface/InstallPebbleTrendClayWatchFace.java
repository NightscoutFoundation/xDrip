
package com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface;

import com.eveningoutpost.dexdrip.R;

import java.io.InputStream;

/**
 * Created by jamorham on 22/04/2016.
 */
public class InstallPebbleTrendClayWatchFace extends InstallPebbleWatchFace {

    private static String TAG = "InstallPebbleTrendWatchFace";


    @Override
    protected String getTag() {
        return TAG;
    }


    protected InputStream openRawResource() {
        // jstevensog latest watchface
        return getResources().openRawResource(R.raw.xdrip_pebble2);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-clay-auto-install.pbw";
    }


}
