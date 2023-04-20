package com.eveningoutpost.dexdrip.insulin.inpen.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;

import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.utils.math.Converters.unsignedByteToInt;
import static com.eveningoutpost.dexdrip.utils.math.Converters.unsignedShortToInt;

// jamorham

@RequiredArgsConstructor
public class RecordRx extends BaseRx {

    @Expose
    public int index;

    @Expose
    public long timestamp;

    @Expose
    public float units = -1;

    @Expose
    public int flags = 0;

    @Expose
    public int temperature = -10000;

    @Expose
    public byte battery = -1;

    @Expose
    public int duration = -1;

    @Expose
    int checkSum = -1;

    @Expose
    boolean checksumValid = false;

    final TimeRx timeRx;

    @Override
    public RecordRx fromBytes(final byte[] bytes) {

        if (bytes.length != 16) return null;
        this.bytes = bytes;
        buffer = ByteBuffer.wrap(this.bytes);
        index = unsignedShortToInt(buffer.getShort());
        flags = unsignedByteToInt(buffer.get());
        final int timeStampSeconds = buffer.getInt();
        units = ((float) buffer.getShort()) / 2; // negative implies rewind
        temperature = buffer.get();
        battery = buffer.get();
        final int timestampTwentieths = unsignedByteToInt(buffer.get());
        this.timestamp = timeStampSeconds * Constants.SECOND_IN_MS + timestampTwentieths * 50;
        duration = unsignedShortToInt(buffer.getShort()) * 50;
        checkSum = unsignedShortToInt(buffer.getShort());
        this.checksumValid = checksum(bytes) == checkSum;

        return this;
    }

    public boolean valid() {
        return checksumValid; // TODO other sanity checks?
    }

    public long getRealTimeStamp() {
        if (timeRx == null || timestamp < 0) return -1;
        return timeRx.fromPenTime(timestamp);
    }

    @Override
    public String toS() {
        return super.toS() + " " + JoH.dateTimeText(getRealTimeStamp());
    }
}
