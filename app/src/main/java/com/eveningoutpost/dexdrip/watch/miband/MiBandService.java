package com.eveningoutpost.dexdrip.watch.miband;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.HeartRate;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.StepCounter;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserNotification;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer;
import com.eveningoutpost.dexdrip.Services.MissedReadingService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Intents;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.utils.framework.PoorMansConcurrentLinkedDeque;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence.SequenceState;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand4;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence.SequenceStateMiBand5;
import com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceGenerator;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertLevelMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AuthMessages;
import com.eveningoutpost.dexdrip.watch.miband.message.DeviceEvent;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiband3_4;
import com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.ConnectionParameters;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;

import static com.eveningoutpost.dexdrip.Models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.getResourceURI;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.Models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSED;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.SLEEP;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_ALARM;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CALL;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CANCEL;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_MESSAGE;
import static com.eveningoutpost.dexdrip.watch.miband.Const.PREFERRED_MTU_SIZE;
import static com.eveningoutpost.dexdrip.watch.miband.MiBandService.MiBandState.AUTHORIZE_FAILED;
import static com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiband3_4.NightMode.Sheduled;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_FAIL;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_MIBAND4_CODE_FAIL;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_MIBAND4_FAIL;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_REQUEST_RANDOM_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_RESPONSE;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_ENCRYPTED_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_KEY;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SUCCESS;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_ACK_FIND_PHONE_IN_PROGRESS;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.COMMAND_DISABLE_CALL;
import static com.eveningoutpost.dexdrip.xdrip.gs;

/**
 * <p>
 * Data communication with MiBand compatible bands/watches
 */

public class MiBandService extends JamBaseBluetoothSequencer {
    public static final boolean d = true;
    static final List<UUID> huntCharacterstics = new ArrayList<>();
    private static final long RETRY_PERIOD_MS = Constants.SECOND_IN_MS * 30; // sleep for max ms if we have had no signal
    private static final long BG_UPDATE_NO_DATA_INTERVAL = 30 * Constants.MINUTE_IN_MS; //minutes
    private static final long CONNECTION_TIMEOUT = 5 * Constants.MINUTE_IN_MS; //minutes
    private static final long RESTORE_NIGHT_MODE_DELAY = (Constants.SECOND_IN_MS * 7);
    private static final int QUEUE_EXPIRED_TIME = 30; //second
    private static final int QUEUE_DELAY = 0; //ms
    private static final int CALL_ALERT_DELAY = (int) (Constants.SECOND_IN_MS * 10);
    private static final int MESSAGE_DELAY = (int) (Constants.SECOND_IN_MS * 5);
    static BatteryInfo batteryInfo = new BatteryInfo();
    static private long bgWakeupTime;

    static {
        huntCharacterstics.add(Const.UUID_CHAR_HEART_RATE_MEASUREMENT);
    }

    private final PoorMansConcurrentLinkedDeque<QueueMessage> messageQueue = new PoorMansConcurrentLinkedDeque<>();
    private Subscription authSubscription;
    private Subscription notifSubscriptionDeviceEvent;
    private Subscription notifSubscriptionHeartRateMeasurement;
    private Subscription notifSubscriptionStepsMeasurement;
    private AuthMessages authorisation;
    private Boolean isNeedToCheckRevision = true;
    private Boolean isNeedToAuthenticate = true;
    private boolean isWaitingCallResponce = false;
    private Boolean isNeedToRestoreNightMode = false;
    private boolean isNightMode = false;
    private FirmwareOperations firmware;
    private Subscription watchfaceSubscription;
    private MediaPlayer player;
    private PendingIntent bgServiceIntent;
    private MiBandType prevDeviceType = MiBandType.UNKNOWN;
    private QueueMessage queueItem;
    private BroadcastReceiver statusReceiver;
    private String statusIOB = "";
    private boolean prevReadingStatusIsStale = false;
    private String activeAlertType;
    private String missingAlertMessage;

    {
        mState = new MiBandState().setLI(I);
        I.backgroundStepDelay = 0;
        //I.autoConnect = true;
        //I.playSounds = true;
        I.connectTimeoutMinutes = (int) CONNECTION_TIMEOUT;
        startBgTimer();
    }

