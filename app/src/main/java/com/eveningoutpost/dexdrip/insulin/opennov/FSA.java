package com.eveningoutpost.dexdrip.insulin.opennov;

import lombok.Data;
import lombok.ToString;

/**
 * JamOrHam
 * OpenNov state type
 */

@ToString(includeFieldNames = true)
@Data
public class FSA {

    public final Action action;
    public final byte[] payload;

    public enum Action {WRITE, WRITE_READ, READ, LOOP, DONE}

    public static FSA empty() {
        return new FSA(Action.DONE, null);
    }

    public static FSA loop() {
        return new FSA(Action.LOOP, null);
    }

    public static FSA read() {
        return new FSA(Action.READ, null);
    }

    public static FSA writeRead(final byte[] payload) {
        return new FSA(Action.WRITE_READ, payload);
    }

    public static FSA writeNull() {
        return writeRead(new byte[0]);
    }

    public boolean doRead() {
        return action == Action.READ || action == Action.WRITE_READ;
    }

}
