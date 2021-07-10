package com.eveningoutpost.dexdrip.insulin.pendiq;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

import java.util.concurrent.atomic.AtomicInteger;

// jamorham

public class SequenceCounter {

    private static final String STORE_REF = "pendiq-sequence-number";
    private static final AtomicInteger counter = new AtomicInteger((int) PersistentStore.getLong(STORE_REF));

    public static void resetZero() {
        counter.set(0);
    }

    public static short getNext() {
        final short value = (short) (counter.incrementAndGet() % 9999);
        UserError.Log.d("Pendiq", "Returning sequence number: " + value);
        Inevitable.task("save-pendiq-counter", 5000, () -> PersistentStore.setLong(STORE_REF, counter.get()));
        return value;
    }

}
