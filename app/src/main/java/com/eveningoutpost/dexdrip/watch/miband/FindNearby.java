package com.eveningoutpost.dexdrip.watch.miband;

// jamorham

import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.JoH;
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
        JoH.static_toast_long("Searching... Please keep MiBand near your phone" );
        // TODO expand this list
        scanMeister
                .setName(Const.MIBAND_NAME_2)
                .setName(Const.MIBAND_NAME_3)
                .setName(Const.MIBAND_NAME_3_1) //second name
                .setName(Const.MIBAND_NAME_4)
                .addCallBack2(this, TAG).scan();
    }


    @Override
    public void btCallback2(String mac, String status, String name, Bundle bundle) {
        switch (status) {
            case ScanMeister.SCAN_FOUND_CALLBACK:
                MiBand.setMac(mac);
                MiBand.setModel(name, mac);
                JoH.static_toast_long("Found " + name + ", mac address: " + mac);
                break;
            case ScanMeister.SCAN_FAILED_CALLBACK:
            case ScanMeister.SCAN_TIMEOUT_CALLBACK:
                JoH.static_toast_long("Could not find MiBand, please try again, or enter MAC address manually");
                break;

        }
    }
}
