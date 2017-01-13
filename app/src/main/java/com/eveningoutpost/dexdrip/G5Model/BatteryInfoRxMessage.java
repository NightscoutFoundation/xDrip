package com.eveningoutpost.dexdrip.G5Model;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by jamorham on 25/11/2016.
 */


public class BatteryInfoRxMessage extends TransmitterMessage {

    private final static String TAG = G5CollectionService.TAG; // meh

    public static final byte opcode = 0x23;


    public BatteryInfoRxMessage(byte[] packet) {
        UserError.Log.e(TAG, "BatteryRX dbg: " + JoH.bytesToHex(packet));
        if (packet.length >= 18) {
         //
        }
    }

}