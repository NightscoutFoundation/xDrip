package com.eveningoutpost.dexdrip.Models;

import com.google.gson.annotations.Expose;

import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

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
        Log.e(TAG, "persisting sensor data");
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
            Log.e(TAG, "persisting sensor data");
            PersistentStore.setString(SENSOR_DATA_KAY, currentSensorData.toJson());
        }
        return libre2SensorData;
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
        Log.e(TAG, "Successfuly created Libre2SensorData value " + json);
        return fresh;
    }
    
}