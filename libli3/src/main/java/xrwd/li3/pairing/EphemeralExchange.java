package xrwd.li3.pairing;

import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.util.BigIntegers;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

/**
 * P-256 keys and ECDH as the Li3 handshake uses them: public keys travel as 65-byte uncompressed points and
 * shared secrets are the 32-byte x coordinate. Ported from RoundWhiteDiscKit {@code Pairing/EphemeralExchange.swift}
 * (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class EphemeralExchange {

    public static final ECDomainParameters P256;

    static {
        X9ECParameters x9 = ECNamedCurveTable.getByName("secp256r1");
        P256 = new ECDomainParameters(x9.getCurve(), x9.getG(), x9.getN(), x9.getH());
    }

    private EphemeralExchange() {
    }

    /** A fresh key pair for one handshake. */
    public static AsymmetricCipherKeyPair generate(SecureRandom random) {
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(P256, random));
        return generator.generateKeyPair();
    }

    /** The key pair behind a 32-byte big-endian scalar. */
    public static AsymmetricCipherKeyPair fromScalar(byte[] scalar32) throws GeneralSecurityException {
        if (scalar32.length != 32) {
            throw new GeneralSecurityException("A P-256 private key is 32 bytes, got " + scalar32.length);
        }
        BigInteger d = new BigInteger(1, scalar32);
        if (d.signum() == 0 || d.compareTo(P256.getN()) >= 0) {
            throw new GeneralSecurityException("P-256 private key out of range");
        }
        return new AsymmetricCipherKeyPair(new ECPublicKeyParameters(P256.getG().multiply(d).normalize(), P256),
                new ECPrivateKeyParameters(d, P256));
    }

    /** {@code 04 || X || Y}. */
    public static byte[] publicKey65(AsymmetricCipherKeyPair keyPair) {
        return ((ECPublicKeyParameters) keyPair.getPublic()).getQ().getEncoded(false);
    }

    /** Decodes a 65-byte uncompressed point and checks that it lies on P-256. */
    public static ECPublicKeyParameters parsePeer(byte[] raw65) throws GeneralSecurityException {
        if (raw65.length != 65 || raw65[0] != 0x04) {
            throw new GeneralSecurityException("Not an uncompressed P-256 point (" + raw65.length + " bytes)");
        }
        try {
            return new ECPublicKeyParameters(P256.getCurve().decodePoint(raw65), P256);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Not a point on P-256", e);
        }
    }

    /** The ECDH x coordinate, left-padded to 32 bytes. */
    public static byte[] sharedSecret(ECPrivateKeyParameters own, ECPublicKeyParameters peer) {
        ECDHBasicAgreement agreement = new ECDHBasicAgreement();
        agreement.init(own);
        return BigIntegers.asUnsignedByteArray(32, agreement.calculateAgreement(peer));
    }
}
