package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;

/**
 * Created by jamorham on 25/11/2016.
 */

public class VersionRequestTxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh
    byte opcode = 0x4A;
    private byte[] crc = CRC.calculate(opcode);

    public VersionRequestTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
        UserError.Log.e(TAG, "VersionTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}

