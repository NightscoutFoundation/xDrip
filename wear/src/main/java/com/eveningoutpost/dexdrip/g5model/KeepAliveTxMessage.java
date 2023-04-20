package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class KeepAliveTxMessage extends TransmitterMessage {
    public static final int opcode = 0x6;
    private int time;

    private final static String TAG = G5CollectionService.TAG; // meh

    public KeepAliveTxMessage(int time) {
        this.time = time;

        data = ByteBuffer.allocate(2);
        data.put(new byte[]{(byte) opcode, (byte) this.time});
        byteSequence = data.order(ByteOrder.LITTLE_ENDIAN).array();

        UserError.Log.d(TAG, "New KeepAliveRequestTxMessage: " + JoH.bytesToHex(byteSequence));

    }
}