package com.eveningoutpost.dexdrip.services.broadcastservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.Accuracy;
import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.BloodTest;
import com.eveningoutpost.dexdrip.models.HeartRate;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.StepCounter;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserNotification;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PumpStatus;
import com.eveningoutpost.dexdrip.stats.StatsResult;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.store.KeyStore;
import com.eveningoutpost.dexdrip.utils.PowerStateReceiver;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.BroadcastModel;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.GraphLine;
import com.eveningoutpost.dexdrip.services.broadcastservice.models.Settings;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.HashMap;
import java.util.Map;

import lecho.lib.hellocharts.model.Line;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;

// External status line from AAPS added
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLine;
import static com.eveningoutpost.dexdrip.wearintegration.ExternalStatusService.getLastStatusLineTime;

/**
 *  Broadcast API which provides common data like, bg values, graph info, statistic info.
 *  Also it can handle different alarms, save HR data, steps and treatments.
 *  This service was designed as a universal service so multiple thirdparty applications can use it.
 *  Both commands will store application packageKey with settings. Stored settings would be used
 *  when there would be a new bg data, the service will send the graph data to a specific applications
 *  (packageKey) with their own graph settings.
 *  {@link BroadcastService}
 */
public class BroadcastService extends Service {
    /**
     * action which receive data from thirdparty application
     */
    protected static final String ACTION_WATCH_COMMUNICATION_RECEIVER = "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_RECEIVER";

    /**
     * action used to send data to thirdparty application
     */
    protected static final String ACTION_WATCH_COMMUNICATION_SENDER = "com.eveningoutpost.dexdrip.watch.wearintegration.BROADCAST_SERVICE_SENDER";

    private static final int COMMANDS_LIMIT_TIME_SEC = 2;

    public static SharedPreferences.OnSharedPreferenceChangeListener prefListener = (prefs, key) -> {
        if (key.equals(BroadcastEntry.PREF_ENABLED)) {
            JoH.startService(BroadcastService.class);
        }
    };

    protected String TAG = this.getClass().getSimpleName();
    protected Map<String, BroadcastModel> broadcastEntities;

    protected KeyStore keyStore = FastStore.getInstance();

