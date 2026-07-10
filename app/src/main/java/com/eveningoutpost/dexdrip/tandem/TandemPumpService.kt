package com.eveningoutpost.dexdrip.tandem

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.eveningoutpost.dexdrip.models.UserError

/**
 * Foreground service that owns the read-only Tandem pump connection (pumpX2),
 * mirroring how xDrip runs its other BLE devices (InPen/Pendiq): START_STICKY,
 * auto-reconnect, restarted on app launch by [TandemEntry.startIfEnabled].
 *
 * The Activity is only a thin pairing/status UI — all work happens here so the
 * pump keeps syncing in the background and across restarts.
 */
class TandemPumpService : Service(), TandemPumpController.Listener {

    private var controller: TandemPumpController? = null

    companion object {
        private const val TAG = "TandemPumpService"
        private const val NOTIF_ID = 7713
        // Our own channel — xDrip's ONGOING_CHANNEL is registered under a hashed id, so posting to
        // the raw id silently fails ("No Channel found"), leaving the foreground notification missing.
        private const val CHANNEL_ID = "tandem_pump_ongoing"

        @Volatile var lastMeta: TandemPumpController.PumpMetadata? = null
        @Volatile var lastStatus: String = "Idle"
        @Volatile var lastState: TandemPumpController.State = TandemPumpController.State.DISABLED
        @Volatile var uiListener: TandemPumpController.Listener? = null
        @Volatile private var INSTANCE: TandemPumpService? = null

        /** Called by the pairing screen when the user enters the pump's code. */
        fun submitPairingCode(code: String) { INSTANCE?.controller?.submitPairingCode(code) }
        /** Re-pull all recent history from scratch (cursor reset). */
        fun resync() { INSTANCE?.controller?.resync() }
        fun isRunning(): Boolean = INSTANCE != null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() { super.onCreate(); INSTANCE = this; ensureChannel() }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Tandem pump (read-only)", NotificationManager.IMPORTANCE_LOW)
                        .apply { description = "Ongoing Tandem pump sync status" }
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!TandemEntry.isEnabled()) {
            UserError.Log.d(TAG, "Not enabled — stopping")
            stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIF_ID, buildNotification(lastStatus))

        when (intent?.getStringExtra("function")) {
            "stop" -> {
                controller?.stop(); controller = null
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
                return START_NOT_STICKY
            }
            else -> { // "refresh" / null
                // Always the process-wide singleton controller (rebinds our listener). start() is
                // idempotent: it refreshes if already connected, otherwise scans/pairs.
                UserError.Log.d(TAG, "Starting/refreshing pump controller")
                controller = TandemPumpController.get(applicationContext, this).also { it.start() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // Do NOT stop the (process-singleton) controller on a transient service teardown — only the
        // explicit "stop" command (Disable) stops syncing. Just unbind this service instance.
        controller = null
        INSTANCE = null
        super.onDestroy()
    }

    private fun buildNotification(text: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0, Intent(this, TandemDownloadActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("xDrip Tandem")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(tap)
            .build()
    }

    private fun updateNotification(text: String) {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
            nm.notify(NOTIF_ID, buildNotification(text))
        } catch (t: Throwable) { UserError.Log.e(TAG, "notify failed: ${t.message}") }
    }

    // ---- TandemPumpController.Listener (already on main thread) ----
    override fun onStatus(text: String) { lastStatus = text; updateNotification(text); uiListener?.onStatus(text) }
    override fun onLog(line: String) { uiListener?.onLog(line) }
    override fun onNeedPairingCode(deviceName: String?) { lastStatus = "Enter pairing code"; updateNotification(lastStatus); uiListener?.onNeedPairingCode(deviceName) }
    override fun onConnected(modelName: String?) { uiListener?.onConnected(modelName) }
    override fun onMetadata(meta: TandemPumpController.PumpMetadata) { lastMeta = meta; uiListener?.onMetadata(meta) }
    override fun onDone(meta: TandemPumpController.PumpMetadata) {
        lastMeta = meta
        updateNotification("Synced: ${meta.boluses}B ${meta.carbs}C ${meta.basal} basal")
        uiListener?.onDone(meta)
    }
    override fun onError(text: String) { uiListener?.onError(text) }
    override fun onState(state: TandemPumpController.State) { lastState = state; uiListener?.onState(state) }
}
