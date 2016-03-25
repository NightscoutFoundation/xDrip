package com.eveningoutpost.dexdrip.G5Model;

import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeRxMessage extends TransmitterMessage {
    int opcode = 0x3;
    public byte[] tokenHash;
    public byte[] challenge;

    public AuthChallengeRxMessage(byte[] data) {
        if (data.length >= 17) {
            if (data[0] == opcode) {
                tokenHash = Arrays.copyOfRange(data, 1, 9);
                challenge = Arrays.copyOfRange(data, 9, 17);
            }
        }
    }
}
