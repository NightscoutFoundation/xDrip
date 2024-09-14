package com.eveningoutpost.dexdrip.cgm.nsfollow.utils;

/**
 * Choose optimum anticipation times for re-attempting data collection to minimize
 * number of requests from the source but at the same time reduce latency for new
 * value shown in xdrip.
 *
 * We're trying to provide the lowest latency we can, but avoid constantly
 * polling the source to conserve battery life and mobile data costs.
 *
 * @author Original author jamorham
 */
public class Anticipate {

    /**
     * If last + period and a bit < now, ask again after grace
     * If last + period and a bit >= now, ask again after last + period and grace
     */

    public static long next(long now, final long lastTimeStamp, final long period, final long grace) { // Calling the anticipate method without a lag parameter will use a default lag of 0
        return next(now, lastTimeStamp, period, grace, 0);
    }
    public static long next(long now, final long lastTimeStamp, final long period, final long grace, final long lag) {
        final long last = lastTimeStamp + lag; // The calling method can include a non-zero lag parameter to delay the anticipation time to account for source delay.

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
