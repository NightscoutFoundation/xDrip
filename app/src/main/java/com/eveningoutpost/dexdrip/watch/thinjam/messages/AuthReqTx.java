package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import android.annotation.SuppressLint;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJay;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import lombok.Getter;
import lombok.val;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_AUTH_REQ;
import static com.eveningoutpost.dexdrip.watch.thinjam.messages.AuthReqTx.TJ_AuthState.AccessGranted;
import static com.eveningoutpost.dexdrip.watch.thinjam.messages.AuthReqTx.TJ_AuthState.Hello;
import static com.eveningoutpost.dexdrip.watch.thinjam.messages.AuthReqTx.TJ_AuthState.Hello2U2;
import static com.eveningoutpost.dexdrip.watch.thinjam.messages.AuthReqTx.TJ_AuthState.ThePassWordIs;

// jamorham

public class AuthReqTx extends BaseTx {

    private static final String TAG = "BlueJayAuth";

    enum TJ_AuthState {

        Hello(1),
        Hello2U2(2),
        ThePassWordIs(3),
        AccessDenied(4),
        AccessGranted(5);

        @Getter
        private final byte value;

        TJ_AuthState(final int value) {
            this.value = (byte) value;
        }
    }


    public static byte[] ecbEncrypt(final byte[] keyBytes, final byte[] clearBytes) {
        try {
            final SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            @SuppressLint("GetInstance") final Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey);
            return cipher.doFinal(clearBytes);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error during encryption: " + e.toString());
            return null;
        }
    }

    public AuthReqTx(final TJ_AuthState state, final byte[] challenge) {

        init(OPCODE_AUTH_REQ, 9);
        data.put(state.getValue());

        if (challenge != null && challenge.length == 8) {
            data.put(challenge);
        }
    }

    private static byte[] doubleChallenge(final byte[] challenge) {
        if (challenge == null || challenge.length != 8) {
            throw new RuntimeException("Challenge wrong length");
        }
        val dChallenge = new byte[16];
        System.arraycopy(challenge, 0, dChallenge, 0, 8);
        System.arraycopy(challenge, 0, dChallenge, 8, 8);
        UserError.Log.d(TAG, "Full challenge: " + JoH.bytesToHex(dChallenge));
        return dChallenge;
    }

    private static byte[] calculateChallengeReply(final byte[] challenge) {
        UserError.Log.d(TAG, "Calculating challenge reply for: " + JoH.bytesToHex(challenge));
        final String mac = BlueJay.getMac();
        if (mac == null || mac.length() != 17) {
            UserError.Log.e(TAG, "No mac stored to use for auth");
        } else {
            val authKeyString = BlueJay.getAuthKey(mac);
            if (authKeyString != null) {
                UserError.Log.d(TAG, "Using auth key: " + authKeyString);
                final byte[] authKey = JoH.tolerantHexStringToByteArray(authKeyString);
                if (authKey.length == 16) {
                    final byte[] result = ecbEncrypt(authKey, doubleChallenge(challenge));
                    UserError.Log.d(TAG, "Derived: " + JoH.bytesToHex(result));
                    return result != null ? Arrays.copyOfRange(result, 0, 8) : null;
                } else {
                    UserError.Log.e(TAG, "Invalid or missing authentication key for: " + BlueJay.getMac());
                }
            } else {
                UserError.Log.e(TAG, "Auth key was null for: " + mac);
            }
        }
        return null;
    }

    public static AuthReqTx getNextAuthPacket(final byte[] packet) {
        if (packet == null || packet.length == 0) {
            return new AuthReqTx(Hello, null);
        } else {
            if (packet.length >= 10) {
                final byte state = packet[1];
                if (state == Hello2U2.value) {
                    final byte[] challengeFromDevice = Arrays.copyOfRange(packet, 2, 10);
                    final byte[] challengeReply = calculateChallengeReply(challengeFromDevice);
                    UserError.Log.d(TAG, "Device challenge: " + JoH.bytesToHex(challengeFromDevice) + " our reply: " + JoH.bytesToHex(challengeReply));
                    return challengeReply != null ? new AuthReqTx(ThePassWordIs, challengeReply) : null;
                }
            }
        }

        // unknown state??
        return null;
    }

    public static boolean isAccessGranted(final byte[] packet) {
        return (packet != null && packet.length >= 2 && packet[0] == OPCODE_AUTH_REQ && packet[1] == AccessGranted.value);
    }
}
