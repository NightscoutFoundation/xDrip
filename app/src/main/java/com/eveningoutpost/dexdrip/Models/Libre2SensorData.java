package com.eveningoutpost.dexdrip.Models;

public class Libre2SensorData {
    byte []patchUid_ = null;
    byte []patchInfo_ = null;
    int enableTime_;
    int unlockCount_;
    
    static Libre2SensorData currentSensorData = null;
    //byte [] StreamingUnlockPayload_ = null;
    
    static public void setLibre2SensorData(byte []patchUid, byte []patchInfo, int enableTime, int unlockCount) {
        currentSensorData = new Libre2SensorData();
        currentSensorData.patchUid_ = patchUid;
        currentSensorData.patchInfo_ = patchInfo;
        currentSensorData.enableTime_ = enableTime;
        currentSensorData.unlockCount_ = unlockCount;
        //??? persist this
    }
    

    static Libre2SensorData getSensorData(boolean increaseUnlockCount) {
        // read from persistent storage if needed.???
        
        Libre2SensorData libre2SensorData= new Libre2SensorData();
        libre2SensorData.patchUid_ = currentSensorData.patchUid_;
        libre2SensorData.patchInfo_ = currentSensorData.patchInfo_;
        libre2SensorData.enableTime_ = currentSensorData.enableTime_;
        libre2SensorData.unlockCount_ = currentSensorData.unlockCount_;
        if(increaseUnlockCount) {
            currentSensorData.unlockCount_++;
        }
        //??? persist current data
        return libre2SensorData; //??? or null if needed
    }
}