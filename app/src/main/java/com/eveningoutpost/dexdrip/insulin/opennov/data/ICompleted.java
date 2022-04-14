package com.eveningoutpost.dexdrip.insulin.opennov.data;

import com.eveningoutpost.dexdrip.insulin.opennov.Message;

/**
 * JamOrHam
 * OpenNov completed data interface
 */

public interface ICompleted {

    int receiveFinalData(final Message msg);
    int prunePrimingDoses();
}
