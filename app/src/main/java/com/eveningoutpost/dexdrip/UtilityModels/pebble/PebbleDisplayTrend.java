package com.eveningoutpost.dexdrip.UtilityModels.pebble;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.SimpleImageEncoder;
import com.eveningoutpost.dexdrip.UtilityModels.pebble.watchface.InstallPebbleTrendClayWatchFace;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 * <p/>
 * Changed by Andy (created from PebbleSync from PebbleTrend branch)
 * Later cut and pasted from xDrip-Experimental directly from the Sept 2016 beta
 * Primarily the work of John Stevens (jstevensog)
 */
public class PebbleDisplayTrend extends PebbleDisplayAbstract {

    private final static String TAG = PebbleDisplayTrend.class.getSimpleName();

   /*
    public static final int ICON_KEY = 0;
    public static final int BG_KEY = 1;
    public static final int RECORD_TIME_KEY = 2;
    public static final int PHONE_TIME_KEY = 3;
    public static final int BG_DELTA_KEY = 4;
    public static final int UPLOADER_BATTERY_KEY = 5;
    public static final int NAME_KEY = 6;
    public static final int TREND_BEGIN_KEY = 7;
    public static final int TREND_DATA_KEY = 8;
    public static final int TREND_END_KEY = 9;
    public static final int MESSAGE_KEY = 10;
    public static final int VIBE_KEY = 11;
    public static final int SYNC_KEY = 1000;
    public static final int PLATFORM_KEY = 1001;
    public static final int VERSION_KEY = 1002;
    */

    public static final int CHUNK_SIZE = 100;

    private static final String WATCHAPP_FILENAME = "xDrip-Pebble2";
    private static boolean watchFaceInstalled = false;
    private static boolean faceInactive=false;

    private static boolean messageInTransit = false;
    private static boolean transactionFailed = false;
    private static boolean transactionOk = false;
    private static boolean done = false;
    private static boolean sendingData = false;
    private static int current_size = 0;
    private static int image_size = 0;
    private static byte[] chunk;
    private static ByteBuffer buff = null;
    private static ByteArrayOutputStream stream = null;
    public static int retries = 0;
    private static int lastTransactionId = 0;
    private static int currentTransactionId = 0;
    private static int lastTrendPeriod = -1;
    private static long pebble_platform = -1;
    private static String pebble_app_version = "";
    private static long pebble_sync_value = 0;
    private static boolean sentInitialSync = false;

    private boolean no_signal = false;
    private BgGraphBuilder bgGraphBuilder;
    private BgReading mBgReading;
    private static short sendStep = 5;
    private PebbleDictionary dictionary = new PebbleDictionary();

    public static final UUID PEBBLECLAYAPP_UUID = UUID.fromString("51a6140e-92cc-420f-aef6-51b229666742");
    private static Context mContext;


    //////////////////////////////////////// xDrip experimental code compatibility layer
    //////////////////////////////////////// just so code can be cut and pasted easily
    @Override
    public void initDisplay(Context context, PebbleWatchSync pebbleWatchSync, BgGraphBuilder bgGraphBuilder) {
        mContext = context; // just for code compatibility with xDrip experimental
        super.initDisplay(context, pebbleWatchSync, bgGraphBuilder);
        this.bgGraphBuilder = bgGraphBuilder;
        this.pebbleWatchSync = pebbleWatchSync;


        mBgReading = BgReading.last();

        if (PebbleKit.isWatchConnected(mContext)) {
            Log.i(TAG, "onStartCommand called.  Sending Sync Request");
            transactionFailed = false;
            transactionOk = false;
            sendStep = 5;
            messageInTransit = false;
            done = true;
            sendingData = false;
            dictionary.addInt32(SYNC_KEY, 0);
            sendDataToPebble(dictionary);
            dictionary.remove(SYNC_KEY);
            if(!faceInactive && !watchFaceInstalled && pebble_app_version.isEmpty() && sentInitialSync){
                Log.d(TAG,"onStartCommand: No watch app version, sideloading");
                sideloadInstall(mContext, WATCHAPP_FILENAME);
            }
            else if(!faceInactive && !watchFaceInstalled && !pebble_app_version.contentEquals("xDrip-Pebble2") && sentInitialSync){
                Log.d(TAG,"onStartCommand: Wrong watch app version, sideloading");
                sideloadInstall(mContext, WATCHAPP_FILENAME);
            }
            else if(!faceInactive && !watchFaceInstalled && pebble_app_version.contentEquals("xDrip-Pebble2")&& sentInitialSync) {
                Log.d(TAG,"onStartCommand: Watch face is installed");
                watchFaceInstalled=true;
            }
            sentInitialSync = true;
        } else {
            Log.d(TAG, "onStartCommand; No watch connected.");
        }


    }

