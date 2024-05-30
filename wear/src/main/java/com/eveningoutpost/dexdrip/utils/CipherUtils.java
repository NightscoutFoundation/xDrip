package com.eveningoutpost.dexdrip.utils;

/**
 * Created by jamorham on 06/01/16.
 */

import android.util.Base64;
import android.util.Log;

//KS import com.eveningoutpost.dexdrip.GoogleDriveInterface;
import com.eveningoutpost.dexdrip.models.JoH;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtils {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    static final String TAG = "jamorham cip";
    static final byte[] errorbyte = {};
    static String key = "ebe5c0df162a50ba232d2d721ea8e3e1c5423bb0-12bd-48c3-8932-c93883dfcf1f"; // allow override from prefs

    public static byte[] encrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            Log.e(TAG, "Error during encryption: " + e.toString());
            return errorbyte;
        }
    }

    public static byte[] decrypt(byte[] ivBytes, byte[] keyBytes, byte[] textBytes) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(ivBytes);
            SecretKeySpec newKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, newKey, ivSpec);
            return cipher.doFinal(textBytes);
        } catch (Exception e) {
            return errorbyte;
        }
    }

    private static byte[] getKeyBytes(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Password creation exception: " + e.toString());
            return errorbyte;
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hex) {
       try {
           int length = hex.length();
           byte[] bytes = new byte[length / 2];
           for (int i = 0; i < length; i += 2) {
               bytes[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
           }
           return bytes;
       } catch (Exception e){
            Log.e(TAG,"Got Exception: "+e.toString());
           return new byte[0];
        }
    }

    public static String getSHA256(byte[] mydata) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA256");
            digest.update(mydata);
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA hash exception: " + e.toString());
            return null;
        }
    }

    public static String getSHA256(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("SHA256");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "SHA hash exception: " + e.toString());
            return null;
        }
    }

    public static String getMD5(String mykey) {
        try {
            MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(mykey.getBytes(Charset.forName("UTF-8")));
            return bytesToHex(digest.digest()).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 hash exception: " + e.toString());
            return null;
        }
    }

    public static byte[] encryptBytes(byte[] plainText) {
        byte[] keyBytes = getKeyBytes(key);//KS  + GoogleDriveInterface.getDriveKeyString());
        return encryptBytes(plainText, keyBytes);
    }

    public static byte[] encryptBytes(byte[] plainText, byte[] keyBytes) {
        byte[] ivBytes = new byte[16];

        if ((keyBytes == null) || (keyBytes.length != 16)) {
            Log.e(TAG, "Invalid Keybytes length!");
            return errorbyte;
        }
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(ivBytes);
        byte[] cipherData = CipherUtils.encrypt(ivBytes, keyBytes, plainText);
        byte[] destination = new byte[cipherData.length + ivBytes.length];
        System.arraycopy(ivBytes, 0, destination, 0, ivBytes.length);
        System.arraycopy(cipherData, 0, destination, ivBytes.length, cipherData.length);
        return destination;
    }
    public static byte[] decryptBytes(byte[] cipherData) {
        byte[] keyBytes = getKeyBytes(key);//KS   + GoogleDriveInterface.getDriveKeyString());
        return decryptBytes(cipherData,keyBytes);
    }
    public static byte[] decryptBytes(byte[] cipherData,byte[] keyBytes) {
        byte[] ivBytes = new byte[16];
        if (cipherData.length < ivBytes.length) return errorbyte;
        if ((keyBytes==null) || (keyBytes.length != 16)) {
            Log.e(TAG, "Invalid Keybytes length!");
            return errorbyte;
        }
        System.arraycopy(cipherData, 0, ivBytes, 0, ivBytes.length);
        byte[] destination = new byte[cipherData.length - ivBytes.length];
        System.arraycopy(cipherData, ivBytes.length, destination, 0, destination.length);
        return CipherUtils.decrypt(ivBytes, keyBytes, destination);
    }

    public static String decryptString(String cipherData) {
        try {
            return new String(decryptStringToBytes(cipherData), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding on UTF8 " + e.toString());
            return "";
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Got IllegalArgumentException encoding on UTF8 ", e);
            return "";
        }
    }

    public static byte[] decryptStringToBytes(String cipherData) {
        byte[] inbytes = Base64.decode(cipherData, Base64.NO_WRAP);
        byte[] decryptedBytes = decryptBytes(inbytes);
        if ((decryptedBytes.length > 8)
                && (decryptedBytes[0] == (byte) 0x1F)
                && (decryptedBytes[1] == (byte) 0x8B)
                && (decryptedBytes[2] == (byte) 0x08)
                && (decryptedBytes[3] == (byte) 0x00)) {
            decryptedBytes = JoH.decompressBytesToBytes(decryptedBytes);
        }
        return decryptedBytes;
    }

    public static String encryptString(String plainText) {
        byte[] inbytes = plainText.getBytes(Charset.forName("UTF-8"));
        return Base64.encodeToString(encryptBytes(inbytes), Base64.NO_WRAP);
    }

    public static String encryptBytesToString(byte[] inbytes) {
        return Base64.encodeToString(encryptBytes(inbytes), Base64.NO_WRAP);
    }

    public static String compressEncryptString(String plainText) {
        return Base64.encodeToString(CipherUtils.encryptBytes(JoH.compressStringToBytes(plainText))
                , Base64.NO_WRAP);
    }

    public static String compressEncryptBytes(byte[] plainText) {
        return Base64.encodeToString(CipherUtils.encryptBytes(JoH.compressBytesToBytes(plainText))
                , Base64.NO_WRAP);
    }

    public static byte[] getRandomKey() {
        byte[] keybytes = new byte[16];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(keybytes);
        return keybytes;
    }

    public static String getRandomHexKey() {
        return bytesToHex(getRandomKey());
    }
}

