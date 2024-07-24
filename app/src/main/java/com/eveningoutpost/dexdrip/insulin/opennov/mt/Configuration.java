package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Configuration message
 */

public class Configuration extends BaseMessage {

    private static final int MDC_DIM_INTL_UNIT = 5472;

    @Getter
    private int id = -1;
    @Getter
    private int handle = -1;
    @Getter
    private long numberOfSegments = -1;
    @Getter
    private long totalStoredEntries = -1;
    @Getter
    private long totalStorageCapacity = -1;
    @Getter
    private long unitCode = -1;

    @Getter
    private final List<ValueMap> valueMaps = new LinkedList<>();


    public boolean isAsExpected() {
        return unitCode == MDC_DIM_INTL_UNIT
                && numberOfSegments >= 0
                && totalStoredEntries >= 0;
    }


    public static Configuration parse(final ByteBuffer buffer) {
        val id = getUnsignedShort(buffer);
        val count = getUnsignedShort(buffer);
        val len = getUnsignedShort(buffer);

        Configuration configuration = null;
        log("Configuration: " + id + " c:" + count + " len:" + len);
        for (int i = 0; i < count; i++) {

            val cls = getUnsignedShort(buffer);
            val handle = getUnsignedShort(buffer);
            val acount = getUnsignedShort(buffer);
            val alen = getUnsignedShort(buffer);
            for (int j = 0; j < acount; j++) {

                if (configuration == null) {
                    configuration = new Configuration();
                    configuration.id = id;
                    configuration.handle = handle;
                }

                val a = Attribute.parse(buffer);
                if (d) log("Class: " + cls + " " + a.toJson());

                if (a.atype != null) {
                    switch (a.atype) {
                        case MDC_ATTR_NUM_SEG:
                            configuration.numberOfSegments = a.ivalue;
                            break;

                        case MDC_ATTR_METRIC_STORE_USAGE_CNT:
                            configuration.totalStoredEntries = a.ivalue;
                            break;

                        case MDC_ATTR_UNIT_CODE:
                            configuration.unitCode = a.ivalue;
                            break;

                        case MDC_ATTR_METRIC_STORE_CAPAC_CNT:
                            configuration.totalStorageCapacity = a.ivalue;
                            break;

                        case MDC_ATTR_ATTRIBUTE_VAL_MAP:
                            configuration.valueMaps.add(ValueMap.parse(a.bytes));
                            break;
                    }
                }
            }
        }
        return configuration;
    }

    public static Configuration parse(final byte[] bytes) {
        return parse(ByteBuffer.wrap(bytes));
    }
}
