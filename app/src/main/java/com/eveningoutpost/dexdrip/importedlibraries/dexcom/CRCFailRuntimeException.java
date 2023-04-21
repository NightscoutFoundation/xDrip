package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

public class CRCFailRuntimeException extends RuntimeException {
    public CRCFailRuntimeException(String message){
        super(message);
    }
}
