package com.eveningoutpost.dexdrip.healthconnect;

import static com.eveningoutpost.dexdrip.models.JoH.defaultGsonInstance;

import androidx.health.connect.client.records.HeartRateRecord;
import androidx.health.connect.client.records.StepsRecord;

import com.google.gson.annotations.Expose;

import java.util.List;

// jamorham

public class DataReply {
    @Expose
    public List<StepsRecord> stepsRecords;
    @Expose
    public List<HeartRateRecord> heartRateRecords;

    public static DataReply fromJson(final String json) {
        return defaultGsonInstance().fromJson(json, DataReply.class);
    }

}

