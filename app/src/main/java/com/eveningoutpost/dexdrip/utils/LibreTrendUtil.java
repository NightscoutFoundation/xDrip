package com.eveningoutpost.dexdrip.utils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.ReadingData;

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
// This class represents a per minute data from the libre.
class LibreTrendPoint {
    long sensorTime; // The number of minutes from sensor start.
    long rawSensorValue; // The raw value of the sensor 
}

// Represents the last point that we have data on.
class LibreTrendLatest {
    long timestamp = 0;
    int id = 0;
    double bg = 0;
    int glucoseLevelRaw;
    String SensorSN; 
}

public class LibreTrendUtil {

    private static LibreTrendUtil singleton;
    private static final String TAG = "LibreTrendGraph";
    private static final boolean debug_per_minute = false;
    final int MAX_POINTS = 16 * 24 * 60; // Assume that there will not be data for longer than 14 days + some extra.

    private LibreTrendLatest m_libreTrendLatest;
    
    ArrayList<LibreTrendPoint> m_points;
    
    public synchronized static LibreTrendUtil getInstance() {
        if(singleton == null) {
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
        m_points = new ArrayList<LibreTrendPoint>(MAX_POINTS);
        while(m_points.size() < MAX_POINTS) {
            m_points.add(m_points.size(), new LibreTrendPoint());
        }
    }
    
    void Reset() {
        ResetPoints();
        m_libreTrendLatest = new LibreTrendLatest();
 
    }
    
    boolean IsTimeValid(long timeId) {
        return timeId >= 0 && timeId < MAX_POINTS;
    }
    
    List<LibreTrendPoint> getData(long startTimestamp, long endTimestamp) {
        Log.i(TAG, "Size of array is " + m_points.size() + " this = " + this);
        
        long startTime = Math.max(startTimestamp, m_libreTrendLatest.timestamp);
        List<LibreBlock> latestBlocks = LibreBlock.getForTrend(startTime, endTimestamp);
        
        Log.i(TAG, "Size of latestBlocks is " + latestBlocks.size());
        
        // Go for the last libreBlock and get calculated bg and timestamp.
        if (latestBlocks.size() > 0) {
            LibreBlock lastBlock = latestBlocks.get(latestBlocks.size() - 1);
            // 
            ReadingData readingData = NFCReaderX.getTrend(lastBlock);
            if(readingData == null){
                Log.e(TAG, "Error: NFCReaderX.getTrend retuned null for latest block");
                return m_points;
            }
            if(readingData.trend.size() > 0 ) {
                m_libreTrendLatest.id = (int)readingData.trend.get(0).sensorTime;
                m_libreTrendLatest.glucoseLevelRaw = readingData.trend.get(0).glucoseLevelRaw;
            } else {
                Log.e(TAG, "Error no readingData.trend for this point, returning withoug doing anything");
                return m_points;
            }
            m_libreTrendLatest.timestamp = lastBlock.timestamp;
            m_libreTrendLatest.bg = lastBlock.calculated_bg;
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date((long) m_libreTrendLatest.timestamp));
            Log.i(TAG, "Latest values " + time + " m_latestId = " + m_libreTrendLatest.id  + " m_libreTrendLatest.m_GlucoseLevelRaw = " + m_libreTrendLatest.glucoseLevelRaw + " bg = " + m_libreTrendLatest.bg );
        }
        
        // Go over all blocks from the earlier to the latest, and fill the data.
        for (LibreBlock libreBlock : latestBlocks) {
            if(!libreBlock.reference.equals(m_libreTrendLatest.SensorSN)) {
                Log.i(TAG, "Detected a sensor change new sn is " + libreBlock.reference);
                ResetPoints();
                m_libreTrendLatest.SensorSN = libreBlock.reference;
            }
            
            ReadingData readingData = NFCReaderX.getTrend(libreBlock);
            if(readingData == null) {
                Log.i(TAG, "NFCReaderX.getTrend returned null, ignoring reading");
                continue;
            }
            // Go over all trend data (from the earlier to the later)
            for (int i = readingData.trend.size() - 1; i >= 0; i--) {
                GlucoseData glucoseData = readingData.trend.get(i);
                if (debug_per_minute) {
                    Log.i(TAG, "time = " + glucoseData.sensorTime + " = " + glucoseData.glucoseLevelRaw);
                }
                
                long id = glucoseData.sensorTime;
                if(IsTimeValid(id) == false) {
                    Log.e(TAG, "Error invalid id (time) for bg " + id);
                    return m_points;
                }
                
                if(m_points.get((int)id).rawSensorValue == 0) {
                    m_points.get((int)id).rawSensorValue = glucoseData.glucoseLevelRaw;
                    m_points.get((int)id).sensorTime  = id;
                    if(m_libreTrendLatest.id < id && m_libreTrendLatest.id != 0) {
                        Log.wtf(TAG, "Error - we have seen an id bigger than latest id. m_libreTrendLatest.m_Id = " + m_libreTrendLatest.id + " id = " + id);
                        Reset();
                    }
                }
            }
        }
        if(debug_per_minute) {
            Log.i(TAG, "Here are the points that we have");
        
            for(int i =0 ; i < MAX_POINTS ; i++) {
                if(m_points.get(i).rawSensorValue != 0) {
                    if(i != m_points.get(i).sensorTime) {
                        Log.i(TAG, "Error in index i = " + i + " sensorTime = " + m_points.get(i).sensorTime);
                    }
                    // Only print last 60 minutes.
                    if(m_libreTrendLatest.id - i <  60) {
                        Log.i(TAG, "" + i + " " + m_points.get(i).rawSensorValue);
                    }
                }
            }
        }
        return m_points;
    }
    LibreTrendLatest getLibreTrendLatest() {
        return m_libreTrendLatest;
    }
}


