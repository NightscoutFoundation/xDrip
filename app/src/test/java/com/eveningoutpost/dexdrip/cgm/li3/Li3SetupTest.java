package com.eveningoutpost.dexdrip.cgm.li3;

import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowNfcAdapter;

/**
 * Tests for {@link Li3Setup}: the Li3 screen lists the setup steps in order, ticks the ones done, and marks the first
 * one left as next. The account step is only needed for a takeover, so it is never marked next. Setup is finished
 * with a credential and a sensor, and NFC scanning needs NFC on the phone, switched on, and on in xDrip.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class Li3SetupTest extends RobolectricTestWithConfig {

    @After
    public void tearDown() {
        Pref.removeItem("use_nfc_scan");
    }

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

    // ==================================================== Finished ===================================================

    /** A credential and a stored sensor finish the setup. */
    @Test
    public void finished_credentialAndSensor_true() {
        // :: Act
        boolean finished = Li3Setup.finished(true, true);

        // :: Verify
        assertThat(finished).isTrue();
    }

    /** Without the credential the sensor cannot pair, so setup is not finished. */
    @Test
    public void finished_noCredential_false() {
        // :: Act
        boolean finished = Li3Setup.finished(false, true);

        // :: Verify
        assertThat(finished).isFalse();
    }

    /** Without a scanned sensor there is nothing to connect to, so setup is not finished. */
    @Test
    public void finished_noSensor_false() {
        // :: Act
        boolean finished = Li3Setup.finished(true, false);

        // :: Verify
        assertThat(finished).isFalse();
    }

    // ====================================================== NFC ======================================================

    /** NFC on the phone, switched on, and scanning on in xDrip: nothing is missing. */
    @Test
    public void nfcGap_allOn_none() {
        // :: Setup
        nfcPhone(true, true);
        Pref.setBoolean("use_nfc_scan", true);

        // :: Act
        Li3Setup.NfcGap gap = Li3Setup.nfcGap(RuntimeEnvironment.getApplication());

        // :: Verify
        assertThat(gap).isEqualTo(Li3Setup.NfcGap.NONE);
    }

    /** NFC scanning is off in xDrip by default, and then no scan reaches the Li3 code. */
    @Test
    public void nfcGap_scanningOffInXdrip_scanOff() {
        // :: Setup
        nfcPhone(true, true);

        // :: Act
        Li3Setup.NfcGap gap = Li3Setup.nfcGap(RuntimeEnvironment.getApplication());

        // :: Verify
        assertThat(gap).isEqualTo(Li3Setup.NfcGap.SCAN_OFF);
    }

    /** NFC switched off on the phone is named after xDrip's own setting is on. */
    @Test
    public void nfcGap_phoneNfcOff_phoneOff() {
        // :: Setup
        nfcPhone(true, false);
        Pref.setBoolean("use_nfc_scan", true);

        // :: Act
        Li3Setup.NfcGap gap = Li3Setup.nfcGap(RuntimeEnvironment.getApplication());

        // :: Verify
        assertThat(gap).isEqualTo(Li3Setup.NfcGap.PHONE_OFF);
    }

    /** A phone without NFC cannot start a sensor, whatever the settings say. */
    @Test
    public void nfcGap_noNfc_missing() {
        // :: Setup
        nfcPhone(false, false);
        Pref.setBoolean("use_nfc_scan", true);

        // :: Act
        Li3Setup.NfcGap gap = Li3Setup.nfcGap(RuntimeEnvironment.getApplication());

        // :: Verify
        assertThat(gap).isEqualTo(Li3Setup.NfcGap.MISSING);
    }

    // ==================================================== helpers ====================================================

    private static void nfcPhone(boolean hardware, boolean enabled) {
        ShadowNfcAdapter.setNfcHardwareExists(hardware);
        shadowOf(RuntimeEnvironment.getApplication().getPackageManager())
                .setSystemFeature(PackageManager.FEATURE_NFC, hardware);
        if (hardware) {
            shadowOf(NfcAdapter.getDefaultAdapter(RuntimeEnvironment.getApplication())).setEnabled(enabled);
        }
    }
}
