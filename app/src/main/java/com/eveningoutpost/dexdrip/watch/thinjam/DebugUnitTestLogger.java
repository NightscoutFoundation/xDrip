package com.eveningoutpost.dexdrip.watch.thinjam;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.watch.thinjam.io.GetURL;

import lombok.RequiredArgsConstructor;

// jamorham

@RequiredArgsConstructor
public class DebugUnitTestLogger {

    private static final String TAG = "BlueJayMassUnit";

    private static final String SERVER_ADDRESS = "http://127.0.0.1:12577/";

    private final BlueJayService parent;

    public void processTestSuite(final String cmd) {
        JoH.static_toast_long("Test Suite: " + cmd);
        UserError.Log.d(TAG, "Process called with: " + cmd);

        if (cmd == null) {
            UserError.Log.d(TAG, "Null process command received!");
            return;
        }

        if (cmd.equals("shutdown")) {
            parent.stopSelf();
        }

        Inevitable.task("bj-mass-prod-schedule-test" + cmd, 100, new Runnable() {
            @Override
            public void run() {
                final String result = GetURL.getURL(SERVER_ADDRESS + "mass/batch/schedule/" + cmd + "/" + BlueJay.getMac());
                UserError.Log.d(TAG, "Unit test schedule result for " + cmd + ": " + result);
            }
        });
    }

}
