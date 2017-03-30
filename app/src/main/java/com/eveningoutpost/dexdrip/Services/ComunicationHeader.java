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
