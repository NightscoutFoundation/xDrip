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

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.Accuracy;
import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.BloodTest;
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
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.PumpStatus;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.GraphLine;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchBroadcast;
import com.eveningoutpost.dexdrip.wearintegration.WatchBroadcast.Models.WatchSettings;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.Map;

import lecho.lib.hellocharts.model.Line;

import static com.eveningoutpost.dexdrip.UtilityModels.Constants.DAY_IN_MS;

public class WatchBroadcastService extends Service {
    public static final String PREF_ENABLED = "watch_broadcast_enabled";

    public static final String BG_ALERT_TYPE = "BG_ALERT_TYPE";

    protected static final String INTENT_FUNCTION_KEY = "FUNCTION";
    protected static final String INTENT_PACKAGE_KEY = "PACKAGE";
    protected static final String INTENT_REPLY_MSG = "REPLY_MSG";
    protected static final String INTENT_REPLY_CODE = "REPLY_CODE";
    protected static final String INTENT_SETTINGS = "SETTINGS";
    protected static final String INTENT_ALERT_TYPE = "ALERT_TYPE";
    protected static final String INTENT_STAT_HOURS = "stat_hours";

    protected static final String INTENT_REPLY_CODE_OK = "OK";
    protected static final String INTENT_REPLY_CODE_ERROR = "ERROR";
    protected static final String INTENT_REPLY_CODE_PACKAGE_ERROR = "ERROR_NO_PACKAGE";
    protected static final String INTENT_REPLY_CODE_NOT_REGISTERED = "NOT_REGISTERED";

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
    protected static final String CMD_STAT_INFO = "stat_info";
    //listen
    protected static final String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_RECEIVER";
    //send
    protected static final String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.WATCH_BROADCAST_SENDER";
    private static final int COMMADS_LIMIT_TIME = 2;

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key.equals(PREF_ENABLED)) {
            JoH.startService(WatchBroadcastService.class);
        }
    };

    protected String TAG = this.getClass().getSimpleName();
    protected Map<String, WatchBroadcast> broadcastEntities;
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
                        serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_PACKAGE_ERROR);
                        startService = true;
                    }
                } else {
                    if (!broadcastEntities.containsKey(packageKey)) {
                        function = CMD_REPLY_MSG;
                        serviceIntent.putExtra(INTENT_REPLY_MSG, "Error, the app should be registered at first");
                        serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_NOT_REGISTERED);
                        startService = true;
                    }
                }
                if (!startService && JoH.pratelimit(function + "_" + packageKey, COMMADS_LIMIT_TIME)) {
                    switch (function) {
                        case CMD_SET_SETTINGS:
                            watchSettings = intent.getParcelableExtra(INTENT_SETTINGS);
                            if (watchSettings == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "Can't parse WatchSettings");
                                serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_ERROR);
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, new WatchBroadcast(watchSettings));
                            break;
                        case CMD_UPDATE_BG_FORCE:
                            watchSettings = intent.getParcelableExtra(INTENT_SETTINGS);
                            if (watchSettings == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "Can't parse WatchSettings");
                                serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_ERROR);
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, new WatchBroadcast(watchSettings));
                            //update immediately
                            startService = true;
                            break;
                        case CMD_SNOOZE_ALERT:
                            String activeAlertType = intent.getStringExtra(INTENT_ALERT_TYPE);
                            if (activeAlertType == null) {
                                function = CMD_REPLY_MSG;
                                serviceIntent.putExtra(INTENT_REPLY_MSG, "\"ALERT_TYPE\" not specified ");
                                serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_ERROR);
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
                        case CMD_ADD_TREATMENT: //so it would be possible to add treatment via watch
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            double carbs = intent.getDoubleExtra("carbs", 0);
                            double insulin = intent.getDoubleExtra("insulin", 0);
                            Treatments.create(carbs, insulin, timeStamp);
                            function = CMD_REPLY_MSG;
                            serviceIntent.putExtra(INTENT_REPLY_MSG, "Treatment were added");
                            serviceIntent.putExtra(INTENT_REPLY_CODE, INTENT_REPLY_CODE_OK);
                            startService = true;
                            break;
                        case CMD_STAT_INFO:
                            serviceIntent.putExtra(INTENT_STAT_HOURS, intent.getIntExtra(INTENT_STAT_HOURS, 24));
                            startService = true;
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

        JoH.startService(WatchBroadcastService.class, INTENT_FUNCTION_KEY, CMD_START);
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
        WatchBroadcast watchBroadcast;
        Bundle bundle = null;
        //send to all connected apps
        for (Map.Entry<String, WatchBroadcast> entry : broadcastEntities.entrySet()) {
            receiver = entry.getKey();
            watchBroadcast = entry.getValue();
            switch (function) {
                case CMD_UPDATE_BG:
                    bundle = prepareBgBundle(watchBroadcast);
                    sendBroadcast(function, receiver, bundle);
                    break;
            }
        }
        if (function.equals(CMD_UPDATE_BG)) {
            handled = true;
        }

        if (handled) {
            return;
        }
        switch (function) {
            case CMD_REPLY_MSG:
                bundle = new Bundle();
                bundle.putString(INTENT_REPLY_MSG, intent.getStringExtra(INTENT_REPLY_MSG));
                bundle.putString(INTENT_REPLY_CODE, intent.getStringExtra(INTENT_REPLY_CODE));
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                break;
            case CMD_ALERT:
                bundle = new Bundle();
                bundle.putString("type", intent.getStringExtra("type"));
                bundle.putString("message", intent.getStringExtra("message"));
                break;
            case CMD_START:
                break;
            case CMD_STAT_INFO:
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                watchBroadcast = broadcastEntities.get(receiver);
                bundle = prepareStatisticBundle(watchBroadcast, intent.getIntExtra("stat_hours", 24));
                break;
            case CMD_UPDATE_BG_FORCE:
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                watchBroadcast = broadcastEntities.get(receiver);
                bundle = prepareBgBundle(watchBroadcast);
                break;
            case CMD_SNOOZE_ALERT:
                receiver = intent.getStringExtra(INTENT_PACKAGE_KEY);
                String alertName = "";
                String replyMsg = "";
                String replyCode = INTENT_REPLY_CODE_OK;
                int snoozeMinutes = 0;
                double nextAlertAt = JoH.ts();
                String activeAlertType = intent.getStringExtra(INTENT_ALERT_TYPE);
                bundle = new Bundle();
                if (activeAlertType.equals(BG_ALERT_TYPE)) {
                    if (ActiveBgAlert.currentlyAlerting()) {
                        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                        if (activeBgAlert == null) {
                            replyMsg = "Error: snooze was called but no alert is active";
                            replyCode = INTENT_REPLY_CODE_ERROR;
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
                        replyCode = INTENT_REPLY_CODE_ERROR;
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
                if (replyMsg.isEmpty()) {
                    replyMsg = "Snooze accepted";
                    bundle.putString("alertName", alertName);
                    bundle.putInt("snoozeMinutes", snoozeMinutes);
                    bundle.putLong("nextAlertAt", (long) nextAlertAt);
                }
                bundle.putString(INTENT_REPLY_MSG, replyMsg);
                bundle.putString(INTENT_REPLY_CODE, replyCode);
                break;
            default:
                return;
        }
        sendBroadcast(function, receiver, bundle);
    }

    public void sendBroadcast(String function, String receiver, Bundle bundle) {
        Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_SENDER);
        UserError.Log.d(TAG, String.format("sendBroadcast functionName:%s, receiver: %s", function, receiver));

        if (function == null || function.isEmpty()) {
            UserError.Log.d(TAG, "Error, function not specified");
            return;
        }

        intent.putExtra(INTENT_FUNCTION_KEY, function);
        if (receiver != null && !receiver.isEmpty()) {
            intent.setPackage(receiver);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }

    public Bundle prepareStatisticBundle(WatchBroadcast watchBroadcast, int statHours) {
        Bundle bundle;
        if (watchBroadcast.isStatCacheValid(statHours)) {
            UserError.Log.d(TAG, "Stats Cache Hit");
            bundle = watchBroadcast.getStatBundle();
        } else {
            UserError.Log.d(TAG, "Stats Cache Miss");
            UserError.Log.d(TAG, "Getting StatsResult");
            bundle = new Bundle();
            final StatsResult statsResult = new StatsResult(Pref.getInstance(), Constants.HOUR_IN_MS * statHours, JoH.tsl());

            bundle.putString("status.avg", statsResult.getAverageUnitised());
            bundle.putString("status.a1c_dcct", statsResult.getA1cDCCT());
            bundle.putString("status.a1c_ifcc", statsResult.getA1cIFCC());
            bundle.putString("status.in", statsResult.getInPercentage());
            bundle.putString("status.high", statsResult.getHighPercentage());
            bundle.putString("status.low", statsResult.getLowPercentage());
            bundle.putString("status.stdev", statsResult.getStdevUnitised());
            bundle.putString("status.gvi", statsResult.getGVI());
            bundle.putString("status.carbs", String.valueOf(Math.round(statsResult.getTotal_carbs())));
            bundle.putString("status.insulin", JoH.qs(statsResult.getTotal_insulin(), 2));
            bundle.putString("status.royce_ratio", JoH.qs(statsResult.getRatio(), 2));
            bundle.putString("status.capture_percentage", statsResult.getCapturePercentage(false));
            bundle.putString("status.capture_realtime_capture_percentage", statsResult.getRealtimeCapturePercentage(false));
            String accuracyString = "";
            final long accuracy_period = DAY_IN_MS * 3;
            final String accuracy_report = Accuracy.evaluateAccuracy(accuracy_period);
            if ((accuracy_report != null) && (accuracy_report.length() > 0)) {
                accuracyString = accuracy_report;
            } else {
                final String accuracy = BloodTest.evaluateAccuracy(accuracy_period);
                accuracyString = (accuracy != null) ? " " + accuracy : "";
            }
            bundle.putString("status.accuracy", accuracyString);
            bundle.putString("status.steps", String.valueOf(statsResult.getTotal_steps()));
            
            watchBroadcast.setStatCache(bundle, statHours);
        }
        return bundle;
    }

    public Bundle prepareBgBundle(WatchBroadcast watchBroadcast) {
        if (watchBroadcast == null) return null;
        WatchSettings settings = watchBroadcast.getSettings();
        if (settings == null) return null;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());

        Bundle bundle = new Bundle();
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
            bundle.putString("pumpJSON", PumpStatus.toJson());

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
