package com.eveningoutpost.dexdrip.UtilityModels;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 */
public class PebbleSync extends Service {
    private final static String TAG = PebbleSync.class.getSimpleName();
    //    CGM_ICON_KEY = 0x0,		// TUPLE_CSTRING, MAX 2 BYTES (10)
    //    CGM_BG_KEY = 0x1,		// TUPLE_CSTRING, MAX 4 BYTES (253 OR 22.2)
    //    CGM_TCGM_KEY = 0x2,		// TUPLE_INT, 4 BYTES (CGM TIME)
    //    CGM_TAPP_KEY = 0x3,		// TUPLE_INT, 4 BYTES (APP / PHONE TIME)
    //    CGM_DLTA_KEY = 0x4,		// TUPLE_CSTRING, MAX 5 BYTES (BG DELTA, -100 or -10.0)
    //    CGM_UBAT_KEY = 0x5,		// TUPLE_CSTRING, MAX 3 BYTES (UPLOADER BATTERY, 100)
    //    CGM_NAME_KEY = 0x6		// TUPLE_CSTRING, MAX 9 BYTES (xDrip)
    public static final UUID PEBBLEAPP_UUID = UUID.fromString("2c3f5ab3-7506-44e7-b8d0-2c63de32e1ec");
    public static final int ICON_KEY = 0;
    public static final int BG_KEY = 1;
    public static final int RECORD_TIME_KEY = 2;
    public static final int PHONE_TIME_KEY = 3;
    public static final int BG_DELTA_KEY = 4;
    public static final int UPLOADER_BATTERY_KEY = 5;
    public static final int NAME_KEY = 6;

    private Context mContext;
    private BgGraphBuilder bgGraphBuilder;
    private BgReading mBgReading;
    private static int lastTransactionId;
    BroadcastReceiver newSavedBgReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        bgGraphBuilder = new BgGraphBuilder(mContext);
        mBgReading = BgReading.last();
        init();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("broadcast_to_pebble", false)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        Log.w(TAG, "STARTING SERVICE");
        sendData();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy called");
        super.onDestroy();
        if(newSavedBgReceiver != null) {
            unregisterReceiver(newSavedBgReceiver);
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void init() {
        Log.i(TAG, "Initialising...");
        Log.i(TAG, "configuring PebbleDataReceiver");

        PebbleKit.registerReceivedDataHandler(mContext, new PebbleKit.PebbleDataReceiver(PEBBLEAPP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                Log.d(TAG, "receiveData: transactionId is " + String.valueOf(transactionId));
                if (lastTransactionId == 0 || transactionId != lastTransactionId) {
                    lastTransactionId = transactionId;
                    Log.d(TAG, "Received Query. data: " + data.size() + ". sending ACK and data");
                    PebbleKit.sendAckToPebble(context, transactionId);
                    sendData();
                } else {
                    Log.d(TAG, "receiveData: lastTransactionId is "+ String.valueOf(lastTransactionId)+ ", sending NACK");
                    PebbleKit.sendNackToPebble(context,transactionId);
                }
            }
        });
    }

    public PebbleDictionary buildDictionary() {
        PebbleDictionary dictionary = new PebbleDictionary();
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());
        Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal() + " bgReading-" + bgReading() + " now-"+ (int) now.getTime()/1000 + " bgTime-" + (int) (mBgReading.timestamp / 1000) + " phoneTime-" + (int) (new Date().getTime() / 1000) + " bgDelta-" + bgDelta());
        dictionary.addString(ICON_KEY, slopeOrdinal());
        dictionary.addString(BG_KEY, bgReading());
        dictionary.addUint32(RECORD_TIME_KEY, (int) (((mBgReading.timestamp + offsetFromUTC) / 1000)));
        dictionary.addUint32(PHONE_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC) / 1000));
        dictionary.addString(BG_DELTA_KEY, bgDelta());
        if(PreferenceManager.getDefaultSharedPreferences(mContext).getString("dex_collection_method", "DexbridgeWixel").compareTo("DexbridgeWixel")==0) {
            dictionary.addString(UPLOADER_BATTERY_KEY, bridgeBatteryString());
            dictionary.addString(NAME_KEY, "Bridge");
        } else {
            dictionary.addString(UPLOADER_BATTERY_KEY, phoneBattery());
            dictionary.addString(NAME_KEY, "Phone");
        }
        return dictionary;
    }

    public String bridgeBatteryString() {
        return String.format("%d", PreferenceManager.getDefaultSharedPreferences(mContext).getInt("bridge_battery", 0));
    }

    public void sendData(){
        mBgReading = BgReading.last();
        if(mBgReading != null) {
            sendDownload(buildDictionary());
        }
    }

    public String bgReading() {
        return bgGraphBuilder.unitized_string(mBgReading.calculated_value);
    }

    public String bgDelta() {
        String deltaString;
        if((PreferenceManager.getDefaultSharedPreferences(mContext).getString("units","mgdl").compareTo("mgdl") == 0)) {
            deltaString = String.format("%.0f", mBgReading.calculated_value_slope * 360000);
        } else {
            deltaString = String.format("%.1f", (mBgReading.calculated_value_slope * 360000)*Constants.MGDL_TO_MMOLL);
        }
        Log.v(TAG,"bgDelta: "+ deltaString);
        if(Float.valueOf(deltaString) > 0) {
            return ("+"+deltaString);
        } else {
            return deltaString;
        }
    }

    public String phoneBattery() {
        return String.valueOf(getBatteryLevel());
    }

    public String bgUnit() {
        return bgGraphBuilder.unit();
    }

    public void sendDownload(PebbleDictionary dictionary) {
        if (PebbleKit.isWatchConnected(mContext)) {
            if (dictionary != null && mContext != null) {
                Log.d(TAG, "sendDownload: Sending data to pebble");
                PebbleKit.sendDataToPebble(mContext, PEBBLEAPP_UUID, dictionary);
            }
        }
    }

    public int getBatteryLevel() {
        Intent batteryIntent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) { return 50; }
        return (int)(((float)level / (float)scale) * 100.0f);
    }

    public String slopeOrdinal(){
        String arrow_name = mBgReading.slopeName();
        if(arrow_name.compareTo("DoubleDown")==0) return "7";
        if(arrow_name.compareTo("SingleDown")==0) return "6";
        if(arrow_name.compareTo("FortyFiveDown")==0) return "5";
        if(arrow_name.compareTo("Flat")==0) return "4";
        if(arrow_name.compareTo("FortyFiveUp")==0) return "3";
        if(arrow_name.compareTo("SingleUp")==0) return "2";
        if(arrow_name.compareTo("DoubleUp")==0) return "1";
        if(arrow_name.compareTo("9")==0) return arrow_name;
        return "0";
    }
}

