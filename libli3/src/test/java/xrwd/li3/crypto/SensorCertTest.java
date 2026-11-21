package xrwd.li3.crypto;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.util.BigIntegers;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import xrwd.li3.pairing.EphemeralExchange;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link SensorCert}: the 140-byte certificate the sensor sends in Phase 2. The signing key is generated in
 * the test; the manufacturer's patch-signing keys are deliberately not in this module.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class SensorCertTest {

    private static final String SIGNER_SCALAR = "00000000000000000000000000000000000000000000000000000000000077aa";
    private static final String STATIC_SCALAR = "0000000000000000000000000000000000000000000000000000000000003333";

    // ===================================================== parse =====================================================

    /** The sensor's static public key sits at bytes 11..76. */
    @Test
    public void parse_validCert_readsStaticKey() throws Exception {
        // :: Setup
        byte[] staticKey = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(STATIC_SCALAR)));
        byte[] cert = signedCert(staticKey);

        // :: Act
        SensorCert parsed = SensorCert.parse(cert);

        // :: Verify
        assertThat(parsed.publicKey65()).isEqualTo(staticKey);
    }

    /** A certificate that is not 140 bytes is rejected. */
    @Test
    public void parse_wrongSize_throws() {
        // :: Act
        ThrowingRunnable parse = () -> SensorCert.parse(new byte[139]);

        // :: Verify
        assertThrows(GeneralSecurityException.class, parse);
    }

    // ==================================================== verify =====================================================

    /** A certificate signed over its first 76 bytes verifies against the signing key. */
    @Test
    public void verify_signedCert_true() throws Exception {
        // :: Setup
        byte[] staticKey = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(STATIC_SCALAR)));
        SensorCert cert = SensorCert.parse(signedCert(staticKey));

        // :: Act
        boolean valid = cert.verify(signerPublicKey());

        // :: Verify
        assertThat(valid).isTrue();
    }

    /** One flipped bit in the signed part makes the signature fail. */
    @Test
    public void verify_flippedBit_false() throws Exception {
        // :: Setup
        byte[] staticKey = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(STATIC_SCALAR)));
        byte[] raw = signedCert(staticKey);
        raw[3] ^= 1;
        SensorCert cert = SensorCert.parse(raw);

        // :: Act
        boolean valid = cert.verify(signerPublicKey());

        // :: Verify
        assertThat(valid).isFalse();
    }

    private static byte[] signerPublicKey() throws GeneralSecurityException {
        return EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(SIGNER_SCALAR)));
    }

    /** {@code 01 || 10-byte header || static key || ECDSA-P256-SHA256(raw r||s) over bytes 0..76}. */
    private static byte[] signedCert(byte[] staticKey65) throws GeneralSecurityException {
        byte[] cert = new byte[140];
        cert[0] = 0x01;
        for (int i = 1; i < 11; i++) {
            cert[i] = (byte) i;
        }
        System.arraycopy(staticKey65, 0, cert, 11, 65);
        byte[] digest = new byte[32];
        SHA256Digest sha = new SHA256Digest();
        sha.update(cert, 0, 76);
        sha.doFinal(digest, 0);
        AsymmetricCipherKeyPair signer = EphemeralExchange.fromScalar(Hex.toBytes(SIGNER_SCALAR));
        ECDSASigner ecdsa = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));
        ecdsa.init(true, (ECPrivateKeyParameters) signer.getPrivate());
        BigInteger[] rs = ecdsa.generateSignature(digest);
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, rs[0]), 0, cert, 76, 32);
        System.arraycopy(BigIntegers.asUnsignedByteArray(32, rs[1]), 0, cert, 108, 32);
        assertThat(Arrays.copyOfRange(cert, 11, 76)).isEqualTo(staticKey65);
        return cert;
    }
}
