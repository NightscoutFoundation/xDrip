package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

/**
 * Tests for {@link Li3Setup}: the Li3 screen lists the setup steps in order, ticks the ones done, and marks the first
 * one left as next. The account step is only needed for a takeover, so it is never marked next.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Li3SetupTest extends RobolectricTestWithConfig {

    // ===================================================== Steps =====================================================

    /** Nothing done: every step is open, in order, and location comes first. */
    @Test
    public void text_nothingDone_locationNext() {
        // :: Act
        String text = Li3Setup.text(false, false, false, false, false);

        // :: Verify
        assertThat(text).isEqualTo(gs(R.string.li3_setup) + "\n"
                + "○ 1. " + gs(R.string.li3_step_location) + " (" + gs(R.string.li3_setup_next) + ")\n"
                + "○ 2. " + gs(R.string.li3_step_credential) + "\n"
                + "○ 3. " + gs(R.string.li3_step_account) + "\n"
                + "○ 4. " + gs(R.string.li3_step_scan) + "\n"
                + "○ 5. " + gs(R.string.li3_step_readings));
    }

    /** With location and the credential in place, the scan is next and the account step is passed over. */
    @Test
    public void text_credentialDone_scanNext() {
        // :: Act
        String text = Li3Setup.text(true, true, false, false, false);

        // :: Verify
        assertThat(text).contains("✓ 1. " + gs(R.string.li3_step_location) + "\n");
        assertThat(text).contains("✓ 2. " + gs(R.string.li3_step_credential) + "\n");
        assertThat(text).contains("○ 3. " + gs(R.string.li3_step_account) + "\n");
        assertThat(text).contains("○ 4. " + gs(R.string.li3_step_scan) + " (" + gs(R.string.li3_setup_next) + ")");
    }

    /** An account ID that is set ticks the account step. */
    @Test
    public void text_accountIdSet_accountTicked() {
        // :: Act
        String text = Li3Setup.text(false, false, true, false, false);

        // :: Verify
        assertThat(text).contains("✓ 3. " + gs(R.string.li3_step_account));
    }

    /** A done step later in the list does not hide an earlier open one: that one is still next. */
    @Test
    public void text_sensorWithoutCredential_credentialNext() {
        // :: Act
        String text = Li3Setup.text(true, false, false, true, false);

        // :: Verify
        assertThat(text).contains("○ 2. " + gs(R.string.li3_step_credential) + " (" + gs(R.string.li3_setup_next) + ")");
        assertThat(text).contains("✓ 4. " + gs(R.string.li3_step_scan) + "\n");
    }

    /** A scanned sensor with no reading yet leaves waiting for it as the next step. */
    @Test
    public void text_sensorStored_readingsNext() {
        // :: Act
        String text = Li3Setup.text(true, true, false, true, false);

        // :: Verify
        assertThat(text).endsWith("○ 5. " + gs(R.string.li3_step_readings) + " (" + gs(R.string.li3_setup_next) + ")");
    }

    /** With readings coming, every needed step is ticked and none is next. */
    @Test
    public void text_allDone_nothingNext() {
        // :: Act
        String text = Li3Setup.text(true, true, false, true, true);

        // :: Verify
        assertThat(text).contains("✓ 5. " + gs(R.string.li3_step_readings));
        assertThat(text).doesNotContain(gs(R.string.li3_setup_next));
    }
}
