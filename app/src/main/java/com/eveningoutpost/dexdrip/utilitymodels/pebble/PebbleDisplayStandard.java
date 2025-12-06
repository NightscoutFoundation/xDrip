package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.content.Intent;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.Date;
import java.util.TimeZone;

/**
 * Created by THE NIGHTSCOUT PROJECT CONTRIBUTORS (and adapted to fit the needs of this project)
 * <p/>
 * Changed by Andy (created from original PebbleSync)
 */
public class PebbleDisplayStandard extends PebbleDisplayAbstract {

    private final static String TAG = PebbleDisplayStandard.class.getSimpleName();

    private static double last_time_seen = 0;


    @Override
    public void startDeviceCommand() {
        sendData();
    }


    public void receiveData(int transactionId, PebbleDictionary data) {
        Log.d(TAG, "receiveData: transactionId is " + String.valueOf(transactionId));
        if (PebbleWatchSync.lastTransactionId == 0 || transactionId != PebbleWatchSync.lastTransactionId) {
            PebbleWatchSync.lastTransactionId = transactionId;
            Log.d(TAG, "Received Query. data: " + data.size() + ". sending ACK and data");
            PebbleKit.sendAckToPebble(this.context, transactionId);
            sendData();
        } else {
            Log.d(TAG, "receiveData: lastTransactionId is " + String.valueOf(PebbleWatchSync.lastTransactionId) + ", sending NACK");
            PebbleKit.sendNackToPebble(this.context, transactionId);
        }
    }


    private PebbleDictionary buildDictionary() {
        PebbleDictionary dictionary = new PebbleDictionary();
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUTC = tz.getOffset(now.getTime());

        final String bgDelta = getBgDelta();
        final String bgReadingS = getBgReading();
        final String slopeOrdinal = getSlopeOrdinal();
        //boolean no_signal;

        if (use_best_glucose) {
            Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal + " bgReading-" + bgReadingS + //
                    " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (dg.timestamp / 1000) + //
                    " phoneTime-" + (int) (new Date().getTime() / 1000) + " getBgDelta-" + getBgDelta());
            //   no_signal = (dg.mssince > Home.stale_data_millis());
        } else {
            Log.v(TAG, "buildDictionary: slopeOrdinal-" + slopeOrdinal + " bgReading-" + bgReadingS + //
                    " now-" + (int) now.getTime() / 1000 + " bgTime-" + (int) (this.bgReading.timestamp / 1000) + //
                    " phoneTime-" + (int) (new Date().getTime() / 1000) + " getBgDelta-" + getBgDelta());
            //   no_signal = ((new Date().getTime()) - Home.stale_data_millis() - this.bgReading.timestamp > 0);
        }

        dictionary.addString(ICON_KEY, slopeOrdinal);
        dictionary.addString(BG_KEY, bgReadingS);

        if (use_best_glucose) {
            dictionary.addUint32(RECORD_TIME_KEY, (int) (((dg.timestamp + offsetFromUTC) / 1000)));
        } else {
            dictionary.addUint32(RECORD_TIME_KEY, (int) (((this.bgReading.timestamp + offsetFromUTC) / 1000)));
        }

        dictionary.addUint32(PHONE_TIME_KEY, (int) ((new Date().getTime() + offsetFromUTC) / 1000));
        dictionary.addString(BG_DELTA_KEY, bgDelta);

        addBatteryStatusToDictionary(dictionary);

        return dictionary;
    }


    public void sendData() {
        if (use_best_glucose) {
            this.dg = BestGlucose.getDisplayGlucose();
        } else {
            this.bgReading = BgReading.last();
        }

        if (use_best_glucose ? (this.dg != null) : (this.bgReading != null)) {
            sendDownload();
        }
    }


    public String getBgDelta() {
        return this.bgGraphBuilder.unitizedDeltaString(false, false);
    }


    public void sendDownload() {
        PebbleDictionary dictionary = buildDictionary();

        if (dictionary != null && this.context != null) {
            Log.d(TAG, "sendDownload: Sending data to pebble");
            sendDataToPebble(dictionary);
            last_time_seen = JoH.ts();
        }
    }


    private void watchdog() {
        if (last_time_seen == 0) return;
        if ((JoH.ts() - last_time_seen) > 1200000) {
            Intent i = new Intent("com.tasker.jamorham.PEBBLE_JAM");
            if (this.context != null)
                this.context.sendBroadcast(i);

            Log.i(TAG, "Sending pebble fixup");
            last_time_seen = JoH.ts();
        }
    }


}

