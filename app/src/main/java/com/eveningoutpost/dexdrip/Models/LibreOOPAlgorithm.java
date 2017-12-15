package com.eveningoutpost.dexdrip.Models;

import java.io.IOException;
import java.util.Arrays;

import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.utils.FileUtils;

import android.content.Intent;
import android.nfc.tech.NfcV;
import android.os.Bundle;

public class LibreOOPAlgorithm {
	
	private static final String TAG = "LibreOOPAlgorithm";
	
	static private byte[] getPatchInfo(NfcV handle) {
        byte[] bArr = null;
            byte[] response = tranceive3Times(handle, new byte[]{(byte) 2, (byte) -95, (byte) 7, (byte) -62, (byte) -83, (byte) 117, (byte) 33});
            if (resultOk(response)) {
                bArr = Arrays.copyOfRange(response, 1, response.length);
            }
        return bArr;
    }
    
    private static boolean resultOk(byte[] result) {
        return result != null && result.length > 0 && (result[0] & 1) == 0;
    }
    
    static private byte[] tranceive3Times(NfcV handle, byte[] command) {
        byte[] response = null;
        for(int i = 0; i < 3; i++) {
			try {
			    response = handle.transceive(command);
			} catch (IOException e) {
			    Log.e(TAG, "Cought exception on tranceive3Times - retrying");
			}
			if (resultOk(response)) {
				Log.e(TAG, "tranceive3Times returned good buffer");
			    return response;
			}
			i++;
        }
        return null;
    }
    
    static private byte[] readPatch(NfcV handle)  {
        byte[] result = new byte[0X158];
        int totalBlocksToRead = 0X158 / 8;
        int blockIndex = 0;
        while (blockIndex < totalBlocksToRead) {
            int numBlocksToRead = Math.min(3, totalBlocksToRead - blockIndex);
            byte[] response = tranceive3Times(handle, new byte[]{(byte) 2, (byte) 35, (byte) blockIndex, (byte) (numBlocksToRead - 1)});
            if (!resultOk(response) || response.length < (numBlocksToRead * 8) + 1) {
                return null;
            }
            System.arraycopy(response,1, result, 8 * blockIndex, 8 * numBlocksToRead);
            blockIndex += numBlocksToRead;
            
        }
        return result;
    }
    
    static public boolean readOOPData(NfcV handle) {
    	Log.d(TAG, "readOOPData called");
    	getPatchInfo(handle);
    	byte[] fullData = readPatch(handle);
        UserError.Log.e(TAG, "Full data that was received is " + HexDump.dumpHexString(fullData));
        if(fullData != null) {
        	FileUtils.writeToFileWithCurrentDate(TAG, "xDripData", fullData);
        	SendData(fullData);
        }
        
    	return fullData != null;
    }
    
    static public void SendData(byte[] fullData) {
    	Log.i(TAG, "Sending full data to OOP Algorithm");
    	Intent intent = new Intent(Intents.XDRIP_PLUS_LIBRE_DATA);
        Bundle bundle = new Bundle();
        bundle.putByteArray(Intents.LIBRE_DATA_BUFFER, fullData);
        bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, JoH.tsl());
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }
}
