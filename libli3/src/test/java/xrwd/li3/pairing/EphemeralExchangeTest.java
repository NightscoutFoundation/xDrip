package xrwd.li3.pairing;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.security.GeneralSecurityException;

import xrwd.li3.util.Hex;

/**
 * Tests for {@link EphemeralExchange}: the P-256 keys of the pairing handshake. The sensor sends its keys over the
 * air, so a key that is not a point on the curve must be refused before it is used in a key agreement.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class EphemeralExchangeTest {

    /** The order n of P-256. */
    private static final String ORDER = "ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551";

    // ================================================== fromScalar ===================================================

    /** A private key that is not 32 bytes is refused. */
    @Test
    public void fromScalar_not32Bytes_throws() {
        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.fromScalar(new byte[31]);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, parse);
        assertThat(e).hasMessageThat().contains("31");
    }

    /** A private key of zero is refused: it has no public key. */
    @Test
    public void fromScalar_zero_throws() {
        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.fromScalar(new byte[32]);

        // :: Verify
        assertThrows(GeneralSecurityException.class, parse);
    }

    /** A private key equal to the curve order is refused: it is zero on the curve. */
    @Test
    public void fromScalar_curveOrder_throws() {
        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.fromScalar(Hex.toBytes(ORDER));

        // :: Verify
        assertThrows(GeneralSecurityException.class, parse);
    }

    // =================================================== parsePeer ===================================================

    /** A public key from its own private key parses back to the same point. */
    @Test
    public void parsePeer_ownPublicKey_parses() throws Exception {
        // :: Setup
        byte[] key65 = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(
                "00000000000000000000000000000000000000000000000000000000000a1b2c")));

        // :: Act
        byte[] parsed = EphemeralExchange.parsePeer(key65).getQ().getEncoded(false);

        // :: Verify
        assertThat(parsed).isEqualTo(key65);
    }

    /** A key that is not 65 bytes is refused. */
    @Test
    public void parsePeer_wrongLength_throws() {
        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.parsePeer(new byte[64]);

        // :: Verify
        assertThrows(GeneralSecurityException.class, parse);
    }

    /** A 65-byte key that does not start with the uncompressed-point marker 04 is refused. */
    @Test
    public void parsePeer_notUncompressed_throws() throws Exception {
        // :: Setup
        byte[] key65 = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(
                "00000000000000000000000000000000000000000000000000000000000a1b2c")));
        key65[0] = 0x02;

        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.parsePeer(key65);

        // :: Verify
        assertThrows(GeneralSecurityException.class, parse);
    }

    /** A key whose coordinates are not a point on P-256 is refused, so it never reaches a key agreement. */
    @Test
    public void parsePeer_pointNotOnCurve_throws() throws Exception {
        // :: Setup
        byte[] key65 = EphemeralExchange.publicKey65(EphemeralExchange.fromScalar(Hex.toBytes(
                "00000000000000000000000000000000000000000000000000000000000a1b2c")));
        key65[64] ^= 0x01;

        // :: Act
        ThrowingRunnable parse = () -> EphemeralExchange.parsePeer(key65);

        // :: Verify
        GeneralSecurityException e = assertThrows(GeneralSecurityException.class, parse);
        assertThat(e).hasMessageThat().contains("Not a point on P-256");
    }
}
