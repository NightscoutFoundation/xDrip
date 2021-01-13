package com.eveningoutpost.dexdrip.utils.bt;


import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;

import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static com.polidea.rxandroidble2.exceptions.BleScanException.BLUETOOTH_CANNOT_START;
import static com.polidea.rxandroidble2.exceptions.BleScanException.BLUETOOTH_DISABLED;
import static com.polidea.rxandroidble2.exceptions.BleScanException.BLUETOOTH_NOT_AVAILABLE;
import static com.polidea.rxandroidble2.exceptions.BleScanException.LOCATION_PERMISSION_MISSING;
import static com.polidea.rxandroidble2.exceptions.BleScanException.LOCATION_SERVICES_DISABLED;
import static com.polidea.rxandroidble2.exceptions.BleScanException.SCAN_FAILED_ALREADY_STARTED;
import static com.polidea.rxandroidble2.exceptions.BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED;
import static com.polidea.rxandroidble2.exceptions.BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED;
import static com.polidea.rxandroidble2.exceptions.BleScanException.SCAN_FAILED_INTERNAL_ERROR;
import static com.polidea.rxandroidble2.exceptions.BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES;
import static com.polidea.rxandroidble2.exceptions.BleScanException.UNDOCUMENTED_SCAN_THROTTLE;
import static com.polidea.rxandroidble2.exceptions.BleScanException.UNKNOWN_ERROR_CODE;

public class HandleBleScanException {

    public static String handle(String TAG, com.polidea.rxandroidble2.exceptions.BleScanException bleScanException) {
        final String text;

        switch (bleScanException.getReason()) {
            case BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0+ location permission is required. Implement Runtime Permissions";
                break;
            case LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0+";
                break;
            case SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan";
                break;
            case SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",
                        secondsTill(bleScanException.getRetryDateSuggestion())
                );
                break;
            case UNKNOWN_ERROR_CODE:
            case BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        UserErrorLog.w(TAG, text + " " + bleScanException);
        return text;
    }
    private static long secondsTill(Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }
}
