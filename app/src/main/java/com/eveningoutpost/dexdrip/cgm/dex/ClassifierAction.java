package com.eveningoutpost.dexdrip.cgm.dex;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;

import com.eveningoutpost.dexdrip.g5model.BackFillRxMessage;
import com.eveningoutpost.dexdrip.g5model.BackFillStream;
import com.eveningoutpost.dexdrip.g5model.BaseGlucoseRxMessage;
import com.eveningoutpost.dexdrip.g5model.DexTimeKeeper;
import com.eveningoutpost.dexdrip.g5model.GlucoseRxMessage;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.cgm.dex.g7.BackfillControlRx;
import com.eveningoutpost.dexdrip.cgm.dex.g7.EGlucoseRxMessage;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import lombok.val;

/**
 * JamOrHam
 */

public class ClassifierAction {

    private static final String TAG = ClassifierAction.class.getSimpleName();
    static final String CONNECT = "connect";
    static final String BACKFILL = "backfill";
    static final String CONTROL = "control";
    static final String TXID = "UIUIUI";
    public static volatile long lastReadingTimestamp;
    static final BackFillStream stream = new BackFillStream();

    public static void action(final String type, final byte[] data) {

        if (data == null || data.length == 0) return;
        UserError.Log.d(TAG, "Type: " + type + " hex: " + JoH.bytesToHex(data));
        switch (type) {

            case CONNECT:
                UserError.Log.d(TAG, "Connect");
                stream.reset();
                break;

            case BACKFILL:
                stream.pushNew(data);
                UserError.Log.d(TAG, "Added backfill cache: " + JoH.bytesToHex(data));
                break;

            case CONTROL:
                val g7EGlucose = new EGlucoseRxMessage(data);
                if (g7EGlucose.isValid()) {
                    DexTimeKeeper.updateAge(TXID, (int) g7EGlucose.clock);
                    UserError.Log.d(TAG, "Got valid glucose: " + g7EGlucose);
                    if (g7EGlucose.usable()) {
                        lastReadingTimestamp = g7EGlucose.timestamp;
                        if (BgReading.getForPreciseTimestamp(g7EGlucose.timestamp, DexCollectionType.getCurrentDeduplicationPeriod(), false) == null) {
                            final BgReading bgReading = BgReading.bgReadingInsertFromG5(g7EGlucose.glucose, g7EGlucose.timestamp);
                            if (bgReading != null) {
                                try {
                                    bgReading.calculated_value_slope = g7EGlucose.getTrend() / Constants.MINUTE_IN_MS; // note this is different to the typical calculated slope, (normally delta)
                                    if (bgReading.calculated_value_slope == Double.NaN) {
                                        bgReading.hide_slope = true;
                                    }
                                } catch (Exception e) {
                                    // not a good number - does this exception ever actually fire?
                                }
                                bgReading.source_info = "G7 Native";
                                bgReading.noRawWillBeAvailable();
                            }
                        } else {
                            UserError.Log.d(TAG, "Reading already present for this time period");
                        }
                    } else {
                        UserError.Log.d(TAG, "Glucose value is not usable");
                    }

                    break;
                } else {
                    val bfc1 = new BackFillRxMessage(data);
                    val bfc2 = new BackfillControlRx(data);
                    if (bfc1.isValid() || bfc2.isValid()) {
                        Inevitable.task("Process G6/G7 backfill", 3000, ClassifierAction::processBackfill);
                    } else {
                        BaseGlucoseRxMessage glucose = new GlucoseRxMessage(data);
                        if (!glucose.usable()) {
                            glucose = new com.eveningoutpost.dexdrip.g5model.EGlucoseRxMessage(data);
                        }
                        if (glucose.usable()) {
                            UserError.Log.d(TAG, "Updating age from timestamp: " + glucose.timestamp);
                            DexTimeKeeper.updateAge(TXID, glucose.timestamp);
                            val ts = DexTimeKeeper.fromDexTime(TXID, glucose.timestamp);
                            lastReadingTimestamp = ts;
                            if (BgReading.getForPreciseTimestamp(ts, DexCollectionType.getCurrentDeduplicationPeriod(), false) == null) {
                                final BgReading bgReading = BgReading.bgReadingInsertFromG5(glucose.glucose, ts);
                                if (bgReading != null) {
                                    try {
                                        bgReading.calculated_value_slope = glucose.getTrend() / Constants.MINUTE_IN_MS; // note this is different to the typical calculated slope, (normally delta)
                                        if (bgReading.calculated_value_slope == Double.NaN) {
                                            bgReading.hide_slope = true;
                                        }
                                    } catch (Exception e) {
                                        // not a good number - does this exception ever actually fire?
                                    }
                                    bgReading.noRawWillBeAvailable();
                                }
                            } else {
                                UserError.Log.d(TAG, "Reading already present for this time period");
                            }
                        }
                    }
                }
        }
    }

    private static void processBackfill() {
        UserError.Log.d(TAG, "Processing backfill");
        val decoded = stream.decode();
        stream.reset();
        for (BackFillStream.Backsie backsie : decoded) {
            UserError.Log.d(TAG, "Backsie: " + backsie.getDextime());
            val time = DexTimeKeeper.fromDexTime(TXID, backsie.getDextime());
            val since = JoH.msSince(time);
            if ((since > HOUR_IN_MS * 12) || (since < 0)) {
                UserError.Log.wtf(TAG, "Backfill timestamp unrealistic: " + JoH.dateTimeText(time) + " (ignored)");
            } else {
                if (BgReading.getForPreciseTimestamp(time, DexCollectionType.getCurrentDeduplicationPeriod()) == null) {
                    final BgReading bgr = BgReading.bgReadingInsertFromG5(backsie.getGlucose(), time, "Backfill");
                    UserError.Log.d(TAG, "Adding backfilled reading: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
                }
                UserError.Log.d(TAG, "Backsie: " + JoH.dateTimeText(time) + " " + BgGraphBuilder.unitized_string_static(backsie.getGlucose()));
            }
        }
    }

}
