package com.eveningoutpost.dexdrip.cgm.nsfollow;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem.Highlight;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder.DEXCOM_PERIOD;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.NSFollow;

/**
 * jamorham
 *
 * Nightscout follower collection service
 *
 * Handles Android wake up and polling schedule, decoupled from data transport
 *
 * Extended by Asbjørn Aarrestad - asbjorn@aarrestad.com july 2019
 */
public class NightscoutFollowService extends ForegroundService {

    private static final String TAG = "NightscoutFollow";
    private static final long SAMPLE_PERIOD = DEXCOM_PERIOD;

    protected static volatile String lastState = "";

    private static BuggySamsung buggySamsung;
    private static volatile long wakeup_time = 0;

    private static volatile BgReading lastBg;
    private static volatile long lastPoll = 0;
    private static volatile long bgReceiveDelay = 0;
    private static volatile long lastBgTime = 0;
    private static volatile Treatments lastTreatment;
    private static volatile long lastTreatmentTime = 0;
    private static volatile long treatmentReceivedDelay = 0;

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
            if (lastBg != null) {
                lastBgTime = lastBg.timestamp;
            }
            if (lastBg == null || JoH.msSince(lastBg.timestamp) > SAMPLE_PERIOD) {
                if (JoH.ratelimit("last-ns-follow-poll", 5)) {
                    Inevitable.task("NS-Follow-Work", 200, () -> {
                        NightscoutFollow.work(true);
                        lastPoll = JoH.tsl();
                    });
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

    static void updateTreatmentDownloaded() {
        lastTreatment = Treatments.lastNotFromXdrip();
        if(lastTreatment != null && lastTreatmentTime != lastTreatment.timestamp) {
            treatmentReceivedDelay = JoH.msSince(lastTreatment.timestamp);
            lastTreatmentTime = lastTreatment.timestamp;
        }
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

    /**
     * MegaStatus for Nightscout Follower
     */
    public static List<StatusItem> megaStatus() {
        final BgReading lastBg = BgReading.lastNoSenssor();

        String lastPollText = "n/a";
        if (lastPoll > 0) {
            lastPollText = JoH.niceTimeScalar(JoH.msSince(lastPoll));
        }

        long hightlightGrace = Constants.SECOND_IN_MS * 30; // 30 seconds

        // Status for BG receive delay (time from bg was recorded till received in xdrip)
        String ageOfBgLastPoll = "n/a";
        Highlight ageOfLastBgPollHighlight = Highlight.NORMAL;
        if (bgReceiveDelay > 0) {
            ageOfBgLastPoll = JoH.niceTimeScalar(bgReceiveDelay);
            if (bgReceiveDelay > SAMPLE_PERIOD / 2) {
                ageOfLastBgPollHighlight = Highlight.BAD;
            }
            if (bgReceiveDelay > SAMPLE_PERIOD * 2) {
                ageOfLastBgPollHighlight = Highlight.CRITICAL;
            }
        }

        // Status for time since latest BG
        String ageLastBg = "n/a";
        Highlight bgAgeHighlight = Highlight.NORMAL;
        if (lastBg != null) {
            long age = JoH.msSince(lastBg.timestamp);
            ageLastBg = JoH.niceTimeScalar(age);
            if (age > SAMPLE_PERIOD + hightlightGrace) {
                bgAgeHighlight = Highlight.BAD;
            }
        }

        // Status for treatments
        String ageLastTreatment = "n/a";
        String ageOfTreatmentWhenReceived = "n/a";
        if(lastTreatment != null) {
            long age = JoH.msSince(lastTreatment.timestamp);
            ageLastTreatment = JoH.niceTimeScalar(age);
            ageOfTreatmentWhenReceived = JoH.niceTimeScalar(treatmentReceivedDelay);
        }

        // Build status
        List<StatusItem> statuses = new ArrayList<>();

        statuses.add(new StatusItem("Latest BG", ageLastBg + (lastBg != null ? " ago" : ""), bgAgeHighlight));
        statuses.add(new StatusItem("BG receive delay", ageOfBgLastPoll, ageOfLastBgPollHighlight));

        if(NightscoutFollow.treatmentDownloadEnabled()) {
            statuses.add(new StatusItem());
            statuses.add(new StatusItem("Latest Treatment", ageLastTreatment + (lastTreatment != null ? " ago" : "")));
            statuses.add(new StatusItem("Treatment receive delay", ageOfTreatmentWhenReceived));
        }

        statuses.add(new StatusItem());
        statuses.add(new StatusItem("Last poll", lastPollText + (lastPoll > 0 ? " ago" : "")));
        statuses.add(new StatusItem("Next poll in", JoH.niceTimeScalar(wakeup_time - JoH.tsl())));
        if (lastBg != null) {
            statuses.add(new StatusItem("Last BG time", JoH.dateTimeText(lastBg.timestamp)));
        }
        statuses.add(new StatusItem("Next poll time", JoH.dateTimeText(wakeup_time)));
        statuses.add(new StatusItem());
        statuses.add(new StatusItem("Buggy Samsung", JoH.buggy_samsung ? "Yes" : "No"));
        statuses.add(new StatusItem("Download treatments", NightscoutFollow.treatmentDownloadEnabled() ? "Yes" : "No"));

        if (StringUtils.isNotBlank(lastState)) {
            statuses.add(new StatusItem());
            statuses.add(new StatusItem("Last state", lastState));
        }

        return statuses;
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
