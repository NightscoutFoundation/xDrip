package com.eveningoutpost.dexdrip.insulin.opennov.mt;


import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov segment info
 */

public class SegmentInfo extends BaseMessage {

    @Getter
    private int instnum;
    @Getter
    private long usage = -1;
    private int acount;
    private int alength;

    @Getter
    @Setter
    private boolean processed = false;

    @Getter
    private SegmentInfoMap map;

    private final List<Attribute> items = new ArrayList<>();

    public boolean isTypical() {
        return usage >= 0
                && map != null && map.isTypical();
    }

    public static SegmentInfo parse(final ByteBuffer buffer) {
        val r = new SegmentInfo();
        r.instnum = getUnsignedShort(buffer);
        r.acount = getUnsignedShort(buffer);
        r.alength = getUnsignedShort(buffer);
        log("Acount: " + r.acount);
        for (int i = 0; i < r.acount; i++) {
            val a = Attribute.parse(buffer);

            switch (a.atype) {
                case MDC_ATTR_PM_SEG_MAP:
                    r.map = SegmentInfoMap.parse(a.bytes);
                    break;
                case MDC_ATTR_SEG_USAGE_CNT:
                    r.usage = a.ivalue;
                    break;
            }

            r.items.add(a);
        }
        return r;
    }

}
