package com.eveningoutpost.dexdrip.watch.miband.message;

import android.os.Environment;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.watch.miband.Const;
import com.eveningoutpost.dexdrip.watch.miband.MiBand;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_REQUEST_RANDOM_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_ENCRYPTED_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_KEY;


public class AuthMessages extends BaseMessage {
    private byte authFlags = OperationCodes.AUTH_BYTE;
    private byte cryptFlags = 0x00;
    private static final String TAG = "MiBandAuth";

    byte[] localKey;
    public ByteBuffer data = null;

    public AuthMessages(MiBand.MiBandType mibandType, String authKey) {
        if (mibandType == MiBand.MiBandType.MI_BAND4) {
            cryptFlags = OperationCodes.AUTH_MIBAND4_CRYPT_FLAG;
        }
        localKey = CipherUtils.getRandomKey();
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.substring(0, 2).equals("0x")) {
                srcBytes = HexDump.hexStringToByteArray(authKey.substring(2));
            } else srcBytes = HexDump.hexStringToByteArray(authKey);
            System.arraycopy(srcBytes, 0, localKey, 0, Math.min(srcBytes.length, 16));
        }
    }

    public byte[] getLocalKey() {
        return localKey;
    }

    private byte[] encrypt(final byte[] keyBytes) {
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

    public static Boolean isValidAuthKey(String authKey){
        if ((authKey.length() != 32) || !authKey.matches("[a-zA-Z0-9]+"))
            return false;
        return true;
    }

    public static String getAuthCodeFromFilesSystem(String mac) {
        String authKey = "";
        String macFileName = mac.replace(":", "").toUpperCase();
        String fileName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/freemyband/" + "miband" + macFileName + ".txt";
        File f = new File(fileName);
        if (f.exists() && f.isFile()) {
            try {
                FileInputStream fin = null;
                fin = new FileInputStream(f);
                BufferedReader br = new BufferedReader(new InputStreamReader(fin));

                String line = null;
                while ((line = br.readLine()) != null) {
                    // System.out.println(line);
                    String[] splited = line.split(";");
                    if (splited[0].equalsIgnoreCase(mac)) {
                        authKey = splited[1];
                    } else continue;
                }
                br.close();
            } catch (FileNotFoundException e) {

            } catch (IOException e) {
            }

        }
        return authKey.toLowerCase();
    }

    public byte[] getAuthCommand() {
        init(18);
        putData(AUTH_SEND_KEY);
        putData(authFlags);
        //if (cryptFlags == 0)
            putData(localKey);
        return getBytes();
    }

    public byte[] getAuthKeyRequest() {
        if (cryptFlags == 0x00) {
            init(2);
            putData(AUTH_REQUEST_RANDOM_AUTH_NUMBER);
            putData(authFlags);
        }
        else{
            init(3);
            putData((byte) (cryptFlags | AUTH_REQUEST_RANDOM_AUTH_NUMBER));
            putData(authFlags);
            putData((byte)0x02);
        }
        return getBytes();
    }

    public byte[] calculateAuthReply(byte[] responseAuthKey) {
        UserError.Log.d(TAG, "Calculating localKey reply for: " + JoH.bytesToHex(localKey));
        final byte[] result = encrypt(responseAuthKey);
        if (result == null) throw new RuntimeException("Cannot calculate auth reply");
        UserError.Log.d(TAG, "Derived: " + JoH.bytesToHex(result));
        init(18);
        putData((byte) (AUTH_SEND_ENCRYPTED_AUTH_NUMBER | cryptFlags));
        putData(authFlags);
        putData(result);
        return getBytes();
    }

    @Override
    public UUID getCharacteristicUUID() {
        return Const.UUID_CHARACTERISTIC_AUTH;
    }
}
