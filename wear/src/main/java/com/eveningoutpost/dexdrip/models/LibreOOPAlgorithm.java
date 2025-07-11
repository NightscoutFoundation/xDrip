package com.eveningoutpost.dexdrip.models;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;

import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.LibreAlarmReceiver;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.CompatibleApps;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.LibreUtils;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;

import static com.eveningoutpost.dexdrip.NFCReaderX.verifyTime;
import static com.eveningoutpost.dexdrip.xdrip.gs;

class UnlockBuffers {
    UnlockBuffers(byte[] btUnlockBuffer, byte[] nfcUnlockBuffer, String deviceName, ArrayList<byte[]> unlockBufferArray, int unlockCount) {
        this.btUnlockBuffer = btUnlockBuffer;
        this.nfcUnlockBuffer = nfcUnlockBuffer;
        this.deviceName = deviceName;
        this.unlockBufferArray = unlockBufferArray;
        this.unlockCount = unlockCount;

    }

    public byte[] btUnlockBuffer;
    public byte[] nfcUnlockBuffer;
    public String deviceName;
    public ArrayList<byte[]> unlockBufferArray;
    public int unlockCount;
}


public class LibreOOPAlgorithm {
    private static final String TAG = "LibreOOPAlgorithm";
    // This are the shifts of the libre different buffers
    static final int[] LIBRE2_SHIFT = {0, 2, 4, 6, 7, 12, 15};

    public enum SensorType {
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
    
