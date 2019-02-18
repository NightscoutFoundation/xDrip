package com.eveningoutpost.dexdrip.cgm.nsfollow;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.NSFollow;

/**
 * jamorham
 *
 * Nightscout follower collection service
 *
 * Handles Android wake up and polling schedule, decoupled from data transport
 *
 */

// TODO MegaStatus

public class NightscoutFollowService extends ForegroundService {

    private static final String TAG = "NightscoutFollow";
    private static final long SAMPLE_PERIOD = DEXCOM_PERIOD;

    protected static volatile String lastState = "";

    private static BuggySamsung buggySamsung;
    private static volatile long wakeup_time = 0;

    private BgReading lastBg;

    private void buggySamsungCheck() {
        if (buggySamsung == null) {
            buggySamsung = new BuggySamsung(TAG);
        }
        buggySamsung.evaluate(wakeup_time);
        wakeup_time = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("NSFollow-osc", 60000);
        try {

            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP");

            // Check service should be running
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                //       msg("Stopping");
                stopSelf();
                return START_NOT_STICKY;
            }
           buggySamsungCheck();

            // Check current
            lastBg = BgReading.lastNoSenssor();
            if (lastBg == null || JoH.msSince(lastBg.timestamp) > SAMPLE_PERIOD) {

                if (JoH.ratelimit("last-ns-follow-poll", 5)) {
                    Inevitable.task("NS-Follow-Work", 200, () -> NightscoutFollow.work(true));
                }
            } else {
                UserError.Log.d(TAG, "Already have recent reading: " + JoH.msSince(lastBg.timestamp));
            }

            scheduleWakeUp();
        } finally {
            JoH.releaseWakeLock(wl);
        }
        return START_STICKY;
    }

    static void scheduleWakeUp() {
        final BgReading lastBg = BgReading.lastNoSenssor();
        final long last = lastBg != null ? lastBg.timestamp : 0;

        final long grace = Constants.SECOND_IN_MS * 10;
        final long next = Anticipate.next(JoH.tsl(), last, SAMPLE_PERIOD, grace) + grace;
        wakeup_time = next;
        UserError.Log.d(TAG, "Anticipate next: " + JoH.dateTimeText(next) + "  last: " + JoH.dateTimeText(last));

        JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next), WakeLockTrampoline.getPendingIntent(NightscoutFollowService.class, Constants.NSFOLLOW_SERVICE_FAILOVER_ID));

    }

    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == NSFollow;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static void msg(final String msg) {
        lastState = msg;
    }

    public static SpannableString nanoStatus() {
        return JoH.emptyString(lastState) ? null : new SpannableString(lastState);
    }
}
