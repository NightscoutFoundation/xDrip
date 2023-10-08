package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov segment info map
 */

public class SegmentInfoMap extends BaseMessage {

    private static final int MDC_ATTR_NU_VAL_OBS_SIMP = 2646;
    private static final int MDC_ATTR_ENUM_OBS_VAL_BASIC_BIT_STR = 2662;

    private int bits;
    private int acount;
    private int alength;

    @Getter
    private final List<SegmentEntry> items = new ArrayList<>();

    public boolean isTypical() {
        return items.size() == 3
                && items.get(0).otype == 13313
                && items.get(0).metricType == 130
                && items.get(0).val1 == MDC_ATTR_NU_VAL_OBS_SIMP
                && items.get(1).otype == 13314
                && items.get(1).metricType == 130
                && items.get(1).val1 == MDC_ATTR_ENUM_OBS_VAL_BASIC_BIT_STR
                && items.get(2).otype == 61440
                && items.get(2).metricType == 130
                && items.get(2).val1 == MDC_ATTR_ENUM_OBS_VAL_BASIC_BIT_STR;
    }

    public static SegmentInfoMap parse(final ByteBuffer buffer) {
        val r = new SegmentInfoMap();
        r.bits = getUnsignedShort(buffer);
        r.acount = getUnsignedShort(buffer);
        r.alength = getUnsignedShort(buffer);
        log("SegmentInfoMap Ecount: " + r.acount);
        for (int i = 0; i < r.acount; i++) {
            val a = SegmentEntry.parse(buffer);
            r.items.add(a);
        }
        return r;
    }

    public static SegmentInfoMap parse(final byte[] buffer) {
        return parse(ByteBuffer.wrap(buffer));
    }
}