    private static final boolean isBetweenValidTime(Date startTime, Date endTime, Date currentTime) {
        //Start Time
        Calendar StartTime = Calendar.getInstance();
        StartTime.setTime(startTime);
        StartTime.set(1, 1, 1);

        Calendar EndTime = Calendar.getInstance();
        EndTime.setTime(endTime);
        EndTime.set(1, 1, 1);

        //Current Time
        Calendar CurrentTime = Calendar.getInstance();
        CurrentTime.setTime(currentTime);
        CurrentTime.set(1, 1, 1);
        if (EndTime.compareTo(StartTime) > 0) {
            if ((CurrentTime.compareTo(StartTime) >= 0) && (CurrentTime.compareTo(EndTime) <= 0)) {
                return true;
            } else {
                return false;
            }
        } else if (EndTime.compareTo(StartTime) < 0) {
            if ((CurrentTime.compareTo(EndTime) >= 0) && (CurrentTime.compareTo(StartTime) <= 0)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    // Mega Status
    public static List<StatusItem> megaStatus() {

        final List<StatusItem> l = new ArrayList<>();
        final Inst II = Inst.get(MiBandService.class.getSimpleName());


        l.add(new StatusItem("Model", MiBand.getModel()));
        l.add(new StatusItem("Software version", MiBand.getVersion()));

        l.add(new StatusItem("Mac address", MiBand.getMac()));
        l.add(new StatusItem("Connected", II.isConnected ? gs(R.string.yes) : gs(R.string.no)));
        l.add(new StatusItem("Is authenticated", MiBand.isAuthenticated() ? gs(R.string.yes) : gs(R.string.no)));
        if (II.isConnected) {
            int levelInPercent = batteryInfo.getLevelInPercent();
            String levelInPercentText;
            if (levelInPercent == 1000)
                levelInPercentText = "Unknown";
            else
                levelInPercentText = levelInPercent + "%";
            l.add(new StatusItem("Battery", levelInPercentText));
        }
        if (II.wakeup_time != 0) {
            final long till = msTill(II.wakeup_time);
            if (till > 0) l.add(new StatusItem("Wake Up", niceTimeScalar(till)));
        }

        if (bgWakeupTime != 0) {
            final long till = msTill(bgWakeupTime);
            if (till > 0) l.add(new StatusItem("Next time update", niceTimeScalar(till)));
        }

        l.add(new StatusItem("State", II.state));

        final int qsize = II.getQueueSize();
        if (qsize > 0) {
            l.add(new StatusItem("Queue", qsize + " items"));
        }

        return l;
    }

    private Class getPrefBinder() {
        MiBandType type = MiBand.getMibandType();
        if (type == MiBandType.MI_BAND2)
            return Miband2PrefBinding.class;
        else return Miband3_4PrefBinding.class;
    }

    @Override
    public void onCreate() {

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
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(statusReceiver);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Exception unregistering broadcast receiver: " + e);
        }
    }

    private boolean readyToProcessCommand() {
        boolean result = I.state.equals(MiBandState.SLEEP) || I.state.equals(MiBandState.CLOSED) || I.state.equals(MiBandState.CLOSE) || I.state.equals(MiBandState.INIT) || I.state.equals(MiBandState.CONNECT_NOW);
        if (!result && I.state.equals(MiBandState.AUTHORIZE_FAILED) && MiBandType.supportPairingKey(MiBand.getMibandType())) {
            return true;
        }
        if (!I.isConnected) {
            return true;
        }
        if (!result)
            UserError.Log.d(TAG, "readyToProcessCommand not ready because state :" + I.state.toString());
        return result;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                final String authMac = MiBand.getPersistentAuthMac();
                String mac
                        = MiBand.getMac();
                MiBandType currDevice = MiBand.getMibandType();
                if ((currDevice != prevDeviceType) && currDevice != MiBandType.UNKNOWN) {
                    prevDeviceType = currDevice;
                    UserError.Log.d(TAG, "Found new device: " + currDevice.toString());
                    MiBandEntry.sendPrefIntent(MIBAND_INTEND_STATES.UPDATE_PREF_SCREEN, 0, "");
                }
                if (!authMac.equalsIgnoreCase(mac) || authMac.isEmpty()) {
                    prevDeviceType = MiBand.getMibandType();
                    if (!authMac.isEmpty()) {
                        String model = MiBand.getModel();
                        MiBand.setPersistentAuthMac(""); //flush old auth info
                        MiBand.setModel(model, mac);
                    }
                    isNeedToAuthenticate = true;
                }
                if (emptyString(mac)) {
                    // if mac not set then start up a scan and do nothing else
                    new FindNearby().scan();
                } else {
                    setAddress(mac);
                    if (intent != null) {
                        final String function = intent.getStringExtra("function");
                        if (function != null) {
                            UserError.Log.d(TAG, "onStartCommand was called with function:" + function);

                            String message_type = intent.getStringExtra("message_type");
                            String message = intent.getStringExtra("message");
                            String title = intent.getStringExtra("title");

                            message = message != null ? message : "";
                            message_type = message_type != null ? message_type : "";
                            title = title != null ? title : "";
                            if (function.equals("refresh") && !JoH.pratelimit("miband-set-time-via-refresh-" + MiBand.getMac(), 5)) {
                                return START_STICKY;
                            } else {
                                if (function.equals("after_alarm")){
                                    messageQueue.addFirst(new QueueMessage(function, message_type, message, title));
                                    handleCommand();
                                }
                                else {
                                    messageQueue.add(new QueueMessage(function, message_type, message, title));
                                    if (readyToProcessCommand()) {
                                        handleCommand();
                                    }
                                }
                            }
                        } else {
                            // no specific function
                        }
                    }
                }
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopBgUpdateTimer();
                stopConnection();
                changeState(CLOSE);
                prevReadingStatusIsStale = false;
                stopSelf();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void handleCommand() {
        if (messageQueue.isEmpty()) return;
        do {
            queueItem = messageQueue.poll();
        } while (queueItem.isExpired() && !messageQueue.isEmpty());
        if (queueItem.isExpired()) return;

        switch (queueItem.functionName) {
            case "refresh":
                whenToRetryNextBgTimer(); //recalculate isNightMode
                ((MiBandState) mState).setSettingsSequence();
                break;
            case "message":
                ((MiBandState) mState).setQueueSequence();
                break;
            case "alarm":
                ((MiBandState) mState).setAlarmSequence();
                if (isNightMode) {
                    messageQueue.addFirst(new QueueMessage("update_bg_force"));
                }
                break;
            case "after_alarm":
                if (!I.state.equals(MiBandState.WAITING_USER_RESPONSE)) break;
                vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                if (!missingAlertMessage.isEmpty()) {
                    String msgText = xdrip.getAppContext().getString(R.string.miband_alert_missing_text) + missingAlertMessage;
                    messageQueue.addFirst(new QueueMessage("message", MIBAND_NOTIFY_TYPE_MESSAGE, msgText, xdrip.getAppContext().getString(R.string.miband_alert_missing_title_text)));
                }
                ((MiBandState) mState).setQueueSequence();
                break;
            case "update_bg":
                if (isNightMode) {
                    UserError.Log.d(TAG, "Skip bg update because of night mode");
                    return;
                }
                boolean curReadingStatusIsStale = isStaleReading();

                if (prevReadingStatusIsStale && curReadingStatusIsStale) {
                    UserError.Log.d(TAG, "Skip bg update because of staleReading");
                    return;
                }

                prevReadingStatusIsStale = curReadingStatusIsStale;
                startBgTimer();
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case "update_bg_force":
                startBgTimer();
                ((MiBandState) mState).setSendReadingSequence();
                break;
            case "update_bg_as_notification":
                ((MiBandState) mState).setSendReadingSequence();
                break;
            default:
                return;
        }

        changeState(INIT);
    }

    private boolean isStaleReading() {
        BgReading last = BgReading.last();
        if (last == null || last.isStale()) {
            return true;
        }
        return false;
    }

    private long whenToRetryNextBgTimer() {
        final long bg_time;

        Calendar expireDate = Calendar.getInstance();
        long currTimeMillis = JoH.tsl();
        expireDate.setTimeInMillis(System.currentTimeMillis() + BG_UPDATE_NO_DATA_INTERVAL);
        isNightMode = false;
        if (MiBandEntry.isNightModeEnabled()) {
            int nightModeInterval = MiBandEntry.getNightModeInterval();
            if (nightModeInterval != MiBandEntry.NIGHT_MODE_INTERVAL_STEP) {
                Calendar currCal = Calendar.getInstance();
                Date curr = currCal.getTime();
                Date start = MiBandEntry.getNightModeStart();
                Date end = MiBandEntry.getNightModeEnd();
                boolean result = isBetweenValidTime(start, end, curr);
                UserError.Log.d(TAG, "isBetweenValidTime: " + result);
                if (result) {
                    Calendar futureCal = Calendar.getInstance();
                    futureCal.setTimeInMillis(currTimeMillis + nightModeInterval * Constants.MINUTE_IN_MS);

                    Date futureDate = futureCal.getTime();
                    if (!isBetweenValidTime(start, end, futureDate)) {
                        Calendar calEndCal = Calendar.getInstance();
                        calEndCal.setTime(end);
                        futureCal.set(Calendar.HOUR_OF_DAY, calEndCal.get(Calendar.HOUR_OF_DAY));
                        futureCal.set(Calendar.MINUTE, calEndCal.get(Calendar.MINUTE));
                    }
                    expireDate = (Calendar) futureCal.clone();
                    isNightMode = true;
                }
            }
        }
        bg_time = expireDate.getTimeInMillis() - currTimeMillis;
        return bg_time;
    }

    private void stopBgUpdateTimer() {
        JoH.cancelAlarm(xdrip.getAppContext(), bgServiceIntent);
        bgWakeupTime = 0;
        isNightMode = false;
    }

    private void startBgTimer() {
        stopBgUpdateTimer();
        if (shouldServiceRun() && MiBand.isAuthenticated() && !MiBandEntry.isNeedSendReadingAsNotification()) {
            final long retry_in = whenToRetryNextBgTimer();
            UserError.Log.d(TAG, "Scheduling next BgTimer in: " + JoH.niceTimeScalar(retry_in) + " @ " + JoH.dateTimeText(retry_in + JoH.tsl()));
            bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, "update_bg_force");
            JoH.wakeUpIntent(xdrip.getAppContext(), retry_in, bgServiceIntent);
            bgWakeupTime = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Retry timer was not sheduled");
        }
    }

    private void acknowledgeFindPhone() {
        UserError.Log.d(TAG, "acknowledgeFindPhone");
        I.connection.writeCharacteristic(Const.UUID_CHARACTERISTIC_3_CONFIGURATION, COMMAND_ACK_FIND_PHONE_IN_PROGRESS)
                .subscribe(val -> {
                    if (d)
                        UserError.Log.d(TAG, "Wrote acknowledgeFindPhone: " + JoH.bytesToHex(val));
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not write acknowledgeFindPhone: " + throwable);
                });
    }

    private void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        switch (value[0]) {
            case DeviceEvent.CALL_REJECT:
                UserError.Log.d(TAG, "call rejected");
                if (I.state.equals(MiBandState.WAITING_USER_RESPONSE)) {
                    String alertName = "";
                    int snoozeMinutes = 0;
                    String msgText = "";
                    double next_alert_at = JoH.ts();
                    if (activeAlertType.equals(Const.BG_ALERT_TYPE)) {
                        if (ActiveBgAlert.currentlyAlerting()) {
                            ActiveBgAlert activeBgAlert = ActiveBgAlert.getOnly();
                            if (activeBgAlert == null) {
                                UserError.Log.e(TAG, "Error, snooze was called but no alert is active");
                            } else {
                                AlertType alert = ActiveBgAlert.alertTypegetOnly();
                                if (alert != null) {
                                    alertName = alert.name;
                                    snoozeMinutes = alert.default_snooze;
                                }
                                AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1, true);
                                next_alert_at = activeBgAlert.next_alert_at;
                            }
                        }

                    } else {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        snoozeMinutes = (int) MissedReadingService.getOtherAlertSnoozeMinutes(prefs, activeAlertType);
                        UserNotification.snoozeAlert(activeAlertType, snoozeMinutes);
                        UserNotification userNotification = UserNotification.GetNotificationByType(activeAlertType);
                        if (userNotification != null) {
                            next_alert_at = userNotification.timestamp;
                        }
                    }
                    msgText = String.format(xdrip.getAppContext().getString(R.string.miband_alert_snooze_text), alertName, snoozeMinutes, JoH.hourMinuteString((long) next_alert_at));
                    UserError.Log.d(TAG, msgText);
                    messageQueue.addFirst(new QueueMessage("message", MIBAND_NOTIFY_TYPE_MESSAGE, msgText, xdrip.getAppContext().getString(R.string.miband_alert_snooze_title_text)));
                    startBgTimer();
                    handleCommand();
                }
                isWaitingCallResponce = false;
                break;
            case DeviceEvent.CALL_IGNORE:
                UserError.Log.d(TAG, "call ignored");
                if (I.state.equals(MiBandState.WAITING_USER_RESPONSE)) {
                    startBgTimer();
                    changeState(SLEEP);
                }
                isWaitingCallResponce = false;
                break;
            case DeviceEvent.BUTTON_PRESSED:
                UserError.Log.d(TAG, "button pressed");
                break;
            case DeviceEvent.BUTTON_PRESSED_LONG:
                UserError.Log.d(TAG, "button long-pressed ");
                break;
            case DeviceEvent.START_NONWEAR:
                UserError.Log.d(TAG, "non-wear start detected");
                break;
            case DeviceEvent.ALARM_TOGGLED:
                UserError.Log.d(TAG, "An alarm was toggled");
                break;
            case DeviceEvent.FELL_ASLEEP:
                UserError.Log.d(TAG, "Fell asleep");
                break;
            case DeviceEvent.WOKE_UP:
                UserError.Log.d(TAG, "Woke up");
                break;
            case DeviceEvent.STEPSGOAL_REACHED:
                UserError.Log.d(TAG, "Steps goal reached");
                break;
            case DeviceEvent.TICK_30MIN:
                UserError.Log.d(TAG, "Tick 30 min (?)");
                break;
            case DeviceEvent.FIND_PHONE_START:
                UserError.Log.d(TAG, "find phone started");
                if ((JoH.ratelimit("band_find phone_sound", 3))) {
                    player = JoH.playSoundUri(getResourceURI(R.raw.default_alert));
                }
                acknowledgeFindPhone();
                break;
            case DeviceEvent.FIND_PHONE_STOP:
                UserError.Log.d(TAG, "find phone stopped");
                if (player != null && player.isPlaying()) player.stop();
                break;
            case DeviceEvent.MUSIC_CONTROL:
                UserError.Log.d(TAG, "got music control");
                switch (value[1]) {
                    case 0:
                        UserError.Log.d(TAG, "Music app Event.PLAY");
                        break;
                    case 1:
                        UserError.Log.d(TAG, "Music app Event.PAUSE");
                        break;
                    case 3:
                        UserError.Log.d(TAG, "Music app Event.NEXT");
                        break;
                    case 4:
                        UserError.Log.d(TAG, "Music app Event.PREVIOUS");
                        break;
                    case 5:
                        UserError.Log.d(TAG, "Music app Event.VOLUMEUP");
                        break;
                    case 6:
                        UserError.Log.d(TAG, "Music app Event.VOLUMEDOWN");
                        break;
                    case (byte) 224:
                        UserError.Log.d(TAG, "Music app started");
                        break;
                    case (byte) 225:
                        UserError.Log.d(TAG, "Music app terminated");
                        break;
                    default:
                        UserError.Log.d(TAG, "unhandled music control event " + value[1]);
                        return;
                }
                break;
            case DeviceEvent.MTU_REQUEST:
                int mtu = (value[2] & 0xff) << 8 | value[1] & 0xff;
                UserError.Log.d(TAG, "device announced MTU of " + mtu);
                break;
            default:
                UserError.Log.d(TAG, "unhandled event " + value[0]);
        }
    }

