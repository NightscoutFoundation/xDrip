package com.eveningoutpost.dexdrip.watch.lefun.messages;

// jamorham

public class TxSetScreens extends BaseTx {

    private static final byte opcode = 0x07;

    public static final int STEP_COUNTER = 0;
    public static final int STEP_DISTANCE = 1;
    public static final int STEP_CALORIES = 2;
    public static final int HEART_RATE = 3;
    public static final int HEART_PRESSURE = 4;
    public static final int FIND_PHONE = 5;
    public static final int MAC_ADDRESS = 6;
    public static final int UNKNOWN_BUT_ON = 7;

    public TxSetScreens() {

        init(4);

        data.put(opcode);
        data.put(WRITE);

        bitmap_start_offset = 4;
        enable(UNKNOWN_BUT_ON);

    }

}
