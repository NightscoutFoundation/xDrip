package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov specification element
 */

public class Specification extends BaseMessage {

    private static final int SERIAL_NUMBER = 1;
    private static final int PART_NUMBER = 2;
    private static final int HW_VERSION = 3;
    private static final int SW_VERSION = 4;

    @Getter
    private String serial;
    @Getter
    private String partNumber;
    @Getter
    private String softWareRevision;
    @Getter
    private String hardWareRevision;

    public static Specification parse(final ByteBuffer buffer) {
        val r = new Specification();
        val scount = getUnsignedShort(buffer);
        val ssize = getUnsignedShort(buffer);
        log("Specification: " + scount + " size:" + ssize);
        for (int i = 0; i < scount; i++) {
            val specType = getUnsignedShort(buffer);
            val componentId = getUnsignedShort(buffer);
            val s = getIndexedString(buffer);

            log("spectype: " + specType + " component: " + componentId + " " + s);

            switch (specType) {
                case SERIAL_NUMBER:
                    r.serial = s;
                    break;
                case PART_NUMBER:
                    r.partNumber = s;
                    break;
                case HW_VERSION:
                    r.hardWareRevision = s;
                    break;
                case SW_VERSION:
                    r.softWareRevision = s;
                    break;

                default:
                    log("Unknown specification type: " + specType);
            }
        }
        return r;
    }

    public static Specification parse(final byte[] bytes) {
        return parse(ByteBuffer.wrap(bytes));
    }
}
