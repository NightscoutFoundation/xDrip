package com.eveningoutpost.dexdrip.utilitymodels;

import com.eveningoutpost.dexdrip.NewDataObserver;
import com.eveningoutpost.dexdrip.models.APStatus;
import com.eveningoutpost.dexdrip.models.DateUtil;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class NightscoutBasalRate {
    private static final boolean d = false;
    private static final String TAG = "NightscoutBasalRate";
    private static final int requiredSegments = 24;
    private static final int minimumProfileBRDuration = 1000;

    private static ArrayList<APStatusEntry> statusEntries = new ArrayList<>();
    private static List<Double> profile = new ArrayList<>();

    public static void setTreatments(final String response) throws Exception {
        final JSONArray jsonArray = new JSONArray(response);

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject tr = (JSONObject) jsonArray.get(i);
            final String etype = tr.has("eventType") ? tr.getString("eventType") : "<null>";

            if (!etype.equals("Temp Basal")) {
                continue;
            }

            Double absolute = null;
            Long durationInMilliseconds = null;
            Long timestamp = null;

            try {
                absolute = tr.has("absolute") ? tr.getDouble("absolute") : null;
            } catch (JSONException e) {
                UserError.Log.e(TAG, "Could not parse absolute: " + tr.get("absolute"));
            }

            try {
                durationInMilliseconds = tr.has("durationInMilliseconds") ? tr.getLong("durationInMilliseconds") : null;
            } catch (JSONException e) {
                UserError.Log.e(TAG, "Could not parse durationInMilliseconds: " + tr.get("durationInMilliseconds"));
            }

            try {
                timestamp = tr.has("date") ? tr.getLong("date") : null;
            } catch (JSONException e) {
                UserError.Log.e(TAG, "Could not parse date: " + tr.get("date"));
            }

            if (absolute != null && durationInMilliseconds != null && timestamp != null) {
                APStatus lastAPStatus = APStatus.last();

                if (lastAPStatus != null && timestamp < lastAPStatus.timestamp) {
                    if (d) {

                        UserError.Log.ueh(TAG, "APStatus exists: - value: " + lastAPStatus.basal_absolute + " - timestamp: " + JoH.dateTimeText(lastAPStatus.timestamp) + " Omitting: - value: " + absolute + " - timestamp: " + JoH.dateTimeText(timestamp));
                    }
                    continue;
                }

                if (d) {
                    UserError.Log.ueh(TAG, "New Temp Basal from Nightscout: - value: " + absolute + " - timestamp: " + JoH.dateTimeText(timestamp));
                }

                statusEntries.add(new APStatusEntry(absolute, timestamp, durationInMilliseconds, false));
            }
        }

        tryProcessBasalRate();
    }

    public static void setProfile(List<Double> loadedProfile) {
        profile = loadedProfile;

        tryProcessBasalRate();
    }

    public static void tryProcessBasalRate() {
        if (statusEntries.isEmpty() || profile.isEmpty()) {
            if (d) {
                UserError.Log.ueh(TAG, "Trying to process Nightscout basal rate, but:" + (statusEntries.isEmpty() ? " - treatments are still empty" : "") + (profile.isEmpty() ? " - profile is still empty" : ""));
            }
            return;
        }

        long nowInMilliseconds = System.currentTimeMillis();

        if (d) {
            UserError.Log.ueh(TAG, "Process Nightscout basal rate with " + statusEntries.size() + " new tbr entries and " + profile.size() + " profile segments.");
        }

        statusEntries.sort(APStatusEntry::compare);

        for (int i = 0; statusEntries.size() >= i + 1; i++) {
            int nextIndex = i + 1;

            boolean hasNext = nextIndex < statusEntries.size();

            APStatusEntry currentEntry = statusEntries.get(i);
            APStatusEntry nextEntry = hasNext ? statusEntries.get(nextIndex) : null;

            long currentEntryFinishTimestamp = currentEntry.timestamp + currentEntry.durationInMilliseconds;

            if (currentEntryFinishTimestamp < nowInMilliseconds && (nextEntry == null || nextEntry.timestamp > currentEntryFinishTimestamp)) {
                APStatusEntry nextSegment = getNextSegment(
                    currentEntryFinishTimestamp,
                    profile,
                    Math.min(nowInMilliseconds, nextEntry != null ? nextEntry.timestamp : Long.MAX_VALUE)
                );

                if (nextSegment.durationInMilliseconds < minimumProfileBRDuration) {
                    continue;
                }

                if (d) {
                    UserError.Log.ueh(TAG, "Adding next profile segment: " + nextSegment.absolute + "U, timestamp: " + JoH.dateTimeText(nextSegment.timestamp) + ", duration: " + nextSegment.durationInMilliseconds + "ms");
                }

                statusEntries.add(nextIndex, nextSegment);
            }
        }

        statusEntries.forEach(entry -> {
            APStatus.createEfficientRecord(entry.timestamp, entry.absolute);
        });

        NewDataObserver.newExternalStatus(false);

        statusEntries = new ArrayList<>();
        profile = new ArrayList<>();
    }

    static APStatusEntry getNextSegment(long lastFinishTimestamp, List<Double> profile, long nextTimestamp) {
        int hour = getHourOfTheDay(lastFinishTimestamp);
        double absolute = profile.get(hour);
        long finishTimestamp = getHourFinishTimestamp(lastFinishTimestamp, nextTimestamp);

        long duration = finishTimestamp - lastFinishTimestamp;

        return new APStatusEntry(absolute, lastFinishTimestamp, duration, true);
    }

    static int getHourOfTheDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(timestamp);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    static long getHourFinishTimestamp(long timestamp, long nextTimestamp) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTimeInMillis(timestamp);
        calendar.add(Calendar.HOUR, 1);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MILLISECOND, -1);

        return Math.min(calendar.getTimeInMillis(), nextTimestamp);
    }
}

class APStatusEntry {
    double absolute;
    long timestamp;
    // For debugging
    long durationInMilliseconds;
    double durationInMinutes;
    String timestampText;
    String timestampEndText;
    boolean isProfile;
    boolean isTBR;

    public APStatusEntry(double absolute, long timestamp, long durationInMilliseconds, boolean isProfile) {
        this.absolute = absolute;
        this.timestamp = timestamp;
        this.durationInMilliseconds = durationInMilliseconds;
        this.durationInMinutes = (double) Math.round((double) durationInMilliseconds / 1000) / 60;

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());

        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        this.timestampText = df.format(new Date(timestamp));
        this.timestampEndText = df.format(new Date(timestamp + durationInMilliseconds));
        this.isProfile = isProfile;
        this.isTBR = !isProfile;
    }

    public static int compare(APStatusEntry a, APStatusEntry b) {
        return (int) (a.timestamp - b.timestamp);
    }
}