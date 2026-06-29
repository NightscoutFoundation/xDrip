package com.eveningoutpost.dexdrip.tandem

import com.jwoglom.pumpx2.pump.messages.Message
import com.jwoglom.pumpx2.pump.messages.request.currentStatus.*

/**
 * READ-ONLY status requests for the pump-status display (battery / cartridge / IOB / basal).
 * Deliberately MINIMAL and sent one-at-a-time AFTER the history sync: the t:slim terminates the
 * BLE link if flooded with many simultaneous requests, so we only ask for what the screen shows.
 * The bulk time-series (boluses/carbs/basal) comes from the history log, not these.
 * All confirmed no-arg in pumpX2 v1.9.0; never insulin-delivery commands.
 */
object ReadRequests {
    fun all(): List<Message> = listOf(
        CurrentBatteryV2Request(),       // battery %
        InsulinStatusRequest(),          // cartridge units remaining
        ControlIQIOBRequest(),           // insulin on board
        CurrentBasalStatusRequest(),     // current basal rate
        CurrentEGVGuiDataRequest(),      // latest CGM glucose + trend (Pump tab; -> BG if enabled)
        ControlIQInfoV1Request(),        // closed-loop on/off, user mode, total daily insulin
        CurrentActiveIdpValuesRequest()  // active carb ratio / ISF / target / insulin duration
    )
}
