package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.LibreUtils;

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
        public PatchState state;
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
        }

        Log.i(TAG, "Product type = " + type + " (family = " + productType + ")");
        Log.i(TAG, "Security version = " + securityVersion);

        String msg = "Localization = 0x" + Integer.toHexString(localization) + " (region = " + region;
        if (subregion != 0) {
            msg += ", subregion = 0x" + Integer.toHexString(subregion);
            if (subregion == 0xC0) {
                msg += " (France)";
            }
        }
        Log.i(TAG, msg + ")");

        int generation = (patchInfo[4] & 0xFF) + ((patchInfo[5] & 0xFF) << 8);
        String model = type;
        if (generation == 1 && type.equals("Libre 3")) {
            model += " Plus";
        }
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

    public static class ActivationResponse {
        public byte[] bdAddress; // 6-byte Bluetooth device address (MAC)
        public byte[] BLE_Pin; // 4 bytes
        public long activationTime; // 4-byte UNIX timestamp
    }

    public static byte[] buildActivationCommand(byte manufacturerCode, long activationTime, long receiverID) {
        byte[] nfcActivationCommand = new byte[]{0x02, (byte) 0xA0, manufacturerCode};
        byte[] parameters = new byte[10];

        long time = (activationTime != 0 ? activationTime : System.currentTimeMillis() / 1000) - 1;
        parameters[0] = (byte) (time & 0xFF);
        parameters[1] = (byte) ((time >> 8) & 0xFF);
        parameters[2] = (byte) ((time >> 16) & 0xFF);
        parameters[3] = (byte) ((time >> 24) & 0xFF);

        parameters[4] = (byte) (receiverID & 0xFF);
        parameters[5] = (byte) ((receiverID >> 8) & 0xFF);
        parameters[6] = (byte) ((receiverID >> 16) & 0xFF);
        parameters[7] = (byte) ((receiverID >> 24) & 0xFF);

        long crc = LibreUtils.computeCRC16(parameters, -2, parameters.length);
        parameters[8] = (byte) (crc & 0xFF);
        parameters[9] = (byte) ((crc >> 8) & 0xFF);

        byte[] activationCommand = Arrays.copyOf(nfcActivationCommand, nfcActivationCommand.length + parameters.length);
        System.arraycopy(parameters, 0, activationCommand, nfcActivationCommand.length, parameters.length);

        return activationCommand;
    }

    public static void parseActivationResponse(byte[] response) {
        if (response == null || response.length == 0) return;

        int start = 0;
        while (start < response.length && (response[start] & 0xFF) == 0xA5) {
            start++;
        }

        if (start >= response.length) return;

        byte flag = response[start];
        response = Arrays.copyOfRange(response, start + 1, response.length);

        if (flag == 0x01 && response.length == 1) {

            Log.e(TAG, "Activation error code = 0x" + JoH.bytesToHex(response));
            // getting 0xb0 / 0xb2 on an expired sensor
            // getting 0xb1 on a sensor activated by the reader
            // getting NFC error 0xc2 when altering crc16
            // getting NFC error 0xc1 when omitting crc16
            return;
        }

        if (flag == 0x00 && response.length == 16) {

            ActivationResponse activationResponse = new ActivationResponse();
            activationResponse.bdAddress = JoH.reverseBytes(Arrays.copyOfRange(response, 0, 6));
            activationResponse.BLE_Pin = Arrays.copyOfRange(response, 6, 10);
            activationResponse.activationTime = (response[10] & 0xFFL) | ((response[11] & 0xFFL) << 8) | ((response[12] & 0xFFL) << 16) | ((response[13] & 0xFFL) << 24);

            int crc = ((response[14] & 0xFF) | ((response[15] & 0xFF) << 8));
            long computedCrc = LibreUtils.computeCRC16(response, -2, 16);

            Log.i(TAG, "Activation response: BLE address = " + JoH.bytesToHexMacFormat(activationResponse.bdAddress) +
                    ", BLE PIN = " + JoH.bytesToHex(activationResponse.BLE_Pin) +
                    ", activation time = " + JoH.dateTimeText(activationResponse.activationTime * 1000) +
                    ", CRC = " + Integer.toHexString(crc) +
                    ", computed CRC = " + Long.toHexString(computedCrc));

            if (crc != computedCrc) {
                Log.e(TAG, "Activation response CRC mismatch!");
            }
        }
    }

}
