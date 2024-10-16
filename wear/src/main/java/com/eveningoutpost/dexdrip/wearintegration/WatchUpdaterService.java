package com.eveningoutpost.dexdrip.wearintegration;

// jamorham

import com.eveningoutpost.dexdrip.ListenerService;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.models.UserError.Log;

public class WatchUpdaterService {

    // stub - mostly not used on wear

    public static void checkOb1Queue() {
    }

    public static boolean isEnabled() {
        return true; // when running on wear we expect that wear integration will always be enabled on phone
    }

    public static void startServiceAndResendDataIfNeeded(long since) {
        Log.e("WatchUpdaterService", "startServiceAndResendDataIfNeeded - SYNC_ALL_DATA");
        // if needed this is where we would trigger return of extra backfilled data from wear to phone
        // TODO this may well be quite inefficient or miss records at the moment
        ListenerService.SendData(xdrip.getAppContext(), ListenerService.SYNC_ALL_DATA, null);

    }

    // Start listener service with selfping to reesablish connectivity via LTE
    public static void startServiceAndPingMyself() {
        Log.e("WatchUpdaterService", "startServiceAndPingMyself - PING_MYSELF");
        // if needed this is where we would trigger return of extra backfilled data from wear to phone
        // TODO this may well be quite inefficient or miss records at the moment
        ListenerService.SendData(xdrip.getAppContext(), ListenerService.PING_MYSELF, null);

    }

}