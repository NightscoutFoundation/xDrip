package com.eveningoutpost.dexdrip.watch.thinjam.messages;


import com.eveningoutpost.dexdrip.Models.JoH;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_SET_TIME;

// jamorham

public class SetTimeTx extends BaseTx {

    private static final long Y2K_EPOCH = 946684800;

    @Expose
    @Getter
    private long timestamp;
    @Expose
    private int tzOffset;

    public SetTimeTx() {

        final Calendar mCalendar = new GregorianCalendar();
        final TimeZone mTimeZone = mCalendar.getTimeZone();
        this.tzOffset = (mTimeZone.getRawOffset() + (int) getActualDSTOffset(mTimeZone)) / 1000;

        init(OPCODE_SET_TIME, 8);
        this.timestamp = JoH.tsl();
        long time = (timestamp / 1000) - Y2K_EPOCH;
        data.putInt((int) (time & 0xFFFFFFFF));
        data.putInt(tzOffset);
    }

    public SetTimeTx(byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (data.get() == OPCODE_SET_TIME && packet.length == 9) {
            timestamp = ((((long) data.getInt()) & 0xFFFFFFFF) + Y2K_EPOCH) * 1000;
            tzOffset = data.getInt();
        } else {
            timestamp = -1;
        }
    }

    boolean isValid() {
        return timestamp != -1;
    }

    @Override
    public String toS() {
        return "Valid: " + isValid() + " " + super.toS() + " " + JoH.dateTimeText(timestamp);
    }

    // TODO move to general utility class
    // get the actual millis we are offset now
    private static long getActualDSTOffset(final TimeZone mTimeZone) {
        return (mTimeZone.useDaylightTime() && mTimeZone.inDaylightTime(new Date())) ? mTimeZone.getDSTSavings() : 0;
    }
}
