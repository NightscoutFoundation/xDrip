package com.eveningoutpost.dexdrip.cgm.sharefollow;

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
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.MAX_RECORDS_TO_ASK_FOR;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.NON_US_SHARE_BASE_URL;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.US_SHARE_BASE_URL;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.SHFollow;

/**
 * jamorham
 *
 * Dexcom Share follower collection service
 *
 * Handles Android wake up and polling schedule, decoupled from data transport
 */

public class ShareFollowService extends ForegroundService {

    private static final String TAG = "ShareFollow";
    private static final long SAMPLE_PERIOD = DEXCOM_PERIOD;

    protected static volatile String lastState = "";

    private static BuggySamsung buggySamsung;
    private static volatile long wakeup_time = 0;
    private static volatile long last_wakeup = 0;

    private BgReading lastBg;
    private static ShareFollowDownload downloader;


    @Override
    public void onCreate() {
        super.onCreate();
        resetInstance(); // manage static reference life cycle
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("SHFollow-osc", 60000);
        try {

            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP");
            // Check service should be running
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                stopSelf();
                return START_NOT_STICKY;
            }
            buggySamsungCheck();

            last_wakeup = JoH.tsl();

            // Check current
            lastBg = BgReading.lastNoSenssor();
            if (lastBg == null || msSince(lastBg.timestamp) > SAMPLE_PERIOD) {
                // Get the data
                if (downloader == null) {
                    downloader = new ShareFollowDownload(
                            Pref.getBoolean("dex_share_us_acct", true) ? US_SHARE_BASE_URL : NON_US_SHARE_BASE_URL,
                            Pref.getString("shfollow_user", ""), Pref.getString("shfollow_pass", ""));
                }

                if (JoH.ratelimit("last-sh-follow-poll", 5)) {
                    Inevitable.task("SH-Follow-Work", 200, () -> downloader.doEverything(MAX_RECORDS_TO_ASK_FOR));
                }
            } else {
                UserError.Log.d(TAG, "Already have recent reading: " + msSince(lastBg.timestamp));
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

        JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next), WakeLockTrampoline.getPendingIntent(ShareFollowService.class, Constants.SHFOLLOW_SERVICE_FAILOVER_ID));
    }

    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == SHFollow;
    }

    // remember needs proguard exclusion due to access by reflection
    public static boolean isCollecting() {
        return msSince(last_wakeup) < (Constants.MINUTE_IN_MS * 15);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetInstance(); // manage static reference life cycle
    }

    private void buggySamsungCheck() {
        if (buggySamsung == null) {
            buggySamsung = new BuggySamsung(TAG);
        }
        buggySamsung.evaluate(wakeup_time);
        wakeup_time = 0;
    }

    private static void msg(final String msg) {
        lastState = msg;
    }

    private static String getBestStatusMessage() {
        // service state overrides downloader state reply
        if (emptyString(lastState)) {
            if (downloader != null) {
                return downloader.getStatus();
            }
        } else {
            return lastState;
        }
        return null;
    }

    public synchronized static void resetInstanceAndInvalidateSession() {
        try {
            if (downloader != null) {
                downloader.invalidateSession();
            }
        } catch (Exception e) {
            // concurrency related
        }
        resetInstance();
    }

    public static void resetInstance() {
        downloader = null;
    }

    public static SpannableString nanoStatus() {
        final String current_state = getBestStatusMessage();
        return emptyString(current_state) ? null : new SpannableString(current_state);
    }
}





