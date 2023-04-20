package com.eveningoutpost.dexdrip.utilitymodels;


import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.LIBRE_MULTIPLIER;

/**
 * Created by jamorham on 27/03/2017.
 */

/*
 Helper class to support xBridge protocol extensions
 */

public class XbridgePlus {

    private static final String TAG = "XbridgePlus";

    private static final int LEN = 0;
    private static final int CMD = 1;
    private static final int SUB = 2;
    private static final int TREND = 3;


    private static final byte CMD_XBRIDGE_PLUS = 0x02;

    private static final byte SUB_REQUEST = 0x00;

    private static final byte REQUEST_DATA = 0x00;
    private static final byte LIBRE_STATUS = 0x01;
    private static final byte REQUESTED_LAST15_PART1 = 0x02;
    private static final byte REQUESTED_LAST15_PART2 = 0x03;
    private static final byte REQUESTED_LAST8H_PART1 = 0x04;
    private static final byte REQUESTED_LAST8H_PART2 = 0x05;
    private static final byte REQUESTED_LAST8H_PART3 = 0x06;
    private static final byte REQUESTED_LAST8H_PART4 = 0x07;


    public static byte[] sendDataRequestPacket() {
        return new byte[]{0x0c, CMD_XBRIDGE_PLUS, SUB_REQUEST, REQUEST_DATA, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }

    public static byte[] sendLast15ARequestPacket() {
        return new byte[]{0x0c, CMD_XBRIDGE_PLUS, SUB_REQUEST, REQUESTED_LAST15_PART1, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }

    public static byte[] sendLast15BRequestPacket() {
        return new byte[]{0x0c, CMD_XBRIDGE_PLUS, SUB_REQUEST, REQUESTED_LAST15_PART2, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }

    public static boolean isXbridgePacket(byte[] buffer) {
        if (((buffer.length > 1) && (buffer[0] == 0x07 || buffer[0] == 0x11 || buffer[0] == 0x15))) {
            if (buffer[0] == 0x07 && buffer[1] == -15) return true; // beacon
            if (((buffer[0] == 0x11 || buffer[0] == 0x15) && buffer[1] == 0x00) && buffer.length >= 0x11)
                return true; // data or buffered data
        }
        return false;
    }

    public static boolean isXbridgeExtensionPacket(byte[] buffer) {
        return buffer.length == 19
                && unsignedByteToInt(buffer[LEN]) == buffer.length
                && unsignedByteToInt(buffer[CMD]) == CMD_XBRIDGE_PLUS;
    }

    public static byte[] decodeXbridgeExtensionPacket(byte[] buffer) {
        if (isXbridgeExtensionPacket(buffer)) {
            switch (buffer[SUB]) {

                case REQUESTED_LAST15_PART1:
                    UserError.Log.d(TAG, "Received last 15 minute part A reply packet");
                    process15minData(buffer, TREND, 0, 8);
                    break;
                case REQUESTED_LAST15_PART2:
                    UserError.Log.d(TAG, "Received last 15 minute part B reply packet");
                    process15minData(buffer, TREND, 7, 8);
                    if (JoH.ratelimit("last-requested-xbridge-part1", 120)) {
                        UserError.Log.d(TAG, "Requesting Last15 part 1");
                        return sendLast15ARequestPacket();
                    }
                    break;

                default:
                    UserError.Log.d(TAG, "Unrecoginized xBridge+ packet type: " + unsignedByteToInt(buffer[SUB]));
            }
        }
        return null;
    }

    private static void process15minData(byte[] buffer, int src_offset, int min_offset, int count) {
        long timestamp = JoH.tsl();
        for (int i = src_offset; i < (src_offset + (count * 2)); i = i + 2) {
            double val = LIBRE_MULTIPLIER * (unsignedBytesToInt(buffer[i], buffer[i + 1]) & 0xFFF);
            UserError.Log.d(TAG, "Received 15 min value: " + JoH.qs(val, 4) + " for minute: " + min_offset);

            final long this_timestamp = timestamp - (min_offset * Constants.MINUTE_IN_MS);
            // TODO we may want to use getForPreciseTimestamp instead..
            if (BgReading.readingNearTimeStamp(this_timestamp) == null) {
                UserError.Log.d(TAG, "Creating a new reading at: " + JoH.dateTimeText(this_timestamp));
                BgReading.create(val, val, xdrip.getAppContext(), this_timestamp, min_offset != 0);
            } else {
                UserError.Log.d(TAG, "Already a reading for minute offset: " + min_offset);
            }

            min_offset++;
        }
    }


    static private int unsignedByteToInt(byte b) {
        return b & 0xFF;
    }

    static private int unsignedBytesToInt(byte b, byte c) {
        return ((unsignedByteToInt(c) << 8) + unsignedByteToInt(b)) & 0xFFFF;
    }

}
