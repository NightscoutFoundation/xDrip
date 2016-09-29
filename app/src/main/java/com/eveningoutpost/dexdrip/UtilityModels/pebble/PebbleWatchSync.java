package com.eveningoutpost.dexdrip.UtilityModels.pebble;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.xdrip;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 */

/**
 * Refactored by Andy, to be abble to use both Pebble displays
 */
public class PebbleWatchSync extends Service {

    // watch faces
    public static final UUID PEBBLEAPP_UUID = UUID.fromString("79f8ecb3-7214-4bfc-b996-cb95148ee6d3");

    // apps
    public static final UUID PEBBLE_CONTROL_APP_UUID = UUID.fromString("aa14a012-96c8-4ce6-9466-4bfdf0d5a74e");


    private final static String TAG = PebbleWatchSync.class.getSimpleName();

    public static int lastTransactionId;


    private static Context context;
    private static BgGraphBuilder bgGraphBuilder;
    private static Map<PebbleDisplayType, PebbleDisplayInterface> pebbleDisplays;

    private UUID currentWatchFaceUUID;


    public static void setPebbleType(int pebbleType) {
        PebbleUtil.pebbleDisplayType = PebbleUtil.getPebbleDisplayType(pebbleType);
    }


    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        bgGraphBuilder = new BgGraphBuilder(context);

        initPebbleDisplays();
        PebbleUtil.pebbleDisplayType = getCurrentBroadcastToPebbleSetting();
        Log.d(TAG,"onCreate for: "+PebbleUtil.pebbleDisplayType.toString());

        currentWatchFaceUUID = getActivePebbleDisplay().watchfaceUUID();

        init();
    }

    private void initPebbleDisplays() {

        if (pebbleDisplays == null) {
            pebbleDisplays = new HashMap<>();
            pebbleDisplays.put(PebbleDisplayType.None, new PebbleDisplayDummy());
            pebbleDisplays.put(PebbleDisplayType.Standard, new PebbleDisplayStandard());
            pebbleDisplays.put(PebbleDisplayType.Trend, new PebbleDisplayTrendOld());
            pebbleDisplays.put(PebbleDisplayType.TrendClassic, new PebbleDisplayTrendOld());
            pebbleDisplays.put(PebbleDisplayType.TrendClay, new PebbleDisplayTrend());
        }

        for (PebbleDisplayInterface pdi : pebbleDisplays.values()) {
            pdi.initDisplay(context, this, bgGraphBuilder);
        }
    }


    private PebbleDisplayInterface getActivePebbleDisplay() {
        return pebbleDisplays.get(PebbleUtil.pebbleDisplayType);
    }


    public static PebbleDisplayType getCurrentBroadcastToPebbleSetting() {
        int pebbleType = PebbleUtil.getCurrentPebbleSyncType(PreferenceManager.getDefaultSharedPreferences(context));

        return PebbleUtil.getPebbleDisplayType(pebbleType);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (getCurrentBroadcastToPebbleSetting() == PebbleDisplayType.None) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Log.i(TAG, "STARTING SERVICE PebbleWatchSync");
        getActivePebbleDisplay().startDeviceCommand();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    protected void init() {
        Log.i(TAG, "Initialising...");
        Log.i(TAG, "configuring PebbleDataReceiver for: "+currentWatchFaceUUID.toString());


        PebbleKit.registerReceivedDataHandler(context, new PebbleKit.PebbleDataReceiver(currentWatchFaceUUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                getActivePebbleDisplay().receiveData(transactionId, data);
            }
        });

        PebbleKit.registerReceivedAckHandler(context, new PebbleKit.PebbleAckReceiver(currentWatchFaceUUID) {
            @Override
            public void receiveAck(Context context, int transactionId) {
                getActivePebbleDisplay().receiveAck(transactionId);
            }
        });

        PebbleKit.registerReceivedNackHandler(context, new PebbleKit.PebbleNackReceiver(currentWatchFaceUUID) {
            @Override
            public void receiveNack(Context context, int transactionId) {
                getActivePebbleDisplay().receiveNack(transactionId);
            }
        });

        // control app
        PebbleKit.registerReceivedDataHandler(context, new PebbleKit.PebbleDataReceiver(PEBBLE_CONTROL_APP_UUID) {
            @Override
            public void receiveData(final Context context, final int transactionId, final PebbleDictionary data) {
                getActivePebbleDisplay().receiveAppData(transactionId, data);
            }
        });



    }

    public static void receiveAppData(int transactionId, PebbleDictionary data) {
        Log.d(TAG, "receiveAppData: transactionId is " + String.valueOf(transactionId));

        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);

        PebbleKit.sendAckToPebble(xdrip.getAppContext(), transactionId);
        JoH.static_toast_long("Alarm snoozed by pebble");
    }



}

