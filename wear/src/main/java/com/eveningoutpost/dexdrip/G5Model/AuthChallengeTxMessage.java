package com.eveningoutpost.dexdrip.G5Model;
import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthChallengeTxMessage extends TransmitterMessage {
    byte opcode = 0x4;
    byte[] challengeHash;

    private final static String TAG = G5CollectionService.TAG; // meh
    public AuthChallengeTxMessage(byte[] challenge) {
        challengeHash = challenge;

        data = ByteBuffer.allocate(9);
        data.put(opcode);
        data.put(challengeHash);

        byteSequence = data.array();
        UserError.Log.d(TAG,"AuthChallengeTX: "+ JoH.bytesToHex(byteSequence));
    }
}
