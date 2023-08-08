package com.eveningoutpost.dexdrip.g5model;

import lombok.val;

/**
 * JamOrHam
 */

public class ExtraState {

    boolean p;
    boolean t;
    boolean l;
    boolean h;
    boolean s;
    boolean error;

    public static ExtraState parse(byte state) {

        val es = new ExtraState();
        es.p = (state & 0x01) != 0;
        es.t = (state & 0x02) != 0;
        es.l = (state & 0x04) != 0;
        es.h = (state & 0x08) != 0;
        es.s = (state & 0x10) != 0;
        es.error = (state & 0x80) != 0;
        return es;
    }

    public static ExtraState parse(int state) {
        return parse((byte) state);
    }

}
