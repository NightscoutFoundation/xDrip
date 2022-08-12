package com.eveningoutpost.dexdrip.Models;

import com.google.gson.annotations.Expose;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Libre2SensorData {

    @Expose
    byte []patchUid_ = null;
    @Expose
    byte []patchInfo_ = null;
    @Expose
    int enableTime_;
    @Expose
    int connectionIndex_;
    @Expose
    String deviceName_;
    @Expose
    Map<Integer, byte[]> bt_cached_keys = new HashMap<Integer, byte[]>();
    
    static Libre2SensorData currentSensorData = null;
    private static final String TAG = "Libre2SensorData";
    private static final String SENSOR_DATA_KAY = "Libre2SensorData";
    
    static synchronized public void setLibre2SensorData(byte []patchUid, byte []patchInfo, int enableTime, int connectionIndex, String deviceName) {
        currentSensorData = new Libre2SensorData();
        currentSensorData.patchUid_ = patchUid;
        currentSensorData.patchInfo_ = patchInfo;
        currentSensorData.enableTime_ = enableTime;
        currentSensorData.connectionIndex_ = connectionIndex;
        currentSensorData.deviceName_ = deviceName;
        Log.d(TAG, "persisting sensor data");
        PersistentStore.setString(SENSOR_DATA_KAY, currentSensorData.toJson());
    }
    

    static synchronized Libre2SensorData getSensorData(boolean increaseConnectionIndex) {
        if(currentSensorData == null) {
            String json = PersistentStore.getString(SENSOR_DATA_KAY);
            currentSensorData = createFromJson(json);
            if(currentSensorData == null) {
                return null;
            }
        }
        
        Libre2SensorData libre2SensorData= new Libre2SensorData();
        libre2SensorData.patchUid_ = currentSensorData.patchUid_;
        libre2SensorData.patchInfo_ = currentSensorData.patchInfo_;
        libre2SensorData.enableTime_ = currentSensorData.enableTime_;
        libre2SensorData.connectionIndex_ = currentSensorData.connectionIndex_;
        libre2SensorData.deviceName_ = currentSensorData.deviceName_;
        if(increaseConnectionIndex) {
            currentSensorData.connectionIndex_++;
            Log.d(TAG, "persisting sensor data");
            PersistentStore.setString(SENSOR_DATA_KAY, currentSensorData.toJson());
        }
        return libre2SensorData;
    }

    // This function only updates the unlockBufferArray
    static synchronized void saveBtUnlockArray( ArrayList<byte[]> unlockBufferArray, int connectionIndex) {
        if(currentSensorData == null) {
            String json = PersistentStore.getString(SENSOR_DATA_KAY);
            currentSensorData = createFromJson(json);
            if(currentSensorData == null) {
                return;
            }
        }
        currentSensorData.bt_cached_keys.clear();
        for(int i = 0 ; i < unlockBufferArray.size(); i++) {
            currentSensorData.bt_cached_keys.put(connectionIndex + i, unlockBufferArray.get(i));
        }
        Log.e(TAG, "persisting sensor data: bt_cached_keys: size:"+currentSensorData.bt_cached_keys.size());
        PersistentStore.setString(SENSOR_DATA_KAY, currentSensorData.toJson());
    }

    static synchronized public byte[] getCachedBtUnlockKey(boolean increaseConnectionIndex) {
        if(currentSensorData == null) {
            String json = PersistentStore.getString(SENSOR_DATA_KAY);
            currentSensorData = createFromJson(json);
            if(currentSensorData == null) {
                return null;
            }
        }
        byte[] ret = currentSensorData.bt_cached_keys.get(currentSensorData.connectionIndex_);
        if(ret == null) {
            Log.e(TAG, "No cached data exists, returning without data.");
            return null;
        }

        if(increaseConnectionIndex) {
            currentSensorData.connectionIndex_++;
            Log.d(TAG, "persisting sensor data connection count increased");
            PersistentStore.setString(SENSOR_DATA_KAY, currentSensorData.toJson());
        }
        return ret;
    }
    
    private String toJson() {
        return JoH.defaultGsonInstance().toJson(this);        
    }

    private static Libre2SensorData createFromJson(String json) {
        if (json == null) {
            return null;
        }
        Libre2SensorData fresh;
        try {
            fresh = JoH.defaultGsonInstance().fromJson(json, Libre2SensorData.class);
        } catch (Exception e) {
            Log.e(TAG, "Libre2SensorData Got exception processing json msg: " + e );
            return null;
        }
        Log.d(TAG, "Successfuly created Libre2SensorData value " + json);
        return fresh;
    }
    
}