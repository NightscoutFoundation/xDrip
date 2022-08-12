package com.eveningoutpost.dexdrip.glucosemeter;

import com.eveningoutpost.dexdrip.Models.JoH;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;

/**
 * Created by jamorham on 07/12/2016.
 */

public class CurrentTimeRx extends BluetoothCHelper {

    private ByteBuffer data = null;
    public boolean noClockAccess = false;
    public boolean sequenceNotReliable = false;
    public int year, month, day, hour, minute, second;
    public long time;
    public long timediff;
    public long timediff_calculated;

    public CurrentTimeRx() { }

    public CurrentTimeRx(byte[] packet) {

        if (packet.length >= 7) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

            year = unsignedBytesToInt(data.get(0), data.get(1));
            month = unsignedByteToInt(data.get(2));
            day = unsignedByteToInt(data.get(3));
            hour = unsignedByteToInt(data.get(4));
            minute = unsignedByteToInt(data.get(5));
            second = unsignedByteToInt(data.get(6));

            final Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, day, hour, minute, second);
            time = calendar.getTimeInMillis();
            timediff_calculated = System.currentTimeMillis() - time; // ms behind local clock
            timediff = -timediff_calculated; // use timediff, set to 0 otherwise
        }
    }

    public String toNiceString() {
        return "Difference: " + ((int) (timediff / 1000)) + " secs: " + JoH.dateTimeText(time);
    }

    public String toString() {
        return "timediff: " + timediff_calculated + "  year: " + year + " month: " + month + " day: " + day + " hour: " + hour + " minute: " + minute + " second: " + second + " ts: " + time;
    }
}

