package com.eveningoutpost.dexdrip.insulin.opennov.mt;


import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * OpenNov Event Report
 */

public class EventReport extends BaseMessage {

    public static final int MDC_NOTI_CONFIG = 3356;
    public static final int MDC_NOTI_SEGMENT_DATA = 3361;

    public int handle = -1;
    public int instance = -1;
    public long index = -1;
    public long count = -1;
    public final List<InsulinDose> doses = new ArrayList<>();
    public Configuration configuration;

    public static EventReport parse(final ByteBuffer buffer) {
        val handle = getUnsignedShort(buffer);
        val relativeTime = getUnsignedInt(buffer);
        val eventType = getUnsignedShort(buffer);
        val len = getUnsignedShort(buffer);
        log("EventReport: handle: " + handle + " rt: " + relativeTime + " " + eventType);
        val er = new EventReport();
        er.handle = handle;

        switch (eventType) {
            case MDC_NOTI_SEGMENT_DATA:
                er.instance = getUnsignedShort(buffer);
                er.index = getUnsignedInt(buffer);
                er.count = getUnsignedInt(buffer);
                val status = getUnsignedShort(buffer);
                val bcount = getUnsignedShort(buffer);

                if (d) log("EventReport: segment data: " + len + " instance: " + er.instance + " index: " + er.index + " count: " + er.count + " status: " + Integer.toHexString(status) + " bcount: " + bcount);

                for (int i = 0; i < er.count; i++) {
                    er.doses.add(InsulinDose.parse(buffer, relativeTime));
                }
                return er;

            case MDC_NOTI_CONFIG:
                er.configuration = Configuration.parse(buffer);
                return er;
            default:
                log("Unknown event report type: " + eventType);
                break;
        }

        return null;
    }


}
