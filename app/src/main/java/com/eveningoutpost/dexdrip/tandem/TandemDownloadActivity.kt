package com.eveningoutpost.dexdrip.tandem

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.eveningoutpost.dexdrip.R
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The single Tandem screen, reachable from Settings → Experimental → "Tandem pump".
 * Three tabs: SYNC (connect/pair/controls/log), PUMP (live read-only values not on xDrip's
 * native screens), SETTINGS (choose what to sync). It only drives the background
 * [TandemPumpService]; closing it does NOT stop syncing. `--ez demo true` renders sample data.
 */
class TandemDownloadActivity : Activity(), TandemPumpController.Listener {

    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private lateinit var logScroll: ScrollView
    private lateinit var pairingRow: LinearLayout
    private lateinit var pairingInput: EditText
    private lateinit var connectButton: Button
    private lateinit var disableButton: Button
    private lateinit var syncButton: Button
    private lateinit var forgetButton: Button

    private lateinit var tabSync: View
    private lateinit var tabPump: View
    private lateinit var tabSettings: View

    private lateinit var valModel: TextView
    private lateinit var valConn: TextView
    private lateinit var valBattery: TextView
    private lateinit var valCartridge: TextView
    private lateinit var valIob: TextView
    private lateinit var valBasal: TextView
    private lateinit var valGlucose: TextView
    private lateinit var valControlIq: TextView
    private lateinit var valTdd: TextView
    private lateinit var valProfile: TextView
    private lateinit var valCarbRatio: TextView
    private lateinit var valIsf: TextView
    private lateinit var valTarget: TextView
    private lateinit var valInsDur: TextView
    private lateinit var valLastSync: TextView
    private lateinit var valImported: TextView

