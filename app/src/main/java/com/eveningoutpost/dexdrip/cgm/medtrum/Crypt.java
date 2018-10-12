package com.eveningoutpost.dexdrip.cgm.medtrum;

// jamorham

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.CIPHER_BIT_FLIP;

public class Crypt {

    /**
     Pseudo-random generator based on Minimal Standard by Lewis, Goodman, and Miller in 1969.

     I[j+1] = a*I[j] (mod m)
     where a = 16807
     m = 2147483647
     Using Schrage's algorithm, a*I[j] (mod m) can be rewritten as:

     a*(I[j] mod q) - r*{I[j]/q}      if >= 0
     a*(I[j] mod q) - r*{I[j]/q} + m  otherwise
     where: {} denotes integer division
     q = {m/a} = 127773
     r = m (mod a) = 2836
     note that the seed value of 0 cannot be used in the calculation as it results in 0 itself

     */

    static long schrageRandomInt(long ix) {
        long k1 = ix / 127773L;
        ix = 16807L * (ix - (k1 * 127773L)) - (k1 * 2836L);
        if (ix < 0)
            ix += 2147483647;
        return ix;
    }

    public static long doubleSchrage(long ix) {
        return schrageRandomInt(schrageRandomInt(ix));
    }


    static byte[] getCodeBook(long serial) {
        final ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt((int) doubleSchrage(serial ^ CIPHER_BIT_FLIP));
        return buffer.array();
    }

    static void codeBookProcess(byte[] data, byte[] book) {
        for (int i = 0, k = 0; i < data.length; i++, k = (k + 1) % book.length) {
            data[i] ^= book[k];
        }
    }

    public static void code(byte[] data, long serial) {
        codeBookProcess(data, getCodeBook(serial));
    }


}
