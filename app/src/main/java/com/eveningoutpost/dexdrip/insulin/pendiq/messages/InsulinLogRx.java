package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class InsulinLogRx extends BaseMessage {

    @Expose
    public long timestamp = -1;
    @Expose
    public int length = -1;
    @Expose
    public int sequence = -1;
    @Expose
    public byte packetType = -1;
    @Expose
    public byte resultCode = -1;
    @Expose
    public byte reportType = -1;
    @Expose
    public byte unknownD = -1;
    @Expose
    public byte unknownE = -1;
    @Expose
    public double insulin = -1;
    @Expose
    public double adjustment = 0;
    @Expose
    public byte type = -1;


    // expecting packet without control characters
    public InsulinLogRx(byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        length = data.getInt();
        sequence = data.getShort();

        packetType = data.get(); // should be 4 == RESULT_PACKET
        resultCode = data.get(); // should be 1 == OK or 2 == NOT_OK
        reportType = data.get(); // should be 1 == INSULIN_CLASSIFIER

        timestamp = ((long) (data.getInt() - getTimeZoneOffsetSeconds())) * 1000;

        unknownD = data.get();
        unknownE = data.get();

        insulin = ((double) data.getShort()) / 100;
        adjustment = ((double) data.getShort()) / 100;
        type = data.get();

        // TODO type enum

    }

    public String getTimeStampString() {
        if (timestamp == -1) return null;
        return JoH.dateTimeText(timestamp);
    }

    public String getSummary() {
        return getTimeStampString() + " ::  " + insulin + "U";
    }

    @Override
    public String toS() {
        return super.toS() + " " + getTimeStampString();
    }
}



