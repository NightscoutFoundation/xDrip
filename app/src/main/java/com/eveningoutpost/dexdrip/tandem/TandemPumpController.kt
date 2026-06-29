package com.eveningoutpost.dexdrip.tandem

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.eveningoutpost.dexdrip.models.APStatus
import com.welie.blessed.BondState
import com.jwoglom.pumpx2.pump.PumpState
import com.jwoglom.pumpx2.pump.TandemError
import com.jwoglom.pumpx2.pump.bluetooth.PumpReadyState
import com.jwoglom.pumpx2.pump.bluetooth.TandemBluetoothHandler
import com.jwoglom.pumpx2.pump.bluetooth.TandemConfig
import com.jwoglom.pumpx2.pump.bluetooth.TandemPump
import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.helpers.Dates
import com.jwoglom.pumpx2.pump.messages.models.InsulinUnit
import com.jwoglom.pumpx2.pump.messages.models.KnownDeviceModel
import com.jwoglom.pumpx2.pump.messages.models.PairingCodeType
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.HistoryLogStatusRequest
import com.jwoglom.pumpx2.pump.messages.response.authentication.AbstractCentralChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.authentication.AbstractPumpChallengeResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQIOBResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBasalStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentBatteryAbstractResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.HistoryLogStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.InsulinStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.TimeSinceResetResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentEGVGuiDataResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ControlIQInfoV1Response
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.CurrentActiveIdpValuesResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.ProfileStatusResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSettingsResponse
import com.jwoglom.pumpx2.pump.messages.response.currentStatus.IDPSegmentResponse
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentEGVGuiDataRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ControlIQInfoV1Request
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.CurrentActiveIdpValuesRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.ProfileStatusRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.IDPSettingsRequest
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.IDPSegmentRequest
import com.eveningoutpost.dexdrip.models.BgReading
import com.eveningoutpost.dexdrip.profileeditor.BasalProfile
import com.jwoglom.pumpx2.pump.messages.response.historyLog.HistoryLogStreamResponse
import com.jwoglom.pumpx2.pump.messages.response.qualifyingEvent.QualifyingEvent
import com.welie.blessed.BluetoothPeripheral
import com.welie.blessed.HciStatus
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

/**
 * READ-ONLY Tandem t:slim X2 / Mobi driver for xDrip+, built on pumpX2 (MIT).
 *
 * Owned by [TandemPumpService] (a foreground service) so the connection survives
 * backgrounding and process restarts, and auto-reconnects (like xDrip's InPen).
 *
 * Feeds xDrip NATIVE stores (no new display UI):
 *   boluses -> Treatments(insulin) ; carbs -> Treatments(carbs) ; basal -> APStatus(abs U/hr).
 * Reads are incremental across runs via a persisted sequence cursor.
 *
 * SAFETY: only currentStatus + historyLog READ requests are sent. CONTROL /
 * CONTROL_STREAM are never used; pumpX2 blocks insulin-delivery messages unless
 * enableActionsAffectingInsulinDelivery() is called — never called here.
 */
