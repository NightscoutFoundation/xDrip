package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.G5CollectionService;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */

public class TransmitterMessage {
    protected static final String TAG = G5CollectionService.TAG; // meh
    static final int INVALID_TIME = 0xFFFFFFFF;
    @Expose
    long postExecuteGuardTime = 50;

    @Expose
    public volatile byte[] byteSequence = null;
    public ByteBuffer data = null;


    public void setData() {
        byte[] newData;
    }

    static int getUnsignedShort(ByteBuffer data) {
        return ((data.get() & 0xff) + ((data.get() & 0xff) << 8));
    }

    static int getUnsignedByte(ByteBuffer data) {
        return ((data.get() & 0xff));
    }

    static int getUnixTime() {
        return (int) (JoH.tsl() / 1000);
    }

    void init(byte opcode, int length) {
        data = ByteBuffer.allocate(length);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
    }


    byte[] appendCRC() {
        data.put(CRC.calculate(getByteSequence(), 0, byteSequence.length - 2));
        return getByteSequence();
    }

    boolean checkCRC(byte[] data) {
        if ((data == null) || (data.length < 3)) return false;
        final byte[] crc = CRC.calculate(data, 0, data.length - 2);
        return crc[0] == data[data.length - 2] && crc[1] == data[data.length - 1];
    }

    byte[] getByteSequence() {
        byteSequence = data.array();
        return byteSequence;
    }

    long guardTime() {
        return postExecuteGuardTime;
    }
}
