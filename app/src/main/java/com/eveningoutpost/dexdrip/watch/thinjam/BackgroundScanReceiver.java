package com.eveningoutpost.dexdrip.watch.thinjam;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.RxBleProvider;
import com.eveningoutpost.dexdrip.utils.bt.BtCallBack2;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.BackgroundScanner;
import com.polidea.rxandroidble2.scan.ScanResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.utils.bt.ScanMeister.SCAN_FOUND_CALLBACK;


// jamorham

// Unfortunately we have to do this in an external class and via the manifest as otherwise the intents do not appear to get delivered

// TODO can we merge this feature in to ScanMeister?


public class BackgroundScanReceiver extends BroadcastReceiver {

    @Getter
    private static final String ACTION_NAME = "Action-BACKGROUND-SCAN";
    private static final String CALLING_CLASS = "CallingClass";

    private static final ConcurrentHashMap<String, BtCallBack2> callbacks2 = new ConcurrentHashMap<>();

    // Callback boiler plate v2 callbacks
    public static void addCallBack2(final BtCallBack2 callback, final String name) {
        callbacks2.put(name, callback);
    }

    public static void removeCallBack(final String name) {
        callbacks2.remove(name);
    }

    private static boolean processCallbacks(final String TAG, final String address, final String name, final String status) {
        boolean called_back = false;
        for (final Map.Entry<String, BtCallBack2> entry : callbacks2.entrySet()) {
            UserError.Log.d(TAG, "Callback2: " + entry.getKey());
            entry.getValue().btCallback2(address, status, name, null);
            called_back = true;
        }
        if (!called_back) {
            UserError.Log.d(TAG, "No callbacks registered!!");
        }
        return called_back;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();

        android.util.Log.d("BackgroundScanReceiver", "GOT SCAN INTENT!! " + action);

        if (action != null && action.equals(ACTION_NAME)) {

            String caller = intent.getStringExtra(CALLING_CLASS);
            if (caller == null) caller = this.getClass().getSimpleName();

            // TODO by class name?
            final BackgroundScanner backgroundScanner = RxBleProvider.getSingleton().getBackgroundScanner();
            try {
                final List<ScanResult> scanResults = backgroundScanner.onScanResultReceived(intent);
                final String matchedMac = scanResults.get(0).getBleDevice().getMacAddress();
                final String matchedName = scanResults.get(0).getBleDevice().getName();
                final boolean calledBack = processCallbacks(caller, matchedMac, matchedName, SCAN_FOUND_CALLBACK);
                UserError.Log.d(caller, "Scan results received: " + matchedMac + " " + scanResults);
                if (!calledBack) {
                    try {
                        // bit of an ugly fix to system wide persistent nature of background scans and lack of proper support for one hit over various android devices
                        backgroundScanner.stopBackgroundBleScan(PendingIntent.getBroadcast(xdrip.getAppContext(), 142, // must match original
                                intent, PendingIntent.FLAG_UPDATE_CURRENT));
                    } catch (Exception e) {
                        //
                    }
                }
            } catch (NullPointerException | BleScanException exception) {
                UserError.Log.e(caller, "Failed to scan devices" + exception);
            }

        }

        // ignore invalid actions
    }
}

