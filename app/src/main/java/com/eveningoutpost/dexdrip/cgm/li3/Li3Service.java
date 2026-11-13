package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.SpannableString;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import xrwd.li3.Li3SensorState;
import xrwd.li3.ble.Li3Ble;
import xrwd.li3.crypto.AppIdentity;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;

/**
 * Keeps a Bluetooth connection to the Li3 that was started or taken over by NFC. Without a stored sensor or a
 * downloaded credential it waits; a new sensor stored by NFC replaces the connection on the next wake-up.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3Service extends ForegroundService {

    private static final String TAG = "Li3";
    /**
     * How long after the wear end the collector keeps connecting. An older sensor sends for 12 hours after its wear
     * ends; that Li3 does the same is an assumption, so this errs on the side of a little extra Bluetooth over lost
     * readings.
     */
    private static final long ENDED_GRACE_MS = 12 * HOUR_IN_MS;

    private static volatile String stage = "";
    private static volatile String lastError = "";
    private static volatile long lastReadingAt;
    /** When the sensor last sent a reading, stored or refused; 0 when none came. */
    private static volatile long lastReceivedAt;
    /** Why the last reading was refused, or null when it was stored or none came. */
    private static volatile String lastRefusal;
    private static volatile Li3Readings readings;
    /** The running Bluetooth session, for the diagnostics report; null when none runs. */
    private static volatile Session runningSession;

    private Li3Ble ble;
    private Session session;
    private String bleSerial;
    private long wakeupTime;
    private boolean offeredCredential;

    /**
     * One Bluetooth session's listener. It keeps the readings of its own sensor, so a value still arriving from an old
     * session after a new sensor was scanned is checked against the old sensor and not stored on the new one.
     */
    static final class Session implements Li3Ble.Listener {

        /** Longer than the longest retry wait plus a scan and a connect, so a working session is never cut. */
        private static final long STALL_MS = 12 * MINUTE_IN_MS;
        private static final int RECENT_STAGES = 20;
        private static final String RETRY_PREFIX = "Reconnecting in ";

        private final Li3Readings readings;
        /** When the session last reported a stage or a value; 0 when it never did. */
        private volatile long lastProgressAt;
        private final long startedAt = tsl();
        private volatile boolean locationNotified;
        /** Set when the service stops the session; guarded by this. */
        private boolean stopped;
        /** Written by the Bluetooth worker, read by the diagnostics report; guarded by this. */
        private final LinkedList<String> recentStages = new LinkedList<>();
        private final Map<String, Integer> retryCounts = new LinkedHashMap<>();

        Session(Li3Readings readings) {
            this.readings = readings;
        }

        long startedAt() {
            return startedAt;
        }

        /** Its Bluetooth worker may still hand over a frame it was opening; that reading no longer sets the status. */
        synchronized void stop() {
            stopped = true;
        }

        /** @return the newest stages, oldest first, each with its time */
        synchronized List<String> recentStages() {
            return new ArrayList<>(recentStages);
        }

        /** @return how often the session retried, by reason, in the order the reasons were first seen */
        synchronized Map<String, Integer> retryCounts() {
            return new LinkedHashMap<>(retryCounts);
        }

        private synchronized void remember(String newStage) {
            recentStages.add(dateTimeText(tsl()) + " " + newStage);
            while (recentStages.size() > RECENT_STAGES) {
                recentStages.removeFirst();
            }
            final int reasonAt = newStage.indexOf(": ");
            if (newStage.startsWith(RETRY_PREFIX) && reasonAt >= 0) {
                final String reason = newStage.substring(reasonAt + 2);
                final Integer count = retryCounts.get(reason);
                retryCounts.put(reason, count == null ? 1 : count + 1);
            }
        }

        /**
         * @return true when the session has not moved on for {@link #STALL_MS}, e.g. a retry timer that Doze held
         * back or a failure that never asked for a retry
         */
        boolean stalled(long nowMs) {
            return lastProgressAt == 0 || nowMs - lastProgressAt > STALL_MS;
        }

        @Override
        public void onStage(String newStage) {
            lastProgressAt = tsl();
            stage = newStage;
            // the refusal was for a reading on the link as it was; the next refused reading shows it again
            lastRefusal = null;
            remember(newStage);
            UserError.Log.d(TAG, "Stage: " + newStage);
        }

        @Override
        public void onPaired(Li3SensorState state) {
            if (!Li3Store.updateState(state)) {
                UserError.Log.e(TAG, "Li3 " + state.serial() + " paired but is no longer the stored sensor");
                return;
            }
            UserError.Log.uel(TAG, "Li3 " + state.serial() + " paired");
        }

        /**
         * A reading shows the connection works again, so the last error no longer shows on the status page. Even a
         * refused reading counts as collecting, so the missed-reading check does not restart a working connection.
         */
        @Override
        public void onReading(RealtimeReading reading) {
            lastProgressAt = tsl();
            lastReceivedAt = tsl();
            lastError = "";
            if (locationNotified) {
                JoH.cancelNotification(Constants.LI3_LOCATION_NOTIFICATION_ID);
            }
            if (readings.onRealtime(reading, tsl())) {
                lastReadingAt = tsl();
            }
            synchronized (this) {
                if (!stopped) {
                    lastRefusal = readings.lastRefusal();
                }
            }
        }

        @Override
        public void onStatus(PatchStatus status) {
            lastProgressAt = tsl();
            UserError.Log.d(TAG, "Status: life " + status.currentLifeCount() + " state " + status.state());
            readings.onStatus(status);
        }

        @Override
        public void onHistory(List<HistoricalPage.Entry> entries) {
            UserError.Log.d(TAG, "History entries: " + entries.size());
            readings.onHistory(entries, tsl());
        }

        @Override
        public void onError(String message) {
            lastError = message;
            UserError.Log.uel(TAG, "Li3 error: " + message);
        }

        /**
         * Notifies once per session: the scan retries every few seconds, and a dismissed notification must stay
         * dismissed. The Li3 screen asks for the permission and for location to be turned on.
         */
        @Override
        public void onLocationNeeded() {
            if (locationNotified) {
                return;
            }
            locationNotified = true;
            final Context context = xdrip.getAppContext();
            final Intent intent = new Intent(context, Li3Activity.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            JoH.showNotification(gs(R.string.li3_title), gs(R.string.li3_location_needed), pendingIntent,
                    Constants.LI3_LOCATION_NOTIFICATION_ID, false, false, false);
        }

        /**
         * Turns Bluetooth on when the Turn Bluetooth on setting allows it, as the Dexcom collector does. At most once
         * every 30 seconds: Bluetooth takes a few seconds to come on, and the retries come sooner than that.
         */
        @Override
        public void onBluetoothOff() {
            if (Pref.getBoolean("automatically_turn_bluetooth_on", true)
                    && JoH.ratelimit("li3-enabling-bluetooth", 30)) {
                UserError.Log.uel(TAG, "Bluetooth is off; turning it on");
                JoH.setBluetoothEnabled(xdrip.getAppContext(), true);
            }
        }

        /**
         * Kept in xDrip's log only with extra logging on for this tag. Never as an error or a warning: an out-of-range
         * sensor fails a connect every few minutes, and those must not fill a normal user's error list.
         */
        @Override
        public void onLog(int priority, String message) {
            final String line = "BLE: " + message;
            if (priority == android.util.Log.VERBOSE) {
                UserError.Log.v(TAG, line);
            } else if (priority == android.util.Log.DEBUG) {
                UserError.Log.d(TAG, line);
            } else {
                UserError.Log.i(TAG, line);
            }
        }
    }

    /** Logged with extra logging on, so a restart of the collector shows as the service ending and starting. */
    @Override
    public void onCreate() {
        super.onCreate();
        UserError.Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG + "-osc", 60_000);
        try {
            UserError.Log.d(TAG, ble == null ? "Start command, no session running"
                    : "Start command, session running for " + bleSerial);
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                stopSelf();
                return START_NOT_STICKY;
            }
            offerCredential();
            startIfNeeded();
            scheduleWakeUp();
        } finally {
            JoH.releaseWakeLock(wl);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        UserError.Log.d(TAG, "Service destroyed");
        stopBle();
        super.onDestroy();
    }

    /**
     * Without a credential, notifies once per start of the collector. The notification opens the Li3 screen, which
     * offers the download, and goes away when the credential is kept.
     */
    private void offerCredential() {
        if (!offeredCredential && Li3Credential.get(this).load().isEmpty()) {
            offeredCredential = true;
            final Intent intent = new Intent(this, Li3Activity.class);
            final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            JoH.showNotification(gs(R.string.li3_title), gs(R.string.li3_stage_no_credential), pendingIntent,
                    Constants.LI3_CREDENTIAL_NOTIFICATION_ID, false, false, false);
        }
    }

    private void startIfNeeded() {
        Li3SensorState state = Li3Store.loadState();
        if (state == null) {
            stopBle();
            stage = gs(R.string.li3_stage_no_sensor);
            return;
        }
        if (ended(state)) {
            if (ble != null) {
                UserError.Log.uel(TAG, "Li3 " + state.serial() + " ended, no longer connecting to it");
            }
            stopBle();
            stage = gs(R.string.li3_stage_sensor_ended);
            return;
        }
        Li3Store.ensureSensor(state);
        int fromAttempt = 0;
        if (ble != null && state.serial().equals(bleSerial)) {
            if (!session.stalled(tsl())) {
                // a retry or scan timer the phone slept through runs now instead of when the phone wakes for other work
                ble.runOverdue();
                return;
            }
            UserError.Log.uel(TAG, "Li3 connection made no progress, restarting it");
            fromAttempt = ble.reconnectAttempt();
        }
        List<AppIdentity> identities = Li3Credential.get(this).load();
        if (identities.isEmpty()) {
            stopBle();
            stage = gs(R.string.li3_stage_no_credential);
            return;
        }
        stopBle();
        Li3Readings sessionReadings = new Li3Readings(state);
        readings = sessionReadings;
        session = new Session(sessionReadings);
        runningSession = session;
        ble = new Li3Ble(xdrip.getAppContext(), state, identities, session);
        bleSerial = state.serial();
        UserError.Log.uel(TAG, "Connecting to Li3 " + state.serial());
        ble.start(fromAttempt);
    }

    /** Whether the sensor's wear ended more than {@link #ENDED_GRACE_MS} ago, so it no longer sends readings. */
    private static boolean ended(Li3SensorState state) {
        return state.activationMs() > 0 && tsl() > state.activationMs() + Li3Store.getWearMs() + ENDED_GRACE_MS;
    }

    /** @return the running Bluetooth session, or null when none runs */
    @Nullable
    static Session currentSession() {
        return runningSession;
    }

    private void stopBle() {
        if (session != null) {
            session.stop();
        }
        // the refusal was the stopped session's sensor's
        lastRefusal = null;
        if (ble != null) {
            ble.stop();
            ble = null;
            session = null;
            runningSession = null;
            bleSerial = null;
            readings = null;
        }
    }

    private void scheduleWakeUp() {
        long next = tsl() + MINUTE_IN_MS * 5;
        if (Math.abs(next - wakeupTime) > SECOND_IN_MS * 10) {
            wakeupTime = next;
            JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next),
                    WakeLockTrampoline.getPendingIntent(Li3Service.class, Constants.LI3_SERVICE_FAILOVER_ID));
        }
    }

    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == DexCollectionType.Li3;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** MegaStatus rows: the sensor, where the connection is, and a button to the Li3 screen. */
    public static List<StatusItem> megaStatus() {
        List<StatusItem> si = new ArrayList<>();
        Li3SensorState state = Li3Store.loadState();
        // a forgotten sensor's session can still be in the statics until the collector restart has run
        Li3Readings current = state == null ? null : readings;
        if (current != null) {
            si.addAll(current.megaStatus());
        } else {
            si.add(new StatusItem(gs(R.string.li3_sensor),
                    state == null ? gs(R.string.li3_none) : state.serial()));
        }
        String shownStage = state == null ? gs(R.string.li3_stage_no_sensor) : stage;
        if (!shownStage.isEmpty()) {
            si.add(new StatusItem(gs(R.string.li3_stage), shownStage));
        }
        if (!lastError.isEmpty()) {
            si.add(new StatusItem(gs(R.string.li3_last_error), lastError, StatusItem.Highlight.BAD));
        }
        String refusal = lastRefusal;
        if (state != null && refusal != null) {
            si.add(new StatusItem(gs(R.string.li3_reading_not_stored), refusal, StatusItem.Highlight.BAD));
        }
        if (state != null && state.activationMs() > 0) {
            si.add(new StatusItem(gs(R.string.li3_started), dateTimeText(state.activationMs())));
            long left = state.activationMs() + Li3Store.getWearMs() - tsl();
            // the home screen's text, so both say the same
            String ends = left > 0 ? SensorDays.get().getSpannable().toString() : gs(R.string.li3_expired);
            if (!ends.isEmpty()) {
                si.add(new StatusItem(gs(R.string.li3_remaining), ends));
            }
        }
        if (lastReadingAt > 0) {
            si.add(new StatusItem(gs(R.string.li3_last_reading), niceTimeScalar(msSince(lastReadingAt))));
        }
        Li3Credential stored = Li3Credential.get(xdrip.getAppContext());
        boolean credential = !stored.load().isEmpty();
        si.add(new StatusItem(gs(R.string.li3_credential),
                credential ? gs(R.string.li3_credential_ready) : gs(R.string.li3_none),
                credential ? StatusItem.Highlight.NORMAL : StatusItem.Highlight.BAD));
        String downloadError = stored.lastDownloadError();
        if (!credential && !downloadError.isEmpty()) {
            si.add(new StatusItem(gs(R.string.li3_download_failed), downloadError, StatusItem.Highlight.BAD));
        }
        si.add(new StatusItem(gs(R.string.li3_title), gs(R.string.li3_open), StatusItem.Highlight.NORMAL,
                "button-press", Li3Store::startControlActivity));
        return si;
    }

    /** Clears when the last reading came, as at the start of the process. */
    @VisibleForTesting
    static void forgetReadingTimes() {
        lastReadingAt = 0;
        lastReceivedAt = 0;
    }

    // accessed via reflection
    public static boolean isCollecting() {
        return recent(lastReceivedAt);
    }

    /** Whether a reading was stored in the last 12 minutes; a refused one does not count. */
    static boolean hasRecentReading() {
        return recent(lastReadingAt);
    }

    /** Whether a time is set and less than 12 minutes ago. */
    private static boolean recent(long at) {
        return at > 0 && msSince(at) < MINUTE_IN_MS * 12;
    }

    // accessed via reflection
    public static SpannableString nanoStatus() {
        String refusal = lastRefusal;
        if (refusal != null) {
            // the stage says the sensor streams, while no reading from it is stored
            return new SpannableString(gs(R.string.li3_reading_not_stored) + ": " + refusal);
        }
        // a refused reading counts as collecting, but only a stored one hides the stage
        if (stage.isEmpty() || recent(lastReadingAt)) {
            return null;
        }
        // the home screen says where the setup steps are; the Li3 screen shows them next to the stage
        return new SpannableString(stage.equals(gs(R.string.li3_stage_no_sensor))
                ? gs(R.string.li3_home_no_sensor) : stage);
    }
}
