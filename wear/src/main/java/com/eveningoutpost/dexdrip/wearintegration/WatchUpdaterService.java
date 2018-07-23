package com.eveningoutpost.dexdrip.wearintegration;

// jamorham

public class WatchUpdaterService {

    // stub - not used on wear

    public static void checkOb1Queue() {
    }

    public static boolean isEnabled() {
        return true; // when running on wear we expect that wear integration will always be enabled on phone
    }

    public static void startServiceAndResendDataIfNeeded(long since) {
        // if needed this is where we would trigger return of extra backfilled data from wear to phone
    }

}