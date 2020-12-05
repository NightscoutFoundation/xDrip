package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.cgm.medtrum.Const;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.DEX_RAW_SCALE;

/**
 * jamorham
 *
 * Base class for Medtrum CGM messages
 */

public class BaseMessage {

    protected static final String TAG = "Medtrum-Msg";

    public ByteBuffer data = null;
    protected byte[] byteSequence = null;


    void init(byte opcode, int length, boolean lengthHeader) {
        data = ByteBuffer.allocate(length + (lengthHeader ? 2 : 0));
        data.order(ByteOrder.LITTLE_ENDIAN);
        if (lengthHeader) {
            data.putShort((short) length);
        }
        data.put(opcode);
    }

    void wrap(byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
    }

    int getUnsignedByte() {
        return data.get() & 0xff;
    }

    int getUnsignedShort() {
        return data.getShort() & 0xffff;
    }

    long getUnsignedInt() {
        return data.getInt() & 0xffffffffL;
    }

    public boolean exceedsMTU() {
        return data.array().length > Const.BLUETOOTH_MTU;
    }

    public double performCalculation(int sensorRaw, int calibrationSlope, int calibrationIntercept) {
        return (double) sensorRaw * 1000d / (double) calibrationSlope + ((double) calibrationIntercept);
    }

    public int getSensorRawEmulateDex(int sensorRaw) {
        if (sensorRaw > 0) {
            return sensorRaw * DEX_RAW_SCALE;
        } else {
            return 0;
        }
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }

    public byte[] getByteSequence() {
        if (byteSequence == null) {
            byteSequence = data.array(); // finalize
        }
        return byteSequence;
    }

    static boolean isBitSet(final int value, final int position) {
        return (value >> position & 1) == 1;
    }

}
