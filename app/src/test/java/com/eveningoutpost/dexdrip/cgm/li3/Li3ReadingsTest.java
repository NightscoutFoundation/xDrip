package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowToast;

import java.util.List;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3Readings}: which realtime and historic Li3 values become xDrip readings, at which time,
 * and what the status page shows about them.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3ReadingsTest extends RobolectricTestWithConfig {

    /** A US 15-day Li3: 60 minutes warm-up, 21600 minutes wear. */
    private static final String PATCH_INFO = "00a50001000200010060541e020401040c0130525243393839415151ff17";
    private static final byte[] PIN = {0x32, 0x25, (byte) 0xec, 0x72};
    private static final byte[] KAUTH = Hex.toBytes("00112233445566778899aabbccddeeff");
    private static final byte[] FAST_DATA = Hex.toBytes("0102030405a0b0c0");

    private long now;
    private Li3SensorState state;
    private Li3Readings readings;

    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        // DexCollectionType reads the one-minute setting once, when the class loads, and keeps it for every later test
        // in the run: load it here with the setting off, before any test turns it on
        Pref.removeItem("libre_one_minute");
        DexCollectionType.getDexCollectionType();
        Li3Store.forgetSensor();
        BgReading.deleteALL();
        new Delete().from(UserError.class).execute(); // log rows outlive each test, and the log tests count them
        now = tsl();
        state = new Li3SensorState("0RRC989AQ", "CC:22:DF:B8:F9:58", PIN);
        // Life counts 990-1010 are the last twenty minutes, as realtime readings from a running sensor are
        state.setActivationMs(now - 1010 * MINUTE_IN_MS);
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(PATCH_INFO)));
        state.setKAuth(KAUTH);
        Li3Store.saveState(state);
        Li3Store.ensureSensor(state);
        readings = new Li3Readings(state);
    }

    @After
    public void tearDown() {
        Pref.removeItem("libre_one_minute");
        Pref.removeItem("libre_filter_length");
        Pref.removeItem(Li3Store.PREF_SMOOTHING);
        Pref.removeItem("bypass_calibration_quality_check");
        Pref.removeItem("dex_collection_method");
        UserError.ExtraLogTags.readPreference("");
        Li3Store.forgetSensor();
        Calibration.deleteAll(); // a stored note calibration would block later tests from deleting the sensor
        BgReading.deleteALL();
    }

    // ================================================== onRealtime ===================================================

    /** With one-minute readings on, readings a minute apart are both stored at activation + life count minutes. */
    @Test
    public void onRealtime_oneMinuteOn_storesEveryMinute() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", true);

        // :: Act
        readings.onRealtime(reading(1001, 120), now);
        readings.onRealtime(reading(1002, 125), now);

        // :: Verify
        assertThat(storedAt(1001).calculated_value).isEqualTo(120.0);
        assertThat(storedAt(1002).calculated_value).isEqualTo(125.0);
    }

    /** A stored reading names the collector as its source. */
    @Test
    public void onRealtime_stored_sourceIsLi3() {
        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(storedAt(1000).source_info).isEqualTo("Li3");
    }

    /** With one-minute readings off, only the reading on a five-minute life count is stored. */
    @Test
    public void onRealtime_oneMinuteOff_storesOnlyFiveMinuteLifeCount() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", false);

        // :: Act
        readings.onRealtime(reading(999, 120), now);
        readings.onRealtime(reading(1000, 125), now);

        // :: Verify
        assertThat(storedAt(999)).isNull();
        assertThat(storedAt(1000).calculated_value).isEqualTo(125.0);
    }

    /** A reading with no value or a zero value is not stored, and its life count is remembered as rejected. */
    @Test
    public void onRealtime_noValueOrZero_notStoredAndRejected() {
        // :: Act
        readings.onRealtime(reading(1000, null), now);
        readings.onRealtime(reading(1005, 0), now);

        // :: Verify
        assertThat(storedAt(1000)).isNull();
        assertThat(storedAt(1005)).isNull();
        assertThat(state.rejectedLifeCounts()).containsAtLeast(1000, 1005);
    }

    /** LO is stored as 39 mg/dL, so xDrip shows LOW and the low alerts see it. */
    @Test
    public void onRealtime_low_storedAsLow() {
        // :: Act
        boolean stored = readings.onRealtime(reading(1000, 20), now);

        // :: Verify
        assertThat(stored).isTrue();
        assertThat(storedAt(1000).calculated_value).isEqualTo(39.0);
        assertThat(storedAt(1000).displayValue(null)).isEqualTo("LOW");
    }

    /** HI is stored as 501 mg/dL, so xDrip shows HIGH and the high alerts see it. */
    @Test
    public void onRealtime_high_storedAsHigh() {
        // :: Act
        boolean stored = readings.onRealtime(reading(1000, 600), now);

        // :: Verify
        assertThat(stored).isTrue();
        assertThat(storedAt(1000).calculated_value).isEqualTo(501.0);
        assertThat(storedAt(1000).displayValue(null)).isEqualTo("HIGH");
    }

    /** A reading during warm-up is not stored. */
    @Test
    public void onRealtime_warmup_notStored() {
        // :: Act
        readings.onRealtime(reading(55, 120), now);

        // :: Verify
        assertThat(storedAt(55)).isNull();
    }

    /** A reading at or after the end of wear is not stored. */
    @Test
    public void onRealtime_pastWear_notStored() {
        // :: Setup
        state.setActivationMs(now - 16 * DAY_IN_MS);

        // :: Act
        readings.onRealtime(reading(21600, 120), now);

        // :: Verify
        assertThat(storedAt(21600)).isNull();
    }

    /** A reading the sensor flags as not displayable is not stored. */
    @Test
    public void onRealtime_notDisplayable_notStored() {
        // :: Act
        readings.onRealtime(reading(1000, 120, 0x8001), now);

        // :: Verify
        assertThat(storedAt(1000)).isNull();
    }

    /** A repeated life count does not replace the stored value, and a lower one is not stored. */
    @Test
    public void onRealtime_repeatedOrLowerLifeCount_notStoredAgain() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onRealtime(reading(1000, 180), now);
        readings.onRealtime(reading(995, 180), now);

        // :: Verify
        assertThat(storedAt(1000).calculated_value).isEqualTo(120.0);
        assertThat(storedAt(995)).isNull();
    }

    /** A reading whose time is more than fifteen minutes ahead of now is not stored. */
    @Test
    public void onRealtime_inFuture_notStored() {
        // :: Act
        readings.onRealtime(reading(1445, 120), now);

        // :: Verify
        assertThat(storedAt(1445)).isNull();
    }

    /**
     * A reading up to fifteen minutes ahead of now is stored, so a start time that is a few minutes ahead of this
     * phone's clock does not refuse every reading of the sensor. It is stored at now, so it never looks newer than it
     * is.
     */
    @Test
    public void onRealtime_tenMinutesAhead_storedAtNow() {
        // :: Act
        boolean stored = readings.onRealtime(reading(1020, 120), now);

        // :: Verify
        assertThat(stored).isTrue();
        assertThat(storedAt(1020)).isNull();
        assertThat(BgReading.getForPreciseTimestamp(now, 1000).calculated_value).isEqualTo(120.0);
    }

    /**
     * A realtime reading is the sensor's current minute, so one whose time is more than fifteen minutes before now
     * means the start time is wrong: it is not stored, and history does not store that minute either.
     */
    @Test
    public void onRealtime_farInPast_notStoredAndRejected() {
        // :: Setup
        state.setActivationMs(now - DAY_IN_MS);

        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(storedAt(1000)).isNull();
        assertThat(state.rejectedLifeCounts()).contains(1000);
    }

    /** A reading whose time is fifteen minutes before now is still stored. */
    @Test
    public void onRealtime_fifteenMinutesOld_stored() {
        // :: Act
        readings.onRealtime(reading(995, 120), now);

        // :: Verify
        assertThat(storedAt(995).calculated_value).isEqualTo(120.0);
    }

    /** A stored reading becomes the last life count and value, which the next backfill starts after. */
    @Test
    public void onRealtime_stored_updatesLastLifeCount() {
        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(state.lastLifeCount()).isEqualTo(1000);
        assertThat(state.lastMgdl()).isEqualTo(120);
    }

    // =================================================== onHistory ===================================================

    /** A history entry at an existing reading's time leaves that reading's value as it was. */
    @Test
    public void onHistory_existingReading_unchanged() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(1000, 180), now);

        // :: Verify
        assertThat(storedAt(1000).calculated_value).isEqualTo(120.0);
    }

    /** A history entry at a reading stored under an earlier xDrip sensor is not stored a second time. */
    @Test
    public void onHistory_readingUnderEarlierSensor_notStoredAgain() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);
        Sensor.stopSensor();
        Li3Store.ensureSensor(state);

        // :: Act
        readings.onHistory(entries(1000, 180), now);

        // :: Verify
        assertThat(readingsAt(1000)).isEqualTo(1);
    }

    /** A history entry for a life count whose realtime reading was rejected is not stored. */
    @Test
    public void onHistory_rejectedLifeCount_notStored() {
        // :: Setup
        readings.onRealtime(reading(990, null), now);
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(990, 130), now);

        // :: Verify
        assertThat(storedAt(990)).isNull();
    }

    /** A LO history entry in a gap is stored as 39 mg/dL. */
    @Test
    public void onHistory_lowGapEntry_storedAsLow() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(990, 20), now);

        // :: Verify
        assertThat(storedAt(990).calculated_value).isEqualTo(39.0);
    }

    /** A history entry ahead of the last realtime reading is stored: no realtime reading was stored for it. */
    @Test
    public void onHistory_aheadOfLastReading_stored() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(1005, 130), now);

        // :: Verify
        assertThat(storedAt(1005).calculated_value).isEqualTo(130.0);
    }

    /** History with no realtime reading at all is stored, so a hole at the start of a connection is filled. */
    @Test
    public void onHistory_noRealtimeReading_stored() {
        // :: Act
        readings.onHistory(entries(982, 130), now);

        // :: Verify
        assertThat(storedAt(982).calculated_value).isEqualTo(130.0);
    }

    /** A history entry whose time is more than fifteen minutes ahead of now is not stored. */
    @Test
    public void onHistory_moreThanFifteenMinutesAhead_notStored() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        int stored = readings.onHistory(entries(1030, 130), now);

        // :: Verify
        assertThat(stored).isEqualTo(0);
    }

    /**
     * A realtime reading refused only for the sensor's quality flags is not remembered as refused, so history can still
     * fill that minute.
     */
    @Test
    public void onHistory_afterNotDisplayableReading_fillsThatMinute() {
        // :: Setup
        readings.onRealtime(reading(1000, 120, 0x8001), now);

        // :: Act
        readings.onHistory(entries(1000, 130), now);

        // :: Verify
        assertThat(state.rejectedLifeCounts()).doesNotContain(1000);
        assertThat(storedAt(1000).calculated_value).isEqualTo(130.0);
    }

    /** A realtime reading refused for a time too far in the past is remembered, so history does not fill it either. */
    @Test
    public void onHistory_afterReadingTooOld_notStored() {
        // :: Setup
        state.setActivationMs(now - DAY_IN_MS);
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(1000, 130), now);

        // :: Verify
        assertThat(storedAt(1000)).isNull();
    }

    /**
     * A reading flagged not displayable whose time is also too far in the past is remembered as refused, so history
     * does not store that minute at the same wrong time.
     */
    @Test
    public void onHistory_afterNotDisplayableReadingTooOld_notStored() {
        // :: Setup
        state.setActivationMs(now - DAY_IN_MS);
        readings.onRealtime(reading(1000, 120, 0x8001), now);

        // :: Act
        readings.onHistory(entries(1000, 130), now);

        // :: Verify
        assertThat(state.rejectedLifeCounts()).contains(1000);
        assertThat(storedAt(1000)).isNull();
    }

    /** A status frame saves the sensor state, so a backfill ask or give-up made on a status frame is kept. */
    @Test
    public void onStatus_savesState() {
        // :: Setup
        state.openHole(1061, 1070);

        // :: Act
        readings.onStatus(PatchStatus.parse(Hex.toBytes("0000000000000004e8030000")));

        // :: Verify
        assertThat(Li3Store.loadState().backfillFrom()).isEqualTo(1061);
    }

    /** A history page saves the sensor state, so a backfill hole opened or closed on the connection is kept. */
    @Test
    public void onHistory_page_savesState() {
        // :: Setup
        state.openHole(1061, 1070);

        // :: Act
        readings.onHistory(entries(982, 130), now);

        // :: Verify
        assertThat(Li3Store.loadState().backfillFrom()).isEqualTo(1061);
        assertThat(Li3Store.loadState().backfillTo()).isEqualTo(1070);
    }

    /** A valid entry in a gap is stored with the sensor's value, on a life count off the five-minute grid too. */
    @Test
    public void onHistory_gapEntry_stored() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", false);
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(982, 130), now);

        // :: Verify
        assertThat(storedAt(982).calculated_value).isEqualTo(130.0);
        assertThat(storedAt(987).calculated_value).isEqualTo(131.0);
    }

    // =================================================== smoothing ===================================================

    /** Smoothing is off by default, so the stored value is the sensor's value. */
    @Test
    public void smoothing_offByDefault_storesSensorValue() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", true);
        readings.onRealtime(reading(1000, 100), now);

        // :: Act
        readings.onRealtime(reading(1001, 130), now);

        // :: Verify
        assertThat(storedAt(1001).calculated_value).isEqualTo(130.0);
    }

    /** With smoothing on, the stored value is the LibreReceiver weighted average over the filter length. */
    @Test
    public void smoothing_on_storesWeightedAverage() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", true);
        Pref.setBoolean(Li3Store.PREF_SMOOTHING, true);
        Pref.setString("libre_filter_length", "25");
        readings.onRealtime(reading(1000, 100), now);

        // :: Act
        readings.onRealtime(reading(1001, 130), now);

        // :: Verify
        // (100 × (1 − 1/25) + 130 × 1) / (0.96 + 1) = 115.3
        assertThat(storedAt(1001).calculated_value).isEqualTo(115.0);
    }

    // ===================================================== slope =====================================================

    /** A stored reading carries the slope from the reading before it, so the trend arrow is not flat. */
    @Test
    public void slope_secondReading_storedWithSlopeFromPrevious() {
        // :: Setup
        readings.onRealtime(reading(1005, 120), now);

        // :: Act
        readings.onRealtime(reading(1010, 130), now);

        // :: Verify
        BgReading previous = storedAt(1005);
        BgReading current = storedAt(1010);
        assertThat(current.calculated_value_slope)
                .isWithin(1e-12).of(10d / (current.timestamp - previous.timestamp));
    }

    /** A reading already stored at that minute by another source keeps the slope it had. */
    @Test
    public void slope_existingReadingAtSameMinute_leftAsItWas() {
        // :: Setup
        readings.onRealtime(reading(1005, 120), now);
        BgReading.bgReadingInsertFromGluPro(125, state.activationMs() + 1010 * MINUTE_IN_MS, "other");

        // :: Act
        readings.onRealtime(reading(1010, 130), now);

        // :: Verify
        assertThat(storedAt(1010).calculated_value).isEqualTo(125.0);
        assertThat(storedAt(1010).calculated_value_slope).isEqualTo(0.0);
    }

    // =================================================== fastData ====================================================

    /** The undecoded fast data of each reading goes to the log as hex. */
    @Test
    public void fastData_loggedAsHex() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");

        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        boolean logged = false;
        for (UserError entry : UserError.all()) {
            logged |= entry.message.contains("0102030405a0b0c0");
        }
        assertThat(logged).isTrue();
    }

    // ====================================================== log ======================================================

    /** With extra logging on, a stored reading is logged with its value. */
    @Test
    public void onRealtime_stored_logsValue() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");
        Pref.setBoolean("libre_one_minute", true);

        // :: Act
        readings.onRealtime(reading(1001, 120), now);

        // :: Verify
        assertThat(logged("Life count 1001 stored: 120 mg/dL")).isTrue();
    }

    /** With one-minute readings off, a reading between the five-minute ones says why it was not stored. */
    @Test
    public void onRealtime_offInterval_logsWhy() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");
        Pref.setBoolean("libre_one_minute", false);

        // :: Act
        readings.onRealtime(reading(999, 120), now);

        // :: Verify
        assertThat(logged("Life count 999 not stored: not on the 5-minute interval")).isTrue();
    }

    /** The same refusal minute after minute, as through warm-up, is one line in the event log, not one a minute. */
    @Test
    public void onRealtime_sameRefusalRepeated_oneUserEvent() {
        // :: Act
        readings.onRealtime(reading(1001, null), now);
        readings.onRealtime(reading(1002, null), now);
        readings.onRealtime(reading(1003, null), now);

        // :: Verify
        assertThat(refusalEvents()).isEqualTo(1);
    }

    /** A new reason for refusing is logged as a user event, so a change such as a wrong start time is not missed. */
    @Test
    public void onRealtime_refusalReasonChanges_userEventForEach() {
        // :: Act
        readings.onRealtime(reading(1001, null), now);
        readings.onRealtime(reading(1002, 120, 0x8001), now);

        // :: Verify
        assertThat(refusalEvents()).isEqualTo(2);
    }

    /** A refusal after a reading was accepted is logged as a user event again. */
    @Test
    public void onRealtime_refusedAgainAfterAccepted_userEventAgain() {
        // :: Setup
        Pref.setBoolean("libre_one_minute", true);

        // :: Act
        readings.onRealtime(reading(1001, null), now);
        readings.onRealtime(reading(1002, 120), now);
        readings.onRealtime(reading(1003, null), now);

        // :: Verify
        assertThat(refusalEvents()).isEqualTo(2);
    }

    /** The reason the last reading was refused is kept, so the status can show a refusal that lasts. */
    @Test
    public void lastRefusal_readingRefused_givesReason() {
        // :: Act
        readings.onRealtime(reading(1001, null), now);

        // :: Verify
        assertThat(readings.lastRefusal()).isEqualTo("value null outside 39-501");
    }

    /** An accepted reading ends the refusal. */
    @Test
    public void lastRefusal_acceptedAfterRefused_null() {
        // :: Setup
        readings.onRealtime(reading(1001, null), now);

        // :: Act
        readings.onRealtime(reading(1002, 120), now);

        // :: Verify
        assertThat(readings.lastRefusal()).isNull();
    }

    /** Each history page is summed up in one line: how many entries, which life counts, how many were stored. */
    @Test
    public void onHistory_page_logsSummary() {
        // :: Setup
        UserError.ExtraLogTags.readPreference("Li3:d");
        Pref.setBoolean("libre_one_minute", false);
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        readings.onHistory(entries(982, 130), now);

        // :: Verify
        assertThat(logged("History: 6 entries, life counts 982 to 1007, 6 stored")).isTrue();
    }

    // ================================================== megaStatus ===================================================

    /** The status rows show the sensor, its product, state, last reading, backfill count and fast data. */
    @Test
    public void megaStatus_listsSensorAndReadings() {
        // :: Setup
        readings.onStatus(PatchStatus.parse(Hex.toBytes("0000000000000004e8030000")));
        readings.onRealtime(reading(1000, 120), now);
        readings.onHistory(entries(982, 130), now);

        // :: Act
        String text = text(readings.megaStatus());

        // :: Verify
        assertThat(text).contains("0RRC989AQ");
        assertThat(text).contains("Li3, 15 days");
        assertThat(text).contains("1000");
        assertThat(text).contains("0102030405a0b0c0");
        assertThat(text).contains("Backfilled: 6");
        assertThat(text).contains("Sensor state: 4");
    }

    /** The status rows never show the BLE PIN or kAuth. */
    @Test
    public void megaStatus_noSecrets() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);

        // :: Act
        String text = text(readings.megaStatus()).toLowerCase();

        // :: Verify
        assertThat(text).doesNotContain(Hex.of(PIN));
        assertThat(text).doesNotContain(Hex.of(KAUTH));
    }

    /** The service status says whether the app credential is downloaded. */
    @Test
    public void megaStatus_service_showsMissingCredential() {
        // :: Act
        String text = text(Li3Service.megaStatus());

        // :: Verify
        assertThat(text).contains("App credential: none");
    }

    // ================================================ sensor replaced ================================================

    /**
     * After another sensor was started or taken over, a reading still arriving from the old session is not stored and
     * does not overwrite the new sensor.
     */
    @Test
    public void onRealtime_otherSensorStored_notStoredAndNewSensorKept() {
        // :: Setup
        Li3Store.saveState(new Li3SensorState("0RRCNEW00", "CC:22:DF:B8:F9:59", PIN));

        // :: Act
        boolean stored = readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(stored).isFalse();
        assertThat(storedAt(1000)).isNull();
        assertThat(Li3Store.loadState().serial()).isEqualTo("0RRCNEW00");
    }

    /** A rescan of the same sensor that gave it a new PIN is not overwritten by the old session either. */
    @Test
    public void onRealtime_sameSensorNewPin_newPinKept() {
        // :: Setup
        byte[] newPin = {0x01, 0x02, 0x03, 0x04};
        Li3Store.saveState(new Li3SensorState("0RRC989AQ", "CC:22:DF:B8:F9:58", newPin));

        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(Li3Store.loadState().blePin()).isEqualTo(newPin);
    }

    /** After the sensor was forgotten, a reading from the old session does not bring it back. */
    @Test
    public void onRealtime_forgotten_sensorStaysForgotten() {
        // :: Setup
        Li3Store.forgetSensor();

        // :: Act
        readings.onRealtime(reading(1000, 120), now);

        // :: Verify
        assertThat(Li3Store.loadState()).isNull();
        assertThat(storedAt(1000)).isNull();
    }

    /** History arriving from the old session after another sensor was stored fills nothing. */
    @Test
    public void onHistory_otherSensorStored_nothingStored() {
        // :: Setup
        readings.onRealtime(reading(1000, 120), now);
        Li3Store.saveState(new Li3SensorState("0RRCNEW00", "CC:22:DF:B8:F9:59", PIN));

        // :: Act
        int stored = readings.onHistory(entries(982, 130), now);

        // :: Verify
        assertThat(stored).isEqualTo(0);
        assertThat(storedAt(982)).isNull();
    }

    // ================================================== calibration ==================================================

    /** Li3 is never calibrated: a finger-stick calibration is refused and the reading keeps the sensor's value. */
    @Test
    public void calibration_li3_refusedAndReadingUnchanged() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        readings.onRealtime(reading(1010, 120), now);

        // :: Act
        Calibration calibration = Calibration.create(100, RuntimeEnvironment.application);

        // :: Verify
        assertThat(calibration).isNull();
        assertThat(Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3)).isEmpty();
        assertThat(storedAt(1010).calculated_value).isEqualTo(120.0);
    }

    /**
     * A refused calibration is an event, not an error: the event log says calibration is off for the source, nothing is
     * logged as an error, and the toast says the readings are used as the sensor reports them.
     */
    @Test
    public void calibration_li3_refusalLoggedAsEventWithReason() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        readings.onRealtime(reading(1010, 120), now);

        // :: Act
        Calibration.create(100, RuntimeEnvironment.application);

        // :: Verify
        int events = 0;
        int errors = 0;
        for (UserError entry : UserError.all()) {
            if (!"Calibration".equals(entry.shortError)) {
                continue;
            }
            if (entry.severity == 5 && "Calibration not used: calibration is off for Li3".equals(entry.message)) {
                events++;
            }
            if (entry.severity == 2 || entry.severity == 3) {
                errors++;
            }
        }
        assertThat(events).isEqualTo(1);
        assertThat(errors).isEqualTo(0);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo("Calibration is off for this data source. Readings are used as the sensor reports them.");
    }

    /** A finger-stick kept as a note only is still stored for the graph, and the reading keeps the sensor's value. */
    @Test
    public void calibration_li3NoteOnly_storedAndReadingUnchanged() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        readings.onRealtime(reading(1010, 120), now);

        // :: Act
        Calibration note = Calibration.create(100, 0, RuntimeEnvironment.application, true, 0);

        // :: Verify
        assertThat(note).isNotNull();
        assertThat(note.bg).isEqualTo(100.0);
        assertThat(storedAt(1010).calculated_value).isEqualTo(120.0);
    }

    /** The initial double calibration is refused for Li3 too, even with the data quality check bypassed. */
    @Test
    public void initialCalibration_li3_refusedAndReadingsUnchanged() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.Li3);
        Pref.setBoolean("bypass_calibration_quality_check", true);
        readings.onRealtime(reading(1000, 110), now);
        readings.onRealtime(reading(1005, 115), now);
        readings.onRealtime(reading(1010, 120), now);

        // :: Act
        Calibration.initialCalibration(100, 105, RuntimeEnvironment.application);

        // :: Verify
        assertThat(Calibration.getCalibrationsForSensor(Sensor.currentSensor(), 3)).isEmpty();
        assertThat(storedAt(1000).calculated_value).isEqualTo(110.0);
        assertThat(storedAt(1010).calculated_value).isEqualTo(120.0);
    }

    // ==================================================== helpers ====================================================

    private static boolean logged(String message) {
        for (UserError entry : UserError.all()) {
            if ("Li3".equals(entry.shortError) && message.equals(entry.message)) {
                return true;
            }
        }
        return false;
    }

    /** How many refused readings were logged as user events, the lines the event log shows without extra logging. */
    private static int refusalEvents() {
        int count = 0;
        for (UserError entry : UserError.all()) {
            if (entry.severity == 5 && entry.message.startsWith("Li3 reading at life count")) {
                count++;
            }
        }
        return count;
    }

    /** How many readings, under any xDrip sensor, are stored at this life count's time. */
    private int readingsAt(int lifeCount) {
        return new Select().from(BgReading.class)
                .where("timestamp = ?", state.activationMs() + lifeCount * MINUTE_IN_MS).execute().size();
    }

    private BgReading storedAt(int lifeCount) {
        return BgReading.getForPreciseTimestamp(state.activationMs() + lifeCount * MINUTE_IN_MS, 1000);
    }

    private static RealtimeReading reading(int lifeCount, Integer mgdl) {
        return reading(lifeCount, mgdl, 0);
    }

    private static RealtimeReading reading(int lifeCount, Integer mgdl, int qualityWord) {
        byte[] b = new byte[RealtimeReading.SIZE];
        putU16(b, 0, lifeCount);
        putU16(b, 2, qualityWord);
        b[14] = 3;
        putU16(b, 15, mgdl == null ? 0 : mgdl);
        System.arraycopy(FAST_DATA, 0, b, 21, FAST_DATA.length);
        return RealtimeReading.parse(b);
    }

    /** One historic page from {@code start}, values {@code first}, {@code first + 1}, … five minutes apart. */
    private static List<HistoricalPage.Entry> entries(int start, int first) {
        byte[] b = new byte[HistoricalPage.SIZE];
        putU16(b, 0, start);
        for (int i = 0; i < 6; i++) {
            putU16(b, 2 + 2 * i, first + i);
        }
        return HistoricalPage.parse(b);
    }

    private static void putU16(byte[] b, int offset, int value) {
        b[offset] = (byte) value;
        b[offset + 1] = (byte) (value >> 8);
    }

    private static String text(List<StatusItem> items) {
        StringBuilder sb = new StringBuilder();
        for (StatusItem item : items) {
            sb.append(item.name).append(": ").append(item.value).append('\n');
        }
        return sb.toString();
    }
}
