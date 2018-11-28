package com.eveningoutpost.dexdrip.watch.lefun.messages;

public class TxSetLocaleFeature extends BaseTx {

    final byte opcode = 0x02;

    public static final int CLOCK_FORMAT_12_HOUR = 1;
    public static final int DISTANCE_FORMAT_IMPERIAL = 2;

    public TxSetLocaleFeature(final int function, final boolean state) {

        init(5);

        data.put(opcode);
        data.put(WRITE);
        data.put((byte) 0);

        switch (function) {
            case CLOCK_FORMAT_12_HOUR:
                data.put((byte) (state ? 1 : 0)); // 0 = 24 hour / 1 = 12 hour
                data.put((byte) 255);
                break;
            case DISTANCE_FORMAT_IMPERIAL:
                data.put((byte) 255);
                data.put((byte) (state ? 1 : 0)); // 0 = cm/kg / 1 = feet/lb
            default:
                throw new RuntimeException("Unknown type");
        }

    }

}
