package com.eveningoutpost.dexdrip.insulin.opennov.mt;

import static com.eveningoutpost.dexdrip.models.JoH.tsl;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.insulin.opennov.BaseMessage;

import java.nio.ByteBuffer;

import lombok.val;

/**
 * JamOrHam
 * OpenNov Insulin Dose
 */

public class InsulinDose extends BaseMessage {

    private static final double MAX_UNIT_VALUE = 60d;

    long relativeTime;
    public long absoluteTime;
    public double units;
    long flags;

    public boolean isValid() {
        return units > 0 && units < 100
                && flags == 0x08000000
                && absoluteTime < tsl()
                && absoluteTime > tsl() - (Constants.MONTH_IN_MS * 12);
    }

    public String getHash() {
        return "Open" + relativeTime + ":" + units;
    }

    public static InsulinDose parse(final ByteBuffer buffer, long relativeTime) {
        val d = new InsulinDose();
        d.relativeTime = getUnsignedInt(buffer);
        d.absoluteTime = tsl() - ((relativeTime - d.relativeTime) * Constants.SECOND_IN_MS);
        val units = getUnsignedInt(buffer);
        if ((units & 0xFFFF0000) == 0xFF000000L) {
            d.units = (units & 0xFFFF) / 10d;
            if (d.units > MAX_UNIT_VALUE) {
                error("Beyond maximum units value, marking invalid " + d.toJson());
                d.units = -1;
            }
        } else {
            d.units = -1;
        }

        d.flags = getUnsignedInt(buffer);
        return d;
    }
}
