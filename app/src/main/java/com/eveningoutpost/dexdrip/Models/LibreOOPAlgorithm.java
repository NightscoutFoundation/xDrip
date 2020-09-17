package com.eveningoutpost.dexdrip.Models;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.LibreAlarmReceiver;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.CompatibleApps;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import static com.eveningoutpost.dexdrip.xdrip.gs;

public class LibreOOPAlgorithm {
    private static final String TAG = "LibreOOPAlgorithm";
    
    public enum SensorType
    {
        Libre1(0),
        Libre1New(1),
        LibreUS14Day(2),
        Libre2(3),
        LibreProH(4);

        int value;
        private SensorType(int value) {
            this.value = value;
        }

    }
    
    
    static public void SendData(byte[] fullData, long timestamp, String tagId) {
        SendData(fullData, timestamp, null, null, tagId);
    }
    
    static public void SendData(byte[] fullData, long timestamp, byte []patchUid,  byte []patchInfo, String tagId) {
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
        bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, timestamp);
        bundle.putString(Intents.LIBRE_SN, PersistentStore.getString("LibreSN"));
        bundle.putString(Intents.TAG_ID, tagId);
        bundle.putInt(Intents.LIBRE_RAW_ID, android.os.Process.myPid());
        
        if(patchUid != null) {
            bundle.putByteArray(Intents.LIBRE_PATCH_UID_BUFFER, patchUid);
        }
        if(patchInfo != null) {
            bundle.putByteArray(Intents.LIBRE_PATCH_INFO_BUFFER, patchInfo);
        }
        
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        final String packages = PersistentStore.getString(CompatibleApps.EXTERNAL_ALG_PACKAGES);
        if (packages.length() > 0) {
            final String[] packagesE = packages.split(",");
            for (final String destination : packagesE) {
                if (destination.length() > 3) {
                    intent.setPackage(destination);
                    Log.d(TAG, "Sending to package: " + destination);
                    xdrip.getAppContext().sendBroadcast(intent);
                }
            }
        } else {
            Log.d(TAG, "Sending to generic package");
            xdrip.getAppContext().sendBroadcast(intent);
        }
        lastSentData = JoH.tsl();
    }
    
    
    static public void HandleData(String oopData) {
        Log.e(TAG, "HandleData called with " + oopData);
        OOPResults oOPResults = null;
        try {
            final Gson gson = new GsonBuilder().create();
            OOPResultsContainer oOPResultsContainer = gson.fromJson(oopData, OOPResultsContainer.class);
            
            if(oOPResultsContainer.Message != null) {
                Log.e(TAG, "recieved a message from oop algorithm:" + oOPResultsContainer.Message);
            }
            
            if(oOPResultsContainer.oOPResultsArray.length > 0) {
                oOPResults =  oOPResultsContainer.oOPResultsArray[0];
            } else {
                Log.e(TAG, "oOPResultsArray exists, but size is zero");
                return;
            }
        } catch (Exception  e) { //TODO: what exception should we catch here.
            Log.e(TAG, "HandleData cought exception ", e);
            return;
        }
        boolean use_raw = Pref.getBooleanDefaultFalse("calibrate_external_libre_algorithm");
        ReadingData.TransferObject libreAlarmObject = new ReadingData.TransferObject();
        libreAlarmObject.data = new ReadingData();
        libreAlarmObject.data.trend = new ArrayList<GlucoseData>();
        
        double factor = 1;
        if(use_raw) {
            // When handeling raw, data is expected to be bigger in a factor of 1000 and 
            // is then devided by Constants.LIBRE_MULTIPLIER
            factor = 1000 / Constants.LIBRE_MULTIPLIER;
        }
        
        // Add the first object, that is the current time
        GlucoseData glucoseData = new GlucoseData();
        glucoseData.sensorTime = oOPResults.currentTime;
        glucoseData.realDate = oOPResults.timestamp;
        glucoseData.glucoseLevel = (int)(oOPResults.currentBg * factor);
        glucoseData.glucoseLevelRaw = (int)(oOPResults.currentBg * factor);
        
        libreAlarmObject.data.trend.add(glucoseData);
        
        // TODO: Add here data of last 10 minutes or whatever.
        
        
       // Add the historic data
        libreAlarmObject.data.history = new ArrayList<GlucoseData>();
        for(HistoricBg historicBg : oOPResults.historicBg) {
            if(historicBg.quality == 0) {
                glucoseData = new GlucoseData();
                glucoseData.realDate = oOPResults.timestamp + (historicBg.time - oOPResults.currentTime) * 60000;
                glucoseData.glucoseLevel = (int)(historicBg.bg * factor);
                glucoseData.glucoseLevelRaw = (int)(historicBg.bg * factor);
                libreAlarmObject.data.history.add(glucoseData);
            }
        }
        
        // Add the current point again. This is needed in order to have the last gaps closed.
        // TODO: Base this on real BG values.
        glucoseData = new GlucoseData();
        glucoseData.realDate = oOPResults.timestamp;
        glucoseData.glucoseLevel = (int)(oOPResults.currentBg * factor);
        glucoseData.glucoseLevelRaw = (int)(oOPResults.currentBg * factor);
        libreAlarmObject.data.history.add(glucoseData);
        
        Log.e(TAG, "HandleData Created the following object " + libreAlarmObject.toString());
        LibreAlarmReceiver.CalculateFromDataTransferObject(libreAlarmObject, use_raw);
        
    }
    
    public static SensorType getSensorType(byte []SensorInfo) {
        int SensorNum = (SensorInfo[0] & 0xff) << 16 | (SensorInfo[1] & 0xff) << 8 | SensorInfo[2];
        switch (SensorNum) {
            case 0xdf0000: return SensorType.Libre1;
            case 0xa20800: return SensorType.Libre1New;
            case 0xe50003: return SensorType.LibreUS14Day;
            case 0x9d0830: return SensorType.Libre2;
            case 0x700010: return SensorType.LibreProH;
        }
        Log.e(TAG, "Sensor type unknown, returning libre1 as failsafe");
        return SensorType.Libre1;
    }
    
    // Functions that are used for an external decoder.
    static public boolean IsDecriptableData(byte []patchInfo) {
        SensorType sensorType = getSensorType(patchInfo);
        return sensorType == SensorType.LibreUS14Day || sensorType == SensorType.Libre2;
    }
    
    // Two variables that are used to see if oop2 is installed.
    static long lastRecievedData = 0;
    static long lastSentData = 0;
    
    static public void handleOop2DecryptFarmResult(String tagId, long CaptureDateTime, byte[] buffer, byte []patchUid,  byte []patchInfo ) {
        lastRecievedData = JoH.tsl();
        Log.e(TAG, "handleOop2PingResult - data" + JoH.bytesToHex(buffer));
        NFCReaderX.HandleGoodReading(tagId, buffer, CaptureDateTime, false , patchUid, patchInfo, true );
    }
    
    static public void LogIfOOP2NotAlive() {
        if(lastSentData == 0) {
            // We still don't know
            return;
        }
        if(JoH.msSince(lastSentData) > 5 * 1000  && lastRecievedData == 0 ) {
            // We have sent date, but still got no response, so warning.
            Log.e(TAG, "OOP is not alive, sending data but no response.");
            JoH.static_toast_long(gs(R.string.xdrip_oop2_not_installed));
        }

    }
    
    static void SendIntent(String target, Bundle bundle) {
        lastSentData = JoH.tsl();
        Intent intent = new Intent(target);
        bundle.putInt(Intents.LIBRE_RAW_ID, android.os.Process.myPid());
        
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        final String packages = PersistentStore.getString(CompatibleApps.EXTERNAL_ALG_PACKAGES);
        if (packages.length() > 0) {
            final String[] packagesE = packages.split(",");
            for (final String destination : packagesE) {
                if (destination.length() > 3) {
                    intent.setPackage(destination);
                    Log.d(TAG, "Sending to package: " + destination);
                    xdrip.getAppContext().sendBroadcast(intent);
                }
            }
        } else {
            Log.d(TAG, "Sending to generic package");
            xdrip.getAppContext().sendBroadcast(intent);
        }
    }
}
