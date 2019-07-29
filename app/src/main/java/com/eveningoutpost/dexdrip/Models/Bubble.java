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
    private static volatile ArrayList<Byte> bytes = null;
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


    static int errorCount = 0;

    public static BridgeResponse decodeBubblePacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        int count = Pref.getInt("Cobblecount", 0);
        if (buffer[0] == -128) {
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
            bytes = null;
            s_full_data = null;
            Log.e(TAG, "reset data");
            return getBubbleResponse();
        }
        if (buffer[0] == -64) {
//            int i2 = 0;
//            ArrayList<Byte> bytett = new ArrayList();
//            for (byte b : buffer) {
//                if (i2 >= 2) {
//                    bytett.add(b);
//                }
//                i2++;
//            }
            String SensorSn = LibreUtils.decodeSerialNumberKey(Arrays.copyOfRange(buffer, 2, 10));
//            if (!SensorSn.equals(PersistentStore.getString("LibreSN"))) {
//                Sensor.stopSensor();
//                Sensor.create(System.currentTimeMillis());
//            }
            PersistentStore.setString("LibreSN", SensorSn);
            return reply;
        }
        if (buffer[0] == -126) {
            int expectedSize = lens;

            if (s_full_data == null) {
                InitBuffer(expectedSize);
            }
            if (bytes == null) {
                bytes = new ArrayList();
            }
            addData(buffer);
            PersistentStore.setBoolean("CobbleNoSensor", false);
            return reply;

        }

        if (buffer[0] == -65) {
            int count2 = Pref.getInt("CobbleNoSensor", 0);
            count2 = count2 + 1;
            Pref.setInt("CobbleNoSensor", count2);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.e(TAG, "No sensor has been found");
            reply.setError_message(gs(R.string.no_sensor_found));
            s_full_data = null;
            flag = true;
            bytes = new ArrayList();
            errorCount++;
            if (errorCount <= 2) {
                return getBubbleResponse();
            }
            PersistentStore.setBoolean("CobbleNoSensor", true);
            return reply;
        }


        return reply;
    }

    static boolean flag = true;

    static void addData(byte[] buffer) {
        int i2 = 0;
        for (byte b : buffer) {
            if (i2 >= 4) {
                bytes.add(b);
                s_acumulatedSize++;
            }
            i2++;
        }
        AreWeDone();
    }



    static void AreWeDone() {
        if (s_acumulatedSize < lens) {
            return;
        }
        Byte[] byte1 = new Byte[]{};
        String data2 = HexDump.bytesToHexString(bytes.subList(0, 344).toArray(byte1));
        byte[] data = HexDump.hexStringToBytes(data2);
        long now = JoH.tsl();

        String SensorSn = PersistentStore.getString("LibreSN");
        bytes = new ArrayList<>();
        flag = true;
        int expectedSize = lens;
        InitBuffer(expectedSize);
        boolean checksum_ok = NFCReaderX.HandleGoodReading(SensorSn, data, now, true);
        errorCount = 0;
        Log.e(TAG, "We have all the data that we need " + s_acumulatedSize + " checksum_ok = " + checksum_ok + data2);
        if (!checksum_ok) {
            int count = Pref.getInt("CobbleCheckFlase", 0);
            count = count + 1;
            Pref.setInt("CobbleCheckFlase", count);
        }

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
