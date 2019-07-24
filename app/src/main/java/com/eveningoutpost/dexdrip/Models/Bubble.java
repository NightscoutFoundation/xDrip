package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.BridgeResponse;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 */

public class Bubble {
    private static final String TAG = "DexCollectionService";//?????"Bubble";

    private static final String CHECKSUM_FAILED = "checksum failed";

    private enum TOMATO_STATES {
        REQUEST_DATA_SENT,
        RECIEVING_DATA
    }



    static volatile TOMATO_STATES s_state;

    private static volatile long s_lastReceiveTimestamp;
    private static volatile byte[] s_full_data = null;
    private static volatile ArrayList<Byte> bytes = null;
    private static volatile int s_acumulatedSize = 0;
    private static volatile boolean s_recviedEnoughData;


    public static boolean isBubble() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice == null || activeBluetoothDevice.name == null) {
            return false;
        }
        return activeBluetoothDevice.name.contentEquals("Bubble");
    }

    public static BridgeResponse getSNRespones() {
        final BridgeResponse reply = new BridgeResponse();
        ByteBuffer ackMessage = ByteBuffer.allocate(3);
        ackMessage.put(0, (byte) 0x04);
        ackMessage.put(1, (byte) 0x00);
        ackMessage.put(2, (byte) 0x01);
        reply.add(ackMessage);
        return reply;
    }

    public static BridgeResponse getBubbleResponse() {
//        Log.e("xxxxxx", "getBubbleResponse");
        final BridgeResponse reply = new BridgeResponse();
        ByteBuffer ackMessage = ByteBuffer.allocate(6);
        ackMessage.put(0, (byte) 0x02);
        ackMessage.put(1, (byte) 0x01);
        ackMessage.put(2, (byte) 0x00);
        ackMessage.put(3, (byte) 0x00);
        ackMessage.put(4, (byte) 0x00);
        ackMessage.put(5, (byte) 0x2B);
        reply.add(ackMessage);

//        reply.setDelay(10000);
        return reply;
    }

    public static int lens = 344;

    public static String serNumber(String uid) {
        StringBuilder temp2 = new StringBuilder();
        for (int i = uid.length(); i > 0; i = i - 2) {
            String temp = uid.substring(i - 2, i);
//            System.out.println(temp);
//            System.out.println(i);
            temp2.append(temp);
        }
        uid = temp2.toString();
        StringBuilder sb = new StringBuilder();
        try {
            char snParts[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'C', 'D', 'E', 'F',
                    'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
            String uidStr = uid.substring(4, uid.length());
            long number = Long.parseLong(uidStr, 16);
            String s = Long.toBinaryString(number) + "00";
//            System.out.println(s);

            for (int i = 0; i < s.length(); i = i + 5) {
//                System.out.println(i);
                String temp = s.substring(i, i + 5);
                int index = toInt(temp);
                sb.append(snParts[index]);

            }
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
        return "0" + sb.toString();
    }

    public static int toInt(String bi) {
        int len = bi.length();
        int sum = 0;
        int tmp, max = len - 1;
        for (int i = 0; i < len; ++i) {
            tmp = bi.charAt(i) - '0';
            sum += tmp * Math.pow(2, max--);
        }
        return sum;
    }

    static int errorCount = 0;

    public static BridgeResponse decodeBubblePacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        int count = Pref.getInt("Cobblecount", 0);
//        Log.e(TAG, "decodeBubblePacket" + Arrays.toString(buffer));
        if (buffer[0] == -128) {
            PersistentStore.setString("Bubblebattery", Integer.toString(buffer[4]));
            Pref.setInt("bridge_battery", buffer[4]);
//            String data=DexCollectionService.bytesToHexString(buffer);
            PersistentStore.setString("BubbleHArdware", Integer.toString(buffer[2]) + ".0");
            PersistentStore.setString("BubbleFirmware", Integer.toString(buffer[1]) + ".0");
//            PersistentStore.setString("LibreSN", SensorSn);
            ByteBuffer ackMessage = ByteBuffer.allocate(6);
            ackMessage.put(0, (byte) 0x02);
            ackMessage.put(1, (byte) 0x01);
            ackMessage.put(2, (byte) 0x00);
            ackMessage.put(3, (byte) 0x00);
            ackMessage.put(4, (byte) 0x00);
            ackMessage.put(5, (byte) 0x2B);
            reply.add(ackMessage);
            bytes = null;
            dateAll = false;
            s_full_data = null;
            Log.e(TAG, "reset data");
            return getBubbleResponse();
        }
        if (buffer[0] == -64) {
            int i2 = 0;
            ArrayList<Byte> bytett = new ArrayList();
            for (byte b : buffer) {
                if (i2 >= 2) {
                    bytett.add(b);
                }
                i2++;
            }
            Byte[] byte2 = new Byte[4];
            String UUID = HexDump.dumpHexString(bytett.subList(0, 8).toArray(byte2));
            String SensorSn = serNumber(UUID);
            if (!SensorSn.equals(PersistentStore.getString("LibreSN"))) {
                Sensor.stopSensor();
                Sensor.create(System.currentTimeMillis());
            }
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
            if (dateAll) {
                count = count + 1;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
                Pref.setInt("Cobblecount", count);
                dateAll = false;
                s_full_data = null;
                bytes = new ArrayList();
                errorCount = 0;
            }
            s_state = TOMATO_STATES.RECIEVING_DATA;
            return reply;

        }

        if (buffer[0] == -65) {
            int count2 = Pref.getInt("CobbleNoSensor", 0);
            count2 = count2 + 1;
            Pref.setInt("CobbleNoSensor", count2);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Log.e(TAG, "No sensor has been found");
            reply.setError_message(gs(R.string.no_sensor_found));
            dateAll = false;
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


    public static boolean dateAll = false;

    static void AreWeDone() {
        if (s_recviedEnoughData) {
            return;
        }
        if (s_acumulatedSize < lens) {
            return;
        }
        Byte[] byte1 = new Byte[]{};
        String data2 = HexDump.dumpHexString(bytes.subList(0, 344).toArray(byte1));
        Log.e(TAG, data2);
        byte[] data = HexDump.hexStringToByteArray(data2);
        s_recviedEnoughData = true;
        long now = JoH.tsl();

        String SensorSn = PersistentStore.getString("LibreSN");
        bytes = new ArrayList<>();
        flag = true;
        int expectedSize = lens;
        InitBuffer(expectedSize);
        dateAll = true;
        boolean checksum_ok = NFCReaderX.HandleGoodReading(SensorSn, data, now, true);
        Log.e(TAG, "We have all the data that we need " + s_acumulatedSize + " checksum_ok = " + checksum_ok + HexDump.dumpHexString(data));
        if (!checksum_ok) {
            int count = Pref.getInt("CobbleCheckFlase", 0);
            count = count + 1;
            Pref.setInt("CobbleCheckFlase", count);
        }

    }



    static void InitBuffer(int expectedSize) {
        s_full_data = new byte[expectedSize];
        s_acumulatedSize = 0;
        s_recviedEnoughData = false;

    }

    public static ArrayList<ByteBuffer> initialize() {
        Log.e(TAG, "initialize!");
        Pref.setInt("bridge_battery", 0); //force battery to no-value before first reading
        return resetBubbleState();
    }

    private static ArrayList<ByteBuffer> resetBubbleState() {
        ArrayList<ByteBuffer> ret = new ArrayList<>();

        s_state = TOMATO_STATES.REQUEST_DATA_SENT;
        // Make Bubble send data every 5 minutes
        ByteBuffer ackMessage = ByteBuffer.allocate(3);
        ackMessage.put(0, (byte) 0x00);
        ackMessage.put(1, (byte) 0x01);
        ackMessage.put(2, (byte) 0x05);
        ret.add(ackMessage);
        return ret;
    }
}
