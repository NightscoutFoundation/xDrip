package xrwd.li3.data;

import java.util.Arrays;

/**
 * One data-plane frame as it travels over BLE: {@code ciphertext || tag || sequenceLE2}. Ported from
 * RoundWhiteDiscKit {@code DataPlane/DataFrame.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class DataFrame {

    static final int TRAILER_SIZE = 2;

    private final byte[] encrypted;
    private final int sequence;

    private DataFrame(byte[] encrypted, int sequence) {
        this.encrypted = encrypted;
        this.sequence = sequence;
    }

    /** @throws IllegalArgumentException if the frame has no room for the trailer and one encrypted byte */
    public static DataFrame parse(byte[] wire) {
        if (wire.length < TRAILER_SIZE + 1) {
            throw new IllegalArgumentException("Data frame of " + wire.length + " bytes is too short");
        }
        int split = wire.length - TRAILER_SIZE;
        int sequence = (wire[split] & 0xff) | (wire[split + 1] & 0xff) << 8;
        return new DataFrame(Arrays.copyOf(wire, split), sequence);
    }

    static byte[] toWire(byte[] encrypted, int sequence) {
        byte[] wire = Arrays.copyOf(encrypted, encrypted.length + TRAILER_SIZE);
        wire[encrypted.length] = (byte) sequence;
        wire[encrypted.length + 1] = (byte) (sequence >> 8);
        return wire;
    }

    /** Ciphertext followed by the CCM tag. */
    public byte[] encrypted() {
        return encrypted.clone();
    }

    public int sequence() {
        return sequence;
    }

    public byte[] toWire() {
        return toWire(encrypted, sequence);
    }
}
