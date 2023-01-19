package com.eveningoutpost.dexdrip.utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.ReadingData;

import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.utils.LibreTrendPoint;

import com.eveningoutpost.dexdrip.Models.UserError.Log;

/* 
    This class helps to retrieve the latest libre trend points. It holds data of one sensor only.
    So, it can hold data of up to 14.5 days (actually storage is of 16 days).
    It gets it's data from LibreBlock, and caches the results that it sees.
    It always work form the first point to the last point, so when asked to get data, it gets it by the time.
    This class will be a singleton to allow storing the data from time to time. (For more efficient operation).
    
    For each sensor, we hold the data based on the minute from start that we see.
    If we missed more than 3 readings we might not have data for some time.
    
    If we identify a change in sensor id, we clear all existing points and start calculating again.
    

*/

// Represents the last point that we have data on.
class LibreTrendLatest {
    long timestamp = 0;
    int id = 0;

    // bg and glucoseLevelRaw might be from a previous point that we have data for.
    private double bg = 0;
    private int glucoseLevelRaw;
    String SensorSN;

    // A factor of zero, means that the object is not valid.
    double getFactor() {
        if (glucoseLevelRaw == 0) {
            return 0;
        }
        return bg / glucoseLevelRaw;
    }

    void setFactorData(int glucoseLevelRaw, double bg) {
        this.glucoseLevelRaw = glucoseLevelRaw;
        this.bg = bg;
    }

    void updateLastReading(LibreBlock libreBlock) {
        List<GlucoseData> trend = NFCReaderX.getLibreTrend(libreBlock);
        if (trend == null || trend.size() == 0 || trend.get(0).glucoseLevelRaw == 0 || libreBlock.timestamp < timestamp) {
            return;
        }
        this.timestamp = libreBlock.timestamp;
        this.bg = libreBlock.calculated_bg;
        this.id = trend.get(0).sensorTime;
        this.glucoseLevelRaw = trend.get(0).glucoseLevelRaw;
    }


    public String toString() {
        return "{ timestamp " + JoH.dateTimeText(timestamp) + " id " + id + " bg " + bg + " glucoseLevelRaw " + glucoseLevelRaw + " SensorSN " + SensorSN + "}";
    }
}

class LazyLibreList extends ArrayList<LibreTrendPoint> {

    public LazyLibreList(int max_points) {
        super(max_points);
        while (super.size() < max_points) {
            super.add(null);
        }
    }

    @Override
    public LibreTrendPoint get(int index) {
        LibreTrendPoint v = super.get(index);
        if (v == null) {
            v = new LibreTrendPoint();
            super.set(index, v);
        }
        return v;
    }
}

public class LibreTrendUtil {

    private static LibreTrendUtil singleton;
    private static final String TAG = "LibreTrendGraph";
    private static final boolean debug_per_minute = false;
    public final static int MAX_POINTS = 16 * 24 * 60; // Assume that there will not be data for longer than 14 days + some extra.

    private LibreTrendLatest m_libreTrendLatest;

    ArrayList<LibreTrendPoint> m_points;

    public void updateLastReading(LibreBlock libreBlock) {
        // Before we update m_libreTrendLatest we call getData as it affects the cache.
        // If there is no timestamp, only take the last 60 minutes.
        long startTime = m_libreTrendLatest.timestamp > 0 ? m_libreTrendLatest.timestamp :
                libreBlock.timestamp - 60 * Constants.MINUTE_IN_MS;
        getData(startTime, libreBlock.timestamp, false);
        m_libreTrendLatest.updateLastReading(libreBlock);
    }

    public synchronized static LibreTrendUtil getInstance() {
        if (singleton == null) {
            singleton = new LibreTrendUtil();
        }
        Log.i(TAG, "getInstance this = " + singleton);
        return singleton;
    }

    LibreTrendUtil() {
        Log.i(TAG, "LibreTrendUtil constructor called this = " + this);
        Reset();
    }

    void ResetPoints() {
        m_points = new LazyLibreList(MAX_POINTS);
    }

    void Reset() {
        ResetPoints();
        m_libreTrendLatest = new LibreTrendLatest();

    }

    boolean IsTimeValid(long timeId) {
        return timeId >= 0 && timeId < MAX_POINTS;
    }

