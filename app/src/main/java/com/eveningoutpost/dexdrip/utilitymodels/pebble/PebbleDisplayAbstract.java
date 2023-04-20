package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.ParakeetHelper;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.store.KeyStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.Preferences;
import com.eveningoutpost.dexdrip.xdrip;
import com.getpebble.android.kit.PebbleKit;
import com.getpebble.android.kit.util.PebbleDictionary;

import java.util.UUID;

/**
 * Created by andy on 02/06/16.
 */
public abstract class PebbleDisplayAbstract implements PebbleDisplayInterface {

    protected static final int ICON_KEY = 0;
    protected static final int BG_KEY = 1;
    protected static final int RECORD_TIME_KEY = 2;
    protected static final int PHONE_TIME_KEY = 3;
    protected static final int BG_DELTA_KEY = 4;
    protected static final int UPLOADER_BATTERY_KEY = 5;
    protected static final int NAME_KEY = 6;
    protected static final int TREND_BEGIN_KEY = 7;
    protected static final int TREND_DATA_KEY = 8;
    protected static final int TREND_END_KEY = 9;
    protected static final int MESSAGE_KEY = 10;
    protected static final int VIBE_KEY = 11;

    protected static final int TBR_KEY = 12;
    protected static final int IOB_KEY = 13;

    protected static final int NO_BLUETOOTH_KEY = 111;
    protected static final int COLLECT_HEALTH_KEY = 112;

    protected static final int SYNC_KEY = 1000;
    protected static final int PLATFORM_KEY = 1001;
    protected static final int VERSION_KEY = 1002;


    protected static final int MAX_VALUES =60*24;

    protected Context context;
    protected BgGraphBuilder bgGraphBuilder;
    protected BgReading bgReading;
    protected PebbleWatchSync pebbleWatchSync;

    protected static final boolean use_best_glucose = true;
    protected BestGlucose.DisplayGlucose dg;

    protected long last_seen_timestamp = 0;

    protected static final String PEBBLE_BWP_SYMBOL = "😐";

    protected KeyStore keyStore = FastStore.getInstance();

    public void receiveNack(int transactionId) {
        // default no implementation
    }

    public void receiveAck(int transactionId) {
        // default no implementation
    }

    public void receiveAppData(int transactionId, PebbleDictionary data) {
        // handle app incoming data
        PebbleWatchSync.receiveAppData(transactionId, data);
    }

    public UUID watchfaceUUID()
    {
        return PebbleWatchSync.PEBBLEAPP_UUID;
    }

    @Override
    public void initDisplay(Context context, PebbleWatchSync pebbleWatchSync, BgGraphBuilder bgGraphBuilder) {
        this.context = context;
        this.bgGraphBuilder = bgGraphBuilder;
        this.pebbleWatchSync = pebbleWatchSync;
    }


    public int getBatteryLevel() {
        Intent batteryIntent = this.context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if (level == -1 || scale == -1) {
            return 50;
        }
        return (int) (((float) level / (float) scale) * 100.0f);
    }

    synchronized void pebble_watchdog(boolean online, String tag) {
        if (online) {
            last_seen_timestamp = JoH.tsl();
        } else {
            if (last_seen_timestamp == 0) return;
            if ((JoH.msSince(last_seen_timestamp) > 20 * Constants.MINUTE_IN_MS)) {
                if (!JoH.isOngoingCall()) {
                    last_seen_timestamp = JoH.tsl();
                    if (Pref.getBooleanDefaultFalse("bluetooth_watchdog")) {
                        UserError.Log.e(tag, "Triggering pebble watchdog reset!");
                        JoH.restartBluetooth(xdrip.getAppContext());
                    } else {
                        UserError.Log.e(tag, "Would have Triggered pebble watchdog reset but bluetooth watchdog is disabled");
                    }
                } else {
                    UserError.Log.d(tag, "Ongoing call blocking pebble watchdog reset");
                }
            }
        }
    }


    public String getBatteryString(String key) {
        return String.format("%d", PreferenceManager.getDefaultSharedPreferences(this.context).getInt(key, 0));
    }


    public String getSlopeOrdinal() {
        if ((use_best_glucose && dg == null) || (!use_best_glucose && this.bgReading == null))
            return "0";

        final String arrow_name = (use_best_glucose ? dg.delta_name : this.bgReading.slopeName());
        if (arrow_name.equalsIgnoreCase("DoubleDown"))
            return "7";

        if (arrow_name.equalsIgnoreCase("SingleDown"))
            return "6";

        if (arrow_name.equalsIgnoreCase("FortyFiveDown"))
            return "5";

        if (arrow_name.equalsIgnoreCase("Flat"))
            return "4";

        if (arrow_name.equalsIgnoreCase("FortyFiveUp"))
            return "3";

        if (arrow_name.equalsIgnoreCase("SingleUp"))
            return "2";

        if (arrow_name.equalsIgnoreCase("DoubleUp"))
            return "1";

        if (arrow_name.equalsIgnoreCase("9"))
            return arrow_name;

        return "0";
    }


    public String getPhoneBatteryStatus() {
        return String.valueOf(getBatteryLevel());
    }


    public DexCollectionType getDexCollectionType() {
        return DexCollectionType.getType(PreferenceManager.getDefaultSharedPreferences(this.context).getString("dex_collection_method", "DexbridgeWixel"));
    }


    public boolean isDexBridgeWixel() {
        return getDexCollectionType() == DexCollectionType.DexbridgeWixel;
    }

    public boolean doWeDisplayWixelBatteryStatus() {
        DexCollectionType dexCollectionType = getDexCollectionType();

        return ((dexCollectionType == DexCollectionType.DexbridgeWixel || //
                (dexCollectionType == DexCollectionType.WifiWixel && ParakeetHelper.isRealParakeetDevice())) &&
                getBooleanValue("display_bridge_battery", true));
    }

    public boolean getBooleanValue(String key) {
        return getBooleanValue(key, false);
    }


    public boolean getBooleanValue(String key, boolean defaultValue) {
        return Preferences.getBooleanPreferenceViaContextWithoutException(this.context, key, defaultValue);
    }


    public void sendDataToPebble(final PebbleDictionary data) {
        synchronized (data) {
            PebbleKit.sendDataToPebble(this.context, watchfaceUUID(), data);
        }
    }


    public void addBatteryStatusToDictionary(PebbleDictionary dictionary) {
        if (doWeDisplayWixelBatteryStatus()) {

            if (isDexBridgeWixel()) {
                dictionary.addString(UPLOADER_BATTERY_KEY, getBatteryString("bridge_battery"));
                dictionary.addString(NAME_KEY, "Bridge");
            } else {
                dictionary.addString(UPLOADER_BATTERY_KEY, getBatteryString("parakeet_battery"));
                dictionary.addString(NAME_KEY, "Phone");
            }

        } else {
            dictionary.addString(UPLOADER_BATTERY_KEY, getPhoneBatteryStatus());
            dictionary.addString(NAME_KEY, "Phone");
        }

    }

    public void removeBatteryStatusFromDictionary(PebbleDictionary dictionary) {
        dictionary.remove(UPLOADER_BATTERY_KEY);
        dictionary.remove(NAME_KEY);
    }


    public String getBgReading() {
        return (use_best_glucose) ? ((dg != null) ? dg.unitized : "") : this.bgGraphBuilder.unitized_string(this.bgReading.calculated_value);
    }


}
