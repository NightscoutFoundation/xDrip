package com.eveningoutpost.dexdrip.wearintegration;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;


/**
 * Created by klaus3d3.
 */

public class Amazfitservice extends Service {
    BestGlucose.DisplayGlucose dg;
    private Transporter transporter;
    //private Context context;
    DataBundle dataBundle = new DataBundle();
    private HeartRate heartrate;
    private StepCounter stepcounter;

    @Override
    public void onCreate() {
        super.onCreate();

        transporter = (TransporterClassic) Transporter.get(getApplicationContext(), "com.eveningoutpost.dexdrip.wearintegration");
        transporter.connectTransportService();
        transporter.addChannelListener(new Transporter.ChannelListener() {
            @Override
            public void onChannelChanged(boolean ready) {
                //Transporter is ready if ready is true, send an action now. This will **NOT** work before the transporter is ready!
                //You can change the action to whatever you want, there's also an option for a data bundle to be added (see below)
                if (ready) UserError.Log.e("Amazfitservice", "channel changed ");
            }
        });

        transporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem item) {
                if (item.getAction().equals("Amazfit_Healthdata")) {
                    DataBundle databundle = item.getData();
                    final StepCounter pm = StepCounter.createEfficientRecord(JoH.tsl(), databundle.getInt("steps"));
                    HeartRate.create(JoH.tsl(), databundle.getInt("heart_rate"), databundle.getInt("heart_acuracy"));

                } else UserError.Log.e("Amazfitservice", item.getAction());
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

        if (transporter != null) {
            transporter.disconnectTransportService();
        }
        UserError.Log.e("Amazfitservice", "killing service ");

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        if (!transporter.isTransportServiceConnected()) {
            UserError.Log.e("Amazfitservice", "Service not connected - trying to reconnect ");
            transporter.connectTransportService();
            try {
                wait(1000);
            } catch (Exception e) {
            }
        }


        if (!transporter.isTransportServiceConnected()) {
            UserError.Log.e("Amazfitservice", "Service is not connectable ");
            ;
        } else {
            UserError.Log.e("Amazfitservice", "Service is connected ");

            transporter.send("Xdrip_synced_SGV_data", getDataBundle());
            UserError.Log.e("Amazfitservice", "Send Data to watch ");
        }
        return START_STICKY;

    }


    public DataBundle getDataBundle() {
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        //UserError.Log.e("Amazfitservice", "putting data together ");

        dataBundle.putLong("date", dg.timestamp);
        dataBundle.putString("sgv", String.valueOf(dg.unitized) + " " + dg.delta_arrow);
        dataBundle.putString("delta", String.valueOf(dg.spannableString(dg.unitized_delta)));
        dataBundle.putBoolean("ishigh", dg.isHigh());
        dataBundle.putBoolean("islow", dg.isLow());
        dataBundle.putBoolean("isstale", dg.isStale());
        dataBundle.putBoolean("fromplugin", dg.from_plugin);
        dataBundle.putString("extra_string", dg.extra_string);
        dataBundle.putString("plugin_name", dg.plugin_name);
        dataBundle.putInt("warning", dg.warning);


        return dataBundle;
    }


}
