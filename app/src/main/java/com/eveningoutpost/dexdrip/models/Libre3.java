package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.util.Arrays;

public class Libre3 {

    private static final String TAG = "Libre3";

    public enum PatchState {
        MANUFACTURING(0),
        STORAGE(1),
        INSERTION_DETECTION(2),
        INSERTION_FAILED(3),
        PAIRED(4),
        EXPIRED(5),
        TERMINATED_NORMAL(6),
        ERROR(7),
        ERROR_TERMINATED(8);

        private final int value;

        PatchState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static PatchState fromInt(int i) {
            for (PatchState state : PatchState.values()) {
                if (state.value == i) {
                    return state;
                }
            }
            return ERROR;
        }
    }

    public class PatchInfo {
        public int securityVersion;
        public int localization;
        public int generation;
        public int wearDuration;
        public int productType;
        public int warmupDuration;
        public byte[] fwVersion;
        public int state;
        public byte[] compressedSerialNumber; // DMX
        public int mode;
    }


    public static void parsePatchInfo(byte[] patchInfo) {
        if (patchInfo == null || patchInfo.length < 24) {
            Log.e(TAG, "Invalid patchInfo length: " + (patchInfo == null ? "null" : patchInfo.length));
            return;
        }

        int productType = patchInfo[12] & 0xFF;
        String type;
        switch (productType) {
            case 4:
                type = "Libre 3";
                break;
            case 9:
                type = "Lingo";
                break;
            default:
                type = "Unknown";
                break;
        }

        int securityVersion = (patchInfo[0] & 0xFF) + ((patchInfo[1] & 0xFF) << 8);
        int localization = (patchInfo[2] & 0xFF) + ((patchInfo[3] & 0xFF) << 8);
        String region = (localization & 0xFF) == 2 ? "USA" : "Europe";

        int subregion = (localization & 0xFF00) >> 8;
        if (subregion != 0) {
            type = "Libre Select";
            if (subregion == 0xC0) {
                Log.i(TAG, "Subregion: France (0x" + Integer.toHexString(subregion) + ")");
            }
        }

        Log.i(TAG, "Product type = " + type + " (family = " + productType + ")");
        Log.i(TAG, "Security version = " + securityVersion);
        Log.i(TAG, "Localization = " + localization + " (region = " + region + ")");

        int generation = (patchInfo[4] & 0xFF) + ((patchInfo[5] & 0xFF) << 8);
        String model = generation == 1 ? "Libre 3 Plus" : "Libre 3";
        Log.i(TAG, "Puck generation = " + generation + " (model = " + model + ")");

        int wearDuration = (patchInfo[6] & 0xFF) + ((patchInfo[7] & 0xFF) << 8);
        Log.i(TAG, "Wear duration = " + wearDuration + " minutes (" + (wearDuration / 60.0 / 24.0) + " days)");

        byte[] fwVersion = Arrays.copyOfRange(patchInfo, 8, 12);
        String firmware = String.format("%d.%d.%d.%d", fwVersion[3], fwVersion[2], fwVersion[1], fwVersion[0]);
        Log.i(TAG, "Firmware version = " + firmware);

        int warmupDuration = patchInfo[13] & 0xFF;
        int warmupTime = warmupDuration * 5;
        Log.i(TAG, "Warmup time = " + warmupTime + " minutes");

        int state = patchInfo[14] & 0xFF;
        PatchState patchState = PatchState.fromInt(state);
        Log.i(TAG, "Sensor state = " + patchState + " (raw: " + state + ")");

        byte[] compressedSerialNumber = Arrays.copyOfRange(patchInfo, 15, 24);
        String serial = new String(compressedSerialNumber);
        if (productType == 9) {
            serial = "9" + serial;
        } else if (type.equals("Libre Select")) {
            serial = "4" + serial;
        }
        Log.i(TAG, "Serial number = " + serial);
    }

    // Converts a LibreView account UUID (8-4-4-4-12 string) into a receiverID
    // see https://insulinclub.de/index.php?thread/33795-free-three-ein-xposed-lsposed-modul-mit-erweiterungen-f%C3%BCr-abbott-libre-3-dexcom/&postID=655055#post655055
    public static long computeReceiverID(String accountID) {
        int res = 0;
        for (byte el : accountID.getBytes()) {
            res = (res * 0x811C9DC5) ^ (el & 0xFF);
        }
        return (long) res & 0xFFFFFFFFL;
    }

}
