package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;

import lombok.Getter;

// jamorham

public class AdvertRx extends BaseRx {

    @Expose
    @Getter
    byte[] flagBytes = new byte[4];

    @Expose
    @Getter
    int flags = 0;

    @Override
    public AdvertRx fromBytes(final byte[] bytes) {
        if (bytes == null || bytes.length < 16) return null;
        this.bytes = bytes;
        buffer = ByteBuffer.wrap(this.bytes);
        buffer.position(10);
        flags = buffer.getInt();
        flagBytes = ByteBuffer.allocate(4)
                .putInt(flags).array();
        flags >>= 24;
        return this;
    }
}
