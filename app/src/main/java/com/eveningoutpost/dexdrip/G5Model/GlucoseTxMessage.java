package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by jamorham on 25/11/2016.
 */

public class GlucoseTxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh
    final byte opcode = 0x30;

    public GlucoseTxMessage() {
        data = ByteBuffer.allocate(3).order(ByteOrder.LITTLE_ENDIAN);
        data.put(opcode);
        appendCRC();
        UserError.Log.d(TAG, "GlucoseTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}

