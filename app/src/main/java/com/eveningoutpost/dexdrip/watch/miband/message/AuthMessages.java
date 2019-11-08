package com.eveningoutpost.dexdrip.watch.miband.message;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.watch.miband.Const;

import java.nio.ByteBuffer;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_REQ;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_REQ2;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_REQ3;


public class AuthMessages extends BaseMessage{

    private static final String TAG = "MiBandAuth";
    byte[] localKey;
    public ByteBuffer data = null;


    private byte[] encrypt(final byte[] keyBytes ) {
        try {
            final SecretKeySpec newKey = new SecretKeySpec(localKey, "AES");
            final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey);
            return cipher.doFinal(keyBytes);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error during encryption: " + e.toString());
            return null;
        }
    }

    public byte[] getAuthCommand() {
        init(OPCODE_AUTH_REQ, 18);
        localKey = CipherUtils.getRandomKey();
        putData(localKey);
        return getBytes();
    }

    public byte[] getAuthKeyRequest() {
        init(OPCODE_AUTH_REQ2, 2);
        return getBytes();
    }

    public byte[] calculateAuthReply(byte[] responseAuthKey) {
        UserError.Log.d(TAG, "Calculating localKey reply for: " + JoH.bytesToHex(localKey));
        final byte[] result = encrypt(responseAuthKey);
        if (result == null) throw new RuntimeException("Cannot calculate auth reply");
        UserError.Log.d(TAG, "Derived: " + JoH.bytesToHex(result));
        init(OPCODE_AUTH_REQ3, 18);
        putData(result);
        return getBytes();
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CUSTOM_SERVICE_AUTH_CHARACTERISTIC;
    }
}
