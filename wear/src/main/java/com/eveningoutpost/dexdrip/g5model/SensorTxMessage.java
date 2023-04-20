package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

import java.nio.ByteBuffer;

/**
 * Created by jcostik1 on 3/26/16.
 */
public class SensorTxMessage extends BaseMessage {
    byte opcode = 0x2e;
    byte[] crc = CRC.calculate(opcode);


    public SensorTxMessage() {
        data = ByteBuffer.allocate(3);
        data.put(opcode);
        data.put(crc);
        byteSequence = data.array();
        UserError.Log.d(TAG, "SensorTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}
