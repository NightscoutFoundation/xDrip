package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov APDU Type
 */

@Builder
@AllArgsConstructor
public class Apdu extends BaseMessage {

    int at;
    public ApduType apduType;
    public int choiceLength;
    byte[] choicePayload;

    private Apdu() {
    }

    public byte[] encode() {
        val b = ByteBuffer.allocate((choicePayload != null ? choicePayload.length : 0) + 4);
        putUnsignedShort(b, at);
        putIndexedBytes(b, choicePayload);
        return b.array();
    }

    public static Apdu parse(final ByteBuffer buffer) {
        val apdu = new Apdu();
        apdu.at = getUnsignedShort(buffer);
        apdu.apduType = ApduType.findByValue(apdu.at);
        if (apdu.apduType == null) {
            error("Cannot parse apdu type: " + apdu.at);
            return null;
        }
        apdu.choiceLength = getUnsignedShort(buffer);
        return apdu;
    }

    public boolean isError() {
        return apduType == ApduType.AbrtApdu;
    }

    public boolean wantsRelease() {
        return apduType == ApduType.RlrqApdu;
    }

    @AllArgsConstructor
    public enum ApduType {
        AarqApdu(0xE200),
        AareApdu(0xE300),
        RlrqApdu(0xE400),
        RlreApdu(0xE500),
        AbrtApdu(0xE600),
        PrstApdu(0xE700);

        @Getter
        int value;

        public static ApduType findByValue(final int v) {
            for (val i : ApduType.values()) {
                if (i.value == v) return i;
            }
            return null;
        }
    }
}
