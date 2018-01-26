package com.eveningoutpost.dexdrip.Models;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.NSEmulatorReceiver;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.TransmitterRawData;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.MockDataSource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertFromJson;

import java.util.Arrays;

import org.json.JSONException;

public class LibreOOPAlgorithm {
    private static final String TAG = "LibreOOPAlgorithm";
    
    static public void SendData(byte[] fullData) {
        if(fullData == null) {
            Log.e(TAG, "SendData called with null data");
            return;
        }
        
        if(fullData.length < 344) {
            Log.e(TAG, "SendData called with data size too small. " + fullData.length);
            return;
        }
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
    
    static public void HandleData(String oopData) {
        Log.e(TAG, "HandleData called with " + oopData);
        try {
            final Gson gson = new GsonBuilder().create();
            final OOPResults oOPResults = gson.fromJson(oopData, OOPResults.class);
            
            // Enter the historic data
            for(HistoricBg historicBg : oOPResults.historicBg) {
                if(historicBg.quality == 0) {
                    NSEmulatorReceiver.bgReadingInsertFromData(
                        oOPResults.timestamp + (historicBg.time - oOPResults.currentTime) * 60000, historicBg.bg, false);
                }
            }
            
            NSEmulatorReceiver.bgReadingInsertFromData(oOPResults.timestamp, oOPResults.currentBg, true);
            
            
            
        } catch (Exception  e) { //TODO: what exception should we catch here.
            Log.e(TAG, "HandleData cought exception ", e);
        }
    }
}
