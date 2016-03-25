package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class GlucoseTxMessage extends TransmitterMessage {
    int opcode = 0x30;
    byte[] crc = CRC.calculate(ByteBuffer.allocate(4).putInt(opcode).array());
}
