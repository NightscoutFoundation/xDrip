package com.eveningoutpost.dexdrip.UtilityModels;

import android.provider.BaseColumns;
import android.util.Log;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Models.BgReading;

import java.util.List;

/**
 * Created by stephenblack on 11/7/14.
 */
@Table(name = "BgSendQueue", id = BaseColumns._ID)
public class BgSendQueue extends Model {

    @Column(name = "bgReading", index = true)
    public BgReading bgReading;

    @Column(name = "success", index = true)
    public boolean success;

    @Column(name = "operation_type")
    public String operation_type;

    public static BgSendQueue nextBgJob() {
        return new Select()
                .from(BgSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .limit(1)
                .executeSingle();
    }

    public static List<BgSendQueue> queue() {
        return new Select()
                .from(BgSendQueue.class)
                .where("success !=", true)
                .orderBy("_ID desc")
                .execute();
    }

    public static void addToQueue(BgReading bgReading, String operation_type) {
        BgSendQueue bgSendQueue = new BgSendQueue();
        bgSendQueue.operation_type = operation_type;
        bgSendQueue.bgReading = bgReading;
        bgSendQueue.success = false;
        bgSendQueue.save();
        Log.w("SENSOR QUEUE:", bgSendQueue.toString());
    }
}