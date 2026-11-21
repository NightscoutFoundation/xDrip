package xrwd.li3.nfc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import xrwd.li3.pairing.ReceiverId;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link NfcCommand}: the read-patch-info, activate and switch-receiver NFC commands and the CRC they carry.
 * Vectors are commands a sensor accepted, from RoundWhiteDiscKit 7dd6396 {@code NFCActivationCommandTests.swift:33-60}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class NfcCommandTest {

    // ===================================================== crc16 =====================================================

    /** NFCActivationCommandTests.swift:33-37: the CRC over time and receiver ID of an accepted fresh pair. */
    @Test
    public void crc16_acceptedFreshPairBody_matches() {
        // :: Act
        int crc = NfcCommand.crc16(Hex.toBytes("fc2bee6978830d6f"));

        // :: Verify
        assertThat(crc).isEqualTo(0x71b4);
    }

    // ==================================================== command ====================================================

    /** NFCActivationCommandTests.swift:39-44: the activate command a fresh sensor accepted. */
    @Test
    public void command_activate_matchesCapturedWire() {
        // :: Act
        byte[] command = NfcCommand.command(NfcCommand.Code.ACTIVATE, 1777216508L, new ReceiverId(0x6f0d8378L));

        // :: Verify
        assertThat(Hex.of(command)).isEqualTo("02a07afc2bee6978830d6fb471");
    }

    /** NFCActivationCommandTests.swift:45-48: a switch-receiver command an active sensor accepted. */
    @Test
    public void command_switchReceiver_matchesCapturedWire() {
        // :: Act
        byte[] command = NfcCommand.command(NfcCommand.Code.SWITCH_RECEIVER, 1778097295L, new ReceiverId(0xc3251f23L));

        // :: Verify
        assertThat(Hex.of(command)).isEqualTo("02a87a8f9cfb69231f25c3aac8");
    }

    /** NFCActivationCommandTests.swift:49-52: a second accepted switch-receiver command, back to the first receiver. */
    @Test
    public void command_switchReceiverBack_matchesCapturedWire() {
        // :: Act
        byte[] command = NfcCommand.command(NfcCommand.Code.SWITCH_RECEIVER, 1778100341L, new ReceiverId(0x6f0d8378L));

        // :: Verify
        assertThat(Hex.of(command)).isEqualTo("02a87a75a8fb6978830d6f4cd4");
    }

    /** The patch-info read is the bare custom command with the sensor's manufacturer code. */
    @Test
    public void readPatchInfo_isA1WithManufacturerCode() {
        // :: Act
        String command = Hex.of(NfcCommand.READ_PATCH_INFO);

        // :: Verify
        assertThat(command).isEqualTo("02a17a");
    }
}
