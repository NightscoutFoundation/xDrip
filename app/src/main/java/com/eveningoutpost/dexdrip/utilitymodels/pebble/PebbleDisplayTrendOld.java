package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.graphics.Bitmap;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.SimpleImageEncoder;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;
import com.getpebble.android.kit.util.PebbleTuple;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 * <p/>
 * Changed by Andy (created from PebbleSync from PebbleTrend branch)
 */
public class PebbleDisplayTrendOld extends PebbleDisplayAbstract {

    private final static String TAG = PebbleDisplayTrendOld.class.getSimpleName();

  /*
    public static final int TREND_BEGIN_KEY = 7;
    public static final int TREND_DATA_KEY = 8;
    public static final int TREND_END_KEY = 9;
    public static final int MESSAGE_KEY = 10;
    public static final int VIBE_KEY = 11;

    private static final int NO_BLUETOOTH_KEY = 111;
    private static final int COLLECT_HEALTH_KEY = 112;

    public static final int SYNC_KEY = 1000;
    public static final int PLATFORM_KEY = 1001;
    public static final int VERSION_KEY = 1002;
*/
    private static final int CHUNK_SIZE = 100;
    public static final boolean d = false;

    private static byte last_collect_health_key_byte = 0x1A;
    private static byte last_bluetooth_key_byte = 0x1A;
    private static boolean messageInTransit = false;
    private static boolean transactionFailed = false;
    private static boolean transactionOk = false;
    private static boolean done = false;
    private static boolean sendingData = false;
    private static int lastTrendPeriod = -1;
    private static int current_size = 0;
    private static int image_size = 0;
    private static byte[] chunk;
    private static ByteBuffer buff = null;
    public static int retries = 0;
    private static final boolean debugPNG = false;
    private static boolean didTrend = false;
    private static final ReentrantLock lock = new ReentrantLock();

    private static long pebble_platform = -1;
    private static String pebble_app_version = "";
    private static long pebble_sync_value = 0;
    private static boolean sentInitialSync = false;

    private static short sendStep = 5;
    private final PebbleDictionary dictionary = new PebbleDictionary();


    @Override
    public void startDeviceCommand() {
        if (JoH.ratelimitmilli("pebble-trend", 250)) {
            transactionFailed = false;
            transactionOk = false;
            sendStep = 5;
            messageInTransit = false;
            done = true;
            sendingData = false;
            sendData();
        } else {
            Log.d(TAG, "SendData ratelimited!");
        }
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

        if (d) Log.i(TAG, "receiveAck: Got an Ack for transactionId " + transactionId);
        messageInTransit = false;
        transactionOk = true;
        transactionFailed = false;
        retries = 0;

        if (!done && sendingData)
            sendData();
    }


    @Override
    public void receiveData(int transactionId, PebbleDictionary data) {
        Log.d(TAG, "receiveData: transactionId is " + String.valueOf(transactionId));
        this.pebbleWatchSync.lastTransactionId = transactionId;
        Log.d(TAG, "Received Query. data: " + data.size() + ".");
        PebbleKit.sendAckToPebble(this.context, transactionId);
        evaluateDataFromPebble(data);
        transactionFailed = false;
        transactionOk = false;
        messageInTransit = false;
        sendStep = 5;
        sendData();
    }

