package com.eveningoutpost.dexdrip.tandem

import java.util.UUID

/**
 * Pure, side-effect-free conversions from Tandem pump data into the shapes xDrip stores.
 *
 * Deliberately split out of [TandemPumpController] (which owns the BLE / pumpX2 lifecycle and all
 * the Android wiring) so this logic has no Android, Bluetooth or pump dependencies and is trivially
 * unit-tested — see `TandemMappingTest`.
 */
object TandemMapping {

    /** xDrip's basal profile is a fixed 24 hourly blocks; the editor ignores any other length. */
    const val PROFILE_BLOCKS = 24

    /**
     * Stable, deterministic id for a pump history record so re-syncing the same record never
     * double-inserts into xDrip (boluses/carbs are de-duplicated by this uuid).
     */
    fun eventUuid(kind: String, sequenceNum: Long): String =
        UUID.nameUUIDFromBytes("tandem-$kind-$sequenceNum".toByteArray()).toString()

    /**
     * Temp-basal percent (delivered vs the profile base rate) — the value xDrip's basal line plots.
     * 100 when running the profile, <100 when reduced/suspended, >100 when boosted, and 100 when the
     * base rate is unknown (avoids a divide-by-zero and a misleading 0%).
     */
    fun basalPercent(deliveredRate: Double, profileBaseRate: Double): Int =
        if (profileBaseRate > 0.0) Math.round(deliveredRate / profileBaseRate * 100.0).toInt() else 100

    /**
     * Sequence number to begin a history pull from:
     *  - incremental sync (cursor already advanced) -> only records newer than the cursor;
     *  - first sync (cursor == 0) -> a recent window of at most [maxInitialLogs] records, so we never
     *    try to pull the pump's entire (potentially hundreds of thousands of) log over BLE.
     * Always clamped to the pump's oldest still-available sequence ([firstSeq]).
     */
    fun firstSyncStartSeq(firstSeq: Long, lastSeq: Long, cursor: Long, maxInitialLogs: Long): Long =
        if (cursor > 0) maxOf(firstSeq, cursor + 1)
        else maxOf(firstSeq, lastSeq - maxInitialLogs + 1)

    /**
     * Expand a pump IDP basal profile (segment-start-minute -> rate U/hr, e.g. `{0:0.85, 360:0.65}`)
     * into xDrip's fixed [PROFILE_BLOCKS] hourly blocks. Each hour takes the rate of the segment in
     * effect at the start of that hour; hours before the first segment wrap to the last segment
     * (covering the midnight boundary). Returns empty for an empty input.
     */
    fun expandProfileToHourlyBlocks(segmentStartMinuteToRate: Map<Int, Double>): List<Double> {
        if (segmentStartMinuteToRate.isEmpty()) return emptyList()
        val starts = segmentStartMinuteToRate.keys.sorted()
        val wrapRate = segmentStartMinuteToRate[starts.last()] ?: 0.0
        return (0 until PROFILE_BLOCKS).map { hour ->
            val minute = hour * 60
            var rate = wrapRate
            for (s in starts) { if (s <= minute) rate = segmentStartMinuteToRate[s] ?: rate else break }
            rate
        }
    }

    /**
     * Control-IQ target BG in mg/dL from the pump's CurrentActiveIdpValues message. pumpX2 reads this
     * field as a 16-bit value, but its high byte overlaps the adjacent insulin-duration field, so that
     * byte can leak in — e.g. with a 300-minute duration (0x012C) the low byte 0x2C lands in target's
     * high byte: 110 + (0x2C shl 8) = 11374, which then renders as ~631 mmol/L. Target BG is always a
     * single byte (< 256 mg/dL), so masking to the low byte recovers the real value (11374 -> 110).
     */
    fun targetBgMgdl(rawTargetBg: Int): Int = rawTargetBg and 0xFF
}
