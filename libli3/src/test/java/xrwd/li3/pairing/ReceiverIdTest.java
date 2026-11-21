package xrwd.li3.pairing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.util.Locale;

/**
 * Tests for {@link ReceiverId}: the 32-bit receiver ID a Li3 sensor is activated with, derived from an account ID
 * (two app-specific folds) or from a per-install unique ID. Vectors come from RoundWhiteDiscKit 7dd6396,
 * {@code *ReceiverIDTests.swift} and {@code NFCActivationCommandTests.swift:5-31}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class ReceiverIdTest {

    /** DiaBLE's documented sample account, not a real one. */
    private static final String SAMPLE_ACCOUNT = "2977dec2-492a-11ea-9702-0242ac110002";

    // ================================================== fromAccount ==================================================

    /** *ReceiverIDTests.swift:10-25: the sample account folds to the first official app's ID. */
    @Test
    public void fromAccount_li3App_matchesDocumentedValue() {
        // :: Act
        ReceiverId id = ReceiverId.fromAccount(SAMPLE_ACCOUNT, ReceiverId.Fold.LI3_APP);

        // :: Verify
        assertThat(id.value()).isEqualTo(524_381_581L);
        assertThat(id.value()).isEqualTo(0x1f416d8dL);
        assertThat(id.littleEndianHex()).isEqualTo("8d6d411f");
    }

    /** *ReceiverIDTests.swift:10-25: the sample account folds to the second official app's ID. */
    @Test
    public void fromAccount_multiSensorApp_matchesDocumentedValue() {
        // :: Act
        ReceiverId id = ReceiverId.fromAccount(SAMPLE_ACCOUNT, ReceiverId.Fold.MULTI_SENSOR_APP);

        // :: Verify
        assertThat(id.value()).isEqualTo(2_203_515_335L);
        assertThat(id.littleEndianHex()).isEqualTo("c7f95683");
    }

    /** *ReceiverIDTests.swift:49-61: an upper-case account ID gives the same ID for both folds. */
    @Test
    public void fromAccount_upperCase_sameAsLowerCase() {
        for (ReceiverId.Fold fold : ReceiverId.Fold.values()) {
            // :: Act
            ReceiverId upper = ReceiverId.fromAccount(SAMPLE_ACCOUNT.toUpperCase(Locale.ROOT), fold);

            // :: Verify
            assertThat(upper.value()).isEqualTo(ReceiverId.fromAccount(SAMPLE_ACCOUNT, fold).value());
        }
    }

    /** *ReceiverIDTests.swift:66-72: a trailing partial word is left-padded, not dropped or right-padded. */
    @Test
    public void fromAccount_multiSensorApp_shortTrailingWordLeftPadded() {
        // :: Act
        ReceiverId id = ReceiverId.fromAccount("2977dec2-4", ReceiverId.Fold.MULTI_SENSOR_APP);

        // :: Verify
        assertThat(id.value()).isEqualTo((0x32393737L + 0x64656332L + 0x00002d34L) & 0xffffffffL);
    }

    /** *ReceiverIDTests.swift:30-45: the first app's fold equals the accountless fold on lower case. */
    @Test
    public void fromAccount_li3App_equalsAccountlessOnCapturedIds() {
        for (String uniqueId : new String[]{"5abb0ad8-dc2e-4ede-9e2d-67472a3e630e",
                "6147368e-c060-44a5-9e96-1c02333f43c0"}) {
            // :: Act
            ReceiverId fromAccount = ReceiverId.fromAccount(uniqueId, ReceiverId.Fold.LI3_APP);

            // :: Verify
            assertThat(fromAccount.value()).isEqualTo(ReceiverId.accountless(uniqueId).value());
        }
    }

    // ================================================== accountless ==================================================

    /** NFCActivationCommandTests.swift:5-23: the unique ID behind an accepted fresh-pair activation. */
    @Test
    public void accountless_capturedFreshPair_matches() {
        // :: Act
        ReceiverId id = ReceiverId.accountless("5abb0ad8-dc2e-4ede-9e2d-67472a3e630e");

        // :: Verify
        assertThat(id.value()).isEqualTo(0x6f0d8378L);
        assertThat(id.littleEndianHex()).isEqualTo("78830d6f");
    }

    /** NFCActivationCommandTests.swift:17-21: the unique ID behind an accepted fresh-install activation. */
    @Test
    public void accountless_capturedFreshInstall_matches() {
        // :: Act
        ReceiverId id = ReceiverId.accountless("6147368e-c060-44a5-9e96-1c02333f43c0");

        // :: Verify
        assertThat(id.value()).isEqualTo(0xc3251f23L);
        assertThat(id.littleEndianHex()).isEqualTo("231f25c3");
    }

    /** *ReceiverIDTests.swift:57-60: the accountless fold does not lowercase, so case changes the ID. */
    @Test
    public void accountless_upperCase_differsFromLowerCase() {
        // :: Act
        ReceiverId upper = ReceiverId.accountless(SAMPLE_ACCOUNT.toUpperCase(Locale.ROOT));

        // :: Verify
        assertThat(upper.value()).isNotEqualTo(ReceiverId.accountless(SAMPLE_ACCOUNT).value());
    }

    // ============================================== fromLittleEndianHex ==============================================

    /** NFCActivationCommandTests.swift:25-31: colon-separated wire bytes parse back to the ID. */
    @Test
    public void fromLittleEndianHex_withColons_parses() {
        // :: Act
        ReceiverId id = ReceiverId.fromLittleEndianHex("78:83:0d:6f");

        // :: Verify
        assertThat(id.value()).isEqualTo(0x6f0d8378L);
        assertThat(id.littleEndianHex()).isEqualTo("78830d6f");
        assertThat(id.littleEndian()).isEqualTo(new byte[]{0x78, (byte) 0x83, 0x0d, 0x6f});
        assertThat(id.display()).isEqualTo("0x6f0d8378 / 78830d6f");
    }

    /** NFCActivationCommandTests.swift:31: a 0x prefix is ignored. */
    @Test
    public void fromLittleEndianHex_withPrefix_parses() {
        // :: Act
        ReceiverId id = ReceiverId.fromLittleEndianHex("0x78830d6f");

        // :: Verify
        assertThat(id.value()).isEqualTo(0x6f0d8378L);
    }

    /** Anything but four bytes is not a receiver ID. */
    @Test
    public void fromLittleEndianHex_wrongLength_throws() {
        // :: Act
        ThrowingRunnable parse = () -> ReceiverId.fromLittleEndianHex("78830d");

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }
}
