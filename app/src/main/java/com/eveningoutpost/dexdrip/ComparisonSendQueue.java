package com.eveningoutpost.dexdrip;

import android.provider.BaseColumns;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created by stephenblack on 11/7/14.
 */
@Table(name = "ComparisonSendQueue", id = BaseColumns._ID)
public class ComparisonSendQueue extends Model {

    @Column(name = "comparison", index = true)
    public Comparison comparison;

    @Column(name = "success", index = true)
    public boolean success;


    public static ComparisonSendQueue nextComparisonJob() {
        ComparisonSendQueue job = new Select()
                .from(ComparisonSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
        return job;
    }

    public static List<ComparisonSendQueue> queue() {
        return new Select()
                .from(ComparisonSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .execute();
    }

    public static void addToQueue(Comparison calibration) {
        ComparisonSendQueue comparisonSendQueue = new ComparisonSendQueue();
        comparisonSendQueue.comparison = calibration;
        comparisonSendQueue.success = false;
        comparisonSendQueue.save();
    }
}