package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.BridgeResponse;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 *
 */

public class Bubble {
    private static final String TAG = "Bubble";//?????"Bubble";

    private static volatile byte[] s_full_data = null;
    private static volatile int s_acumulatedSize = 0;

    public static boolean isBubble() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice == null || activeBluetoothDevice.name == null) {
            return false;
        }
        return activeBluetoothDevice.name.contentEquals("Bubble");
    }


    public static BridgeResponse getBubbleResponse() {
        final BridgeResponse reply = new BridgeResponse();
        ByteBuffer ackMessage = ByteBuffer.allocate(6);
        ackMessage.put(0, (byte) 0x02);
        ackMessage.put(1, (byte) 0x01);
        ackMessage.put(2, (byte) 0x00);
        ackMessage.put(3, (byte) 0x00);
        ackMessage.put(4, (byte) 0x00);
        ackMessage.put(5, (byte) 0x2B);
        reply.add(ackMessage);
        return reply;
    }

    public static int lens = 344;
    public static int BUBBLE_FOOTER = 8;

    static int errorCount = 0;


    static byte[] patchUid = null;
    static byte[] patchInfo = null;

    public static BridgeResponse decodeBubblePacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        int first = 0xff & buffer[0];
        if (first == 0x80) {
            PersistentStore.setString("Bubblebattery", Integer.toString(buffer[4]));
            Pref.setInt("bridge_battery", buffer[4]);
            String bubblefirmware = buffer[2] + "." + buffer[3];
            String bubbleHArdware = buffer[buffer.length-2] + "." + buffer[buffer.length-1];
            PersistentStore.setString("BubbleHArdware", bubbleHArdware);
            PersistentStore.setString("BubbleFirmware", bubblefirmware);
            ByteBuffer ackMessage = ByteBuffer.allocate(6);
            ackMessage.put(0, (byte) 0x02);
            ackMessage.put(1, (byte) 0x01);
            ackMessage.put(2, (byte) 0x00);
            ackMessage.put(3, (byte) 0x00);
            ackMessage.put(4, (byte) 0x00);
            ackMessage.put(5, (byte) 0x2B);
            reply.add(ackMessage);
            s_full_data = null;
            return getBubbleResponse();
        }
        if (first == 0xC0) {
            patchUid = Arrays.copyOfRange(buffer, 2, 10);
            String SensorSn = LibreUtils.decodeSerialNumberKey(patchUid);
            PersistentStore.setString("LibreSN", SensorSn);
            return reply;
        }
        if (first == 0xC1) {
            double fv = JoH.tolerantParseDouble(PersistentStore.getString("BubbleFirmware"));
            if (fv < 1.35) {
                patchInfo = Arrays.copyOfRange(buffer, 3, 9);
            } else {
                if (buffer.length >= 11) {
                    patchInfo = Arrays.copyOfRange(buffer, 5, 11);
                }
            }
            return reply;
        }
        if (first == 0x82) {
            int expectedSize = lens + BUBBLE_FOOTER;
            if (s_full_data == null) {
                InitBuffer(expectedSize);
            }
            addData(buffer);
            return reply;

        }

        if (first == 0xBF) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.e(TAG, "No sensor has been found");
            reply.setError_message(gs(R.string.no_sensor_found));
            s_full_data = null;
            errorCount++;
            if (errorCount <= 2) {
                return getBubbleResponse();
            }
            return reply;
        }


        return reply;
    }


    static void addData(byte[] buffer) {
        System.arraycopy(buffer, 4, s_full_data, s_acumulatedSize, 16);
        s_acumulatedSize = s_acumulatedSize + buffer.length - 4;
        AreWeDone();
    }


    static void AreWeDone() {
        if (s_acumulatedSize < lens) {
            return;
        }
        long now = JoH.tsl();
        String SensorSn = PersistentStore.getString("LibreSN");


        byte[] data = Arrays.copyOfRange(s_full_data, 0, 344);

        boolean checksum_ok = NFCReaderX.HandleGoodReading(SensorSn, data, now, true, patchUid, patchInfo);
        int expectedSize = lens + BUBBLE_FOOTER;
        InitBuffer(expectedSize);
        errorCount = 0;
        Log.e(TAG, "We have all the data that we need " + s_acumulatedSize + " checksum_ok = " + checksum_ok + HexDump.dumpHexString(data));

    }


    static void InitBuffer(int expectedSize) {
        s_full_data = new byte[expectedSize];
        s_acumulatedSize = 0;
    }

    public static ArrayList<ByteBuffer> initialize() {
        Log.e(TAG, "initialize!");
        Pref.setInt("bridge_battery", 0); //force battery to no-value before first reading
        return resetBubbleState();
    }

    private static ArrayList<ByteBuffer> resetBubbleState() {
        ArrayList<ByteBuffer> ret = new ArrayList<>();

        // Make Bubble send data every 5 minutes
        ByteBuffer ackMessage = ByteBuffer.allocate(3);
        ackMessage.put(0, (byte) 0x00);
        ackMessage.put(1, (byte) 0x01);
        ackMessage.put(2, (byte) 0x05);
        ret.add(ackMessage);
        return ret;
    }
}
