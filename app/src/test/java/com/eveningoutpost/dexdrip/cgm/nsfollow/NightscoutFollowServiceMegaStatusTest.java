package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Verifies that {@link NightscoutFollowService#megaStatus} displays uploader battery
 * and charging status when available, and omits them when not set.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowServiceMegaStatusTest extends RobolectricTestWithConfig {

    @Before
    public void setUp() {
        super.setUp();
        NightscoutFollowService.clearUploaderStatus();
    }

    private boolean hasLabel(final List<StatusItem> items, final String label) {
        return items.stream().anyMatch(i -> label.equals(i.name));
    }

    private String valueFor(final List<StatusItem> items, final String label) {
        return items.stream()
                .filter(i -> label.equals(i.name))
                .map(i -> i.value)
                .findFirst()
                .orElse(null);
    }

    // ===== Uploader battery ======================================================================

    @Test
    public void megaStatus_showsUploaderBattery_whenSet() {
        // :: Setup
        NightscoutFollowService.updateUploaderStatus(84, null);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Uploader battery")).isTrue();
        assertThat(valueFor(items, "Uploader battery")).isEqualTo("84%");
    }

    @Test
    public void megaStatus_omitsUploaderBattery_whenNull() {
        // :: Setup — charging known but battery unknown
        NightscoutFollowService.updateUploaderStatus(null, true);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Uploader battery")).isFalse();
    }

    // ===== Charging status ======================================================================

    @Test
    public void megaStatus_showsChargingYes_whenCharging() {
        // :: Setup
        NightscoutFollowService.updateUploaderStatus(null, true);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Uploader charging")).isTrue();
        assertThat(valueFor(items, "Uploader charging")).isEqualTo("Yes");
    }

    @Test
    public void megaStatus_showsChargingNo_whenNotCharging() {
        // :: Setup
        NightscoutFollowService.updateUploaderStatus(null, false);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Uploader charging")).isTrue();
        assertThat(valueFor(items, "Uploader charging")).isEqualTo("No");
    }

    @Test
    public void megaStatus_omitsChargingRow_whenChargingIsNull() {
        // :: Setup — battery present but charging unknown
        NightscoutFollowService.updateUploaderStatus(80, null);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Uploader charging")).isFalse();
    }

    // ===== Section omission ======================================================================

    @Test
    public void megaStatus_omitsUploaderSection_whenBothNull() {
        // :: Setup — clearUploaderStatus called in @Before

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify — neither row present
        assertThat(hasLabel(items, "Uploader battery")).isFalse();
        assertThat(hasLabel(items, "Uploader charging")).isFalse();
    }
}
