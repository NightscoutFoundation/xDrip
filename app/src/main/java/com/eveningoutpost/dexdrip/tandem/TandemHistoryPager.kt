package com.eveningoutpost.dexdrip.tandem

import android.os.Handler
import com.eveningoutpost.dexdrip.models.APStatus
import com.eveningoutpost.dexdrip.models.Treatments
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HistoryLogRequest
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BasalRateChangeHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolexCompletedHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.BolusCompletedHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.CarbEnteredHistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLog
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLogStreamResponse
import kotlin.math.min

/**
 * Incremental Tandem history-log pull: chunked requests, in-order stream handling, a persisted
 * sequence cursor (so reconnects only fetch new records), de-duplication, a stall watchdog, and
 * mapping each record into xDrip's native stores (Treatments / APStatus, gated by [TandemSync]).
 *
 * Split out of [TandemPumpController] so the paging state machine is isolated from the BLE / pairing
 * lifecycle. It never touches Bluetooth, the pump or the UI directly — the owner wires sending and
 * progress reporting through [Callback]; all scheduling uses the supplied [sender] handler.
 */
class TandemHistoryPager(
    private val sender: Handler,
    private val clockOffsetMs: () -> Long,
    private val callback: Callback
) {
    interface Callback {
        /** Send a history request to the pump (a no-op if not currently connected). */
        fun sendHistory(msg: Message)
        fun onStatus(text: String)
        fun onLog(line: String)
        /** Running progress: records seen so far, total expected this session, and import counts. */
        fun onProgress(received: Int, total: Long, boluses: Int, carbs: Int, basal: Int)
        /** History pull finished for this connection. */
        fun onComplete(boluses: Int, carbs: Int, basal: Int)
    }

    private companion object {
        const val HISTORY_CHUNK = 250
        // First sync only pulls a recent window — the pump can hold hundreds of thousands of logs and
        // fetching them all over an unstable BLE link never completes. Reconnects extend forward via
        // the persisted cursor.
        const val MAX_INITIAL_LOGS = 2000L
        const val WATCHDOG_MS = 2500L
        const val MAX_STALLS = 12
        const val CURSOR_KEY = "tandem_last_seq"
    }

    private val seenSeq = HashSet<Long>()
    private var histLast = 0L
    private var startSeq = 0L
    private var nextSeq = 0L
    private var chunkStart = 0L
    private var chunkEndExcl = 0L
    private var stalls = 0
    private var lastSeenAtWatchdog = -1
    @Volatile private var maxSeqSeen = 0L
    @Volatile private var started = false
    @Volatile private var complete = false
    @Volatile private var stopped = false

    // Cumulative xDrip inserts for the controller's lifetime (de-dup means re-syncs don't re-count).
    private var boluses = 0
    private var carbs = 0
    private var basal = 0

    /** Prepare for a fresh pull on (re)connect; keeps the de-dup set + cursor so we only fetch new. */
    fun reset() {
        stopped = false; started = false; complete = false
        stalls = 0; lastSeenAtWatchdog = -1
        sender.removeCallbacks(watchdog)
    }

    /** Reset the cursor + de-dup so the next pull re-fetches the whole recent window. */
    fun resetCursor() {
        PersistentStore.setLong(CURSOR_KEY, 0)
        seenSeq.clear()
        reset()
    }

    /** Stop scheduling (on disconnect / disable). */
    fun stop() {
        stopped = true
        sender.removeCallbacks(watchdog)
    }

    fun onStatusResponse(resp: HistoryLogStatusResponse) {
        if (started) return
        started = true; complete = false
        histLast = resp.lastSequenceNum
        val cursor = PersistentStore.getLong(CURSOR_KEY)
        startSeq = TandemMapping.firstSyncStartSeq(resp.firstSequenceNum, histLast, cursor, MAX_INITIAL_LOGS)
        nextSeq = startSeq
        maxSeqSeen = startSeq - 1
        val total = sessionTotal()
        callback.onProgress(seenSeq.size, total, boluses, carbs, basal)
        callback.onLog("HISTORY: seq $startSeq..$histLast ($total new, cursor=$cursor)")
        if (total <= 0) { finish(); return }
        requestNextChunk(); scheduleWatchdog()
    }

    fun onStreamResponse(resp: HistoryLogStreamResponse) {
        for (hl in (resp.historyLogs ?: emptyList())) {
            val seq = hl.sequenceNum
            if (seq > maxSeqSeen) maxSeqSeen = seq
            if (!seenSeq.add(seq)) continue
            ingest(hl)
        }
        callback.onProgress(seenSeq.size, sessionTotal(), boluses, carbs, basal)
        // The pump streams logs in ascending order; the moment we've seen the chunk's last sequence,
        // request the next chunk immediately rather than waiting on the stall watchdog.
        if (!complete && maxSeqSeen >= chunkEndExcl - 1) requestNextChunk()
    }

    private fun sessionTotal(): Long = if (histLast >= startSeq) histLast - startSeq + 1 else 0L

    private fun requestNextChunk() {
        if (stopped) return
        if (nextSeq > histLast) { finish(); return }
        chunkStart = nextSeq
        val count = min(HISTORY_CHUNK.toLong(), histLast - chunkStart + 1).toInt()
        chunkEndExcl = chunkStart + count; nextSeq = chunkEndExcl
        callback.sendHistory(HistoryLogRequest(chunkStart, count))
    }

    private fun scheduleWatchdog() { sender.postDelayed(watchdog, WATCHDOG_MS) }
    private val watchdog = object : Runnable {
        override fun run() {
            if (stopped || complete) return
            if (nextSeq > histLast) { finish(); return }
            if (seenSeq.size == lastSeenAtWatchdog) {
                stalls++; callback.onLog("HISTORY stall ($stalls) at ${seenSeq.size}")
                if (stalls >= MAX_STALLS) { finish(); return }
                requestNextChunk()
            }
            lastSeenAtWatchdog = seenSeq.size
            sender.postDelayed(this, WATCHDOG_MS)
        }
    }

    private fun finish() {
        if (complete) return
        complete = true
        sender.removeCallbacks(watchdog)
        val maxSeen = seenSeq.maxOrNull() ?: 0L
        if (maxSeen > 0) PersistentStore.setLong(CURSOR_KEY, maxOf(PersistentStore.getLong(CURSOR_KEY), maxSeen))
        callback.onComplete(boluses, carbs, basal)
    }

    private fun ingest(hl: HistoryLog) {
        val ts = Dates.fromJan12008ToUnixEpochSeconds(hl.pumpTimeSec) * 1000L + clockOffsetMs()
        when (hl) {
            is BolusCompletedHistoryLog -> if (TandemSync.isOn(TandemSync.BOLUSES)) addBolus(hl.sequenceNum, hl.insulinDelivered.toDouble(), ts)
            is BolexCompletedHistoryLog -> if (TandemSync.isOn(TandemSync.BOLUSES)) addBolus(hl.sequenceNum, hl.insulinDelivered.toDouble(), ts)
            is CarbEnteredHistoryLog -> if (TandemSync.isOn(TandemSync.CARBS)) addCarbs(hl.sequenceNum, hl.carbs.toDouble(), ts)
            is BasalRateChangeHistoryLog -> if (TandemSync.isOn(TandemSync.BASAL)) addBasal(hl.commandBasalRate.toDouble(), hl.baseBasalRate.toDouble(), ts)
            else -> {}
        }
    }

    private fun addBolus(seq: Long, units: Double, ts: Long) {
        if (units <= 0.0) return
        val uuid = TandemMapping.eventUuid("bolus", seq)
        try { if (Treatments.byuuid(uuid) == null) { Treatments.create(0.0, units, ts, uuid); boluses++ } }
        catch (t: Throwable) { callback.onLog("bolus insert failed seq$seq: ${t.message}") }
    }

    private fun addCarbs(seq: Long, grams: Double, ts: Long) {
        if (grams <= 0.0) return
        val uuid = TandemMapping.eventUuid("carb", seq)
        try { if (Treatments.byuuid(uuid) == null) { Treatments.create(grams, 0.0, ts, uuid); carbs++ } }
        catch (t: Throwable) { callback.onLog("carb insert failed seq$seq: ${t.message}") }
    }

    private fun addBasal(rateUperHr: Double, profileRate: Double, ts: Long) {
        if (rateUperHr < 0.0) return
        // xDrip's basal line plots basal_percent (delivered vs profile base); the log carries both, so
        // derive the TBR% directly (createEfficientRecord can't, without an active xDrip profile).
        val pct = TandemMapping.basalPercent(rateUperHr, profileRate)
        try { APStatus.createEfficientRecord(ts, pct, rateUperHr); basal++ }
        catch (t: Throwable) { callback.onLog("basal insert failed: ${t.message}") }
    }
}
