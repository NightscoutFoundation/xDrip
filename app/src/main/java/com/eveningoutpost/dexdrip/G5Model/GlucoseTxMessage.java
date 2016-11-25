package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by jamorham on 25/11/2016.
 */

public class GlucoseTxMessage extends TransmitterMessage {

    byte opcode = 0x30;
    byte[] crc = CRC.calculate(opcode);

    public GlucoseTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
    }
}

