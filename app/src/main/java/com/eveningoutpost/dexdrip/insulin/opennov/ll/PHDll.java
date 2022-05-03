package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

import java.nio.ByteBuffer;
import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov link layer codec
 */

@Builder
@AllArgsConstructor
public class PHDll extends MyByteBuffer {

    private static final int MB = 1 << 7;
    private static final int ME = 1 << 6;
    private static final int CF = 1 << 5;
    private static final int SR = 1 << 4;
    private static final int IL = 1 << 3;

    private static final int WELL_KNOWN = 1;

    private static final byte[] typeID = "PHD".getBytes();
    private static final int defaultOpcode = 0xD1;

    @Builder.Default
    int opcode = -1;
    @Builder.Default
    int typeLen = -1;
    @Builder.Default
    int payloadLen = -1;
    @Builder.Default
    int idHeaderLen = -1;
    @Getter
    @Builder.Default
    int seq = -1;
    @Builder.Default
    int chk = 0;

    byte[] idHeader;
    @Getter
    byte[] inner;

    public PHDll() {
    }

    public boolean checkBit(final int bit) {
        return (((chk >> bit) & 1) != 0);
    }

    public void setCheckBit(final int bit) {
        chk |= (1 << bit);
    }

    public byte[] encode() {
        val ilen = inner != null ? inner.length : 0;
        val idLen = idHeader != null ? idHeader.length : 0;
        val hasId = idLen > 0;
        val b = ByteBuffer.allocate(ilen + 7);
        putUnsignedByte(b, MB | ME | SR | (hasId ? IL : 0) | WELL_KNOWN);
        putUnsignedByte(b, typeID.length);
        putUnsignedByte(b, ilen + 1);
        if (hasId) {
            b.put(idHeader);
        }
        b.put(typeID);
        putUnsignedByte(b, (seq & 0x0F) | 0x80 | chk);
        if (ilen > 0) {
            b.put(inner);
        }
        return b.array();
    }

    public static PHDll parse(final byte[] bytes) {
        if (bytes == null) return null;
        val b = ByteBuffer.wrap(bytes);
        val phd = new PHDll();
        phd.opcode = getUnsignedByte(b);
        val hasID = (phd.opcode & IL) != 0;
        phd.typeLen = getUnsignedByte(b);
        phd.payloadLen = getUnsignedByte(b) - 1;
        if (hasID) {
            phd.idHeaderLen = getUnsignedByte(b);
        }
        val pprotoId = new byte[3];
        b.get(pprotoId);
        if (!Arrays.equals(pprotoId, typeID)) {
            return null;
        }
        if (hasID) {
            phd.idHeader = new byte[phd.idHeaderLen];
            b.get(phd.idHeader);
        }
        phd.chk = getUnsignedByte(b);
        phd.seq = phd.chk & 0x0F;
        phd.inner = new byte[phd.payloadLen];
        b.get(phd.inner);
        return phd;
    }

}
