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



    public static BridgeResponse decodeBubblePacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        int first = 0xff & buffer[0];
        if (first == 0x80) {
            PersistentStore.setString("Bubblebattery", Integer.toString(buffer[4]));
            Pref.setInt("bridge_battery", buffer[4]);
            PersistentStore.setString("BubbleHArdware", Integer.toString(buffer[2]) + ".0");
            PersistentStore.setString("BubbleFirmware", Integer.toString(buffer[1]) + ".0");
            ByteBuffer ackMessage = ByteBuffer.allocate(6);
            ackMessage.put(0, (byte) 0x02);
            ackMessage.put(1, (byte) 0x01);
            ackMessage.put(2, (byte) 0x00);
            ackMessage.put(3, (byte) 0x00);
            ackMessage.put(4, (byte) 0x00);
            ackMessage.put(5, (byte) 0x2B);
            reply.add(ackMessage);
            s_full_data = null;
            Log.e(TAG, "reset data");
            return getBubbleResponse();
        }
        if (first == 0xC0) {
            String SensorSn = LibreUtils.decodeSerialNumberKey(Arrays.copyOfRange(buffer, 2, 10));
            PersistentStore.setString("LibreSN", SensorSn);
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
        byte []patchUid = null;
        byte []patchInfo = null;
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
