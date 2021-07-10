package com.eveningoutpost.dexdrip.Models;

// class from LibreAlarm

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import java.util.ArrayList;
import java.util.List;

public class ReadingData {

    public List<GlucoseData> trend;
    public List<GlucoseData> history;
    public byte[] raw_data;

    public ReadingData() {
        this.trend = new ArrayList<>();
        this.history = new ArrayList<>();
        // The two bytes are needed here since some components don't like a null pointer.
        this.raw_data = new byte[2];
    }

    public ReadingData(List<GlucoseData> trend, List<GlucoseData> history) {
        this.trend = trend;
        this.history = history;
    }

    public static class TransferObject {
        public long id;
        public ReadingData data;

        public TransferObject() {}

        public TransferObject(long id, ReadingData data) {
            this.id = id;
            this.data = data;
        }
    }
    
    // A function to calculate the smoothing based only on 3 points.
    private void CalculateSmothedData3Points() {
        for (int i=0; i < trend.size() - 2 ; i++) {
            trend.get(i).glucoseLevelRawSmoothed = 
                    (trend.get(i).glucoseLevelRaw + trend.get(i+1).glucoseLevelRaw + trend.get(i+2).glucoseLevelRaw) / 3;
        }
        // Set the last two points. (doing our best - this will only be used if there are no previous readings).
        if(trend.size() >= 2) {
            // We have two points, use their average for both
            int average = (trend.get(trend.size()-2).glucoseLevelRaw + trend.get(trend.size()-1).glucoseLevelRaw ) / 2;
            trend.get(trend.size()-2).glucoseLevelRawSmoothed = average;
            trend.get(trend.size()-1).glucoseLevelRawSmoothed = average;
        } else if(trend.size() == 1){
            // Only one point, use it
            trend.get(trend.size()-1).glucoseLevelRawSmoothed = trend.get(trend.size()-1).glucoseLevelRaw;
        }
        
    }
    
    private void CalculateSmothedData5Points() {
        // In all places in the code, there should be exactly 16 points.
        // Since that might change, and I'm doing an average of 5, then in the case of less then 5 points,
        // I'll only copy the data as is (to make sure there are reasonable values when the function returns).
        if(trend.size() < 5) {
            for (int i=0; i < trend.size() - 4 ; i++) {
                trend.get(i).glucoseLevelRawSmoothed = trend.get(i).glucoseLevelRaw;
            }
            return;
        }
        
        for (int i=0; i < trend.size() - 4 ; i++) {
            trend.get(i).glucoseLevelRawSmoothed = 
                    (trend.get(i).glucoseLevelRaw + 
                     trend.get(i+1).glucoseLevelRaw + 
                     trend.get(i+2).glucoseLevelRaw + 
                     trend.get(i+3).glucoseLevelRaw +
                     trend.get(i+4).glucoseLevelRaw ) / 5;
        }
        // We now have to calculate the last 4 points, will do our best...
        trend.get(trend.size()-4).glucoseLevelRawSmoothed = 
                (trend.get(trend.size()-4).glucoseLevelRaw + 
                 trend.get(trend.size()-3).glucoseLevelRaw +
                 trend.get(trend.size()-2).glucoseLevelRaw +
                 trend.get(trend.size()-1).glucoseLevelRaw ) / 4;
        
        trend.get(trend.size()-3).glucoseLevelRawSmoothed = 
               (trend.get(trend.size()-3).glucoseLevelRaw +
                trend.get(trend.size()-2).glucoseLevelRaw +
                trend.get(trend.size()-1).glucoseLevelRaw ) / 3;

        // Use the last two points for both last points
        trend.get(trend.size()-2).glucoseLevelRawSmoothed = 
                (trend.get(trend.size()-2).glucoseLevelRaw +
                trend.get(trend.size()-1).glucoseLevelRaw ) / 2;
        
        trend.get(trend.size()-1).glucoseLevelRawSmoothed = trend.get(trend.size()-2).glucoseLevelRawSmoothed;
    }
    
    public void CalculateSmothedData() {
        CalculateSmothedData5Points();
        // print the values, remove before release
        for (int i=0; i < trend.size() ; i++) {
            Log.e("xxx","" + i + " raw val " +  trend.get(i).glucoseLevelRaw + " smoothed " +  trend.get(i).glucoseLevelRawSmoothed);
        }
    }
}
