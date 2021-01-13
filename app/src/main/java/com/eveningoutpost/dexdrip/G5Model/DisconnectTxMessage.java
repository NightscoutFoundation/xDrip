package com.eveningoutpost.dexdrip.G5Model;


import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.usererror.UserErrorLog;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;

/**
 * Created by joeginley on 3/16/16.
 */
public class DisconnectTxMessage extends BaseMessage {
    byte opcode = 0x09;
    private final static String TAG = G5CollectionService.TAG; // meh
    public DisconnectTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);

        byteSequence = data.array();
        UserErrorLog.d(TAG,"DisconnectTX: "+ JoH.bytesToHex(byteSequence));
    }
}

