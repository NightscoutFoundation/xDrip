package xrwd.li3.nfc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import xrwd.li3.util.Hex;

/**
 * Tests for {@link PatchInfo}: the sensor's reply to the patch-info read. Vectors are US sensors captured by
 * RoundWhiteDiscKit 7dd6396 {@code NFCActivationCommandTests.swift:62-111}; no other region has a capture.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class PatchInfoTest {

    private static final String FRESH = "00a50001000200010060541e020401040c0130525243393839415151ff17";
    private static final String ACTIVE = "00a50001000200010060541e020401040c04305252433938394151c6ca";

    // ===================================================== parse =====================================================

    /** NFCActivationCommandTests.swift:62-73: every field of a fresh sensor's reply. */
    @Test
    public void parse_freshSensor_readsAllFields() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes(FRESH));

        // :: Verify
        assertThat(info.serial()).isEqualTo("0RRC989AQ");
        assertThat(info.stateByte()).isEqualTo(0x01);
        assertThat(info.wearMinutes()).isEqualTo(21600);
        assertThat(info.firmware()).isEqualTo("1.4.2.30");
        assertThat(info.generation()).isEqualTo(1);
        assertThat(info.productType()).isEqualTo(4);
        assertThat(info.warmupMinutes()).isEqualTo(60);
        assertThat(info.securityVersion()).isEqualTo(1);
        assertThat(info.region()).isEqualTo(PatchInfo.Region.US);
    }

    /** NFCActivationCommandTests.swift:75-86: an active sensor's reply differs only in state and trailer. */
    @Test
    public void parse_activeSensor_readsState() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes(ACTIVE));

        // :: Verify
        assertThat(info.serial()).isEqualTo("0RRC989AQ");
        assertThat(info.stateByte()).isEqualTo(0x04);
        assertThat(info.wearMinutes()).isEqualTo(21600);
        assertThat(info.firmware()).isEqualTo("1.4.2.30");
        assertThat(info.warmupMinutes()).isEqualTo(60);
        assertThat(info.region()).isEqualTo(PatchInfo.Region.US);
    }

    /** NFCActivationCommandTests.swift:89-97: a reply without the leading 00 status byte parses the same. */
    @Test
    public void parse_withoutLeadingStatusByte_parses() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes(ACTIVE.substring(2)));

        // :: Verify
        assertThat(info.serial()).isEqualTo("0RRC989AQ");
        assertThat(info.stateByte()).isEqualTo(0x04);
        assertThat(info.region()).isEqualTo(PatchInfo.Region.US);
    }

    /** NFCActivationCommandTests.swift:99-108: a repeated a5 pad is collapsed before the fields are read. */
    @Test
    public void parse_repeatedA5Pad_collapsed() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes("00a5a5a5a5a5a5a5" + ACTIVE.substring(2)));

        // :: Verify
        assertThat(info.serial()).isEqualTo("0RRC989AQ");
        assertThat(info.stateByte()).isEqualTo(0x04);
        assertThat(info.recommended()).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
        assertThat(info.region()).isEqualTo(PatchInfo.Region.US);
    }

    // ================================================== recommended ==================================================

    /** A sensor in state 0x01 has never been started, so it gets the activate command. */
    @Test
    public void recommended_freshSensor_activate() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes(FRESH));

        // :: Verify
        assertThat(info.recommended()).isEqualTo(NfcCommand.Code.ACTIVATE);
    }

    /** Any other state means the sensor is running, so it gets the switch-receiver command. */
    @Test
    public void recommended_activeSensor_switchReceiver() {
        // :: Act
        PatchInfo info = PatchInfo.parse(Hex.toBytes(ACTIVE));

        // :: Verify
        assertThat(info.recommended()).isEqualTo(NfcCommand.Code.SWITCH_RECEIVER);
    }

    // =================================================== malformed ===================================================

    /** A reply cut short before the serial ends is rejected, not read past its end. */
    @Test
    public void parse_truncated_throws() {
        // :: Act
        ThrowingRunnable parse = () -> PatchInfo.parse(Hex.toBytes(ACTIVE.substring(0, 50)));

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    /** A reply that does not start with the a5 marker is not patch info. */
    @Test
    public void parse_wrongMarker_throws() {
        // :: Act
        ThrowingRunnable parse = () -> PatchInfo.parse(Hex.toBytes("00b6" + ACTIVE.substring(4)));

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }
}
