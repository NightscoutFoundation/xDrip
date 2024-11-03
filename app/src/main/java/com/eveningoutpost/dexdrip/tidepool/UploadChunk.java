package com.eveningoutpost.dexdrip.tidepool;


import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;

import com.eveningoutpost.dexdrip.models.APStatus;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Profile;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.LogSlider;
import com.eveningoutpost.dexdrip.utils.NamedSliderProcessor;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lombok.val;

/**
 * jamorham
 * <p>
 * This class gets the next time slice of all data to upload
 */

public class UploadChunk implements NamedSliderProcessor {

    private static final String TAG = "TidepoolUploadChunk";
    private static final String LAST_UPLOAD_END_PREF = "tidepool-last-end";

    private static final long MAX_UPLOAD_SIZE = Constants.DAY_IN_MS * 7; // don't change this
    private static final long DEFAULT_WINDOW_OFFSET = Constants.MINUTE_IN_MS * 15;
    private static final long MAX_LATENCY_THRESHOLD_MINUTES = 1440; // minutes per day

    private static final boolean D = false;

    public static String getNext(final Session session) {
        session.start = getLastEnd();
        session.end = maxWindow(session.start);

        final String result = get(session.start, session.end);
        if (result != null && result.length() < 3) {
            UserError.Log.d(TAG, "No records in this time period, setting start to best end time");
            setLastEnd(Math.max(session.end, getOldestRecordTimeStamp()));
        }
        return result;
    }

    public static String get(final long start, final long end) {

        UserError.Log.d(TAG, "Syncing data between: " + dateTimeText(start) + " -> " + dateTimeText(end));
        if (end <= start) {
            UserError.Log.e(TAG, "End is <= start: " + dateTimeText(start) + " " + dateTimeText(end));
            return null;
        }
        if (end - start > MAX_UPLOAD_SIZE) {
            UserError.Log.e(TAG, "More than max range - rejecting");
            return null;
        }

        final List<BaseElement> records = new LinkedList<>();

        if (!Pref.getBooleanDefaultFalse("tidepool_no_treatments")) {
            records.addAll(getTreatments(start, end));
        }
        records.addAll(getBloodTests(start, end));
        records.addAll(getBasals(start, end));
        records.addAll(getBgReadings(start, end));

        return JoH.defaultGsonInstance().toJson(records);
    }

    private static long getWindowSizePreference() {
        try {
            long value = (long) getLatencySliderValue(Pref.getInt("tidepool_window_latency", 0));
            return Math.max(value * Constants.MINUTE_IN_MS, DEFAULT_WINDOW_OFFSET);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Reverting to default of 15 minutes due to Window Size exception: " + e);
            return DEFAULT_WINDOW_OFFSET; // default
        }
    }

    private static long maxWindow(final long last_end) {
        //UserError.Log.d(TAG, "Max window is: " + getWindowSizePreference());
        return Math.min(last_end + MAX_UPLOAD_SIZE, JoH.tsl() - getWindowSizePreference());
    }

    public static long getLastEnd() {
        long result = PersistentStore.getLong(LAST_UPLOAD_END_PREF);
        return Math.max(result, JoH.tsl() - Constants.MONTH_IN_MS * 2);
    }

    public static void setLastEnd(final long when) {
        if (when > getLastEnd()) {
            PersistentStore.setLong(LAST_UPLOAD_END_PREF, when);
            UserError.Log.d(TAG, "Updating last end to: " + dateTimeText(when));
        } else {
            UserError.Log.e(TAG, "Cannot set last end to: " + dateTimeText(when) + " vs " + dateTimeText(getLastEnd()));
        }
    }

    static List<BaseElement> getTreatments(final long start, final long end) {
        List<BaseElement> result = new LinkedList<>();
        final List<Treatments> treatments = Treatments.latestForGraph(1800, start, end);
        for (Treatments treatment : treatments) {
            if (treatment.carbs > 0) {
                EWizard eWizard = EWizard.fromTreatment(treatment);
                if (eWizard != null) {
                    result.add(eWizard);
                }
            } else if (treatment.insulin > 0) {
                EBolus eBolus = EBolus.fromTreatment(treatment);
                if (eBolus != null) {
                    result.add(eBolus);
                }
            } else {
                // note only TODO
            }
        }
        return result;
    }


