package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

final class MedtrumFollowData {

    private static final double MIN_GLUCOSE_MG_DL = 36d;
    private static final double MAX_GLUCOSE_MG_DL = 600d;

    static final class Entry {
        final long timestamp;
        final double glucose;
        @Nullable final String trendName;

        Entry(final long timestamp, final double glucose, @Nullable final String trendName) {
            this.timestamp = timestamp;
            this.glucose = glucose;
            this.trendName = trendName;
        }
    }

    static final class Result {
        final List<Entry> entries;
        final String patient;
        final String error;

        Result(final List<Entry> entries, final String patient, final String error) {
            this.entries = entries;
            this.patient = patient;
            this.error = error;
        }

        boolean isSuccessful() {
            return error == null;
        }
    }

    private MedtrumFollowData() {
    }

    static Result parseMonitor(final JsonObject response, final String configuredPatient) {
        final String responseError = responseError(response);
        if (responseError != null) return new Result(new ArrayList<>(), "", responseError);

        final JsonArray monitors = getArray(response, "monitorlist");
        if (monitors == null || monitors.size() == 0) {
            return new Result(new ArrayList<>(), "", "No followed patients returned by EasyView");
        }

        JsonObject selected = null;
        final String wanted = configuredPatient == null ? "" : configuredPatient.trim();
        for (final JsonElement element : monitors) {
            if (!element.isJsonObject()) continue;
            final JsonObject candidate = element.getAsJsonObject();
            final String username = getString(candidate, "username");
            if ((!wanted.isEmpty() && wanted.equalsIgnoreCase(username))
                    || (wanted.isEmpty() && candidate.has("sensor_status"))) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return new Result(new ArrayList<>(), wanted, wanted.isEmpty()
                    ? "No patient with sensor data returned by EasyView"
                    : "Configured EasyView patient was not found");
        }

        final String patient = getString(selected, "username");
        final JsonObject sensor = getObject(selected, "sensor_status");
        if (sensor == null) return new Result(new ArrayList<>(), patient, "Patient has no sensor data");

        final double glucose = getDouble(sensor, "glucose", 0);
        final long timestamp = getLong(sensor, "updateTime", 0) * 1000L;
        if (glucose <= 0 || timestamp <= 0) {
            return new Result(new ArrayList<>(), patient, sensorStateError(sensor));
        }

        final double glucoseMgDl = toMgDl(glucose);
        if (!isValidGlucose(glucoseMgDl)) {
            return new Result(new ArrayList<>(), patient, "EasyView returned an out-of-range glucose value");
        }
        final List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(timestamp, glucoseMgDl, trendName((int) getLong(sensor, "glucoseRate", -1))));
        return new Result(entries, patient, null);
    }

    static Result parseGraph(final JsonObject response, final long notBefore) {
        final String responseError = responseError(response);
        if (responseError != null) return new Result(new ArrayList<>(), "", responseError);

        final List<Entry> entries = new ArrayList<>();
        final JsonArray data = getArray(response, "data");
        if (data == null) return new Result(entries, "", null);
        for (final JsonElement element : data) {
            if (!element.isJsonArray()) continue;
            final JsonArray row = element.getAsJsonArray();
            if (row.size() < 4) continue;
            try {
                final long timestamp = (long) (row.get(1).getAsDouble() * 1000L);
                final double glucose = row.get(3).getAsDouble();
                final double glucoseMgDl = toMgDl(glucose);
                if (timestamp >= notBefore && isValidGlucose(glucoseMgDl)) {
                    entries.add(new Entry(timestamp, glucoseMgDl, null));
                }
            } catch (RuntimeException ignored) {
                // Ignore malformed individual history rows while retaining valid values.
            }
        }
        return new Result(entries, "", null);
    }

    static String responseError(final JsonObject response) {
        if (response == null) return "Empty response from EasyView";
        final String result = getString(response, "res");
        if (!result.isEmpty() && !"OK".equalsIgnoreCase(result)) {
            final String message = getString(response, "msg");
            return message.isEmpty() ? "EasyView returned " + result : message;
        }
        return null;
    }

    private static String sensorStateError(final JsonObject sensor) {
        final long sequence = getLong(sensor, "sequence", -1);
        if (sequence >= 0 && sequence <= 15) return "EasyView sensor is warming up";
        final long calibrationAt = getLong(sensor, "nextSequenceNeedCalibrate", Long.MAX_VALUE);
        if (sequence >= calibrationAt) return "EasyView sensor needs calibration";
        return "EasyView returned no valid glucose value";
    }

    private static double toMgDl(final double value) {
        return value <= MAX_GLUCOSE_MG_DL / Constants.MMOLL_TO_MGDL
                ? value * Constants.MMOLL_TO_MGDL : value;
    }

    private static boolean isValidGlucose(final double valueMgDl) {
        return valueMgDl >= MIN_GLUCOSE_MG_DL && valueMgDl <= MAX_GLUCOSE_MG_DL;
    }

    @Nullable
    private static String trendName(final int trend) {
        switch (trend) {
            case 1: return "FortyFiveUp";
            case 2: return "SingleUp";
            case 3: return "DoubleUp";
            case 4: return "FortyFiveDown";
            case 5: return "SingleDown";
            case 6: return "DoubleDown";
            case 0:
            case 8: return "Flat";
            default: return null;
        }
    }

    private static JsonArray getArray(final JsonObject object, final String name) {
        try {
            return object.has(name) && object.get(name).isJsonArray() ? object.getAsJsonArray(name) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static JsonObject getObject(final JsonObject object, final String name) {
        try {
            return object.has(name) && object.get(name).isJsonObject() ? object.getAsJsonObject(name) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String getString(final JsonObject object, final String name) {
        try {
            return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : "";
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static long getLong(final JsonObject object, final String name, final long fallback) {
        try {
            return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsLong() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static double getDouble(final JsonObject object, final String name, final double fallback) {
        try {
            return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsDouble() : fallback;
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}


