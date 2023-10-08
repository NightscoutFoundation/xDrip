package com.eveningoutpost.dexdrip.cgm.carelinkfollow;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.DEXCOM_PERIOD;

/**
 * CareLink Follow Service
 * - main service class for managing CareLink Connect data retrieval
 * - start/stop data retrieval
 * - provide status infos of follower service
 */

public class CareLinkFollowService extends ForegroundService {

    private static final String TAG = "CareLinkFollow";
    private static final long SAMPLE_PERIOD = DEXCOM_PERIOD;

    protected static volatile String lastState = "";

    private static BuggySamsung buggySamsung;
    private static volatile long wakeup_time = 0;
    private static volatile long last_wakeup = 0;
    private static volatile long lastPoll = 0;
    private static volatile BgReading lastBg;
    private static volatile long bgReceiveDelay;
    private static volatile long lastBgTime;

    private static CareLinkFollowDownloader downloader;
    private static volatile int gracePeriod = 0;
    private static volatile int missedPollInterval = 0;


    @Override
    public void onCreate() {
        super.onCreate();
        resetInstance(); // manage static reference life cycle
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
        gracePeriod = 0;
        missedPollInterval = 0;
    }

    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == DexCollectionType.CLFollow;
    }

    private static long getGraceMillis() {
        return Constants.SECOND_IN_MS * gracePeriod;
    }

    private static long getMissedIntervalMillis() {
        if (missedPollInterval == 0)
            return SAMPLE_PERIOD;
        else
            return Constants.MINUTE_IN_MS * missedPollInterval;
    }

    static void scheduleWakeUp() {
        final BgReading lastBg = BgReading.lastNoSenssor();
        final long last = lastBg != null ? lastBg.timestamp : 0;

        final long next = anticipateNextWakeUp(JoH.tsl(), last, SAMPLE_PERIOD, getGraceMillis(), getMissedIntervalMillis());
        wakeup_time = next;
        UserError.Log.d(TAG, "Anticipate next: " + JoH.dateTimeText(next) + "  last BG timestamp: " + JoH.dateTimeText(last));

        JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next), WakeLockTrampoline.getPendingIntent(CareLinkFollowService.class, Constants.CARELINK_SERVICE_FAILOVER_ID));
    }

    public static long anticipateNextWakeUp(long now, final long last, final long period, final long grace, final long missedInterval) {

        long next;

        //recent reading (less then data period) => last + period + grace
        if ((now - last) < period) {
            next = last + period + grace;
        }
        //old reading => anticipated next + grace
        else {
            //last expected
            next = now + ((last - now) % period);
            //add missed poll interval until future time is reached
            while (next < now) {
                next += missedInterval;
            }
            //add grace
            next += grace;
        }

        return next;

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("CareLinkFollow-osc", 60000);
        try {

            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP");

            // Check service should be running
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                msg("Stopping");
                stopSelf();
                return START_NOT_STICKY;
            }
            buggySamsungCheck();

            last_wakeup = JoH.tsl();

            // Check current
            if (gracePeriod == 0)
                gracePeriod = Pref.getStringToInt("clfollow_grace_period", 30);
            if (missedPollInterval == 0)
                missedPollInterval = Pref.getStringToInt("clfollow_missed_poll_interval", 5);
            lastBg = BgReading.lastNoSenssor();
            if (lastBg != null) {
                lastBgTime = lastBg.timestamp;
            }
            if (lastBg == null || msSince(lastBg.timestamp) > SAMPLE_PERIOD) {
                // Get the data
                if (downloader == null) {
                    downloader = new CareLinkFollowDownloader(
                            Pref.getString("clfollow_user", ""),
                            Pref.getString("clfollow_pass", ""),
                            Pref.getString("clfollow_country", "").toLowerCase(),
                            Pref.getString("clfollow_patient", "")
                    );
                }

                if (JoH.ratelimit("last-carelink-follow-poll", 5)) {
                    Inevitable.task("CareLink-Follow-Work", 200, () -> {
                        try {
                            downloader.doEverything();
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

    /**
     * MegaStatus for Connect Follower
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
        megaStatus.add(new StatusItem("Data period:", JoH.niceTimeScalar(SAMPLE_PERIOD)));
        megaStatus.add(new StatusItem("Grace period:", JoH.niceTimeScalar(getGraceMillis())));
        megaStatus.add(new StatusItem("Missed poll interval:", JoH.niceTimeScalar(getMissedIntervalMillis())));
        megaStatus.add(new StatusItem());
        megaStatus.add(new StatusItem("Last poll", lastPoll > 0 ? JoH.niceTimeScalar(JoH.msSince(lastPoll)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Last wakeup", last_wakeup > 0 ? JoH.niceTimeScalar(JoH.msSince(last_wakeup)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Next poll in", JoH.niceTimeScalar(wakeup_time - JoH.tsl())));
        if (lastBg != null) {
            megaStatus.add(new StatusItem("Last BG time", JoH.dateTimeText(lastBg.timestamp)));
        }
        megaStatus.add(new StatusItem("Last poll time", lastPoll > 0 ? JoH.dateTimeText(lastPoll) : "n/a"));
        megaStatus.add(new StatusItem("Next poll time", JoH.dateTimeText(wakeup_time)));
        megaStatus.add(new StatusItem());
        megaStatus.add(new StatusItem("Buggy handset", JoH.buggy_samsung ? "Yes" : "No"));

        return megaStatus;
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

    public static SpannableString nanoStatus() {
        final String current_state = getBestStatusMessage();
        return emptyString(current_state) ? null : new SpannableString(current_state);
    }

}