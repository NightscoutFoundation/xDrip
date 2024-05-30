package com.eveningoutpost.dexdrip.cgm.medtrum;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;
import com.polidea.rxandroidble2.scan.ScanResult;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum.processDataFromScanRecord;

// jamorham

public class Scanner extends ScanMeister {

    private String TAG = "MedtrumScanner";

    @Override
    protected synchronized void onScanResult(ScanResult bleScanResult) {

        if (address == null) {
            UserError.Log.d(TAG, "Address has been set to null, stopping scan.");
            stopScan("Address nulled");
            return;
        }
        final String this_address = bleScanResult.getBleDevice().getMacAddress();

        boolean matches = this_address.equals(address);


        if (matches) {
            stopScan("Got match");

            processDataFromScanRecord(bleScanResult.getScanRecord());

            JoH.threadSleep(500);
            processCallBacks(this_address, "SCAN_FOUND");
            releaseWakeLock();
        }

        if (JoH.quietratelimit("scan-debug-result", 5)) {
            UserError.Log.d(TAG, "Found device: " + this_address + (matches ? "  MATCH" : ""));
        }

    }

    protected Scanner setTag(String tag) {
        this.TAG = tag;
        return this;
    }

}
