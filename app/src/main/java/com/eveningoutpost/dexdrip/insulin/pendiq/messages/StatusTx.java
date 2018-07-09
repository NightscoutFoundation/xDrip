package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.insulin.pendiq.Const;

import static com.eveningoutpost.dexdrip.insulin.pendiq.SequenceCounter.getNext;

public class StatusTx extends BaseMessage {

    public StatusTx() {
        init(getNext(),2);
        data.put(Const.REQUEST_RESULT);
        data.put(Const.OPCODE_GET_STATUS);
        appendFooter();
    }

}
