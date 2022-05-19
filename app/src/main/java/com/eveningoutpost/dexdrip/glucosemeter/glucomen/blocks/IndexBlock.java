package com.eveningoutpost.dexdrip.glucosemeter.glucomen.blocks;

import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import java.nio.ByteBuffer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.val;

/**
 * JamOrHam
 * IndexBlock handler
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class IndexBlock extends MyByteBuffer {

    int status = -1;
    public int glucoseRecords = -1;
    public int ketoneRecords = -1;
    int memoryState = -1;

    public boolean glucoseMemoryFull() {
        return (memoryState & 0x0A) != 0;
    }

    public boolean ketoneMemoryFull() {
        return (memoryState & 0xA0) != 0;
    }

    public static IndexBlock parse(final byte[] bytes) {
        if (bytes == null) return null;
        val b = ByteBuffer.wrap(bytes);
        val i = new IndexBlock();
        i.status = getUnsignedByte(b);
        i.glucoseRecords = getUnsignedShort(b);
        i.ketoneRecords = getUnsignedByte(b);
        i.memoryState = getUnsignedByte(b);
        return i;
    }
}
