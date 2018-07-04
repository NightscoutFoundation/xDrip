package com.eveningoutpost.dexdrip.wearintegration;


import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.UserError;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.StepCounter;

import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;

import com.eveningoutpost.dexdrip.xdrip;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.DataTransportResult;
import com.huami.watch.transport.TransportDataItem;


import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;




/**
 * Created by klaus3d3.
 */

public class Amazfitservice extends Service {
    BestGlucose.DisplayGlucose dg;

    Transporter transporter;
    private Context context;
    private DataBundle databundle = new DataBundle();


    private static String action;
    private String space_mins;
    private double low_occurs_at;



    @Override
    public void onCreate() {
        super.onCreate();


        transporter = (TransporterClassic)Transporter.get(getApplicationContext(), "com.eveningoutpost.dexdrip.wearintegration");

        transporter.connectTransportService();
        transporter.addChannelListener(new Transporter.ChannelListener() {
            @Override
            public void onChannelChanged(boolean ready) {
                //Transporter is ready if ready is true, send an action now. This will **NOT** work before the transporter is ready!
                //You can change the action to whatever you want, there's also an option for a data bundle to be added (see below)
                if(ready) UserError.Log.e("Amazfitservice", "channel changed ");
            }

        });

        Transporter.DataSendResultCallback test = new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {

                UserError.Log.e("Amazfitservice", "Result back ");
            }
        };
        transporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem item) {

                //Confirmation that watch received SGV Data
                if (item.getAction().equals("SGVDataConfirmation")) {
                    DataBundle db = item.getData();
                    UserError.Log.e("Amazfitservice", db.getString("reply_message"));
                }
                if (item.getAction().equals("CancelConfirmation")) {
                    DataBundle db = item.getData();
                    UserError.Log.e("Amazfitservice", db.getString("reply_message"));
                }

                // In case of getting a remote Snooze from watch check for an active alert and confirm snooze in case of
                if (item.getAction().equals("Amazfit_Remote_Snooze")) {
                    DataBundle db = new DataBundle();
                    UserError.Log.e("Amazfitservice", "Remote SNOOZE recieved");
                    if (ActiveBgAlert.currentlyAlerting()) {
                        UserError.Log.e("Amazfitservice", "snoozing all alarms");
                        AlertPlayer.defaultSnooze();
                        db.putString("reply_message","Snooze accepted by Phone"); }
                    else {
                        UserError.Log.e("Amazfitservice", "No Alarms found to snooze");
                        db.putString("reply_message","No alert found");
                        }
                    transporter.send("SnoozeRemoteConfirmation", db);
                }



            }


        });

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {


        transporter.disconnectTransportService();
        UserError.Log.e("Amazfitservice", "killing service ");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (!transporter.isTransportServiceConnected()){
            UserError.Log.e("Amazfitservice", "Service not connected - trying to reconnect ");
            transporter.connectTransportService();
            }


            if (!transporter.isTransportServiceConnected()) {
                UserError.Log.e("Amazfitservice", "Service is not connectable ");

            }else{



                transporter.send(getAction(), getDataBundle());
            UserError.Log.e("Amazfitservice", "trying to send Data to watch ");

            }
        return START_STICKY;


    }

    public static String getAction() { return action;}
    public static void setAction(String actionremote) {
        action=actionremote;
    }

    public DataBundle getDataBundle() {
        DataBundle datatosend = new DataBundle();
        if (action.equals("xDrip_synced_SGV_data")) {datatosend =  getSGVdata();}
        if (action.equals("xDrip_Alarm")) {datatosend =  getAlarmdata();}
        if (action.equals("xDrip_AlarmCancel")) {datatosend =  getAlarmCancelData();}
       return datatosend;
    }



    public DataBundle getSGVdata() {
        DataBundle db = new DataBundle();
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        db.putLong("date", dg.timestamp);
        db.putString("sgv", String.valueOf(dg.unitized)+" "+ dg.delta_arrow);
        db.putString("delta", String.valueOf(dg.spannableString(dg.unitized_delta)));
        db.putBoolean("ishigh", dg.isHigh());
        db.putBoolean("islow", dg.isLow());
        db.putBoolean("isstale", dg.isStale());
        db.putBoolean("fromplugin", dg.from_plugin);
        if (BgGraphBuilder.low_occurs_at > 0) {
            db.putString("low_predicted", xdrip.getAppContext().getString(R.string.low_predicted));
            db.putString("in", xdrip.getAppContext().getString(R.string.in));
            db.putString("space_mins", xdrip.getAppContext().getString(R.string.space_mins));
            db.putDouble("low_occurs_at",BgGraphBuilder.low_occurs_at);
        }
        db.putString("plugin_name", dg.plugin_name);
        db.putString("reply_message", "Watch acknowledged DATA");

        return db;
    }
    public DataBundle getAlarmdata() {
        DataBundle db = new DataBundle();
        db.putString("uuid", ActiveBgAlert.getOnly().alert_uuid.toString());
        db.putString("reply_message", "Watch acknowledged ALARM");
        db.putString("alarmtext", ActiveBgAlert.getOnly().toString());
        return db;
    }
    public DataBundle getAlarmCancelData() {
        DataBundle db = new DataBundle();
        db.putString("reply_message", "Watch acknowledged CANCEL");
        return db;
    }




}
