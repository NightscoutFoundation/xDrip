package com.eveningoutpost.dexdrip.g5model;


import android.text.SpannableString;

import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight;
import com.eveningoutpost.dexdrip.ui.helpers.Span;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;

import lombok.Getter;
import lombok.val;

import static com.eveningoutpost.dexdrip.g5model.FirmwareCapability.isDeviceAlt2;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.getFirmwareXDetails;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.services.G5BaseService.usingG6;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.getTransmitterID;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.usingNativeMode;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.None;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getBestCollectorHardwareName;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getDexCollectionType;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.hasDexcomRaw;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.hasLibre;

// jamorham

// helper class to deal with sensor expiry and warmup time

public class SensorDays {

    private static final String TAG = "SensorDays";

    private static final long UNKNOWN = -1;
    private static final int USE_DEXCOM_STRATEGY = 5;
    private static final int USE_LIBRE_STRATEGY = 6;

    private static final long CAL_THRESHOLD1 = DAY_IN_MS * 4;
    private static final long CAL_THRESHOLD2 = Constants.HOUR_IN_MS * 18;

    private static final HashMap<String, SensorDays> cache = new HashMap<>();

    @Getter
    private long period = UNKNOWN;
    @Getter
    private long warmupMs = 2 * HOUR_IN_MS;
    private long created = 0;
    private int strategy = 0;

    // load current config and compute
    public static SensorDays get() {
        val type = getDexCollectionType();
        val tx_id = getTransmitterID();
        return get(type, tx_id);
    }

    // compute based on type
    public static SensorDays get(DexCollectionType type, final String tx_id) {

        if (type == null) type = None;  // obscure workaround

        // get cached result
        val result = cache.get(type + tx_id);
        if (result != null && result.cacheValid()) return result;

        val ths = new SensorDays();

        if (hasLibre(type)) {
            String libreVersion = PersistentStore.getString("LibreVersion");
            ths.period = DAY_IN_MS * 14; // TODO 10 day sensors?
            if (libreVersion.equals("3")) {
                ths.period = DAY_IN_MS * 15;
            }
            ths.strategy = USE_LIBRE_STRATEGY;
            ths.warmupMs = HOUR_IN_MS;

        } else if (hasDexcomRaw(type)) {
            ths.strategy = USE_DEXCOM_STRATEGY;
            val vr2 = (VersionRequest2RxMessage)
                    getFirmwareXDetails(tx_id, 2);
            if (vr2 != null) {
                ths.period = DAY_IN_MS * vr2.typicalSensorDays;
            } else {
                if (usingG6()) {
                    ths.period = DAY_IN_MS * 10; // G6 default
                } else {
                    ths.period = DAY_IN_MS * 7; // G5
                }
            }
            val vr3 = (VersionRequest2RxMessage) getFirmwareXDetails(tx_id, 3);
            if (vr3 != null) {
                ths.warmupMs = Math.min(Constants.SECOND_IN_MS * vr3.warmupSeconds, 2 * HOUR_IN_MS);
            } else {
               ths.warmupMs = 2 * HOUR_IN_MS;
            }

            if (getBestCollectorHardwareName().equals("G7")) {
                ths.period = DAY_IN_MS * 10 + HOUR_IN_MS * 12; // The device lasts 10.5 days.
                ths.warmupMs = 30 * MINUTE_IN_MS; // The warmup time is 30 minutes.
            }

            if (isDeviceAlt2(getTransmitterID())) {
                ths.period = DAY_IN_MS * 15 + HOUR_IN_MS * 12;
            }

        } else {
            // unknown type
        }
        ths.created = tsl();
        cache.put(type + tx_id, ths);
        return ths;
    }

    public static void clearCache() {
        cache.clear();
    }

    private long getDexcomStart() {
        if (usingNativeMode()) {
            return DexSessionKeeper.getStart();
        } else {
            try {
                // In non-native mode the expiration is a guide only
                return Sensor.currentSensor().started_at;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    private static long getLibreAgeMs() {
        return Pref.getInt("nfc_sensor_age", -50000) * MINUTE_IN_MS;
    }

    private long getLibreStart() {
        try {
            val age_ms = getLibreAgeMs();
            if (age_ms > 0) {
                return tsl() - age_ms;
            } else {
                return Sensor.currentSensor().started_at;
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public long getStart() {
        switch (strategy) {
            case USE_DEXCOM_STRATEGY:
                return getDexcomStart();
            case USE_LIBRE_STRATEGY:
                return getLibreStart();
            default:
                return 0; // very large error default will be caught by sanity check
        }
    }

    private boolean isStarted() {
        return getStart() > 0;
    }

    // returns 0 if invalid
    public long getRemainingSensorPeriodInMs() {
        //UserError.Log.d(TAG, "Get start debug returns: " + JoH.dateTimeText(getStart()));
        if (isValid()) {
            val elapsed = msSince(getStart());
            long remaining = period - elapsed;
            // sanity check
            if ((remaining < 0) || (remaining > period)) {
                remaining = 0;
            }
            return remaining;
        } else {
            return 0;
        }
    }

    long getSensorEndTimestamp() {
        if (isValid()) {
            return getStart() + period;
        } else {
            return 0;
        }
    }

    // Add resolution / update cache
    public SpannableString getSpannable() {

        val expiryMs = getRemainingSensorPeriodInMs();

        if (expiryMs > 0) {
            if (expiryMs > CAL_THRESHOLD1) {
                val fmt = xdrip.gs(R.string.expires_days);
                return new SpannableString(MessageFormat.format(fmt, roundDouble((double) expiryMs / DAY_IN_MS, 1)));
            } else {
                // expiring soon
                val niceTime = new SimpleDateFormat(expiryMs < CAL_THRESHOLD2 ? "h:mm a" : "EEE, h:mm a", Locale.getDefault()).format(getSensorEndTimestamp());
                return Span.colorSpan(MessageFormat.format(xdrip.gs(R.string.expires_at), niceTime), expiryMs < CAL_THRESHOLD2 ? Highlight.BAD.color() : Highlight.NOTICE.color());
            }
        }
        return new SpannableString("");
    }

    public boolean isValid() {
        return isKnown() && isSessionLive() && isStarted();
    }

    private boolean isSessionLive() {
        return Sensor.isActive();
    }

    boolean isKnown() {
        return period != UNKNOWN;
    }

    boolean cacheValid() {
        return msSince(created) < MINUTE_IN_MS * 10;
    }

    void invalidateCache() {
        created = -1;
    }

}
