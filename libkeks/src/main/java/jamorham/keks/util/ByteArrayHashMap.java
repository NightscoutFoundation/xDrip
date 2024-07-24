package jamorham.keks.util;

import static jamorham.keks.Curve.FIELD_SIZE;

import java.util.HashMap;

import lombok.val;

/**
 * JamOrHam
 */

public class ByteArrayHashMap extends HashMap<Integer, byte[]> {

    public byte[] mget(Integer key) {
        val v = super.get(key);
        if (v == null) {
            super.put(key, new byte[FIELD_SIZE]);
            return super.get(key);
        }
        return v;
    }

}
