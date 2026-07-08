package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.After;
import org.junit.Test;

/**
 * Tests for {@link NsServerCapabilities}: default-supported state, sticky marking of an
 * unsupported endpoint, automatic reset when the server URL changes, and self-healing after the
 * recheck window. The recheck window is exercised through the {@code ...At(url, nowMs)} test
 * seams so it is deterministic without depending on wall-clock time (the class reads
 * {@link com.eveningoutpost.dexdrip.models.JoH#tsl()} in production).
 *
 * @author Asbjørn Aarrestad
 */
public class NsServerCapabilitiesTest extends RobolectricTestWithConfig {

    private static final String URL_A = "http://127.0.0.1:17580/";
    private static final String URL_B = "https://my.ns.example.com/";

    @After
    public void tearDown() {
        NsServerCapabilities.reset();
    }

    @Test
    public void deviceStatusSupportedByDefault() {
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_A)).isTrue();
    }

    @Test
    public void markDeviceStatusUnsupported_isStickyForSameUrl() {
        NsServerCapabilities.markDeviceStatusUnsupported(URL_A);
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_A)).isFalse();
    }

    @Test
    public void flagResetsWhenUrlChanges() {
        NsServerCapabilities.markDeviceStatusUnsupported(URL_A);

        // :: Act — switch server: flag clears
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_B)).isTrue();

        // :: Verify — switching back re-probes (state is not remembered per-URL)
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_A)).isTrue();
    }

    // ===== Self-healing ==========================================================================

    @Test
    public void allowsReprobe_afterRecheckWindowElapses() {
        // :: Setup — mark unsupported at a fixed base time
        final long base = 1_000_000_000L;
        NsServerCapabilities.markDeviceStatusUnsupportedAt(URL_A, base);

        // :: Verify — suppressed just before the window, re-probed once it elapses
        assertThat(NsServerCapabilities.supportsDeviceStatusAt(
                URL_A, base + NsServerCapabilities.DEVICESTATUS_RECHECK_MS - 1)).isFalse();
        assertThat(NsServerCapabilities.supportsDeviceStatusAt(
                URL_A, base + NsServerCapabilities.DEVICESTATUS_RECHECK_MS)).isTrue();
    }

    @Test
    public void markSupported_clearsFlagImmediately_forSelfHeal() {
        // :: Setup
        NsServerCapabilities.markDeviceStatusUnsupported(URL_A);
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_A)).isFalse();

        // :: Act — endpoint responded OK again
        NsServerCapabilities.markDeviceStatusSupported(URL_A);

        // :: Verify — back to normal immediately
        assertThat(NsServerCapabilities.supportsDeviceStatus(URL_A)).isTrue();
    }
}
