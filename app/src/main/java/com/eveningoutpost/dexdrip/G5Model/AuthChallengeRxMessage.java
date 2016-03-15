package com.eveningoutpost.dexdrip.G5Model;

import java.nio.ByteBuffer;

/**
 * Created by jcostik1 on 3/15/16.
 */
public class AuthChallengeRxMessage extends TransmitterRxMessage {
    static byte opcode = 0x3;
    ByteBuffer tokenHash = null;
    ByteBuffer challenge = null;

    AuthChallengeRxMessage(ByteBuffer data) {
        if (data.array().length >= 17) {
            if (data.array()[0] == opcode + 1 ) {
                //challenge =
                //tokenHash =
            }
        }
    }
}
