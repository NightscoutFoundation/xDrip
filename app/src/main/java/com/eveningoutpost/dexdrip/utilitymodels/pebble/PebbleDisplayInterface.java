package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.content.Context;

import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Created by andy on 02/06/16.
 */
public interface PebbleDisplayInterface {

    /**
     * This is command that will be sent to device, in onStartCommand.
     */
    void startDeviceCommand();

    /**
     * For receiveData event
     */
    void receiveData(int transactionId, PebbleDictionary data);

    /**
     * For receiveData event
     */
    void receiveAppData(int transactionId, PebbleDictionary data);

    /**
     * For receiveNack event
     */
    void receiveNack(int transactionId);

    /**
     * For receiveAck event
     */
    void receiveAck(int transactionId);

    /**
     * Init
     */
    void initDisplay(Context context, PebbleWatchSync pebbleWatchSync, BgGraphBuilder bgGraphBuilder);

    /**
     *
     * Current UUID
     */
    UUID watchfaceUUID();

}
