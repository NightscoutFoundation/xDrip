package com.eveningoutpost.dexdrip.insulin.aaps;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests that a device-status broadcast from AAPS updates the pump values xDrip shows.
 * <p>
 * The JSON is parsed with gson into a Kotlin class from the bundled Nightscout SDK, which has no no-arg
 * constructor. A gson upgrade must still fill it.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class AAPSStatusHandlerTest extends RobolectricTestWithConfig {

    // ===== Pump status ===============================================================================================

    /** Reservoir and battery from an AAPS device status end up in PumpStatus. */
    @Test
    public void processDeviceStatus_setsReservoirAndBattery() {
        // :: Setup
        final String json = "{\"device\":\"openaps://AAPS\",\"pump\":{\"clock\":\"2026-10-02T12:00:00Z\","
                + "\"reservoir\":123.4,\"battery\":{\"percent\":75},\"status\":{\"status\":\"normal\"}}}";

        // :: Act
        AAPSStatusHandler.processDeviceStatus(json);

        // :: Verify
        assertThat(PumpStatus.getBattery()).isEqualTo(75.0);
        assertThat(PumpStatus.getReservoirString()).contains("123.4U");
    }
}
