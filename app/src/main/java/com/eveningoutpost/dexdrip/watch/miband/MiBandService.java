package com.eveningoutpost.dexdrip.watch.miband;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Pair;

import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.store.KeyStore;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.utils.framework.IncomingCallsReceiver;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.watch.PrefBindingFactory;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertLevelMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AuthMessages;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.FeaturesControllMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.TimeMessage;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.eveningoutpost.dexdrip.Models.ActiveBgAlert.currentlyAlerting;
import static com.eveningoutpost.dexdrip.Models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.Models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_NOTIFY_RESPONSE1;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_NOTIFY_RESPONSE2;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_NOTIFY_RESPONSE_ERROR;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.OPCODE_AUTH_NOTIFY_RESPONSE_SUCCESS;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.isCommandEqual;

/**
 * Jamorham
 * <p>
 * Data communication with MiBand compatible bands/watches
 */

public class MiBandService extends JamBaseBluetoothSequencer {

    private static final String MESSAGE = "miband-Message";
    private static final String MESSAGE_TYPE = "miband-Message-Type";
    private final KeyStore keyStore = FastStore.getInstance();
    private static final boolean d = true;
    private static final long MAX_RETRY_BACKOFF_MS = Constants.SECOND_IN_MS * 300; // sleep for max ms if we have had no signal
    private static final int QUEUE_EXPIRED_TIME = 30; //second
    private static final int QUEUE_DELAY = 50; //ms
    private static final long BG_UPDATE_INTERVAL = 10 * Constants.MINUTE_IN_MS; //seconds *1k
    private Subscription notificationSubscription;
    private Subscription authSubscription;
    private AuthMessages authorisation;
    private Boolean isNeedToCheckRevision = true;
    public static Timer bgUpdateTimer;

    final Runnable canceller = () -> {
        if (!currentlyAlerting() && !IncomingCallsReceiver.isRingingNow()) {
            UserError.Log.d(TAG, "Clearing queue as alert / call ceased");
            emptyQueue();
        }
    };
    ;

    {
        mState = new MiBandState().setLI(I);
        I.queue_write_characterstic = new AlertMessage().getCharacteristicUUID();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("Miband service", 60000);
        try {
            if (shouldServiceRun()) {
                final String authMac = MiBand.getAuthMac();
                String mac = MiBand.getMac();
                if (!authMac.equals(mac)) {
                    MiBand.setAuthMac("");
                }
                if (emptyString(mac)) {
                    // if mac not set then start up a scan and do nothing else
                    new FindNearby().scan();
                } else {
                    setAddress(mac);
                    if (intent != null) {
                        final String function = intent.getStringExtra("function");
                        if (function != null) {
                            switch (function) {
                                case "refresh":
                                    changeState(MiBandState.INIT);
                                    break;
                                case "message":
                                    final String message = intent.getStringExtra("message");
                                    final String message_type = intent.getStringExtra("message_type");
                                    if (message != null) {
                                        keyStore.putS(MESSAGE, message);
                                        keyStore.putS(MESSAGE_TYPE, message_type != null ? message_type : "");
                                        changeState(MiBandState.QUEUE_MESSAGE);
                                    }
                                    break;
                                case "set_time":
                                    changeState(MiBandState.SET_TIME);
                                    break;
                            }
                        } else {
                            // no specific function
                        }
                    }
                }
                if (MiBandEntry.isNeedSendReading()) {
                    if (bgUpdateTimer == null) {
                        bgUpdateTimer = new Timer();
                        bgUpdateTimer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                if (I.state.equals(MiBandState.SLEEP) || I.state.equals(MiBandState.CLOSED))
                                    changeState(MiBandState.SET_TIME);
                            }
                        }, BG_UPDATE_INTERVAL, BG_UPDATE_INTERVAL);
                    }
                } else {
                    stopUpdateTimer();
                }
                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                stopUpdateTimer();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void stopUpdateTimer() {
        if (bgUpdateTimer != null) {
            bgUpdateTimer.cancel();
            bgUpdateTimer = null;
        }
    }

    static final List<UUID> huntCharacterstics = new ArrayList<>();

