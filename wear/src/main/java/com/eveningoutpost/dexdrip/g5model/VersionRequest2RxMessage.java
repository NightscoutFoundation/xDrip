package com.eveningoutpost.dexdrip.g5model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import lombok.Getter;
import lombok.val;

/**
 * Created by jamorham on 25/11/2016.
 */


public class VersionRequest2RxMessage extends BaseMessage {

    public static final byte opcode = 0x53;
    public static final byte opcode2 = 0x52;

    public int status;
    public int typicalSensorDays;
    public int featureBits;
    public long lifeSeconds;
    public int warmupSeconds;
    public int version1;
    public int version2;
    @Getter
    public boolean type2;


    public VersionRequest2RxMessage(byte[] packet) {
        type2 = packet.length == 9;
        if (packet.length >= 9) {
            // TODO check CRC??
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            val op = data.get();
            status = data.get();
            if (op == opcode) {
                typicalSensorDays = getUnsignedByte(data);
                featureBits = getUnsignedShort(data);
                warmupSeconds = getUnsignedShort(data); // only valid in type 2
                // 12 more bytes of unknown data
                // crc
            }
            if (op == opcode2) {
                lifeSeconds = getUnsignedInt(data);
                warmupSeconds = getUnsignedShort(data);
                version1 = (int) getUnsignedInt(data);
                version2 = getUnsignedByte(data);
                typicalSensorDays =  (int) Math.min(getUnsignedShort(data), lifeSeconds / 86400);
            }
        }
    }

    public String toString() {
        return String.format(Locale.US, "Status: %s / Typical Days: %d / : Feature Bits %d",
                TransmitterStatus.getBatteryLevel(status).toString(), typicalSensorDays, featureBits);
    }

}