class TandemPumpController private constructor(
    private val appContext: Context
) {
    /** Reassigned whenever a (re)started service binds; may briefly be null. */
    @Volatile var listener: Listener? = null
    data class PumpMetadata(
        var model: String? = null,
        var connected: Boolean = false,
        var batteryPercent: Int? = null,
        var cartridgeUnits: Int? = null,
        var iobUnits: Double? = null,
        var currentBasal: Double? = null,
        var boluses: Int = 0,
        var carbs: Int = 0,
        var basal: Int = 0,
        var historyReceived: Int = 0,
        var historyTotal: Long = 0,
        var lastSync: Long = 0,
        // ---- extra live values shown on the "Pump" tab (not on xDrip's native screens) ----
        var glucoseMgdl: Int? = null,
        var trendRate: Double? = null,
        var closedLoop: Boolean? = null,
        var controlIqMode: String? = null,
        var tddUnits: Double? = null,
        var carbRatio: Double? = null,
        var isf: Int? = null,
        var targetBg: Int? = null,
        var insulinDurationMin: Int? = null,
        var basalProfileName: String? = null
    )

    /** High-level connection state used to drive button enable/disable in the UI. */
    enum class State { DISABLED, SCANNING, NEEDS_CODE, CONNECTED, SYNCING }

    interface Listener {
        fun onStatus(text: String)
        fun onLog(line: String)
        fun onNeedPairingCode(deviceName: String?)
        fun onConnected(modelName: String?)
        fun onMetadata(meta: PumpMetadata)
        fun onDone(meta: PumpMetadata)
        fun onError(text: String)
        fun onState(state: State)
    }

    companion object {
        private const val TAG = "TandemPump"
        // Process-wide singleton. pumpX2's TandemBluetoothHandler is itself a singleton bound to the
        // first Pump it is given, so there must be exactly ONE controller (one Pump, one handler, one
        // sender thread) for the app's lifetime — otherwise BLE callbacks land on a stale controller
        // whose sender thread has been quit. The service always (re)binds its listener via get().
        @Volatile private var INSTANCE: TandemPumpController? = null
        fun get(context: Context, listener: Listener): TandemPumpController {
            val existing = INSTANCE
            if (existing != null) { existing.listener = listener; return existing }
            return TandemPumpController(context.applicationContext).also { it.listener = listener; INSTANCE = it }
        }
        @Volatile private var loggingPlanted = false
        // pumpX2's TandemBluetoothHandler is a process singleton bound to the FIRST Pump it sees,
        // so all BLE callbacks fire on that Pump even after we create a new controller (e.g. after
        // Forget & re-pair). Track the peripheral + Pump that actually received the callbacks here,
        // so submitPairingCode() always pairs through the right one regardless of controller churn.
        @Volatile private var activePeripheral: BluetoothPeripheral? = null
        @Volatile private var activePump: TandemPump? = null
        @Volatile private var activeChallenge: AbstractCentralChallengeResponse? = null
    }

    private val main = Handler(Looper.getMainLooper())
    private val sender = HandlerThread("tandem-sender").also { it.start() }
    private val senderHandler = Handler(sender.looper)

    private var btHandler: TandemBluetoothHandler? = null
    private var pump: Pump? = null
    @Volatile private var peripheral: BluetoothPeripheral? = null
    @Volatile private var centralChallenge: AbstractCentralChallengeResponse? = null
    @Volatile private var stopped = false
    @Volatile private var keepConnected = true
    @Volatile private var connected = false

    // --- near-realtime updates: stay connected and re-pull on the pump's own push events, with a
    //     periodic poll as a backstop. refresh() is a cheap incremental pull (new history + the live
    //     status set: CGM / basal / IOB / battery). ---
    private val POLL_INTERVAL_MS = 30_000L  // backstop cadence while connected (events make it instant)
    private val EVENT_DEBOUNCE_MS = 700L    // coalesce a burst of qualifying events into one pull
    private val MIN_REFRESH_GAP_MS = 5_000L // never re-pull more often than this, whatever the trigger
    @Volatile private var lastLiveRefreshAt = 0L
    // The basal profile rarely changes and reading it chains to many segment requests, so pull it only
    // once per connection (and on explicit resync) rather than on every poll.
    @Volatile private var needProfileRead = true

    private val meta = PumpMetadata()
    // Pump clocks drift (this test pump was ~49 days behind). Anchor every history timestamp to the
    // phone's clock: offset = phone-now − pump-now. The newest pump event then maps to ~now and lands
    // in xDrip's graph window; relative spacing of older events is preserved.
    @Volatile private var pumpClockOffsetMs = 0L

    @Volatile private var state: State = State.DISABLED
    // Basal-profile (IDP) import: read the active profile's segments, then write xDrip's basal profile.
    private var idpActiveId = -1
    private var idpSegmentsExpected = 0
    private val idpRates = java.util.TreeMap<Int, Double>() // segment start-minute -> rate U/hr

    // The history-log paging state machine — fed the status/stream responses; reports back here.
    private val pager = TandemHistoryPager(senderHandler, { pumpClockOffsetMs }, object : TandemHistoryPager.Callback {
        override fun sendHistory(msg: Message) { peripheral?.let { safeSend(it, msg) } }
        override fun onStatus(text: String) = status(text)
        override fun onLog(line: String) = log(line)
        override fun onProgress(received: Int, total: Long, boluses: Int, carbs: Int, basal: Int) {
            meta.historyReceived = received; meta.historyTotal = total
            meta.boluses = boluses; meta.carbs = carbs; meta.basal = basal
            emitMeta()
        }
        override fun onComplete(boluses: Int, carbs: Int, basal: Int) {
            meta.boluses = boluses; meta.carbs = carbs; meta.basal = basal
            meta.lastSync = System.currentTimeMillis()
            status("Synced — $boluses boluses, $carbs carbs, $basal basal → xDrip.")
            main.post { listener?.onDone(meta) }
            emitState(State.CONNECTED)
            // Now (and only now) fetch the small status set for the Pump tab, paced one-by-one. If
            // basal-profile sync is on, append ProfileStatus — its response chains to IDP segments.
            val per = peripheral
            if (per != null && !stopped && connected) {
                val q = ArrayDeque<Message>(ReadRequests.all())
                if (TandemSync.isOn(TandemSync.PROFILE) && needProfileRead) {
                    q.add(ProfileStatusRequest()); needProfileRead = false
                }
                senderHandler.postDelayed({ sendStatusReads(per, q) }, 500L)
            }
        }
    })

    fun start() {
        // Surface pumpX2's own Timber diagnostics into logcat (auth gate, CentralChallenge,
        // connection-sharing detection, errors) — otherwise they are silently dropped.
        if (!loggingPlanted) { loggingPlanted = true; try { timber.log.Timber.plant(timber.log.Timber.DebugTree()) } catch (_: Throwable) {} }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
        stopped = false; keepConnected = true
        // Create the Pump + handler exactly once for the process (the handler is a pumpX2 singleton).
        if (pump == null) {
            // t:slim X2 firmware v7.7+ (current) uses the 6-digit JPAKE pairing code. Without this,
            // pumpX2 doesn't know the API version yet and defaults to LONG_16CHAR -> it sends the
            // legacy CentralChallengeRequest, which a v7.7+ pump silently rejects before the code box
            // can appear. SHORT_6CHAR makes the fallback report a v7.7+ API version, so pumpX2 takes
            // the JPAKE path. withUnbond… auto-clears a stale bond after repeated init failures.
            val p = Pump(
                TandemConfig()
                    .withPairingCodeType(PairingCodeType.SHORT_6CHAR)
                    .withUnbondAfterInitialConnectionHardFailuresCount(2)
            ); pump = p
            btHandler = TandemBluetoothHandler.getInstance(appContext, p, null)
        }
        if (connected) { status("Connected — syncing into xDrip…"); emitState(State.CONNECTED); refresh(); return }
        status("Scanning for a Tandem pump…"); emitState(State.SCANNING)
        // First-time pairing (no saved pairing code yet): proactively clear any stale Android bond
        // BEFORE connecting, so bondState != BONDED on connect -> createBond() fires -> the OS shows a
        // fresh pairing request -> pump answers the JPAKE -> the in-app code box appears.
        // (ControlX2's ensurePumpUnbondedForFreshInit pattern.) Once paired we keep the bond so
        // reconnects are seamless.
        if (PumpState.getPairingCode(appContext).isNullOrBlank() && removeStaleBonds()) {
            status("Cleared a previous pairing — rescanning for a fresh pairing request…")
            senderHandler.postDelayed({ scanLoop() }, 1800L)
            return
        }
        senderHandler.post { scanLoop() }
    }

    /** Remove any leftover Android bond for a Tandem pump (returns true if one was removed). */
    private fun removeStaleBonds(): Boolean {
        var removed = false
        try {
            val adapter = (appContext.getSystemService(android.content.Context.BLUETOOTH_SERVICE)
                as? android.bluetooth.BluetoothManager)?.adapter ?: return false
            for (dev in adapter.bondedDevices ?: emptySet()) {
                val n = dev.name ?: ""
                if (n.contains("tslim", true) || n.contains("tandem", true) || n.contains("mobi", true)) {
                    try { dev.javaClass.getMethod("removeBond").invoke(dev); removed = true; log("Removed stale bond: $n") }
                    catch (t: Throwable) { log("removeBond failed for $n: ${t.message}") }
                }
            }
        } catch (t: Throwable) { log("bond scan failed: ${t.message}") }
        return removed
    }

    private fun scanLoop() {
        var tries = 0
        while (!stopped && tries < 20) {
            try { btHandler?.startScan(); log("Scanning…"); return }
            catch (e: SecurityException) { tries++; log("Waiting for BT permission ($tries)"); Thread.sleep(500) }
        }
    }

    fun submitPairingCode(code: String) {
        // Prefer the peripheral/Pump that actually received the BLE callbacks (the one bound to
        // pumpX2's singleton handler), falling back to this controller's own references.
        val per = activePeripheral ?: peripheral
        val p: TandemPump? = activePump ?: pump
        val challenge = activeChallenge ?: centralChallenge
        val clean = code.trim().replace("-", "").replace(" ", "")
        log("submitPairingCode: len=${clean.length} per=${per?.address} pump=${p != null}")
        if (per == null || p == null) { error("No pump connected yet."); return }
        status("Pairing…")
        senderHandler.post {
            try { PumpState.setPairingCode(appContext, clean); p.pair(per, challenge, clean) }
            catch (t: Throwable) { error("Pairing failed: ${t.message}") }
        }
    }

    /** Re-pull new history while connected (called periodically / on demand by the service). */
    fun refresh() {
        val per = peripheral ?: return
        if (!connected) return
        pager.reset()
        senderHandler.post { safeSend(per, HistoryLogStatusRequest()) }
    }

    // Backstop poll loop: re-pull on a fixed cadence while connected (re-posts itself).
    private val pollRunnable = object : Runnable {
        override fun run() {
            if (stopped || !connected) return
            doLiveRefresh("poll")
            senderHandler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }
    private val eventRefreshRunnable = Runnable { doLiveRefresh("event") }

    private fun startPolling() {
        senderHandler.removeCallbacks(pollRunnable)
        senderHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }
    private fun stopPolling() {
        senderHandler.removeCallbacks(pollRunnable)
        senderHandler.removeCallbacks(eventRefreshRunnable)
    }

    /** The pump pushed a change — re-pull promptly (debounced so a burst becomes a single pull). */
    private fun onPumpEvent() {
        senderHandler.removeCallbacks(eventRefreshRunnable)
        senderHandler.postDelayed(eventRefreshRunnable, EVENT_DEBOUNCE_MS)
    }

    private fun doLiveRefresh(reason: String) {
        if (stopped || !connected) return
        if (state == State.SYNCING) return // initial history sync still running — let it finish
        val now = System.currentTimeMillis()
        if (now - lastLiveRefreshAt < MIN_REFRESH_GAP_MS) return // rate-limit, whatever the trigger
        lastLiveRefreshAt = now
        log("live refresh ($reason)")
        refresh()
    }

    /** Reset the history cursor + de-dup and re-pull the whole recent window from scratch. */
    fun resync() {
        pager.resetCursor()
        needProfileRead = true // re-read the basal profile on an explicit resync
        val per = peripheral
        if (connected && per != null) { status("Resyncing all recent data…"); senderHandler.post { safeSend(per, HistoryLogStatusRequest()) } }
    }

    fun isConnected() = connected
    fun metadata() = meta

    fun stop() {
        // Singleton: stop syncing without tearing down the (reusable) handler/central or the sender
        // thread — pumpX2's handler is a singleton and central.close() can't be cleanly re-opened.
        stopped = true; keepConnected = false
        pager.stop()
        stopPolling()
        try { btHandler?.central?.stopScan() } catch (_: Throwable) {}
        try { (activePeripheral ?: peripheral)?.cancelConnection() } catch (_: Throwable) {}
        connected = false; meta.connected = false
        emitState(State.DISABLED)
    }

    private inner class Pump(config: TandemConfig) : TandemPump(appContext, config) {
        override fun onPumpDiscovered(peripheral: BluetoothPeripheral?, scanResult: android.bluetooth.le.ScanResult?, readyState: PumpReadyState?): Boolean {
            if (stopped) return false // disabled mid-scan: don't auto-connect
            log("Discovered ${peripheral?.name} (${peripheral?.address})")
            return super.onPumpDiscovered(peripheral, scanResult, readyState)
        }
        override fun onInitialPumpConnection(peripheral: BluetoothPeripheral?) {
            this@TandemPumpController.peripheral = peripheral
            activePeripheral = peripheral; activePump = this
            log("onInitialPumpConnection: peripheral=${peripheral?.address} bond=${peripheral?.bondState}")
            // pumpX2 waits until Android reports BONDED, but it never *initiates* bonding itself —
            // it relies on the pump forcing link encryption. The t:slim's authorization
            // characteristic doesn't always trigger that auto-bond in time, so the pump terminates
            // the connection and no prompt ever appears. Kick off bonding here (the same thing
            // xDrip's InPen service does) so the system pairing request actually shows.
            try {
                if (peripheral != null && peripheral.bondState != BondState.BONDED) {
                    status("Connected — starting Bluetooth pairing. Accept the request on your phone (check the notification shade)…")
                    val started = peripheral.createBond()
                    log("createBond() -> $started (bondState=${peripheral.bondState})")
                } else {
                    status("Connected — already paired; syncing…")
                }
            } catch (t: Throwable) {
                log("createBond failed: ${t.message}")
            }
            super.onInitialPumpConnection(peripheral)
        }
        override fun onPairingPromptNotAcceptedYet(peripheral: BluetoothPeripheral?, retryAttempt: Int) {
            // Intentionally NOT calling super: the default raises a PAIRING_PROMPT_NOT_ACCEPTED_YET
            // critical error. pumpX2 keeps retrying on its own; we just guide the user instead.
            status("Waiting for you to accept the Bluetooth pairing request — check your phone's notification shade. Make sure the pump still shows “Pair Device”. If nothing appears, tap “Forget & re-pair”.")
        }
        override fun onWaitingForPairingCode(peripheral: BluetoothPeripheral?, centralChallengeResponse: AbstractCentralChallengeResponse?) {
            log("onWaitingForPairingCode: peripheral=${peripheral?.address} challenge=${centralChallengeResponse != null}")
            this@TandemPumpController.peripheral = peripheral
            this@TandemPumpController.centralChallenge = centralChallengeResponse
            activePeripheral = peripheral; activePump = this; activeChallenge = centralChallengeResponse
            val saved = PumpState.getPairingCode(appContext)
            if (!saved.isNullOrBlank()) { status("Re-using saved pairing code…"); senderHandler.post { pair(peripheral, centralChallengeResponse, saved) } }
            else { log("Prompting for pairing code"); emitState(State.NEEDS_CODE); main.post { listener?.onNeedPairingCode(peripheral?.name) } }
        }
        override fun onInvalidPairingCode(peripheral: BluetoothPeripheral?, resp: AbstractPumpChallengeResponse?) {
            error("Pump rejected the pairing code — re-check it on the pump and retry.")
            emitState(State.NEEDS_CODE)
            main.post { listener?.onNeedPairingCode(peripheral?.name) }
        }
        override fun onPumpModel(peripheral: BluetoothPeripheral?, model: KnownDeviceModel?) {
            super.onPumpModel(peripheral, model)
            meta.model = when (model) { KnownDeviceModel.TSLIM_X2 -> "t:slim X2"; KnownDeviceModel.MOBI -> "Tandem Mobi"; else -> model?.name ?: "Tandem Pump" }
            emitMeta()
        }
        override fun onPumpConnected(peripheral: BluetoothPeripheral?) {
            this@TandemPumpController.peripheral = peripheral
            activePeripheral = peripheral; activePump = this
            connected = true; meta.connected = true
            super.onPumpConnected(peripheral)
            main.post { listener?.onConnected(meta.model) }
            status("Connected — syncing into xDrip…"); emitState(State.SYNCING)
            needProfileRead = true // read the basal profile once per fresh connection
            beginSync(peripheral)
            startPolling() // keep pulling live updates after the initial sync
        }
        override fun onReceiveMessage(peripheral: BluetoothPeripheral?, message: Message?) {
            if (message == null) return
            when (message) {
                is HistoryLogStatusResponse -> pager.onStatusResponse(message)
                is HistoryLogStreamResponse -> pager.onStreamResponse(message)
                is CurrentBatteryAbstractResponse -> { meta.batteryPercent = message.batteryPercent; emitMeta() }
                is InsulinStatusResponse -> { meta.cartridgeUnits = message.currentInsulinAmount; emitMeta() }
                is ControlIQIOBResponse -> { meta.iobUnits = InsulinUnit.from1000To1(message.mudaliarIOB); emitMeta() }
                is CurrentBasalStatusResponse -> {
                    val rate = InsulinUnit.from1000To1(message.currentBasalRate)
                    val profileRate = InsulinUnit.from1000To1(message.profileBasalRate)
                    meta.currentBasal = rate; emitMeta()
                    // Anchor the live rate at "now". xDrip's basal line plots basal_percent (TBR%), so
                    // derive it from the pump's current-vs-profile rate — otherwise it defaults to -1
                    // and draws at y=0 (off-screen). (createEfficientRecord only ever appends newer
                    // records, so a steady basal stays one point until the rate changes; an actively-
                    // changing Control-IQ basal fills the line from the history rate-change events.)
                    if (TandemSync.isOn(TandemSync.BASAL)) {
                        val pct = TandemMapping.basalPercent(rate, profileRate)
                        try { APStatus.createEfficientRecord(System.currentTimeMillis(), pct, rate) } catch (_: Throwable) {}
                    }
                }
                is TimeSinceResetResponse -> {
                    val pumpNowMs = Dates.fromJan12008ToUnixEpochSeconds(message.currentTime) * 1000L
                    pumpClockOffsetMs = System.currentTimeMillis() - pumpNowMs
                    log("Pump clock=${message.currentTimeInstant} -> offset ${pumpClockOffsetMs / 86400000L}d (anchoring history to phone time)")
                }
                // ---- live values for the Pump tab (+ optional glucose -> BG) ----
                is CurrentEGVGuiDataResponse -> {
                    meta.glucoseMgdl = message.cgmReading; meta.trendRate = message.trendRate / 10.0; emitMeta()
                    if (TandemSync.isOn(TandemSync.GLUCOSE) && message.cgmReading in 10..600) {
                        val ts = Dates.fromJan12008ToUnixEpochSeconds(message.bgReadingTimestampSeconds) * 1000L + pumpClockOffsetMs
                        try { BgReading.bgReadingInsertFromInt(message.cgmReading, ts, 12 * 60000L, false, "Tandem pump") } catch (t: Throwable) { log("BG insert failed: ${t.message}") }
                    }
                }
                is ControlIQInfoV1Response -> {
                    meta.closedLoop = message.closedLoopEnabled
                    meta.tddUnits = InsulinUnit.from1000To1(message.totalDailyInsulin.toLong())
                    meta.controlIqMode = "mode ${message.currentUserModeTypeId}"
                    emitMeta()
                }
                is CurrentActiveIdpValuesResponse -> {
                    meta.carbRatio = message.currentCarbRatio / 1000.0
                    meta.isf = message.currentIsf
                    // Mask the leaked insulin-duration high byte (see TandemMapping.targetBgMgdl).
                    meta.targetBg = TandemMapping.targetBgMgdl(message.currentTargetBg)
                    meta.insulinDurationMin = message.currentInsulinDuration
                    emitMeta()
                }
                is ProfileStatusResponse -> {
                    idpActiveId = message.idpSlot0Id
                    if (TandemSync.isOn(TandemSync.PROFILE) && idpActiveId >= 0) {
                        idpRates.clear(); idpSegmentsExpected = 0
                        senderHandler.postDelayed({ activePeripheral?.let { safeSend(it, IDPSettingsRequest(idpActiveId)) } }, 300L)
                    }
                }
                is IDPSettingsResponse -> {
                    meta.basalProfileName = message.name; emitMeta()
                    idpSegmentsExpected = message.numberOfProfileSegments
                    requestIdpSegments()
                }
                is IDPSegmentResponse -> {
                    idpRates[message.profileStartTime] = InsulinUnit.from1000To1(message.profileBasalRate.toLong())
                    if (idpRates.size >= idpSegmentsExpected) saveBasalProfile()
                }
                else -> log("RESP ${message.javaClass.simpleName}")
            }
        }
        override fun onReceiveQualifyingEvent(peripheral: BluetoothPeripheral?, events: MutableSet<QualifyingEvent>?) {
            // The pump pushes these whenever something changes (new CGM reading, bolus, basal change,
            // etc.) — react immediately for near-realtime updates instead of waiting for the next poll.
            log("EVENT $events")
            onPumpEvent()
        }
        override fun onPumpDisconnected(peripheral: BluetoothPeripheral?, status: HciStatus?): Boolean {
            connected = false; meta.connected = false; emitMeta()
            stopPolling()
            log("Disconnected: $status (reconnect=${keepConnected})")
            emitState(if (keepConnected) State.SCANNING else State.DISABLED)
            return keepConnected // auto-reconnect unless we were told to stop
        }
        override fun onPumpCriticalError(peripheral: BluetoothPeripheral?, reason: TandemError?) { super.onPumpCriticalError(peripheral, reason); error("Pump error: ${reason?.name}") }
    }

    private fun beginSync(per: BluetoothPeripheral?) {
        per ?: return
        // History FIRST (boluses/carbs/basal — the data that matters for the graph), and keep BLE
        // traffic light: the pump terminates the link if hit with many simultaneous requests. The
        // small status-read set runs afterwards (see the pager's onComplete -> sendStatusReads).
        pager.reset()
        senderHandler.postDelayed({ status("Requesting history log…"); safeSend(per, HistoryLogStatusRequest()) }, 700L)
    }

    /** Send the minimal status reads one-at-a-time (paced) so we never flood the pump. */
    private fun sendStatusReads(per: BluetoothPeripheral, queue: ArrayDeque<Message>) {
        if (stopped || !connected) return
        val msg = queue.removeFirstOrNull() ?: return
        safeSend(per, msg)
        senderHandler.postDelayed({ sendStatusReads(per, queue) }, 600L)
    }

    private fun safeSend(per: BluetoothPeripheral, msg: Message) {
        if (stopped) return
        try { pump?.sendCommand(per, msg) } catch (t: Throwable) { log("send failed (${msg.javaClass.simpleName}): ${t.message}") }
    }

    /** Request each segment of the active IDP profile, paced one-at-a-time so we never flood. */
    private fun requestIdpSegments() {
        val per = activePeripheral ?: return
        if (idpSegmentsExpected <= 0) return
        var delay = 250L
        for (i in 0 until idpSegmentsExpected) {
            senderHandler.postDelayed({ if (!stopped) safeSend(per, IDPSegmentRequest(idpActiveId, i)) }, delay)
            delay += 400L
        }
    }

    /** Expand the collected IDP segments into xDrip's 24 hourly basal blocks and save the profile. */
    private fun saveBasalProfile() {
        try {
            val rates = TandemMapping.expandProfileToHourlyBlocks(idpRates)
            if (rates.isEmpty()) return
            val ref = BasalProfile.getActiveRateName()
            BasalProfile.save(ref, rates)
            // Name the xDrip slot after the pump's profile (e.g. "Gym only"); user can rename in the editor.
            meta.basalProfileName?.let { if (it.isNotBlank()) BasalProfile.setName(ref, it) }
            log("Saved basal profile '${meta.basalProfileName}' (${idpRates.size} segments) -> xDrip profile $ref")
        } catch (t: Throwable) { log("basal profile save failed: ${t.message}") }
    }

    fun currentState(): State = state
    private fun emitState(s: State) { state = s; main.post { listener?.onState(s) } }
    private fun emitMeta() { main.post { listener?.onMetadata(meta) } }
    private fun status(s: String) { Log.i(TAG, s); main.post { listener?.onStatus(s) } }
    private fun log(s: String) { Log.i(TAG, s); main.post { listener?.onLog(s) } }
    private fun error(s: String) { Log.e(TAG, s); main.post { listener?.onError(s) } }
}
