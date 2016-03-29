package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/28/16.
 */
public class TransmitterTimeTxMessage extends TransmitterMessage {
    byte opcode = 0x24;
    byte[] crc = CRC.calculate(opcode);

    public TransmitterTimeTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}
