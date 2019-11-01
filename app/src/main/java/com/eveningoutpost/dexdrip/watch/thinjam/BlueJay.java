package com.eveningoutpost.dexdrip.watch.thinjam;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.ui.activities.ThinJamActivity;

import java.util.Arrays;

import lombok.val;

// jamorham

public class BlueJay {

    /**
     * Jamorham
     *
     * BlueJay Lightweight logic class
     */

    private static final String TAG = "BlueJay";
    private static final String PREF_BLUEJAY_MAC = "bluejay_mac";
    private static final String PREF_BLUEJAY_AUTH = "bluejay-auth-";
    private static final String PREF_BLUEJAY_IDENTITY = "bluejay-identity-";
    private static final String PREF_BLUEJAY_BEEP = "bluejay_beep_on_connect";
    private static final String PREF_BLUEJAY_SEND_READINGS = "bluejay_send_readings";

    public static boolean isCollector() {
        return Pref.getBooleanDefaultFalse("bluejay_collector_enabled");
    }

    // process a pairing QR code and if it is from BlueJay then store and process it and return true
    public static boolean processQRCode(byte[] barcodeBytes) {
        if (barcodeBytes != null && barcodeBytes.length > 2
                && barcodeBytes[0] == (byte) 0x41 && (barcodeBytes[1] == (byte) 0x80) && barcodeBytes.length >= 26) {
            barcodeBytes = Arrays.copyOfRange(QRnibbleShift(barcodeBytes), 2, 26);
            UserError.Log.d(TAG, "PROCESS BARCODE2: " + JoH.bytesToHex(barcodeBytes) + " " + barcodeBytes.length);
            if ((barcodeBytes[0] == (byte) 0x04) && (barcodeBytes[1] == (byte) 0x21)) {
                // bluejay init
                if (barcodeBytes.length == 24) {
                    UserError.Log.d(TAG, "QR bytes: " + JoH.bytesToHex(barcodeBytes));
                    final byte[] macBytes = Arrays.copyOfRange(barcodeBytes, 2, 8);
                    final byte[] keyBytes = Arrays.copyOfRange(barcodeBytes, 8, 24);
                    BlueJay.storeAuthKey(JoH.bytesToHex(macBytes), JoH.bytesToHex(keyBytes));
                    BlueJay.setMac(JoH.macFormat(JoH.bytesToHex(macBytes)));
                    BlueJayEntry.setEnabled();
                    BlueJayEntry.initialStartIfEnabled();
                    ThinJamActivity.refreshFromStoredMac();
                    return true;
                } else {
                    UserError.Log.e(TAG, "QR code Length doesn't match @ " + barcodeBytes.length);
                }
            } else {
                UserError.Log.d(TAG, "Can't find magic in qr code");
            }
        }
        return false;
    }

    // convert QR code data in to something more manageable
    static byte[] QRnibbleShift(final byte[] input) {
        final byte[] output = new byte[input.length + 1];
        for (int p = 0; p < input.length * 2; p++) {
            int nib = input[p / 2] & 0xFF;
            if (p % 2 == 0) {
                nib = (nib >> 4) & 0x0F;
            } else {
                nib = (nib << 4) & 0xF0;
            }
            output[(p + 1) / 2] |= (byte) nib;
        }
        return output;
    }

    public static void storeAuthKey(String mac, final String key) {
        mac = JoH.macFormat(mac);
        UserError.Log.d(TAG, "STORE AUTH: " + mac + " " + key);
        if (mac != null && mac.length() == 17
                && key != null && key.length() == 32) {
            Pref.setString(PREF_BLUEJAY_AUTH + mac.toUpperCase(), key);
        } else {
            UserError.Log.e(TAG, "Cannot store auth key as may be invalid");
        }
    }

    public static String getAuthKey(final String mac) {
        if (mac == null) return null;
        return Pref.getString(PREF_BLUEJAY_AUTH + JoH.macFormat(mac).toUpperCase(), null);
    }

    public static boolean haveAuthKey(final String mac) {
        final String key = getAuthKey(mac);
        return key != null && key.length() > 10;
    }

    public static void storeIdentityKey(String mac, final String key) {
        mac = JoH.macFormat(mac);
        UserError.Log.d(TAG, "STORE IDENTITY: " + mac + " " + key);
        if (mac != null && mac.length() == 17
                && key != null && key.length() == 32) {
            Pref.setString(PREF_BLUEJAY_IDENTITY + mac.toUpperCase(), key);
        } else {
            UserError.Log.e(TAG, "Cannot store identity key as may be invalid");
        }
    }

    public static boolean hasIdentityKey() {
        val address = getMac();
        return address != null && getIdentityKey(address) != null;
    }

    public static String getIdentityKey(final String mac) {
        if (mac == null) return null;
        return Pref.getString(PREF_BLUEJAY_IDENTITY + JoH.macFormat(mac).toUpperCase(), null);
    }

    public static String getMac() {
        return Pref.getString(PREF_BLUEJAY_MAC, null);
    }

    static void setMac(final String mac) {
        Pref.setString(PREF_BLUEJAY_MAC, mac);
    }

    static boolean shouldBeepOnConnect() {
        return Pref.getBooleanDefaultFalse(PREF_BLUEJAY_BEEP);
    }

    static boolean shouldSendReadings() {
        return Pref.getBooleanDefaultFalse(PREF_BLUEJAY_SEND_READINGS);
    }

    public static boolean localAlarmsEnabled() {
        return Pref.getBoolean("bluejay_local_alarms", true);
    }

    public static void showLatestBG() {
        if (BlueJayEntry.isEnabled() && shouldSendReadings()) {
            // already on background thread and debounced
            JoH.startService(BlueJayService.class, "function", "sendglucose");
        }
    }

}
