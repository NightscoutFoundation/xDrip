package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;

/**
 * Created by jamorham on 25/11/2016.
 */

public class GlucoseTxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh
    byte opcode = 0x30;
    byte[] crc = CRC.calculate(opcode);

    public GlucoseTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
        UserError.Log.d(TAG, "GlucoseTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}

