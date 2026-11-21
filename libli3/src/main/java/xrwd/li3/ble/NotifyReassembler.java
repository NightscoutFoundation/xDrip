package xrwd.li3.ble;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Rebuilds one sensor→phone handshake message from its notifies: each is a 1-byte sequence number followed by up to
 * 19 payload bytes. A skipped sequence number means a notify was lost, so the message fails rather than arriving
 * short. Ported from RoundWhiteDiscKit {@code BLE/FrameAssembler.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class NotifyReassembler {

    private final int expectedLength;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private int nextSequence = -1;

    public NotifyReassembler(int expectedLength) {
        this.expectedLength = expectedLength;
    }

    /**
     * @return true once the message is complete
     * @throws IOException on an empty notify, a sequence gap, or more bytes than the message holds
     */
    public boolean add(byte[] chunk) throws IOException {
        if (chunk.length == 0) {
            throw new IOException("Empty notify");
        }
        int sequence = chunk[0] & 0xff;
        if (nextSequence >= 0 && sequence != nextSequence) {
            throw new IOException("Notify sequence gap: expected " + nextSequence + ", got " + sequence);
        }
        if (buffer.size() + chunk.length - 1 > expectedLength) {
            throw new IOException("Notify overruns a " + expectedLength + "-byte message");
        }
        nextSequence = (sequence + 1) & 0xff;
        buffer.write(chunk, 1, chunk.length - 1);
        return isComplete();
    }

    public boolean isComplete() {
        return buffer.size() == expectedLength;
    }

    /** @throws IllegalStateException if the message is not complete yet */
    public byte[] result() {
        if (!isComplete()) {
            throw new IllegalStateException("Have " + buffer.size() + " of " + expectedLength + " bytes");
        }
        return buffer.toByteArray();
    }
}
