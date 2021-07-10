package com.eveningoutpost.dexdrip.insulin.inpen;

import android.os.ParcelUuid;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.bt.ScanMeister;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.util.concurrent.TimeUnit;

import io.reactivex.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.insulin.inpen.Constants.SCAN_SERVICE_UUID;
import static com.eveningoutpost.dexdrip.insulin.inpen.InPen.STORE_INPEN_ADVERT;

// jamorham

public class InPenScanMeister extends ScanMeister {

    private static final String TAG = "InPenScanMeister";
    private static final boolean D = false;

    @Override
    public synchronized void scan() {

        extendWakeLock((scanSeconds + 1) * Constants.SECOND_IN_MS);
        stopScan("Scan start");
        UserError.Log.d(TAG, "startScan called: hunting: " + address + " " + name);
        scanSubscription = new Subscription(rxBleClient.scanBleDevices(
                new ScanSettings.Builder()
                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build(),
                new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(SCAN_SERVICE_UUID)).build())
                .timeout(scanSeconds, TimeUnit.SECONDS) // is unreliable
                .subscribeOn(Schedulers.io())
                .subscribe(this::onScanResult, this::onScanFailure));

        Inevitable.task(STOP_SCAN_TASK_ID, scanSeconds * Constants.SECOND_IN_MS, this::stopScanWithTimeoutCallback);
    }


    @Override
    protected synchronized void onScanResult(ScanResult bleScanResult) {

        final String this_address = bleScanResult.getBleDevice().getMacAddress();
        final byte[] scanRecordBytes = bleScanResult.getScanRecord().getBytes();

        if (D) UserError.Log.d(TAG, JoH.bytesToHex(bleScanResult.getScanRecord().getBytes()));

        if (scanRecordBytes != null && scanRecordBytes.length > 0) {
            PersistentStore.setBytes(STORE_INPEN_ADVERT + this_address, scanRecordBytes);
        }
        stopScan("Got match");
        JoH.threadSleep(500);
        processCallBacks(this_address, "SCAN_FOUND");
        releaseWakeLock();
    }
}
