package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;

// created by jamorham

public class SessionStopTxMessage extends BaseMessage {

    final byte opcode = 0x28;
    final int length = 7;
    {
        postExecuteGuardTime = 1000;
    }

    SessionStopTxMessage(int stopTime) {

        init(opcode, length);
        data.putInt(stopTime);
        appendCRC();
    }

    SessionStopTxMessage(String transmitterId) {
        final int stopTime = DexTimeKeeper.getDexTime(transmitterId, JoH.tsl());
        init(opcode, 7);
        data.putInt(stopTime);
        appendCRC();
    }


}
