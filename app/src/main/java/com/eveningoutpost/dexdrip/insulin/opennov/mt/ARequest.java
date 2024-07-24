package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import static com.eveningoutpost.dexdrip.insulin.opennov.mt.ApoepElement.APOEP;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov A Request
 */

public class ARequest extends BaseMessage {

    @Getter
    private int protocol;
    @Getter
    private long version;
    @Getter
    private int elements;

    @Getter
    private ApoepElement apoep;

    public boolean valid() {
        return protocol == APOEP
                && apoep != null
                && apoep.systemId.length == 8;
    }

    public static ARequest parse(final ByteBuffer buffer) {
        val ar = new ARequest();
        ar.version = getUnsignedInt(buffer);
        ar.elements = getUnsignedShort(buffer);
        val len = getUnsignedShort(buffer);

        for (int i = 0; i < ar.elements; i++) {
            ar.protocol = getUnsignedShort(buffer);
            val bytes = getIndexedBytes(buffer);
            if (d) log("AR Protocol: " + ar.protocol + " " + HexDump.dumpHexString(bytes));
            if (ar.protocol == APOEP) {
                ar.apoep = ApoepElement.parse(bytes);
                if (d) log(ar.apoep.toJson());
            }
        }
        return ar;
    }
}
