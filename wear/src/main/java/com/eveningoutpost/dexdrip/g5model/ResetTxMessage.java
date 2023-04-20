package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import java.nio.ByteBuffer;


/**
 * Created by jcostik1 on 3/26/16.
 */
public class ResetTxMessage extends TransmitterMessage {
    byte opcode = 0x42;
    private byte[] crc = CRC.calculate(opcode);
    private final static String TAG = G5CollectionService.TAG; // meh


    public ResetTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
        UserError.Log.d(TAG, "ResetTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}
