package xrwd.li3.nfc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import xrwd.li3.util.Hex;

/**
 * Tests for {@link ActivationResponse}: the sensor's reply to activate or switch-receiver, which carries the BLE
 * address and PIN. Vectors from RoundWhiteDiscKit 7dd6396 {@code NFCActivationCommandTests.swift:113-148}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class ActivationResponseTest {

    private static final String ACTIVATED = "00a50058f9b8df22cc3225ec7200000000ad06";

    // ===================================================== parse =====================================================

    /** NFCActivationCommandTests.swift:113-121: the address, PIN and time sit at the corrected boundaries. */
    @Test
    public void parse_activation_readsAddressPinAndTime() {
        // :: Act
        ActivationResponse response = ActivationResponse.parse(Hex.toBytes(ACTIVATED));

        // :: Verify
        assertThat(response.bleAddress()).isEqualTo("CC:22:DF:B8:F9:58");
        assertThat(Hex.of(response.blePin())).isEqualTo("3225ec72");
        assertThat(Hex.of(response.activationTimeRaw())).isEqualTo("00000000");
    }

    /** NFCActivationCommandTests.swift:134-142: a switch-receiver reply has the same layout plus the start time. */
    @Test
    public void parse_switchReceiver_readsActivationTime() {
        // :: Act
        ActivationResponse response = ActivationResponse.parse(Hex.toBytes("00a50058f9b8df22cc02bafbbbfc2bee698e6e"));

        // :: Verify
        assertThat(response.bleAddress()).isEqualTo("CC:22:DF:B8:F9:58");
        assertThat(Hex.of(response.blePin())).isEqualTo("02bafbbb");
        assertThat(Hex.of(response.activationTimeRaw())).isEqualTo("fc2bee69");
    }

    /** NFCActivationCommandTests.swift:144-148: the 18-byte reply without the leading 00 parses the same. */
    @Test
    public void parse_withoutLeadingStatusByte_parses() {
        // :: Act
        ActivationResponse response = ActivationResponse.parse(Hex.toBytes(ACTIVATED.substring(2)));

        // :: Verify
        assertThat(response.bleAddress()).isEqualTo("CC:22:DF:B8:F9:58");
        assertThat(Hex.of(response.blePin())).isEqualTo("3225ec72");
    }

    // =================================================== malformed ===================================================

    /** One byte short of the 18-byte form is rejected. */
    @Test
    public void parse_seventeenBytes_throws() {
        // :: Act
        ThrowingRunnable parse = () -> ActivationResponse.parse(Hex.toBytes(ACTIVATED.substring(4)));

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    /** One byte past the 19-byte form is rejected rather than read with shifted fields. */
    @Test
    public void parse_twentyBytes_throws() {
        // :: Act
        ThrowingRunnable parse = () -> ActivationResponse.parse(Hex.toBytes(ACTIVATED + "00"));

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    /** The exception message never carries the reply bytes, since they hold the BLE PIN. */
    @Test
    public void parse_malformed_messageOmitsBytes() {
        // :: Act
        ThrowingRunnable parse = () -> ActivationResponse.parse(Hex.toBytes(ACTIVATED + "00"));

        // :: Verify
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, parse);
        assertThat(e.getMessage()).doesNotContain("3225ec72");
    }
}
