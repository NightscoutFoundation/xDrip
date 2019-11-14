package com.eveningoutpost.dexdrip.watch.miband.message;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public abstract class BaseMessage implements Characteristic {
    private  ByteBuffer data = null;

    protected void init(final byte[] bytes, final int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
        data.put(bytes);
    }
    protected void init(final int length) {
        data = ByteBuffer.allocate(length).order(ByteOrder.LITTLE_ENDIAN);
    }

    protected void putData(final byte[] bytes) {
        data.put(bytes);
    }

    protected void putData(final byte b) {
        data.put(b);
    }

    protected byte[] getBytes() {
        return data.array();
    }
    public static byte fromUint8(int value) {
        return (byte) (value & 0xff);
    }
    public static byte[] fromUint16(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
        };
    }
}

interface Characteristic{
    UUID getCharacteristicUUID();
}
