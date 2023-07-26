package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Created by jamorham on 09/01/2017.
 * <p>
 * Wrapper for legacy Activity dependency
 */

abstract class FauxActivity {

    private final static String TAG = "FauxActivity";

    protected void onCreate(Bundle savedInstanceState) {
        UserError.Log.d(TAG, "onCreate called: " + JoH.backTrace());
    }

    protected void onResume() {
        UserError.Log.d(TAG, "onResume called: " + JoH.backTrace());
    }

    protected void onPause() {
        UserError.Log.d(TAG, "onPause called: " + JoH.backTrace());
    }

    protected void startService(Intent intent) {
        xdrip.getAppContext().startService(intent);
    }

    protected void finish() {
        UserError.Log.d(TAG, "finish() called: " + JoH.backTrace());
    }
}
