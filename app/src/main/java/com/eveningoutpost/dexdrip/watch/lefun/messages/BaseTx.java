package com.eveningoutpost.dexdrip.watch.lefun.messages;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static com.eveningoutpost.dexdrip.watch.lefun.LeFun.calculateCRC;

// jamorham

public abstract class BaseTx {

    private static final int HEADER_FOOTER_SIZE = 3;
    private static final byte START_OF_MESSAGE = (byte) 0xAB;

    protected int bitmap_start_offset = -1;
    protected int bitmap = 0x0000;

    static final byte READ = 0x00;
    protected static final byte WRITE = 0x01;

    public ByteBuffer data = null;
    private byte[] byteSequence = null;


    void init(final int length) {
        final int packet_length = length + HEADER_FOOTER_SIZE;
        data = ByteBuffer.allocate(packet_length);
        data.put(START_OF_MESSAGE);
        data.put((byte) packet_length);

    }

    public byte[] getBytes() {
        if (byteSequence == null) {
            byteSequence = data.array();
            injectChecksum(byteSequence);
        }
        return byteSequence;
    }

    public List<byte[]> getFragmentStream() {
        final List<byte[]> list = new LinkedList<>();
        list.add(getBytes());
        return list;
    }

    public String toHexDump() {
        return HexDump.dumpHexString(getBytes());
    }

    public static byte[] prepareRaw(final byte[] buffer) {
        buffer[1] = (byte) buffer.length;
        injectChecksum(buffer);
        return buffer;
    }

    private static void injectChecksum(final byte[] buffer) {
        buffer[buffer.length - 1] = calculateCRC(buffer, buffer.length - 1);
    }

    private void updateBitmap() {
        if (bitmap_start_offset < 0) {
            throw new RuntimeException("Bitfield manipulation without defining offset");
        }
        data.putShort(bitmap_start_offset, (short) (bitmap & 0xFFFF));
    }

    public BaseTx set(final int screen, final boolean enabled) {
        if (enabled) {
            bitmap |= 1 << screen;
        } else {
            bitmap &= ~(1 << screen);
        }
        updateBitmap();
        return this;
    }

    public BaseTx enable(final int screen) {
        return set(screen, true);
    }

    public BaseTx disable(final int screen) {
        return set(screen, false);
    }


}
