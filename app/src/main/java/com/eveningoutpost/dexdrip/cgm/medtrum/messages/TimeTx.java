package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

import com.eveningoutpost.dexdrip.models.JoH;

import java.util.Calendar;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_TIME_REQST;

// jamorham

public class TimeTx extends BaseMessage {

    final byte opcode = OPCODE_TIME_REQST; // 0x57
    final int length = 6;

    public TimeTx() {
        init(opcode, length, true);
        data.put((byte) 0x01);

        final Calendar cal = Calendar.getInstance();
        cal.set(2014, 0, 1, 0, 0, 0);
        final int secondsSinceReferenceDate = (int) (JoH.msSince(cal.getTimeInMillis()) / 1000);

        data.putInt(secondsSinceReferenceDate);

    }
}
