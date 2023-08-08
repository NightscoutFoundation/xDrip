package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.insulin.inpen.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// jamorham

public abstract class BaseTx {
    static final long HEADER_MAGIC = 2299122346905637072L;
    public ByteBuffer data = null;
    private byte[] byteSequence = null;

    void init(final int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    static int getCounterId() {
        return -Constants.COUNTER_ID;
    }

    public byte[] getBytes() {
        if (byteSequence == null) {
            byteSequence = data.array();
        }
        return byteSequence;
    }

    public static byte[] format(final byte[] input) {
        final byte[] output = new byte[input.length];
        for (int p = 0; p < input.length; p++) {
            output[p] = (byte) (p < 0x0D ? input[p] ^ ~p : input[p]);
        }
        return output;
    }

    public static byte[] prepare(final byte[] input, final byte[] bytes) {
        final byte[] output = new byte[input.length];
        for (int p = 0; p < input.length; p++) {
            output[p] = (byte) (input[p] ^ bytes[p % bytes.length]);
        }
        return output;
    }

    public String toHexDump() {
        return HexDump.dumpHexString(getBytes());
    }

}
