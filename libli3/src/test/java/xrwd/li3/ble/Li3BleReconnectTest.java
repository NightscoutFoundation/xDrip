package xrwd.li3.ble;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for how {@link Li3Ble} retries a sensor it cannot reach: a few quick active attempts, then a low-power wait
 * in the background with an active scan between waits, so a phone left out of range does not drain its battery.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
@RunWith(RobolectricTestRunner.class)
public class Li3BleReconnectTest {

    // ================================================== attemptMode ==================================================

    /** The first six attempts take turns connecting by address and scanning, as before. */
    @Test
    public void attemptMode_firstSix_alternateAddressAndScan() {
        // :: Act
        Li3Ble.AttemptMode first = Li3Ble.attemptMode(0, true);
        Li3Ble.AttemptMode sixth = Li3Ble.attemptMode(5, true);

        // :: Verify
        assertThat(first).isEqualTo(Li3Ble.AttemptMode.BY_ADDRESS);
        assertThat(sixth).isEqualTo(Li3Ble.AttemptMode.BY_SCAN);
    }

    /** After six failed attempts the collector waits in the background, with a scan between the waits. */
    @Test
    public void attemptMode_afterSixFailures_alternatesBackgroundAndScan() {
        // :: Act
        Li3Ble.AttemptMode seventh = Li3Ble.attemptMode(6, true);
        Li3Ble.AttemptMode eighth = Li3Ble.attemptMode(7, true);
        Li3Ble.AttemptMode ninth = Li3Ble.attemptMode(8, true);

        // :: Verify
        assertThat(seventh).isEqualTo(Li3Ble.AttemptMode.IN_BACKGROUND);
        assertThat(eighth).isEqualTo(Li3Ble.AttemptMode.BY_SCAN);
        assertThat(ninth).isEqualTo(Li3Ble.AttemptMode.IN_BACKGROUND);
    }

    /** Without a valid address there is nothing to wait for in the background, so every attempt scans. */
    @Test
    public void attemptMode_noAddress_alwaysScans() {
        // :: Act
        Li3Ble.AttemptMode first = Li3Ble.attemptMode(0, false);
        Li3Ble.AttemptMode seventh = Li3Ble.attemptMode(6, false);

        // :: Verify
        assertThat(first).isEqualTo(Li3Ble.AttemptMode.BY_SCAN);
        assertThat(seventh).isEqualTo(Li3Ble.AttemptMode.BY_SCAN);
    }

    // =============================================== reconnectDelayMs ================================================

    /** While the attempts are active, each waits five seconds longer than the one before. */
    @Test
    public void reconnectDelayMs_activeAttempt_growsByFiveSeconds() {
        // :: Act
        long delay = Li3Ble.reconnectDelayMs(3, true);

        // :: Verify
        assertThat(delay).isEqualTo(15_000);
    }

    /** A minute passes before a background wait, so a failing scan does not fill the log. */
    @Test
    public void reconnectDelayMs_beforeBackgroundWait_oneMinute() {
        // :: Act
        long delay = Li3Ble.reconnectDelayMs(6, true);

        // :: Verify
        assertThat(delay).isEqualTo(60_000);
    }

    /** The scan after a background wait follows at once: the wait itself was the pause. */
    @Test
    public void reconnectDelayMs_afterBackgroundWait_fiveSeconds() {
        // :: Act
        long delay = Li3Ble.reconnectDelayMs(7, true);

        // :: Verify
        assertThat(delay).isEqualTo(5_000);
    }

    /** Without a valid address the delay keeps growing up to five minutes, as before. */
    @Test
    public void reconnectDelayMs_noAddress_growsToFiveMinutes() {
        // :: Act
        long delay = Li3Ble.reconnectDelayMs(100, false);

        // :: Verify
        assertThat(delay).isEqualTo(300_000);
    }
}
