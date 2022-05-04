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
 * OpenNov type 4 update
 */

@Builder
@AllArgsConstructor
public class T4Update extends MyByteBuffer {

    private static final int CLA = 0x00;
    private static final int UPDATE_COMMAND = 0xD6;

    int offset;
    byte[] bytes;

    boolean firstFragment;
    boolean lastFragment;

    public byte[] encode() {
        if (bytes == null) bytes = new byte[0];
        val isFragment = offset > 0;
        val hasDlen = offset == 0 || firstFragment;
        val len = lastFragment ? 7 : (bytes.length + (hasDlen ? 7 : 5));
        val b = ByteBuffer.allocate(len);
        putUnsignedByte(b, CLA);
        putUnsignedByte(b, UPDATE_COMMAND);
        putUnsignedShort(b, isFragment ? (offset + 2) : 0);
        putUnsignedByte(b, lastFragment ? 2 : bytes.length + (hasDlen ? 2 : 0));
        if (hasDlen) {
            putUnsignedShort(b, firstFragment ? 0 : bytes.length);
        }
        if (!lastFragment) {
            b.put(bytes);
        }
        return b.array();
    }

    public List<byte[]> encodeForMtu(final int mtu) {
        if (bytes == null) return null;
        val blist = new LinkedList<byte[]>();
        int offset = 0;
        val b = ByteBuffer.wrap(bytes);
        while (b.remaining() > 0) {
            val chunkSize = Math.min(b.remaining(), mtu - 7);
            val pBytes = new byte[chunkSize];
            b.get(pBytes);
            val chunk = builder().bytes(pBytes)
                    .offset(offset)
                    .firstFragment(offset == 0 && b.remaining() > 0)
                    .build().encode();
            if (chunk != null) {
                blist.add(chunk);
            }
            offset += chunkSize;
        }
        if (blist.size() > 1) {
            blist.addLast(builder().bytes(bytes).lastFragment(true)
                    .build().encode());
            // fragmented
        } else if (blist.size() == 0) { //  empty
            blist.addFirst(builder().build().encode());
        }
        return blist;
    }


}
