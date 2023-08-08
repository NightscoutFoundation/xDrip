package com.eveningoutpost.dexdrip.models;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

class HistoricBg {

    public int quality;
    public int time;
    public double bg;
}

class OOPResults {
    double currentBg;
    int currentTime;
    int currentTrend;
    HistoricBg [] historicBg;
    long timestamp;
    String serialNumber;

    String toGson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}

public class OOPResultsContainer {
    OOPResultsContainer() {
        oOPResultsArray = new OOPResults[0];
        version = 1;
    }

    String toGson() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }

    int version;
    String Message; 

    OOPResults[] oOPResultsArray;
}