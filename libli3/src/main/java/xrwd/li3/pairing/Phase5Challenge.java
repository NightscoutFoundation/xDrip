package xrwd.li3.pairing;

import java.security.GeneralSecurityException;

import xrwd.li3.crypto.AesCcm;

/**
 * The Phase 5 phone→sensor answer: AES-CCM under kAuth of {@code R1 || R2 || tail4} with a 4-byte tag, zero-padded to
 * 54 bytes so it goes out as three 18-byte writes. The nonce is the 7-byte trailer of the sensor's 23-byte R1 notify.
 * Ported from RoundWhiteDiscKit {@code Pairing/ChallengeMessage.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Phase5Challenge {

    public static final int WIRE_SIZE = 54;
    private static final int TAG_SIZE = 4;

    private Phase5Challenge() {
    }

    public static byte[] build(byte[] kAuth, byte[] sensorR1, byte[] phoneR2, byte[] tail4, byte[] nonce7)
            throws GeneralSecurityException {
        requireLength("R1", sensorR1, 16);
        requireLength("R2", phoneR2, 16);
        requireLength("tail", tail4, 4);
        requireLength("nonce", nonce7, 7);
        byte[] plain = new byte[36];
        System.arraycopy(sensorR1, 0, plain, 0, 16);
        System.arraycopy(phoneR2, 0, plain, 16, 16);
        System.arraycopy(tail4, 0, plain, 32, 4);
        byte[] sealed = AesCcm.encrypt(kAuth, nonce7, plain, TAG_SIZE);
        byte[] wire = new byte[WIRE_SIZE];
        System.arraycopy(sealed, 0, wire, 0, sealed.length);
        return wire;
    }

    private static void requireLength(String name, byte[] value, int length) {
        if (value.length != length) {
            throw new IllegalArgumentException("Phase 5 " + name + " must be " + length + " bytes, got "
                    + value.length);
        }
    }
}
