package com.eveningoutpost.dexdrip.wearintegration;

import com.eveningoutpost.dexdrip.AlertList;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Home;

import android.graphics.Color;
import android.os.IBinder;
import android.support.annotation.Nullable;

import android.util.Log;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Notifications;

import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;


import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;
import com.kieronquinn.library.amazfitcommunication.Utils;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;


/**
 * Created by klaus3d3.
 */

public class Amazfitservice extends Service {
    BestGlucose.DisplayGlucose dg;

    Transporter transporter;
    private Context context;
    DataBundle dataBundle = new DataBundle();
    private HeartRate heartrate;
    private StepCounter stepcounter;



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

        transporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem item) {

                //Confirmation that watch received SGV Data
                if (item.getAction().equals("SGVDataConfirmation")) {
                    UserError.Log.e("Amazfitservice", "Data transmission confirmed by watch");
                }

                // In case of getting a remote Snooze from watch check for an active alert and confirm snooze in case of
                if (item.getAction().equals("Amazfit_Remote_Snooze")) {

                    if (ActiveBgAlert.getOnly()!=null) {
                        UserError.Log.e("Amazfitservice", "Remote Snooze Received - snoozing all alarms");
                        AlertPlayer.defaultSnooze();
                        DataBundle db = new DataBundle();
                        db.putString("U","test");
                        if (!transporter.isTransportServiceConnected()) UserError.Log.e("Amazfitservice", "Transporter is not available");
                        transporter.send("SnoozeRemoteConfirmation", db);
                    }
                    else {UserError.Log.e("Amazfitservice", "No Alarms found to snooze");
                       }
                }
                //else UserError.Log.e("Amazfitservice", item.getAction());


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


            transporter.send("Xdrip_synced_SGV_data", getDataBundle());
            UserError.Log.e("Amazfitservice", "trying to send Data to watch " );
            }
        return START_STICKY;


    }


    public DataBundle getDataBundle() {
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        String prediction_text = "";

       //UserError.Log.e("Amazfitservice", "putting data together ");

        dataBundle.putLong("date", dg.timestamp);
        dataBundle.putString("sgv", String.valueOf(dg.unitized)+" "+ dg.delta_arrow);
        dataBundle.putString("delta", String.valueOf(dg.spannableString(dg.unitized_delta)));
        dataBundle.putBoolean("ishigh", dg.isHigh());
        dataBundle.putBoolean("islow", dg.isLow());
        dataBundle.putBoolean("isstale", dg.isStale());
        dataBundle.putBoolean("fromplugin", dg.from_plugin);

        prediction_text="";
        if (BgGraphBuilder.low_occurs_at > 0) {
            final double now = JoH.ts();
            final double predicted_low_in_mins = (BgGraphBuilder.low_occurs_at - now) / 60000;
            if (predicted_low_in_mins > 1) {
                prediction_text=(getString(R.string.low_predicted) + " " + getString(R.string.in) + ": " + (int) predicted_low_in_mins + getString(R.string.space_mins));
            }
        }
        dataBundle.putString("extra_string", prediction_text);
        dataBundle.putString("plugin_name", dg.plugin_name);
        dataBundle.putInt("warning", dg.warning);

        Notifications.

        UserError.Log.e("AmazfitService", "Alarming =" + String.valueOf(ActiveBgAlert.currentlyAlerting()));
        dataBundle.putBoolean("activealarm",ActiveBgAlert.currentlyAlerting());
        return dataBundle;
    }



}
