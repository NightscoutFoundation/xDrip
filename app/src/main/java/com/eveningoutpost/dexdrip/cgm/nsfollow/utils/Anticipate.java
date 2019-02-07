package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

/**
 * jamorham
 *
 * Choose optimum anticipation times for re-attempting data collection
 */

public class Anticipate {

    public static long next(long now, final long last, final long period, final long grace) {

        final long since = now - last;
        if (since <= (grace * 2)) {
            // recent reading already
            return last + period - grace;
        }

        final long modulus = (last - now) % period;
        long nextMin;
        if (modulus < -grace) {
            nextMin = (now + modulus) + period - grace;
        } else {
            nextMin = (now + modulus);
        }
        while (nextMin <= now) {
            nextMin += grace;
        }

        return nextMin;
    }


}
