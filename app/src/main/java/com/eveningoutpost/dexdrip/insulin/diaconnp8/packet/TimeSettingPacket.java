package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import org.joda.time.DateTime;

import java.nio.ByteBuffer;

public class TimeSettingPacket extends DiaconnP8Packet {

    public static final byte MSG_TYPE = 0x0C;

    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        DateTime nowDate = DateTime.now();

        buffer.put((byte)((nowDate.getYear() - 2000) & 0xff));
        buffer.put((byte)(nowDate.getMonthOfYear() & 0xff));
        buffer.put((byte)(nowDate.getDayOfMonth() & 0xff));
        buffer.put((byte)(nowDate.getHourOfDay() & 0xff));
        buffer.put((byte)(nowDate.getMinuteOfHour() & 0xff));
        buffer.put((byte)(nowDate.getSecondOfMinute() & 0xff));
        return suffixEncode(buffer);
    }
}
