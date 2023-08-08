package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class StatusRx extends BaseMessage{

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
    public byte usb = -1;
    @Expose
    public byte pc = -1;
    @Expose
    public String pin = null;
    @Expose
    public byte ble = -1;
    @Expose
    public byte sound = -1;
    @Expose
    public byte volume = -1;
    @Expose
    public byte timezone = -1;
    @Expose
    public String locale = null;
    @Expose
    public long lastDate = -1;
    @Expose
    public double lastAmount = -1;
    @Expose
    public double lastAdjust = -1;
    @Expose
    public byte cartType = -1;
    @Expose
    public double remaining = -1;
    @Expose
    public byte itype = -1;
    @Expose
    public byte timeblocks = -1;
    @Expose
    public byte currentTimeblock = -1;
    @Expose
    public short unknownD = -1;
    @Expose
    public short unknownE = -1;
    @Expose
    public short unknownF = -1;


    // expecting packet without control characters
    public StatusRx(byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        length = data.getInt();
        sequence = data.getShort();

        packetType = data.get(); // should be 4 == RESULT_PACKET
        resultCode = data.get(); // should be 1 == OK or 2 == NOT_OK
        reportType = data.get(); // should be 1 == STATUS_CLASSIFIER

        usb = data.get();
        pc = data.get();

        final byte[] pinBytes = new byte[4];
        data.get(pinBytes,0,4);
        pin = new String(pinBytes, Charset.forName("ASCII"));

        ble = data.get();
        sound = data.get();
        volume = data.get();
        timezone = data.get();

        final byte[] localeBytes = new byte[2];
        data.get(localeBytes,0, 2);
        locale = new String(localeBytes, Charset.forName("ASCII"));

        // TODO Timezone correction & save from this record also?
        lastDate = ((long)data.getInt()) * 1000;
        lastAmount = ((double)data.getShort()/100);
        lastAdjust = ((double)data.getShort()/100);
        // TODO integrate display of these items
        cartType = data.get();
        remaining = ((double)data.getShort()/100);

        itype = data.get();
        timeblocks = data.get();
        currentTimeblock = data.get();
        unknownD = data.getShort();
        unknownE = data.getShort();
        unknownF = data.getShort();

    }

    public String getLastDateString() {
        return JoH.dateTimeText(lastDate);
    }

}
