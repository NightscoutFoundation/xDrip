package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.AlarmManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ServiceController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlarmManager;
import org.robolectric.shadows.ShadowSystemClock;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import xrwd.li3.Li3SensorState;
import xrwd.li3.crypto.IdentityBundle;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3Service}'s wake-ups: a running connection is kept while it makes progress, restarted once it
 * stalls, and replaced when another sensor is stored. Bluetooth is off, so every attempt fails at once and asks for a
 * retry, which is the progress a session reports; the credential is the synthetic bundle from Li3CredentialTest.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
// JoH is instrumented so that ShadowSystemClock.advanceBy() reaches JoH.tsl(), which the stall check reads
@Config(instrumentedPackages = {"com.eveningoutpost.dexdrip.models.JoH"})
public class Li3ServiceWakeUpTest extends RobolectricTestWithConfig {

    /** A US 15-day Li3: 60 minutes warm-up, 21600 minutes wear. */
    private static final String PATCH_INFO = "00a50001000200010060541e020401040c0130525243393839415151ff17";

    private ServiceController<Li3Service> service;

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        new Delete().from(UserError.class).execute(); // log rows outlive each test, and these tests count them
        Li3Store.forgetSensor();
        UserError.ExtraLogTags.readPreference("Li3:d");
        Pref.setBoolean("automatically_turn_bluetooth_on", false);
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
        // DexCollectionType reads the one-minute setting once, when the class loads, and keeps it for every later test
        Pref.removeItem("libre_one_minute");
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.destroy();
        }
        Li3Credential.pinnedDigest = IdentityBundle.DIGEST;
        Pref.removeItem("dex_collection_method");
        Pref.removeItem("automatically_turn_bluetooth_on");
        Li3Store.forgetSensor();
        UserError.ExtraLogTags.readPreference("");
    }

    // ==================================================== wake-up ====================================================

    /** The first start opens a connection to the stored sensor. */
    @Test
    public void start_storedSensor_connects() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");

        // :: Act
        start();

        // :: Verify
        assertThat(Li3Service.currentSession()).isNotNull();
        assertThat(logRows("Connecting to Li3 0RRC989AQ")).hasSize(1);
    }

    /** A wake-up while the connection still makes progress keeps it, rather than starting it over. */
    @Test
    public void wakeUp_progressing_keepsSession() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        start();
        Li3Service.Session first = Li3Service.currentSession();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(Li3Service.currentSession()).isSameInstanceAs(first);
        assertThat(logRows("Connecting to Li3 0RRC989AQ")).hasSize(1);
    }

    /** A retry the phone slept through runs at the wake-up, rather than when the phone wakes for other work. */
    @Test
    public void wakeUp_retryOverdue_runsIt() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        start();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(1));

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(logRowsStartingWith("BLE: Wait ran out ")).hasSize(1);
    }

    /** A connection that made no progress for over 12 minutes is started over at the wake-up. */
    @Test
    public void wakeUp_stalled_restartsSession() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        start();
        Li3Service.Session first = Li3Service.currentSession();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(13));

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(Li3Service.currentSession()).isNotSameInstanceAs(first);
        assertThat(logRows("Li3 connection made no progress, restarting it")).hasSize(1);
        assertThat(logRows("Connecting to Li3 0RRC989AQ")).hasSize(2);
    }

    /**
     * A restarted connection goes on counting attempts, so it keeps the wait and scan that attempt has reached rather
     * than starting over from a direct connect.
     */
    @Test
    public void wakeUp_stalled_continuesAttemptCount() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        start();
        ShadowSystemClock.advanceBy(Duration.ofMinutes(13));

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(logRows("BLE: Attempt 1, by address")).hasSize(1);
        assertThat(logRows("BLE: Attempt 2, by scan")).hasSize(1);
    }

    /** A newly stored sensor replaces the connection to the old one at the next wake-up, without waiting for a stall. */
    @Test
    public void wakeUp_otherSensorStored_replacesSession() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        start();
        Li3Service.Session first = Li3Service.currentSession();
        sensor("0RRCNEW00");

        // :: Act
        service.startCommand(0, 2);

        // :: Verify
        assertThat(Li3Service.currentSession()).isNotSameInstanceAs(first);
        assertThat(logRows("Connecting to Li3 0RRCNEW00")).hasSize(1);
    }

    /** Each start sets the next wake-up about five minutes ahead, so a connection is checked even if nothing else runs. */
    @Test
    public void start_schedulesWakeUp() throws Exception {
        // :: Setup
        storeCredential();
        sensor("0RRC989AQ");
        long before = tsl();

        // :: Act
        start();

        // :: Verify
        ShadowAlarmManager alarms = shadowOf((AlarmManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.ALARM_SERVICE));
        ShadowAlarmManager.ScheduledAlarm next = alarms.getNextScheduledAlarm();
        assertThat(next).isNotNull();
        assertThat(next.getTriggerAtMs()).isAtLeast(before + 5 * MINUTE_IN_MS);
        assertThat(next.getTriggerAtMs()).isAtMost(tsl() + 5 * MINUTE_IN_MS);
    }

    // ==================================================== helpers ====================================================

    private void start() {
        service = Robolectric.buildService(Li3Service.class).create().startCommand(0, 1);
    }

    /** Stores the synthetic credential and lets {@link Li3Credential#get} load it. */
    private static void storeCredential() throws Exception {
        byte[] json = Li3CredentialTest.bundle();
        Li3Credential.pinnedDigest = Li3CredentialTest.sha256(json);
        new Li3Credential(new File(RuntimeEnvironment.getApplication().getFilesDir(), "li3"),
                Li3Credential.pinnedDigest).store(Li3CredentialTest.xz(json));
    }

    /** Stores a sensor activated 1000 minutes ago, well inside its wear. */
    private static void sensor(String serial) {
        Li3SensorState state = new Li3SensorState(serial, "CC:22:DF:B8:F9:58", new byte[]{1, 2, 3, 4});
        state.setActivationMs(tsl() - 1000 * MINUTE_IN_MS);
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(PATCH_INFO)));
        Li3Store.saveState(state);
    }

    private static List<UserError> logRows(String message) {
        List<UserError> rows = new ArrayList<>();
        for (UserError row : UserError.all()) {
            if ("Li3".equals(row.shortError) && message.equals(row.message)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private static List<UserError> logRowsStartingWith(String prefix) {
        List<UserError> rows = new ArrayList<>();
        for (UserError row : UserError.all()) {
            if ("Li3".equals(row.shortError) && row.message.startsWith(prefix)) {
                rows.add(row);
            }
        }
        return rows;
    }
}
