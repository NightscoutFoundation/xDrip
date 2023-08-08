package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.BroadcastSnooze;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
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
 * Refactored by Andy, to be able to use both Pebble displays
 */
public class PebbleWatchSync extends ForegroundService {

    // watch faces
    public static final UUID PEBBLEAPP_UUID = UUID.fromString("79f8ecb3-7214-4bfc-b996-cb95148ee6d3");

    // apps
    public static final UUID PEBBLE_CONTROL_APP_UUID = UUID.fromString("aa14a012-96c8-4ce6-9466-4bfdf0d5a74e");


    private final static String TAG = PebbleWatchSync.class.getSimpleName();
    private final static long sanity_timestamp = 1478197375;
    private final static boolean d = false;

    // these must match in watchface
    private final static int HEARTRATE_LOG = 101;
    private final static int MOVEMENT_LOG = 103;

    public static int lastTransactionId;

    private long last_heartrate_timestamp = 0;
    private long last_movement_timestamp = 0;

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
        int pebbleType = PebbleUtil.getCurrentPebbleSyncType();

        return PebbleUtil.getPebbleDisplayType(pebbleType);
    }

    private void check_and_enable_bluetooth() {
        if (Build.VERSION.SDK_INT > 17) {
            try {
                final BluetoothManager bluetooth_manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                if (!bluetooth_manager.getAdapter().isEnabled()) {
                    if (Pref.getBoolean("automatically_turn_bluetooth_on", true)) {
                        JoH.setBluetoothEnabled(getApplicationContext(), true);
                        //Toast.makeText(this, "Trying to turn Bluetooth on", Toast.LENGTH_LONG).show();
                        //} else {
                        //Toast.makeText(this, "Please turn Bluetooth on!", Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Error checking/enabling bluetooth: " + e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (getCurrentBroadcastToPebbleSetting() == PebbleDisplayType.None) {
            stopSelf();
            return START_NOT_STICKY;
        }

        final PowerManager.WakeLock wl = JoH.getWakeLock("pebble_service_start", 60000);
        try {
            Log.i(TAG, "STARTING SERVICE PebbleWatchSync");
            check_and_enable_bluetooth();
            getActivePebbleDisplay().startDeviceCommand();
        } finally {
            JoH.releaseWakeLock(wl);
        }
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

        PebbleKit.registerDataLogReceiver(context, new PebbleKit.PebbleDataLogReceiver(currentWatchFaceUUID) {
            @Override
            public void receiveData(Context context, UUID logUuid, Long timestamp,
                                    Long tag, int data) {
                if (d)
                    Log.d(TAG, "receiveLogData: uuid:" + logUuid + " " + JoH.dateTimeText(timestamp * 1000) + " tag:" + tag + " data: " + data);
            }

            @Override
            public void receiveData(Context context, UUID logUuid, Long timestamp,
                                    Long tag, Long data) {
                Log.d(TAG, "receiveLogData: uuid:" + logUuid + " started: " + JoH.dateTimeText(timestamp * 1000) + " tag:" + tag + " data: " + data);
                if (Pref.getBoolean("use_pebble_health", true)) {
                    if ((tag != null) && (data != null)) {
                        final int s = ((int) (long) tag) & 0xfffffff7; // alternator

                        switch (s) {
                            case HEARTRATE_LOG:
                                if (data > sanity_timestamp) {
                                    if (last_heartrate_timestamp > 0) {
                                        Log.e(TAG, "Out of sequence heartrate timestamp received!");
                                    }
                                    last_heartrate_timestamp = data;
                                } else {
                                    if (data > 0) {
                                        if (last_heartrate_timestamp > 0) {
                                            final HeartRate hr = new HeartRate();
                                            hr.timestamp = last_heartrate_timestamp * 1000;
                                            hr.bpm = (int) (long) data;
                                            Log.d(TAG, "Saving HeartRate: " + hr.toS());
                                            hr.saveit();
                                            last_heartrate_timestamp = 0; // reset state
                                        } else {
                                            Log.e(TAG, "Out of sequence heartrate value received!");
                                        }
                                    }
                                }
                                break;

                            case MOVEMENT_LOG:
                                if (data > sanity_timestamp) {
                                    if (last_movement_timestamp > 0) {
                                        Log.e(TAG, "Out of sequence movement timestamp received!");
                                    }
                                    last_movement_timestamp = data;
                                } else {
                                    if (data > 0) {
                                        if (last_movement_timestamp > 0) {
                                            final StepCounter pm = StepCounter.createEfficientRecord(last_movement_timestamp * 1000, (int)(long) data);
                                            Log.d(TAG, "Saving Movement: " + pm.toS());
                                            last_movement_timestamp = 0; // reset state
                                        } else {
                                            Log.e(TAG, "Out of sequence movement value received!");
                                        }
                                    }
                                }
                                break;

                            default:
                                Log.e(TAG, "Unknown pebble data log type received: " + s);
                                break;

                        }
                    } else {
                        Log.e(TAG, "Got null Long in receive data");
                    }
                }
            }


            @Override
            public void receiveData(Context context, UUID logUuid, Long timestamp,
                                    Long tag, byte[] data) {
               if (d) Log.d(TAG,"receiveLogData: uuid:"+logUuid+" "+JoH.dateTimeText(timestamp*1000)+" tag:"+tag+" hexdata: "+JoH.bytesToHex(data));
            }

            @Override
            public void onFinishSession(Context context, UUID logUuid, Long timestamp,
                                        Long tag) {
                if (d) Log.i(TAG, "Session " + tag + " finished!");
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
        BroadcastSnooze.send();
        JoH.static_toast_long("Alarm snoozed by pebble");
    }



}

