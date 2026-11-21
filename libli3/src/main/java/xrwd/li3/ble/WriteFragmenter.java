package xrwd.li3.ble;

import java.util.ArrayList;
import java.util.List;

/**
 * Splits a phone→sensor message into ATT writes: each is a 2-byte little-endian byte offset followed by up to 18
 * payload bytes. Ported from RoundWhiteDiscKit {@code BLE/FrameAssembler.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class WriteFragmenter {

    public static final int PAYLOAD_SIZE = 18;

    private WriteFragmenter() {
    }

    public static List<byte[]> split(byte[] message) {
        List<byte[]> fragments = new ArrayList<>();
        for (int offset = 0; offset < message.length; offset += PAYLOAD_SIZE) {
            int length = Math.min(PAYLOAD_SIZE, message.length - offset);
            byte[] fragment = new byte[2 + length];
            fragment[0] = (byte) offset;
            fragment[1] = (byte) (offset >> 8);
            System.arraycopy(message, offset, fragment, 2, length);
            fragments.add(fragment);
        }
        return fragments;
    }
}
