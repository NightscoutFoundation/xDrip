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

public class LibreBluetooth {
    private static final String TAG = "DexCollectionService";//?????"LibreBluettoh";

    private final static int LIBRE_DATA_LENGTH = 46;

    private static volatile long s_lastReceiveTimestamp;
    private static volatile byte[] s_full_data = null;
    private static volatile int s_acumulatedSize = 0;

    public static boolean isLibreBluettoh() {
        final ActiveBluetoothDevice activeBluetoothDevice = ActiveBluetoothDevice.first();
        if (activeBluetoothDevice == null || activeBluetoothDevice.name == null) {
            return false;
        }

        return activeBluetoothDevice.name.startsWith(LibreOOPAlgorithm.getLibreDeviceName());
    }

    public static BridgeResponse decodeLibrePacket(byte[] buffer, int len) {
        final BridgeResponse reply = new BridgeResponse();
        // Check time, probably need to start on sending
        long now = JoH.tsl();
        if(now - s_lastReceiveTimestamp > 3*1000) {
            // We did not receive data in 3 seconds, moving to init state again
            Log.d(TAG, "Recieved a buffer after " + (now - s_lastReceiveTimestamp) / 1000 +  " seconds, starting again. "+
            "already acumulated " + s_acumulatedSize + " bytes.");
            s_acumulatedSize = 0;
        }
        
        s_lastReceiveTimestamp = now;
        if (buffer == null) {
            Log.e(TAG, "null buffer passed to decodeTomatoPacket");
            return reply;
        } 
        
        addData(buffer);
        return reply;
    }
    
    static void addData(byte[] buffer) {
        if(s_acumulatedSize + buffer.length > s_full_data.length) {
            Log.e(TAG, "Error recieving too much data. exiting. s_acumulatedSize = " + s_acumulatedSize + 
                    " buffer.length = " + buffer.length + " s_full_data.length " + s_full_data.length);
            return;
            
        }
        System.arraycopy(buffer, 0, s_full_data, s_acumulatedSize, buffer.length);
        s_acumulatedSize += buffer.length;
        areWeDone();
    }
    
    static void areWeDone() {
        if(s_acumulatedSize < LIBRE_DATA_LENGTH ) {
            //Log.e(TAG,"Getting out, since not enough data s_acumulatedSize = " + s_acumulatedSize);
            return;
        }

        PersistentStore.setLong("libre-reading-timestamp", JoH.tsl());
        
        Log.d(TAG, "We have all the data that we need " + s_acumulatedSize + HexDump.dumpHexString(s_full_data));

        SendData(s_full_data, JoH.tsl());
        s_acumulatedSize = 0;
    }

    static public void SendData(byte []data, long timestamp) {
        if( Pref.getBooleanDefaultFalse("external_blukon_algorithm")) {
            return;
        }
        // Send to OOP2 for drcryption.
        LibreOOPAlgorithm.logIfOOP2NotAlive();

        Libre2SensorData currentSensorData = Libre2SensorData.getSensorData(false);
        if(currentSensorData == null || currentSensorData.patchUid_ == null) {
            Log.e(TAG, "SendData - we have the data but patchUid == null");
            return;
        }

        byte []patchUid = currentSensorData.patchUid_;
        if(NFCReaderX.use_fake_de_data()) {
            patchUid =  new byte[]{(byte)0xd6, (byte)0xf1, (byte)0x0f, (byte)0x01, (byte)0x00, (byte)0xa4, (byte)0x07, (byte)0xe0};
        }

        Log.d(TAG, "SendData patchUid = " + HexDump.dumpHexString(patchUid));

        LibreOOPAlgorithm.sendBleData(data, timestamp, patchUid);
    }

    static void initBuffer(int expectedSize) {
        s_full_data = new byte[expectedSize];
        s_acumulatedSize = 0;

    }

    public static byte[] initialize() {
        Log.i(TAG, "initialize!");
        initBuffer(LIBRE_DATA_LENGTH);
        byte[] btUnlockBuffer = LibreOOPAlgorithm.getCachedBtUnlockKey(true);
        if(btUnlockBuffer != null) {
            return btUnlockBuffer;
        }
        UnlockBuffers unlockBuffers =  LibreOOPAlgorithm.sendGetBluetoothEnablePayload(true);
        if(unlockBuffers == null) {
            Log.e(TAG, "sendGetBluetoothEnablePayload returned null");
            return null;
        }
        return unlockBuffers.btUnlockBuffer;
    }
}


