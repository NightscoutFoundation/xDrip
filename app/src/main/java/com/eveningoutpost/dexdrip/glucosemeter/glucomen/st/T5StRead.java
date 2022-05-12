package com.eveningoutpost.dexdrip.glucosemeter.glucomen.st;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * GlucoMen nfc read message
 */

@Builder
@AllArgsConstructor
public class T5StRead extends BaseMessage {

    int offset;
    int length;

    public byte[] encode() {
        val b = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
        putUnsignedByte(b, HIGH_DATA_RATE_FLAG);
        putUnsignedByte(b, EXTENDED_RB);
        putUnsignedShort(b, offset);
        putUnsignedShort(b, Math.max(0, length - 1));
        return b.array();
    }

}