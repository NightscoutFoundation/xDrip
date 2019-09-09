package com.eveningoutpost.dexdrip.Models;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.Blukon;
import com.eveningoutpost.dexdrip.UtilityModels.BridgeResponse;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import static com.eveningoutpost.dexdrip.xdrip.gs;
/**
 * Created by Tzachi Dar on 7.3.2018.
 */

public class Tomato {
    private static final String TAG = "DexCollectionService";//?????"Tomato";

    private static final String CHECKSUM_FAILED = "checksum failed";
    private static final String SERIAL_FAILED = "serial failed";

    private enum TOMATO_STATES {
        REQUEST_DATA_SENT,
        RECIEVING_DATA
    }
    private final static int TOMATO_HEADER_LENGTH = 18;

    
    static volatile TOMATO_STATES s_state;
    
    private static volatile long s_lastReceiveTimestamp;
    private static volatile byte[] s_full_data = null;
    private static volatile int s_acumulatedSize = 0;
    private static volatile boolean s_recviedEnoughData;


    public static boolean isTomato() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice == null || activeBluetoothDevice.name == null) {
            return false;
        }

        return activeBluetoothDevice.name.startsWith("miaomiao");
    }

    public static BridgeResponse decodeTomatoPacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        // Check time, probably need to start on sending
        long now = JoH.tsl();
        if(now - s_lastReceiveTimestamp > 3*1000) {
            // We did not receive data in 3 seconds, moving to init state again
            Log.e(TAG, "Recieved a buffer after " + (now - s_lastReceiveTimestamp) / 1000 +  " seconds, starting again. "+
            "already acumulated " + s_acumulatedSize + " bytes.");
            s_state = TOMATO_STATES.REQUEST_DATA_SENT;
        }
        
        s_lastReceiveTimestamp = now;
        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeTomatoPacket");
            return reply;
        } 
        if (s_state == TOMATO_STATES.REQUEST_DATA_SENT) {
            if(buffer.length == 1 && buffer[0] == 0x32) {
                Log.e(TAG, "returning allow sensor confirm");
                
                ByteBuffer allowNewSensor = ByteBuffer.allocate(2);
                allowNewSensor.put(0, (byte) 0xD3);
                allowNewSensor.put(1, (byte) 0x01);
                reply.add(allowNewSensor);
                
                // For debug, make it send data every minute (did not work...)
                ByteBuffer newFreqMessage = ByteBuffer.allocate(2);
                newFreqMessage.put(0, (byte) 0xD1);
                newFreqMessage.put(1, (byte) 0x05);
                reply.add(newFreqMessage);
                
                //command to start reading
                ByteBuffer ackMessage = ByteBuffer.allocate(1);
                ackMessage.put(0, (byte) 0xF0);
                reply.add(ackMessage);
                return reply;
            }
            
            if(buffer.length == 1 && buffer[0] == 0x34) {
                Log.e(TAG, "No sensor has been found");
                reply.setError_message(gs(R.string.no_sensor_found));
              return reply;
            }
            
            // 18 is the expected header size
            if(buffer.length >= TOMATO_HEADER_LENGTH && buffer[0] == 0x28) {
                // We are starting to receive data, need to start accumulating
                
                // &0xff is needed to convert to hex.
                int expectedSize = 256 * (int)(buffer[1] & 0xFF) + (int)(buffer[2] & 0xFF);
                Log.e(TAG, "Starting to acumulate data expectedSize = " + expectedSize);
                InitBuffer(expectedSize);
                addData(buffer);
                s_state = TOMATO_STATES.RECIEVING_DATA;
                return reply;
                
            } else {
                if (JoH.quietratelimit("unknown-initial-packet", 1)) {
                    Log.d(TAG,"Unknown initial packet makeup received" + HexDump.dumpHexString(buffer));
                }
                return reply;
            }
        }
        
        if (s_state == TOMATO_STATES.RECIEVING_DATA) {
            //Log.e(TAG, "received more data s_acumulatedSize = " + s_acumulatedSize + " current buffer size " + buffer.length);
            try {
                addData(buffer);
            } catch (RuntimeException e) {
                // if the checksum failed lets ask for the data set again but not more than once per minute
                if (e.getMessage().equals(CHECKSUM_FAILED)) {
                   if (JoH.ratelimit("tomato-full-retry",60)
                           || JoH.ratelimit("tomato-full-retry2",60)) {
                       reply.getSend().clear();
                       reply.getSend().addAll(Tomato.resetTomatoState());
                       reply.setDelay(8000);
                       reply.setError_message(gs(R.string.checksum_failed__retrying));
                       Log.d(TAG,"Asking for retry of data");
                   }
                } else if (e.getMessage().equals(SERIAL_FAILED)) {
                    reply.setError_message("Sensor Serial Problem");
                } else throw e;
            }

            return reply;
        }
        
        Log.wtf(TAG, "Very strange, In an unexpected state " + s_state);
        
        return reply;
    }
    
    static void addData(byte[] buffer) {
        if(s_acumulatedSize + buffer.length > s_full_data.length) {
            Log.e(TAG, "Error recieving too much data. exiting. s_acumulatedSize = " + s_acumulatedSize + 
                    "buffer.length = " + buffer.length + " s_full_data.length " + s_full_data.length);
            //??? send something to start back??
            return;
            
        }
        System.arraycopy(buffer, 0, s_full_data, s_acumulatedSize, buffer.length);
        s_acumulatedSize += buffer.length;
        AreWeDone();
    }
    
    static void AreWeDone() {
        if(s_recviedEnoughData) {
            // This reading already ended
            return;
        }

        if(s_acumulatedSize < 344 + TOMATO_HEADER_LENGTH + 1) {
            return;   
        }
        byte[] data = Arrays.copyOfRange(s_full_data, TOMATO_HEADER_LENGTH, TOMATO_HEADER_LENGTH+344);
        s_recviedEnoughData = true;
        
        long now = JoH.tsl();
        // Important note, the actual serial number is 8 bytes long and starts at addresses 5.
        final String SensorSn = LibreUtils.decodeSerialNumberKey(Arrays.copyOfRange(s_full_data, 5, 13));
        boolean checksum_ok = NFCReaderX.HandleGoodReading(SensorSn, data, now, true);
        Log.e(TAG, "We have all the data that we need " + s_acumulatedSize + " checksum_ok = " + checksum_ok + HexDump.dumpHexString(data));

        if(!checksum_ok) {
            throw new RuntimeException(CHECKSUM_FAILED);
        }

        if (SensorSanity.checkLibreSensorChangeIfEnabled(SensorSn)) {
            Log.e(TAG,"Problem with Libre Serial Number - not processing");
            throw new RuntimeException(SERIAL_FAILED);
        }

        PersistentStore.setString("Tomatobattery", Integer.toString(s_full_data[13]));
        Pref.setInt("bridge_battery", s_full_data[13]);
        PersistentStore.setString("TomatoHArdware",HexDump.toHexString(s_full_data,16,2));
        PersistentStore.setString("TomatoFirmware",HexDump.toHexString(s_full_data,14,2));
        PersistentStore.setString("LibreSN", SensorSn);

        
    }

    // This is the function that we should have once we are able to read all data realiably.
    static void AreWeDoneMax() {
        
        if(s_acumulatedSize == s_full_data.length) {
            Log.e(TAG, "We have a full packet");
        } else {
            return;
        }
        if(s_full_data[s_full_data.length -1] != 0x29) {
            Log.e(TAG, "recieved full data, but last byte is not 0x29. It is " + s_full_data[s_full_data.length -1]);
            return;
        }
        // We have all the data
        if(s_full_data.length < 344 + TOMATO_HEADER_LENGTH + 1) {
            Log.e(TAG, "We have all the data, but it is not enough... s_full_data.length = " + s_full_data.length );
            return;
        }
        Log.e(TAG, "We have a full packet");
        
    }


    static void InitBuffer(int expectedSize) {
        s_full_data = new byte[expectedSize];
        s_acumulatedSize = 0;
        s_recviedEnoughData = false;

    }

    public static ArrayList<ByteBuffer> initialize() {
        Log.i(TAG, "initialize!");
        Pref.setInt("bridge_battery", 0); //force battery to no-value before first reading
        return resetTomatoState();
    }

    private static ArrayList<ByteBuffer> resetTomatoState() {
        ArrayList<ByteBuffer> ret = new ArrayList<>();

        s_state = TOMATO_STATES.REQUEST_DATA_SENT;
        
        // Make tomato send data every 5 minutes
        ByteBuffer newFreqMessage = ByteBuffer.allocate(2);    
        newFreqMessage.put(0, (byte) 0xD1);    
        newFreqMessage.put(1, (byte) 0x05);    
        ret.add(newFreqMessage);

        //command to start reading
        ByteBuffer ackMessage = ByteBuffer.allocate(1);
        ackMessage.put(0, (byte) 0xF0);
        ret.add(ackMessage);
        return ret;
    }
}
