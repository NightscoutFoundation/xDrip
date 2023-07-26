package com.eveningoutpost.dexdrip.utilitymodels;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import lombok.Data;

/**
 * Created by jamorham on 16/03/2018.
 */

@Data
public class BridgeResponse {

    private final LinkedList<ByteBuffer> send;
    private String error_message;
    private long delay;
    private boolean still_waiting_for_data = false;
    private boolean got_all_data = false;

    public BridgeResponse() {
        send = new LinkedList<>();
    }

    public boolean hasError() {
        return error_message != null;
    }

    public void add(ByteBuffer buffer) {
        send.add(buffer);
    }

    public boolean shouldDelay() {
        return delay > 0;
    }
    
    public boolean StillWaitingForData() {
        return still_waiting_for_data;
    }
    public void SetStillWaitingForData() {
        still_waiting_for_data = true;
    }
    
    public boolean GotAllData() {
        return got_all_data;
    }
    
    public void SetGotAllData() {
        got_all_data = true;
    }
    
}


