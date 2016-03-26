package com.eveningoutpost.dexdrip.G5Model;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthStatusRxMessage extends TransmitterMessage {
    int opcode = 0x5;
    public int authenticated;
    public int bonded;

    public AuthStatusRxMessage(byte[] data) {
        if (data.length >= 3) {
            if (data[0] == opcode) {
                authenticated = data[1];
                bonded = data[2];
            }
        }
    }
}