    // numeric limits must match max time windows

    static long getOldestRecordTimeStamp() {
        // TODO we could make sure we include records older than the first bg record for completeness

        final long start = 0;
        final long end = JoH.tsl();

        final List<BgReading> bgReadingList = BgReading.latestForGraphAsc(1, start, end);
        if (bgReadingList != null && bgReadingList.size() > 0) {
            return bgReadingList.get(0).timestamp;
        }
        return -1;
    }

    static List<EBloodGlucose> getBloodTests(final long start, final long end) {
        return EBloodGlucose.fromBloodTests(BloodTest.latestForGraph(1800, start, end));
    }

    static List<ESensorGlucose> getBgReadings(final long start, final long end) {
        return ESensorGlucose.fromBgReadings(BgReading.latestForGraphAsc(15000, start, end));
    }

    private static double getRateForApStatus(final APStatus apStatus) {
        if (apStatus.basal_absolute >= 0) {
            return apStatus.basal_absolute;
        }
        return Profile.getBasalRateAbsoluteFromPercent(apStatus.timestamp, apStatus.basal_percent);
    }

    static List<EBasal> getBasals(final long start, final long end) {
        final List<EBasal> basals = new LinkedList<>();
        final List<APStatus> aplistMaster = APStatus.latestForGraph(15000, start - (Constants.HOUR_IN_MS * 3), end, false);
        final List<APStatus> aplist = new LinkedList<>();
        APStatus previous = null;
        for (val apStatus : aplistMaster) {
            if (apStatus.timestamp < start) {
                previous = apStatus;
                continue;
            } else {
                // add opening record starting at start time, using previous record if suitable
                if (previous != null && apStatus.timestamp != start) {
                    previous.timestamp = start;
                    aplist.add(previous);
                    previous = null;
                }
            }
            aplist.add(apStatus);
        }

        // add opening record starting at start time, using previous record if suitable
        // but only if there were no records falling within our actual time span
        if (previous != null && aplist.size() == 0) {
            previous.timestamp = start;
            aplist.add(previous);
        }

        if (aplist.size() > 0) {
            // add closing record up to end time
            val apStatus = aplist.get(aplist.size() - 1);
            if (apStatus.timestamp < end) {
                aplist.add(new APStatus(end, apStatus.basal_percent, apStatus.basal_absolute));
            }
        }
        EBasal current = null;
        for (APStatus apStatus : aplist) {
            final double this_rate = getRateForApStatus(apStatus);

            if (current != null) {
                if (this_rate != current.rate || apStatus.timestamp == end) {
                    current.duration = apStatus.timestamp - current.timestamp;
                    UserError.Log.d(TAG, "Adding current: " + current.toS());
                    if (current.isValid()) {
                        basals.add(current);
                    } else {
                        UserError.Log.e(TAG, "Current basal is invalid: " + current.toS());
                    }
                    current = null;
                } else {
                    UserError.Log.d(TAG, "Same rate as previous basal record: " + current.rate + " " + apStatus.toS());
                }
            }
            if (current == null) {
                current = new EBasal(this_rate, apStatus.timestamp, 0, UUID.nameUUIDFromBytes(("tidepool-basal" + apStatus.timestamp).getBytes()).toString()); // start duration is 0
            }
        }
        if (D) {
            for (val b : basals) {
                UserError.Log.e(TAG, b.toS());
            }
        }
        return basals;
    }

    @Override
    public int interpolate(final String name, final int position) {
        switch (name) {
            case "latency":
                return getLatencySliderValue(position);
        }
        throw new RuntimeException("name not matched in interpolate");
    }

    private static int getLatencySliderValue(final int position) {
        return (int) LogSlider.calc(0, 300, 15, MAX_LATENCY_THRESHOLD_MINUTES, position);
    }
}
