package com.eveningoutpost.dexdrip.utilitymodels;

// jamorham

import android.util.Pair;

import com.eveningoutpost.dexdrip.models.BgReading;

import java.util.List;

public class GraphTools {

    // bgreadings must be in ascending time order
    // result first is offsetted position, second is matched
    public static Pair<Float, Float> bestYPosition(final List<BgReading> readings, final long timestamp, final boolean mgdl, final boolean ascending, final double highMark, final double offset) {

        final double unitsScale = (mgdl ? 1f : Constants.MGDL_TO_MMOLL);
        final float defaultPosition = (float) (7 * unitsScale);

        float offsetedYpos = defaultPosition;
        float calculatedYpos = defaultPosition;

        if (readings != null && readings.size() > 0) {

            final float offsetScaled = (float) (offset * unitsScale);

            BgReading before = null;
            BgReading after = null;

            final int chopResult = runBinarySearchIteratively(readings, timestamp, ascending);

            for (int i = chopResult; ascending ? (i < readings.size()) : (i >= 0); i = i + (ascending ? 1 : -1)) {
                if (readings.get(i).timestamp <= timestamp) {
                    before = readings.get(i);
                } else {
                    after = readings.get(i);
                    break;
                }
            }

            if (before == null && after == null) {
                return new Pair<>(offsetedYpos, calculatedYpos);
            }

            if (before == null) {
                before = after;
            } else if (after == null) {
                after = before;
            }

            calculatedYpos = (float) (interpolateCalculatedValue(before, after, timestamp) * unitsScale);

            if (calculatedYpos >= highMark) {
                offsetedYpos = calculatedYpos - offsetScaled;
            } else {
                offsetedYpos = calculatedYpos + offsetScaled;
            }

        }
        return new Pair<>(offsetedYpos, calculatedYpos);

    }

    // run a binary chop search on sorted bgReading dataset to find best match before the one we want
    private static int runBinarySearchIteratively(List<BgReading> readings, long hunt, boolean ascending) {

        int low = 0;
        int high = readings.size() - 1;

        int index = -1;
        int middle = 0;
        if (ascending) {
            while (low <= high) {
                middle = (low + high) / 2;
                if (readings.get(middle).timestamp < hunt) {
                    low = middle + 1;
                } else if (readings.get(middle).timestamp > hunt) {
                    high = middle - 1;
                } else if (readings.get(middle).timestamp == hunt) {
                    index = middle;
                    break;
                }
            }
        } else {
            while (low <= high) {
                middle = (low + high) / 2;
                if (readings.get(middle).timestamp > hunt) {
                    low = middle + 1;
                } else if (readings.get(middle).timestamp < hunt) {
                    high = middle - 1;
                } else if (readings.get(middle).timestamp == hunt) {
                    index = middle;
                    break;
                }
            }
        }
        if (index == -1) {
            index = middle + (ascending ? -1 : 1);
            if (index < 0) {
                if (ascending) {
                    index = 0;
                } else {
                    index = readings.size() - 1;
                }
            } else if (index > readings.size() - 1) {
                index = readings.size() - 1;
            }
        }
        return index;
    }

    public static float yposRatio(final float first, final float second, final float ratio) {
        return first + ((second - first) * ratio);
    }

    // TODO should we throw the exception so we can choose to handle it, what if timestamp is way away from firsttimestamp?
    static double interpolate(long firstTimestamp, double firstValue, long secondTimestamp,
                              double secondValue, long timestamp) {
        if (firstTimestamp == secondTimestamp) {
            return firstValue; // we cannot construct a ratio
        }
        try {
            final double ratio = (double) (timestamp - firstTimestamp) / (secondTimestamp - firstTimestamp);
            return firstValue + ((secondValue - firstValue) * ratio);

        } catch (ArithmeticException e) {
            // likely division by zero due to first.timestamp == second.timestamp
            return firstValue;
        }
    }

    // TODO make a generic version
    static double interpolateCalculatedValue(BgReading first, BgReading second, long timestamp) {
        return interpolate(first.timestamp, first.getDg_mgdl(), second.timestamp, second.getDg_mgdl(), timestamp);
    }
}



