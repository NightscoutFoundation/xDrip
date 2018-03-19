package com.eveningoutpost.dexdrip.UtilityModels;

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
}


