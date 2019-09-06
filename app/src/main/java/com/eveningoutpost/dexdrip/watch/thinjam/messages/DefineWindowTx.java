package com.eveningoutpost.dexdrip.watch.thinjam.messages;


import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_DEFINE_WINDOW;

// jamorham

public class DefineWindowTx extends BaseTx {

    public DefineWindowTx(final byte id, final byte type, final byte start_x, final byte start_y, final byte width, final byte height, final byte timeout, final byte colorEffect) {
        init(OPCODE_DEFINE_WINDOW, 8);
        data.put(id);
        data.put(type); // 1 = mono bitmap
        data.put(start_x);
        data.put(start_y);
        data.put(width);
        data.put(height);
        data.put(timeout);
        data.put(colorEffect);
        // TODO use rest of packet??
    }

}
