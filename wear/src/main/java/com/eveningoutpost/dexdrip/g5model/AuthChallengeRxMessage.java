package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

import java.util.Arrays;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeRxMessage extends BaseMessage {
    public static final int opcode = 0x03;
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
