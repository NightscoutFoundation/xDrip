package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.insulin.pendiq.Const;

import static com.eveningoutpost.dexdrip.insulin.pendiq.SequenceCounter.getNext;

public class InsulinLogTx extends BaseMessage {

    public InsulinLogTx(long since) {
        init(getNext(),6);
        data.put(Const.REQUEST_RESULT);
        data.put(Const.OPCODE_GET_INSULIN_LOG);
        data.putInt((int)(since / 1000L)); // appears not to be implemented by device
        appendFooter();
    }

}
