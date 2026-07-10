package com.eveningoutpost.dexdrip.tandem

import com.eveningoutpost.dexdrip.utilitymodels.Pref

/**
 * Per-data-type sync preferences for the Tandem integration. The user chooses what to pull from the
 * pump (Settings tab) before enabling; everything is read-only. Glucose defaults OFF because this
 * xDrip usually already has its own CGM session and pump glucose would clash with it.
 */
object TandemSync {
    const val GLUCOSE = "tandem_sync_glucose"      // pump CGM -> xDrip BG  (opt-in, can clash)
    const val BOLUSES = "tandem_sync_boluses"      // boluses -> Treatments
    const val CARBS = "tandem_sync_carbs"          // carbs -> Treatments
    const val BASAL = "tandem_sync_basal"          // delivered basal -> APStatus
    const val PROFILE = "tandem_sync_profile"      // basal profile (IDP) -> xDrip basal profile
    const val CONFIGURED = "tandem_sync_configured" // user has visited Settings at least once

    private fun def(key: String) = when (key) {
        GLUCOSE -> false
        else -> true
    }

    @JvmStatic fun isOn(key: String): Boolean = Pref.getBoolean(key, def(key))
    @JvmStatic fun set(key: String, on: Boolean) = Pref.setBoolean(key, on)

    @JvmStatic fun isConfigured(): Boolean = Pref.getBooleanDefaultFalse(CONFIGURED)
    @JvmStatic fun setConfigured() = Pref.setBoolean(CONFIGURED, true)

    /** True if at least one data type is selected (otherwise enabling would do nothing). */
    @JvmStatic fun anySelected(): Boolean =
        isOn(GLUCOSE) || isOn(BOLUSES) || isOn(CARBS) || isOn(BASAL) || isOn(PROFILE)
}