    private void evaluateDataFromPebble(PebbleDictionary data) {

        try {
            if (data.size() > 0) {
                pebble_sync_value = data.getUnsignedIntegerAsLong(SYNC_KEY);
                pebble_platform = data.getUnsignedIntegerAsLong(PLATFORM_KEY);
                pebble_app_version = data.getString(VERSION_KEY);
                Log.d(TAG, "receiveData: pebble_sync_value=" + pebble_sync_value + ", pebble_platform=" + pebble_platform + ", pebble_app_version=" + pebble_app_version);

                switch ((int) pebble_platform) {
                    case 0:
                        if (PebbleUtil.pebbleDisplayType != PebbleDisplayType.TrendClassic) {
                            PebbleUtil.pebbleDisplayType = PebbleDisplayType.TrendClassic;
                            //JoH.static_toast_short("Switching to Pebble Classic Trend");
                            Log.d(TAG, "Changing to Classic Trend due to platform id");
                        }
                        break;
                }

            } else {
                Log.d(TAG, "receiveData: pebble_app_version not known");
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Got exception trying to parse data from pebble: " + e);
        }
    }



    private String lastBfReadingSent;

    public PebbleDictionary buildDictionary() {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());

       // if (this.dictionary == null) {
       //     this.dictionary = new PebbleDictionary();
       // }

        if (use_best_glucose ? (this.dg != null) : (this.bgReading != null)) {
            boolean no_signal;

            final String slopeOrdinal = getSlopeOrdinal();
            final String bgReadingS = getBgReading();

            if (use_best_glucose)
            {
                Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal + " bgReading-" + bgReadingS + //
                        " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (dg.timestamp / 1000) + //
                        " phoneTime-" + (int) (new Date().getTime() / 1000) + " getBgDelta-" + getBgDelta());
                no_signal = (dg.mssince > Home.stale_data_millis());
            } else {
                Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal + " bgReading-" + bgReadingS + //
                        " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (this.bgReading.timestamp / 1000) + //
                        " phoneTime-" + (int) (new Date().getTime() / 1000) + " getBgDelta-" + getBgDelta());
                no_signal = ((new Date().getTime()) - Home.stale_data_millis() - this.bgReading.timestamp > 0);
            }

            if (!getBooleanValue("pebble_show_arrows") || no_signal) {
                this.dictionary.addString(ICON_KEY, "0");
            } else {
                this.dictionary.addString(ICON_KEY, slopeOrdinal);
            }

            if (no_signal) {
                // We display last reading, even if none was sent for some time.
                if (this.lastBfReadingSent != null) {
                    this.dictionary.addString(BG_KEY, this.lastBfReadingSent);
                    this.dictionary.addInt8(VIBE_KEY, (byte) (getBooleanValue("pebble_vibrate_no_signal") ? 0x01 : 0x00)); // not sure what this does exactly
                } else {
                    this.dictionary.addString(BG_KEY, "?RF");
                    this.dictionary.addInt8(VIBE_KEY, (byte) (getBooleanValue("pebble_vibrate_no_signal") ? 0x01 : 0x00));
                }
            } else {
                this.dictionary.addString(BG_KEY, bgReadingS);
                if (getBooleanValue("pebble_vibe_alerts", false) && ActiveBgAlert.currentlyAlerting()) {
                    dictionary.addInt8(VIBE_KEY, (byte) 0x03);
                } else {
                    this.dictionary.addInt8(VIBE_KEY, (byte) 0x00);
                }
                this.lastBfReadingSent = bgReadingS;
            }

            if (use_best_glucose) {
                this.dictionary.addUint32(RECORD_TIME_KEY, (int) (((dg.timestamp + offsetFromUTC) / 1000)));
            } else {
                this.dictionary.addUint32(RECORD_TIME_KEY, (int) (((this.bgReading.timestamp + offsetFromUTC) / 1000)));
            }

            if (getBooleanValue("pebble_show_delta")) {
                if (no_signal) {
                    this.dictionary.addString(BG_DELTA_KEY, "No Signal");
                } else {
                    this.dictionary.addString(BG_DELTA_KEY, getBgDelta());
                    if (((keyStore.getS("bwp_last_insulin") != null) && (JoH.msSince(keyStore.getL("bwp_last_insulin_timestamp")) < Constants.MINUTE_IN_MS * 11))
                            && getBooleanValue("pebble_show_bwp")) {
                        this.dictionary.addString(BG_DELTA_KEY, PEBBLE_BWP_SYMBOL + keyStore.getS("bwp_last_insulin")); // 😐
                    }

                }
            } else {
                this.dictionary.addString(BG_DELTA_KEY, "");
            }

            String msg = PreferenceManager.getDefaultSharedPreferences(this.context).getString("pebble_special_value", "");

            byte bluetooth_key_byte = (byte) (getBooleanValue("pebble_vibrate_no_bluetooth") ? 0x01 : 0x00);
            this.dictionary.addInt8(NO_BLUETOOTH_KEY, bluetooth_key_byte);

