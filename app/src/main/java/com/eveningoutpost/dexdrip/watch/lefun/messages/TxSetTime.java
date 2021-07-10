package com.eveningoutpost.dexdrip.watch.lefun.messages;

import java.util.Calendar;

// jamorham

public class TxSetTime extends BaseTx {

    private static final byte opcode = 0x04;

    public TxSetTime(final long timestamp) {
        this(timestamp, false, false);
    }

    public TxSetTime(final long timestamp, boolean zeroMonth, boolean zeroDay) {
        this(timestamp, zeroMonth, zeroDay, null);
    }

    public TxSetTime(final long timestamp, boolean zeroMonth, boolean zeroDay, Byte hardMonth) {

        init(8);

        data.put(opcode);
        data.put(WRITE);

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);

        data.put((byte) (c.get(Calendar.YEAR) % 100));
        if (hardMonth == null) {
            data.put((byte) (zeroMonth ? 0 : c.get(Calendar.MONTH) + 1));
        } else {
            data.put(hardMonth);
        }
        data.put((byte) (zeroDay ? 0 : c.get(Calendar.DAY_OF_MONTH)));
        data.put((byte) c.get(Calendar.HOUR_OF_DAY));
        data.put((byte) c.get(Calendar.MINUTE));
        data.put((byte) c.get(Calendar.SECOND));
    }

}
