package com.eveningoutpost.dexdrip.Models;

// class from LibreAlarm

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import java.util.ArrayList;
import java.util.List;

public class ReadingData {

    public PredictionData prediction;
    public List<GlucoseData> trend;
    public List<GlucoseData> history;
    public byte[] raw_data;

    public ReadingData(PredictionData.Result result) {
        this.prediction = new PredictionData();
        this.prediction.realDate = System.currentTimeMillis();
        this.prediction.errorCode = result;
        this.trend = new ArrayList<>();
        this.history = new ArrayList<>();
        // The two bytes are needed here since some components don't like a null pointer.
        this.raw_data = new byte[2];
    }

    public ReadingData(PredictionData prediction, List<GlucoseData> trend, List<GlucoseData> history) {
        this.prediction = prediction;
        this.trend = trend;
        this.history = history;
    }

    public ReadingData() {}

    public static class TransferObject {
        public long id;
        public ReadingData data;

        public TransferObject() {}

        public TransferObject(long id, ReadingData data) {
            this.id = id;
            this.data = data;
        }
    }
    
    public void CalculateSmothedData() {
        int i;
        for (i=0; i < trend.size() - 2 ; i++) {
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
        // print the values, remove before release
        for (i=0; i < trend.size() ; i++) {
            Log.e("xxx","" + i + " raw val " +  trend.get(i).glucoseLevelRaw + " smoothed " +  trend.get(i).glucoseLevelRawSmoothed);
        }
    }
}
