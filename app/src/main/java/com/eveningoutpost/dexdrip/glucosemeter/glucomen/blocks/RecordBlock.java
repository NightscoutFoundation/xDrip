package com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks;

import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;

import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import java.nio.ByteBuffer;
import java.util.Calendar;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

/**
 * JamOrHam
 * RecordBlock handler
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class RecordBlock extends MyByteBuffer {

    int zeroStatus;
    int year;
    int month;
    int day;
    int hour;
    int min;
    public double mmolValue;
    int checkDigit;
    int computedCheckDigit = -1;
    public boolean mgdl;
    boolean control;

    public static RecordBlock parse(final byte[] bytes) {

        if (bytes == null) return null;

        val b = ByteBuffer.wrap(bytes);
        val g = new RecordBlock();

        g.zeroStatus = getUnsignedByte(b);
        val bs1 = getBits(b, 1);
        g.mgdl = bs1.get(7);
        g.control = bs1.get(6);
        g.year = 2000 + bs1.getInt(5, 0);
        val bs2 = getBits(b, 1);

        g.month = bs2.getInt(4, 7);
        // TODO flag bits in lower nibble
        val bs3 = getBits(b, 2);
        g.day = bs3.getInt(0, 4);
        g.hour = bs3.getInt(5, 9);
        g.min = bs3.getInt(10, 15);

        val bs4 = getBits(b, 2);
        // TODO unknown so far

        g.mmolValue = getUnsignedByte(b) / 10d;     // TODO WARNING MMOL ONLY
        g.checkDigit = getUnsignedByte(b);

        b.rewind();
        b.get();
        g.computedCheckDigit = 0;
        for (int i = 1; i < (bytes.length - 1); i++) {
            int mult;
            if (i == 5) {
                mult = 10;
            } else {
                mult = 1;
            }
            g.computedCheckDigit = (g.computedCheckDigit + (getUnsignedByte(b) * mult));
        }
        g.computedCheckDigit &= 0xFF;
        return g;
    }

    public boolean isOkay() {
        return computedCheckDigit == checkDigit
                && month > 0
                && month < 13
                && day > 0
                && day < 32
                && hour < 24
                && min < 60
                && mmolValue > 0.0d;
    }

    public boolean isUsable() {
        return isOkay()
                && !control
                && year > 2021
                && mmolValue > 1.0d
                && mmolValue < 31d;
    }

    public boolean isUsableKetone() {
        return isOkay()
                && !control
                && year > 2021
                && mmolValue < 31d;
    }

    public double getMgDlValue() {
        return roundDouble(Unitized.mgdlConvert(mmolValue), 2);
    }

    public long getTimestamp() {
        val ucalendar = Calendar.getInstance();
        ucalendar.set(year, month - 1, day, hour, min, 0);
        return ucalendar.getTime().getTime();
    }

}
