package com.eveningoutpost.dexdrip.glucosemeter;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.UUID;

/**
 * Created by jamorham on 06/12/2016.
 */

public class GlucoseReadingRx extends BluetoothCHelper {

    public ByteBuffer data = null;

    private int flags;
    public int sequence;
    public int year;
    public int month;
    public int day;
    public int hour;
    public int minute;
    public int second;
    public int offset;
    public float kgl;
    public float mol;
    public double mgdl;
    public long time;
    public int sampleType;
    public int sampleLocation;
    public String device;
    public boolean contextInfoFollows;

    public GlucoseReadingRx() {}

    public GlucoseReadingRx(byte[] packet) {
        this(packet, null);
    }

    public GlucoseReadingRx(byte[] packet, String device) {
        if (packet.length >= 14) {
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

            flags = data.get(0);
            final boolean timeOffsetPresent = (flags & 0x01) > 0;
            final boolean typeAndLocationPresent = (flags & 0x02) > 0;
            final boolean concentrationUnitKgL = (flags & 0x04) == 0;
            final boolean sensorStatusAnnunciationPresent = (flags & 0x08) > 0;
            contextInfoFollows = (flags & 0x10) > 0;

            sequence = data.getShort(1);
            year = data.getShort(3);
            month = data.get(5);
            day = data.get(6);
            hour = data.get(7);
            minute = data.get(8);
            second = data.get(9);

            int ptr = 10;
            if (timeOffsetPresent) {
                offset = data.getShort(ptr);
                ptr += 2;
            }

            if (concentrationUnitKgL) {
                kgl = getSfloat16(data.get(ptr), data.get(ptr + 1));
                mgdl = kgl * 100000;
            } else {
                mol = getSfloat16(data.get(ptr), data.get(ptr + 1));
                mgdl = mol * 1000 * Constants.MMOLL_TO_MGDL;
            }
            ptr += 2;

            if (typeAndLocationPresent) {
                final int typeAndLocation = data.get(ptr);
                sampleLocation = (typeAndLocation & 0xF0) >> 4;
                sampleType = (typeAndLocation & 0x0F);
                ptr++;
            }

            if (sensorStatusAnnunciationPresent) {
                final int status = data.get(ptr);

            }

            final Calendar calendar = Calendar.getInstance();
            calendar.set(year, month - 1, day, hour, minute, second);
            time = calendar.getTimeInMillis();

            this.device = device;
        }
    }

    public String toString() {
        return "Glucose data: mg/dl: " + mgdl + "  mol/l: " + mol + "  kg/l: " + kgl
                + "  seq:" + sequence + " sampleType: " + sampleType + "  sampleLocation: " + sampleLocation + "  time: " + hour + ":" + minute + ":" + second
                + "  " + day + "-" + month + "-" + year + " timeoffset: " + offset + " timestamp: " + time + " from: " + device + (contextInfoFollows ? "  CONTEXT FOLLOWS" : "");
    }

    public long offsetMs() {
        return (offset * 60000);
    }

    public UUID getUuid() {
        data.rewind();
        final byte[] barr = new byte[data.remaining()];
        data.get(barr);
        return UUID.nameUUIDFromBytes(barr);
    }

    public double asKetone() {
        return mgdl / 10d;
    }

}
