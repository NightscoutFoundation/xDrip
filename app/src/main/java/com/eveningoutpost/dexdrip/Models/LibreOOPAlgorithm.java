package com.eveningoutpost.dexdrip.Models;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;

import java.util.Arrays;

public class LibreOOPAlgorithm {
	private static final String TAG = "LibreOOPAlgorithm";
	
    static public void SendData(byte[] fullData) {
    	Log.i(TAG, "Sending full data to OOP Algorithm data-len = " + fullData.length);
    	
    	fullData = java.util.Arrays.copyOfRange(fullData, 0, 0x158);
    	Log.i(TAG, "Data that will be sent is " + HexDump.dumpHexString(fullData));
    	
    	
    	
    	Intent intent = new Intent(Intents.XDRIP_PLUS_LIBRE_DATA);
        Bundle bundle = new Bundle();
        bundle.putByteArray(Intents.LIBRE_DATA_BUFFER, fullData);
        bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, JoH.tsl());
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }
    
    
}
