package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

// jamorham

public class BatteryRx extends BaseRx {

    @Expose
    @Getter
    int batteryPercent = -1;

    @Override
    public BatteryRx fromBytes(final byte[] bytes) {
        if (bytes.length != 1) return null;
        buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        batteryPercent = buffer.get();
        return this;
    }

}
