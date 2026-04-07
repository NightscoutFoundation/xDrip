package lwld.glucose.profile.packet;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * JamOrHam
 * <p>
 * Packet creation utilities
 */

public class Packet {

    public static byte[] setCgmSessionStartTime(
            long utcMillis) {

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(utcMillis);

        int year = cal.get(Calendar.YEAR);

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Little-endian year
        out.write(year & 0xFF);           // year % 256
        out.write((year >> 8) & 0xFF);    // year / 256

        out.write(cal.get(Calendar.MONTH) + 1); // Java months are 0-based
        out.write(cal.get(Calendar.DAY_OF_MONTH));
        out.write(cal.get(Calendar.HOUR_OF_DAY));
        out.write(cal.get(Calendar.MINUTE));
        out.write(cal.get(Calendar.SECOND));

        // TODO always UTC at the moment
        out.write(0);
        out.write(0);
        // out.write(timeZone.value);
        // out.write(dstOffset.value);

        return CRC.appendCrc(out.toByteArray());
    }
}
