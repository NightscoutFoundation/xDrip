package com.eveningoutpost.dexdrip.UtilityModels;

import java.nio.ByteBuffer;
import java.util.LinkedList;

import lombok.Data;

/**
 * Created by jamorham on 16/03/2018.
 */

@Data
public class BridgeResponse {

    public final LinkedList<ByteBuffer> send;
    String error_message;
    long delay;

    public BridgeResponse() {
        send = new LinkedList<>();
    }

    public boolean hasError() {
        return error_message != null;
    }

    public boolean shouldDelay() {
        return delay > 0;
    }
}


