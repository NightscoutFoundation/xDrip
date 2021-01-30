package com.eveningoutpost.dexdrip.Models;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.LibreAlarmReceiver;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.CompatibleApps;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.LibreUtils;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import static com.eveningoutpost.dexdrip.xdrip.gs;

class UnlockBuffers {
    UnlockBuffers(byte [] btUnlockBuffer,  byte [] nfcUnlockBuffer, String deviceName) {
        this.btUnlockBuffer = btUnlockBuffer;
        this.nfcUnlockBuffer = nfcUnlockBuffer;
        this.deviceName = deviceName;
    }
    public byte [] btUnlockBuffer;
    public byte [] nfcUnlockBuffer;
    public String deviceName;
}


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
    
    
    static public void sendData(byte[] fullData, long timestamp, String tagId) {
        sendData(fullData, timestamp, null, null, tagId);
    }
    
    static public void sendData(byte[] fullData, long timestamp, byte []patchUid,  byte []patchInfo, String tagId) {
        if(fullData == null) {
            Log.e(TAG, "sendData called with null data");
            return;
        }
        
        if(fullData.length < 344) {
            Log.e(TAG, "sendData called with data size too small. " + fullData.length);
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
    
    static public void sendBleData(byte[] fullData, long timestamp, byte []patchUid) {
        if(fullData == null) {
            Log.e(TAG, "sendBleData called with null data");
            return;
        }
        
        if(fullData.length != 46) {
            Log.e(TAG, "sendBleData called with wrong data size " + fullData.length);
            return;
        }
        Log.i(TAG, "Sending full data to OOP Algorithm data-len = " + fullData.length);

        Bundle bundle = new Bundle();
        bundle.putByteArray(Intents.LIBRE_DATA_BUFFER, fullData);
        bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, timestamp);
        bundle.putInt(Intents.LIBRE_RAW_ID, android.os.Process.myPid());
        bundle.putByteArray(Intents.LIBRE_PATCH_UID_BUFFER, patchUid);
        
        sendIntent(Intents.XDRIP_PLUS_LIBRE_BLE_DATA, bundle);
    }

    // A mechanism to wait for the unlock buffer to return:
    static UnlockBuffers waitForUnlockPayload() {
        UnlockBuffers ret;
        UnlockBlockingQueue.clear();
        try {
            ret = UnlockBlockingQueue.poll(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interuptted exception", e);
            return null;
        }
        if(ret == null) {
            Log.e(TAG, "waitForUnlockPayload (sendGetBlutoothEnablePayload) returning null");
        } else {
            Log.e(TAG, "waitForUnlockPayload (sendGetBlutoothEnablePayload) got data payload is " + JoH.bytesToHex(ret.btUnlockBuffer) + " "+  JoH.bytesToHex(ret.nfcUnlockBuffer));
        }
        return ret;
    }
    
    static public  UnlockBuffers sendGetBlutoothEnablePayload(boolean increaseConnectionIndex) {
        Libre2SensorData currentSensorData = Libre2SensorData.getSensorData(increaseConnectionIndex);
        if(currentSensorData == null) {
            Log.e(TAG, "sendGetBlutoothEnablePayload currentSensorData == null");
            return null;
        }
        Log.e(TAG, "sendGetBlutoothEnablePayload called enableTime_ = " + currentSensorData.enableTime_ +
                " connectionIndex_ " + currentSensorData.connectionIndex_ + 
                 " patchUid " + JoH.bytesToHex(currentSensorData.patchUid_) +
                 " patchInfo " + JoH.bytesToHex(currentSensorData.patchInfo_) +
                " increaseConnectionIndex " + increaseConnectionIndex);
        
        Bundle bundle = new Bundle();
        bundle.putInt(Intents.LIBRE_RAW_ID, android.os.Process.myPid());
        bundle.putByteArray(Intents.LIBRE_PATCH_UID_BUFFER, currentSensorData.patchUid_);
        bundle.putByteArray(Intents.LIBRE_PATCH_INFO_BUFFER, currentSensorData.patchInfo_);
        bundle.putInt(Intents.ENABLE_TIME, currentSensorData.enableTime_);
        bundle.putInt(Intents.CONNECTION_INDEX, currentSensorData.connectionIndex_);
        sendIntent(Intents.XDRIP_PLUS_BLUETOOTH_ENABLE, bundle);
        return waitForUnlockPayload();
    }
    
    static public  Pair<byte[], String> nfcSendgetBlutoothEnablePayload() {
        UnlockBuffers unlockBuffers = sendGetBlutoothEnablePayload(false);
        if(unlockBuffers == null) {
            Log.e(TAG, "nfcSendgetBlutoothEnablePayload returning null");
            return null;
        }
        return new Pair(unlockBuffers.nfcUnlockBuffer, unlockBuffers.deviceName);
    }

    static public String getLibreDeviceName() {
        
        Libre2SensorData currentSensorData = Libre2SensorData.getSensorData(false);
        if(currentSensorData == null || currentSensorData.deviceName_ == null) {
            Log.e(TAG, "getLibreDeviceName currentSensorData == null");
            return "unknown";
        }
        return currentSensorData.deviceName_;
    }
    
    static public void handleData(String oopData) {
        Log.e(TAG, "handleData called with " + oopData);
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
        
        Log.e(TAG, "handleData Created the following object " + libreAlarmObject.toString());
        LibreAlarmReceiver.CalculateFromDataTransferObject(libreAlarmObject, use_raw);
    }
    
    public static SensorType getSensorType(byte []SensorInfo) {
        if(SensorInfo == null) {
            return SensorType.Libre1;
        }
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

    public static int readBits(byte []buffer, int byteOffset,int  bitOffset, int  bitCount) {
        if (bitCount == 0) {
            return 0;
        }
        int res = 0;
        for (int i = 0; i < bitCount; i++) {
            final int totalBitOffset = byteOffset * 8 + bitOffset + i;
            final int byte1 = (int)Math.floor(totalBitOffset / 8);
            final int bit = totalBitOffset % 8;
            if (totalBitOffset >= 0 && ((buffer[byte1] >> bit) & 0x1) == 1) {
                res = res | (1 << i);
            }
        }
        return res;
    }
    
    public static void handleDecodedBleResult(long timestamp, byte[] ble_data, byte []patchUid) {

        int raw  = LibreOOPAlgorithm.readBits(ble_data, 0 , 0 , 0xe);
        int sensorTime = 256 * (ble_data[41] & 0xFF) + (ble_data[40] & 0xFF);
        Log.e(TAG, "Creating BG time =  " + sensorTime + "raw = " + raw);
        
        ReadingData.TransferObject libreAlarmObject = new ReadingData.TransferObject();
        libreAlarmObject.data = new ReadingData();
        libreAlarmObject.data.trend = new ArrayList<GlucoseData>();

        libreAlarmObject.data.raw_data = ble_data;
        
        // Add the first object, that is the current time
        GlucoseData glucoseData = new GlucoseData();
        glucoseData.sensorTime = sensorTime;
        glucoseData.realDate = timestamp;
        glucoseData.glucoseLevel = raw;
        glucoseData.glucoseLevelRaw = raw;
        
        libreAlarmObject.data.trend.add(glucoseData);
        String SensorSN = LibreUtils.decodeSerialNumberKey(patchUid);
        
        // TODO: Add here data of last 10 minutes or whatever.
        Log.e(TAG, "handleDecodedBleResult Created the following object " + libreAlarmObject.toString());
        LibreAlarmReceiver.processReadingDataTransferObject(libreAlarmObject, timestamp, SensorSN, true /*=allowupload*/, patchUid, null/*=patchInfo*/);   
    }
    
    
    // Functions that are used for an external decoder.
    static public boolean isDecodeableData(byte []patchInfo) {
        SensorType sensorType = getSensorType(patchInfo);
        return sensorType == SensorType.LibreUS14Day || sensorType == SensorType.Libre2;
    }
    
    // Two variables that are used to see if oop2 is installed.
    static long lastRecievedData = 0;
    static long lastSentData = 0;
    static ArrayBlockingQueue<UnlockBuffers> UnlockBlockingQueue = new ArrayBlockingQueue<UnlockBuffers>(1);
    
    static public void handleOop2DecryptFarmResult(String tagId, long CaptureDateTime, byte[] buffer, byte []patchUid,  byte []patchInfo ) {
        lastRecievedData = JoH.tsl();
        Log.e(TAG, "handleOop2PingResult - data" + JoH.bytesToHex(buffer));
        NFCReaderX.HandleGoodReading(tagId, buffer, CaptureDateTime, false , patchUid, patchInfo, true );
    }
    
    
    static public void  handleOop2BlutoothEnableResult(byte[] bt_unlock_buffer, byte[] nfc_unlock_buffer, byte[] patchUid, byte[] patchInfo, String device_name) {
        lastRecievedData = JoH.tsl();
        Log.e(TAG, "handleOop2BlutoothEnableResult - data bt_unlock_buffer " + JoH.bytesToHex(bt_unlock_buffer) + "\n nfc_unlock_buffer "+ JoH.bytesToHex(nfc_unlock_buffer));
        UnlockBlockingQueue.clear();
        try {
            UnlockBlockingQueue.add(new UnlockBuffers(bt_unlock_buffer, nfc_unlock_buffer, device_name));
        } catch (IllegalStateException  is) {
            Log.e(TAG, "Queue is full", is);

        }
        
    }
    
    static public void logIfOOP2NotAlive() {
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
    
    static void sendIntent(String target, Bundle bundle) {
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