    @Override
    protected void onServicesDiscovered(RxBleDeviceServices services) {
        boolean found = false;
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                UserError.Log.d(TAG, "-- Character: " + getUUIDName(characteristic.getUuid()));

                for (final UUID check : huntCharacterstics) {
                    if (characteristic.getUuid().equals(check)) {
                        found = true;
                    }
                }
            }
        }
        if (found) {
            I.isDiscoveryComplete = true;
            I.discoverOnce = true;
            changeNextState();
        } else {
            UserError.Log.e(TAG, "Could not find characteristic during service discovery. This is very unusual");
        }
    }

    @SuppressLint("CheckResult")
    private void getSoftwareRevision() {
        I.connection.readCharacteristic(Const.UUID_CHAR_SOFTWARE_REVISION_STRING).subscribe(
                readValue -> {
                    String revision = new String(readValue);
                    UserError.Log.d(TAG, "Got software revision: " + revision);
                    MiBand.setVersion(revision, MiBand.getPersistentAuthMac());
                    isNeedToCheckRevision = false;
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read software revision: " + throwable);
                    changeNextState();
                });
    }

    @SuppressLint("CheckResult")
    private void getBatteryInfo() {
        I.connection.readCharacteristic(Const.UUID_CHARACTERISTIC_6_BATTERY_INFO).subscribe(
                readValue -> {
                    if (d)
                        UserError.Log.d(TAG, "Got battery info: " + JoH.bytesToHex(readValue));
                    batteryInfo = new BatteryInfo(readValue);
                }, throwable -> {
                    if (d)
                        UserError.Log.e(TAG, "Could not read battery info: " + throwable);
                });
    }

    @SuppressLint("CheckResult")
    private void getModelName() {
        I.connection.readCharacteristic(Const.UUID_CHAR_DEVICE_NAME).subscribe(
                readValue -> {
                    String name = new String(readValue);
                    if (d)
                        UserError.Log.d(TAG, "Got device name: " + name);
                    MiBand.setModel(name, MiBand.getPersistentAuthMac());
                    changeNextState();
                }, throwable -> {
                    if (d)
                        UserError.Log.e(TAG, "Could not read device name: " + throwable);
                    changeNextState();
                });
    }

    private Boolean sendBG() {
        BgReading last = BgReading.last();
        AlertMessage message = new AlertMessage();
        if (last == null || last.isStale()) {
            return false;
        } else {
            String messageText = "BG: " + last.displayValue(null) + " " + last.displaySlopeArrow();
            UserError.Log.uel(TAG, "Send alert msg: " + messageText);
            if (MiBand.getMibandType() == MiBandType.MI_BAND2) {
                new QueueMe()
                        .setBytes(message.getAlertMessageOld(messageText.toUpperCase(), AlertMessage.AlertCategory.SMS_MMS))
                        .setDescription("Send alert msg: " + messageText)
                        .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
            } else {
                new QueueMe()
                        .setBytes(message.getAlertMessage(messageText.toUpperCase(), AlertMessage.AlertCategory.CustomHuami, AlertMessage.CustomIcon.APP_11, messageText.toUpperCase()))
                        .setDescription("Send alert msg: " + messageText)
                        .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
            }
        }
        return true;
    }

    private void vibrateAlert(AlertLevelMessage.AlertLevelType level) {
        if (level == AlertLevelMessage.AlertLevelType.NoAlert) {
            new QueueMe()
                    .setBytes(COMMAND_DISABLE_CALL)
                    .setDescription("Send specific disable command for" + level)
                    .setQueueWriteCharacterstic(Const.UUID_CHARACTERISTIC_CHUNKEDTRANSFER)
                    .expireInSeconds(QUEUE_EXPIRED_TIME)
                    .setDelayMs(QUEUE_DELAY)
                    .queue();
        }

        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getAlertLevelMessage(level))
                .setDescription("Send vibrateAlert: " + level)
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(QUEUE_DELAY)
                .queue();
    }

    private void periodicVibrateAlert(int count, int activeVibrationTime, int pauseVibrationTime) {
        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getPeriodicVibrationMessage((byte) count, (short) activeVibrationTime, (short) pauseVibrationTime))
                .setDescription(String.format("Send periodicVibrateAlert c:%d a:%d p:%d", count, activeVibrationTime, pauseVibrationTime))
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs((activeVibrationTime + pauseVibrationTime) * count)
                .queue();
    }

    private void sendSettings() {
       /* List<Pair<Integer, Boolean>> features = PrefBindingFactory.getInstance(getPrefBinder()).getStates("miband_feature_");
        FeaturesControllMessage featureMessage = new FeaturesControllMessage();
        for (Pair<Integer, Boolean> item : features) {
            byte[] message = featureMessage.getMessage(item);
            if (message.length != 0) {
                new QueueMe()
                        .setBytes(message)
                        .setQueueWriteCharacterstic(featureMessage.getCharacteristicUUID())
                        .setDescription("Set feature:" + item.first + ":" + item.second)
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
            }
        }

        List<Integer> screenOpt = PrefBindingFactory.getInstance(getPrefBinder()).getEnabled("miband_screen");

        DisplayControllMessage dispMessage;
        MiBandType type = MiBand.getMibandType();
        if (type == MiBandType.MI_BAND2)
            dispMessage = new DisplayControllMessageMiBand2();
        else
            dispMessage = new DisplayControllMessageMiband3_4();
        new QueueMe()
                .setBytes(dispMessage.getDisplayItemsCmd(screenOpt))
                .setQueueWriteCharacterstic(dispMessage.getCharacteristicUUID())
                .setDescription("Set screens")
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(QUEUE_DELAY)
                .queue();
        */
        setNightMode();
    }

    private void queueMessage() {
        String message = queueItem.message;
        String title = queueItem.title;
        message = message.replace("@", "");
        if (d)
            UserError.Log.d(TAG, "Queuing message alert: " + message);
        AlertMessage alertMessage = new AlertMessage();
        switch (queueItem.message_type != null ? queueItem.message_type : "null") {
            case MIBAND_NOTIFY_TYPE_CALL:
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Send call alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .setRunnable(() -> isWaitingCallResponce = true)
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                if (d)
                    UserError.Log.d(TAG, "Queued call alert: " + message);
                break;
            case MIBAND_NOTIFY_TYPE_CANCEL:
                if (isWaitingCallResponce) {
                    vibrateAlert(AlertLevelMessage.AlertLevelType.NoAlert); //disable call
                    isWaitingCallResponce = false;
                    if (d)
                        UserError.Log.d(TAG, "Call disabled");
                }
                break;
            case MIBAND_NOTIFY_TYPE_ALARM:
                new QueueMe()
                        .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.Call))
                        .setDescription("Sent glucose alert: " + message)
                        .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                        .expireInSeconds(QUEUE_EXPIRED_TIME)
                        .setDelayMs(QUEUE_DELAY)
                        .queue();
                activeAlertType = title;
                missingAlertMessage = message;
                stopBgUpdateTimer();
                bgServiceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_BG_RETRY_ID, "after_alarm");
                JoH.wakeUpIntent(xdrip.getAppContext(), CALL_ALERT_DELAY, bgServiceIntent);
                break;
            case MIBAND_NOTIFY_TYPE_MESSAGE:
                if (MiBand.getMibandType() == MiBandType.MI_BAND2) {
                    new QueueMe()
                            .setBytes(alertMessage.getAlertMessageOld(message, AlertMessage.AlertCategory.SMS_MMS))
                            .setDescription("Sent message: " + message)
                            .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                            .expireInSeconds(QUEUE_EXPIRED_TIME)
                            .setDelayMs(MESSAGE_DELAY)
                            .queue();
                } else {
                    new QueueMe()
                            .setBytes(alertMessage.getAlertMessage(message, AlertMessage.AlertCategory.CustomHuami, AlertMessage.CustomIcon.RED_WHITE_FIRE_8, title))
                            .setDescription("Sent message: " + message)
                            .setQueueWriteCharacterstic(alertMessage.getCharacteristicUUID())
                            .expireInSeconds(QUEUE_EXPIRED_TIME)
                            .setDelayMs(MESSAGE_DELAY)
                            .queue();
                }
                break;
            default: // glucose
                break;
        }
        // this parent method might get called multiple times
        Inevitable.task("miband-s-queue", 200, () -> changeNextState());

    }

    @SuppressLint("CheckResult")
    private void authPhase() {
        extendWakeLock(30000);
        RxBleConnection connection = I.connection;
        if (d)
            UserError.Log.d(TAG, "Authorizing");
        if (I.connection == null) {
            if (d)
                UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }

        String authKey = MiBand.getPersistentAuthKey();
        if (MiBandType.supportPairingKey(MiBand.getMibandType())) {
            if (authKey.isEmpty()) {
                authKey = MiBand.getAuthKey();
                if (authKey.isEmpty()) {
                    authKey = AuthMessages.getAuthCodeFromFilesSystem(MiBand.getMac());
                }
                if (!AuthMessages.isValidAuthKey(authKey)) {
                    JoH.static_toast_long("Wrong miband authorization key, please recheck a key and try to reconnect again");
                    changeState(AUTHORIZE_FAILED);
                    return;
                } else {
                    MiBand.setAuthKey(authKey);
                }
            }
        }
        if (!AuthMessages.isValidAuthKey(authKey)) {
            authKey = "";
        }
        if (d)
            UserError.Log.d(TAG, "authKey: " + authKey);

        authorisation = new AuthMessages(MiBand.getMibandType(), authKey);
        if (d)
            UserError.Log.d(TAG, "localKey: " + JoH.bytesToHex(authorisation.getLocalKey()));
        authSubscription = new Subscription(
                connection.setupNotification(authorisation.getCharacteristicUUID())
                        .timeout(20, TimeUnit.SECONDS) // WARN
                        // .observeOn(Schedulers.newThread()) // needed?
                        .doOnNext(notificationObservable -> {
                                    if (d)
                                        UserError.Log.d(TAG, "Notification for auth enabled");
                                    if (MiBand.isAuthenticated()) {
                                        connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthKeyRequest()) //get random key from band
                                                .subscribe(val -> {
                                                    if (d)
                                                        UserError.Log.d(TAG, "Wrote getAuthKeyRequest: " + JoH.bytesToHex(val));
                                                }, throwable -> {
                                                    UserError.Log.e(TAG, "Could not getAuthKeyRequest: " + throwable);
                                                });
                                    } else {
                                        connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthCommand())
                                                .subscribe(characteristicValue -> {
                                                            UserError.Log.d(TAG, "Wrote getAuthCommand, got: " + JoH.bytesToHex(characteristicValue));
                                                        },
                                                        throwable -> {
                                                            UserError.Log.e(TAG, "Could not write getAuthCommand: " + throwable);
                                                        }
                                                );
                                    }

                                }
                        )
                        .flatMap(notificationObservable -> notificationObservable)
                        .subscribe(bytes -> {
                            // incoming notifications
                            if (d)
                                UserError.Log.d(TAG, "Received auth notification bytes: " + bytesToHex(bytes));
                            ProcessAuthCommands(connection, bytes);
                            // changeNextState();
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in authSubscription Notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                // maybe legacy - ignore for now but needs better handling
                                UserError.Log.d(TAG, "Characteristic not found for notification");
                            } else if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                            } else if (throwable instanceof BleDisconnectedException) {
                                UserError.Log.d(TAG, "Disconnected while enabling notifications");
                            } else if (throwable instanceof TimeoutException) {
                                //check if it is normal timeout
                                if (!MiBand.isAuthenticated()) {
                                    String errorText = "MiBand authentication failed due to authentication timeout. When your Mi Band vibrates and blinks, tap it a few times in a row.";
                                    UserError.Log.d(TAG, errorText);
                                    JoH.static_toast_long(errorText);
                                }
                            }
                            if (authSubscription != null) {
                                authSubscription.unsubscribe();
                            }
                            changeState(CLOSE);
                            releaseWakeLock();
                        }));
    }

    @SuppressLint("CheckResult")
    private void ProcessAuthCommands(RxBleConnection connection, byte[] value) {
        if (value[0] == AUTH_RESPONSE &&
                value[1] == AUTH_SEND_KEY &&
                (value[2] & 0x0f) == AUTH_SUCCESS) {
            connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthKeyRequest()) //get random key from band
                    .subscribe(val -> {
                        if (d)
                            UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ1: " + JoH.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ1: " + throwable);
                    });
        } else if (value[0] == AUTH_RESPONSE &&
                (value[1] & 0x0f) == AUTH_REQUEST_RANDOM_AUTH_NUMBER &&
                value[2] == AUTH_SUCCESS) {
            byte[] tmpValue = Arrays.copyOfRange(value, 3, 19);
            try {
                byte[] authReply = authorisation.calculateAuthReply(tmpValue);
                connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authReply) //get random key from band
                        .subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ2: " + JoH.bytesToHex(val));
                        }, throwable -> {
                            UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ2: " + throwable);
                        });
            } catch (Exception e) {
                JoH.static_toast_long(e.getMessage());
                UserError.Log.e(TAG, (e.getMessage()));
                changeState(AUTHORIZE_FAILED);
            }
        } else if (value[0] == AUTH_RESPONSE &&
                (value[1] & 0x0f) == AUTH_SEND_ENCRYPTED_AUTH_NUMBER &&
                value[2] == AUTH_SUCCESS) {
            isNeedToAuthenticate = false;
            if (MiBand.getPersistentAuthMac().isEmpty()) {
                MiBand.setPersistentAuthMac(MiBand.getMac());
                MiBand.setPersistentAuthKey(JoH.bytesToHex(authorisation.getLocalKey()), MiBand.getPersistentAuthMac());
                String msg = "MiBand was successfully authenticated";
                JoH.static_toast_long(msg);
                UserError.Log.e(TAG, msg);
            }
            if (authSubscription != null) {
                authSubscription.unsubscribe();
            }
            changeNextState();
        } else if (value[0] == AUTH_RESPONSE &&
                (((value[2] & 0x0f) == AUTH_FAIL) || (value[2] == AUTH_MIBAND4_FAIL) || (value[2] == AUTH_MIBAND4_CODE_FAIL))) {
            MiBand.setPersistentAuthKey("", MiBand.getPersistentAuthMac());
            if (authSubscription != null) {
                authSubscription.unsubscribe();
            }
            String msg = "Cannot authorize miband, please recheck Auth code";
            JoH.static_toast_long(msg);
            UserError.Log.e(TAG, msg);
            changeState(AUTHORIZE_FAILED);
        }
    }

    @SuppressLint("CheckResult")
    private void installWatchface() {
        extendWakeLock(60 * Constants.SECOND_IN_MS);
        RxBleConnection connection = I.connection;
        if (d)
            UserError.Log.d(TAG, "Install WatchFace");
        if (I.connection == null) {
            if (d)
                UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        try {
            MiBandType mibandType = MiBand.getMibandType();
            WatchFaceGenerator wfGen = new WatchFaceGenerator(getBaseContext().getAssets(),
                    MiBand.getMibandType());
            byte[] fwArray = wfGen.genWatchFace(statusIOB);
            if (fwArray == null || fwArray.length == 0) {
                resetFirmwareState(false, "Empty image");
                return;
            }
            SequenceState sequenceState = null;
            if (mibandType == MiBandType.MI_BAND4) {
                sequenceState = new SequenceStateMiBand4();
            } else if (mibandType == MiBandType.MI_BAND5 || mibandType == MiBandType.AMAZFIT5) {
                sequenceState = new SequenceStateMiBand5();
            } else {
                resetFirmwareState(false, "Not supported band type");
                return;
            }
            firmware = new FirmwareOperations(fwArray, sequenceState);
        } catch (Exception e) {
            resetFirmwareState(false, "FirmwareOperations error " + e.getMessage());
            return;
        }
        if (d)
            UserError.Log.d(TAG, "Begin uploading Watchface, lenght: " + firmware.getSize());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (d)
                UserError.Log.d(TAG, "Requesting high priority connection");
            requestConnectionPriority(connection, BluetoothGatt.CONNECTION_PRIORITY_HIGH);
        }
        firmware.nextSequence();
        processFirmwareSequence();
    }

    private synchronized void processFirmwareSequence() {
        RxBleConnection connection = I.connection;

        String seq = firmware.getSequence();
        if (d)
            UserError.Log.d(TAG, "processFirmwareSequence seq:" + seq.toString());
        switch (seq) {
            case SequenceState.SET_NIGHTMODE: {
                isNeedToRestoreNightMode = true;
                DisplayControllMessageMiband3_4 dispControl = new DisplayControllMessageMiband3_4();
                Calendar sheduledCalendar = Calendar.getInstance();
                sheduledCalendar.set(Calendar.HOUR_OF_DAY, 0);
                sheduledCalendar.set(Calendar.MINUTE, 0);
                Date sheduledDate = sheduledCalendar.getTime();
                connection.writeCharacteristic(dispControl.getCharacteristicUUID(), dispControl.setNightModeCmd(Sheduled, sheduledDate, sheduledDate))
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote nigntmode: " + JoH.bytesToHex(valB));
                                    firmware.nextSequence();
                                    processFirmwareSequence();
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write nigntmode: " + throwable);
                                    firmware.nextSequence();
                                    processFirmwareSequence();
                                }
                        );
                break;
            }

            case SequenceState.PREPARE_UPLOAD: {
                firmware.nextSequence();
                connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.prepareFWUploadInitCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote prepareFWUploadInitCommand: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write prepareFWUploadInitCommand: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }

            case SequenceState.TRANSFER_FW_START: {
                connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.getFirmwareStartCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote Start command: " + JoH.bytesToHex(valB));
                                    firmware.nextSequence();
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write Start command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceState.TRANSFER_SEND_WF_INFO: {
                firmware.nextSequence();
                connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.getFwInfoCommand())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote getFwInfoCommand: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write firmware info: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }

            case SequenceStateMiBand5.UNKNOWN_REQUEST: {
                firmware.nextSequence();
                connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.getUnknownMiBand5Command())
                        .subscribe(valB -> {
                                    UserError.Log.d(TAG, "Wrote getUnknownMiBand5Command: " + JoH.bytesToHex(valB));
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write getUnknownMiBand5Command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceStateMiBand5.UNKNOWN_INIT_COMMAND: {
                connection.writeCharacteristic(Const.UUID_CHARACTERISTIC_3_CONFIGURATION, OperationCodes.COMMAND_MIBAND5_UNKNOW_INIT)
                        .subscribe(val -> {
                                    UserError.Log.d(TAG, "Wrote miband5 unknown init command: " + JoH.bytesToHex(val));
                                    firmware.nextSequence();
                                    processFirmwareSequence();
                                },
                                throwable -> {
                                    UserError.Log.e(TAG, "Could not write miband5 unknown init command: " + throwable);
                                    resetFirmwareState(false);
                                }
                        );
                break;
            }
            case SequenceState.NOTIFICATION_ENABLE: {
                watchfaceSubscription = new Subscription(
                        connection.setupNotification(firmware.getFirmwareCharacteristicUUID())
                                .timeout(60, TimeUnit.SECONDS) // WARN
                                .doOnNext(notificationObservable -> {
                                            if (d)
                                                UserError.Log.d(TAG, "Notification for firmware enabled");
                                            firmware.nextSequence();
                                            processFirmwareSequence();
                                        }
                                )
                                .flatMap(notificationObservable -> notificationObservable)
                                .subscribe(bytes -> {
                                    // incoming notifications
                                    if (d)
                                        UserError.Log.d(TAG, "Received firmware notification bytes: " + bytesToHex(bytes));
                                    processFirmwareNotifications(bytes);
                                }, throwable -> {
                                    UserError.Log.d(TAG, "Throwable in firmware notification: " + throwable);
                                    if (throwable instanceof BleCharacteristicNotFoundException) {
                                        // maybe legacy - ignore for now but needs better handling
                                        UserError.Log.d(TAG, "Characteristic not found for notification");
                                    } else if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                        UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                                    } else if (throwable instanceof BleDisconnectedException) {
                                        UserError.Log.d(TAG, "Disconnected while enabling notifications");
                                    } else if (throwable instanceof TimeoutException) {
                                        UserError.Log.d(TAG, "Timeout");
                                    }
                                    resetFirmwareState(false);
                                }));
                break;
            }
        }
    }

    @SuppressLint("CheckResult")
    private void processFirmwareNotifications(byte[] value) {
        if (d)
            UserError.Log.d(TAG, "processFirmwareNotifications: " + bytesToHex(value));

        if (value.length != 3 && value.length != 11) {
            UserError.Log.e(TAG, "Notifications should be 3 or 11 bytes long.");
            return;
        }
        boolean success = value[2] == OperationCodes.SUCCESS;
        String seq = firmware.getSequence();
        if (value[0] == OperationCodes.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case OperationCodes.COMMAND_FIRMWARE_INIT: {
                        if (seq == SequenceState.WAITING_PREPARE_UPLOAD_RESPONSE ||
                                seq == SequenceState.WAITING_TRANSFER_SEND_WF_INFO_RESPONSE) {
                            firmware.nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_START_DATA: {
                        sendFirmwareData();
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_CHECKSUM: {
                        if (seq == SequenceState.WAITING_SEND_CHECKSUM_RESPONSE) {
                            firmware.nextSequence();
                            if (firmware.getFirmwareType() == FirmwareOperations.FirmwareType.FIRMWARE) {
                                //send reboot
                            } else {
                                UserError.Log.e(TAG, "Watch Face has been installed successfully");
                                changeNextState();
                                resetFirmwareState(true);
                            }

                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_REBOOT: {
                        UserError.Log.e(TAG, "Reboot command successfully sent.");
                        resetFirmwareState(true);
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_UNKNOWN_MIBAND5: {
                        if (seq == SequenceStateMiBand5.WAITING_UNKNOWN_REQUEST_RESPONCE) {
                            firmware.nextSequence();
                            processFirmwareSequence();
                        }
                        break;
                    }
                    default: {
                        resetFirmwareState(false, "Unexpected response during firmware update");
                    }
                }
            } catch (Exception ex) {
                resetFirmwareState(false);
            }
        } else {
            String errorMessage = null;
            Boolean sendBGNotification = false;
            if (value[2] == OperationCodes.LOW_BATTERY_ERROR) {
                errorMessage = "Cannot upload watchface, low battery, please charge device";
                sendBGNotification = true;
            } else if (value[2] == OperationCodes.TIMER_RUNNING) {
                errorMessage = "Cannot upload watchface, timer running on band";
            } else if (value[2] == OperationCodes.ON_CALL) {
                errorMessage = "Cannot upload watchface, call in progress";
            } else {
                errorMessage = "Unexpected notification during firmware update:" + JoH.bytesToHex(value);
            }
            resetFirmwareState(false, errorMessage);
            if (sendBGNotification) {
                JoH.startService(MiBandService.class, "function", "update_bg_as_notification");
                changeState(SLEEP);
            }
        }
    }

    private void resetFirmwareState(Boolean result) {
        resetFirmwareState(result, null);
    }

    private void resetFirmwareState(Boolean result, String customText) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestConnectionPriority(I.connection, BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
        }
        emptyQueue();
        if (watchfaceSubscription != null) {
            watchfaceSubscription.unsubscribe();
            watchfaceSubscription = null;
        }
        String finishText = customText;
        if (customText == null) {
            if (!result)
                finishText = xdrip.getAppContext().getResources().getString(R.string.miband_watchface_istall_error);
            else
                finishText = xdrip.getAppContext().getResources().getString(R.string.miband_watchface_istall_success);
        }
        UserError.Log.d(TAG, "resetFirmwareState result:" + result + ":" + finishText);

        if (!result) {
            prevReadingStatusIsStale = false; //try to resend readings on the next bg update
        }

        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || I.isConnected == false) return;
        changeState(MiBandState.RESTORE_NIGHTMODE);
    }

    @RequiresApi(26)
    private Observable<ConnectionParameters> requestConnectionPriority(RxBleConnection rxBleConnection, int priority) {
        return Observable.merge(
                rxBleConnection.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH, 1, TimeUnit.MILLISECONDS).toObservable(),
                rxBleConnection.observeConnectionParametersUpdates().take(1)
        );
    }

    private void sendFirmwareData() {
        byte[] fwbytes = firmware.getBytes();
        int len = firmware.getSize();
        int mtu = I.connection.getMtu();
        if (!MiBandEntry.isNeedToDisableHightMTU())
            firmware.setMTU(mtu);

        final int packetLength = firmware.getPackeLenght();
        if (d)
            UserError.Log.d(TAG, "Firmware packet lengh: " + packetLength);
        int packets = len / packetLength;

        // going from 0 to len
        int firmwareProgress = 0;
        for (int i = 0; i < packets; i++) {
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);
            int finalI = i;
            I.connection.writeCharacteristic(firmware.getFirmwareDataCharacteristicUUID(), fwChunk).subscribe(val -> {
                        if (d)
                            UserError.Log.d(TAG, "Wrote Chunk:" + finalI);
                    },
                    throwable -> {
                        if (d)
                            UserError.Log.e(TAG, "Could not write fwChunk: " + throwable);
                        resetFirmwareState(false);
                    }
            );
            firmwareProgress += packetLength;
            int progressPercent = (int) ((((float) firmwareProgress) / len) * 100);
            if ((i > 0) && (i % FirmwareOperations.FIRMWARE_SYNC_PACKET == 0)) {
                I.connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.getSyncCommand()).subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote Sync" + progressPercent + "%");
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write Sync: " + throwable);
                            resetFirmwareState(false);
                        }
                );
            }
        }
        if (firmwareProgress < len) { //last chunk
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
            I.connection.writeCharacteristic(firmware.getFirmwareDataCharacteristicUUID(), fwChunk)
                    .subscribe(val -> {
                                if (d)
                                    UserError.Log.d(TAG, "Wrote last fwChunk");
                            },
                            throwable -> {
                                if (d)
                                    UserError.Log.e(TAG, "Could not write last fwChunk: " + throwable);
                                resetFirmwareState(false);
                            }
                    );
        }

        firmware.nextSequence();
        I.connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.getChecksumCommand())
                .subscribe(val -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote getChecksumCommand");
                            firmware.nextSequence();
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write getChecksumCommand: " + throwable);
                            resetFirmwareState(false);
                        }
                );
    }

    private void setNightMode() {
        if (d)
            UserError.Log.d(TAG, "Restore night mode");
        Date start = null, end = null;
        DisplayControllMessageMiband3_4.NightMode nightMode = DisplayControllMessageMiband3_4.NightMode.Off;
        if (MiBandEntry.isNightModeEnabled()) {
            nightMode = DisplayControllMessageMiband3_4.NightMode.Sheduled;
            start = MiBandEntry.getNightModeStart();
            end = MiBandEntry.getNightModeEnd();
        }
        RxBleConnection connection = I.connection;
        DisplayControllMessageMiband3_4 dispControl = new DisplayControllMessageMiband3_4();
        connection.writeCharacteristic(dispControl.getCharacteristicUUID(), dispControl.setNightModeCmd(nightMode, start, end))
                .subscribe(valB -> {
                            if (d)
                                UserError.Log.d(TAG, "Wrote nightmode");
                            isNeedToRestoreNightMode = false;
                        },
                        throwable -> {
                            if (d)
                                UserError.Log.e(TAG, "Could not write nightmode: " + throwable);
                        }
                );
    }

    private void handleHeartrate(byte[] value) {
        if (value.length == 2 && value[0] == 0) {
            int hrValue = (value[1] & 0xff);
            if (d)
                UserError.Log.d(TAG, "heart rate: " + hrValue);
            HeartRate.create(JoH.tsl(), hrValue, 1);
        }
    }

    private void handleRealtimeSteps(byte[] value) {
        if (value == null) {
            return;
        }
        if (value.length == 13) {
            byte[] stepsValue = new byte[] {value[1], value[2]};
            int steps = FirmwareOperations.toUint16(stepsValue);
            if (d)
                UserError.Log.d(TAG, "realtime steps: " + steps);
            StepCounter.createEfficientRecord(JoH.tsl(), steps);
        } else {
            UserError.Log.d(TAG, "Unrecognized realtime steps value: " + bytesToHex(value));
        }
    }

    @SuppressLint("CheckResult")
    private void enableNotifications() {
        if (d)
            UserError.Log.d(TAG, "enableNotifications called");
        if (I.connection == null) {
            if (d)
                UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        if (I.isNotificationEnabled) {
            if (d)
                UserError.Log.d(TAG, "Notifications already enabled");
            changeNextState();
            return;
        }

        enableHeartRateNotification();
        enableStepsNotification();

        if (notifSubscriptionDeviceEvent != null) {
            notifSubscriptionDeviceEvent.unsubscribe();
        }
        if (d)
            UserError.Log.d(TAG, "Requesting to enable device event notifications");

        I.connection.requestMtu(PREFERRED_MTU_SIZE).subscribe();

        notifSubscriptionDeviceEvent = new Subscription(I.connection.setupNotification(Const.UUID_CHARACTERISTIC_DEVICEEVENT)
            .doOnNext(notificationObservable -> {
                I.isNotificationEnabled = true;
                changeNextState();
            }).flatMap(notificationObservable -> notificationObservable)
            //.timeout(5, TimeUnit.SECONDS)
            .observeOn(Schedulers.newThread())
            .subscribe(bytes -> {
                    // incoming notifications
                    if (d)
                        UserError.Log.d(TAG, "Received device notification bytes: " + bytesToHex(bytes));
                    handleDeviceEvent(bytes);
                }, throwable -> {
                    UserError.Log.d(TAG, "Throwable in notifSubscriptionDeviceEvent notification: " + throwable);
                    I.isNotificationEnabled = false;
                    if (throwable instanceof BleCharacteristicNotFoundException) {
                        // maybe legacy - ignore for now but needs better handling
                        UserError.Log.d(TAG, "Characteristic not found for notification");
                        changeNextState();
                    } else {
                        UserError.Log.d(TAG, "Disconnected exception");
                        isNeedToAuthenticate = true;
                        messageQueue.clear();
                        changeState(CLOSE);
                    }
                }
            ));
    }

    private void enableHeartRateNotification() {
        if (MiBandEntry.isNeedToCollectHR()) {
            if (notifSubscriptionHeartRateMeasurement != null) return;
        } else {
            if (notifSubscriptionHeartRateMeasurement != null) {
                notifSubscriptionHeartRateMeasurement.unsubscribe();
                notifSubscriptionHeartRateMeasurement = null;
                return;
            }
        }

        if (d)
            UserError.Log.d(TAG, "Requesting to enable HR notifications");

        notifSubscriptionHeartRateMeasurement = new Subscription(I.connection.setupNotification(Const.UUID_CHAR_HEART_RATE_MEASUREMENT)
            .flatMap(notificationObservable -> notificationObservable)
            .observeOn(Schedulers.newThread())
            .subscribe(bytes -> {
                    // incoming notifications
                    if (d)
                        UserError.Log.d(TAG, "Received HR notification bytes: " + bytesToHex(bytes));
                    handleHeartrate(bytes);
                }, throwable -> {
                    notifSubscriptionHeartRateMeasurement.unsubscribe();
                    notifSubscriptionHeartRateMeasurement = null;
                    UserError.Log.d(TAG, "Throwable in HR notification: " + throwable);
                    if (throwable instanceof BleCharacteristicNotFoundException) {
                        UserError.Log.d(TAG, "HR Characteristic not found for notification");
                    } else {
                        UserError.Log.d(TAG, "HR Disconnected exception");
                    }
                }
            ));
    }

    private void enableStepsNotification() {
        if (MiBandEntry.isNeedToCollectSteps()) {
            if (notifSubscriptionStepsMeasurement != null) return;
        } else {
            if (notifSubscriptionStepsMeasurement != null) {
                notifSubscriptionStepsMeasurement.unsubscribe();
                notifSubscriptionStepsMeasurement = null;
                return;
            }
        }

        if (d)
            UserError.Log.d(TAG, "Requesting to enable steps notifications");

        notifSubscriptionStepsMeasurement = new Subscription(I.connection.setupNotification(Const.UUID_CHARACTERISTIC_7_REALTIME_STEPS)
            .flatMap(notificationObservable -> notificationObservable)
            .observeOn(Schedulers.newThread())
            .subscribe(bytes -> {
                    // incoming notifications
                    if (d)
                        UserError.Log.d(TAG, "Received steps notification bytes: " + bytesToHex(bytes));
                    handleRealtimeSteps(bytes);
                }, throwable -> {
                notifSubscriptionStepsMeasurement.unsubscribe();
                notifSubscriptionStepsMeasurement = null;
                    UserError.Log.d(TAG, "Throwable in steps notification: " + throwable);
                    if (throwable instanceof BleCharacteristicNotFoundException) {
                        UserError.Log.d(TAG, "steps Characteristic not found for notification");
                    } else {
                        UserError.Log.d(TAG, "steps Disconnected exception");
                    }
                }
            ));
    }

    @Override
    protected synchronized boolean automata() {
        if (d)
            UserError.Log.d(TAG, "Automata called in" + TAG);
        extendWakeLock(2000);
        if (shouldServiceRun()) {
            switch (I.state) {
                case INIT:
                    // connect by default
                    changeNextState();
                    break;
                case MiBandState.GET_MODEL_NAME:
                    cancelRetryTimer();
                    if (isNeedToRestoreNightMode) {
                        setNightMode();
                    }
                    if (MiBand.getModel().isEmpty()) {
                        getModelName();
                    } else changeNextState();
                    break;
                case MiBandState.GET_SOFT_REVISION:
                    if (MiBand.getVersion().isEmpty() || isNeedToCheckRevision)
                        getSoftwareRevision();
                    else changeNextState();
                    break;
                case MiBandState.AUTHENTICATE:
                    if (isNeedToAuthenticate) {
                        changeNextState();
                    } else {
                        changeState(MiBandState.ENABLE_NOTIFICATIONS);
                    }
                    break;
                case MiBandState.AUTHORIZE:
                    authPhase();
                    break;
                case MiBandState.ENABLE_NOTIFICATIONS:
                    enableNotifications();
                    break;
                case MiBandState.SEND_SETTINGS:
                    sendSettings();
                    changeNextState();
                    break;
                case MiBandState.SEND_BG:
                    if (!MiBandEntry.isNeedSendReading()) {
                        changeState(MiBandState.SEND_QUEUE);
                        break;
                    }

                    final String bgAsNotification = queueItem.functionName;
                    if (!MiBandType.supportPairingKey(MiBand.getMibandType())
                            || MiBandEntry.isNeedSendReadingAsNotification()
                            || bgAsNotification.equals("update_bg_as_notification")) {
                        Boolean result = sendBG();
                        if (result) changeState(MiBandState.VIBRATE_AFTER_READING);
                        else changeState(MiBandState.SEND_QUEUE);
                        break;
                    }
                    changeState(MiBandState.INSTALL_WATCHFACE);
                    break;
                case MiBandState.INSTALL_WATCHFACE:
                    installWatchface();
                    changeNextState();
                    break;
                case MiBandState.INSTALL_WATCHFACE_IN_PROGRESS:
                    break;
                case MiBandState.INSTALL_WATCHFACE_FINISHED:
                    break;
                case MiBandState.RESTORE_NIGHTMODE:
                    if (isNeedToRestoreNightMode) {
                        // do nothing because something happen with connection while sending nightmode
                        extendWakeLock(RESTORE_NIGHT_MODE_DELAY + Constants.SECOND_IN_MS);
                        JoH.threadSleep(RESTORE_NIGHT_MODE_DELAY);
                        setNightMode();
                        if (I.state.equals(CLOSED) || I.state.equals(CLOSE) || I.isConnected == false)
                            break;
                    }
                    changeNextState();
                    break;
                case MiBandState.VIBRATE_AFTER_READING:
                    if (MiBandEntry.isVibrateOnReadings() && !MiBandEntry.isNeedSendReadingAsNotification())
                        vibrateAlert(AlertLevelMessage.AlertLevelType.VibrateAlert);
                    changeNextState();
                    break;
                case MiBandState.GET_BATTERY_INFO:
                    getBatteryInfo();
                    changeNextState();
                    break;
                case MiBandState.QUEUE_MESSAGE:
                    queueMessage();
                    break;
                case MiBandState.WAITING_USER_RESPONSE:
                    break;
                case SLEEP:
                    handleCommand();
                    break;
                case CLOSED:
                    stopConnection();
                    return super.automata();
                default:
                    return super.automata();
            }
        } else {
            UserError.Log.d(TAG, "Service should not be running inside automata");
            stopSelf();
        }
        return true; // lies
    }

    private void stopConnection() {
        isNeedToAuthenticate = true;
        isWaitingCallResponce = false;
        messageQueue.clear();
        setRetryTimerReal(); // local retry strategy
    }

    @Override
    public void resetBluetoothIfWeSeemToAlreadyBeConnected(String mac) {
        //super.resetBluetoothIfWeSeemToAlreadyBeConnected(mac); //do not reset
    }

    private boolean shouldServiceRun() {
        return MiBandEntry.isEnabled();
    }

    @Override
    protected void setRetryTimerReal() {
        if (shouldServiceRun() && MiBand.isAuthenticated()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimerReal: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            I.serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_RETRY_ID, "message");
            I.retry_time = JoH.wakeUpIntent(xdrip.getAppContext(), retry_in, I.serviceIntent);
            I.wakeup_time = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void cancelRetryTimer() {
        JoH.cancelAlarm(xdrip.getAppContext(), I.serviceIntent);
        I.wakeup_time = 0;
    }

    private long whenToRetryNext() {
        I.retry_backoff = RETRY_PERIOD_MS;
        return I.retry_backoff;
    }

    public enum MIBAND_INTEND_STATES {
        UPDATE_PREF_SCREEN,
        UPDATE_PREF_DATA
    }

    static class MiBandState extends JamBaseBluetoothSequencer.BaseState {
        static final String SEND_BG = "Setting Time";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Queue message";
        static final String WAITING_USER_RESPONSE = "WAITING_USER_RESPONSE";
        static final String AUTHENTICATE = "Authenticate";
        static final String AUTHORIZE = "Authorize phase";
        static final String AUTHORIZE_FAILED = "Authorization failed";
        static final String GET_MODEL_NAME = "Getting model name";
        static final String GET_SOFT_REVISION = "Getting software revision";
        static final String ENABLE_NOTIFICATIONS = "Enable notification";
        static final String GET_BATTERY_INFO = "Getting battery info";
        static final String INSTALL_WATCHFACE = "Watchface installation";
        static final String INSTALL_WATCHFACE_IN_PROGRESS = "Watchface installation in progress";
        static final String INSTALL_WATCHFACE_FINISHED = "Watchface installation finished";
        static final String RESTORE_NIGHTMODE = "RESTORE_NIGHTMODE";
        static final String VIBRATE_AFTER_READING = "Vibrate";

        private static final String TAG = "MiBandStateSequence";

        void prepareInitialSequences() {
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(GET_MODEL_NAME);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(AUTHENTICATE);
            sequence.add(AUTHORIZE);
            sequence.add(ENABLE_NOTIFICATIONS);
        }

        void prepareFinalSequences() {
            sequence.add(SEND_QUEUE);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(SLEEP);
            sequence.add(AUTHORIZE_FAILED);
        }

        void setSendReadingSequence() {
            UserError.Log.d(TAG, "SET UPDATE WATCHFACE DATA SEQUENCE");
            prepareInitialSequences();
            sequence.add(SEND_BG);
            sequence.add(INSTALL_WATCHFACE);
            sequence.add(INSTALL_WATCHFACE_IN_PROGRESS);
            sequence.add(INSTALL_WATCHFACE_FINISHED);
            sequence.add(RESTORE_NIGHTMODE);
            sequence.add(VIBRATE_AFTER_READING);
            prepareFinalSequences();
        }

        void setAlarmSequence() {
            UserError.Log.d(TAG, "SET ALARM SEQUENCE");
            prepareInitialSequences();
            sequence.add(QUEUE_MESSAGE);
            sequence.add(SEND_QUEUE);
            sequence.add(WAITING_USER_RESPONSE);
            sequence.add(SLEEP);
            sequence.add(AUTHORIZE_FAILED);
        }

        void setQueueSequence() {
            UserError.Log.d(TAG, "SET QUEUE SEQUENCE");
            prepareInitialSequences();
            sequence.add(QUEUE_MESSAGE);
            prepareFinalSequences();
        }

        void setSettingsSequence() {
            UserError.Log.d(TAG, "SET SETTINGS SEQUENCE");
            prepareInitialSequences();
            sequence.add(SEND_SETTINGS);
            prepareFinalSequences();
        }
    }

    public class QueueMessage {
        @Getter
        private String functionName;
        @Getter
        private String message_type = "";
        @Getter
        private String message = "";
        @Getter
        private String title = "";
        @Getter
        private long expireAt;

        public QueueMessage(String functionName) {
            this.functionName = functionName;
        }

        public QueueMessage(String functionName, String message_type, String message, String title) {
            this(functionName);
            this.message_type = message_type;
            this.message = message;
            this.title = title;
            this.expireAt = JoH.tsl() + (Constants.MINUTE_IN_MS * 10);
        }

        boolean isExpired() {
            return expireAt != 0 && expireAt < JoH.tsl();
        }
    }
}
