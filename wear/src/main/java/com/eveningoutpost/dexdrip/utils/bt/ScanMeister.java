package com.eveningoutpost.dexdrip.utils.bt;

import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.RxBleProvider;
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.exceptions.BleScanException;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.NoArgsConstructor;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.Models.JoH.ratelimit;


// jamorham

// Stand alone bluetooth scanner using BtCallBack

@NoArgsConstructor
public class ScanMeister {

    private static final String TAG = ScanMeister.class.getSimpleName();
    private static final String STOP_SCAN_TASK_ID = "stop_meister_scan";
    private static final int DEFAULT_SCAN_SECONDS = 30;
    private static final int MINIMUM_RSSI = -97; // ignore all quieter than this
    private static volatile Subscription scanSubscription;
    private final RxBleClient rxBleClient = RxBleProvider.getSingleton();
    private final ConcurrentHashMap<String, BtCallBack> callbacks = new ConcurrentHashMap<>();
    private final PowerManager.WakeLock wl = JoH.getWakeLock("jam-bluetooth-meister", 1000);
    private int scanSeconds = DEFAULT_SCAN_SECONDS;
    private volatile String address;


    public ScanMeister(String address) {
        this.address = address;
    }

    public ScanMeister setScanSeconds(int seconds) {
        this.scanSeconds = seconds;
        return this;
    }

    public ScanMeister setAddress(String address) {
        this.address = address;
        return this;
    }

    // Callback boiler plate
    public ScanMeister addCallBack(BtCallBack callback, String name) {
        callbacks.put(name, callback);
        return this;
    }

    public void removeCallBack(String name) {
        callbacks.remove(name);
    }

    private synchronized void processCallBacks(String address, String status) {
        if (address == null) address = "NULL";
        boolean called_back = false;
        UserError.Log.d(TAG, "Processing callbacks for " + address + " " + status);
        for (Map.Entry<String, BtCallBack> entry : callbacks.entrySet()) {
            UserError.Log.d(TAG, "Callback: " + entry.getKey());
            entry.getValue().btCallback(address, status);
            called_back = true;
        }
        if (!called_back) {
            UserError.Log.d(TAG, "No callbacks registered!!");
        }
    }

    ///

    public synchronized void scan() {
        extendWakeLock((scanSeconds + 1) * Constants.SECOND_IN_MS);
        stopScan("Scan start");
        UserError.Log.d(TAG, "startScan called: hunting: " + address);
        scanSubscription = rxBleClient.scanBleDevices(
                new ScanSettings.Builder()

                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()//,  //new ScanFilter.Builder()
                //
                // add custom filters if needed
                //  .build()
        )
                .timeout(scanSeconds, TimeUnit.SECONDS) // is unreliable
                .subscribeOn(Schedulers.io())
                .subscribe(this::onScanResult, this::onScanFailure);

        Inevitable.task(STOP_SCAN_TASK_ID, scanSeconds * Constants.SECOND_IN_MS, this::stopScanWithTimeoutCallback);
    }

    public void stop() {
        stopScan("Scan stop");
    }

    private void stopScanWithTimeoutCallback() {
        stopScan("Stop with Timeout");
        processCallBacks(address, "SCAN_TIMEOUT");
    }

    private synchronized void stopScan(String source) {
        UserError.Log.d(TAG, "stopScan called from: " + source);
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
            UserError.Log.d(TAG, "stopScan stopped scan");
            scanSubscription = null;
            Inevitable.kill(STOP_SCAN_TASK_ID);
        }
    }


    // Successful result from our bluetooth scan
    private synchronized void onScanResult(ScanResult bleScanResult) {

        if (address == null) {
            UserError.Log.d(TAG, "Address has been set to null, stopping scan.");
            stopScan("Address nulled");
            return;
        }

        final int rssi = bleScanResult.getRssi();
        if (rssi > MINIMUM_RSSI) {
            //final String this_name = bleScanResult.getBleDevice().getName();
            final String this_address = bleScanResult.getBleDevice().getMacAddress();
            final boolean matches = address != null && address.equalsIgnoreCase(this_address);
            if (matches || JoH.quietratelimit("scanmeister-show-result", 2)) {
                UserError.Log.d(TAG, "Found a device: " + this_address + " rssi: " + rssi + "  " + (matches ? "-> MATCH" : ""));
            }
            if (matches) {
                stopScan("Got match");
                JoH.threadSleep(500);
                processCallBacks(this_address, "SCAN_FOUND");
                releaseWakeLock();
            }

        } else {
            if (JoH.quietratelimit("log-low-rssi", 2)) {
                UserError.Log.d(TAG, "Low rssi device: " + bleScanResult.getBleDevice().getMacAddress() + " rssi: " + rssi);
            }
        }
    }


    // Failed result from our bluetooth scan
    private synchronized void onScanFailure(Throwable throwable) {
        UserError.Log.d(TAG, "onScanFailure: " + throwable);
        if (throwable instanceof BleScanException) {
            final String info = HandleBleScanException.handle(TAG, (BleScanException) throwable);
            UserError.Log.d(TAG, "Scan failure: " + info);
            if (((BleScanException) throwable).getReason() == BleScanException.BLUETOOTH_DISABLED) {
                // Attempt to turn bluetooth on
                if (ratelimit("bluetooth_toggle_on", 30)) {
                    UserError.Log.d(TAG, "Pause before Turn Bluetooth on");
                    JoH.threadSleep(2000);
                    UserError.Log.e(TAG, "Trying to Turn Bluetooth on");
                    JoH.setBluetoothEnabled(xdrip.getAppContext(), true);
                }
            }
            processCallBacks(address, "SCAN_FAILED");
        } else if (throwable instanceof TimeoutException) {
            // note this code path not always reached - see inevitable task
            processCallBacks(address, "SCAN_TIMEOUT");
        }

        stopScan("Scan failure");
        releaseWakeLock();
    }


    private synchronized void extendWakeLock(long ms) {
        JoH.releaseWakeLock(wl); // lets not get too messy
        wl.acquire(ms);
    }

    private void releaseWakeLock() {
        JoH.releaseWakeLock(wl);
    }

}
