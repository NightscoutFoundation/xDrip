package com.eveningoutpost.dexdrip.utils;

import java.util.ArrayList;
import java.util.List;

import com.eveningoutpost.dexdrip.NFCReaderX;
import com.eveningoutpost.dexdrip.Models.GlucoseData;
import com.eveningoutpost.dexdrip.Models.LibreBlock;
import com.eveningoutpost.dexdrip.Models.ReadingData;

import com.eveningoutpost.dexdrip.Models.UserError.Log;

// This class is used to hold a cache of the latest points in the database.
// It should be used for efficient retrieval of data. (even when showing more than the
// latest 15 minutes of data.)
// This class will be a singleton to allow storing the data from time to time.

public class LibreTrendUtil {

    private static LibreTrendUtil singleton;
    private static final String TAG = "LibreTrendGraph";
    final int MAX_POINTS = 16 * 24 * 60; // Assume that there will not be data for longer than 14 days + some extra.

    long m_latestPointTimestamp = 0;
    long m_latestId = 0;
    ArrayList<LibreTrendPoint> m_points;
    
    public synchronized static LibreTrendUtil getInstance() {
        if(singleton == null) {
           singleton = new LibreTrendUtil();
        }
        Log.e(TAG, "getInstance this = " + singleton);
        return singleton;
     }
    
    LibreTrendUtil() {
        Log.e(TAG, "LibreTrendUtil constructor called this = " + this);
        m_points = new ArrayList<LibreTrendPoint>(MAX_POINTS);// (Collections.nCopies(MAX_POINTS,new LibreTrendPoint()));// Arrays.asList(new LibreTrendPoint[MAX_POINTS]);//new ArrayList<LibreTrendPoint>(MAX_POINTS);
        while(m_points.size() < MAX_POINTS) {
            m_points.add(m_points.size(), new LibreTrendPoint());
        }
    }
    
    
    boolean IsTimeValid(long timeId) {
        return timeId >= 0 && timeId < MAX_POINTS;
    }
    
    List<LibreTrendPoint> getData(long startTimestamp, long endTimestamp) {
        Log.e(TAG, "Size of array is " + m_points.size() + " this = " + this);
        
        long startTime = Math.max(startTimestamp, m_latestPointTimestamp);
        List<LibreBlock> latestBlocks = LibreBlock.getForTrend(startTime, endTimestamp);
        
        Log.e(TAG, "Size of latestBlocks is " + latestBlocks.size());
        for (LibreBlock libreBlock : latestBlocks) {
            ReadingData readingData = NFCReaderX.getTrend(libreBlock);
            // Go over all trend data (from the last to the start)
            for (int i = readingData.trend.size() - 1; i >= 0; i--) {
                GlucoseData glucoseData = readingData.trend.get(i);
                Log.e(TAG, "time = " + glucoseData.sensorTime + " = " + glucoseData.glucoseLevelRaw);
                
                long id = glucoseData.sensorTime;
                if(IsTimeValid(id) == false) {
                    Log.e(TAG, "Error invalid id (time) for bg " + id);
                    return m_points;
                }
                
                if(m_points.get((int)id).rawSensorValue != 0) {
                    // Sanity check that we don't have a bad value.
                    if(m_points.get((int)id).rawSensorValue != glucoseData.glucoseLevelRaw) {
                        Log.e(TAG, "Error for time " + id + " Existing value" + (m_points.get((int)id).rawSensorValue) + 
                                " != " + glucoseData.glucoseLevelRaw); 
                    }
                } else {
                    m_points.get((int)id).rawSensorValue = glucoseData.glucoseLevelRaw;
                    m_points.get((int)id).sensorTime  = id;
                    if(m_latestId > id) {
                        Log.wtf(TAG, "Error latest id is going back " + m_latestId + " " + id);
                        return m_points;
                    }
                    m_latestId = id;
                }
            }
        }
        Log.e(TAG, "Here are the points that we have");
        for(int i =0 ; i < MAX_POINTS ; i++) {
            if(m_points.get(i).rawSensorValue != 0) {
                if(i != m_points.get(i).sensorTime) {
                    Log.e(TAG, "Error in index i = " + i + " sensorTime = " + m_points.get(i).sensorTime);
                }
                Log.e(TAG, "" + i + " " + m_points.get(i).rawSensorValue);
            }
        }
        return m_points;
    }
    
}

class LibreTrendPoint {
    long sensorTime; // The number of minutes from sensor start. //????????? Do we need this
    long rawSensorValue; // The raw value of the sensor 
    
}
