package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jcostik1 on 3/26/16.
 */
public class SensorRxMessage extends TransmitterMessage {
    byte opcode = 0x2f;
    public TransmitterStatus status;
    public int timestamp;
    public int unfiltered;
    public int filtered;

    public SensorRxMessage(byte[] packet) {
        if (packet.length >= 14) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                status = TransmitterStatus.getBatteryLevel(data.get(1));
                timestamp = data.getInt(2);

                unfiltered = data.getInt(6);
                filtered = data.getInt(10);
            }
        }
    }
}
