package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

public class TxSetFeatures extends BaseTx {

    private static final byte opcode = 0x08;

    public static final int LIFT_TO_WAKE = 0;
    public static final int SEDENTARY_REMINDER = 1;
    public static final int DRINKING_REMINDER = 2;
    public static final int CAMERA = 3;
    public static final int UNKNOWN4 = 4;
    public static final int ANTI_LOST = 5;
    public static final int UNKNOWN6 = 6;
    public static final int UNKNOWN7 = 7;


    public TxSetFeatures() {

        init(4);

        data.put(opcode);
        data.put(WRITE);

        bitmap_start_offset = 4;

        enable(CAMERA); // needed for shake
    }


}