    private val logBuf = StringBuilder()
    private val PERM_REQ = 7711
    private var demo = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tandem_download)
        title = "Tandem Pump"
        demo = intent?.getBooleanExtra("demo", false) == true

        statusText = findViewById(R.id.tandemStatusText)
        logText = findViewById(R.id.tandemLogText)
        logScroll = findViewById(R.id.tandemLogScroll)
        pairingRow = findViewById(R.id.tandemPairingRow)
        pairingInput = findViewById(R.id.tandemPairingInput)
        connectButton = findViewById(R.id.tandemConnectButton)
        disableButton = findViewById(R.id.tandemDisableButton)
        syncButton = findViewById(R.id.tandemSyncButton)
        forgetButton = findViewById(R.id.tandemForgetButton)
        tabSync = findViewById(R.id.tabSync)
        tabPump = findViewById(R.id.tabPump)
        tabSettings = findViewById(R.id.tabSettings)
        valModel = findViewById(R.id.valModel)
        valConn = findViewById(R.id.valConn)
        valBattery = findViewById(R.id.valBattery)
        valCartridge = findViewById(R.id.valCartridge)
        valIob = findViewById(R.id.valIob)
        valBasal = findViewById(R.id.valBasal)
        valGlucose = findViewById(R.id.valGlucose)
        valControlIq = findViewById(R.id.valControlIq)
        valTdd = findViewById(R.id.valTdd)
        valProfile = findViewById(R.id.valProfile)
        valCarbRatio = findViewById(R.id.valCarbRatio)
        valIsf = findViewById(R.id.valIsf)
        valTarget = findViewById(R.id.valTarget)
        valInsDur = findViewById(R.id.valInsDur)
        valLastSync = findViewById(R.id.valLastSync)
        valImported = findViewById(R.id.valImported)

        findViewById<Button>(R.id.tabBtnSync).setOnClickListener { showTab(0) }
        findViewById<Button>(R.id.tabBtnPump).setOnClickListener { showTab(1) }
        findViewById<Button>(R.id.tabBtnSettings).setOnClickListener { showTab(2) }

        bindSyncCheck(R.id.syncBoluses, TandemSync.BOLUSES)
        bindSyncCheck(R.id.syncCarbs, TandemSync.CARBS)
        bindSyncCheck(R.id.syncBasal, TandemSync.BASAL)
        bindSyncCheck(R.id.syncProfile, TandemSync.PROFILE)
        bindSyncCheck(R.id.syncGlucose, TandemSync.GLUCOSE)

        connectButton.setOnClickListener {
            if (TandemEntry.isEnabled()) { ensurePermsThen { TandemEntry.startWithRefresh() } }
            else if (!TandemSync.isConfigured()) {
                showTab(2); toast("Choose what to sync, then tap Enable & Sync again.")
            } else if (!TandemSync.anySelected()) {
                showTab(2); toast("Select at least one data type to sync.")
            } else ensurePermsThen { TandemEntry.setEnabled(true) }
        }
        disableButton.setOnClickListener {
            TandemEntry.setEnabled(false)
            statusText.text = "Disabled. Background sync stopped."
            applyButtons(TandemPumpController.State.DISABLED)
        }
        syncButton.setOnClickListener { ensurePermsThen { TandemEntry.startWithRefresh() } }
        findViewById<Button>(R.id.tandemResyncButton).setOnClickListener {
            ensurePermsThen {
                TandemEntry.resyncAll()
                statusText.text = "Resyncing all recent data from the pump…"
            }
        }
        forgetButton.setOnClickListener {
            ensurePermsThen {
                pairingRow.visibility = View.GONE
                TandemEntry.forgetAndRepair()
                statusText.text = "Forgetting the pump and re-pairing… put the pump in Bluetooth → Pair Device, then accept the pairing request."
            }
        }
        findViewById<Button>(R.id.tandemPairButton).setOnClickListener {
            val code = pairingInput.text.toString()
            if (code.isBlank()) { toast("Enter the code shown on the pump"); return@setOnClickListener }
            TandemPumpService.submitPairingCode(code)
            pairingRow.visibility = View.GONE
        }
        showTab(0)
    }

    private fun bindSyncCheck(id: Int, key: String) {
        val cb = findViewById<CheckBox>(id)
        cb.isChecked = TandemSync.isOn(key)
        cb.setOnCheckedChangeListener { _, checked -> TandemSync.set(key, checked) }
    }

    private fun showTab(which: Int) {
        tabSync.visibility = if (which == 0) View.VISIBLE else View.GONE
        tabPump.visibility = if (which == 1) View.VISIBLE else View.GONE
        tabSettings.visibility = if (which == 2) View.VISIBLE else View.GONE
        if (which == 2) TandemSync.setConfigured()
    }

    override fun onResume() {
        super.onResume()
        if (demo) { renderDemo(); return }
        TandemPumpService.uiListener = this
        statusText.text = TandemPumpService.lastStatus
        TandemPumpService.lastMeta?.let { renderMeta(it) }
        applyButtons(TandemPumpService.lastState)
        bindSyncCheck(R.id.syncBoluses, TandemSync.BOLUSES)
        bindSyncCheck(R.id.syncCarbs, TandemSync.CARBS)
        bindSyncCheck(R.id.syncBasal, TandemSync.BASAL)
        bindSyncCheck(R.id.syncProfile, TandemSync.PROFILE)
        bindSyncCheck(R.id.syncGlucose, TandemSync.GLUCOSE)
    }

    override fun onPause() {
        if (!demo) TandemPumpService.uiListener = null
        super.onPause()
    }

    /** Enable/disable buttons so only valid actions are available for the current state. */
    private fun applyButtons(s: TandemPumpController.State) {
        if (demo) return
        val enabled = TandemEntry.isEnabled()
        connectButton.text = if (enabled) "Re-connect" else "Enable & Sync"
        connectButton.isEnabled = !enabled || s == TandemPumpController.State.CONNECTED
        disableButton.isEnabled = enabled
        syncButton.isEnabled = enabled && s == TandemPumpController.State.CONNECTED
        forgetButton.isEnabled = enabled && s != TandemPumpController.State.SYNCING
        pairingRow.visibility = if (s == TandemPumpController.State.NEEDS_CODE) View.VISIBLE else View.GONE
        valConn.text = when (s) {
            TandemPumpController.State.DISABLED -> "Disabled"
            TandemPumpController.State.SCANNING -> "Scanning…"
            TandemPumpController.State.NEEDS_CODE -> "Awaiting pairing code"
            TandemPumpController.State.CONNECTED -> "Connected"
            TandemPumpController.State.SYNCING -> "Syncing…"
        }
    }

    // ---- TandemPumpController.Listener (forwarded by the service on the main thread) ----
    override fun onStatus(text: String) { if (!demo) statusText.text = text }
    override fun onLog(line: String) {
        logBuf.append(line).append('\n')
        if (logBuf.length > 40_000) logBuf.delete(0, logBuf.length - 32_000)
        logText.text = logBuf
        logScroll.post { logScroll.fullScroll(View.FOCUS_DOWN) }
    }
    override fun onNeedPairingCode(deviceName: String?) {
        pairingRow.visibility = View.VISIBLE
        statusText.text = "Enter the pairing code shown on ${deviceName ?: "the pump"} (pump: Bluetooth → Pair Device)."
    }
    override fun onConnected(modelName: String?) { statusText.text = "Connected to ${modelName ?: "pump"} — syncing into xDrip…" }
    override fun onMetadata(meta: TandemPumpController.PumpMetadata) { renderMeta(meta) }
    override fun onDone(meta: TandemPumpController.PumpMetadata) {
        renderMeta(meta)
        statusText.text = "Synced — open xDrip to see boluses, carbs and basal on the graph."
    }
    override fun onError(text: String) { onLog("ERROR: $text"); if (!demo) statusText.text = text }
    override fun onState(state: TandemPumpController.State) { if (!demo) runOnUiThread { applyButtons(state) } }

    private val useMgdl by lazy { Pref.getString("units", "mgdl") == "mgdl" }
    /** Format a mg/dL pump value in the user's configured xDrip units (mg/dL or mmol/L). */
    private fun bgStr(mgdl: Int): String {
        val v = BgGraphBuilder.unitized(mgdl.toDouble(), useMgdl)
        return if (useMgdl) "%.0f mg/dL".format(v) else "%.1f mmol/L".format(v)
    }

    private fun renderMeta(m: TandemPumpController.PumpMetadata) {
        valModel.text = m.model ?: "—"
        valBattery.text = m.batteryPercent?.let { "$it%" } ?: "—"
        valCartridge.text = m.cartridgeUnits?.let { "$it U" } ?: "—"
        valIob.text = m.iobUnits?.let { "%.2f U".format(it) } ?: "—"
        valBasal.text = m.currentBasal?.let { "%.2f U/hr".format(it) } ?: "—"
        valGlucose.text = m.glucoseMgdl?.let { bgStr(it) } ?: "—"
        valControlIq.text = m.closedLoop?.let { if (it) "On" else "Off" } ?: "—"
        valTdd.text = m.tddUnits?.let { "%.1f U".format(it) } ?: "—"
        valProfile.text = m.basalProfileName ?: "—"
        valCarbRatio.text = m.carbRatio?.let { "%.1f g/U".format(it) } ?: "—"
        valIsf.text = m.isf?.let { "${bgStr(it)}/U" } ?: "—"
        valTarget.text = m.targetBg?.let { bgStr(it) } ?: "—"
        valInsDur.text = m.insulinDurationMin?.let { "$it min" } ?: "—"
        valLastSync.text = if (m.lastSync > 0) SimpleDateFormat("d MMM HH:mm", Locale.getDefault()).format(Date(m.lastSync)) else "—"
        valImported.text = "${m.boluses} bolus · ${m.carbs} carb · ${m.basal} basal"
    }

    /** Fills every tab with representative sample data (documentation screenshots). */
    private fun renderDemo() {
        val m = TandemPumpController.PumpMetadata(
            model = "t:slim X2", connected = true, batteryPercent = 78, cartridgeUnits = 142,
            iobUnits = 1.85, currentBasal = 0.85, boluses = 38, carbs = 41, basal = 212,
            lastSync = System.currentTimeMillis(), glucoseMgdl = 124, trendRate = 0.3,
            closedLoop = true, controlIqMode = "mode 0", tddUnits = 41.2, carbRatio = 9.0,
            isf = 38, targetBg = 110, insulinDurationMin = 300, basalProfileName = "Default"
        )
        renderMeta(m)
        valConn.text = "Connected"
        statusText.text = "Connected to t:slim X2 — synced."
        connectButton.text = "Re-connect"
        logBuf.setLength(0)
        listOf(
            "Discovered tslim X2 (C8:3A:35:..)", "Authenticated (JPAKE) — syncing…",
            "Pump clock -> offset 0d", "HISTORY: seq 9001..10462 (291 new)",
            "BOLUS 5.20u", "CARBS 45g", "BASAL 0.85 U/hr",
            "Saved basal profile 'Default' (6 segments) -> xDrip profile 1",
            "Synced — 38 boluses, 41 carbs, 212 basal → xDrip."
        ).forEach { logBuf.append(it).append('\n') }
        logText.text = logBuf
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()

    private var pendingAction: (() -> Unit)? = null
    private fun requiredPerms(): Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun ensurePermsThen(action: () -> Unit) {
        val missing = requiredPerms().filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) action()
        else { pendingAction = action; requestPermissions(missing.toTypedArray(), PERM_REQ) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERM_REQ) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) pendingAction?.invoke()
            else toast("Bluetooth permissions are required.")
            pendingAction = null
        }
    }
}
