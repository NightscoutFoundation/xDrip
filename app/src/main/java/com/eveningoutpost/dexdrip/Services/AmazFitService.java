package com.eveningoutpost.dexdrip.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;
import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class AmazFitService extends Service {
    private Transporter transporter;
    private Context context;

    public AmazFitService() {

        DataBundle dataBundle = null;
        //Create the transporter **WARNING** The second parameter MUST be the same on both your watch and phone companion apps!
        //Please change the module name to something unique, but keep it the same for both apps!
        transporter = (TransporterClassic) Transporter.get(this, "XdripSGVData");
        //Add a channel listener to listen for ready event
        transporter.addChannelListener(new Transporter.ChannelListener() {
            @Override
            public void onChannelChanged(boolean ready) {
                //Transporter is ready if ready is true, send an action now. This will **NOT** work before the transporter is ready!
                //You can change the action to whatever you want, there's also an option for a data bundle to be added (see below)

                if (ready) {
                    final BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
                    Log.e("AmazFitService", "Sending Data");
                    dataBundle.putString("sgv", dg.unitized);
                    dataBundle.putString("direction", dg.delta_name);
                    dataBundle.putLong("datetime", dg.timestamp);
                    dataBundle.putDouble("delta", dg.delta_mgdl);
                    transporter.send("XdripSGVData", dataBundle);
                    Log.e("AmazFitService", "Data sent");
                }
            }
        });
        transporter.connectTransportService();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

}
