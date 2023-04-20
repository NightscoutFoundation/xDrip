package com.eveningoutpost.dexdrip.g5model;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.G5CollectionService;

/**
 * Created by jamorham on 25/11/2016.
 */

public class BatteryInfoTxMessage extends BaseMessage {

    private final static String TAG = G5CollectionService.TAG; // meh
    static final byte opcode = 0x22;

    public BatteryInfoTxMessage() {
        init(opcode, 3);
        UserError.Log.e(TAG, "BatteryInfoTx dbg: " + JoH.bytesToHex(byteSequence));
    }
}

