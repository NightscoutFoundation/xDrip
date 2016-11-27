package com.eveningoutpost.dexdrip.G5Model;


import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthStatusRxMessage extends TransmitterMessage {
    int opcode = 0x5;
    public int authenticated;
    public int bonded;
    private final static String TAG = G5CollectionService.TAG; // meh

    public AuthStatusRxMessage(byte[] packet) {
        if (packet.length >= 3) {
            if (packet[0] == opcode) {
                data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);

                authenticated = data.get(1);
                bonded = data.get(2);
                UserError.Log.d(TAG,"AuthRequestRxMessage:  authenticated:"+authenticated+"  bonded:"+bonded);
            }
        }
    }
}
