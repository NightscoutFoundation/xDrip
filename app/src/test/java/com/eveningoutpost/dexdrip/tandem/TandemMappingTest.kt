package com.eveningoutpost.dexdrip.tandem

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [TandemMapping] — the pure pump-data -> xDrip conversions. No Android, Bluetooth or
 * pump dependencies, so these run as plain fast JVM tests.
 */
class TandemMappingTest {

    // ---- eventUuid: de-duplication key for re-synced records ----

    @Test
    fun eventUuid_isDeterministic() {
        // :: Act / Verify
        assertThat(TandemMapping.eventUuid("bolus", 42))
            .isEqualTo(TandemMapping.eventUuid("bolus", 42))
    }

    @Test
    fun eventUuid_distinctPerKindAndSequence_andValidShape() {
        assertThat(TandemMapping.eventUuid("bolus", 42)).isNotEqualTo(TandemMapping.eventUuid("carb", 42))
        assertThat(TandemMapping.eventUuid("bolus", 42)).isNotEqualTo(TandemMapping.eventUuid("bolus", 43))
        assertThat(TandemMapping.eventUuid("bolus", 1)).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    // ---- basalPercent: delivered vs profile base (what xDrip's basal line plots) ----

    @Test
    fun basalPercent_isHundredWhenRunningProfile() {
        assertThat(TandemMapping.basalPercent(0.85, 0.85)).isEqualTo(100)
    }

    @Test
    fun basalPercent_reflectsDeviation() {
        assertThat(TandemMapping.basalPercent(0.0, 0.85)).isEqualTo(0)    // suspended
        assertThat(TandemMapping.basalPercent(1.70, 0.85)).isEqualTo(200) // boosted
        assertThat(TandemMapping.basalPercent(0.425, 0.85)).isEqualTo(50) // halved
    }

    @Test
    fun basalPercent_safe100WhenBaseRateUnknown() {
        // avoids divide-by-zero and a misleading 0%
        assertThat(TandemMapping.basalPercent(0.85, 0.0)).isEqualTo(100)
    }

    // ---- firstSyncStartSeq: history window selection ----

    @Test
    fun firstSyncStartSeq_firstSync_capsToRecentWindow() {
        // cursor 0 -> start = lastSeq - maxInitial + 1
        assertThat(TandemMapping.firstSyncStartSeq(firstSeq = 1_000, lastSeq = 10_000, cursor = 0, maxInitialLogs = 2_000))
            .isEqualTo(8_001)
    }

    @Test
    fun firstSyncStartSeq_firstSync_clampsToOldestAvailable() {
        // requested window is larger than what the pump still holds -> clamp to firstSeq
        assertThat(TandemMapping.firstSyncStartSeq(firstSeq = 9_500, lastSeq = 10_000, cursor = 0, maxInitialLogs = 2_000))
            .isEqualTo(9_500)
    }

    @Test
    fun firstSyncStartSeq_incremental_returnsOnlyRecordsNewerThanCursor() {
        assertThat(TandemMapping.firstSyncStartSeq(firstSeq = 1_000, lastSeq = 10_000, cursor = 9_990, maxInitialLogs = 2_000))
            .isEqualTo(9_991)
    }

    // ---- expandProfileToHourlyBlocks: IDP segments -> xDrip's 24 hourly blocks ----

    @Test
    fun expandProfile_returns24Blocks_withTheRateInEffectEachHour() {
        // 0.85 from 00:00, 0.65 from 06:00 (360 min), 0.90 from 18:00 (1080 min)
        val rates = TandemMapping.expandProfileToHourlyBlocks(mapOf(0 to 0.85, 360 to 0.65, 1080 to 0.90))

        assertThat(rates).hasSize(TandemMapping.PROFILE_BLOCKS) // 24 — required by xDrip's editor
        assertThat(rates[0]).isEqualTo(0.85)   // 00:00
        assertThat(rates[5]).isEqualTo(0.85)   // 05:00 still first segment
        assertThat(rates[6]).isEqualTo(0.65)   // 06:00 boundary
        assertThat(rates[17]).isEqualTo(0.65)  // 17:00
        assertThat(rates[18]).isEqualTo(0.90)  // 18:00 boundary
        assertThat(rates[23]).isEqualTo(0.90)  // 23:00
    }

    @Test
    fun expandProfile_singleSegment_isFlatFor24Hours() {
        val rates = TandemMapping.expandProfileToHourlyBlocks(mapOf(0 to 1.2))
        assertThat(rates).hasSize(24)
        assertThat(rates.toSet()).containsExactly(1.2)
    }

    @Test
    fun expandProfile_segmentNotStartingAtMidnight_wrapsToLastSegment() {
        // a single segment starting at 06:00 -> hours before it wrap around to that segment
        val rates = TandemMapping.expandProfileToHourlyBlocks(mapOf(360 to 0.7))
        assertThat(rates[0]).isEqualTo(0.7)
        assertThat(rates[6]).isEqualTo(0.7)
    }

    @Test
    fun expandProfile_emptyInput_returnsEmpty() {
        assertThat(TandemMapping.expandProfileToHourlyBlocks(emptyMap())).isEmpty()
    }

    // ---- targetBgMgdl: strip the insulin-duration byte pumpX2 leaks into the 16-bit target field ----

    @Test
    fun targetBgMgdl_masksLeakedHighByteFromInsulinDuration() {
        // 110 mg/dL with a 300-min (0x012C) duration leaking 0x2C into the high byte -> 11374.
        assertThat(TandemMapping.targetBgMgdl(110 + (0x2C shl 8))).isEqualTo(110)
    }

    @Test
    fun targetBgMgdl_leavesCleanValuesUnchanged() {
        assertThat(TandemMapping.targetBgMgdl(110)).isEqualTo(110)
        assertThat(TandemMapping.targetBgMgdl(0xFF)).isEqualTo(255) // max single-byte target
    }
}