    static {
        huntCharacterstics.add(Const.UUID_CHAR_NEW_ALERT);
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
                        I.readCharacteristic = check;
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
        if (isNeedToCheckRevision) { //check only once
            I.connection.readCharacteristic(Const.UUID_CHAR_SOFTWARE_REVISION_STRING).subscribe(
                    readValue -> {
                        String revision = new String(readValue);
                        UserError.Log.d(TAG, "Got software revision: " + revision);
                        MiBand.setVersion(revision);
                        isNeedToCheckRevision = false;
                        changeNextState();
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not read software revision: " + throwable);
                        changeNextState();
                    });
        } else changeNextState();
    }

    @SuppressLint("CheckResult")
    private void getModelName() {
        I.connection.readCharacteristic(Const.UUID_CHAR_DEVICE_NAME).subscribe(
                readValue -> {
                    String name = new String(readValue);
                    UserError.Log.d(TAG, "Got device name: " + name);
                    MiBand.setModel(name);
                    changeNextState();
                }, throwable -> {
                    UserError.Log.e(TAG, "Could not read device name: " + throwable);
                    changeNextState();
                });
    }

    private void sendBG() {
        final BgReading last = BgReading.last();
        FunAlmanac.Reply rep;
        if (last == null || last.isStale()) {
            rep = FunAlmanac.getRepresentation(0, "Flat");
        } else {
            final double mmol_value = roundDouble(mmolConvert(last.getDg_mgdl()), 1);

            rep = FunAlmanac.getRepresentation(mmol_value, last.slopeName());
        }
        TimeMessage message = new TimeMessage();
        new QueueMe()
                .setBytes(message.getTimeMessage(rep.timestamp))
                .setDescription("Send time representation for: " + rep.input)
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(50)
                .queue();
    }

