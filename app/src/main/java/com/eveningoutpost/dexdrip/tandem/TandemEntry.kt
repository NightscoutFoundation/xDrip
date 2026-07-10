package com.eveningoutpost.dexdrip.tandem

import android.bluetooth.BluetoothManager
import android.content.Context
import com.eveningoutpost.dexdrip.models.JoH
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore
import com.eveningoutpost.dexdrip.utilitymodels.Pref
import com.eveningoutpost.dexdrip.xdrip
import com.jwoglom.pumpx2.pump.PumpState

/**
 * Lightweight entry point for the Tandem pump integration, mirroring xDrip's
 * InPenEntry: an enable flag + helpers that (re)start the foreground service.
 * [startIfEnabled] is called from Home on app launch so it survives restarts.
 */
object TandemEntry {

    const val PREF_ENABLED = "tandem_enabled"

    @JvmStatic fun isEnabled(): Boolean = Pref.getBooleanDefaultFalse(PREF_ENABLED)

    @JvmStatic fun setEnabled(enabled: Boolean) {
        Pref.setBoolean(PREF_ENABLED, enabled)
        if (enabled) startWithRefresh() else stop()
    }

    private fun startWith(function: String) {
        Inevitable.task("tandem-changed-$function", 1000L) {
            JoH.startService(TandemPumpService::class.java, "function", function)
        }
    }

    @JvmStatic fun startWithRefresh() = startWith("refresh")

    /** Re-pull all recent history from scratch (resets the sequence cursor). */
    @JvmStatic fun resyncAll() {
        try { PersistentStore.setLong("tandem_last_seq", 0) } catch (_: Throwable) {}
        when {
            TandemPumpService.isRunning() -> TandemPumpService.resync()
            isEnabled() -> startWithRefresh()
            else -> setEnabled(true)
        }
    }

    @JvmStatic fun stop() {
        JoH.startService(TandemPumpService::class.java, "function", "stop")
    }

    /** Called on app launch (Home) — restarts the service if the user enabled it. */
    @JvmStatic fun startIfEnabled() {
        if (isEnabled() && JoH.ratelimit("tandem-start", 40)) startWithRefresh()
    }

    /**
     * Forget the pump and start a fresh pairing. A stale Android bond (e.g. from a previous
     * half-finished attempt, or after the pump's "Pair Device" was re-opened) makes the OS think
     * it is already paired, so it never shows a new pairing request and the pump terminates the
     * link. This clears pumpX2's saved pairing material, the history cursor, and removes the
     * Android bond, then re-enables so the next scan triggers a clean pairing prompt.
     */
    @JvmStatic fun forgetAndRepair() {
        val ctx = xdrip.getAppContext()
        stop()
        try { PumpState.resetState(ctx) } catch (_: Throwable) {}          // pairing code + JPAKE + saved MAC
        try { PersistentStore.setLong("tandem_last_seq", 0) } catch (_: Throwable) {} // re-pull history from start
        removeTandemBonds()
        Inevitable.task("tandem-repair", 2500L) { setEnabled(true) }
    }

    /** Remove any existing Android bond for a Tandem pump (legacy reflection, like ControlX2). */
    private fun removeTandemBonds() {
        try {
            val adapter = (xdrip.getAppContext().getSystemService(Context.BLUETOOTH_SERVICE)
                as? BluetoothManager)?.adapter ?: return
            for (dev in adapter.bondedDevices ?: emptySet()) {
                val name = dev.name ?: ""
                if (name.contains("tslim", true) || name.contains("tandem", true) || name.contains("mobi", true)) {
                    try { dev.javaClass.getMethod("removeBond").invoke(dev) } catch (_: Throwable) {}
                }
            }
        } catch (_: Throwable) {}
    }
}
