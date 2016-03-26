package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jcostik1 on 3/26/16.
 */
public class SensorTxMessage extends TransmitterMessage{
    int opcode = 0x2e;
    byte[] crc = CRC.calculate(ByteBuffer.allocate(4).putInt(opcode).array());

    public SensorTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put((byte)opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}