    private void vibrateAlert(AlertLevelMessage.AlertLevelType level) {
        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getAlertLevelMessage(level))
                .setDescription("Send vibrateAlert: " + level)
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(50)
                .queue();
    }

    private void periodicVibrateAlert(int count, int activeVibrationTime, int pauseVibrationTime) {
        AlertLevelMessage message = new AlertLevelMessage();
        new QueueMe()
                .setBytes(message.getPeriodicVibrationMessage((byte) count, (short) activeVibrationTime, (short) pauseVibrationTime))
                .setDescription(String.format("Send periodicVibrateAlert c:%d a:$d p:%d" + count, activeVibrationTime, pauseVibrationTime))
                .setQueueWriteCharacterstic(message.getCharacteristicUUID())
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(QUEUE_DELAY)
                .queue();
    }

    private void sendSettings() {
        List<Pair<Integer, Boolean>> features = PrefBindingFactory.getInstance(MibandPrefBinding.class).getStates("miband_feature_");
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

        List<Integer> screenOpt = PrefBindingFactory.getInstance(MibandPrefBinding.class).getEnabled("miband_screen");
        DisplayControllMessage dispMessage = new DisplayControllMessage();
        new QueueMe()
                .setBytes(dispMessage.getDisplayItemsCmd(screenOpt))
                .setQueueWriteCharacterstic(dispMessage.getCharacteristicUUID())
                .setDescription("Set screens")
                .expireInSeconds(QUEUE_EXPIRED_TIME)
                .setDelayMs(QUEUE_DELAY)
                .queue();
    }

    private void queueMessage() {
        final String alert = keyStore.getS(MESSAGE);
        final String type = keyStore.getS(MESSAGE_TYPE);

        UserError.Log.d(TAG, "Queuing message alert of type: " + type + " " + alert);

        if (!emptyString(alert)) {

            switch (type != null ? type : "null") {
                case "call":
                    new QueueMe()
                            .setBytes(new AlertMessage().getAlertMessage(alert, AlertMessage.AlertType.Call))
                            .setDescription("Send call alert: " + alert)
                            .expireInSeconds(60)
                            .setDelayMs(5000)
                            .setRunnable(canceller)
                            .queue();
                    UserError.Log.d(TAG, "Queued call alert: " + alert);
                    break;

                default: // glucose
                    for (int repeats = 0; repeats < 5; repeats++) {
                        new QueueMe()
                                .setBytes(new AlertMessage().getAlertMessage(alert, AlertMessage.AlertType.SMS_MMS))
                                .setDescription("Send alert: " + alert)
                                .expireInSeconds(60)
                                .setDelayMs(200)
                                .queue();
                    }
                    break;
            }
            // this parent method might get called multiple times
            Inevitable.task("miband-s-queue", 200, () -> changeState(mState.next()));
        } else {
            UserError.Log.e(TAG, "Alert message requested but no message set");
        }
    }

    @SuppressLint("CheckResult")
    private void authPhase() {
        RxBleConnection connection = I.connection;
        UserError.Log.d(TAG, "Authorizing");
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }

        UserError.Log.d(TAG, "Requesting to enable notifications for auth");
        authorisation = new AuthMessages();
        authSubscription = new Subscription(
                connection.setupNotification(authorisation.getCharacteristicUUID())
                        .timeout(15, TimeUnit.SECONDS) // WARN
                        // .observeOn(Schedulers.newThread()) // needed?
                        .doOnNext(notificationObservable -> {
                                    UserError.Log.d(TAG, "Notification for auth enabled");
                                    connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthCommand())
                                            .subscribe(characteristicValue -> {
                                                        UserError.Log.d(TAG, "Wrote AuthRequestTX, got: " + JoH.bytesToHex(characteristicValue));
                                                    },
                                                    throwable -> {
                                                        UserError.Log.e(TAG, "Could not write AuthRequestTX: " + throwable);
                                                    }
                                            );
                                }
                        )
                        .flatMap(notificationObservable -> notificationObservable)
                        //.timeout(5, TimeUnit.SECONDS)
                        //.observeOn(Schedulers.newThread())
                        .subscribe(bytes -> {
                            // incoming notifications
                            UserError.Log.d(TAG, "Received notification bytes: " + bytesToHex(bytes));
                            ProcessAuthCommands(connection, bytes);
                            // changeNextState();
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in Record Notification: " + throwable);
                            if (throwable instanceof BleCharacteristicNotFoundException) {
                                // maybe legacy - ignore for now but needs better handling
                                UserError.Log.d(TAG, "Characteristic not found for notification");
                                changeState(CLOSE);
                            } else if (throwable instanceof BleCannotSetCharacteristicNotificationException) {
                                UserError.Log.e(TAG, "Problems setting notifications - disconnecting");
                                changeState(CLOSE);
                            } else if (throwable instanceof BleDisconnectedException) {
                                UserError.Log.d(TAG, "Disconnected while enabling notifications");
                                changeState(CLOSE);
                            } else if (throwable instanceof TimeoutException) {
                                //check if it is normal timeout
                                if (!MiBand.isAuthenticated()) {
                                    UserError.Log.d(TAG, "Timeout");
                                    changeState(CLOSE);
                                }
                            }

                        }));
    }

    @SuppressLint("CheckResult")
    private void ProcessAuthCommands(RxBleConnection connection, byte[] value) {
        if (isCommandEqual(OPCODE_AUTH_NOTIFY_RESPONSE1, value)) {
            connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthKeyRequest()) //get random key from band
                    .subscribe(val -> {
                        UserError.Log.d(TAG, "Wrote auth request: " + JoH.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not write auth request: " + throwable);
                    });
        } else if (isCommandEqual(OPCODE_AUTH_NOTIFY_RESPONSE2, value)) {
            byte[] tmpValue = Arrays.copyOfRange(value, 3, 19);
            byte[] authReply = authorisation.calculateAuthReply(tmpValue);
            connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authReply) //get random key from band
                    .subscribe(val -> {
                        UserError.Log.d(TAG, "Wrote auth key: " + JoH.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not write auth key: " + throwable);
                    });
        } else if (isCommandEqual(OPCODE_AUTH_NOTIFY_RESPONSE_SUCCESS, value)) {
            MiBand.setAuthMac(MiBand.getMac());
            if (authSubscription != null) {
                authSubscription.unsubscribe();
            }
            changeNextState();
        } else if (isCommandEqual(OPCODE_AUTH_NOTIFY_RESPONSE_ERROR, value)) {
            MiBand.setAuthMac("");
            if (authSubscription != null) {
                authSubscription.unsubscribe();
            }
            changeState(CLOSE);
        }
    }

    @Override
    protected synchronized boolean automata() {
        extendWakeLock(1000);
        UserError.Log.d(TAG, "Automata called in" + TAG);

        if (shouldServiceRun()) {

            switch (I.state) {

                case MiBandState.INIT:
                    // connect by default
                    emptyQueue();
                    changeNextState();
                    break;
                case MiBandState.AUTHENTICATE:
                    if (MiBand.isAuthenticated()) {
                        changeNextState();
                        changeNextState();
                    } else
                        changeNextState();
                    break;
                case MiBandState.AUTHORIZE:
                    authPhase();
                    break;
                case MiBandState.GET_MODEL_NAME:
                    if (MiBand.getModel().isEmpty()) getModelName();
                    else changeNextState();
                    break;
                case MiBandState.GET_SOFT_REVISION:
                    getSoftwareRevision();
                    break;
                case MiBandState.ENABLE_NOTIFICATIONS:
                    //enableNotification();
                    changeNextState();
                    break;
                case MiBandState.SEND_SETTINGS:
                    sendSettings();
                    periodicVibrateAlert(3, 1000, 300);
                    changeNextState();
                    break;
                case MiBandState.SET_TIME:
                    if (!MiBandEntry.isNeedSendReading()) {
                        changeNextState();
                        break;
                    }
                    vibrateAlert(AlertLevelMessage.AlertLevelType.VibrateAlert);
                    sendBG();
                    changeNextState();
                    break;
                case MiBandState.QUEUE_MESSAGE:
                    queueMessage();
                    changeNextState();
                    break;
                default:
                    return super.automata();
            }
        } else {
            UserError.Log.d(TAG, "Service should not be running inside automata");
            stopSelf();
        }
        return true; // lies
    }

    private boolean shouldServiceRun() {
        return MiBandEntry.isEnabled();
    }

    @Override
    protected void setRetryTimerReal() {
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            I.serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.MIBAND_SERVICE_RETRY_ID);
            //PendingIntent.getService(xdrip.getAppContext(), Constants.MiBand_SERVICE_RETRY_ID,
            //        new Intent(xdrip.getAppContext(), this.getClass()), 0);
            I.retry_time = JoH.wakeUpIntent(xdrip.getAppContext(), retry_in, I.serviceIntent);
            I.wakeup_time = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }


    private long whenToRetryNext() {
        I.retry_backoff += Constants.SECOND_IN_MS;
        if (I.retry_backoff > MAX_RETRY_BACKOFF_MS) {
            I.retry_backoff = MAX_RETRY_BACKOFF_MS;
        }
        return Constants.SECOND_IN_MS * 10 + I.retry_backoff;
    }

    static class MiBandState extends JamBaseBluetoothSequencer.BaseState {
        static final String SET_TIME = "Setting TimeMessage";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Sending Alert";
        static final String AUTHENTICATE = "Authenticate";
        static final String AUTHORIZE = "Authorize phase";
        static final String GET_MODEL_NAME = "Getting model name";
        static final String GET_SOFT_REVISION = "Getting software revision";
        static final String ENABLE_NOTIFICATIONS = "Enable notify";

        {
            Initialize();
        }

        void Initialize() {
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(AUTHENTICATE);
            sequence.add(AUTHORIZE);
            sequence.add(GET_MODEL_NAME);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(SEND_SETTINGS);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
            //
            sequence.add(SET_TIME);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
            //
            sequence.add(QUEUE_MESSAGE);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
        }
    }

    // Mega Status
    public static List<StatusItem> megaStatus() {

        final List<StatusItem> l = new ArrayList<>();
        final Inst II = Inst.get(MiBandService.class.getSimpleName());

        if (MiBand.isAuthenticated()) {
            l.add(new StatusItem("Model", MiBand.getModel()));
            l.add(new StatusItem("Software version", MiBand.getVersion()));
        }
        l.add(new StatusItem("Mac address", MiBand.getMac()));
        l.add(new StatusItem("Connected", II.isConnected ? "Yes" : "No"));
        l.add(new StatusItem("Is authenticated", MiBand.isAuthenticated() ? "Yes" : "No"));
        if (II.wakeup_time != 0) {
            final long till = msTill(II.wakeup_time);
            if (till > 0) l.add(new StatusItem("Wake Up", niceTimeScalar(till)));
        }
        l.add(new StatusItem("State", II.state));

        final int qsize = II.getQueueSize();
        if (qsize > 0) {
            l.add(new StatusItem("Queue", qsize + " items"));
        }

        return l;
    }
}
