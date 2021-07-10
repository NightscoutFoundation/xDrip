package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

/**
 * Choose optimum anticipation times for re-attempting data collection to minimize
 * number of requests to nightscout but at the same time reduce latency from new
 * value is available till it is shown in xdrip.
 *
 * We're trying to give the user the lowest latency on the data we can, but avoiding constantly
 * polling for data to conserve battery life and mobile data costs.
 *
 * @author Original author jamorham
 */
public class Anticipate {

    /**
     * If last + period and a bit < now, ask again after grace
     * If last + period and a bit >= now, ask again after last + period and grace
     */
    public static long next(long now, final long last, final long period, final long grace) {

        final long since = now - last;
        if (since <= (grace * 2)) {
            // recent reading already
            return last + period - grace;
        }

        // Find time outside period schedule where we are now.
        final long modulus = (last - now) % period;
        long nextMin;
        // Try to wake up on next expected
        if (modulus < -grace) {
            nextMin = (now + modulus) + period - grace;
        } else {
            nextMin = (now + modulus);
        }

        // Make sure result is after now.
        while (nextMin <= now) {
            nextMin += grace;
        }

        return nextMin;
    }
}