package com.eveningoutpost.dexdrip.watch.thinjam;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusLine;
import com.eveningoutpost.dexdrip.ui.activities.ThinJamActivity;

import java.util.Arrays;

import lombok.val;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isEnabled;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayInfo.getInfo;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_AUDIO_I;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_AUDIO_O;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_DISP_A;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_DISP_B;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.FEATURE_TJ_DISP_C;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_TEXTBOX1;

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
    private static final String PREF_BLUEJAY_SEND_STATUS_LINE = "bluejay_send_status_line";
    private static final String PREF_BLUEJAY_SEND_BACKFILL = "bluejay_send_backfill";
    private static final String LAST_BLUEJAY_STATUSLINE = "bluejay-last-statusline";

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
        if (emptyString(mac)) return null;
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
        if (emptyString(mac)) return null;
        return Pref.getString(PREF_BLUEJAY_IDENTITY + JoH.macFormat(mac).toUpperCase(), null);
    }

    static boolean hasAuthCache() {
        return hasIdentityKey() && haveAuthKey(getMac());
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

    static boolean shouldSendStatusLine() {
        return Pref.getBooleanDefaultFalse(PREF_BLUEJAY_SEND_STATUS_LINE);
    }

    static boolean shouldSendBackfill() {
        return Pref.getBooleanDefaultFalse(PREF_BLUEJAY_SEND_BACKFILL);
    }

    public static boolean localAlarmsEnabled() {
        return Pref.getBoolean("bluejay_local_alarms", true);
    }

    public static boolean remoteApiEnabled() {
        return Pref.getBooleanDefaultFalse("bluejay_use_broadcast_api");
    }

    public static void showLatestBG() {
        if (isEnabled() && versionSufficient(getInfo(getMac()).buildNumber, FEATURE_TJ_DISP_A)) {
            if (shouldSendReadings()) {
                // already on background thread and debounced
                JoH.startService(BlueJayService.class, "function", "sendglucose");
            }
            if (shouldSendBackfill()) {
                sendBackfill();
            }
            if (shouldSendStatusLine()) {
                showStatusLine();
            }
        }
    }

    public static boolean versionSufficient(final int version, final int feature) {
        if (!hasAuthCache()) return false;
        switch (feature) {
            case FEATURE_TJ_DISP_A:
                return version > 32;
            case FEATURE_TJ_DISP_B:
                return version > 60;
            case FEATURE_TJ_DISP_C:
                return version > 2030;
            case FEATURE_TJ_AUDIO_I:
                return version > 2099;
            case FEATURE_TJ_AUDIO_O:
                return version > 2100;
            default:
                UserError.Log.e(TAG, "Request for unknown feature " + feature);
                return false;
        }
    }


    public static void showStatusLine() {
        if (isEnabled() && shouldSendStatusLine()) {
            final String currentStatusLine = StatusLine.extraStatusLine();
            final String lastStatusLine = PersistentStore.getString(LAST_BLUEJAY_STATUSLINE);
            if (!currentStatusLine.equals(lastStatusLine) || JoH.ratelimit("bj-duplicate-statusline", 300)) {
                PersistentStore.setString(LAST_BLUEJAY_STATUSLINE, currentStatusLine);
                BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_TEXTBOX1, currentStatusLine);
            }
        }
    }

    public static void sendBackfill() {
        if (isEnabled() && shouldSendBackfill()) {
            if (JoH.ratelimit("bj-sendbackfill-data", 300)) {
                JoH.startService(BlueJayService.class, "function", "sendbackfill");
            }
        }
    }

}
