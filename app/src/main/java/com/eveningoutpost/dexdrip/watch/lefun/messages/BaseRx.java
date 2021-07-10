package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

import com.eveningoutpost.dexdrip.watch.lefun.LeFun;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public abstract class BaseRx {

    public static final byte START_BYTE = 0x5A;

    protected int length = -1;

    protected byte[] bytes;
    protected ByteBuffer buffer;

    abstract public BaseRx fromBytes(final byte[] bytes);

    protected boolean validate(final byte opcode) {
        if (buffer == null) {
            buffer = ByteBuffer.wrap(bytes);
        }

        return (bytes != null && bytes.length >= 4
                && buffer.get() == START_BYTE
                && bytes[1] == bytes.length
                && buffer.get() == length
                && buffer.get() == opcode
                && bytes[bytes.length - 1] == LeFun.calculateCRC(bytes, bytes.length - 1));
    }

    protected String getStringBytes(final int count) {
        final byte[] buf = new byte[count];
        int i;
        for (i = 0; i < count; i++) {
            buf[i] = buffer.get();
            if (buf[i] == 0x00) break;
        }
        try {
            return new String(buf, 0, i, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    protected String getCanonicalVersion(final int count) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(".");
            sb.append(buffer.get());
        }
        return sb.toString();
    }

}
