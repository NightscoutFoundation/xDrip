package com.eveningoutpost.dexdrip.g5model;

import static com.eveningoutpost.dexdrip.g5model.DexTimeKeeper.getDexTime;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;

/**
 * JamOrHam
 */

public class BackFillTxMessage2 extends BaseMessage {

    final byte opcode = 0x59;
    final int length = 9;

    public BackFillTxMessage2(final int startDexTime, final int endDexTime) {
        init(opcode, length);
        data.putInt(startDexTime);
        data.putInt(endDexTime);
        getByteSequence();
        UserError.Log.d(TAG, "BackfillTxMessage2 dbg: " + JoH.bytesToHex(byteSequence));
    }

    public static BackFillTxMessage2 get(final String id, final long startTime, final long endTime) {
        final int dexStart = getDexTime(id, startTime);
        final int dexEnd = getDexTime(id, endTime);
        if (dexStart < 1 || dexEnd < 1) {
            UserError.Log.e(TAG, "Unable to calculate start or end time for BackFillTxMessage2");
            return null;
        }
        return new BackFillTxMessage2(dexStart, dexEnd);
    }

}
