package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import lombok.Getter;

/**
 * Created by jamorham on 25/11/2016.
 */


public class VersionRequest2RxMessage extends BaseMessage {

    public static final byte opcode = 0x53;

    public int status;
    public int typicalSensorDays;
    public int featureBits;
    public int warmupSeconds;
    @Getter
    public boolean type2;


    public VersionRequest2RxMessage(byte[] packet) {
        type2 = packet.length == 9;
        if (packet.length >= 9) {
            // TODO check CRC??
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (data.get() == opcode) {
                status = data.get();
                typicalSensorDays = getUnsignedByte(data);
                featureBits = getUnsignedShort(data);
                warmupSeconds = getUnsignedShort(data); // only valid in type 2
                // 12 more bytes of unknown data
                // crc
            }
        }
    }

    public String toString() {
        return String.format(Locale.US, "Status: %s / Typical Days: %d / : Feature Bits %d",
                TransmitterStatus.getBatteryLevel(status).toString(), typicalSensorDays, featureBits);
    }

}