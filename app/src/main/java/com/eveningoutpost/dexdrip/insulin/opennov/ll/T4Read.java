package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov type 4 read
 */

@Builder
@AllArgsConstructor
public class T4Read extends MyByteBuffer {

    private static final int CLA = 0x00;
    private static final int INS_RB = 0xB0;

    int offset;
    int length;

    public byte[] encode() {
        val b = ByteBuffer.allocate(5);
        putUnsignedByte(b, CLA);
        putUnsignedByte(b, INS_RB);
        putUnsignedShort(b, offset);
        putUnsignedByte(b, length);
        return b.array();
    }

    public List<byte[]> encodeForMtu(final int mtu) {
        val blist = new LinkedList<byte[]>();

        int thisLength = length;
        int thisOffset = offset;

        while (thisLength > 0) {
            val thisRead = Math.min(thisLength, mtu);
            blist.add(T4Read.builder().offset(thisOffset).length(thisRead)
                    .build().encode());
            thisOffset += thisRead;
            thisLength -= thisRead;
        }
        return blist;
    }

}
