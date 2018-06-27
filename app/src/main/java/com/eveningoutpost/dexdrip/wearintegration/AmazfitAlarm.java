package com.eveningoutpost.dexdrip.wearintegration;

import com.eveningoutpost.dexdrip.Models.UserError;
import com.huami.watch.transport.DataBundle;
import com.huami.watch.transport.TransportDataItem;


import com.kieronquinn.library.amazfitcommunication.Transporter;
import com.kieronquinn.library.amazfitcommunication.TransporterClassic;
import com.kieronquinn.library.amazfitcommunication.Utils;
import com.eveningoutpost.dexdrip.wearintegration.Amazfitservice;


public class AmazfitAlarm  {

    private Amazfitservice amazfitservice = new Amazfitservice();
    private Transporter transporter = amazfitservice.transporter;


    public void AmazfitAlarm(){


        if (!transporter.isTransportServiceConnected()) {
            UserError.Log.e("AmazfitAlarm", "Service is not connected ");

        }else{
            UserError.Log.e("AmazfitAlarm", "Alarming.... ");
            transporter.send("Alarm");}

    }
}