    public String slopeOrdinal(){
        if(mBgReading == null) return "0";
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

    //

    @Override
    public UUID watchfaceUUID() {
        return PEBBLECLAYAPP_UUID;
    }

    @Override
    public void startDeviceCommand() {
        transactionFailed = false;
        transactionOk = false;
        sendStep = 5;
        messageInTransit = false;
        done = true;
        sendingData = false;

        if (pebble_app_version.isEmpty() && sentInitialSync) {
            Log.d(TAG, "onStartCommand: No Response and no pebble_app_version.");
        }

        Log.d(TAG, "onStart: Pebble App Version not known.  Sending Version Request");

        sendData();
    }


    @Override
    public void receiveNack(int transactionId) {
        Log.i(TAG, "receiveNack: Got an Nack for transactionId " + transactionId + ". Waiting and retrying.");

        if (retries < 3) {
            transactionFailed = true;
            transactionOk = false;
            messageInTransit = false;
            retries++;
            sendData();
        } else {
            Log.i(TAG, "recieveNAck: exceeded retries.  Giving Up");
            transactionFailed = false;
            transactionOk = false;
            messageInTransit = false;
            sendStep = 4;
            retries = 0;
            done = true;
        }
    }


    @Override
    public void receiveAck(int transactionId) {
        Log.i(TAG, "receiveAck: Got an Ack for transactionId " + transactionId);
        currentTransactionId++;
        messageInTransit = false;
        transactionOk = true;
        transactionFailed = false;
        retries = 0;
        if (!done && sendingData) sendData();
    }


    @Override
    public void receiveData(int transactionId, PebbleDictionary data) {

        Log.d(TAG, "receiveData: transactionId is " + String.valueOf(transactionId));
        lastTransactionId = transactionId;
        Log.d(TAG, "Received Query. data: " + data.size() + ".");
        if (data.size() > 0) {
            pebble_sync_value = data.getUnsignedIntegerAsLong(SYNC_KEY);
            pebble_platform = data.getUnsignedIntegerAsLong(PLATFORM_KEY);
            pebble_app_version = data.getString(VERSION_KEY);
            Log.d(TAG, "receiveData: pebble_sync_value=" + pebble_sync_value + ", pebble_platform=" + pebble_platform + ", pebble_app_version=" + pebble_app_version);
        } else {
            Log.d(TAG, "receiveData: pebble_app_version not known");
        }
        PebbleKit.sendAckToPebble(context, transactionId);
        transactionFailed = false;
        transactionOk = false;
        messageInTransit = false;
        sendStep = 5;
        sendData();
    }


    private String lastBfReadingSent;

    public void buildDictionary() {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());
        if (dictionary == null) {
            dictionary = new PebbleDictionary();
        }

        // check for alerts
        boolean alerting = ActiveBgAlert.currentlyAlerting();
        alerting = alerting && PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_vibe_alerts", true);

        if (alerting) {
            dictionary.addInt8(VIBE_KEY, (byte) 0x03);
        } else {
            dictionary.addInt8(VIBE_KEY, (byte) 0x00);
        }


        if (mBgReading != null) {
            Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal() + " bgReading-" + bgReading() + " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (mBgReading.timestamp / 1000) + " phoneTime-" + (int) (new Date().getTime() / 1000) + " bgDelta-" + bgDelta());
            no_signal = ((new Date().getTime()) - (60000 * 16) - mBgReading.timestamp > 0);
            if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_show_arrows", true)) {
                dictionary.addString(ICON_KEY, "0");
            } else {
                dictionary.addString(ICON_KEY, slopeOrdinal());
            }
            if (no_signal) {
                dictionary.addString(BG_KEY, "?RF");
                if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_vibrate_no_signal", true)) {
                    if (!alerting) dictionary.addInt8(VIBE_KEY, (byte) 0x01);
                }
            } else {
                dictionary.addString(BG_KEY, bgReading());
                if (!alerting) dictionary.addInt8(VIBE_KEY, (byte) 0x00);
            }
            dictionary.addUint32(RECORD_TIME_KEY, (int) (((mBgReading.timestamp + offsetFromUTC) / 1000)));
            if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_show_delta", true)) {
                if (no_signal) {
                    dictionary.addString(BG_DELTA_KEY, "No Signal");
                } else {
                    dictionary.addString(BG_DELTA_KEY, bgDelta());
                }
            } else {
                dictionary.addString(BG_DELTA_KEY, "");
            }
            String msg = PreferenceManager.getDefaultSharedPreferences(mContext).getString("pebble_special_value", "");
            if (bgReading().compareTo(msg) == 0) {
                dictionary.addString(MESSAGE_KEY, PreferenceManager.getDefaultSharedPreferences(mContext).getString("pebble_special_text", "BAZINGA!"));
            } else {
                dictionary.addString(MESSAGE_KEY, "");
            }
        } else {
            Log.v(TAG, "buildDictionary: latest mBgReading is null, so sending default values");
            dictionary.addString(ICON_KEY, slopeOrdinal());
            dictionary.addString(BG_KEY, "?SN");
            //dictionary.addString(BG_KEY, bgReading());
            dictionary.addUint32(RECORD_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC / 1000)));
            dictionary.addString(BG_DELTA_KEY, "No Sensor");
            dictionary.addString(MESSAGE_KEY, "");
        }
        dictionary.addUint32(PHONE_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC) / 1000));
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getString("dex_collection_method", "DexbridgeWixel").compareTo("DexbridgeWixel") == 0 &&
                PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("display_bridge_battery", true)) {
            dictionary.addString(UPLOADER_BATTERY_KEY, bridgeBatteryString());
            dictionary.addString(NAME_KEY, "Bridge");
        } else {
            dictionary.addString(UPLOADER_BATTERY_KEY, phoneBattery());
            dictionary.addString(NAME_KEY, "Phone");
        }
    }


    public void sendTrendToPebble() {
        //create a sparkline bitmap to send to the pebble
        Log.i(TAG, "sendTrendToPebble called: sendStep= " + sendStep + ", messageInTransit= " + messageInTransit + ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
        if (!done && (sendStep == 1 && ((!messageInTransit && !transactionOk && !transactionFailed) || (messageInTransit && !transactionOk && transactionFailed)))) {
            if (!messageInTransit && !transactionOk && !transactionFailed) {
                if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_display_trend", false)) {
                    sendStep = 5;
                    transactionFailed = false;
                    transactionOk = false;
                    done = true;
                    current_size = 0;
                    buff = null;

                }
                boolean highLine = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_high_line", false);
                boolean lowLine = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_low_line", false);
                String trendPeriodString = PreferenceManager.getDefaultSharedPreferences(mContext).getString("pebble_trend_period", "3");
                Integer trendPeriod = Integer.parseInt(trendPeriodString);

                if ((trendPeriod != lastTrendPeriod) || (JoH.ratelimit("pebble-bggraphbuilder", 60))) {
                    long end = System.currentTimeMillis() + (60000 * 5);
                    long start = end - (60000 * 60 * trendPeriod) - (60000 * 10);
                    this.bgGraphBuilder = new BgGraphBuilder(context, start, end, NUM_VALUES, true);
                    lastTrendPeriod = trendPeriod;
                }

                Log.d(TAG, "sendTrendToPebble: highLine is " + highLine + ", lowLine is " + lowLine + ",trendPeriod is " + trendPeriod);
                //encode the trend bitmap as a PNG
                int depth = 16;
                Bitmap bgTrend;
                if (pebble_platform == 0) {
                    Log.d(TAG, "sendTrendToPebble: Encoding trend as Monochrome.");
                    depth = 2;
                    bgTrend = new BgSparklineBuilder(mContext)
                            .setBgGraphBuilder(bgGraphBuilder)
                            .setStart(System.currentTimeMillis() - 60000 * 60 * trendPeriod)
                            .setEnd(System.currentTimeMillis())
                            .setHeightPx(63)
                            .setWidthPx(84)
                            .showHighLine(highLine)
                            .showLowLine(lowLine)
                            .setTinyDots(true)
                            .setSmallDots(false)
                            .build();
                } else {
                    bgTrend = new BgSparklineBuilder(mContext)
                            .setBgGraphBuilder(bgGraphBuilder)
                            .setStart(System.currentTimeMillis() - 60000 * 60 * trendPeriod)
                            .setEnd(System.currentTimeMillis())
                            .setHeightPx(84)
                            .setWidthPx(144)
                            .showHighLine(highLine)
                            .showLowLine(lowLine)
                            .setTinyDots()
                            .setSmallDots()
                            .build();

                }
                byte[] img = SimpleImageEncoder.encodeBitmapAsPNG(bgTrend, true, depth, true);
                image_size = img.length;
                buff = ByteBuffer.wrap(img);
                bgTrend.recycle();
                //Prepare the TREND_BEGIN_KEY dictionary.  We expect the length of the image to always be less than 65535 bytes.
                if (buff != null) {
                    if (dictionary == null) {
                        dictionary = new PebbleDictionary();
                    }
                    dictionary.addInt16(TREND_BEGIN_KEY, (short) image_size);
                    Log.d(TAG, "sendTrendToPebble: Sending TREND_BEGIN_KEY to pebble, image size is " + image_size);
                } else {
                    Log.d(TAG, "sendTrendToPebble: Error converting stream to ByteBuffer, buff is null.");
                    sendStep = 4;
                    return;
                }
            }
            transactionFailed = false;
            transactionOk = false;
            messageInTransit = true;
            sendDataToPebble(this.dictionary);
        }
        if (sendStep == 1 && !done && !messageInTransit && transactionOk && !transactionFailed) {
            Log.i(TAG, "sendTrendToPebble: sendStep " + sendStep + " complete.");
            dictionary.remove(TREND_BEGIN_KEY);
            current_size = 0;
            sendStep = 2;
            transactionOk = false;
        }
        if (!done && ((sendStep == 2 && !messageInTransit) || sendStep == 3 && transactionFailed)) {
            if (!transactionFailed && !messageInTransit) {
                // send image chunks to Pebble.
                Log.d(TAG, "sendTrendToPebble: current_size is " + current_size + ", image_size is " + image_size);
                if (current_size < image_size) {
                    dictionary.remove(TREND_DATA_KEY);
                    if ((image_size <= (current_size + CHUNK_SIZE))) {
                        chunk = new byte[image_size - current_size];
                        Log.d(TAG, "sendTrendToPebble: sending chunk of size " + (image_size - current_size));
                        buff.get(chunk, 0, image_size - current_size);
                        sendStep = 3;
                    } else {
                        chunk = new byte[CHUNK_SIZE];
                        Log.d(TAG, "sendTrendToPebble: sending chunk of size " + CHUNK_SIZE);
                        buff.get(chunk, 0, CHUNK_SIZE);
                        current_size += CHUNK_SIZE;
                    }
                    dictionary.addBytes(TREND_DATA_KEY, chunk);
                }
            }
            Log.d(TAG, "sendTrendToPebble: Sending TREND_DATA_KEY to pebble, current_size is " + current_size);
            transactionFailed = false;
            transactionOk = false;
            messageInTransit = true;
            sendDataToPebble(this.dictionary);
        }
        if (sendStep == 3 && !done && !messageInTransit && transactionOk && !transactionFailed) {
            Log.i(TAG, "sendTrendToPebble: sendStep " + sendStep + " complete.");
            dictionary.remove(TREND_DATA_KEY);
            sendStep = 4;
            transactionOk = false;
            buff = null;
            stream = null;
        }
        if (!done && (sendStep == 4 && ((!messageInTransit && !transactionOk && !transactionFailed) || (messageInTransit && !transactionOk && transactionFailed)))) {
            if (!transactionFailed) {
                // prepare the TREND_END_KEY dictionary and send it.
                dictionary.addUint8(TREND_END_KEY, (byte) 0);
                Log.d(TAG, "sendTrendToPebble: Sending TREND_END_KEY to pebble.");
            }
            transactionFailed = false;
            transactionOk = false;
            messageInTransit = true;
            sendDataToPebble(this.dictionary);
        }
        if (sendStep == 4 && !done && transactionOk && !messageInTransit && !transactionFailed) {
            Log.i(TAG, "sendTrendToPebble: sendStep " + sendStep + " complete.");
            dictionary.remove(TREND_END_KEY);
            sendStep = 5;
            transactionFailed = false;
            transactionOk = false;
            done = true;
            current_size = 0;
            buff = null;
        }
    }


    private void clearDictionary() {
        this.dictionary.remove(ICON_KEY);
        this.dictionary.remove(BG_KEY);
        this.dictionary.remove(NAME_KEY);
        this.dictionary.remove(BG_DELTA_KEY);
        this.dictionary.remove(PHONE_TIME_KEY);
        this.dictionary.remove(RECORD_TIME_KEY);
        this.dictionary.remove(UPLOADER_BATTERY_KEY);
        this.dictionary.remove(VIBE_KEY);
    }

    public String bridgeBatteryString() {
        return String.format("%d", PreferenceManager.getDefaultSharedPreferences(mContext).getInt("bridge_battery", 0));
    }


    public void sendData() {
        final boolean online = PebbleKit.isWatchConnected(mContext);
        if (online) {
            if (sendStep == 5) {
                sendStep = 0;
                done = false;
                dictionary.remove(ICON_KEY);
                dictionary.remove(BG_KEY);
                dictionary.remove(NAME_KEY);
                dictionary.remove(BG_DELTA_KEY);
                dictionary.remove(PHONE_TIME_KEY);
                dictionary.remove(RECORD_TIME_KEY);
                dictionary.remove(UPLOADER_BATTERY_KEY);
                dictionary.remove(VIBE_KEY);
            }
            Log.i(TAG, "sendData: messageInTransit= " + messageInTransit + ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
            if (sendStep == 0 && !messageInTransit && !transactionOk && !transactionFailed) {
                mBgReading = BgReading.last();
                sendingData = true;
                buildDictionary();
                sendDownload();
            }
            if (sendStep == 0 && !messageInTransit && transactionOk && !transactionFailed) {
                Log.i(TAG, "sendData: sendStep 0 complete, clearing dictionary");
                dictionary.remove(ICON_KEY);
                dictionary.remove(BG_KEY);
                dictionary.remove(NAME_KEY);
                dictionary.remove(BG_DELTA_KEY);
                dictionary.remove(PHONE_TIME_KEY);
                dictionary.remove(RECORD_TIME_KEY);
                dictionary.remove(UPLOADER_BATTERY_KEY);
                dictionary.remove(VIBE_KEY);
                transactionOk = false;
                sendStep = 1;
            }
            if (sendStep > 0 && sendStep < 5) {
                if (!PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_display_trend", false)) {
                    sendStep = 5;
                } else {
                    sendTrendToPebble();
                }
            }
            if (sendStep == 5) {
                sendStep = 5;
                Log.i(TAG, "sendData: finished sending.  sendStep = " + sendStep);
                done = true;
                transactionFailed = false;
                transactionOk = false;
                messageInTransit = false;
                sendingData = false;
            }
        }
        pebble_watchdog(online, TAG);
    }


    private void sideloadInstall(Context context, String filename) {
        try {
            context.startActivity(new Intent(context, InstallPebbleTrendClayWatchFace.class));
        } catch (Exception e) {
            Log.e(TAG, "Got exception in sideloadInstall: " + e);
        }
    }


    public String getBgDelta() {
        return this.bgGraphBuilder.unitizedDeltaString(getBooleanValue("pebble_show_delta_units"), true);
    }

    public String bgReading() {
        Sensor sensor = new Sensor();
        if (PreferenceManager.getDefaultSharedPreferences(mContext).getString("dex_collection_method", "DexbridgeWixel").compareTo("DexbridgeWixel") == 0) {
            Log.d(TAG, "bgReading: found xBridge wixel, sensor.isActive=" + sensor.isActive() + ", sensor.stopped_at=" + sensor.currentSensor().stopped_at + ", sensor.started_at=" + sensor.currentSensor().started_at);
            if (!(sensor.isActive())) {
                Log.d(TAG, "bgReading: No active Sensor");
                return "?SN";
            }
            if ((sensor.currentSensor().started_at + 60000 * 60 * 2 >= System.currentTimeMillis())) {
                return "?CD";
            }
        }
        return bgGraphBuilder.unitized_string(mBgReading.calculated_value);
    }

    public String bgDelta() {
        return new BgGraphBuilder(mContext).unitizedDeltaString(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean("pebble_show_delta_units", false), true);
    }

    public String phoneBattery() {
        return String.valueOf(getBatteryLevel());
    }

    public String bgUnit() {
        return bgGraphBuilder.unit();
    }


    public void sendDownload() {
        if (this.dictionary != null && this.context != null) {
            Log.d(TAG, "sendDownload: Sending data to pebble");
            messageInTransit = true;
            transactionFailed = false;
            transactionOk = false;
            sendDataToPebble(this.dictionary);
        }
    }

    /*
    public int getBatteryLevel() {
     // is in abstract base class
    }
    */


    public boolean doWeDisplayTrendData() {
        return getBooleanValue("pebble_display_trend");
    }

    public String getBgReading() {

        if (isDexBridgeWixel()) {
            Log.d(TAG, "bgReading: found xBridge wixel, sensor.isActive=" + Sensor.isActive() + ", sensor.stopped_at=" + Sensor.currentSensor().stopped_at + ", sensor.started_at=" + Sensor.currentSensor().started_at);
            if (!(Sensor.isActive())) {
                Log.d(TAG, "bgReading: No active Sensor");
                return "?SN";
            }
            if ((Sensor.currentSensor().started_at + 60000 * 60 * 2 >= System.currentTimeMillis())) {
                return "?CD";
            }
        }

        return this.bgGraphBuilder.unitized_string(this.bgReading.calculated_value);
    }

}