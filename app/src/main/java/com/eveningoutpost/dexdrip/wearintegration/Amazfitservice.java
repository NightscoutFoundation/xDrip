package com.eveningoutpost.dexdrip.wearintegration;

import com.eveningoutpost.dexdrip.Models.UserError;
import android.app.Service;
import android.content.Context;
import android.content.Intent;

import android.os.IBinder;
import android.support.annotation.Nullable;

import android.util.Log;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;


import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;
import com.kieronquinn.library.amazfitcommunication.Utils;

import android.os.Handler;




/**
 * Created by edoardotassinari on 07/04/18.
 */

public class Amazfitservice extends Service {
    BestGlucose.DisplayGlucose dg;
    private Transporter transporter;
    private Context context;
    DataBundle dataBundle = new DataBundle();

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
            }


        });
        UserError.Log.e("Amazfitservice", "Connecting Service ");
        transporter.connectTransportService();
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
            UserError.Log.e("Amazfitservice", "Service not connected ");
            UserError.Log.e("Amazfitservice", "trying to connect ");
            transporter.connectTransportService();
        }
        transporter.send("com.eveningoutpost.dexdrip.wearintegration", getDataBundle());
        return START_STICKY;

    }


    public DataBundle getDataBundle() {
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        UserError.Log.e("Amazfitservice", "putting data together ");

        dataBundle.putString("date", String.valueOf(dg.timestamp));
        dataBundle.putString("sgv", String.valueOf(dg.unitized_value));
        dataBundle.putString("delta", String.valueOf(dg.unitized_delta));
        dataBundle.putString("direction", String.valueOf(dg.delta_name));
        return dataBundle;
    }


}
