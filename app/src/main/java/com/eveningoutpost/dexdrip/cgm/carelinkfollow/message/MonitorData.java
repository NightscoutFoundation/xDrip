package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

public class MonitorData {

    public String deviceFamily;

    public boolean isBle() {
        return deviceFamily.contains("BLE");
    }

}
