package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by jamorham on 25/11/2016.
 */


public class VersionRequest2RxMessage extends BaseMessage {

    public static final byte opcode = 0x53;

    public int status;
    public int typicalSensorDays;
    public int featureBits;


    public VersionRequest2RxMessage(byte[] packet) {
        if (packet.length >= 18) {
            // TODO check CRC??
            data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
            if (data.get() == opcode) {
                status = data.get();
                typicalSensorDays = getUnsignedByte(data);
                featureBits = getUnsignedShort(data);
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