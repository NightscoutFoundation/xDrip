package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

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
        UserError.Log.d(TAG,"DisconnectTX: "+ JoH.bytesToHex(byteSequence));
    }
}

