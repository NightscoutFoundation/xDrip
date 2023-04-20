package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.utils.math.Converters.unsignedIntToLong;

// jamorham

public class TimeRx extends BaseRx {

    @Expose
    @Getter
    long penTime = -1;

    @Expose
    long sampleTime = -1;

    @Getter
    @Expose
    long penEpoch = -1;

    @Override
    public TimeRx fromBytes(final byte[] bytes) {
        this.sampleTime = JoH.tsl();
        if (bytes.length != 4) return null;
        buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        this.penTime = unsignedIntToLong(buffer.getInt());
        this.penEpoch = sampleTime - (penTime * Constants.SECOND_IN_MS);
        if (penEpoch < 1462628838000L) return null;
        return this;
    }


    public long fromPenTime(final long clock) {
        return penEpoch + clock;
    }
}
