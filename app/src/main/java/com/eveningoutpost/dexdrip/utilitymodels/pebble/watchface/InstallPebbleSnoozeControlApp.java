
package com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface;

import com.eveningoutpost.dexdrip.R;

import java.io.InputStream;

/**
 * Created by jamorham on 22/04/2016.
 */
public class InstallPebbleSnoozeControlApp extends InstallPebbleWatchFace {

    private static String TAG = "InstallPebbleSnoozeControlApp";


    @Override
    protected String getTag() {
        return TAG;
    }


    protected InputStream openRawResource() {

        return getResources().openRawResource(R.raw.xdrip_plus_pebble_snooze);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-snooze-auto-install.pbw";
    }


}