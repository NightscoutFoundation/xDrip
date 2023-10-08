package com.eveningoutpost.dexdrip.insulin.opennov.ll;

import static com.eveningoutpost.dexdrip.models.JoH.tolerantHexStringToByteArray;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.val;

/**
 * JamOrHam
 * OpenNov type 4 select helper
 */

@Builder
@AllArgsConstructor
public class T4Select extends BaseMessage {

    private static final int CLA = 0x00;
    private static final int INS_SL = 0xA4;
    private static final int BY_NAME = 0x04;
    private static final int FIRST_ONLY = 0x0C;

    private static final byte[] NDEF_TAG_APPLICATION_SELECT = tolerantHexStringToByteArray("D2760000850101");
    private static final byte[] CAPABILITY_CONTAINER_SELECT = tolerantHexStringToByteArray("E103");
    private static final byte[] NDEF_SELECT = tolerantHexStringToByteArray("E104");

    @Builder.Default
    private byte[] bytes = new byte[0];
    int p1;
    int p2;
    @Builder.Default
    int le = -1;

    public T4Select aSelect() {
        p1 = BY_NAME;
        le = 0;
        bytes = NDEF_TAG_APPLICATION_SELECT;
        return this;
    }

    public T4Select ccSelect() {
        p2 = FIRST_ONLY;
        bytes = CAPABILITY_CONTAINER_SELECT;
        return this;
    }

    public T4Select ndefSelect() {
        p2 = FIRST_ONLY;
        bytes = NDEF_SELECT;
        return this;
    }

    public byte[] encode() {
        val hasLe = (le != -1);
        val b = ByteBuffer.allocate(5 + bytes.length + (hasLe ? 1 : 0));
        putUnsignedByte(b, CLA);
        putUnsignedByte(b, INS_SL);
        putUnsignedByte(b, p1);
        putUnsignedByte(b, p2);
        putUnsignedByte(b, bytes.length);
        b.put(bytes);
        if (hasLe) {
            putUnsignedByte(b, le);
        }
        return b.array();
    }

}
