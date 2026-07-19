package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Verifies that {@link NightscoutFollowService#megaStatus} displays uploader battery
 * and charging status when available, and omits them when not set, and that rows which
 * would tell the user nothing are left off the page.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowServiceMegaStatusTest extends RobolectricTestWithConfig {

    @Before
    public void setUp() {
        super.setUp();
        NightscoutFollowService.clearUploaderStatus();
        JoH.buggy_samsung = false;
    }

    @After
    public void tearDown() {
        JoH.buggy_samsung = false; // static flag — must not leak into other tests
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
    public void megaStatus_appendsChargingSuffix_whenBatteryAndChargingBothSet() {
        // :: Setup — typical NS response: battery known and charging
        NightscoutFollowService.updateUploaderStatus(100, true);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify — single row showing battery with charging suffix
        assertThat(valueFor(items, "Uploader battery")).isEqualTo("100% (charging)");
    }

    @Test
    public void megaStatus_omitsChargingSuffix_whenBatterySetAndNotCharging() {
        // :: Setup
        NightscoutFollowService.updateUploaderStatus(84, false);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify — value shows plain percentage, no suffix
        assertThat(valueFor(items, "Uploader battery")).isEqualTo("84%");
    }

    // ===== Section omission ======================================================================

    @Test
    public void megaStatus_omitsUploaderSection_whenBothNull() {
        // :: Setup — clearUploaderStatus called in @Before

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify — neither row present
        assertThat(hasLabel(items, "Uploader battery")).isFalse();
    }

    // ===== Redundant absolute timestamps =========================================================
    // The page shows each fact once, as an age relative to now. The absolute wall-clock variants
    // said the same thing and made the reader do the subtraction, so they are not listed.

    @Test
    public void megaStatus_omitsAbsoluteLastBgTime_butKeepsRelativeLatestBg() {
        // :: Setup — a reading exists, so the absolute row would have been shown before
        insertReading(JoH.tsl() - Constants.MINUTE_IN_MS);

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Last BG time")).isFalse();
        assertThat(hasLabel(items, "Latest BG")).isTrue();
        assertThat(valueFor(items, "Latest BG")).endsWith(" ago");
    }

    @Test
    public void megaStatus_omitsAbsoluteNextPollTime_butKeepsRelativeNextPollIn() {
        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Next poll time")).isFalse();
        assertThat(hasLabel(items, "Next poll in")).isTrue();
    }

    // ===== Buggy handset ========================================================================
    // The follower runs the buggy-samsung wakeup workaround, so the row is relevant — but it is
    // only listed when the workaround is actually in use, as the collector status pages do.

    @Test
    public void megaStatus_omitsBuggyHandset_whenHandsetIsNotBuggy() {
        // :: Setup — buggy_samsung reset to false in @Before

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Buggy handset")).isFalse();
    }

    @Test
    public void megaStatus_showsBuggyHandset_whenHandsetIsBuggy() {
        // :: Setup
        JoH.buggy_samsung = true;

        // :: Act
        List<StatusItem> items = NightscoutFollowService.megaStatus();

        // :: Verify
        assertThat(hasLabel(items, "Buggy handset")).isTrue();
    }

    private void insertReading(final long timestamp) {
        final BgReading bg = new BgReading();
        bg.calculated_value = 120.0;
        bg.raw_data = 120.0;
        bg.timestamp = timestamp;
        bg.save();
    }
}
