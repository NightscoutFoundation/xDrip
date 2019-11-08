package com.eveningoutpost.dexdrip.watch.miband;

// jamorham

import android.os.Bundle;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.utils.bt.BtCallBack2;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;

public class FindNearby implements BtCallBack2 {

    private static final String TAG = "MiBand Scan";
    private static ScanMeister scanMeister;

    public synchronized void scan() {

        if (scanMeister == null) {
            scanMeister = new ScanMeister();
        } else {
            scanMeister.stop();
        }

        // TODO expand this list
        scanMeister
                .setName("MI Band 2")
                .setName("MI Band 3")
                .setName("MI Band 4")
                .addCallBack2(this, TAG).scan();
    }


    @Override
    public void btCallback2(String mac, String status, String name, Bundle bundle) {
        switch (status) {
            case ScanMeister.SCAN_FOUND_CALLBACK:
                MiBand.setMac(mac);
                MiBand.setModel(name);
                JoH.static_toast_long("Found " + name + ", mac address: " + mac);
                MiBandEntry.startWithRefresh();
                break;
            case ScanMeister.SCAN_FAILED_CALLBACK:
            case ScanMeister.SCAN_TIMEOUT_CALLBACK:
                JoH.static_toast_long(status);
                break;

        }
    }
}
