package xrwd.li3.data;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.List;

import xrwd.li3.data.Lifecycle.Phase;
import xrwd.li3.data.PatchStatus.Attention;
import xrwd.li3.data.RealtimeReading.Trend;
import xrwd.li3.util.Hex;

/**
 * Tests for the decrypted data-plane records — realtime reading, patch status, historic page — and the sensor
 * lifecycle that gates them. Vectors are live plaintexts from RoundWhiteDiscKit 7dd6396 {@code DataPlaneTests.swift}
 * and {@code PublicSmokeTests.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class RecordDecodersTest {

    /** A live 98 mg/dL reading at life count 14563 (DataPlaneTests.swift). */
    private static final String LIVE_98 = "e3386200d6ff0000f023d1386c000b62006c00200a9e03113d03100000";

    /** A clean 151 mg/dL reading at life count 1073 (PublicSmokeTests.swift). */
    private static final String USABLE_151 = "3104970044000000e43e1f0484000b970084000080f006584b760e0008";

    /** A healthy live patch status at life count 14564. */
    private static final String LIVE_STATUS = "9d280000e0000704e4381300";

    // ================================================ RealtimeReading ================================================

    /** Four consecutive live readings decode to their life count, glucose, trend and fast data. */
    @Test
    public void realtime_liveReadings_decode() {
        // :: Setup
        String[][] cases = {
                {"e3386200d6ff0000f023d1386c000b62006c00200a9e03113d03100000", "14563", "98", "9e03113d03100000"},
                {"e4386200d6ff0000f023d1386c000b62006c001f0a9b03093d0b100000", "14564", "98", "9b03093d0b100000"},
                {"e5386100d9ff00008c23d1386c000b61006c001e0a9403053d12100000", "14565", "97", "9403053d12100000"},
                {"e6386100daff00008c23d1386c000b61006c001d0a9203003dfb0f0000", "14566", "97", "9203003dfb0f0000"},
        };

        for (String[] c : cases) {
            // :: Act
            RealtimeReading reading = RealtimeReading.parse(Hex.toBytes(c[0]));

            // :: Verify
            assertThat(reading.lifeCount()).isEqualTo(Integer.parseInt(c[1]));
            assertThat(reading.mgdl()).isEqualTo(Integer.parseInt(c[2]));
            assertThat(reading.trend()).isEqualTo(Trend.STABLE);
            assertThat(reading.dataQualityError()).isEqualTo(0);
            assertThat(reading.sensorCondition()).isEqualTo(0);
            assertThat(reading.displayable()).isTrue();
            assertThat(Hex.of(reading.fastData())).isEqualTo(c[3]);
        }
    }

    /** The live reading the glucose channel decrypted is 103 mg/dL at life count 14554. */
    @Test
    public void realtime_liveDecryptedPlaintext_decodes() {
        // :: Act
        RealtimeReading reading = RealtimeReading.parse(
                Hex.toBytes("da386700e3ff0000ac26c73871000b670071001a0abf03f33c59100000"));

        // :: Verify
        assertThat(reading.lifeCount()).isEqualTo(14554);
        assertThat(reading.mgdl()).isEqualTo(103);
    }

    /** A clean reading with good data quality is displayable. */
    @Test
    public void realtime_cleanReading_displayable() {
        // :: Act
        RealtimeReading reading = RealtimeReading.parse(Hex.toBytes(USABLE_151));

        // :: Verify
        assertThat(reading.lifeCount()).isEqualTo(1073);
        assertThat(reading.mgdl()).isEqualTo(151);
        assertThat(reading.trend()).isEqualTo(Trend.STABLE);
        assertThat(reading.displayable()).isTrue();
    }

    /** Values under 39 read as LO (39) and over 501 as HI (501), as the official app shows them. */
    @Test
    public void realtime_outOfDisplayRange_clampsToLoHi() {
        // :: Act
        RealtimeReading low = RealtimeReading.parse(withU16(LIVE_98, 15, 38));
        RealtimeReading high = RealtimeReading.parse(withU16(LIVE_98, 15, 600));

        // :: Verify
        assertThat(low.mgdl()).isEqualTo(39);
        assertThat(high.mgdl()).isEqualTo(501);
    }

    /** A reading RWDK marks as no value (0, or 1000 and up) has no mg/dL and is not displayable. */
    @Test
    public void realtime_noValue_mgdlNull() {
        // :: Act
        RealtimeReading zero = RealtimeReading.parse(withU16(LIVE_98, 15, 0));
        RealtimeReading tooHigh = RealtimeReading.parse(withU16(LIVE_98, 15, 1000));

        // :: Verify
        assertThat(zero.mgdl()).isNull();
        assertThat(zero.displayable()).isFalse();
        assertThat(tooHigh.mgdl()).isNull();
        assertThat(tooHigh.displayable()).isFalse();
    }

    /** A data-quality error in the current word blocks the reading even though a value is present. */
    @Test
    public void realtime_dataQualityError_notDisplayable() {
        // :: Act
        RealtimeReading reading = RealtimeReading.parse(withU16(LIVE_98, 2, 0x8062));
        RealtimeReading smoke = RealtimeReading.parse(withU16(USABLE_151, 2, 0x8097));

        // :: Verify
        assertThat(reading.mgdl()).isEqualTo(98);
        assertThat(reading.dataQualityError()).isEqualTo(0x8062);
        assertThat(reading.displayable()).isFalse();
        assertThat(smoke.dataQualityError()).isEqualTo(0x8097);
        assertThat(smoke.displayable()).isFalse();
    }

    /** A sensor-condition flag (bits 13–14) blocks the reading. */
    @Test
    public void realtime_sensorCondition_notDisplayable() {
        // :: Act
        RealtimeReading reading = RealtimeReading.parse(withU16(LIVE_98, 2, 0x2062));

        // :: Verify
        assertThat(reading.dataQualityError()).isEqualTo(0);
        assertThat(reading.sensorCondition()).isEqualTo(1);
        assertThat(reading.displayable()).isFalse();
    }

    /** The not-actionable flag is advisory: an otherwise clean reading stays displayable. */
    @Test
    public void realtime_notActionable_stillDisplayable() {
        // :: Setup
        byte[] plain = Hex.toBytes(USABLE_151);
        plain[14] = 0x03;

        // :: Act
        RealtimeReading reading = RealtimeReading.parse(plain);

        // :: Verify
        assertThat(reading.trend()).isEqualTo(Trend.STABLE);
        assertThat(reading.displayable()).isTrue();
    }

    /** Trend bits 6 and 7 have no meaning and decode as unknown. */
    @Test
    public void realtime_undefinedTrend_unknown() {
        // :: Setup
        byte[] plain = Hex.toBytes(USABLE_151);
        plain[14] = 0x0e;

        // :: Act
        RealtimeReading reading = RealtimeReading.parse(plain);

        // :: Verify
        assertThat(reading.trend()).isEqualTo(Trend.UNKNOWN);
    }

    /** A plaintext of the wrong size is rejected. */
    @Test
    public void realtime_wrongSize_throws() {
        // :: Act
        ThrowingRunnable parse = () -> RealtimeReading.parse(new byte[28]);

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    // ================================================== PatchStatus ==================================================

    /** The live status decodes to an active sensor with no error. */
    @Test
    public void patchStatus_live_decodes() {
        // :: Act
        PatchStatus status = PatchStatus.parse(Hex.toBytes(LIVE_STATUS));

        // :: Verify
        assertThat(status.currentLifeCount()).isEqualTo(14564);
        assertThat(status.state()).isEqualTo(4);
        assertThat(status.errorCode()).isEqualTo(0);
        assertThat(status.attention()).isEqualTo(Attention.NONE);
    }

    /** The status the live glucose channel decrypted carries life count 14555. */
    @Test
    public void patchStatus_liveDecryptedPlaintext_decodes() {
        // :: Act
        PatchStatus status = PatchStatus.parse(Hex.toBytes("9d280000e0000704db381607"));

        // :: Verify
        assertThat(status.currentLifeCount()).isEqualTo(14555);
    }

    /** Error codes map as RWDK: 3 and 7 check sensor, 5 and 6 ended, 8 replace, anything else unknown. */
    @Test
    public void patchStatus_errorCodes_mapToAttention() {
        // :: Act
        Attention check = statusWithError("0300").attention();

        // :: Verify
        assertThat(check).isEqualTo(Attention.CHECK_SENSOR);
        assertThat(statusWithError("0500").attention()).isEqualTo(Attention.SENSOR_ENDED);
        assertThat(statusWithError("0600").attention()).isEqualTo(Attention.SENSOR_ENDED);
        assertThat(statusWithError("0700").attention()).isEqualTo(Attention.CHECK_SENSOR);
        assertThat(statusWithError("0800").attention()).isEqualTo(Attention.REPLACE_SENSOR);
        assertThat(statusWithError("0900").attention()).isEqualTo(Attention.UNKNOWN);
        assertThat(statusWithError("0900").errorCode()).isEqualTo(9);
    }

    /** With no error code, the patch state decides: 3 check, 5 and 6 ended, 7 and 8 replace, others none. */
    @Test
    public void patchStatus_patchStates_mapToAttention() {
        // :: Act
        Attention check = statusWithState(3).attention();

        // :: Verify
        assertThat(check).isEqualTo(Attention.CHECK_SENSOR);
        assertThat(statusWithState(4).attention()).isEqualTo(Attention.NONE);
        assertThat(statusWithState(5).attention()).isEqualTo(Attention.SENSOR_ENDED);
        assertThat(statusWithState(6).attention()).isEqualTo(Attention.SENSOR_ENDED);
        assertThat(statusWithState(7).attention()).isEqualTo(Attention.REPLACE_SENSOR);
        assertThat(statusWithState(8).attention()).isEqualTo(Attention.REPLACE_SENSOR);
        assertThat(statusWithState(9).attention()).isEqualTo(Attention.NONE);
        assertThat(statusWithState(9).state()).isEqualTo(9);
    }

    /** A plaintext of the wrong size is rejected. */
    @Test
    public void patchStatus_wrongSize_throws() {
        // :: Act
        ThrowingRunnable parse = () -> PatchStatus.parse(new byte[11]);

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    // ================================================ HistoricalPage =================================================

    /** A live page holds six readings five life counts apart. */
    @Test
    public void historicalPage_live_decodes() {
        // :: Act
        List<HistoricalPage.Entry> entries = HistoricalPage.parse(Hex.toBytes("8e0369006b006d006b0069006e00"));

        // :: Verify
        assertThat(entries).hasSize(6);
        int[] values = {105, 107, 109, 107, 105, 110};
        for (int i = 0; i < 6; i++) {
            assertThat(entries.get(i).lifeCount).isEqualTo(910 + 5 * i);
            assertThat(entries.get(i).mgdl).isEqualTo(values[i]);
        }
    }

    /** The page the live historic channel decrypted starts at 920. */
    @Test
    public void historicalPage_liveDecryptedPlaintext_decodes() {
        // :: Act
        List<HistoricalPage.Entry> entries = HistoricalPage.parse(Hex.toBytes("98036d006b0069006e006a006500"));

        // :: Verify
        assertThat(entries.get(0).lifeCount).isEqualTo(920);
        assertThat(entries.get(5).lifeCount).isEqualTo(945);
        assertThat(entries.get(5).mgdl).isEqualTo(101);
    }

    /** An entry with no value has no mg/dL, so backfill skips it. */
    @Test
    public void historicalPage_noValue_mgdlNull() {
        // :: Act
        List<HistoricalPage.Entry> entries = HistoricalPage.parse(Hex.toBytes("8e0300006b006d006b0069006e00"));

        // :: Verify
        assertThat(entries.get(0).mgdl).isNull();
        assertThat(entries.get(1).mgdl).isEqualTo(107);
    }

    /** A plaintext of the wrong size is rejected. */
    @Test
    public void historicalPage_wrongSize_throws() {
        // :: Act
        ThrowingRunnable parse = () -> HistoricalPage.parse(new byte[12]);

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    // =================================================== Lifecycle ===================================================

    /** A 12-minute-old sensor with a 60-minute warm-up is warming up with 48 minutes left. */
    @Test
    public void lifecycle_twelveMinutes_warmingUp() {
        // :: Setup
        PatchStatus status = PatchStatus.parse(Hex.toBytes("0c000000000000040c000000"));
        Lifecycle lifecycle = new Lifecycle(60, 21600);

        // :: Act
        Phase phase = lifecycle.phase(status.currentLifeCount());

        // :: Verify
        assertThat(phase).isEqualTo(Phase.WARMUP);
        assertThat(lifecycle.remainingWarmupMinutes(12)).isEqualTo(48);
        assertThat(lifecycle.remainingWearMinutes(12)).isEqualTo(21588);
        assertThat(lifecycle.usable(12)).isFalse();
    }

    /** The live sensor at 14564 of 20160 minutes is active with 5596 minutes left. */
    @Test
    public void lifecycle_liveSensor_active() {
        // :: Setup
        Lifecycle lifecycle = new Lifecycle(Lifecycle.DEFAULT_WARMUP_MINUTES, 20160);

        // :: Act
        Phase phase = lifecycle.phase(14564);

        // :: Verify
        assertThat(Lifecycle.DEFAULT_WARMUP_MINUTES).isEqualTo(60);
        assertThat(phase).isEqualTo(Phase.ACTIVE);
        assertThat(lifecycle.remainingWearMinutes(14564)).isEqualTo(5596);
        assertThat(lifecycle.usable(14564)).isTrue();
    }

    /** Readings count from the end of warm-up up to, not including, the end of wear. */
    @Test
    public void lifecycle_usable_boundaries() {
        // :: Setup
        Lifecycle lifecycle = new Lifecycle(60, 21600);

        // :: Act
        boolean lastWarmupMinute = lifecycle.usable(59);
        boolean firstActiveMinute = lifecycle.usable(60);

        // :: Verify
        assertThat(lastWarmupMinute).isFalse();
        assertThat(firstActiveMinute).isTrue();
        assertThat(lifecycle.usable(21599)).isTrue();
        assertThat(lifecycle.usable(21600)).isFalse();
        assertThat(lifecycle.phase(21600)).isEqualTo(Phase.EXPIRED);
        assertThat(lifecycle.remainingWearMinutes(30000)).isEqualTo(0);
    }

    private static byte[] withU16(String hex, int offset, int value) {
        byte[] bytes = Hex.toBytes(hex);
        bytes[offset] = (byte) value;
        bytes[offset + 1] = (byte) (value >> 8);
        return bytes;
    }

    private static PatchStatus statusWithError(String errorWordHex) {
        return PatchStatus.parse(Hex.toBytes("9d28" + errorWordHex + "e0000704e4381300"));
    }

    private static PatchStatus statusWithState(int state) {
        byte[] bytes = Hex.toBytes(LIVE_STATUS);
        bytes[7] = (byte) state;
        return PatchStatus.parse(bytes);
    }
}
