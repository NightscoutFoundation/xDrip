package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.SpannableString;

import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.cgm.nsfollow.utils.Anticipate;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.BuggySamsung;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.List;

public class MedtrumFollowService extends ForegroundService {

    private static final String TAG = "MedtrumFollow";
    private static final long SAMPLE_PERIOD = 2 * Constants.MINUTE_IN_MS;

    private static volatile long wakeupTime;
    private static volatile long lastWakeup;
    private static volatile long lastPoll;
    private static volatile long bgReceiveDelay;
    private static volatile long lastBgTime;
    private static BuggySamsung buggySamsung;
    private static MedtrumFollowDownloader downloader;

    @Override
    public void onCreate() {
        super.onCreate();
        resetInstance();
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        final PowerManager.WakeLock wakeLock = JoH.getWakeLock("MedtrumFollow-osc", 60_000);
        try {
            if (DexCollectionType.getDexCollectionType() != DexCollectionType.MedtrumFollow) {
                stopSelf();
                return START_NOT_STICKY;
            }
            if (buggySamsung == null) buggySamsung = new BuggySamsung(TAG);
            buggySamsung.evaluate(wakeupTime);
            wakeupTime = 0;
            lastWakeup = JoH.tsl();
            scheduleWakeUp();

            final ConnectivityManager connectivityManager =
                    (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null || connectivityManager.getActiveNetwork() == null) {
                UserError.Log.d(TAG, "No network available; skipping poll");
                return START_STICKY;
            }

            final BgReading last = BgReading.lastNoSenssor();
            if (last == null || JoH.msSince(last.timestamp) > SAMPLE_PERIOD) {
                if (JoH.ratelimit("last-medtrum-follow-poll", 5)) {
                    Inevitable.task("Medtrum-Follow-Work", 200, () -> {
                        getDownloader().download();
                        lastPoll = JoH.tsl();
                    });
                }
            } else {
                UserError.Log.d(TAG, "Already have recent reading: " + JoH.msSince(last.timestamp));
            }
        } finally {
            JoH.releaseWakeLock(wakeLock);
        }
        return START_STICKY;
    }

    private static synchronized MedtrumFollowDownloader getDownloader() {
        if (downloader == null) downloader = new MedtrumFollowDownloader();
        return downloader;
    }

    public static synchronized void resetInstanceAndInvalidateSession() {
        if (downloader != null) downloader.invalidateSession();
        resetInstance();
    }

    public static synchronized void resetInstance() {
        downloader = null;
    }

    static void scheduleWakeUp() {
        final BgReading lastBg = BgReading.lastNoSenssor();
        final long last = lastBg == null ? 0 : lastBg.timestamp;
        final long grace = 10 * Constants.SECOND_IN_MS;
        final long next = Anticipate.next(JoH.tsl(), last, SAMPLE_PERIOD, grace) + grace;
        wakeupTime = next;
        JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next),
                WakeLockTrampoline.getPendingIntent(MedtrumFollowService.class,
                        Constants.MEDTRUM_FOLLOW_SERVICE_FAILOVER_ID));
    }

    static void updateBgReceiveDelay() {
        final BgReading lastBg = BgReading.lastNoSenssor();
        if (lastBg != null && lastBg.timestamp != lastBgTime) {
            bgReceiveDelay = JoH.msSince(lastBg.timestamp);
            lastBgTime = lastBg.timestamp;
        }
    }

    public static boolean isCollecting() {
        return JoH.msSince(lastWakeup) < 15 * Constants.MINUTE_IN_MS;
    }

    public static List<StatusItem> megaStatus() {
        final List<StatusItem> statuses = new ArrayList<>();
        final BgReading lastBg = BgReading.lastNoSenssor();
        statuses.add(new StatusItem(gs(R.string.latest_bg), lastBg == null ? gs(R.string.n_a)
                : JoH.niceTimeScalar(JoH.msSince(lastBg.timestamp)) + gs(R.string.ago)));
        statuses.add(new StatusItem(gs(R.string.bg_receive_delay), bgReceiveDelay <= 0 ? gs(R.string.n_a)
                : JoH.niceTimeScalar(bgReceiveDelay)));
        statuses.add(new StatusItem(gs(R.string.last_poll), lastPoll <= 0 ? gs(R.string.n_a)
                : JoH.niceTimeScalar(JoH.msSince(lastPoll)) + gs(R.string.ago)));
        statuses.add(new StatusItem(gs(R.string.next_poll_in), wakeupTime <= 0 ? gs(R.string.n_a)
                : JoH.niceTimeScalar(wakeupTime - JoH.tsl())));
        if (downloader != null && !downloader.getSelectedPatient().isEmpty()) {
            statuses.add(new StatusItem(gs(R.string.easyview_patient), downloader.getSelectedPatient()));
        }
        if (downloader != null && !downloader.getStatus().isEmpty()) {
            statuses.add(new StatusItem(gs(R.string.last_state), downloader.getStatus()));
        }
        return statuses;
    }

    public static SpannableString nanoStatus() {
        if (downloader == null || downloader.getStatus().isEmpty()) return null;
        return new SpannableString(downloader.getStatus());
    }

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetInstance();
    }
}

