package com.eveningoutpost.dexdrip.utilitymodels.pebble.watchface;

import com.eveningoutpost.dexdrip.R;

import java.io.InputStream;

/**
 * Created by jamorham on 08/06/2016.
 */
public class InstallPebbleClassicTrendWatchface extends InstallPebbleWatchFace {


    private final static String TAG = "InstallPebbleClassicTrendWatchFace";


    @Override
    protected String getTag() {
        return TAG;
    }


    protected InputStream openRawResource() {
        return getResources().openRawResource(R.raw.xdrip_pebble_classic_trend);
    }

    protected String getOutputFilename() {
        return "xDrip-plus-pebble-classic-trend-auto-install.pbw";
    }


}
