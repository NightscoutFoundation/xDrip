package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.Color;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchSettings;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.X;
import static com.eveningoutpost.dexdrip.UtilityModels.ColorCache.getCol;

public class WatchBroadcastService extends Service {
    protected static final int NUM_VALUES = (60 / 5) * 24;
    protected static final String INTENT_ACTION_KEY = "ACTION";
    protected static final String INTENT_DEVICE_PACKAGE_KEY = "PACKAGE";
    protected static final String INTENT_SETTINGS = "SETTINGS";
    protected static final String CMD_SET_SETTINGS = "set_settings";
    protected static final String CMD_UPDATE_BG_FORCE = "update_bg_force";
    protected static final String CMD_SNOOZE_ALARM = "snooze_alarm";
    protected String TAG = this.getClass().getSimpleName();
    protected Map<String, WatchSettings> broadcastEntities;

    //listen
    String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_RECEIVER";
    //send
    String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_SENDER";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            UserError.Log.e(TAG, "got settings intent " + action);
            if (action != null && action.equals(ACTION_WATCH_COMMUNICATION_RECEIVER)) {
                String packageKey = intent.getStringExtra(INTENT_DEVICE_PACKAGE_KEY);
                String request = intent.getStringExtra(INTENT_ACTION_KEY);
                switch (request) {
                    case CMD_SET_SETTINGS:
                        broadcastEntities.put(packageKey, intent.getParcelableExtra(INTENT_SETTINGS));
                        break;
                    case CMD_UPDATE_BG_FORCE:
                        broadcastEntities.put(packageKey, intent.getParcelableExtra(INTENT_SETTINGS));
                        //update immediately
                        forceUpdateBG(packageKey);
                        break;
                    case CMD_SNOOZE_ALARM:
                        break;
                }
            }
        }
    };

    public static void forceUpdateBG(final String packageKey) {
        if (WatchBroadcastEntry.isEnabled()) {
            if (JoH.ratelimit("watch-broadcast-update-bg-force", 5)) {
                JoH.startService(WatchBroadcastService.class, "function", CMD_UPDATE_BG_FORCE, INTENT_DEVICE_PACKAGE_KEY, packageKey);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        UserError.Log.e(TAG, "starting service");
        broadcastEntities = new HashMap<>();
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_WATCH_COMMUNICATION_RECEIVER));
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "killing service");
        broadcastEntities.clear();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (WatchBroadcastEntry.isEnabled()) {
            if (intent != null) {
                final String function = intent.getStringExtra("function");
                if (function != null) {
                    handleCommand(function, intent);
                } else {
                    // no specific function
                }
            }
            return START_STICKY;
        } else {
            UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void handleCommand(String functionName, Intent intentIn) {
        UserError.Log.d(TAG, "handleCommand function:" + functionName);
        boolean exit = false;
        String receiver = "BROADCAST";
        for (Map.Entry<String, WatchSettings> entry : broadcastEntities.entrySet()) {
            String key = entry.getKey();
            WatchSettings value = entry.getValue();
        }

        do {
            Bundle bundle = new Bundle();
            switch (functionName) {
                case "refresh":
                    break;
                case "message":
                    break;
                case "alarm":
                    break;
                case "update_bg":
                    bundle = prepareBundle(1, JoH.tsl(), bundle);
                    break;
                case CMD_UPDATE_BG_FORCE:
                    receiver = intentIn.getStringExtra(INTENT_DEVICE_PACKAGE_KEY);
                    WatchSettings settings = broadcastEntities.get(receiver);
                    bundle = prepareBundle(settings.getGraphSince(), JoH.tsl(), bundle);
                    exit = true;
                    break;
            }
            Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_SENDER);
            intent.putExtra(INTENT_ACTION_KEY, functionName);
            intent.putExtra(INTENT_DEVICE_PACKAGE_KEY, receiver);
            intent.putExtras(bundle);
            intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            xdrip.getAppContext().sendBroadcast(intent);
        } while (!exit);
    }

    public Bundle prepareBundle(long start, long end, Bundle bundle) {
        if (start > end) {
            long temp = end;
            end = start;
            start = temp;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());

        bundle.putInt("fuzzer", Pref.getBoolean("lower_fuzzer", false) ? 500 * 15 * 5 : 1000 * 30 * 5); // 37.5 seconds : 2.5 minutes
        bundle.putLong("start", start);
        bundle.putLong("end", end);
        bundle.putDouble("highMark", JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170));
        bundle.putDouble("lowMark", JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70));
        bundle.putBoolean("doMgdl", (prefs.getString("units", "mgdl").equals("mgdl")));

        ArrayList<Color> colors = new ArrayList<>();
        for (X color : X.values()) {
            colors.add(new Color(color.name(), getCol(color)));
        }
        bundle.putParcelableArrayList("colors", colors);

        List<BgReading> bgReadings = BgReading.latestForGraph(NUM_VALUES, start, end);
        List<Treatments> treatments = Treatments.latestForGraph(NUM_VALUES, start, end + (120 * 60 * 1000));

        return bundle;
    }
}
