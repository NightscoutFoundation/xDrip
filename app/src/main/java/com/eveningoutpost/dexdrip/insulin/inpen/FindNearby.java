package com.eveningoutpost.dexdrip.insulin.inpen;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;

// jamorham

public class FindNearby implements BtCallBack {

    private static final String TAG = "InPen Scan";
    private static ScanMeister scanMeister;

    public synchronized void scan() {

        if (scanMeister == null) {
            scanMeister = new InPenScanMeister().addCallBack(this, TAG);
            scanMeister.allowWide();
        } else {
            scanMeister.stop();
        }
        scanMeister.scan();
    }


    @Override
    public void btCallback(final String mac, final String status) {
        switch (status) {
            case ScanMeister.SCAN_FOUND_CALLBACK:
                UserError.Log.e(TAG, "Found! " + mac);
                InPen.setMac(mac);
                InPenEntry.startWithRefresh();
                if (JoH.ratelimit("found-inpen-first-time",86000)) {
                    JoH.playResourceAudio(R.raw.labbed_musical_chime);
                }
                break;

            case ScanMeister.SCAN_FAILED_CALLBACK:
            case ScanMeister.SCAN_TIMEOUT_CALLBACK:
                UserError.Log.d(TAG, "Scan callback: " + status);
                JoH.static_toast_long(status);
                break;

        }
    }
}


