package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pins the bridge battery alerting behaviour that must survive the removal of the Parakeet
 * battery check from {@link CheckBridgeBattery}.
 *
 * @author Asbjørn Aarrestad - 2026.08
 */
public class CheckBridgeBatteryTest extends RobolectricTestWithConfig {

    private static final String ALERTS = "bridge_battery_alerts";
    private static final String LEVEL = "bridge_battery_alert_level";
    private static final String BATTERY = "bridge_battery";
    private static final String FORCE_WEAR = "disable_wearG5_on_lowbattery";

    @Before
    public void before() {
        cleanup();
    }

    @After
    public void after() {
        cleanup();
    }

    private void cleanup() {
        Pref.removeItem(ALERTS);
        Pref.removeItem(LEVEL);
        Pref.removeItem(BATTERY);
        Pref.removeItem(FORCE_WEAR);
    }

    // ===== checkBridgeBattery ===========================================================================================

    /** With bridge battery alerts switched off, no low battery is ever reported. */
    @Test
    public void checkBridgeBatteryReturnsFalseWhenAlertsDisabled() {
        // :: Setup
        Pref.setBoolean(ALERTS, false);
        Pref.setString(LEVEL, "30");
        Pref.setInt(BATTERY, 5);

        // :: Act
        final boolean lowBattery = CheckBridgeBattery.checkBridgeBattery();

        // :: Verify
        assertWithMessage("alerts disabled suppresses the low bridge battery result")
                .that(lowBattery).isFalse();
    }

    // ===== checkForceWearBridgeBattery ==================================================================================

    /** Forcing the watch off needs both the alert switch and the force-wear switch enabled. */
    @Test
    public void checkForceWearBridgeBatteryRequiresBothSwitches() {
        // :: Setup
        Pref.setString(LEVEL, "30");
        Pref.setInt(BATTERY, 10);

        // :: Act
        Pref.setBoolean(ALERTS, false);
        Pref.setBoolean(FORCE_WEAR, true);
        final boolean alertsOff = CheckBridgeBattery.checkForceWearBridgeBattery();

        Pref.setBoolean(ALERTS, true);
        Pref.setBoolean(FORCE_WEAR, false);
        final boolean forceWearOff = CheckBridgeBattery.checkForceWearBridgeBattery();

        // :: Verify
        assertWithMessage("alerts disabled means no forced wear switch off").that(alertsOff).isFalse();
        assertWithMessage("force wear disabled means no forced wear switch off").that(forceWearOff).isFalse();
    }

    /** Below the threshold, with 5% leeway subtracted, the watch is switched off. */
    @Test
    public void checkForceWearBridgeBatteryReportsLowBatteryBelowThreshold() {
        // :: Setup
        Pref.setBoolean(ALERTS, true);
        Pref.setBoolean(FORCE_WEAR, true);
        Pref.setString(LEVEL, "30");

        // :: Act
        Pref.setInt(BATTERY, 24);
        final boolean below = CheckBridgeBattery.checkForceWearBridgeBattery();

        Pref.setInt(BATTERY, 26);
        final boolean above = CheckBridgeBattery.checkForceWearBridgeBattery();

        // :: Verify
        assertWithMessage("24% is below the 25% effective threshold").that(below).isTrue();
        assertWithMessage("26% is above the 25% effective threshold").that(above).isFalse();
    }
}
