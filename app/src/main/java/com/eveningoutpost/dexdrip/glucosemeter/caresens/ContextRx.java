package com.eveningoutpost.dexdrip.glucosemeter.caresens;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// jamorham

public class ContextRx {

    public final ByteBuffer data;
    public final byte flags;
    public byte secondaryFlags;
    public byte carbFlags;
    public int carbInfo;
    public int mealType = -1;
    public final int sequence;
    public final boolean hasSecondaryFlags;
    public final boolean hasMealType;
    public final boolean hasCarbInfo;


    public ContextRx(byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

        flags = data.get();
        hasSecondaryFlags = (flags & 128) > 0;
        hasMealType = (flags & 2) > 0;
        hasCarbInfo = (flags & 1) > 0;

        sequence = data.getShort();

        if (hasSecondaryFlags) {
            secondaryFlags = data.get();
        }

        if (hasCarbInfo) {
            carbFlags = data.get();
            carbInfo = data.getShort();
        }

        if (hasMealType) {
            mealType = data.get();
        }

    }

    public boolean ketone() {
        return mealType == 0x06;
    }

    public boolean normalRecord() {
        return mealType >= 0 && mealType <= 3;
    }

    public String toString() {
        return "Context: "
                + "Sequence: " + sequence + " "
                + (ketone() ? " KETONE " : "")
                + (hasSecondaryFlags ? " SECONDARY FLAGS " : "")
                + (hasMealType ? " MEALTYPE: " + mealType + " " : "")
                + (hasCarbInfo ? " CARB INFO " : "");
    }

}
