package com.eveningoutpost.dexdrip.wearintegration;



import com.eveningoutpost.dexdrip.BGHistory;
import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;
import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.UserError;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.util.Set;

import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.StepCounter;

import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
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
    private static String action;
    private static String alert_to_send;
    private static int default_snooze;
    private ActiveBluetoothDevice activeBluetoothDevice;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private String space_mins;
    private double low_occurs_at;
    private Transporter transporter;
    //private Context context;
    DataBundle dataBundle = new DataBundle();
    private HeartRate heartrate;
    private StepCounter stepcounter;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());


        transporter = (TransporterClassic)Transporter.get(getApplicationContext(), "com.eveningoutpost.dexdrip.wearintegration");


        transporter = (TransporterClassic) Transporter.get(getApplicationContext(), "com.eveningoutpost.dexdrip.wearintegration");

        transporter.connectTransportService();
        transporter.addChannelListener(new Transporter.ChannelListener() {
            @Override
            public void onChannelChanged(boolean ready) {
                //Transporter is ready if ready is true, send an action now. This will **NOT** work before the transporter is ready!
                //You can change the action to whatever you want, there's also an option for a data bundle to be added (see below)
                if (ready) UserError.Log.e("Amazfitservice", "channel changed - trying automatic resend ");
                Amazfitservice.start("xDrip_synced_SGV_data");
            }

        });


        transporter.addDataListener(new Transporter.DataListener() {
            @Override
            public void onDataReceived(TransportDataItem item) {


                //Confirmation that watch received SGV Data
                if (item.getAction().equals("SGVDataConfirmation")) {
                    DataBundle db = item.getData();
                    //UserError.Log.e("Amazfitservice", db.getString("reply_message"));
                }
                if (item.getAction().equals("CancelConfirmation")) {
                    DataBundle db = item.getData();
                    //UserError.Log.e("Amazfitservice", db.getString("reply_message"));
                }

                // In case of getting a remote Snooze from watch check for an active alert and confirm snooze in case of
                if (item.getAction().equals("Amazfit_Remote_Snooze")) {
                    DataBundle db = item.getData();


                        UserError.Log.e("Amazfitservice", "Remote SNOOZE recieved for " + db.getInt("snoozetime") + " mins");

                        if (ActiveBgAlert.currentlyAlerting() && db.getInt("snoozetime")>0) {
                            UserError.Log.e("Amazfitservice", "snoozing all alarms");
                            AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(),db.getInt("snoozetime"),true);
                            db.putString("reply_message", "Snooze accepted by Phone");
                        } else if (ActiveBgAlert.currentlyAlerting()) {
                            AlertPlayer.defaultSnooze();
                            db.putString("reply_message", "Snooze accepted by Phone");}
                            else{
                            UserError.Log.e("Amazfitservice", "No Alarms found to snooze");
                            db.putString("reply_message", "No alert found");
                        }

                    transporter.send("SnoozeRemoteConfirmation", db);
                }




                if (item.getAction().equals("Amazfit_Healthdata")) {
                    DataBundle databundle = item.getData();
                    final StepCounter pm = StepCounter.createEfficientRecord(JoH.tsl(), databundle.getInt("steps"));
                    HeartRate.create(JoH.tsl(), databundle.getInt("heart_rate"), databundle.getInt("heart_acuracy"));

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

        if (transporter != null) {
            transporter.disconnectTransportService();
        }
        UserError.Log.e("Amazfitservice", "killing service ");

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Transporter.DataSendResultCallback test = new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {

                UserError.Log.e("Amazfitservice", dataTransportResult.toString() );
            }
        };
        if (!transporter.isTransportServiceConnected()) {
            UserError.Log.e("Amazfitservice", "Service not connected - trying to reconnect ");
            transporter.connectTransportService();

        }


        if (!transporter.isTransportServiceConnected()) {
            UserError.Log.e("Amazfitservice", "Service is not connectable ");

        } else {

            transporter.send(getAction(), getDataBundle(),test  );
            //UserError.Log.e("Amazfitservice", "trying to send Data to watch " + action);
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
        if (action.equals("xDrip_Otheralert")) {datatosend =  getOtheralertdata();}
        if (action.equals("xDrip_AlarmCancel")) {datatosend =  getAlarmCancelData();}
        return datatosend;
    }


    private String gettransmitterbattery(){
        TransmitterData td = TransmitterData.last();
        String returntext;
        if (td== null || td.sensor_battery_level == 0){
            returntext="not available";

        } else if((System.currentTimeMillis() - td.timestamp) > 1000*60*60*24){
            returntext="no data in 24 hours";

        } else {
            returntext="" + td.sensor_battery_level;

            if (td.sensor_battery_level <= Dex_Constants.TRANSMITTER_BATTERY_EMPTY) {
                returntext = returntext +" - very low";
            } else if (td.sensor_battery_level <= Dex_Constants.TRANSMITTER_BATTERY_LOW) {
                returntext = returntext +" - low";
                returntext = returntext +"\n(experimental interpretation)";
            } else {
                returntext = returntext +" - ok";
            }
        }
        return returntext;
    }
    public String getCurrentDevice() {
        activeBluetoothDevice = ActiveBluetoothDevice.first();
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        String currentdevice;
        if (activeBluetoothDevice != null) {
            currentdevice=activeBluetoothDevice.name;
        } else {
            currentdevice="None Set";
        }

        String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
        if (collection_method.compareTo("DexcomG5") == 0) {
            Transmitter defaultTransmitter = new Transmitter(prefs.getString("dex_txid", "ABCDEF"));
            if (Build.VERSION.SDK_INT >= 18) {
                mBluetoothAdapter = mBluetoothManager.getAdapter();
            }
            if (mBluetoothAdapter != null) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
                    for (BluetoothDevice device : pairedDevices) {
                        if (device.getName() != null) {

                            String transmitterIdLastTwo = Extensions.lastTwoCharactersOfString(defaultTransmitter.transmitterId);
                            String deviceNameLastTwo = Extensions.lastTwoCharactersOfString(device.getName());

                            if (transmitterIdLastTwo.equals(deviceNameLastTwo)) {
                                currentdevice=defaultTransmitter.transmitterId;
                            }

                        }
                    }
                }
            } else {
                currentdevice="No Bluetooth";
            }
        }
        return currentdevice;
    }
    private String getCollectionMethod() {
        return prefs.getString("dex_collection_method", "BluetoothWixel").replace("Dexbridge", "xBridge");
    }

    public DataBundle getSGVdata() {
        final int sensor_age = Pref.getInt("nfc_sensor_age", 0);
        final String age_problem = (Pref.getBooleanDefaultFalse("nfc_age_problem") ? " \u26A0\u26A0\u26A0" : "");
        final double expires = JoH.tolerantParseDouble(prefs.getString("nfc_expiry_days", "14.5")) - ((double) sensor_age) / 1440;

        DataBundle db = new DataBundle();
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        db.putString("Collection_info",getCollectionMethod());
        db.putString("hardware_source_info",getCurrentDevice());
        db.putString("sensor.latest_battery_level",gettransmitterbattery());
        db.putString("sensor_expires",((expires >= 0) ? (JoH.qs(expires, 1) + "d") : "EXPIRED! ") + age_problem);

        db.putLong("date", dg.timestamp);
        db.putString("sgv", String.valueOf(dg.unitized)+String.valueOf(dg.delta_arrow));
        db.putString("delta", String.valueOf(dg.spannableString(dg.unitized_delta)));
        db.putBoolean("ishigh", dg.isHigh());
        db.putBoolean("islow", dg.isLow());
        db.putBoolean("isstale", dg.isStale());
        if (BgGraphBuilder.low_occurs_at > 0) {
            db.putString("low_predicted", xdrip.getAppContext().getString(R.string.low_predicted));
            db.putString("in", xdrip.getAppContext().getString(R.string.in));
            db.putString("space_mins", xdrip.getAppContext().getString(R.string.space_mins));
            db.putDouble("low_occurs_at",BgGraphBuilder.low_occurs_at);
        }
        db.putString("plugin_name", dg.plugin_name);
        db.putString("reply_message", "Watch acknowledged DATA");
        db.putString("phone_battery", String.valueOf(PowerStateReceiver.getBatteryLevel(getApplicationContext())));

        if(Pref.getBoolean("pref_amazfit_widget_graph",false))
            db.putString("SGVGraph",BitmaptoString(createWearBitmap(Pref.getStringToInt("amazfit_widget_graph_hours",4))));
        else
            db.putString("SGVGraph","false");

        if(Pref.getBoolean("pref_amazfit_watchface_graph",false))
            db.putString("WFGraph",BitmaptoString(createWFBitmap(Pref.getStringToInt("amazfit_watchface_graph_hours",4))));
        else
            db.putString("WFGraph","false");
        return db;
    }
    public DataBundle getAlarmdata() {
        DataBundle db = new DataBundle();
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        db.putString("uuid", alert_to_send);
        db.putString("reply_message", "Watch acknowledged ALARM");
        db.putString("alarmtext", alert_to_send);
        db.putLong("date", System.currentTimeMillis());
        db.putString("sgv", String.valueOf(dg.unitized)+String.valueOf(dg.delta_arrow));
        db.putInt("default_snooze",default_snooze);

        return db;
    }
    public DataBundle getOtheralertdata() {
        DataBundle db = new DataBundle();
        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        db.putString("uuid", alert_to_send);
        db.putString("reply_message", "Watch acknowledged OTHERALERT");
        db.putString("alarmtext", alert_to_send);
        db.putLong("date", System.currentTimeMillis());
        db.putString("sgv", String.valueOf(dg.unitized)+String.valueOf(dg.delta_arrow));


        return db;
    }

    public DataBundle getAlarmCancelData() {
        DataBundle db = new DataBundle();
        db.putString("reply_message", "Watch acknowledged CANCEL");
        db.putLong("date", System.currentTimeMillis());
        return db;
    }

