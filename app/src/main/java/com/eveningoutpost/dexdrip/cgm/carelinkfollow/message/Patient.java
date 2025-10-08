package com.eveningoutpost.dexdrip.cgm.carelinkfollow.message;

public class Patient {

    public String firstName;
    public String lastName;
    public String status;
    public String username;
    public boolean notificationsAllowed;
    public String nickname;
    public String lastDeviceFamily;
    public boolean patientUsesConnect;

    public boolean isBle() {
        return (lastDeviceFamily.contains("BLE") || lastDeviceFamily.contains("SIMPLERA"));
    }

}
