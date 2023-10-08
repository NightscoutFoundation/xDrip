package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.insulin.pendiq.Const;

import static com.eveningoutpost.dexdrip.insulin.pendiq.SequenceCounter.getNext;

public class SetTimeTx extends BaseMessage {

    public SetTimeTx() {
        init(getNext(), 7);
        data.put(Const.DONT_REQUEST_RESULT);
        data.put(Const.OPCODE_SET_TIME_ALT);
        data.putInt((int) ((JoH.tsl() / 1000) + getTimeZoneOffsetSeconds()));
        data.put(getTimeZoneOffsetByte());
        appendFooter();
    }

}
