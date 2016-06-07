package com.eveningoutpost.dexdrip.UtilityModels.pebble;

import android.graphics.Bitmap;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.BgSparklineBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.SimpleImageEncoder;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 * <p/>
 * Changed by Andy (created from PebbleSync from PebbleTrend branch)
 */
public class PebbleDisplayTrendOld extends PebbleDisplayAbstract {

    private final static String TAG = PebbleDisplayTrendOld.class.getSimpleName();

    public static final int TREND_BEGIN_KEY = 7;
    public static final int TREND_DATA_KEY = 8;
    public static final int TREND_END_KEY = 9;
    public static final int MESSAGE_KEY = 10;
    public static final int VIBE_KEY = 11;

    public static final int CHUNK_SIZE = 100;

    private static boolean messageInTransit = false;
    private static boolean transactionFailed = false;
    private static boolean transactionOk = false;
    private static boolean done = false;
    private static boolean sendingData = false;
    private static int current_size = 0;
    private static int image_size = 0;
    private static byte[] chunk;
    private static ByteBuffer buff = null;
    public static int retries = 0;

    private static short sendStep = 5;
    private PebbleDictionary dictionary = new PebbleDictionary();


    @Override
    public void startDeviceCommand() {
        transactionFailed = false;
        transactionOk = false;
        sendStep = 5;
        messageInTransit = false;
        done = true;
        sendingData = false;
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
        transactionFailed = false;
        transactionOk = false;
        messageInTransit = false;
        sendStep = 5;
        sendData();
    }

    private String lastBfReadingSent;

    public PebbleDictionary buildDictionary() {
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());

        if (this.dictionary == null) {
            this.dictionary = new PebbleDictionary();
        }
        
        if (this.bgReading != null) {
            boolean no_signal;

            String slopeOrdinal = getSlopeOrdinal();
            String bgReadingS = getBgReading();

            Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal + " bgReading-" + bgReadingS + //
                    " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (this.bgReading.timestamp / 1000) + //
                    " phoneTime-" + (int) (new Date().getTime() / 1000) + " getBgDelta-" + getBgDelta());
            no_signal = ((new Date().getTime()) - (60000 * 11) - this.bgReading.timestamp > 0);

            if (!getBooleanValue("pebble_show_arrows")) {
                this.dictionary.addString(ICON_KEY, "0");
            } else {
                this.dictionary.addString(ICON_KEY, slopeOrdinal);
            }

            if (no_signal) {
                // We display last reading, even if none was sent for some time.
                if (this.lastBfReadingSent != null) {
                    this.dictionary.addString(BG_KEY, this.lastBfReadingSent);
                    this.dictionary.addInt8(VIBE_KEY, (byte) 0x01); // not sure what this does exactly
                } else {
                    this.dictionary.addString(BG_KEY, "?RF");
                    this.dictionary.addInt8(VIBE_KEY, (byte) 0x01);
                }
            } else {
                this.dictionary.addString(BG_KEY, bgReadingS);
                this.dictionary.addInt8(VIBE_KEY, (byte) 0x00);
                this.lastBfReadingSent = bgReadingS;
            }

            this.dictionary.addUint32(RECORD_TIME_KEY, (int) (((this.bgReading.timestamp + offsetFromUTC) / 1000)));

            if (getBooleanValue("pebble_show_delta")) {
                if (no_signal) {
                    this.dictionary.addString(BG_DELTA_KEY, "No Signal");
                } else {
                    this.dictionary.addString(BG_DELTA_KEY, getBgDelta());
                }
            } else {
                this.dictionary.addString(BG_DELTA_KEY, "");
            }

            String msg = PreferenceManager.getDefaultSharedPreferences(this.context).getString("pebble_special_value", "");

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

        addBatteryStatusToDictionary(this.dictionary);

        return this.dictionary;
    }

    private void sendTrendToPebble() {
        //create a sparkline bitmap to send to the pebble
        Log.i(TAG, "sendTrendToPebble called: sendStep= " + sendStep + ", messageInTransit= " + messageInTransit + //
                ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
        if (!done && (sendStep == 1 && ((!messageInTransit && !transactionOk && !transactionFailed) || //
                (messageInTransit && !transactionOk && transactionFailed)))) {

            if (!messageInTransit && !transactionOk && !transactionFailed) {

                if (!doWeDisplayTrendData()) {
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

                Log.d(TAG, "sendTrendToPebble: highLine is " + highLine + ", lowLine is " + lowLine + ",trendPeriod is " + trendPeriod);
                Bitmap bgTrend = new BgSparklineBuilder(this.context)
                        .setBgGraphBuilder(this.bgGraphBuilder)
                        .setStart(System.currentTimeMillis() - 60000 * 60 * trendPeriod)
                        .setEnd(System.currentTimeMillis())
                        .setHeightPx(84) // 84
                        .setWidthPx(144)
                        .showHighLine(highLine)
                        .showLowLine(lowLine)
                        .setTinyDots()
                                //.setSmallDots()
                        .build();

                //encode the trend bitmap as a PNG
                byte[] img = SimpleImageEncoder.encodeBitmapAsPNG(bgTrend, true, 16, true);
                //byte[] img = SimpleImageEncoder.encodeBitmapAsPNG(bgTrend, true, 2, true);
                image_size = img.length;
                buff = ByteBuffer.wrap(img);
                bgTrend.recycle();
                //Prepare the TREND_BEGIN_KEY dictionary.  We expect the length of the image to always be less than 65535 bytes.
                if (buff != null) {
                    if (this.dictionary == null) {
                        this.dictionary = new PebbleDictionary();
                    }
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
                Log.d(TAG, "sendTrendToPebble: current_size is " + current_size + ", image_size is " + image_size);
                if (current_size < image_size) {
                    this.dictionary.remove(TREND_DATA_KEY);
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
    }


    public void sendData() {
        if (PebbleKit.isWatchConnected(this.context)) {
            Log.d(TAG,"Sendstep: "+sendStep);
            if (sendStep == 5) {
                sendStep = 0;
                done = false;
                clearDictionary();
            }

            Log.i(TAG, "sendData: messageInTransit= " + messageInTransit + ", transactionFailed= " + transactionFailed + ", sendStep= " + sendStep);
            if (sendStep == 0 && !messageInTransit && !transactionOk && !transactionFailed) {
                this.bgReading = BgReading.last();
                sendingData = true;
                buildDictionary();
                sendDownload();
            }


            if (sendStep == 0 && !messageInTransit && transactionOk && !transactionFailed) {
                Log.i(TAG, "sendData: sendStep 0 complete, clearing dictionary");
                clearDictionary();
                transactionOk = false;
                sendStep = 1;
            }
            if (sendStep > 0 && sendStep < 5) {
                if (!doWeDisplayTrendData()) {
                    sendStep = 5;
                } else {
                    sendTrendToPebble();
                }
            }

            if (sendStep == 5) {
                Log.i(TAG, "sendData: finished sending.  sendStep = " + sendStep);
                done = true;
                transactionFailed = false;
                transactionOk = false;
                messageInTransit = false;
                sendingData = false;
            }
        }
    }


    public String getBgDelta() {
        return this.bgGraphBuilder.unitizedDeltaString(getBooleanValue("pebble_show_delta_units"), true);
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

