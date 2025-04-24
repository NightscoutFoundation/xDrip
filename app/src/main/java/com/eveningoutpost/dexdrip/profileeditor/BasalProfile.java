package com.eveningoutpost.dexdrip.profileeditor;

import static com.eveningoutpost.dexdrip.models.JoH.JsonStringToFloatList;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import lombok.val;

// jamorham

public class BasalProfile {

    private static final String[] PROFILE_NAMES = {"1", "2", "3", "4", "5"};
    private static final String BASAL_PREFIX = "BASAL-PROFILE-";
    private static final String ACTIVE_BASAL_PROFILE = "ACTIVE-BASAL-PROFILE";

    private static String getPrefix(final String ref) {
        return BASAL_PREFIX + ref;
    }

    public static void save(final String ref, final List<Double> segments) {
        Pref.setString(getPrefix(ref), JoH.defaultGsonInstance().toJson(segments));
        //android.util.Log.d("PROFILEXX", "Saved value:" + Pref.getString(getPrefix(ref), "null"));
    }

    public static List<Float> load(final String ref) {
        return JsonStringToFloatList(Pref.getString(getPrefix(ref), ""));
    }


    static int getHourOfTheDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(timestamp);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    static int getDifferenceInFullDays(long timestampA, long timestampB) {
        Calendar calendarA = Calendar.getInstance();
        Calendar calendarB = Calendar.getInstance();

        calendarA.setTimeInMillis(timestampA);
        calendarB.setTimeInMillis(timestampB);

        return calendarB.get(Calendar.DAY_OF_YEAR) - calendarA.get(Calendar.DAY_OF_YEAR) + 1;
    }

    public static List<BasalProfileEntryTimed> loadForTimeSpan(final String ref, long startTime, long endTime) {
        final List<Float> profile = load(ref);

        int fullDays = getDifferenceInFullDays(startTime, endTime);

        List<Float> profileForAllDays = new ArrayList<>();

        for (int i = 0; i < fullDays; i++) {
            profileForAllDays.addAll(profile);
        }

        int startHour = getHourOfTheDay(startTime);
        int endHour = getHourOfTheDay(endTime);

        List<Float> sublist = profileForAllDays.subList(startHour, profileForAllDays.size() - 24 + endHour);
        List<BasalProfileEntryTimed> timed = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(startTime);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        sublist.forEach(entry -> {
            timed.add(new BasalProfileEntryTimed(entry, calendar.getTimeInMillis()));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        });

        return timed;
    }

    public static String getActiveRateName() {
        return Pref.getString(ACTIVE_BASAL_PROFILE, "1");
    }

    private static <T> int howManyMatch(final List<T> segments, final int start, final int maxMatches) {
        val current = segments.get(start);
        int matches = 1;
        for (int pos = start + 1; pos < segments.size(); pos++) {
            if (current.equals(segments.get(pos))) {
                matches++;
                if (matches >= maxMatches) {
                    break;
                }
            } else {
                break;
            }
        }
        return matches;
    }

    /**
     * Compress a list of elements where there are consistent repeated identical segments
     * @param segments
     * @param minSize
     */
    public static <T> List<T> consolidate(List<T> segments, final int minSize) {
        if (segments == null) return null;
        if (segments.size() <= minSize) return segments;

        int maxMatches = segments.size() / minSize;
        int iterations = 0;

        while (iterations++ < 1440) {
            final List<T> newSegments = new LinkedList<>();
            int referencePtr = 0;
            int matches;
            boolean cleanRun = true;
            int referenceMatches = -1;

            while (referencePtr < segments.size()) {
                newSegments.add(segments.get(referencePtr));
                matches = howManyMatch(segments, referencePtr, maxMatches);
                if (referenceMatches == -1) {
                    referenceMatches = matches;
                }
                if (matches != referenceMatches) {
                    cleanRun = false;
                    break;
                }
                referencePtr += matches;
            }
            if (cleanRun) {
                return newSegments;
            } else {
                maxMatches = referenceMatches - 1;
            }
        }
        return segments;
    }


    public static String getAllProfilesAsJson() {
        val profiles = new JSONArray();
        for (val profile : PROFILE_NAMES) {
            val item = new JSONObject();
            try {
                val result = Pref.getString(getPrefix(profile), null);
                if (result != null && result.length() > 5) {
                    item.put(profile, result);
                    profiles.put(item);
                }
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        }
        return profiles.length() > 0 ? profiles.toString() : null;
    }

}