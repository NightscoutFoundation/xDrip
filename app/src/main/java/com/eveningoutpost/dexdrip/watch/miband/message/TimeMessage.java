package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.util.Calendar;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations.fromUint16;
import static com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations.fromUint8;

public class TimeMessage extends BaseMessage {
    public byte[] getTimeMessage( long timestamp) {
        init(11);
        //timestamp = 587030940;
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        putData(calendarToRawBytes(c));
        putData((byte) 0x01); //adjust reason (manual time update)
        //constant
        putData((byte) 0x0C);
        return getBytes();
    }

    public static byte[] calendarToRawBytes(Calendar timestamp) {
        // MiBand2:
        // year,year,month,dayofmonth,hour,minute,second,dayofweek,0,0,tz

        byte[] year = fromUint16(timestamp.get(Calendar.YEAR));
        return new byte[] {
                year[0],
                year[1],
                fromUint8(timestamp.get(Calendar.MONTH) + 1),
                fromUint8(timestamp.get(Calendar.DATE)),
                fromUint8(timestamp.get(Calendar.HOUR_OF_DAY)),
                fromUint8(timestamp.get(Calendar.MINUTE)),
                fromUint8(timestamp.get(Calendar.SECOND)),
                dayOfWeekToRawBytes(timestamp),
                0, // fractions256 (not set)
                // 0 (DST offset?) Mi2
                // k (tz) Mi2
        };
    }


    private static byte dayOfWeekToRawBytes(Calendar cal) {
        int calValue = cal.get(Calendar.DAY_OF_WEEK);
        switch (calValue) {
            case Calendar.SUNDAY:
                return 7;
            default:
                return (byte) (calValue - 1);
        }
    }


    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHAR_CURRENT_TIME;
    }
}
