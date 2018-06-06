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

// This class is used to hold a cache of the latest points in the database.
// It should be used for efficient retrieval of data. (even when showing more than the
// latest 15 minutes of data.)
// This class will be a singleton to allow storing the data from time to time.

public class LibreTrendUtil {

    private static LibreTrendUtil singleton;
    private static final String TAG = "LibreTrendGraph";
    final int MAX_POINTS = 16 * 24 * 60; // Assume that there will not be data for longer than 14 days + some extra.

    private LibreTrendLatest m_libreTrendLatest;
    
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
        m_libreTrendLatest = new LibreTrendLatest();
    }
    
    
    boolean IsTimeValid(long timeId) {
        return timeId >= 0 && timeId < MAX_POINTS;
    }
    
    List<LibreTrendPoint> getData(long startTimestamp, long endTimestamp) {
        Log.e(TAG, "Size of array is " + m_points.size() + " this = " + this);
        
        long startTime = Math.max(startTimestamp, m_libreTrendLatest.timestamp);
        List<LibreBlock> latestBlocks = LibreBlock.getForTrend(startTime, endTimestamp);
        
        Log.e(TAG, "Size of latestBlocks is " + latestBlocks.size());
        
        // Go for the last libreBlock and get calculated bg and timestamp.
        if (latestBlocks.size() > 0) {
            LibreBlock lastBlock = latestBlocks.get(latestBlocks.size() - 1);
            // 
            ReadingData readingData = NFCReaderX.getTrend(lastBlock);
            if(readingData.trend.size() > 0 ) {
                m_libreTrendLatest.id = readingData.trend.get(0).sensorTime;
                m_libreTrendLatest.glucoseLevelRaw = readingData.trend.get(0).glucoseLevelRaw;
            } else {
                Log.wtf(TAG, "Error no readingData.trend for this point, returning withoug doing anything");
                return m_points;
            }
            m_libreTrendLatest.timestamp = lastBlock.timestamp;
            m_libreTrendLatest.bg = lastBlock.calculated_bg;
            String time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date((long) m_libreTrendLatest.timestamp));
            Log.e(TAG, "Latest values " + time + " m_latestId = " + m_libreTrendLatest.id  + " m_libreTrendLatest.m_GlucoseLevelRaw = " + m_libreTrendLatest.glucoseLevelRaw + " bg = " + m_libreTrendLatest.bg );
             
        }
        
        
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
                    if(m_libreTrendLatest.id < id) {
                        Log.wtf(TAG, "Error - we have seen an id bigger than latest id. m_libreTrendLatest.m_Id = " + m_libreTrendLatest.id + " id = " + id);
                        return m_points;
                    }
                    
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

// Represents the last point that we have data on.
class LibreTrendLatest {
    long timestamp = 0;
    long id = 0;
    double bg = 0;
    int glucoseLevelRaw;
}
