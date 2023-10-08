package com.eveningoutpost.dexdrip.models;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.BridgeResponse;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.LibreUtils;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

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
    private final static int TOMATO_PATCH_INFO = 6;

    
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

        return activeBluetoothDevice.name.startsWith("miaomiao")
                || activeBluetoothDevice.name.toLowerCase().startsWith("watlaa");
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
                InitBuffer(expectedSize + TOMATO_PATCH_INFO);
                addData(buffer);
                if((JoH.ratelimit("tomato-full-retry",60)
                        || JoH.ratelimit("tomato-full-retry2",60))) {
                    reply.SetStillWaitingForData();
                }
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
                if(s_recviedEnoughData) {
                    reply.SetGotAllData();
                }
                
            } catch (RuntimeException e) {
                // if the checksum failed lets ask for the data set again but not more than once per minute
                if (e.getMessage().equals(CHECKSUM_FAILED)) {
                    // Nothing that needs to be done here since we will now ask for a new reading, if we don't get all data.

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
                    " buffer.length = " + buffer.length + " s_full_data.length " + s_full_data.length);
            //??? send something to start back??
            return;
            
        }
        System.arraycopy(buffer, 0, s_full_data, s_acumulatedSize, buffer.length);
        s_acumulatedSize += buffer.length;
        AreWeDone();
    }
    
    static void AreWeDone() {
        // Give both versions a chance to work.
        final int extended_length = Constants.LIBRE_1_2_FRAM_SIZE + TOMATO_HEADER_LENGTH + 1 + TOMATO_PATCH_INFO;
        if(s_recviedEnoughData && (s_acumulatedSize != extended_length))  {
            // This reading already ended
            Log.e(TAG,"Getting out, as s_recviedEnoughData and we have too much data already s_acumulatedSize = " + s_acumulatedSize);
            return;
        }

        if(s_acumulatedSize < Constants.LIBRE_1_2_FRAM_SIZE + TOMATO_HEADER_LENGTH + 1 ) {
            //Log.e(TAG,"Getting out, since not enough data s_acumulatedSize = " + s_acumulatedSize);
            return;   
        }
        byte[] data = Arrays.copyOfRange(s_full_data, TOMATO_HEADER_LENGTH, TOMATO_HEADER_LENGTH + Constants.LIBRE_1_2_FRAM_SIZE);
        s_recviedEnoughData = true;
        
        long now = JoH.tsl();
        // Important note, the actual serial number is 8 bytes long and starts at addresses 5.
        final String SensorSn = LibreUtils.decodeSerialNumberKey(Arrays.copyOfRange(s_full_data, 5, 13));
        byte []patchUid = null;
        byte []patchInfo = null;
        if(s_acumulatedSize >= extended_length) {
            patchUid = Arrays.copyOfRange(s_full_data, 5, 13);
            patchInfo = Arrays.copyOfRange(s_full_data, TOMATO_HEADER_LENGTH+ Constants.LIBRE_1_2_FRAM_SIZE + 1,
                    TOMATO_HEADER_LENGTH + Constants.LIBRE_1_2_FRAM_SIZE + 1+ TOMATO_PATCH_INFO);
        }
        Log.d(TAG, "patchUid = " + HexDump.dumpHexString(patchUid));
        Log.d(TAG, "patchInfo = " + HexDump.dumpHexString(patchInfo));
        PersistentStore.setString("Tomatobattery", Integer.toString(s_full_data[13]));
        Pref.setInt("bridge_battery", s_full_data[13]);
        // Set the time of the current reading
        PersistentStore.setLong("libre-reading-timestamp", JoH.tsl());

        if(NFCReaderX.use_fake_de_data()) {
            FakeData libreData = FakeLibreData.getInstance().getFakeData();
            data = libreData.data;
            patchUid = libreData.patchUid;
            patchInfo = libreData.patchInfo;
        }

        boolean checksum_ok = NFCReaderX.HandleGoodReading(SensorSn, data, now, true, patchUid, patchInfo);
        Log.e(TAG, "We have all the data that we need " + s_acumulatedSize + " checksum_ok = " + checksum_ok + HexDump.dumpHexString(data));

        if(!checksum_ok) {
            throw new RuntimeException(CHECKSUM_FAILED);
        }

        if (SensorSanity.checkLibreSensorChangeIfEnabled(SensorSn)) {
            Log.e(TAG,"Problem with Libre Serial Number - not processing");
            throw new RuntimeException(SERIAL_FAILED);
        }

        PersistentStore.setString("TomatoHArdware",HexDump.toHexString(s_full_data,16,2));
        PersistentStore.setString("TomatoFirmware",HexDump.toHexString(s_full_data,14,2));
        PersistentStore.setString("LibreSN", SensorSn);
        PersistentStore.setString("EXTERNAL_ALG_PACKAGES", "com.hg4.oopalgorithm.oopalgorithm2");
        
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
        if(s_full_data.length < Constants.LIBRE_1_2_FRAM_SIZE + TOMATO_HEADER_LENGTH + 1) {
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

    public static ArrayList<ByteBuffer> resetTomatoState() {
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
