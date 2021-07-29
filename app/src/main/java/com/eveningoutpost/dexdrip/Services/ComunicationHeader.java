package com.eveningoutpost.dexdrip.Services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

// This is a struct that is supsoed to tell the protocol version and is the first that the client is sending
// The complete protocol is:
// 	1) the client connects
//  2) send this message.
//  3) the server will send numberOfRecords of type ???? that it has.
class ComunicationHeader {
    /**
     *
     */

    @Expose
    int version;
    @Expose
    int numberOfRecords;
//	String message;
//	byte reserved[];

    ComunicationHeader(int numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
        this.version = 1;
    }

    String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}

class ComunicationHeaderV2 {


    @Expose
    int version;
    @Expose
    int numberOfRecords;
    
    // Only send packets that are newer than this time
    @Expose
    long fromTime;
    
    // bluetooth device addresses
    @Expose
    String btAddresses;

    // Used with libre2 in order to allow the raspberry pis to conntect with xDrip.
    @Expose
    String xDripIpAddresses;

    ComunicationHeaderV2(int numberOfRecords) {
        this.numberOfRecords = numberOfRecords;
        this.version = 2;
    }

    String toJson() {
        return new GsonBuilder().create().toJson(this);
    }
}
