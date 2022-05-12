package com.eveningoutpost.dexdrip.glucosemeter.caresens;

// jamorham

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

import lombok.Getter;

public class TimeTx {

    @Getter
    byte[] byteSequence;
    @Getter
    private ByteBuffer data;


    public TimeTx(long timeStamp) {

        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeStamp);

        data = ByteBuffer.allocate(11);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put((byte) 0xC0);
        data.put((byte) 0x03);
        data.put((byte) 0x01);
        data.put((byte) 0x00);
        data.putShort((short) cal.get(Calendar.YEAR));
        data.put((byte) (cal.get(Calendar.MONTH) + 1));
        data.put((byte) cal.get(Calendar.DAY_OF_MONTH));
        data.put((byte) cal.get(Calendar.HOUR_OF_DAY));
        data.put((byte) cal.get(Calendar.MINUTE));
        data.put((byte) cal.get(Calendar.SECOND));

        byteSequence = data.array();

    }

}
