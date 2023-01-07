package com.eveningoutpost.dexdrip.glucosemeter.glucomen.st;

import com.eveningoutpost.dexdrip.buffer.MyByteBuffer;

/**
 * JamOrHam
 * GlucoMen base message class
 */

public class BaseMessage extends MyByteBuffer {

    static final int EXTENDED_RB = 0x33;
    static final int HIGH_DATA_RATE_FLAG = 0x02;
    static final int IC_MFG = 0x02;
}
