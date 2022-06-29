package com.eveningoutpost.dexdrip.cgm.sharefollow;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.MAX_RECORDS_TO_ASK_FOR;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.NON_US_SHARE_BASE_URL;
import static com.eveningoutpost.dexdrip.cgm.sharefollow.ShareConstants.US_SHARE_BASE_URL;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.SHFollow;
import static com.eveningoutpost.dexdrip.xdrip.gs;

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
    private static volatile long lastPoll = 0;
    private static volatile BgReading lastBg;
    private static volatile long bgReceiveDelay;
    private static volatile long lastBgTime;

    private static ShareFollowDownload downloader;

    @Override
    public void onCreate() {
        super.onCreate();
        resetInstance(); // manage static reference life cycle
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("SHFollow-osc", 60_000);
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
            if (lastBg != null) {
                lastBgTime = lastBg.timestamp;
            }
            if (lastBg == null || msSince(lastBg.timestamp) > SAMPLE_PERIOD) {
                // Get the data
                if (downloader == null) {
                    downloader = new ShareFollowDownload(
                            Pref.getBoolean("dex_share_us_acct", true) ? US_SHARE_BASE_URL : NON_US_SHARE_BASE_URL,
                            Pref.getString("shfollow_user", ""), Pref.getString("shfollow_pass", ""));
                }

                if (JoH.ratelimit("last-sh-follow-poll", 5)) {
                    Inevitable.task("SH-Follow-Work", 200, () -> {
                        try {
                            downloader.doEverything(MAX_RECORDS_TO_ASK_FOR);
                        } catch (NullPointerException e) {
                            UserError.Log.e(TAG, "Caught concurrency exception when trying to run doeverything");
                        }
                    });
                    lastPoll = JoH.tsl();
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

    /**
     * MegaStatus for Nightscout Follower
     */
    public static List<StatusItem> megaStatus() {
        final BgReading lastBg = BgReading.lastNoSenssor();

        long hightlightGrace = Constants.SECOND_IN_MS * 30; // 30 seconds

        // Status for BG receive delay (time from bg was recorded till received in xdrip)
        String ageOfBgLastPoll = "n/a";
        StatusItem.Highlight ageOfLastBgPollHighlight = StatusItem.Highlight.NORMAL;
        if (bgReceiveDelay > 0) {
            ageOfBgLastPoll = JoH.niceTimeScalar(bgReceiveDelay);
            if (bgReceiveDelay > SAMPLE_PERIOD / 2) {
                ageOfLastBgPollHighlight = StatusItem.Highlight.BAD;
            }
            if (bgReceiveDelay > SAMPLE_PERIOD * 2) {
                ageOfLastBgPollHighlight = StatusItem.Highlight.CRITICAL;
            }
        }

        // Status for time since latest BG
        String ageLastBg = "n/a";
        StatusItem.Highlight bgAgeHighlight = StatusItem.Highlight.NORMAL;
        if (lastBg != null) {
            long age = JoH.msSince(lastBg.timestamp);
            ageLastBg = JoH.niceTimeScalar(age);
            if (age > SAMPLE_PERIOD + hightlightGrace) {
                bgAgeHighlight = StatusItem.Highlight.BAD;
            }
        }

        List<StatusItem> megaStatus = new ArrayList<>();

        megaStatus.add(new StatusItem("Latest BG", ageLastBg + (lastBg != null ? " ago" : ""), bgAgeHighlight));
        megaStatus.add(new StatusItem("BG receive delay", ageOfBgLastPoll, ageOfLastBgPollHighlight));
        megaStatus.add(new StatusItem("Last poll", lastPoll > 0 ? JoH.niceTimeScalar(JoH.msSince(lastPoll)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Last wakeup", last_wakeup > 0 ? JoH.niceTimeScalar(JoH.msSince(last_wakeup)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Next poll in", JoH.niceTimeScalar(wakeup_time - JoH.tsl())));
        if (lastBg != null) {
            megaStatus.add(new StatusItem("Last BG time", JoH.dateTimeText(lastBg.timestamp)));
        }
        megaStatus.add(new StatusItem("Next poll time", JoH.dateTimeText(wakeup_time)));
        megaStatus.add(new StatusItem("Buggy Samsung", JoH.buggy_samsung ? gs(R.string.yes) : gs(R.string.no)));

        return megaStatus;
    }

    /**
     * Update observedDelay if new bg reading is available
     */
    static void updateBgReceiveDelay() {
        lastBg = BgReading.lastNoSenssor();
        if (lastBg != null && lastBgTime != lastBg.timestamp) {
            bgReceiveDelay = JoH.msSince(lastBg.timestamp);
            lastBgTime = lastBg.timestamp;
        }
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





