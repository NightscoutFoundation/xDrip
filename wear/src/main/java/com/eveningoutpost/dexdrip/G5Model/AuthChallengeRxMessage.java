package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeRxMessage extends TransmitterMessage {
    int opcode = 0x3;
    public byte[] tokenHash;
    public byte[] challenge;
    private final static String TAG = G5CollectionService.TAG; // meh
    public AuthChallengeRxMessage(byte[] data) {
        UserError.Log.d(TAG,"AuthChallengeRX: "+ JoH.bytesToHex(data));
        if (data.length >= 17) {
            if (data[0] == opcode) {
                tokenHash = Arrays.copyOfRange(data, 1, 9);
                challenge = Arrays.copyOfRange(data, 9, 17);
            }
        }
    }
}
