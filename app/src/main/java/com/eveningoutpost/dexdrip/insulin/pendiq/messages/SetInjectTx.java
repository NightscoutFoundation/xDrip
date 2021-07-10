package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.insulin.pendiq.Const;

import static com.eveningoutpost.dexdrip.insulin.pendiq.SequenceCounter.getNext;

public class SetInjectTx extends BaseMessage {

    public SetInjectTx(double units) {
        init(getNext(),5);
        data.put(Const.DONT_REQUEST_RESULT);
        data.put(Const.OPCODE_SET_INJECT);
        data.put((byte)0x00);
        data.putShort((short)(units * 100d));
        appendFooter();
    }

}