            byte collect_health_key_byte = (byte) (getBooleanValue("use_pebble_health") ? 0x01 : 0x00);
            if ((collect_health_key_byte != last_collect_health_key_byte) || JoH.ratelimit("collect_health_key_byte", 3)) {
                this.dictionary.addInt8(COLLECT_HEALTH_KEY, collect_health_key_byte);
                last_collect_health_key_byte = collect_health_key_byte;
            } else {
                this.dictionary.remove(COLLECT_HEALTH_KEY);
            }

            // TODO I think special message is only appropriate with flat trend
            if (bgReadingS.equalsIgnoreCase(msg)) {
                this.dictionary.addString(MESSAGE_KEY, PreferenceManager.getDefaultSharedPreferences(this.context).getString("pebble_special_text", "BAZINGA!"));
            } else {
                this.dictionary.addString(MESSAGE_KEY, "");
            }
        } else {
            Log.v(TAG, "buildDictionary: latest mBgReading is null, so sending default values");
            this.dictionary.addString(ICON_KEY, getSlopeOrdinal());
            this.dictionary.addString(BG_KEY, "?SN");
            this.dictionary.addUint32(RECORD_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC / 1000)));
            this.dictionary.addString(BG_DELTA_KEY, "No Sensor");
            this.dictionary.addString(MESSAGE_KEY, "");
        }

        this.dictionary.addUint32(PHONE_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC) / 1000));

        if (JoH.ratelimit("add_battery_status", 60)) {
            addBatteryStatusToDictionary(this.dictionary);
        } else {
            removeBatteryStatusFromDictionary(this.dictionary);
        }

        return this.dictionary;
    }

    private synchronized void sendTrendToPebble(boolean clearTrend) {
        //create a sparkline bitmap to send to the pebble

        final Bitmap blankTrend;
        if (clearTrend) { blankTrend = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888); Log.d(TAG,"Attempting to blank trend"); } else { blankTrend = null; didTrend=true; }


        Log.i(TAG, "sendTrendToPebble called: sendStep= " + sendStep + ", messageInTransit= " + messageInTransit + //
                ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
        if (!done && (sendStep == 1 && ((!messageInTransit && !transactionOk && !transactionFailed) || //
                (messageInTransit && !transactionOk && transactionFailed)))) {

            if (!messageInTransit && !transactionOk && !transactionFailed) {

                if (!clearTrend && (!doWeDisplayTrendData())) {
                    sendStep = 5;
                    transactionFailed = false;
                    transactionOk = false;
                    done = true;
                    current_size = 0;
                    buff = null;
                }

                boolean highLine = getBooleanValue("pebble_high_line");
                boolean lowLine = getBooleanValue("pebble_low_line");

                String trendPeriodString = PreferenceManager.getDefaultSharedPreferences(this.context).getString("pebble_trend_period", "3");
                Integer trendPeriod = Integer.parseInt(trendPeriodString);

                if ((trendPeriod != lastTrendPeriod) || (JoH.ratelimit("pebble-bggraphbuilder",60)))
                {
                    long end = System.currentTimeMillis() + (60000 * 5);
                    long start = end - (60000 * 60*trendPeriod) -  (60000 * 10);
                    this.bgGraphBuilder = new BgGraphBuilder(context, start, end, MAX_VALUES, true);
                    lastTrendPeriod=trendPeriod;
                }


                Log.d(TAG, "sendTrendToPebble: highLine is " + highLine + ", lowLine is " + lowLine + ",trendPeriod is " + trendPeriod);
                Bitmap bgTrend = new BgSparklineBuilder(this.context)
                        .setBgGraphBuilder(this.bgGraphBuilder)
                        .setStart(System.currentTimeMillis() - 60000 * 60 * trendPeriod)
                        .setEnd(System.currentTimeMillis())
                        .setHeightPx(PebbleUtil.pebbleDisplayType == PebbleDisplayType.TrendClassic ? 63 : 84) // 84
                        .setWidthPx(PebbleUtil.pebbleDisplayType == PebbleDisplayType.TrendClassic ? 84 : 144) // 144
                        .showHighLine(highLine)
                        .showLowLine(lowLine)
                       // .showAxes(true)
                        .setTinyDots(Pref.getBooleanDefaultFalse("pebble_tiny_dots"))
                        .setShowFiltered(Pref.getBooleanDefaultFalse("pebble_filtered_line"))
                                //.setSmallDots()
                        .build();

                //encode the trend bitmap as a PNG

                final byte[] img = SimpleImageEncoder.encodeBitmapAsPNG(clearTrend ? blankTrend : bgTrend, true, PebbleUtil.pebbleDisplayType == PebbleDisplayType.TrendClassic ? 2 : 16, true);

                if (debugPNG) {
                    try {
                        // save debug image output
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/sdcard/download/xdrip-trend-debug.png"));
                        bos.write(img);
                        bos.flush();
                        bos.close();
                    } catch (FileNotFoundException e) {

                    } catch (IOException e) {
                    }
                    // also save full colour
                    final byte[] img2 = SimpleImageEncoder.encodeBitmapAsPNG(bgTrend, true, 16, true);
                    try {
                        // save debug image output
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("/sdcard/download/xdrip-trend-debug-colour.png"));
                        bos.write(img2);
                        bos.flush();
                        bos.close();
                    } catch (FileNotFoundException e) {

                    } catch (IOException e) {

                    }
                }

                image_size = img.length;
                buff = ByteBuffer.wrap(img);
                bgTrend.recycle();
                //Prepare the TREND_BEGIN_KEY dictionary.  We expect the length of the image to always be less than 65535 bytes.
                if (buff != null) {
                    //if (this.dictionary == null) {
                    //    this.dictionary = new PebbleDictionary();
                    //}
                    this.dictionary.addInt16(TREND_BEGIN_KEY, (short) image_size);
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
            this.dictionary.remove(TREND_BEGIN_KEY);
            current_size = 0;
            sendStep = 2;
            transactionOk = false;
        }

        if (!done && ((sendStep == 2 && !messageInTransit) || sendStep == 3 && transactionFailed)) {
            if (!transactionFailed && !messageInTransit) {
                // send image chunks to Pebble.
                if (d) Log.d(TAG, "sendTrendToPebble: current_size is " + current_size + ", image_size is " + image_size);
                if (current_size < image_size) {
                    this.dictionary.remove(TREND_DATA_KEY);
                    if ((image_size <= (current_size + CHUNK_SIZE))) {
                        chunk = new byte[image_size - current_size];
                        if (d) Log.d(TAG, "sendTrendToPebble: sending chunk of size " + (image_size - current_size));
                        buff.get(chunk, 0, image_size - current_size);
                        sendStep = 3;
                    } else {
                        chunk = new byte[CHUNK_SIZE];
                        if (d) Log.d(TAG, "sendTrendToPebble: sending chunk of size " + CHUNK_SIZE);
                        buff.get(chunk, 0, CHUNK_SIZE);
                        current_size += CHUNK_SIZE;
                    }
                    this.dictionary.addBytes(TREND_DATA_KEY, chunk);
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
            this.dictionary.remove(TREND_DATA_KEY);
            sendStep = 4;
            transactionOk = false;
            buff = null;
            //stream = null;
        }

        if (!done && (sendStep == 4 && ((!messageInTransit && !transactionOk && !transactionFailed) || //
                (messageInTransit && !transactionOk && transactionFailed)))) {
            if (!transactionFailed) {
                // prepare the TREND_END_KEY dictionary and send it.
                this.dictionary.addUint8(TREND_END_KEY, (byte) 0);
                Log.d(TAG, "sendTrendToPebble: Sending TREND_END_KEY to pebble.");
            }

            transactionFailed = false;
            transactionOk = false;
            messageInTransit = true;
            sendDataToPebble(this.dictionary);
        }

        if (sendStep == 4 && !done && transactionOk && !messageInTransit && !transactionFailed) {
            Log.i(TAG, "sendTrendToPebble: sendStep " + sendStep + " complete.");
            this.dictionary.remove(TREND_END_KEY);
            sendStep = 5;
            transactionFailed = false;
            transactionOk = false;
            done = true;
            current_size = 0;
            buff = null;
            if (clearTrend) didTrend=false; // cleared
        }
    }


    private void clearDictionary() {
        synchronized (this.dictionary) {
            // might just be easier to instantiate a new dictionary
            final List<Integer> temp = new ArrayList<>();
            for (PebbleTuple aDictionary : this.dictionary) {
                temp.add(aDictionary.key);
            }
            for (Integer i : temp) {
                this.dictionary.remove(i);
            }
        }

  /*      this.dictionary.remove(ICON_KEY);
        this.dictionary.remove(BG_KEY);
        this.dictionary.remove(NAME_KEY);
        this.dictionary.remove(BG_DELTA_KEY);
        this.dictionary.remove(PHONE_TIME_KEY);
        this.dictionary.remove(RECORD_TIME_KEY);
        this.dictionary.remove(UPLOADER_BATTERY_KEY);
        this.dictionary.remove(VIBE_KEY);

        this.dictionary.remove(COLLECT_HEALTH_KEY);
        this.dictionary.remove(NO_BLUETOOTH_KEY);

        */
    }


    public synchronized void sendData() {
        PowerManager.WakeLock wl = JoH.getWakeLock("pebble-trend-sendData",60000);
        try {
            if (lock.tryLock(60, TimeUnit.SECONDS)) {
                try {
                    if (d) Log.d(TAG, "Sendstep: " + sendStep);
                    if (sendStep == 5) {
                        sendStep = 0;
                        done = false;
                        clearDictionary();
                    }

                    if (d)
                        Log.i(TAG, "sendData: messageInTransit= " + messageInTransit + ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
                    if (sendStep == 0 && !messageInTransit && !transactionOk && !transactionFailed) {

                        if (use_best_glucose) {
                            this.dg = BestGlucose.getDisplayGlucose();
                        } else {
                            this.bgReading = BgReading.last();
                        }

                        sendingData = true;
                        buildDictionary();
                        sendDownload();
                    }


                    if (sendStep == 0 && !messageInTransit && transactionOk && !transactionFailed) {
                        if (d) Log.i(TAG, "sendData: sendStep 0 complete, clearing dictionary");
                        clearDictionary();
                        transactionOk = false;
                        sendStep = 1;
                    }
                    if (sendStep > 0 && sendStep < 5) {
                        if (!doWeDisplayTrendData()) {
                            if (didTrend) {
                                sendTrendToPebble(true); // clear trend image
                            } else {
                                sendStep = 5;
                            }
                        } else {
                            sendTrendToPebble(false);
                        }
                    }

                    if (sendStep == 5) {
                        if (d)
                            Log.i(TAG, "sendData: finished sending.  sendStep = " + sendStep);
                        done = true;
                        transactionFailed = false;
                        transactionOk = false;
                        messageInTransit = false;
                        sendingData = false;
                    }
                } catch (final NullPointerException e) {
                    Log.e(TAG, "Got null pointer error " + e);
                } finally {
                    lock.unlock();
                }
            } else {
                Log.w(TAG, "Could not acquire lock within timeout!");
            }
        } catch (InterruptedException e)
        {
            Log.w(TAG,"Got interrupted while waiting to acquire lock!");
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }


    public String getBgDelta() {
        final boolean show_delta_units = getBooleanValue("pebble_show_delta_units");
        return (use_best_glucose) ? (show_delta_units ? dg.unitized_delta : dg.unitized_delta_no_units)
                : this.bgGraphBuilder.unitizedDeltaString(show_delta_units, true);
    }


    public void sendDownload() {
        Log.d(TAG,"send download called");
        if (this.dictionary != null && this.context != null) {
            Log.d(TAG, "sendDownload: Sending data to pebble");
            messageInTransit = true;
            transactionFailed = false;
            transactionOk = false;
            sendDataToPebble(this.dictionary);
        }
    }


    public boolean doWeDisplayTrendData() {
        return getBooleanValue("pebble_display_trend");
    }


}

