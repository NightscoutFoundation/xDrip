package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class BondRequestTxMessage extends TransmitterMessage {
    byte opcode = 0x7;

    private final static String TAG = G5CollectionService.TAG; // meh
    public BondRequestTxMessage() {
        data = ByteBuffer.allocate(1);
        data.put(opcode);
        byteSequence = data.array();
        UserError.Log.d(TAG,"New BONDRequestTxMessage: "+ JoH.bytesToHex(byteSequence));
    }
}

