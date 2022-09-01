package com.eveningoutpost.dexdrip.cgm.webfollow;

import static com.eveningoutpost.dexdrip.Models.JoH.clearRatelimit;
import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.WebFollow;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Generic Web follower collection service
 * <p>
 * Handles Android wake up and polling schedule, decoupled from data transport
 */

public class WebFollowService extends ForegroundService {

    private static final String TAG = "WebFollow";
    private static final long SAMPLE_PERIOD = DexCollectionType.getCollectorSamplePeriod(WebFollow);

    protected static volatile String lastState = "";
    protected static volatile String lastError = "";
    protected static volatile long lastErrorTime = 0;

    private static BuggySamsung buggySamsung;
    private static volatile long wakeup_time = 0;
    private static volatile long last_wakeup = 0;
    private static volatile long lastPoll = 0;
    private static volatile BgReading lastBg;
    private static volatile long lastBgTime;

    private static volatile MContext context;

    @Override
    public void onCreate() {
        super.onCreate();
        resetInstance(); // manage static reference life cycle
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock("WebFollow-osc", 60_000);
        try {
            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP");
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                stopSelf();
                return START_NOT_STICKY;
            }
            buggySamsungCheck();
            last_wakeup = JoH.tsl();
            Sensor.createDefaultIfMissing();
            lastBg = BgReading.lastNoSenssor();
            if (lastBg != null) {
                lastBgTime = lastBg.timestamp;
            }

            if (intent != null) {
                val function = intent.getStringExtra("function");
                if (function != null) {
                    UserError.Log.d(TAG, "Processing function: " + function);
                    switch (function) {
                        case "refresh":
                        case "fullrefresh":
                            MContext.invalidate(function.equals("fullrefresh"));
                            resetInstance();
                            clearRatelimit("last-web-follow-poll");
                            break;
                    }
                }
            }

            if (lastBg == null || msSince(lastBg.timestamp) > SAMPLE_PERIOD) {
                if (context == null) {
                    synchronized (WebFollowService.class) {
                        context = MContext.revive();
                        if (context == null) {
                            UserError.Log.d(TAG, "Loading template from source");
                            Inevitable.task("webfollow-template", 200, () -> {
                                context = Template.get();
                                if (context != null) {
                                    context.save("template");
                                    JoH.startService(WebFollowService.class, "function", "template");
                                } else {
                                    lastError = "Could not get template";
                                    lastErrorTime = JoH.tsl();
                                }
                            });
                        } else {
                            UserError.Log.d(TAG, "context revived successfully");
                        }
                    }
                }
                if (context != null) {
                    if (JoH.pratelimit("last-web-follow-poll", 180)) {
                        Inevitable.task("WebFollow-Work", 200, () -> {
                            try {
                                new Cmd(context).processAll();
                                if (context.lastError != null) {
                                    lastErrorTime = context.lastErrorTime;
                                    lastError = context.lastError;
                                }
                            } catch (NullPointerException e) {
                                UserError.Log.e(TAG, "Caught concurrency exception");
                                e.printStackTrace();
                            }
                            scheduleWakeUp();
                        });
                        lastPoll = JoH.tsl();
                    } else {
                        UserError.Log.d(TAG, "Skipping due to ratelimit");
                    }
                } else {
                    UserError.Log.d(TAG, "No context so cannot proceed");
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
        final long prop_delay = Constants.SECOND_IN_MS * 30;
        final long next = Anticipate.next(JoH.tsl(), last, SAMPLE_PERIOD, 0) + prop_delay;
        wakeup_time = next;
        UserError.Log.d(TAG, "Anticipate next: " + JoH.dateTimeText(next) + "  last: " + JoH.dateTimeText(last));
        JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next), WakeLockTrampoline.getPendingIntent(WebFollowService.class, Constants.WEBFOLLOW_SERVICE_FAILOVER_ID));
    }

    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == WebFollow;
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
        if (emptyString(lastState)) {

        } else {
            return lastState;
        }
        return null;
    }

    /**
     * MegaStatus for Web Follower
     */
    public static List<StatusItem> megaStatus() {
        final BgReading lastBg = BgReading.lastNoSenssor();

        long hightlightGrace = Constants.SECOND_IN_MS * 30; // 30 seconds

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
        megaStatus.add(new StatusItem("Latest reading", ageLastBg + (lastBg != null ? " ago" : ""), bgAgeHighlight));
        megaStatus.add(new StatusItem("Last poll", lastPoll > 0 ? JoH.niceTimeScalar(JoH.msSince(lastPoll)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Last wakeup", last_wakeup > 0 ? JoH.niceTimeScalar(JoH.msSince(last_wakeup)) + " ago" : "n/a"));
        megaStatus.add(new StatusItem("Next poll in", JoH.niceTimeScalar(Math.max(wakeup_time - JoH.tsl(), 0))));
        if (lastBg != null) {
            megaStatus.add(new StatusItem("Last reading time", JoH.dateTimeText(lastBg.timestamp)));
        }
        megaStatus.add(new StatusItem("Next poll time", JoH.dateTimeText(wakeup_time)));

        if (JoH.buggy_samsung) {
            megaStatus.add(new StatusItem("Buggy Samsung", JoH.buggy_samsung ? gs(R.string.yes) : gs(R.string.no)));
        }

        if (lastErrorTime != 0L && (msSince(lastErrorTime) < Constants.HOUR_IN_MS)) {
            megaStatus.add(new StatusItem("Last Error", lastError, StatusItem.Highlight.BAD));
            megaStatus.add(new StatusItem("Last Error time", JoH.niceTimeScalar(JoH.msSince(lastErrorTime))));
        }

        return megaStatus;
    }

    public static void resetInstance() {
        synchronized (WebFollowService.class) {
            context = null;
            lastBg = null;
        }
    }

    public static SpannableString nanoStatus() {
        final String current_state = getBestStatusMessage();
        return emptyString(current_state) ? null : new SpannableString(current_state);
    }
}
