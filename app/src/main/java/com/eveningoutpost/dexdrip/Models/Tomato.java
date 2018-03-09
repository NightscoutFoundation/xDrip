package com.eveningoutpost.dexdrip.Models;
import com.eveningoutpost.dexdrip.NFCReaderX;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;

/**
 * Created by Tzachi Dar on 7.3.2018.
 */

public class Tomato {
    private static final String TAG = "DexCollectionService";//?????"Tomato";

    private static enum TOMATO_STATES {
        REQUEST_DATA_SENT,
        RECIEVING_DATA
    }
    final static int TOMATO_HEADER_LENGTH = 18;

    
    static TOMATO_STATES s_state;
    
    static long s_lastReceiveTimestamp;
    private static byte[] s_full_data = null;
    private static int s_acumulatedSize = 0;
    private static boolean s_recviedEnoughData;
    
    
    
    public static boolean isTomato() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if(activeBluetoothDevice == null || activeBluetoothDevice.name == null) {
            return false;
        }
        
        return activeBluetoothDevice.name.contentEquals("miaomiao");
    }

    public static ArrayList<ByteBuffer> decodeTomatoPacket(byte[] buffer, int len) {
        ArrayList<ByteBuffer> retArray = new ArrayList<ByteBuffer>();
        // Check time, probably need to start on sending
        long now = JoH.tsl();
        if(now - s_lastReceiveTimestamp > 10*1000) {
            // We did not receive data in 10 seconds, moving to init state again
            Log.e(TAG, "Recieved a buffer after " + (now - s_lastReceiveTimestamp) +  " seconds, starting again");
            s_state = TOMATO_STATES.REQUEST_DATA_SENT;
        }
        
        s_lastReceiveTimestamp = now;
        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeTomatoPacket");
            return retArray;
        } 
        if (s_state == TOMATO_STATES.REQUEST_DATA_SENT) {
            if(buffer.length == 1 && buffer[0] == 0x32) {
                Log.e(TAG, "returning allow sensor confirm");
                
                ByteBuffer allowNewSensor = ByteBuffer.allocate(2);
                allowNewSensor.put(0, (byte) 0xD3);
                allowNewSensor.put(1, (byte) 0x01);
                retArray.add(allowNewSensor);
                
                // For debug, make it send data every minute (did not work...)
                ByteBuffer newFreqMessage = ByteBuffer.allocate(2);
                newFreqMessage.put(0, (byte) 0xD1);
                newFreqMessage.put(1, (byte) 0x01);
                retArray.add(newFreqMessage);
                
                //command to start reading
                ByteBuffer ackMessage = ByteBuffer.allocate(1);
                ackMessage.put(0, (byte) 0xF0);
                retArray.add(ackMessage);
                return retArray;
            }
            
            if(buffer.length == 1 && buffer[0] == 0x34) {
                Log.e(TAG, "No sensor has been found");
                return retArray;
            }
            
            // 18 is the expected header size
            if(buffer.length >= 18 && buffer[0] == 0x28) {
                // We are starting to receive data, need to start accumulating
                
                // &0xff is needed to convert to hex.
                int expectedSize = 256 * (int)(buffer[1] & 0xFF) + (int)(buffer[2] & 0xFF);
                Log.e(TAG, "Starting to acumulate data expectedSize = " + expectedSize);
                InitBuffer(expectedSize);
                addData(buffer);
                s_state = TOMATO_STATES.RECIEVING_DATA;
                return retArray;
                
            }
        }
        
        if (s_state == TOMATO_STATES.RECIEVING_DATA) {
            //Log.e(TAG, "received more data s_acumulatedSize = " + s_acumulatedSize + " current buffer size " + buffer.length);
            addData(buffer);
            

            return retArray;
        }
        
        Log.wtf(TAG, "Very strange, In an unexpeted state " + s_state);
        
        return retArray;
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

        if(s_acumulatedSize >= 344 + TOMATO_HEADER_LENGTH + 1) {
            Log.e(TAG, "We have the data that we want");
            byte[] data = Arrays.copyOfRange(s_full_data, TOMATO_HEADER_LENGTH, TOMATO_HEADER_LENGTH+344);
            Log.e(TAG, "We have all the data that we need" + HexDump.dumpHexString(data));
            s_recviedEnoughData = true;
            
            NFCReaderX.HandleGoodReading("tomato", data);
        }
        
        /*
        
        This is the correct code that should run. Unfortunately, we don't get all the data, so we need
        to make a compromise, and use the code above.
        
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
        
        // ????? get more data now that we have all that we need
        
        // ????? call NFCReaderX.onPostExecute() ??????
        
        
        
        byte[] data = Arrays.copyOfRange(s_full_data, TOMATO_HEADER_LENGTH, TOMATO_HEADER_LENGTH+344);
        Log.e(TAG, "We have all the data that we need" + HexDump.dumpHexString(data));
        long now = JoH.tsl();
        // Save raw block record (we start from block 0)
        LibreBlock.createAndSave("tomato", now, data, 0);

        if(Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            LibreOOPAlgorithm.SendData(data, now);
        }
        */
        
    }
    
    static void InitBuffer(int expectedSize) {
        s_full_data = new byte[expectedSize];
        s_acumulatedSize = 0;
        s_recviedEnoughData = false;

    }
    
    public static ArrayList<ByteBuffer> initialize() {
        Log.i(TAG, "initialize!");
        Pref.setInt("bridge_battery", 0); //force battery to no-value before first reading
        ArrayList<ByteBuffer> ret = new ArrayList<ByteBuffer>();

        s_state = TOMATO_STATES.REQUEST_DATA_SENT;
        
        // For debug, make it send data every minute (ERROR - We fail to send this message... needs more work,
        // not sure it works at all)
        ByteBuffer newFreqMessage = ByteBuffer.allocate(2);
        newFreqMessage.put(0, (byte) 0xDD);
        newFreqMessage.put(1, (byte) 0x01);
        ret.add(newFreqMessage);
        
        //command to start reading
        ByteBuffer ackMessage = ByteBuffer.allocate(1);
        ackMessage.put(0, (byte) 0xF0);
        ret.add(ackMessage);
        return ret;
    }
}
