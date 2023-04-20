package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.G5CollectionService;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

// jamorham

public class BaseMessage {

    protected static final String TAG = G5CollectionService.TAG; // meh
    static final int INVALID_TIME = 0xFFFFFFFF;
    @Expose
    long postExecuteGuardTime = 50;
    @Expose
    public volatile byte[] byteSequence;
    public ByteBuffer data;


    void init(final byte opcode, final int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        if (length == 1) {
            getByteSequence();
        } else if (length == 3) {
            appendCRC();
        }
    }

    byte[] appendCRC() {
        data.put(FastCRC16.calculate(getByteSequence(), byteSequence.length - 2));
        return getByteSequence();
    }

    boolean checkCRC(byte[] data) {
        if ((data == null) || (data.length < 3)) return false;
        final byte[] crc = FastCRC16.calculate(data, data.length - 2);
        return crc[0] == data[data.length - 2] && crc[1] == data[data.length - 1];
    }

    byte[] getByteSequence() {
        return byteSequence = data.array();
    }

    long guardTime() {
        return postExecuteGuardTime;
    }

    static long getUnsignedInt(ByteBuffer data) {
        return ((data.get() & 0xff) + ((data.get() & 0xff) << 8) + ((data.get() & 0xff) << 16) + ((data.get() & 0xff) << 24));
    }

    static int getUnsignedShort(ByteBuffer data) {
        return ((data.get() & 0xff) + ((data.get() & 0xff) << 8));
    }

    static int getUnsignedByte(ByteBuffer data) {
        return ((data.get() & 0xff));
    }

    static String dottedStringFromData(ByteBuffer data, int length) {

        final byte[] bytes = new byte[length];
        data.get(bytes);
        final StringBuilder sb = new StringBuilder(100);
        for (byte x : bytes) {
            if (sb.length() > 0) sb.append(".");
            sb.append(String.format(Locale.US, "%d", (x & 0xff)));
        }
        return sb.toString();
    }

    static int getUnixTime() {
        return (int) (JoH.tsl() / 1000);
    }
}