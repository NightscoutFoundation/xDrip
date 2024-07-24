package com.eveningoutpost.dexdrip.watch.thinjam.firmware;

import com.eveningoutpost.dexdrip.models.JoH;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import lombok.val;

// jamorham

public class BlueJayFwId {

    private static String FW_ID_HASH_CONSTANT = "bluejay-firmware-hash-";

    public static String getFwPrefix(final String mac, final String IDhash) {
        if (mac == null || mac.length() != 17) return null;
        if (IDhash == null || IDhash.length() != 32) return null;

        final String password = IDhash.toUpperCase() + FW_ID_HASH_CONSTANT + mac.toUpperCase();
        try {
            val md = MessageDigest.getInstance("SHA-256");
            md.update(password.getBytes());
            return JoH.bytesToHex(md.digest()).toLowerCase().substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}


