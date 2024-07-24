package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov segment entry
 */

public class SegmentEntry extends BaseMessage {

    Stype stype;
    int classId;
    int otype;
    long metricType;
    int handle;
    int amcount;
    int amlen;
    int val1;
    int val2;
    int partition;
    byte[] bytes;

    @AllArgsConstructor
    public enum Stype {
        MDC_ATTR_NU_VAL_OBS_SIMP(2646),
        MDC_ATTR_ENUM_OBS_VAL_BASIC_BIT_STR(2662),
        ;
        int value;

        public static Stype findByValue(final int v) {
            for (val i : Stype.values()) {
                if (i.value == v) return i;
            }
            return null;
        }
    }

    public static SegmentEntry parse(final ByteBuffer buffer) {
        val se = new SegmentEntry();
        se.classId = getUnsignedShort(buffer);
        se.metricType = getUnsignedShort(buffer);
        se.otype = getUnsignedShort(buffer);
        se.handle = getUnsignedShort(buffer);

        se.amcount = getUnsignedShort(buffer);
        se.amlen = getUnsignedShort(buffer);

        if (se.amlen == 4) {
            se.val1 = getUnsignedShort(buffer); // type
            se.val2 = getUnsignedShort(buffer); // len
            se.stype = Stype.findByValue(se.val1);
        } else {
            log("unhandled entry length of " + se.amlen);
            val x = new byte[se.amlen];
            buffer.get(x, 0, se.amlen);
            se.bytes = x;
        }

        if (d)
            log("Segment Entry: " + se.stype + " (" + se.classId + ") part:" + se.partition + " otype: " + se.otype + " metric: " + se.metricType + " amcount: " + se.amcount + " amlen: " + se.amlen + " v1: " + se.val1 + " v2: " + se.val2);
        return se;
    }

}



