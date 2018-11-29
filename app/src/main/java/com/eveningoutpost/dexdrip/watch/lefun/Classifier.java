package com.eveningoutpost.dexdrip.watch.lefun;

// jamorham

import android.util.SparseArray;

import com.eveningoutpost.dexdrip.watch.lefun.messages.BaseRx;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxFind;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxPong;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxShake;

public class Classifier {

    private static final SparseArray<Class> classes = new SparseArray<>();

    static {
        classes.put(RxPong.opcode, RxPong.class);
        classes.put(RxShake.opcode, RxShake.class);
        classes.put(RxFind.opcode, RxFind.class);
    }

    static BaseRx classify(final byte[] bytes) {

        // early validation
        if (bytes == null || bytes.length < 4) return null;
        if (bytes[0] != BaseRx.START_BYTE) return null;
        if (bytes[1] != bytes.length) return null;

        // locate class by opcode
        final Class c = classes.get(bytes[2]);
        if (c != null) {
            try {
                final BaseRx base = (BaseRx) c.newInstance();
                return base.fromBytes(bytes);
            } catch (Exception e) {
                return null;
            }
        }
        return null;

    }

}
