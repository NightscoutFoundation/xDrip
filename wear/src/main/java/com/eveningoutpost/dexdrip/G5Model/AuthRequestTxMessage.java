package com.eveningoutpost.dexdrip.G5Model;

import android.util.Log;

import com.eveningoutpost.dexdrip.G5Model.TransmitterMessage;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.UUID;

import static com.eveningoutpost.dexdrip.utils.CipherUtils.getRandomKey;

/**
 * Created by joeginley on 3/16/16.
 */
public class AuthRequestTxMessage extends TransmitterMessage {
    byte opcode = 0x1;
    public byte[] singleUseToken;
    byte endByte = 0x2;

    private final static String TAG = G5CollectionService.TAG; // meh

    public AuthRequestTxMessage(int token_size) {
        // Create the singleUseToken from a 16 byte array.
        //byte[] uuidBytes = new byte[]{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        byte[] uuidBytes = getRandomKey();
        UUID uuid = UUID.nameUUIDFromBytes(uuidBytes);

        try {
            uuidBytes = uuid.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        ByteBuffer bb = ByteBuffer.allocate(token_size);
        bb.put(uuidBytes, 0, token_size);
        singleUseToken = bb.array();

        // Create the byteSequence.
        data = ByteBuffer.allocate(token_size+2);
        data.put(opcode);
        data.put(singleUseToken);
        data.put(endByte);

        byteSequence = data.array();
        UserError.Log.d(TAG,"New AuthRequestTxMessage: "+ JoH.bytesToHex(byteSequence));
    }
}

