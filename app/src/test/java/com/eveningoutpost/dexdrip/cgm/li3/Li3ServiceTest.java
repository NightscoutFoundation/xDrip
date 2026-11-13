package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.text.SpannableString;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.shadows.ShadowNotificationManager;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.util.Hex;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Tests for {@link Li3Service.Session}: each Bluetooth session hands its values to the readings of its own sensor.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3ServiceTest extends RobolectricTestWithConfig {

    /** A US 15-day Li3: 60 minutes warm-up, 21600 minutes wear. */
    private static final String PATCH_INFO = "00a50001000200010060541e020401040c0130525243393839415151ff17";

    private long now;

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        // DexCollectionType reads the one-minute setting once, when the class loads, and keeps it for every later test
        // in the run: load it here with the setting off, before any test turns it on
        Pref.removeItem("libre_one_minute");
        DexCollectionType.getDexCollectionType();
        Li3Store.forgetSensor();
        Pref.setBoolean("libre_one_minute", true);
        now = tsl();
    }

    @After
    public void tearDown() {
        Pref.removeItem("dex_collection_method");
        Pref.removeItem("libre_one_minute");
        Pref.removeItem("automatically_turn_bluetooth_on");
        JoH.clearRatelimit("li3-enabling-bluetooth");
        Li3Store.forgetSensor();
        SensorDays.clearCache();
        UserError.ExtraLogTags.readPreference("");
    }

    // ==================================================== Session ====================================================

    /**
     * A reading still arriving from the old sensor's session after a new sensor was scanned is not stored, not even
     * at the new sensor's times.
     */
    @Test
    public void onReading_oldSessionAfterNewSensor_notStored() {
        // :: Setup
        Li3SensorState old = sensor("0RRC989AQ", now - 1010 * MINUTE_IN_MS);
        Li3Service.Session oldSession = new Li3Service.Session(new Li3Readings(old));
        Li3SensorState replacement = sensor("0RRCNEW00", now - 2000 * MINUTE_IN_MS);
        new Li3Service.Session(new Li3Readings(replacement));

        // :: Act
        oldSession.onReading(reading(1000, 120));

        // :: Verify
        assertThat(BgReading.getForPreciseTimestamp(old.activationMs() + 1000 * MINUTE_IN_MS, 1000)).isNull();
        assertThat(BgReading.getForPreciseTimestamp(replacement.activationMs() + 1000 * MINUTE_IN_MS, 1000)).isNull();
    }

    /** The current sensor's session stores its readings. */
    @Test
    public void onReading_currentSession_stored() {
        // :: Setup
        Li3SensorState current = sensor("0RRC989AQ", now - 1010 * MINUTE_IN_MS);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(current));

        // :: Act
        session.onReading(reading(1000, 120));

        // :: Verify
        BgReading stored = BgReading.getForPreciseTimestamp(current.activationMs() + 1000 * MINUTE_IN_MS, 1000);
        assertThat(stored.calculated_value).isEqualTo(120.0);
    }

    /** With extra logging on for the collector, a Bluetooth debug line is kept in xDrip's log, where a tester can send it. */
    @Test
    public void onLog_debugWithExtraTag_isStored() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onLog(Log.DEBUG, "gatt.connect()");

        // :: Verify
        assertThat(logRows("BLE: gatt.connect()")).hasSize(1);
    }

    /** Without extra logging a Bluetooth debug line is not kept. */
    @Test
    public void onLog_debugWithoutExtraTag_isNotStored() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("");
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onLog(Log.DEBUG, "gatt.connect()");

        // :: Verify
        assertThat(logRows("BLE: gatt.connect()")).isEmpty();
    }

    /**
     * Without extra logging a Bluetooth error line is not kept either: an out-of-range sensor fails a connect every few
     * minutes, and those must not fill a normal user's error list.
     */
    @Test
    public void onLog_errorWithoutExtraTag_isNotStored() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("");
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onLog(Log.ERROR, "Error (0x85): GATT ERROR");

        // :: Verify
        assertThat(logRows("BLE: Error (0x85): GATT ERROR")).isEmpty();
    }

    /** Each retry is counted by its reason, so a tester's report says at a glance why the session kept retrying. */
    @Test
    public void onStage_retries_countedByReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onStage("Connecting");
        session.onStage("Reconnecting in 5 s: connect failed, reason 10");
        session.onStage("Reconnecting in 10 s: sensor not seen");
        session.onStage("Reconnecting in 15 s: connect failed, reason 10");

        // :: Verify
        assertThat(session.retryCounts())
                .containsExactly("connect failed, reason 10", 2, "sensor not seen", 1)
                .inOrder();
    }

    /** Only the newest 20 stages are kept, oldest first, so a long session does not grow without bound. */
    @Test
    public void onStage_manyStages_keepsNewest20() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        for (int i = 0; i < 25; i++) {
            session.onStage("s" + i);
        }

        // :: Verify
        List<String> stages = session.recentStages();
        assertThat(stages).hasSize(20);
        assertThat(stages.get(0)).endsWith(" s5");
        assertThat(stages.get(19)).endsWith(" s24");
    }

    // ===================================================== Stall =====================================================

    /** A session that never reported a stage or a value is stalled, so the next wake-up restarts it. */
    @Test
    public void stalled_noProgress_true() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        boolean stalled = session.stalled(tsl());

        // :: Verify
        assertThat(stalled).isTrue();
    }

    /** A stage reported eleven minutes ago is recent enough: the session is still working. */
    @Test
    public void stalled_stageElevenMinutesAgo_false() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Connecting");

        // :: Act
        boolean stalled = session.stalled(tsl() + 11 * MINUTE_IN_MS);

        // :: Verify
        assertThat(stalled).isFalse();
    }

    /** No stage or value for thirteen minutes means the session is stuck, e.g. a retry timer that Doze held back. */
    @Test
    public void stalled_stageThirteenMinutesAgo_true() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Reconnecting in 300 s: disconnected, reason 8");

        // :: Act
        boolean stalled = session.stalled(tsl() + 13 * MINUTE_IN_MS);

        // :: Verify
        assertThat(stalled).isTrue();
    }

    /** A reading counts as progress even when it is not stored. */
    @Test
    public void stalled_readingJustNow_false() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onReading(reading(1, 120));

        // :: Act
        boolean stalled = session.stalled(tsl());

        // :: Verify
        assertThat(stalled).isFalse();
    }

    /** A sensor status counts as progress, as during warm-up when no glucose value is stored. */
    @Test
    public void stalled_statusJustNow_false() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStatus(PatchStatus.parse(new byte[PatchStatus.SIZE]));

        // :: Act
        boolean stalled = session.stalled(tsl());

        // :: Verify
        assertThat(stalled).isFalse();
    }

    /** An error alone is not progress: a session that only failed, like one without a Bluetooth adapter, is stalled. */
    @Test
    public void stalled_onlyError_true() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onError("No Bluetooth adapter");

        // :: Act
        boolean stalled = session.stalled(tsl());

        // :: Verify
        assertThat(stalled).isTrue();
    }

    // =================================================== Start ======================================================

    /**
     * Without a credential, starting the collector posts a notification that opens the Li3 screen, which offers the
     * download. It does not open the screen over whatever the user is doing.
     */
    @Test
    public void onStartCommand_noCredential_notifies() {
        // :: Setup
        selectSource(DexCollectionType.Li3);

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        Notification notification = notifications().getNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID);
        assertThat(shadowOf(notification.contentIntent).getSavedIntent().getComponent().getClassName())
                .isEqualTo(Li3Activity.class.getName());
        assertThat(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity()).isNull();
    }

    /** A dismissed notification stays dismissed on the five-minute wake-ups; only a new collector start posts it. */
    @Test
    public void onStartCommand_wakeUp_notifiesOnce() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        ServiceController<Li3Service> service = Robolectric.buildService(Li3Service.class).create();
        service.startCommand(0, 1);
        JoH.cancelNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID);

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(notifications().getNotification(Constants.LI3_CREDENTIAL_NOTIFICATION_ID)).isNull();
    }

    /**
     * A wake-up gives the scanned Li3 an xDrip sensor again after "Stop sensor", so its readings are kept rather
     * than dropped until the next NFC scan.
     */
    @Test
    public void onStartCommand_xdripSensorStopped_startsItAgain() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        sensor("0RRC989AQ", now - 2 * DAY_IN_MS);
        Sensor.stopSensor();

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(Sensor.currentSensor()).isNotNull();
    }

    /** With extra logging on, a restart of the collector shows in the log as the service ending and starting. */
    @Test
    public void lifecycle_withExtraTag_logsCreateAndDestroy() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");
        selectSource(DexCollectionType.Li3);

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1).destroy();

        // :: Verify
        assertThat(logRows("Service created")).hasSize(1);
        assertThat(logRows("Start command, no session running")).hasSize(1);
        assertThat(logRows("Service destroyed")).hasSize(1);
    }

    /** A collector that is not the chosen source stops without opening anything. */
    @Test
    public void onStartCommand_otherSource_opensNothing() {
        // :: Setup
        selectSource(DexCollectionType.Disabled);

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity()).isNull();
    }

    /** Half a day after the sensor's wear ends, the collector stops looking for it, so Bluetooth stops draining. */
    @Test
    public void onStartCommand_sensorEndedHalfADayAgo_stopsLooking() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        sensor("0RRC989AQ", now - 15 * DAY_IN_MS - 13 * HOUR_IN_MS);

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(value(Li3Service.megaStatus(), R.string.li3_stage))
                .isEqualTo(gs(R.string.li3_stage_sensor_ended));
    }

    /** Within half a day of the wear end the collector keeps trying, in case the sensor still sends. */
    @Test
    public void onStartCommand_sensorEndedElevenHoursAgo_keepsLooking() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        sensor("0RRC989AQ", now - 15 * DAY_IN_MS - 11 * HOUR_IN_MS);

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(value(Li3Service.megaStatus(), R.string.li3_stage))
                .isNotEqualTo(gs(R.string.li3_stage_sensor_ended));
    }

    // ================================================== megaStatus ===================================================

    /** Without a credential, the status page says why the last download failed. */
    @Test
    public void megaStatus_downloadFailed_showsReason() {
        // :: Setup
        Li3Credential credential = Li3Credential.get(xdrip.getAppContext());
        assertThrows(GeneralSecurityException.class, () -> credential.download(answering404()));

        // :: Act
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(status.stream().anyMatch(item -> item.value.contains("HTTP 404"))).isTrue();
    }

    /** Right after "Forget sensor" the old session's stage is gone: the page says there is no sensor. */
    @Test
    public void megaStatus_afterForget_showsNoSensor() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Reconnecting in 55 s: connect failed, reason 10");

        // :: Act
        Li3Store.forgetSensor();
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_sensor)).isEqualTo(gs(R.string.li3_none));
        assertThat(value(status, R.string.li3_stage)).isEqualTo(gs(R.string.li3_stage_no_sensor));
    }

    /** A reading after an error shows the connection works again, so the status page drops the old error. */
    @Test
    public void megaStatus_readingAfterError_noLastError() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onError("Pairing failed: timeout");

        // :: Act
        session.onReading(reading(1, 120));
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_last_error)).isNull();
    }

    /** A reading xDrip refuses, here one in warm-up, shows on the status page with the reason. */
    @Test
    public void megaStatus_readingRefused_showsReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onReading(reading(1, 120));
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_reading_not_stored)).isEqualTo("warmup");
    }

    /** An accepted reading after refused ones takes the reason off the status page. */
    @Test
    public void megaStatus_acceptedAfterRefused_noReason() {
        // :: Setup
        Li3Service.Session session =
                new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now - 1010 * MINUTE_IN_MS)));
        session.onReading(reading(1000, 20));

        // :: Act
        session.onReading(reading(1001, 120));
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_reading_not_stored)).isNull();
    }

    /** Right after "Forget sensor" the status page no longer shows why the old sensor's reading was refused. */
    @Test
    public void megaStatus_refusedThenForget_noReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onReading(reading(1, 120));

        // :: Act
        Li3Store.forgetSensor();
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_reading_not_stored)).isNull();
    }

    /** When the link drops after refused readings, the status page shows the reconnect, not the old refusal. */
    @Test
    public void megaStatus_refusedThenReconnecting_showsStageOnly() {
        // :: Setup
        Li3Store.saveState(sensor("0RRC989AQ", now));
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Streaming");
        session.onReading(reading(1, 120));

        // :: Act
        session.onStage("Reconnecting in 5 s: disconnected, reason 8");
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_stage)).isEqualTo("Reconnecting in 5 s: disconnected, reason 8");
        assertThat(value(status, R.string.li3_reading_not_stored)).isNull();
    }

    /**
     * A refused reading that a stopped session still hands over, from a frame it was opening as it stopped, does not
     * show on the status page: the refusal was for a link that is gone.
     */
    @Test
    public void megaStatus_refusedAfterSessionStopped_noReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Streaming");
        session.stop();

        // :: Act
        session.onReading(reading(1, 120));
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_reading_not_stored)).isNull();
    }

    // ================================================== nanoStatus ===================================================

    /**
     * While readings are refused, the line under the graph says why. The stage would say the sensor streams, with no
     * new readings to show for it.
     */
    @Test
    public void nanoStatus_readingRefused_showsReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Streaming");

        // :: Act
        session.onReading(reading(1, 120));

        // :: Verify
        assertThat(Li3Service.nanoStatus().toString()).isEqualTo("Reading not stored: warmup");
    }

    /** With no sensor, the line under the graph says where the setup steps are, since the scan alone is not enough. */
    @Test
    public void nanoStatus_noSensor_pointsToLi3Screen() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        Li3Store.forgetSensor();

        // :: Act
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(String.valueOf(Li3Service.nanoStatus())).isEqualTo(gs(R.string.li3_home_no_sensor));
    }

    /** Once the collector restarts after "Forget sensor", the old sensor's refusal is gone from under the graph. */
    @Test
    public void nanoStatus_refusedThenForgetAndRestart_noReason() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onReading(reading(1, 120));

        // :: Act
        Li3Store.forgetSensor();
        Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);

        // :: Verify
        assertThat(String.valueOf(Li3Service.nanoStatus())).isNotEqualTo("Reading not stored: warmup");
    }

    /**
     * When the link drops after refused readings, the line under the graph no longer gives the refusal as the reason.
     * Whether it shows the stage depends on readings stored in earlier tests, so only the refusal is checked.
     */
    @Test
    public void nanoStatus_refusedThenReconnecting_noReason() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Streaming");
        session.onReading(reading(1, 120));

        // :: Act
        session.onStage("Reconnecting in 5 s: disconnected, reason 8");

        // :: Verify
        assertThat(String.valueOf(Li3Service.nanoStatus())).doesNotContain(gs(R.string.li3_reading_not_stored));
    }

    /** A scan that needs location posts a notification that opens the Li3 screen, which asks for it. */
    @Test
    public void onLocationNeeded_notifies() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onLocationNeeded();

        // :: Verify
        Notification notification = notifications().getNotification(Constants.LI3_LOCATION_NOTIFICATION_ID);
        assertThat(shadowOf(notification.contentIntent).getSavedIntent().getComponent().getClassName())
                .isEqualTo(Li3Activity.class.getName());
    }

    /** Every retry asks again; a dismissed notification stays dismissed for the rest of the session. */
    @Test
    public void onLocationNeeded_afterDismissed_notPostedAgain() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onLocationNeeded();
        JoH.cancelNotification(Constants.LI3_LOCATION_NOTIFICATION_ID);

        // :: Act
        session.onLocationNeeded();

        // :: Verify
        assertThat(notifications().getNotification(Constants.LI3_LOCATION_NOTIFICATION_ID)).isNull();
    }

    /** A reading shows the scan works again, so the notification goes away. */
    @Test
    public void onReading_afterLocationNeeded_removesNotification() {
        // :: Setup
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onLocationNeeded();

        // :: Act
        session.onReading(reading(1, 120));

        // :: Verify
        assertThat(notifications().getNotification(Constants.LI3_LOCATION_NOTIFICATION_ID)).isNull();
    }

    /** The sensor end on the status page is the same text as on the home screen. */
    @Test
    public void megaStatus_runningSensor_endsAsOnHomeScreen() {
        // :: Setup
        selectSource(DexCollectionType.Li3);
        SensorDays.clearCache();
        sensor("0RRC989AQ", now - 2 * DAY_IN_MS - 10 * MINUTE_IN_MS);

        // :: Act
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_remaining)).isEqualTo("Ends in 13 days");
        assertThat(value(status, R.string.li3_remaining)).isEqualTo(SensorDays.get().getSpannable().toString());
    }

    // ================================================= isCollecting ==================================================

    /** Without any reading since the start, the collector is not collecting, so a restart is allowed. */
    @Test
    public void isCollecting_noReading_notCollecting() {
        // :: Setup
        Li3Service.forgetReadingTimes();

        // :: Act
        boolean collecting = Li3Service.isCollecting();

        // :: Verify
        assertThat(collecting).isFalse();
    }

    /**
     * A reading xDrip refuses, here one in warm-up, still shows the sensor is connected, so the missed-reading check
     * does not restart the collector.
     */
    @Test
    public void isCollecting_refusedReading_collecting() {
        // :: Setup
        Li3Service.forgetReadingTimes();
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onReading(reading(1, 120));

        // :: Verify
        assertThat(Li3Service.isCollecting()).isTrue();
    }

    /**
     * After refused readings the link drops; the line under the graph says it is reconnecting, as no reading was
     * stored, even though the refused readings still count as collecting.
     */
    @Test
    public void nanoStatus_refusedThenReconnecting_showsStage() {
        // :: Setup
        Li3Service.forgetReadingTimes();
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onStage("Streaming");
        session.onReading(reading(1, 120));

        // :: Act
        session.onStage("Reconnecting in 5 s: disconnected, reason 8");
        SpannableString nano = Li3Service.nanoStatus();

        // :: Verify
        assertThat(String.valueOf(nano)).isEqualTo("Reconnecting in 5 s: disconnected, reason 8");
    }

    /** A refused reading does not show as the last reading on the status page: no reading was stored. */
    @Test
    public void megaStatus_refusedReading_noLastReading() {
        // :: Setup
        Li3Service.forgetReadingTimes();
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onReading(reading(1, 120));
        List<StatusItem> status = Li3Service.megaStatus();

        // :: Verify
        assertThat(value(status, R.string.li3_last_reading)).isNull();
    }

    // ================================================= Bluetooth off =================================================

    /** With Turn Bluetooth on at its default, an attempt that finds Bluetooth off turns it on, as for Dexcom. */
    @Test
    public void onBluetoothOff_settingDefault_turnsBluetoothOn() {
        // :: Setup
        setBluetoothEnabled(false);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onBluetoothOff();

        // :: Verify
        assertThat(BluetoothAdapter.getDefaultAdapter().isEnabled()).isTrue();
    }

    /** With the setting off, Bluetooth stays off. */
    @Test
    public void onBluetoothOff_settingOff_leavesBluetoothOff() {
        // :: Setup
        Pref.setBoolean("automatically_turn_bluetooth_on", false);
        setBluetoothEnabled(false);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));

        // :: Act
        session.onBluetoothOff();

        // :: Verify
        assertThat(BluetoothAdapter.getDefaultAdapter().isEnabled()).isFalse();
    }

    /**
     * Bluetooth takes a few seconds to come on and the retries come every few seconds, so it is turned on at most once
     * every 30 seconds, as for Dexcom.
     */
    @Test
    public void onBluetoothOff_againWithin30Seconds_doesNotTurnOnAgain() {
        // :: Setup
        setBluetoothEnabled(false);
        Li3Service.Session session = new Li3Service.Session(new Li3Readings(sensor("0RRC989AQ", now)));
        session.onBluetoothOff();
        setBluetoothEnabled(false);

        // :: Act
        session.onBluetoothOff();

        // :: Verify
        assertThat(BluetoothAdapter.getDefaultAdapter().isEnabled()).isFalse();
    }

    // ==================================================== helpers ====================================================

    private static void setBluetoothEnabled(boolean enabled) {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(enabled);
    }

    /** The collector's rows in xDrip's log with this message. */
    private static List<UserError> logRows(String message) {
        List<UserError> rows = new ArrayList<>();
        for (UserError row : UserError.all()) {
            if ("Li3".equals(row.shortError) && message.equals(row.message)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static ShadowNotificationManager notifications() {
        return shadowOf((NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE));
    }

    /** The value of the status row with this name, or null when the page has no such row. */
    private static String value(List<StatusItem> status, int name) {
        return status.stream().filter(item -> gs(name).equals(item.name)).map(item -> item.value).findFirst()
                .orElse(null);
    }


    /** Stores a running sensor as the current one, as an NFC scan does. */
    private static Li3SensorState sensor(String serial, long activationMs) {
        Li3SensorState state = new Li3SensorState(serial, "CC:22:DF:B8:F9:58", new byte[]{1, 2, 3, 4});
        state.setActivationMs(activationMs);
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(PATCH_INFO)));
        Li3Store.saveState(state);
        Li3Store.ensureSensor(state);
        return state;
    }

    /**
     * Selects the source with the one-minute setting off: DexCollectionType reads that setting once, when the class
     * loads, and keeps it for every later test in the run.
     */
    private static void selectSource(DexCollectionType type) {
        Pref.removeItem("libre_one_minute");
        DexCollectionType.setDexCollectionType(type);
    }

    /** A client that never touches the network: every request is answered 404. */
    private static OkHttpClient answering404() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> new Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(404)
                        .message("test")
                        .body(ResponseBody.create(new byte[0], null))
                        .build())
                .build();
    }

    private static RealtimeReading reading(int lifeCount, int mgdl) {
        byte[] b = new byte[RealtimeReading.SIZE];
        b[0] = (byte) lifeCount;
        b[1] = (byte) (lifeCount >> 8);
        b[14] = 3;
        b[15] = (byte) mgdl;
        b[16] = (byte) (mgdl >> 8);
        return RealtimeReading.parse(b);
    }
}
