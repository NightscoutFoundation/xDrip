package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utils.math.SkeletonCRC16;

import java.nio.ByteBuffer;

// jamorham

public abstract class BaseRx {

    protected int length = -1;

    protected byte[] bytes;
    protected ByteBuffer buffer;

    abstract public BaseRx fromBytes(final byte[] bytes);

    protected int checksum(final byte[] bytes) {
        return SkeletonCRC16.crc16Msb(bytes, 0xFFFF, 0x8005, bytes.length - 2);
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }


}
