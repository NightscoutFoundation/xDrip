package xrwd.li3.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.security.GeneralSecurityException;
import java.util.Locale;

import xrwd.li3.pairing.EphemeralExchange;
import xrwd.li3.pairing.Phase5Challenge;
import xrwd.li3.pairing.Phase6Response;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link AppIdentity}, {@link SessionKeys}, {@link Phase5Challenge} and {@link Phase6Response}: the app
 * credential, the kAuth key agreement and the Phase 5/6 messages. Cross-checked against RoundWhiteDiscKit's Python
 * reference with synthetic scalars (phone ephemeral 0x1111, phone static 0x4444), so no real credential is involved.
 * Ported from RoundWhiteDiscKit 7dd6396 {@code AppIdentityTests.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class AppIdentityTest {

    private static final String SCALAR_HEX = "0000000000000000000000000000000000000000000000000000000000004444";
    private static final String CERT_HEX = "03000102030405060708090a0b0c0d0e0f1000005f92a79201000000000000000004a85b3aad"
            + "6f3a346d85523141cb434e1caf4c642b2b3cc952cb07a635cb6a1df1bcbdadb66bae8eb25eca92f8b08b67be02cc736150dd7ffcd3"
            + "f019c875f9ffde" + repeat('0', 128);
    private static final byte[] SENSOR_EPHEMERAL = Hex.toBytes("04a62f048f367359809c2d46c2049d7d7bf268c3c073c47275"
            + "3cb18a24a8ad20b1caccf8104b666795c7f35dac9dc444b3c2c61978198c49859955b99956da5edb");
    private static final byte[] SENSOR_STATIC = Hex.toBytes("048570e95d85825286db92c78317679bdd8ffe3c90d0af84291bf6"
            + "4132b66fcc99c926f087212d75b1f4dbc5d4999b4c5605adf66db801a4de371cdad39ebc55e5");
    private static final byte[] K_AUTH = Hex.toBytes("d321308aaf9d1d68132ff5192ecb67f4");
    private static final String EXPECTED_ANSWER = "a4016be402aef5485155238b8dee3f36f564dd18819889ff25b35221a7f6f207b"
            + "3512232816ce2c5";
    private static final byte[] SENSOR_REPLY = Hex.toBytes("5e10ddf6634fcf8fce8e81bf67d47dd5a66995159a53f7cbc3939a75"
            + "d34a565844e1be3b5ca66092a5127f362ae7c7278feb1184058ce9aed6a77f43b1b2b3b4b5b6b7");

    // ====================================================== of =======================================================

    /** AppIdentityTests.swift:33-37: a scalar that matches the certificate's public point is accepted. */
    @Test
    public void of_matchingKeyAndCert_keepsLabel() throws Exception {
        // :: Act
        AppIdentity identity = AppIdentity.of("test", Hex.toBytes(SCALAR_HEX), Hex.toBytes(CERT_HEX));

        // :: Verify
        assertThat(identity.label()).isEqualTo("test");
        assertThat(identity.certificate()).isEqualTo(Hex.toBytes(CERT_HEX));
    }

    /** AppIdentityTests.swift:39-45: a scalar that is not behind the certificate's public point is rejected. */
    @Test
    public void of_keyCertMismatch_throws() {
        // :: Act
        ThrowingRunnable build = () -> AppIdentity.of("bad", Hex.toBytes(SCALAR_HEX.replace("4444", "5555")),
                Hex.toBytes(CERT_HEX));

        // :: Verify
        assertThrows(GeneralSecurityException.class, build);
    }

    /** A certificate that is not 162 bytes is rejected before any key work. */
    @Test
    public void of_shortCert_throws() {
        // :: Act
        ThrowingRunnable build = () -> AppIdentity.of("short", Hex.toBytes(SCALAR_HEX),
                Hex.toBytes(CERT_HEX.substring(2)));

        // :: Verify
        assertThrows(GeneralSecurityException.class, build);
    }

    // =================================================== toString ====================================================

    /** The credential is a secret: toString names the label and carries no hex of the key or the certificate. */
    @Test
    public void toString_showsLabelOnly() throws Exception {
        // :: Setup
        AppIdentity identity = AppIdentity.of("test", Hex.toBytes(SCALAR_HEX), Hex.toBytes(CERT_HEX));

        // :: Act
        String text = identity.toString().toLowerCase(Locale.ROOT);

        // :: Verify
        assertThat(text).contains("test");
        assertThat(text).doesNotContain("4444");
        assertThat(text).doesNotContain("a85b3aad");
    }

    // ===================================================== kAuth =====================================================

    /** AppIdentityTests.swift:47-54: kAuth from both ECDH secrets matches the Python reference. */
    @Test
    public void kAuth_syntheticScalars_matchesReference() throws Exception {
        // :: Setup
        AppIdentity identity = AppIdentity.of("test", Hex.toBytes(SCALAR_HEX), Hex.toBytes(CERT_HEX));
        AsymmetricCipherKeyPair phoneEphemeral = EphemeralExchange.fromScalar(Hex.toBytes(SCALAR_HEX.replace("4444",
                "1111")));

        // :: Act
        byte[] kAuth = SessionKeys.kAuth(identity, phoneEphemeral, SENSOR_EPHEMERAL, SENSOR_STATIC);

        // :: Verify
        assertThat(kAuth).isEqualTo(K_AUTH);
    }

    // ==================================================== phase5 =====================================================

    /** AppIdentityTests.swift:56-67: the Phase 5 answer is ct||tag of the reference, then a zero pad to 54 bytes. */
    @Test
    public void phase5_build_matchesReference() throws Exception {
        // :: Act
        byte[] wire = Phase5Challenge.build(K_AUTH, range(0x00, 16), range(0x40, 16), Hex.toBytes("deadbeef"),
                Hex.toBytes("a1a2a3a4a5a6a7"));

        // :: Verify
        assertThat(wire).hasLength(54);
        assertThat(Hex.of(wire)).isEqualTo(EXPECTED_ANSWER + repeat('0', 28));
    }

    /** A tail that is not four bytes is rejected rather than encrypted into a wrong-size answer. */
    @Test
    public void phase5_wrongTail_throws() {
        // :: Act
        ThrowingRunnable build = () -> Phase5Challenge.build(K_AUTH, range(0x00, 16), range(0x40, 16),
                Hex.toBytes("dead"), Hex.toBytes("a1a2a3a4a5a6a7"));

        // :: Verify
        assertThrows(IllegalArgumentException.class, build);
    }

    // ==================================================== phase6 =====================================================

    /** AppIdentityTests.swift:69-73: the sensor's Phase 6 reply opens to both nonces, kEnc and ivEnc. */
    @Test
    public void phase6_open_recoversSessionMaterial() throws Exception {
        // :: Act
        Phase6Response response = Phase6Response.open(K_AUTH, SENSOR_REPLY);

        // :: Verify
        assertThat(response.phoneR2()).isEqualTo(range(0x40, 16));
        assertThat(response.sensorR1()).isEqualTo(range(0x00, 16));
        assertThat(response.kEnc()).isEqualTo(range(0x80, 16));
        assertThat(response.ivEnc()).isEqualTo(range(0x90, 8));
    }

    /** A reply under the wrong kAuth fails authentication instead of yielding garbage keys. */
    @Test
    public void phase6_wrongKey_throws() {
        // :: Setup
        byte[] wrongKey = K_AUTH.clone();
        wrongKey[0] ^= 1;

        // :: Act
        ThrowingRunnable open = () -> Phase6Response.open(wrongKey, SENSOR_REPLY);

        // :: Verify
        assertThrows(GeneralSecurityException.class, open);
    }

    private static byte[] range(int start, int length) {
        byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) (start + i);
        }
        return out;
    }

    private static String repeat(char c, int count) {
        return new String(new char[count]).replace('\0', c);
    }
}
