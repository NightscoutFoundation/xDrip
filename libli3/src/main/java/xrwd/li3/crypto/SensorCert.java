package xrwd.li3.crypto;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.signers.ECDSASigner;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import xrwd.li3.pairing.EphemeralExchange;

/**
 * The 140-byte certificate the sensor sends in Phase 2: {@code 01 || 10-byte header || static key (65) ||
 * ECDSA-P256-SHA256 raw r||s over bytes 0..76}. The collector does not verify it (no manufacturer signing key ships
 * here); {@link #verify} exists for when a key is supplied. Ported from RoundWhiteDiscKit
 * {@code Pairing/SensorCert.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class SensorCert {

    public static final int SIZE = 140;
    private static final int PUBKEY_OFFSET = 11;
    private static final int SIGNED_LENGTH = 76;

    private final byte[] raw;

    private SensorCert(byte[] raw) {
        this.raw = raw;
    }

    public static SensorCert parse(byte[] cert140) throws GeneralSecurityException {
        if (cert140.length != SIZE) {
            throw new GeneralSecurityException("Sensor certificate is " + cert140.length + " bytes, not " + SIZE);
        }
        if (cert140[PUBKEY_OFFSET] != 0x04) {
            throw new GeneralSecurityException("Sensor certificate key is not an uncompressed point");
        }
        return new SensorCert(cert140.clone());
    }

    /** The sensor's static key, {@code 04 || X || Y}. */
    public byte[] publicKey65() {
        return Arrays.copyOfRange(raw, PUBKEY_OFFSET, PUBKEY_OFFSET + 65);
    }

    public boolean verify(byte[] signingKey65) throws GeneralSecurityException {
        byte[] digest = new byte[32];
        SHA256Digest sha = new SHA256Digest();
        sha.update(raw, 0, SIGNED_LENGTH);
        sha.doFinal(digest, 0);
        ECDSASigner ecdsa = new ECDSASigner();
        ecdsa.init(false, EphemeralExchange.parsePeer(signingKey65));
        BigInteger r = new BigInteger(1, Arrays.copyOfRange(raw, SIGNED_LENGTH, SIGNED_LENGTH + 32));
        BigInteger s = new BigInteger(1, Arrays.copyOfRange(raw, SIGNED_LENGTH + 32, SIZE));
        return ecdsa.verifySignature(digest, r, s);
    }
}
