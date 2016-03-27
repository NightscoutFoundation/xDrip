package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by jcostik1 on 3/26/16.
 */
public class BatteryTxMessage extends TransmitterMessage{
    int opcode = 0x22;
    byte[] crc = CRC.calculate(ByteBuffer.allocate(4).putInt(opcode).array());

    public BatteryTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put((byte)opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}
