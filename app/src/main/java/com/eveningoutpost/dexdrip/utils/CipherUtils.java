package com.eveningoutpost.dexdrip.utils;

/**
 * Created by jamorham on 06/01/16.
 */

import android.util.Base64;
import android.util.Log;

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

    public static byte[] encryptBytes(byte[] plainText) {
        byte[] ivBytes = new byte[16];

        byte[] keyBytes = getKeyBytes(key);
        if (keyBytes.length != 16) {
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
        byte[] ivBytes = new byte[16];
        if (cipherData.length < ivBytes.length) return errorbyte;
        byte[] keyBytes = getKeyBytes(key);
        if (keyBytes.length != 16) {
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
            byte[] inbytes = Base64.decode(cipherData, Base64.NO_WRAP);
            return new String(decryptBytes(inbytes), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Got unsupported encoding on UTF8 " + e.toString());
            return "";
        }
    }

    public static String encryptString(String plainText) {
        byte[] inbytes = plainText.getBytes(Charset.forName("UTF-8"));
        return Base64.encodeToString(encryptBytes(inbytes), Base64.NO_WRAP);
    }

}
