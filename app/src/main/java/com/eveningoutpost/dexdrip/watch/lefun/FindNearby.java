package com.eveningoutpost.dexdrip.watch.lefun;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;

public class FindNearby implements BtCallBack {

    private static final String TAG = "LeFun Scan";
    private static ScanMeister scanMeister;

    public synchronized void scan() {

        if (scanMeister == null) {
            scanMeister = new ScanMeister();
        } else {
            scanMeister.stop();
        }

        // TODO expand this list
        scanMeister
                .setName("Lefun")
                .setName("F3")
                .setName("W3")
                .addCallBack(this, TAG).scan();
    }


    @Override
    public void btCallback(final String mac, final String status) {
        switch (status) {
            case ScanMeister.SCAN_FOUND_CALLBACK:
                LeFun.setMac(mac);
                JoH.static_toast_long("Found! " + mac);
                LeFunEntry.startWithRefresh();
                break;

            case ScanMeister.SCAN_FAILED_CALLBACK:
            case ScanMeister.SCAN_TIMEOUT_CALLBACK:
                JoH.static_toast_long(status);
                break;

        }
    }
}