    public List<LibreTrendPoint> getData(long startTimestamp, long endTimestamp, boolean calculate_factor) {
        Log.i(TAG, "getData called startTimestamp = " + JoH.dateTimeText(startTimestamp) + " endTimestamp = " + JoH.dateTimeText(endTimestamp) +
                " Size of array is " + m_points.size() + " this = " + this + " m_libreTrendLatest.timestamp " + JoH.dateTimeText(m_libreTrendLatest.timestamp));

        long startTime = Math.max(startTimestamp, m_libreTrendLatest.timestamp);
        // The extra 1 is to make sure we don't read the last packet again and again.
        List<LibreBlock> latestBlocks = LibreBlock.getForTrend(startTime + 1, endTimestamp);

        Log.i(TAG, "Size of latestBlocks is " + latestBlocks.size());
        if (latestBlocks.size() > 0) {
            Log.i(TAG, "Last packet timestamp is " + latestBlocks.get(latestBlocks.size() - 1).timestamp);
        }
        if (calculate_factor) {
            CalculateFactor(latestBlocks);
        }

        // Go over all blocks from the earlier to the latest, and fill the data.
        for (LibreBlock libreBlock : latestBlocks) {
            AddLibreblock(libreBlock);
        }
        if (debug_per_minute) {
            Log.i(TAG, "Here are the points that we have");

            for (int i = 0; i < MAX_POINTS; i++) {
                if (m_points.get(i).rawSensorValue != 0) {
                    if (i != m_points.get(i).sensorTime) {
                        Log.i(TAG, "Error in index i = " + i + " sensorTime = " + m_points.get(i).sensorTime);
                    }
                    // Only print last 60 minutes.
                    if (m_libreTrendLatest.id - i < 60) {
                        Log.i(TAG, "" + i + " " + m_points.get(i).rawSensorValue);
                    }
                }
            }
        }
        return m_points;
    }

    private void AddLibreblock(LibreBlock libreBlock) {
        Log.i(TAG, "AddLibreblock called timestamp = " + JoH.dateTimeText(libreBlock.timestamp));
        if (!libreBlock.reference.equals(m_libreTrendLatest.SensorSN)) {
            Log.i(TAG, "Detected a sensor change (or a new one); new serial number is " + libreBlock.reference);
            ResetPoints();
            m_libreTrendLatest.SensorSN = libreBlock.reference;
        }

        List<GlucoseData> trend = NFCReaderX.getLibreTrend(libreBlock);
        if (trend == null) {
            Log.i(TAG, "NFCReaderX.getTrend returned null, ignoring reading");
            return;
        }
        // Go over all trend data (from the earlier to the later)
        for (int i = trend.size() - 1; i >= 0; i--) {
            GlucoseData glucoseData = trend.get(i);
            if (debug_per_minute) {
                Log.i(TAG, "time = " + glucoseData.sensorTime + " = " + glucoseData.glucoseLevelRaw);
            }

            long id = glucoseData.sensorTime;
            if (IsTimeValid(id) == false) {
                Log.e(TAG, "Error invalid id (time) for bg " + id);
                return;
            }
            Log.i(TAG, "maybe Adding a point with id " + id);

            if (m_points.get((int) id).rawSensorValue == 0) {
                m_points.get((int) id).rawSensorValue = glucoseData.glucoseLevelRaw;
                m_points.get((int) id).sensorTime = id;
                m_points.get((int) id).flags = glucoseData.flags;
                m_points.get((int) id).source = glucoseData.source;
            }
        }
    }

    private void CalculateFactor(List<LibreBlock> latestBlocks) {
        // Go for the last libreBlock and get calculated bg and timestamp.
        ListIterator<LibreBlock> li = latestBlocks.listIterator(latestBlocks.size());
        long lastBlockTime = 0;
        boolean isLast = true;
        while (li.hasPrevious()) {
            LibreBlock libreBlock = li.previous();
            // Get of the loop if no valid data in the last 15 minutes.
            if (lastBlockTime == 0) {
                lastBlockTime = libreBlock.timestamp;
            } else {
                if (JoH.msSince(lastBlockTime, libreBlock.timestamp) > 16 * 60 * 1000) {
                    // We have readings for the last 16 minutes, but none of them has a BG value.
                    // This should not happen, but if it does, data is too old to be used.
                    Log.w(TAG, "getData was not able to find a valid time - quiting");
                    break;
                }
            }
            if (isLast) {
                List<GlucoseData> trend = NFCReaderX.getLibreTrend(libreBlock);
                if (trend == null || trend.size() == 0) {
                    Log.w(TAG, "Error: NFCReaderX.getTrend returned null or empty for latest block");
                    continue;
                }
                // The last object is used to calculate the timestamp and id.
                isLast = false;
                m_libreTrendLatest.id = (int) trend.get(0).sensorTime;
                m_libreTrendLatest.timestamp = libreBlock.timestamp;
            }

            // Now trying to get a valid object with BG and a raw value.
            if (libreBlock.calculated_bg == 0) {
                continue;
            }
            List<GlucoseData> trend = NFCReaderX.getLibreTrend(libreBlock);
            if (trend == null || trend.size() == 0) {
                Log.e(TAG, "Error: NFCReaderX.getTrend returned null or empty for latest block");
                return;
            }
            if (trend.get(0).glucoseLevelRaw == 0) {
                continue;
            }

            m_libreTrendLatest.setFactorData(trend.get(0).glucoseLevelRaw, libreBlock.calculated_bg);
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date((long) m_libreTrendLatest.timestamp));
            Log.i(TAG, "Latest values with valid bg " + time + " m_latestId = " + m_libreTrendLatest.id + " m_libreTrendLatest.m_GlucoseLevelRaw = " + trend.get(0).glucoseLevelRaw + " bg = " + libreBlock.calculated_bg);
            // We have finished the calculations, so getting out.
            break;
        }
    }

    LibreTrendLatest getLibreTrendLatest() {
        return m_libreTrendLatest;
    }
}


