package xrwd.li3.pairing;

import java.security.GeneralSecurityException;
import java.util.Arrays;

import xrwd.li3.crypto.AesCcm;

/**
 * The Phase 6 sensor→phone reply: {@code ciphertext(56) || tag(4) || nonce(7)}, opening under kAuth to
 * {@code R2 || R1 || kEnc(16) || ivEnc(8)}. Holds session keys: no {@code toString} shows them.
 * Ported from RoundWhiteDiscKit {@code Pairing/Phase6Response.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Phase6Response {

    public static final int WIRE_SIZE = 67;
    private static final int SEALED_SIZE = 60;
    private static final int TAG_SIZE = 4;

    private final byte[] plain;

    private Phase6Response(byte[] plain) {
        this.plain = plain;
    }

    /** @throws GeneralSecurityException on a wrong size or a tag that does not match */
    public static Phase6Response open(byte[] kAuth, byte[] wire67) throws GeneralSecurityException {
        if (wire67.length != WIRE_SIZE) {
            throw new GeneralSecurityException("Phase 6 reply is " + wire67.length + " bytes, not " + WIRE_SIZE);
        }
        byte[] sealed = Arrays.copyOfRange(wire67, 0, SEALED_SIZE);
        byte[] nonce = Arrays.copyOfRange(wire67, SEALED_SIZE, WIRE_SIZE);
        return new Phase6Response(AesCcm.decrypt(kAuth, nonce, sealed, TAG_SIZE));
    }

    public byte[] phoneR2() {
        return Arrays.copyOfRange(plain, 0, 16);
    }

    public byte[] sensorR1() {
        return Arrays.copyOfRange(plain, 16, 32);
    }

    public byte[] kEnc() {
        return Arrays.copyOfRange(plain, 32, 48);
    }

    public byte[] ivEnc() {
        return Arrays.copyOfRange(plain, 48, 56);
    }

    @Override
    public String toString() {
        return "Phase6Response";
    }
}