    /**
     *  The receiver listening {@link  ACTION_WATCH_COMMUNICATION_RECEIVER} action.
     *  Every Receiver command requires {@link Const.INTENT_PACKAGE_KEY}
     *  and {@link Const.INTENT_FUNCTION_KEY} extra parameters in the intent.
     *  {@link Const.INTENT_PACKAGE_KEY} describes the thirdparty application and used to identify
     *  it's own settings, so every application should use own identificator.
     *  {@link Const.INTENT_FUNCTION_KEY} describes the function command.
     *  When thirdparty application received  {@link Const.CMD_START}, it can send {@link Const.CMD_SET_SETTINGS}
     *  or {@link Const.CMD_UPDATE_BG_FORCE} command with settings model {@link Settings}.
     *  Both commands will store application packageKey with own settings. Stored settings
     *  would be used when there would be a new BG data in xdrip, the service will send the
     *  graph data to a specific applications (packageKey) with their own graph settings.
     *  If service received a command from not registered packageKey, this command would be skipped.
     *  So it is necessary to "register" third-party applications with CMD_SET_SETTINGS or CMD_UPDATE_BG_FORCE at first.
     *  {@link Settings} model is a {@link Parcelable} object. Please note since Settings model
     *  is located in package com.eveningoutpost.dexdrip.services.broadcastservice.models and
     *  xdrip code replacing 'Services' package name to lowercase 'services' name after
     *  apk compilation, the thirdparty application should use the following package
     *  com.eveningoutpost.dexdrip.services.broadcastservice.models for the settings model.
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action == null || !action.equals(ACTION_WATCH_COMMUNICATION_RECEIVER)) return;
                PowerManager.WakeLock wl = JoH.getWakeLock(TAG, 2000);
                String packageKey = intent.getStringExtra(Const.INTENT_PACKAGE_KEY);
                String function = intent.getStringExtra(Const.INTENT_FUNCTION_KEY);
                UserError.Log.d(TAG, String.format("received broadcast: function: %s, packageKey: %s", function, packageKey));

                boolean startService = false;
                long timeStamp;
                Settings settings = null;
                Intent serviceIntent = new Intent(xdrip.getAppContext(), BroadcastService.class);

                if (Const.CMD_SET_SETTINGS.equals(function) || Const.CMD_UPDATE_BG_FORCE.equals(function)) {
                    if (packageKey == null) {
                        function = Const.CMD_REPLY_MSG;
                        serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "Error, \"PACKAGE\" extra not specified");
                        serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_PACKAGE_ERROR);
                        startService = true;
                    }
                } else {
                    if (!broadcastEntities.containsKey(packageKey)) {
                        function = Const.CMD_REPLY_MSG;
                        serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "Error, the app should be registered at first");
                        serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_NOT_REGISTERED);
                        startService = true;
                    }
                }
                if (!startService && JoH.pratelimit(function + "_" + packageKey, COMMANDS_LIMIT_TIME_SEC)) {
                    switch (function) {
                        case Const.CMD_SET_SETTINGS:
                            try {
                                settings = intent.getParcelableExtra(Const.INTENT_SETTINGS);
                            }
                            catch ( BadParcelableException e){
                                UserError.Log.e(TAG, "broadcast onReceive Error: " + e.toString());
                            }
                            if (settings == null) {
                                function = Const.CMD_REPLY_MSG;
                                serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "Can't parse settings");
                                serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_ERROR);
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, new BroadcastModel(settings));
                            break;
                        case Const.CMD_UPDATE_BG_FORCE:
                            settings = intent.getParcelableExtra(Const.INTENT_SETTINGS);
                            if (settings == null) {
                                function = Const.CMD_REPLY_MSG;
                                serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "Can't parse settings");
                                serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_ERROR);
                                startService = true;
                                break;
                            }
                            broadcastEntities.put(packageKey, new BroadcastModel(settings));
                            //update immediately
                            startService = true;
                            break;
                        case Const.CMD_SNOOZE_ALERT:
                            String activeAlertType = intent.getStringExtra(Const.INTENT_ALERT_TYPE);
                            if (activeAlertType == null) {
                                function = Const.CMD_REPLY_MSG;
                                serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "\"ALERT_TYPE\" not specified ");
                                serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_ERROR);
                                startService = true;
                                break;
                            }
                            serviceIntent.putExtra(Const.INTENT_ALERT_TYPE, activeAlertType);
                            startService = true;
                            break;
                        case Const.CMD_ADD_STEPS:
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            int steps = intent.getIntExtra("value", 0);
                            StepCounter.createEfficientRecord(timeStamp, steps);
                            break;
                        case Const.CMD_ADD_HR:
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            int hrValue = intent.getIntExtra("value", 0);
                            HeartRate.create(timeStamp, hrValue, 1);
                            break;
                        case Const.CMD_ADD_TREATMENT: //so it would be possible to add treatment via watch
                            timeStamp = intent.getLongExtra("timeStamp", JoH.tsl());
                            double carbs = intent.getDoubleExtra("carbs", 0);
                            double insulin = intent.getDoubleExtra("insulin", 0);
                            Treatments.create(carbs, insulin, timeStamp);
                            function = Const.CMD_REPLY_MSG;
                            serviceIntent.putExtra(Const.INTENT_REPLY_MSG, "Treatment were added");
                            serviceIntent.putExtra(Const.INTENT_REPLY_CODE, Const.INTENT_REPLY_CODE_OK);
                            startService = true;
                            break;
                        case Const.CMD_STAT_INFO:
                            serviceIntent.putExtra(Const.INTENT_STAT_HOURS, intent.getIntExtra(Const.INTENT_STAT_HOURS, 24));
                            startService = true;
                            break;
                    }
                }
                if (startService) {
                    serviceIntent.putExtra(Const.INTENT_FUNCTION_KEY, function);
                    serviceIntent.putExtra(Const.INTENT_PACKAGE_KEY, packageKey);
                    xdrip.getAppContext().startService(serviceIntent);
                    return;
                }
                JoH.releaseWakeLock(wl);
            } catch (Exception e) {
                UserError.Log.e(TAG, "broadcast onReceive Error: " + e.toString());
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * When service started it's will send a broadcast message CMD_START for thirdparty
     * applications and waiting for commands from applications by listening broadcastReceiver.
     * @see Const.CMD_START
     */
    @Override
    public void onCreate() {
        UserError.Log.e(TAG, "starting service");
        broadcastEntities = new HashMap<>();
        registerReceiver(broadcastReceiver, new IntentFilter(ACTION_WATCH_COMMUNICATION_RECEIVER));

        JoH.startService(BroadcastService.class, Const.INTENT_FUNCTION_KEY, Const.CMD_START);

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
            if (BroadcastEntry.isEnabled()) {
                if (intent != null) {
                    final String function = intent.getStringExtra(Const.INTENT_FUNCTION_KEY);
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

    /**
     * Handles commands from service receiver
     * @param function Command name
     * @param intent Intent received from service
     */
    private void handleCommand(String function, Intent intent) {
        UserError.Log.d(TAG, "handleCommand function:" + function);
        String receiver = null;
        boolean handled = false;
        BroadcastModel broadcastModel;
        Bundle bundle = null;
        //send to all connected apps
        for (Map.Entry<String, BroadcastModel> entry : broadcastEntities.entrySet()) {
            receiver = entry.getKey();
            broadcastModel = entry.getValue();
            switch (function) {
                case Const.CMD_UPDATE_BG:
                    handled = true;
                    bundle = prepareBgBundle(broadcastModel);
                    sendBroadcast(function, receiver, bundle);
                    break;
                case Const.CMD_ALERT:
                    handled = true;
                    bundle = new Bundle();
                    bundle.putString("type", intent.getStringExtra("type"));
                    bundle.putString("message", intent.getStringExtra("message"));
                    sendBroadcast(function, receiver, bundle);
                    break;
                case Const.CMD_CANCEL_ALERT:
                    sendBroadcast(function, receiver, bundle);
                    break;
            }
        }

        if (handled) {
            return;
        }
        receiver = intent.getStringExtra(Const.INTENT_PACKAGE_KEY);
        switch (function) {
            case Const.CMD_REPLY_MSG:
                bundle = new Bundle();
                bundle.putString(Const.INTENT_REPLY_MSG, intent.getStringExtra(Const.INTENT_REPLY_MSG));
                bundle.putString(Const.INTENT_REPLY_CODE, intent.getStringExtra(Const.INTENT_REPLY_CODE));
                break;
            case Const.CMD_START:
                receiver = null; //broadcast
                break;
            case Const.CMD_STAT_INFO:
                broadcastModel = broadcastEntities.get(receiver);
                bundle = prepareStatisticBundle(broadcastModel, intent.getIntExtra("stat_hours", 24));
                break;
            case Const.CMD_UPDATE_BG_FORCE:
                broadcastModel = broadcastEntities.get(receiver);
                bundle = prepareBgBundle(broadcastModel);
                break;
            case Const.CMD_CANCEL_ALERT:
                receiver = null; //broadcast
                break;
            case Const.CMD_SNOOZE_ALERT:
                String alertName = "";
                String replyMsg = "";
                String replyCode = Const.INTENT_REPLY_CODE_OK;
                int snoozeMinutes = 0;
                double nextAlertAt = JoH.ts();
                String activeAlertType = intent.getStringExtra(Const.INTENT_ALERT_TYPE);
                bundle = new Bundle();
                if (activeAlertType.equals(Const.BG_ALERT_TYPE)) {
                    if (ActiveBgAlert.currentlyAlerting()) {
                        ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                        if (activeBgAlert == null) {
                            replyMsg = "Error: snooze was called but no alert is active";
                            replyCode = Const.INTENT_REPLY_CODE_ERROR;
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
                        replyCode = Const.INTENT_REPLY_CODE_ERROR;
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
                bundle.putString(Const.INTENT_REPLY_MSG, replyMsg);
                bundle.putString(Const.INTENT_REPLY_CODE, replyCode);
                break;
            default:
                return;
        }
        sendBroadcast(function, receiver, bundle);
    }

    /**
     * Will send  {@link Intent} message as a broadcast message or to specific receiver
     * @param function Function name
     * @param receiver If specified, will send a broadcast message to a specific receiver domain
     * @param bundle If specified, would be added to broadcast {@link Intent}
     */
    protected void sendBroadcast(String function, String receiver, Bundle bundle) {
        Intent intent = new Intent(ACTION_WATCH_COMMUNICATION_SENDER);
        UserError.Log.d(TAG, String.format("sendBroadcast functionName: %s, receiver: %s", function, receiver));

        if (function == null || function.isEmpty()) {
            UserError.Log.d(TAG, "Error, function not specified");
            return;
        }

        intent.putExtra(Const.INTENT_FUNCTION_KEY, function);
        if (receiver != null && !receiver.isEmpty()) {
            intent.setPackage(receiver);
        }
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        xdrip.getAppContext().sendBroadcast(intent);
    }

    protected Bundle prepareStatisticBundle(BroadcastModel broadcastModel, int statHours) {
        Bundle bundle;
        if (broadcastModel.isStatCacheValid(statHours)) {
            UserError.Log.d(TAG, "Stats Cache Hit");
            bundle = broadcastModel.getStatBundle();
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
            String accuracyString;
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

            broadcastModel.setStatCache(bundle, statHours);
        }
        return bundle;
    }

    protected Bundle prepareBgBundle(BroadcastModel broadcastModel) {
        if (broadcastModel == null) return null;
        Settings settings = broadcastModel.getSettings();
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
                long graphStartOffset = settings.getGraphStart();
                long graphEndOffset = settings.getGraphEnd();
                long start = JoH.tsl();
                long end = start;
                if (graphStartOffset == 0) {
                    graphStartOffset = Constants.HOUR_IN_MS * 2;
                }
                start = start - graphStartOffset;
                end = end + graphEndOffset;

                bundle.putInt("fuzzer", BgGraphBuilder.FUZZER);
                bundle.putLong("start", start);
                bundle.putLong("end", end);
                bundle.putDouble("highMark", JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170));
                bundle.putDouble("lowMark", JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70));

                BgGraphBuilder bgGraphBuilder = new BgGraphBuilder(xdrip.getAppContext(), start, end);
                bgGraphBuilder.defaultLines(false); // not simple mode in order to receive simulated data

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

            String last_iob = keyStore.getS("last_iob");
            if ( last_iob != null){
                bundle.putString("predict.IOB", last_iob);
                bundle.putLong("predict.IOB.timeStamp", keyStore.getL("last_iob_timestamp"));
            }

            String last_bwp = keyStore.getS("last_bwp");
            if ( last_bwp != null){
                bundle.putString("predict.BWP", last_bwp);
                bundle.putLong("predict.BWP.timeStamp", keyStore.getL("last_bwp_timestamp"));
            }

            // External status line from AAPS added
            bundle.putString("external.statusLine", getLastStatusLine());
            bundle.putLong("external.timeStamp", getLastStatusLineTime());

        }
        return bundle;
    }

}
