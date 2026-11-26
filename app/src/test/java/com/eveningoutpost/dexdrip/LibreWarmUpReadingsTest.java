package com.eveningoutpost.dexdrip;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreOOPAlgorithm;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for readings taken during a Libre sensor's warm-up.
 * <p>
 * The vendor app shows no readings during the first 60 minutes after a sensor starts, but the sensor still sends
 * them, both as live readings and in the history it repeats afterwards. Those values can be thousands of mg/dL off,
 * so xDrip must not store them. The sensor's own clock (minutes since activation) decides, so the user-entered start
 * time and the phone clock play no part.
 * <p>
 * Each test feeds one decoded Bluetooth packet the way the OOP2 decoder hands it to xDrip: 7 live readings (at 0, 2,
 * 4, 6, 7, 12 and 15 minutes ago) and 3 history readings on the sensor's 15-minute grid.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class LibreWarmUpReadingsTest extends RobolectricTestWithConfig {

    private static final String ALGORITHM_TYPE = "calibrate_external_libre_2_algorithm_type";
    private static final String SENSOR_AGE = "nfc_sensor_age";
    private static final String SMOOTHING = "libre_use_smoothed_data";
    private static final int WARM_UP_VALUE = 3000;
    private static final int VALID_VALUE = 120;
    private static final int PACKET_SIZE = 44;

    private long now;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        Pref.setString(ALGORITHM_TYPE, "no_calibration"); // store the decoder's values, no calibration needed
        Pref.removeItem(SENSOR_AGE);
        Sensor.create(1000);
        BgReading.deleteALL();
        now = JoH.tsl();
    }

    @After
    public void tearDown() {
        BgReading.deleteALL();
        Sensor.stopSensor();
        Pref.removeItem(ALGORITHM_TYPE);
        Pref.removeItem(SENSOR_AGE);
        Pref.removeItem(SMOOTHING);
    }

    // ===== Live readings =============================================================================================

    /**
     * A packet sent early in the warm-up stores no reading. Sensor age 10 keeps the history to a single record, so only
     * the live readings are at stake.
     */
    @Test
    public void packetEarlyInWarmUpStoresNothing() {
        // :: Act
        receive(10, WARM_UP_VALUE, WARM_UP_VALUE);

        // :: Verify
        assertThat(BgReading.latest(100)).isEmpty();
    }

    /** A packet sent in the last minute of the warm-up stores no reading, live or from the history. */
    @Test
    public void packetDuringWarmUpStoresNothing() {
        // :: Act
        receive(59, WARM_UP_VALUE, WARM_UP_VALUE);

        // :: Verify
        assertThat(BgReading.latest(100)).isEmpty();
    }

    /**
     * A packet sent during the warm-up still advances the sensor age shown on the home screen. Sensor age 10 keeps the
     * history to a single record, so the packet is handled the same way before and after the warm-up filter.
     */
    @Test
    public void packetDuringWarmUpAdvancesSensorAge() {
        // :: Act
        receive(10, WARM_UP_VALUE, WARM_UP_VALUE);

        // :: Verify
        assertThat(Pref.getInt(SENSOR_AGE, 0)).isEqualTo(10);
    }

    /** The reading taken as the 60-minute warm-up ends is stored, and nothing before it is. */
    @Test
    public void readingAtEndOfWarmUpIsStored() {
        // :: Act
        receive(60, VALID_VALUE, WARM_UP_VALUE);

        // :: Verify
        final List<BgReading> stored = BgReading.latest(100);
        assertThat(stored).isNotEmpty();
        assertThat(stored.get(0).calculated_value).isEqualTo((double) VALID_VALUE);
        assertNoneBefore(stored, 60, 60);
    }

    // ===== Readings from the sensor's history ========================================================================

    /**
     * Shortly after the warm-up the sensor's history still covers the warm-up (minutes 30 and 45 at sensor age 75).
     * Those history readings are left out, while the live readings after the warm-up are stored.
     */
    @Test
    public void historyFromWarmUpIsNotStored() {
        // :: Act
        receive(75, VALID_VALUE, WARM_UP_VALUE);

        // :: Verify
        final List<BgReading> stored = BgReading.latest(100);
        assertThat(stored).isNotEmpty();
        for (final BgReading reading : stored) {
            assertThat(reading.calculated_value).isEqualTo((double) VALID_VALUE);
        }
        assertNoneBefore(stored, 75, 60);
    }

    // ===== Smoothed readings =========================================================================================

    /**
     * With smoothing on, each reading is averaged with the readings of the minutes before it. Just after the warm-up
     * those earlier minutes are warm-up readings, and they are left out of the average.
     */
    @Test
    public void smoothedReadingJustAfterWarmUpLeavesOutWarmUpValues() {
        // :: Setup
        Pref.setBoolean(SMOOTHING, true);

        // :: Act
        receive(62, VALID_VALUE, WARM_UP_VALUE);

        // :: Verify
        final List<BgReading> stored = BgReading.latest(100);
        assertThat(stored).isNotEmpty();
        for (final BgReading reading : stored) {
            assertThat(reading.calculated_value).isEqualTo((double) VALID_VALUE);
        }
    }

    /** With smoothing on, a packet sent during the warm-up still advances the sensor age shown on the home screen. */
    @Test
    public void smoothedPacketDuringWarmUpAdvancesSensorAge() {
        // :: Setup
        Pref.setBoolean(SMOOTHING, true);

        // :: Act
        receive(10, WARM_UP_VALUE, WARM_UP_VALUE);

        // :: Verify
        assertThat(Pref.getInt(SENSOR_AGE, 0)).isEqualTo(10);
    }

    // ===== Helpers ===================================================================================================

    /**
     * Feeds one decoded packet at sensor age {@code sensorTime}. Readings at minute 60 or later carry
     * {@code validValue}, earlier ones {@code warmUpValue}.
     */
    private void receive(final int sensorTime, final int validValue, final int warmUpValue) {
        final int[] trendMinutesAgo = {0, 2, 4, 6, 7, 12, 15};
        final int[] trend = new int[7];
        for (int i = 0; i < trend.length; i++) {
            trend[i] = sensorTime - trendMinutesAgo[i] >= 60 ? validValue : warmUpValue;
        }
        final int historyStart = (sensorTime - 2) / 15 * 15;
        final int[] history = new int[3];
        for (int i = 0; i < history.length; i++) {
            history[i] = historyStart - i * 15 >= 60 ? validValue : warmUpValue;
        }
        LibreOOPAlgorithm.handleDecodedBleResult(now, packet(sensorTime), patchUid(), trend, history);
    }

    /** A decoded Bluetooth packet: 10 records with a non-zero raw value, then the sensor age in minutes. */
    private static byte[] packet(final int sensorTime) {
        final byte[] packet = new byte[PACKET_SIZE];
        for (int record = 0; record < 10; record++) {
            packet[record * 4] = 0x10; // raw value 16, low bits of the record
        }
        packet[40] = (byte) (sensorTime & 0xFF);
        packet[41] = (byte) ((sensorTime >> 8) & 0xFF);
        return packet;
    }

    /** A serial unique to each test, so cached trend data from an earlier test belongs to another sensor. */
    private static byte[] patchUid() {
        final long unique = System.nanoTime();
        final byte[] uid = new byte[8];
        for (int i = 0; i < uid.length; i++) {
            uid[i] = (byte) (unique >> (i * 8));
        }
        return uid;
    }

    /** No stored reading is older than minute {@code minute} of a sensor read at age {@code sensorTime}. */
    private void assertNoneBefore(final List<BgReading> stored, final int sensorTime, final int minute) {
        final long earliest = now - (sensorTime - minute) * 60_000L;
        for (final BgReading reading : stored) {
            assertThat(reading.timestamp).isAtLeast(earliest);
        }
    }
}
