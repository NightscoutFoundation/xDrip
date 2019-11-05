package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.UUID;

public class TimeMessage extends BaseMessage {
    public byte[] getTimeMessage( long timestamp) {
        init(11);
        //timestamp = 587030940;
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);

        ByteBuffer b = ByteBuffer.allocate(2);
        b.putShort((short)c.get(Calendar.YEAR));
        byte[] yearArray = b.array();
        //put year
        putData(yearArray[1]);
        putData(yearArray[0]);

        putData((byte) c.get(Calendar.MONTH));
        putData((byte) c.get(Calendar.DAY_OF_MONTH));
        putData((byte) c.get(Calendar.HOUR_OF_DAY));
        putData((byte) c.get(Calendar.MINUTE));
        putData((byte) c.get(Calendar.SECOND));
        putData((byte) c.get(Calendar.DAY_OF_WEEK));
        putData((byte) 0x00); //fractions256
        putData((byte) 0x01); //adjust reason (manual time update)
        //constant
        putData((byte) 0x0C);
        return getBytes();
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_CURRENT_TIME;
    }
}
