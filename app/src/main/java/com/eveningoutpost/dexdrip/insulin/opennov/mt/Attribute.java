package com.eveningoutpost.dexdrip.insulin.opennov.mt;


import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.AllArgsConstructor;
import lombok.val;

/**
 * JamOrHam
 * OpenNov Attributes
 */

public class Attribute extends BaseMessage {

    public Atype atype;
    public int type;
    public int len;
    public long ivalue;
    public byte[] bytes;

    @AllArgsConstructor
    public enum Atype {
        MDC_ATTR_SYS_ID(2436),
        MDC_ATTR_ID_INSTNO(2338),
        MDC_ATTR_ID_MODEL(2344),
        MDC_ATTR_ID_PROD_SPECN(2349),
        MDC_ATTR_ID_TYPE(2351),
        MDC_ATTR_METRIC_STORE_CAPAC_CNT(2369),
        MDC_ATTR_METRIC_STORE_SAMPLE_ALG(2371),
        MDC_ATTR_METRIC_STORE_USAGE_CNT(2372),
        MDC_ATTR_NUM_SEG(2385),
        MDC_ATTR_OP_STAT(2387),
        MDC_ATTR_SEG_USAGE_CNT(2427),
        MDC_ATTR_TIME_REL(2447),
        MDC_ATTR_UNIT_CODE(2454),
        MDC_ATTR_DEV_CONFIG_ID(2628),
        MDC_ATTR_MDS_TIME_INFO(2629),
        MDC_ATTR_METRIC_SPEC_SMALL(2630),
        MDC_ATTR_REG_CERT_DATA_LIST(2635),
        MDC_ATTR_PM_STORE_CAPAB(2637),
        MDC_ATTR_PM_SEG_MAP(2638),
        MDC_ATTR_ATTRIBUTE_VAL_MAP(2645),
        MDC_ATTR_NU_VAL_OBS_SIMP(2646),
        MDC_ATTR_PM_STORE_LABEL_STRING(2647),
        MDC_ATTR_PM_SEG_LABEL_STRING(2648),
        MDC_ATTR_SYS_TYPE_SPEC_LIST(2650),
        MDC_ATTR_CLEAR_TIMEOUT(2659),
        MDC_ATTR_TRANSFER_TIMEOUT(2660),
        MDC_ATTR_ENUM_OBS_VAL_BASIC_BIT_STR(2662),
        ;
        int value;

        public static Atype findByValue(final int v) {
            for (val i : Atype.values()) {
                if (i.value == v) return i;
            }
            return null;
        }
    }

    public static Attribute parse(final ByteBuffer buffer) {
        val a = new Attribute();
        a.type = getUnsignedShort(buffer);
        a.len = getUnsignedShort(buffer);
        a.bytes = new byte[a.len];
        buffer.get(a.bytes, 0, a.len);
        a.atype = Atype.findByValue(a.type);
        a.ivalue = getIvalue(a.bytes, a.len);
        if (d)
            log("Attribute: " + a.atype + " (" + a.type + ") " + a.len + " b: " + HexDump.dumpHexString(a.bytes));
        return a;
    }

}
