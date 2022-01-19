package com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserNotification;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.GraphLine;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchSettings;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.Map;

import lecho.lib.hellocharts.model.Line;

public class WatchBroadcastService extends Service {
    public static final String PREF_ENABLED = "watch_broadcast_enabled";

    public static final String BG_ALERT_TYPE = "BG_ALERT_TYPE";

    protected static final String INTENT_FUNCTION_KEY = "FUNCTION";
    protected static final String INTENT_PACKAGE_KEY = "PACKAGE";
    protected static final String INTENT_REPLY_MSG = "REPLY_MSG";
    protected static final String INTENT_SETTINGS = "SETTINGS";
    protected static final String INTENT_ALERT_TYPE = "ALERT_TYPE";

    protected static final String CMD_SET_SETTINGS = "set_settings";
    protected static final String CMD_UPDATE_BG_FORCE = "update_bg_force";
    protected static final String CMD_ALERT = "alarm";
    protected static final String CMD_SNOOZE_ALERT = "snooze_alarm";
    protected static final String CMD_ADD_STEPS = "add_steps";
    protected static final String CMD_ADD_HR = "add_hrs";
    protected static final String CMD_ADD_TREATMENT = "add_treatment";
    protected static final String CMD_START = "start";
    protected static final String CMD_UPDATE_BG = "update_bg";
    protected static final String CMD_REPLY_MSG = "reply_msg";
    //listen
    protected static final String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_RECEIVER";
    //send
    protected static final String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_SENDER";
    private static final int COMMADS_LIMIT_TIME = 1;

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key.equals(PREF_ENABLED)) {
            JoH.startService(WatchBroadcastService.class);
        }
    };

    protected String TAG = this.getClass().getSimpleName();
    protected Map<String, WatchSettings> broadcastEntities;
    private String statusIOB = "";
    private BroadcastReceiver statusReceiver;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action == null || !action.equals(ACTION_WATCH_COMMUNICATION_RECEIVER)) return;
                PowerManager.WakeLock wl = JoH.getWakeLock(TAG, 2000);
                String packageKey = intent.getStringExtra(INTENT_PACKAGE_KEY);
                String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                UserError.Log.d(TAG, String.format("received broadcast: function:%s, packageKey: %s", function, packageKey));

                boolean startService = false;
                long timeStamp;
                WatchSettings watchSettings;
                Intent serviceIntent = new Intent(xdrip.getAppContext(), WatchBroadcastService.class);

                if (CMD_SET_SETTINGS.equals(function) || CMD_UPDATE_BG_FORCE.equals(function)) {
                    if (packageKey == null) {
                        function = CMD_REPLY_MSG;
                        serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, \"PACKAGE\" extra not specified");
                        startService = true;
                    }
                } else {
                    if (!broadcastEntities.containsKey(packageKey)) {
                        function = CMD_REPLY_MSG;
                        serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, the app should be registered at first");
                        startService = true;
                    }
                }
                if (!startService && !JoH.pratelimit(function + "_" + packageKey, COMMADS_LIMIT_TIME)) {
                    switch (function) {
                        case CMD_SET_SETTINGS:
                            watchSettings = intent.getParcelableExtra(INTENT_SETTINGS);
                            if (watchSettings == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, can't parse WatchSettings");
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, intent.getParcelableExtra(INTENT_SETTINGS));
                            break;
                        case CMD_UPDATE_BG_FORCE:
                            watchSettings = intent.getParcelableExtra(INTENT_SETTINGS);
                            if (watchSettings == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, can't parse WatchSettings");
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, intent.getParcelableExtra(INTENT_SETTINGS));
                            //update immediately
                            startService = true;
                            break;
                        case CMD_SNOOZE_ALERT:
                            String activeAlertType = intent.getStringExtra(INTENT_ALERT_TYPE);
                            if (activeAlertType == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, \"ALERT_TYPE\" not specified ");
                                startService = true;
                                break;
                            }
                            serviceIntent.putExtra(INTENT_ALERT_TYPE, activeAlertType);
                            startService = true;
                            break;
                        case CMD_ADD_STEPS:
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            int steps = intent.getIntExtra("value", 0);
                            StepCounter.createEfficientRecord(timeStamp, steps);
                            break;
                        case CMD_ADD_HR:
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            int hrValue = intent.getIntExtra("value", 0);
                            HeartRate.create(timeStamp, hrValue, 1);
                            break;
                        case CMD_ADD_TREATMENT: //so it woudl be possible to add treatment via watch
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            double carbs = intent.getDoubleExtra("carbs", 0);
                            double insulin = intent.getDoubleExtra("insulin", 0);
                            Treatments.create(carbs, insulin, timeStamp);
                            break;
                    }
                }
                if (startService) {
                    serviceIntent.putExtra(INTENT_FUNCTION_KEY, function);
                    serviceIntent.putExtra(INTENT_PACKAGE_KEY, packageKey);
                    xdrip.getAppContext().startService(serviceIntent);
                    return;
                }
                JoH.releaseWakeLock(wl);
            } catch (Exception e) {
                UserError.Log.e(TAG, "broadcast onReceive Error: " + e.toString());
            }
        }
    };

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(PREF_ENABLED);
    }

    public static void sendLatestBG() {
        if (isEnabled()) {
            JoH.startService(WatchBroadcastService.class, INTENT_FUNCTION_KEY, CMD_UPDATE_BG);
        }
    }

    public static void initialStartIfEnabled() {
        if (isEnabled()) {
            Inevitable.task("watch-broadcast-initial-start", 500, () -> JoH.startService(WatchBroadcastService.class));
        }
    }

    public static void sendAlert(String type, String message) {
        if (isEnabled()) {
            Inevitable.task("watch-broadcast-send-alert", 100, () -> JoH.startService(WatchBroadcastService.class,
                    INTENT_FUNCTION_KEY, CMD_ALERT,
                    "message", message,
                    "type", type));
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

        statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String iob = intent.getStringExtra("iob");
                if (iob != null) {
                    statusIOB = iob;
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(statusReceiver,
                new IntentFilter(Intents.HOME_STATUS_ACTION));

        JoH.startService(WatchBroadcastService.class, INTENT_FUNCTION_KEY, CMD_START);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        UserError.Log.e(TAG, "killing service");
        broadcastEntities.clear();
        unregisterReceiver(broadcastReceiver);
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception unregistering broadcast receiver: " + e);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG, 10000);
        try {
            if (isEnabled()) {
                if (intent != null) {
                    final String function = intent.getStringExtra(INTENT_FUNCTION_KEY);
                    if (function != null) {
                        try {
                            handleCommand(function, intent);
                        } catch (Exception e) {
                            UserError.Log.e(TAG, "handleCommand Error: " + e.toString());
                        }
                    } else {
                        UserError.Log.d(TAG, "onStartCommand called without function");
                    }
                }
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleCommand(String function, Intent intent) {
        UserError.Log.d(TAG, "handleCommand function:" + function);
        String receiver = null;
        boolean handled = false;
        String replyMsg = "";
        //send to all connected apps
        for (Map.Entry<String, WatchSettings> entry : broadcastEntities.entrySet()) {
            receiver = entry.getKey();
            WatchSettings settings = entry.getValue();
            switch (function) {
                case CMD_UPDATE_BG:
                    Bundle bundle = new Bundle();
                    bundle = prepareBundleBG(settings, bundle);
                    sendBroadcast(function, receiver, bundle, replyMsg);
                    break;
            }
        }
        if (function.equals(CMD_UPDATE_BG)) {
            handled = true;
        }

        if (handled) {
            return;
        }

        Bundle bundle = null;
        switch (function) {
            case CMD_REPLY_MSG:
                replyMsg = intent.getStringExtra(INTENT_REPLY_MSG);
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                break;
            case CMD_ALERT:
                bundle = new Bundle();
                bundle.putString("type", intent.getStringExtra("type"));
                bundle.putString("message", intent.getStringExtra("message"));
                break;
            case CMD_START:
                break;
            case CMD_UPDATE_BG_FORCE:
                bundle = new Bundle();
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                WatchSettings settings = broadcastEntities.get(receiver);
                bundle = prepareBundleBG(settings, bundle);
                break;
            case CMD_SNOOZE_ALERT:
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                String alertName = "";
                int snoozeMinutes = 0;
                double nextAlertAt = JoH.ts();
                String activeAlertType = intent.getStringExtra(INTENT_ALERT_TYPE);
                replyMsg = "Snooze accepted";
                if (activeAlertType.equals(BG_ALERT_TYPE)) {
                    if (ActiveBgAlert.currentlyAlerting()) {
                        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                        if (activeBgAlert == null) {
                            replyMsg = "Error: snooze was called but no alert is active";
                        } else {
                            AlertType alert = ActiveBgAlert.alertTypegetOnly();
                            if (alert != null) {
                                alertName = alert.name;
                                snoozeMinutes = alert.default_snooze;
                            }
                            AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1, true);
                            nextAlertAt = activeBgAlert.next_alert_at;
                        }
                    } else {
                        replyMsg = "Error: No Alarms found to snooze";
                    }
                } else {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    snoozeMinutes = (int) MissedReadingService.getOtherAlertSnoozeMinutes(prefs, activeAlertType);
                    UserNotification.snoozeAlert(activeAlertType, snoozeMinutes);
                    UserNotification userNotification = UserNotification.GetNotificationByType(activeAlertType);
                    if (userNotification != null) {
                        nextAlertAt = userNotification.timestamp;
                    }
                    alertName = activeAlertType;
                }
                bundle = new Bundle();
                bundle.putString("alertName", alertName);
                bundle.putInt("snoozeMinutes", snoozeMinutes);
                bundle.putDouble("nextAlertAt", nextAlertAt);
                break;
            default:
                return;
        }
        sendBroadcast(function, receiver, bundle, replyMsg);
    }

    public void sendBroadcast(String function, String receiver, Bundle bundle, String replyMsg) {
        Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_SENDER);
        UserError.Log.d(TAG, String.format("sendBroadcast functionName:%s, receiver: %s", function, receiver));

        if (function == null || function.isEmpty()) {
            UserError.Log.d(TAG, "error, function not specified");
            return;
        }

        intent.putExtra(INTENT_FUNCTION_KEY, function);
        if (receiver != null && !receiver.isEmpty()) {
            intent.setPackage(receiver);
        }
        if (!replyMsg.isEmpty()) {
            intent.putExtra(INTENT_REPLY_MSG, replyMsg);
            UserError.Log.d(TAG, "replyMsg: " + replyMsg);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }

    public Bundle prepareBundleBG(WatchSettings settings, Bundle bundle) {
        if (settings == null) return null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());

        bundle.putBoolean("doMgdl", (prefs.getString("units", "mgdl").equals("mgdl")));
        bundle.putInt("phoneBattery", PowerStateReceiver.getBatteryLevel(xdrip.getAppContext()));

        BestGlucose.DisplayGlucose dg = BestGlucose.getDisplayGlucose();
        BgReading bgReading = BgReading.last();
        if (dg != null || bgReading != null) {
            String deltaName;
            double bgValue;
            boolean isBgHigh = false;
            boolean isBgLow = false;
            boolean isStale;
            long timeStamp;
            String plugin = "";
            double deltaValue = 0;
            if (dg != null) {
                deltaName = dg.delta_name;
                //fill bg
                bgValue = dg.mgdl;
                isStale = dg.isStale();
                isBgHigh = dg.isHigh();
                isBgLow = dg.isLow();
                timeStamp = dg.timestamp;
                plugin = dg.plugin_name;
                deltaValue = dg.delta_mgdl;
            } else {
                deltaName = bgReading.getDg_deltaName();
                bgValue = bgReading.getDg_mgdl();
                isStale = bgReading.isStale();
                timeStamp = bgReading.getEpochTimestamp();
            }
            bundle.putString("bg.deltaName", deltaName);
            bundle.putDouble("bg.valueMgdl", bgValue);
            bundle.putBoolean("bg.isHigh", isBgHigh);
            bundle.putBoolean("bg.isLow", isBgLow);
            bundle.putLong("bg.timeStamp", timeStamp);
            bundle.putBoolean("bg.isStale", isStale);
            bundle.putString("bg.plugin", plugin);
            bundle.putDouble("bg.deltaValueMgdl", deltaValue);

            bundle.putString("iob", statusIOB);

            Treatments treatment = Treatments.last();
            if (treatment != null && treatment.hasContent() && !treatment.noteOnly()) {
                if (treatment.insulin > 0) {
                    bundle.putDouble("treatment.insulin", treatment.insulin);
                }
                if (treatment.carbs > 0) {
                    bundle.putDouble("treatment.carbs", treatment.carbs);
                }
                bundle.putLong("treatment.timeStamp", treatment.timestamp);
            }

            if (settings.isDisplayGraph()) {
                long start = settings.getGraphStart();
                long end = settings.getGraphEnd();
                if (end == 0) {
                    end = JoH.tsl();
                }
                if (start == 0) {
                    start = JoH.tsl() - Constants.HOUR_IN_MS * 2;
                }
                if (start > end) {
                    long temp = end;
                    end = start;
                    start = temp;
                }
                bundle.putInt("fuzzer", Pref.getBoolean("lower_fuzzer", false) ? 500 * 15 * 5 : 1000 * 30 * 5); // 37.5 seconds : 2.5 minutes
                bundle.putLong("start", start);
                bundle.putLong("end", end);
                bundle.putDouble("highMark", JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170));
                bundle.putDouble("lowMark", JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70));

                BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(xdrip.getAppContext(), start, end);
                bgGraphBuilder.defaultLines(true); // simple mode

                bundle.putParcelable("graph.lowLine", new GraphLine(bgGraphBuilder.lowLine()));
                bundle.putParcelable("graph.highLine", new GraphLine(bgGraphBuilder.highLine()));
                bundle.putParcelable("graph.inRange", new GraphLine(bgGraphBuilder.inRangeValuesLine()));
                bundle.putParcelable("graph.low", new GraphLine(bgGraphBuilder.lowValuesLine()));
                bundle.putParcelable("graph.high", new GraphLine(bgGraphBuilder.highValuesLine()));

                Line[] treatments = bgGraphBuilder.treatmentValuesLine();

                bundle.putParcelable("graph.iob", new GraphLine(treatments[2])); //insulin on board
                bundle.putParcelable("graph.treatment", new GraphLine(treatments[1])); //treatmentValues

                bundle.putParcelable("graph.predictedBg", new GraphLine(treatments[5]));  // predictive
                bundle.putParcelable("graph.cob", new GraphLine(treatments[6]));  //cobValues
                bundle.putParcelable("graph.polyBg", new GraphLine(treatments[7]));  //poly predict ;
            }
        }
        return bundle;
    }
}
