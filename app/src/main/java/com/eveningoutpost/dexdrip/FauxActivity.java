package com.eveningoutpost.dexdrip;


import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;

/**
 * Created by jamorham on 09/01/2017.
 * <p>
 * Wrapper for legacy Activity dependency
 */

abstract class FauxActivity {

    private final static String TAG = "FauxActivity";

    protected void onCreate(Bundle savedInstanceState) {
        UserErrorLog.d(TAG, "onCreate called: " + JoH.backTrace());
    }

    protected void onResume() {
        UserErrorLog.d(TAG, "onResume called: " + JoH.backTrace());
    }

    protected void onPause() {
        UserErrorLog.d(TAG, "onPause called: " + JoH.backTrace());
    }

    protected void startService(Intent intent) {
        xdrip.getAppContext().startService(intent);
    }

    protected void finish() {
        UserErrorLog.d(TAG, "finish() called: " + JoH.backTrace());
    }
}