    static public void sendData(byte[] fullData, long timestamp, byte[] patchUid, byte[] patchInfo, String tagId) {
        if(fullData == null) {
            Log.e(TAG, "sendData called with null data");
            return;
        }
        
        if (fullData.length < Constants.LIBRE_1_2_FRAM_SIZE) {
            Log.e(TAG, "sendData called with data size too small. " + fullData.length);
            return;
        }
        Log.i(TAG, "Sending full data to OOP Algorithm data-len = " + fullData.length);
        
        fullData = java.util.Arrays.copyOfRange(fullData, 0, Constants.LIBRE_1_2_FRAM_SIZE);
        Log.i(TAG, "Data that will be sent is " + HexDump.dumpHexString(fullData));
        
        Intent intent = new Intent(Intents.XDRIP_PLUS_LIBRE_DATA);
        Bundle bundle = new Bundle();
        bundle.putByteArray(Intents.LIBRE_DATA_BUFFER, fullData);
        bundle.putLong(Intents.LIBRE_DATA_TIMESTAMP, timestamp);
        bundle.putString(Intents.LIBRE_SN, PersistentStore.getString("LibreSN"));
        bundle.putString(Intents.TAG_ID, tagId);
        bundle.putInt(Intents.LIBRE_RAW_ID, android.os.Process.myPid());

        if (patchUid != null) {
            bundle.putByteArray(Intents.LIBRE_PATCH_UID_BUFFER, patchUid);
        }
        if (patchInfo != null) {
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

    static public void sendBleData(byte[] fullData, long timestamp, byte[] patchUid) {
        if (fullData == null) {
            Log.e(TAG, "sendBleData called with null data");
            return;
        }

        if (fullData.length != 46) {
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
            ret = UnlockBlockingQueue.poll(9, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Interuptted exception", e);
            return null;
        }
        if (ret == null) {
            Log.e(TAG, "waitForUnlockPayload (sendGetBluetoothEnablePayload) returning null");
        } else {
            Log.e(TAG, "waitForUnlockPayload (sendGetBluetoothEnablePayload) got data payload is " + JoH.bytesToHex(ret.btUnlockBuffer) + " " + JoH.bytesToHex(ret.nfcUnlockBuffer) +
                    " unlockBufferArray = " + ret.unlockBufferArray);
        }
        return ret;
    }

    static public UnlockBuffers sendGetBluetoothEnablePayload(boolean increaseConnectionIndex) {
        Libre2SensorData currentSensorData = Libre2SensorData.getSensorData(increaseConnectionIndex);
        if (currentSensorData == null) {
            Log.e(TAG, "sendGetBluetoothEnablePayload currentSensorData == null");
            return null;
        }
        Log.e(TAG, "sendGetBluetoothEnablePayload called enableTime_ = " + currentSensorData.enableTime_ +
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
        bundle.putInt(Intents.BT_UNLOCK_BUFFER_COUNT, 2000);

        sendIntent(Intents.XDRIP_PLUS_BLUETOOTH_ENABLE, bundle);

        UnlockBuffers unlockBuffers = waitForUnlockPayload();
        if (unlockBuffers != null && unlockBuffers.unlockBufferArray != null) {
            // store the array in the cache.
            Libre2SensorData.saveBtUnlockArray(unlockBuffers.unlockBufferArray, currentSensorData.connectionIndex_);
        }
        return unlockBuffers;
    }

    static public byte[] getCachedBtUnlockKey(boolean increaseConnectionIndex) {
        return Libre2SensorData.getCachedBtUnlockKey(increaseConnectionIndex);
    }

    static public Pair<byte[], String> nfcSendgetBluetoothEnablePayload() {
        UnlockBuffers unlockBuffers = sendGetBluetoothEnablePayload(false);
        if (unlockBuffers == null) {
            Log.e(TAG, "nfcSendgetBluetoothEnablePayload returning null");
            return null;
        }
        return new Pair(unlockBuffers.nfcUnlockBuffer, unlockBuffers.deviceName);
    }

    static public byte[] btSendgetBluetoothEnablePayload(boolean increaseConnectionIndex) {
        byte[] btUnlockBuffer = LibreOOPAlgorithm.getCachedBtUnlockKey(increaseConnectionIndex);
        if (btUnlockBuffer != null) {
            return btUnlockBuffer;
        }
        UnlockBuffers unlockBuffers = sendGetBluetoothEnablePayload(increaseConnectionIndex);
        if (unlockBuffers == null) {
            Log.e(TAG, "btSendgetBluetoothEnablePayload returning null");
            return null;
        }
        return unlockBuffers.btUnlockBuffer;
    }

    static public String getLibreDeviceName() {

        Libre2SensorData currentSensorData = Libre2SensorData.getSensorData(false);
        if (currentSensorData == null || currentSensorData.deviceName_ == null) {
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
        ReadingData readingData = new ReadingData();

        readingData.trend = new ArrayList<GlucoseData>();
        
        // Add the first object, that is the current time
        GlucoseData glucoseData = new GlucoseData();
        glucoseData.sensorTime = oOPResults.currentTime;
        glucoseData.realDate = oOPResults.timestamp;
        glucoseData.glucoseLevel = (int)(oOPResults.currentBg);
        glucoseData.glucoseLevelRaw = (int)(oOPResults.currentBg);
        
        verifyTime(glucoseData.sensorTime, "LibreOOPAlgorithm", null);
        readingData.trend.add(glucoseData);
        
        // TODO: Add here data of last 10 minutes or whatever.
        
        
       // Add the historic data
        readingData.history = new ArrayList<GlucoseData>();
        for(HistoricBg historicBg : oOPResults.historicBg) {
            if(historicBg.quality == 0) {
                glucoseData = new GlucoseData();
                glucoseData.realDate = oOPResults.timestamp + (historicBg.time - oOPResults.currentTime) * 60000;
                glucoseData.glucoseLevel = (int)(historicBg.bg );
                glucoseData.glucoseLevelRaw = (int)(historicBg.bg);
                readingData.history.add(glucoseData);
            }
        }
        
        // Add the current point again. This is needed in order to have the last gaps closed.
        // TODO: Base this on real BG values.
        glucoseData = new GlucoseData();
        glucoseData.realDate = oOPResults.timestamp;
        glucoseData.glucoseLevel = (int)(oOPResults.currentBg );
        glucoseData.glucoseLevelRaw = (int)(oOPResults.currentBg );
        readingData.history.add(glucoseData);
        
        Log.d(TAG, "handleData Created the following object " + readingData.toString());
        LibreAlarmReceiver.CalculateFromDataTransferObject(readingData, false, false);
    }

    public static SensorType getSensorType(byte[] SensorInfo) {
        if (SensorInfo == null) {
            return SensorType.Libre1;
        }
        int SensorNum = (SensorInfo[0] & 0xff) << 16 | (SensorInfo[1] & 0xff) << 8 | SensorInfo[2];
        switch (SensorNum) {
            case 0xdf0000:
                return SensorType.Libre1;
            case 0xa20800:
                return SensorType.Libre1New;
            case 0xe50003:
            case 0xe60003:
                return SensorType.LibreUS14Day;
            case 0x9d0830:
            case 0xc50930:
            case 0xc60931:
            case 0x7f0e31:	
                return SensorType.Libre2;
            case 0x700010:
                return SensorType.LibreProH;
        }
        Log.e(TAG, "Sensor type unknown, returning libre1 as failsafe");
        return SensorType.Libre1;
    }

    public static int readBits(byte[] buffer, int byteOffset, int bitOffset, int bitCount) {
        if (bitCount == 0) {
            return 0;
        }
        int res = 0;
        for (int i = 0; i < bitCount; i++) {
            final int totalBitOffset = byteOffset * 8 + bitOffset + i;
            final int byte1 = (int) Math.floor(totalBitOffset / 8);
            final int bit = totalBitOffset % 8;
            if (totalBitOffset >= 0 && ((buffer[byte1] >> bit) & 0x1) == 1) {
                res = res | (1 << i);
            }
        }
        return res;
    }

    public static void handleDecodedBleResult(long timestamp, byte[] ble_data, byte[] patchUid, int[] trend_bg_vals, int[] history_bg_vals) {
        lastRecievedData = JoH.tsl();
        int raw = LibreOOPAlgorithm.readBits(ble_data, 0, 0, 0xe);
        int sensorTime = 256 * (ble_data[41] & 0xFF) + (ble_data[40] & 0xFF);
        Log.e(TAG, "Creating BG time =  " + sensorTime + " raw = " + raw);

        ReadingData readingData = new ReadingData();
        readingData.raw_data = ble_data;
        // Add bg values inside trend and history.
        readingData.trend = parseBleDataPerMinute(ble_data, trend_bg_vals, timestamp);

        readingData.history = parseBleDataHistory(ble_data, history_bg_vals, timestamp);

        String SensorSN = LibreUtils.decodeSerialNumberKey(patchUid);

        Log.d(TAG, "handleDecodedBleResult Created the following object " + readingData.toString());
        NFCReaderX.sendLibrereadingToFollowers(SensorSN, readingData.raw_data, timestamp, patchUid, null);
        boolean bg_val_exists = trend_bg_vals != null && history_bg_vals != null;
        LibreAlarmReceiver.processReadingDataTransferObject(readingData, timestamp, SensorSN, true /*=allowupload*/, patchUid, null/*=patchInfo*/, bg_val_exists);
    }

    public static ArrayList<GlucoseData> parseBleDataPerMinute(byte[] ble_data, int[] trend_bg_vals, Long captureDateTime) {
        int sensorTime = 256 * (ble_data[41] & 0xFF) + (ble_data[40] & 0xFF);

        ArrayList<GlucoseData> trendList = new ArrayList<>();
        final int DATA_SIZE = 7;
        for (int i = 0; i < DATA_SIZE; i++) {
            GlucoseData glucoseData = new GlucoseData();

            glucoseData.glucoseLevelRaw = LibreOOPAlgorithm.readBits(ble_data, i * 4, 0, 0xe);
            glucoseData.temp = LibreOOPAlgorithm.readBits(ble_data, i * 4, 0xe, 0xc) << 2;
            glucoseData.flags = LibreOOPAlgorithm.readBits(ble_data, i * 4, 0x1a, 0x6);
            glucoseData.source = GlucoseData.DataSource.BLE;

            int relative_time = LIBRE2_SHIFT[i];
            glucoseData.realDate = captureDateTime - relative_time * Constants.MINUTE_IN_MS;
            glucoseData.sensorTime = sensorTime - relative_time;
            if (trend_bg_vals != null && trend_bg_vals.length == DATA_SIZE) {
                glucoseData.glucoseLevel = trend_bg_vals[i];
            }
            if (verifyTime(glucoseData.sensorTime, "parseBleDataPerMinute ", ble_data)) {
                trendList.add(glucoseData);
            }
        }
        return trendList;
    }

    public static ArrayList<GlucoseData> parseBleDataHistory(byte[] ble_data, int[] history_bg_vals, Long captureDateTime) {
        int sensorTime = 256 * (ble_data[41] & 0xFF) + (ble_data[40] & 0xFF);
        //System.out.println("sensorTime = " + sensorTime);
        if (sensorTime < 3) {
            return new ArrayList<>();
        }
        int sensorTimeModulo = (sensorTime - 2) / 15 * 15;
        ArrayList<GlucoseData> historyList = new ArrayList<>();

        final int DATA_SIZE = 3;

        for (int i = 0; i < DATA_SIZE; i++) {
            GlucoseData glucoseData = new GlucoseData();

            glucoseData.glucoseLevelRaw = readBits(ble_data, (i + 7) * 4, 0, 0xe);
            glucoseData.temp = LibreOOPAlgorithm.readBits(ble_data, (i + 7) * 4, 0xe, 0xc) << 2;
            glucoseData.flags = LibreOOPAlgorithm.readBits(ble_data, (i + 7) * 4, 0x1a, 0x6);
            glucoseData.source = GlucoseData.DataSource.BLE;

            int relative_time = i * 15;
            int final_time = sensorTimeModulo - relative_time;
            if (final_time < 0) {
                break;
            }

            glucoseData.realDate = captureDateTime + (final_time - sensorTime) * Constants.MINUTE_IN_MS;
            glucoseData.sensorTime = final_time;
            if (history_bg_vals != null && history_bg_vals.length == DATA_SIZE) {
                glucoseData.glucoseLevel = history_bg_vals[i];
            }
            if (verifyTime(final_time, "parseBleDataHistory", ble_data)) {
                historyList.add(glucoseData);
            }
        }
        return historyList;
    }


    // Functions that are used for an external decoder.
    static public boolean isDecodeableData(byte[] patchInfo) {
        SensorType sensorType = getSensorType(patchInfo);
        return sensorType == SensorType.LibreUS14Day || sensorType == SensorType.Libre2;
    }

    // Two variables that are used to see if oop2 is installed.
    static long lastRecievedData = 0;
    static long lastSentData = 0;
    static ArrayBlockingQueue<UnlockBuffers> UnlockBlockingQueue = new ArrayBlockingQueue<UnlockBuffers>(1);

    static public void handleOop2DecodeFramResult(String tagId, long CaptureDateTime, byte[] buffer, byte[] patchUid, byte[] patchInfo, int[] trend_bg_vals, int[] history_bg_vals) {
        lastRecievedData = JoH.tsl();
        Log.e(TAG, "handleOop2DecodeFramResult - data " + JoH.bytesToHex(buffer));
        NFCReaderX.HandleGoodReading(tagId, buffer, CaptureDateTime, false, patchUid, patchInfo, true, trend_bg_vals, history_bg_vals);
    }


    static public void handleOop2BluetoothEnableResult(byte[] bt_unlock_buffer, byte[] nfc_unlock_buffer, byte[] patchUid, byte[] patchInfo, String device_name,
                                                       ArrayList<byte[]> unlockBufferArray, int unlockCount) {
        lastRecievedData = JoH.tsl();
        Log.e(TAG, "handleOop2BluetoothEnableResult - data bt_unlock_buffer " + JoH.bytesToHex(bt_unlock_buffer) + "\n nfc_unlock_buffer " + JoH.bytesToHex(nfc_unlock_buffer));
        if (unlockBufferArray != null && unlockBufferArray.size() > 0) {
            Log.d(TAG, "handleOop2BluetoothEnableResult big buffer first " + JoH.bytesToHex(unlockBufferArray.get(0)));
        }
        UnlockBlockingQueue.clear();
        try {
            UnlockBlockingQueue.add(new UnlockBuffers(bt_unlock_buffer, nfc_unlock_buffer, device_name, unlockBufferArray, unlockCount));
        } catch (IllegalStateException is) {
            Log.e(TAG, "Queue is full", is);
        }
    }

    static public void logIfOOP2NotAlive() {
        if (lastSentData == 0) {
            // We still don't know
            return;
        }
        if (JoH.msSince(lastSentData) > 5 * 1000 && lastRecievedData == 0) {
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