public static void start(String action_text,BgReading bg){
        action=action_text;

    //JoH.startService(Amazfitservice.class);

}


    public static void start(String action_text,String alert_name, int snooze_time){
        action=action_text;
        alert_to_send=alert_name;
        default_snooze=snooze_time;


        JoH.startService(Amazfitservice.class);
    }


    public static void start(String action_text){
        action=action_text;

        JoH.startService(Amazfitservice.class);

           }
    private Bitmap createWearBitmapTinyDots(long start, long end) {
        return new BgSparklineBuilder(xdrip.getAppContext())
                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext()))
                .setStart(start)
                .setEnd(end)
                .showAxes()
                .setWidthPx(290)
                .setHeightPx(160)
                .setTinyDots()
                .build();
    }
    private Bitmap createWearBitmapSmallDots(long start, long end) {
        return new BgSparklineBuilder(xdrip.getAppContext())
                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext()))
                .setStart(start)
                .setEnd(end)
                .showAxes()
                .setWidthPx(290)
                .setHeightPx(160)
                .setSmallDots()
                .build();
    }
    private Bitmap createWFBitmapTinyDots(long start, long end) {
        return new BgSparklineBuilder(xdrip.getAppContext())
                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext()))
                .setStart(start)
                .setEnd(end)
                //.showAxes()
                .setWidthPx(300)
                .setHeightPx(130)
                .setTinyDots()
                .build();
    }
    private Bitmap createWFBitmapSmallDots(long start, long end) {
        return new BgSparklineBuilder(xdrip.getAppContext())
                .setBgGraphBuilder(new BgGraphBuilder(xdrip.getAppContext()))
                .setStart(start)
                .setEnd(end)
                //.showAxes()
                .setWidthPx(300)
                .setHeightPx(130)
                .setSmallDots()
                .build();
    }

    private Bitmap createWearBitmap(long hours) {
        if(Pref.getBoolean("pref_amazfit_widget_graph_dots",false))
        return createWearBitmapTinyDots(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
        else
            return createWearBitmapSmallDots(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
    }
    private Bitmap createWFBitmap(long hours) {
        if(Pref.getBoolean("pref_amazfit_watchface_graph_dots",false))
        return createWFBitmapTinyDots(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
        else
            return createWFBitmapSmallDots(System.currentTimeMillis() - 60000 * 60 * hours, System.currentTimeMillis());
    }

    private String BitmaptoString(Bitmap bitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
    }


}
