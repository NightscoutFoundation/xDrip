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
import com.eveningoutpost.dexdrip.R;
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
import com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertLevelMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AlertMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.AuthMessages;
import com.eveningoutpost.dexdrip.watch.miband.message.DeviceEvent;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiBand2;
import com.eveningoutpost.dexdrip.watch.miband.message.DisplayControllMessageMiband3_4;
import com.eveningoutpost.dexdrip.watch.miband.message.FeaturesControllMessage;
import com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes;
import com.eveningoutpost.dexdrip.watch.miband.message.TimeMessage;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleCannotSetCharacteristicNotificationException;
import com.polidea.rxandroidble2.exceptions.BleCharacteristicNotFoundException;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.Models.ActiveBgAlert.currentlyAlerting;
import static com.eveningoutpost.dexdrip.Models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.Models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.SEND_QUEUE;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;
import static com.eveningoutpost.dexdrip.watch.miband.MiBand.MiBandType.MI_BAND2;
import static com.eveningoutpost.dexdrip.watch.miband.MiBand.MiBandType.MI_BAND4;
import static com.eveningoutpost.dexdrip.watch.miband.MiBandService.MiBandState.AUTHORIZE_FAILED;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_FAIL;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_MIBAND4_FAIL;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_REQUEST_RANDOM_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_RESPONSE;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_ENCRYPTED_AUTH_NUMBER;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SEND_KEY;
import static com.eveningoutpost.dexdrip.watch.miband.message.OperationCodes.AUTH_SUCCESS;

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
    private static final int QUEUE_DELAY = 0; //ms
    private static final long BG_UPDATE_INTERVAL = 5 * Constants.MINUTE_IN_MS;
    private Subscription subscription;
    private AuthMessages authorisation;
    private Boolean isNeedToCheckRevision = true;
    private Boolean isNeedToAutentificate = true;
    public static Timer bgUpdateTimer;
    static BatteryInfo batteryInfo = new BatteryInfo();
    private FirmwareOperations firmware;

    final Runnable canceller = () -> {
        if (!currentlyAlerting() && !IncomingCallsReceiver.isRingingNow()) {
            UserError.Log.d(TAG, "Clearing queue as alert / call ceased");
            emptyQueue();
        }
    };

    {
        mState = new MiBandState().setLI(I);
        I.queue_write_characterstic = new AlertMessage().getCharacteristicUUID();
    }

    private Class getPrefBinder() {
        MiBand.MiBandType type = MiBand.getMibandType();
        if (type == MI_BAND2)
            return Miband2PrefBinding.class;
        else return Miband3_4PrefBinding.class;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("Miband service", 60000);
        try {

            if (MiBandState.getSequnceType() == MiBandState.SequenceType.INSTALL_WATCHFACE)
                return START_STICKY;
            if (shouldServiceRun()) {
                final String authMac = MiBand.getAuthMac();
                String mac = MiBand.getMac();
                if (!authMac.equalsIgnoreCase(mac) && !authMac.isEmpty()) {
                    MiBand.setAuthMac(""); //flush old auth info
                    isNeedToAutentificate = true;
                }
                if (emptyString(mac)) {
                    // if mac not set then start up a scan and do nothing else
                    new FindNearby().scan();
                } else {
                    startBgTimer();
                    if (!MiBandEntry.isNeedSendReading()) {
                        stopBgUpdateTimer();
                    }
                    setAddress(mac);
                    String message;
                    if (intent != null) {
                        final String function = intent.getStringExtra("function");
                        if (function != null) {
                            switch (function) {
                                case "refresh":
                                    ((MiBandState) mState).setSettingsSequence();
                                    changeState(INIT);
                                    break;
                                case "install-watchface":
                                    message = intent.getStringExtra("message");
                                    if (message != null) {
                                        keyStore.putS(MESSAGE, message);
                                        stopBgUpdateTimer();
                                        ((MiBandState) mState).setInstallWatchfaceSequence();
                                        changeState(INIT);
                                    }
                                    break;
                                case "message":
                                    message = intent.getStringExtra("message");
                                    final String message_type = intent.getStringExtra("message_type");
                                    if (message != null) {
                                        keyStore.putS(MESSAGE, message);
                                        keyStore.putS(MESSAGE_TYPE, message_type != null ? message_type : "");
                                        ((MiBandState) mState).setQueueSequence();
                                        changeState(INIT);
                                    }
                                    break;
                                case "set_time":
                                    ((MiBandState) mState).setTimeSequence();
                                    changeState(INIT);
                                    stopBgUpdateTimer();
                                    startBgTimer();
                                    break;
                            }
                        } else {
                            // no specific function
                        }
                    }
                }


                return START_STICKY;
            } else {
                UserError.Log.d(TAG, "Service is NOT set be active - shutting down");
                stopSelf();
                stopBgUpdateTimer();
                return START_NOT_STICKY;
            }
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    private void stopBgUpdateTimer() {
        if (bgUpdateTimer != null) {
            bgUpdateTimer.cancel();
            bgUpdateTimer = null;
        }
    }

    private void startBgTimer() {
        if (MiBandEntry.isNeedSendReading() && MiBand.isAuthenticated() && (bgUpdateTimer == null)) {
            bgUpdateTimer = new Timer();
            bgUpdateTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (I.state.equals(MiBandState.SLEEP) || I.state.equals(MiBandState.CLOSED) || I.state.equals(SEND_QUEUE)) {
                        ((MiBandState) mState).setTimeSequence();
                        changeState(INIT);
                    }
                }
            }, BG_UPDATE_INTERVAL, BG_UPDATE_INTERVAL);
        }
    }

    private void handleDeviceEvent(byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        switch (value[0]) {
            case DeviceEvent.CALL_REJECT:
                UserError.Log.d(TAG, "call rejected");
                break;
            case DeviceEvent.CALL_IGNORE:
                UserError.Log.d(TAG, "call ignored");
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
                // acknowledgeFindPhone(); // FIXME: premature
                break;
            case DeviceEvent.FIND_PHONE_STOP:
                UserError.Log.d(TAG, "find phone stopped");
                break;
            case DeviceEvent.MUSIC_CONTROL:
                UserError.Log.d(TAG, "got music control");
                switch (value[1]) {
                    case 0:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case 1:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case 3:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case 4:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case 5:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case 6:
                        //deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
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
            default:
                UserError.Log.d(TAG, "unhandled event " + value[0]);
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
                        MiBand.setVersion(revision, MiBand.getAuthMac());
                        isNeedToCheckRevision = false;
                        changeNextState();
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not read software revision: " + throwable);
                        changeNextState();
                    });
        } else changeNextState();
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
                    MiBand.setModel(name, MiBand.getAuthMac());
                    changeNextState();
                }, throwable -> {
                    if (d)
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
                .setDelayMs(QUEUE_DELAY)
                .queue();
    }

    private void sendSettings() {
        List<Pair<Integer, Boolean>> features = PrefBindingFactory.getInstance(getPrefBinder()).getStates("miband_feature_");
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
        MiBand.MiBandType type = MiBand.getMibandType();
        if (type == MI_BAND2)
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
    }

    private void queueMessage() {
        final String alert = keyStore.getS(MESSAGE);
        final String type = keyStore.getS(MESSAGE_TYPE);
        if (d)
            UserError.Log.d(TAG, "Queuing message alert of type: " + type + " " + alert);

        if (!emptyString(alert)) {

            switch (type != null ? type : "null") {
                case "call":
                    new QueueMe()
                            .setBytes(new AlertMessage().getAlertMessage(alert, AlertMessage.AlertCategory.Call))
                            .setDescription("Send call alert: " + alert)
                            .expireInSeconds(QUEUE_EXPIRED_TIME)
                            .setDelayMs(4000)
                            .setRunnable(canceller)
                            .queue();
                    if (d)
                        UserError.Log.d(TAG, "Queued call alert: " + alert);
                    break;

                case "glucose":
                    new QueueMe()
                            .setBytes(new AlertMessage().getAlertMessage("", AlertMessage.AlertCategory.CustomHuami, AlertMessage.CustomIcon.ALARM_CLOCK))
                            .setDescription("Send alert: " + alert)
                            .expireInSeconds(60)
                            .setDelayMs(4000)
                            .queue();
                    new QueueMe()
                            .setBytes(new AlertMessage().getAlertMessage(alert, AlertMessage.AlertCategory.SMS_MMS))
                            .setDescription("Send alert: " + alert)
                            .expireInSeconds(60)
                            .setDelayMs(QUEUE_DELAY)
                            .queue();
                    break;

                default: // glucose

                    break;
            }
            // this parent method might get called multiple times
            Inevitable.task("miband-s-queue", 200, () -> changeState(mState.next()));
        } else {
            if (d)
                UserError.Log.e(TAG, "Alert message requested but no message set");
        }
    }

    @SuppressLint("CheckResult")
    private void authPhase() {
        RxBleConnection connection = I.connection;
        if (d)
            UserError.Log.d(TAG, "Authorizing");
        if (I.connection == null) {
            if (d)
                UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        if (d)
            UserError.Log.d(TAG, "Requesting to enable notifications for auth");

        String authKey = MiBand.getPersistantAuthKey();

        if (MiBand.getMibandType() == MI_BAND4) {
            if (authKey.isEmpty()) {
                authKey = MiBand.getAuthKey();
                if (authKey.isEmpty()) {
                    authKey = AuthMessages.getAuthCodeFromFilesSystem(MiBand.getMac());
                }
                if ((authKey.length() != 32) || !authKey.matches("[a-zA-Z0-9]+")) {
                    JoH.static_toast_long("Wrong miband authorization key, please recheck a key and try to reconnect again");
                    changeState(AUTHORIZE_FAILED);
                    return;
                } else MiBand.setAuthKey(authKey);
            }
        }
        if ((authKey.length() != 32) || !authKey.matches("[a-zA-Z0-9]+")) {
            authKey = "";
        }
        authorisation = new AuthMessages(MiBand.getMibandType(), authKey);
        subscription = new Subscription(
                connection.setupNotification(authorisation.getCharacteristicUUID())
                        .timeout(15, TimeUnit.SECONDS) // WARN
                        // .observeOn(Schedulers.newThread()) // needed?
                        .doOnNext(notificationObservable -> {
                                    if (d)
                                        UserError.Log.d(TAG, "Notification for auth enabled");
                                    connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authorisation.getAuthCommand())
                                            .subscribe(characteristicValue -> {
                                                        UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ, got: " + JoH.bytesToHex(characteristicValue));
                                                    },
                                                    throwable -> {
                                                        UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ: " + throwable);
                                                    }
                                            );
                                }
                        )
                        .flatMap(notificationObservable -> notificationObservable)
                        //.timeout(5, TimeUnit.SECONDS)
                        //.observeOn(Schedulers.newThread())
                        .subscribe(bytes -> {
                            // incoming notifications
                            if (d)
                                UserError.Log.d(TAG, "Received auth notification bytes: " + bytesToHex(bytes));
                            ProcessAuthCommands(connection, bytes);
                            // changeNextState();
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in Record Notification: " + throwable);
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
                                    UserError.Log.d(TAG, "Timeout");
                                }
                            }
                            if (subscription != null) {
                                subscription.unsubscribe();
                            }
                            changeState(CLOSE);
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
            byte[] authReply = authorisation.calculateAuthReply(tmpValue);
            connection.writeCharacteristic(authorisation.getCharacteristicUUID(), authReply) //get random key from band
                    .subscribe(val -> {
                        if (d)
                            UserError.Log.d(TAG, "Wrote OPCODE_AUTH_REQ2: " + JoH.bytesToHex(val));
                    }, throwable -> {
                        UserError.Log.e(TAG, "Could not write OPCODE_AUTH_REQ2: " + throwable);
                    });
        } else if (value[0] == AUTH_RESPONSE &&
                (value[1] & 0x0f) == AUTH_SEND_ENCRYPTED_AUTH_NUMBER &&
                value[2] == AUTH_SUCCESS) {
            isNeedToAutentificate = false;
            if (MiBand.getAuthMac().isEmpty()) {
                MiBand.setAuthMac(MiBand.getMac());
                MiBand.setPersistantAuthKey(JoH.bytesToHex(authorisation.getLocalKey()), MiBand.getAuthMac());
                JoH.static_toast_long("MiBand succesfully authentificated");
            }
            if (subscription != null) {
                subscription.unsubscribe();
            }
            changeNextState();
        } else if (value[0] == AUTH_RESPONSE &&
                (((value[2] & 0x0f) == AUTH_FAIL) || (value[2] == AUTH_MIBAND4_FAIL))) {
            MiBand.setAuthMac("");
            if (subscription != null) {
                subscription.unsubscribe();
            }
            changeState(AUTHORIZE_FAILED);
        }
    }


    @SuppressLint("CheckResult")
    private void installWatchface() {
        final String message = keyStore.getS(MESSAGE);
        final String type = keyStore.getS(MESSAGE_TYPE);
        RxBleConnection connection = I.connection;
        if (d)
            UserError.Log.d(TAG, "Install Watchface");
        if (I.connection == null) {
            if (d)
                UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        if (d)
            UserError.Log.d(TAG, "Requesting to enable notifications for installWatchface");
        InputStream file;
        file = getResources().openRawResource(R.raw.xdrip_miband4);

        firmware = new FirmwareOperations(file);
        subscription = new Subscription(
                connection.setupNotification(firmware.getFirmwareCharacteristicUUID())
                        .timeout(400, TimeUnit.SECONDS) // WARN
                        .doOnNext(notificationObservable -> {
                                    if (d)
                                        UserError.Log.d(TAG, "Notification for firmware enabled");
                                    connection.writeCharacteristic(Const.UUID_CHAR_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, new byte[]{0x06, 0x00, 0x0c, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07})
                                            .doFinally(() -> {  //min con int 06, max con int 12, 2000ms timeout (for faster upload)
                                                        connection.writeCharacteristic(firmware.getFirmwareCharacteristicUUID(), firmware.sendFwInfo())
                                                                .subscribe(valB -> {
                                                                            UserError.Log.d(TAG, "Wrote sendFwInfo, got: " + JoH.bytesToHex(valB));
                                                                        },
                                                                        throwable -> {
                                                                            UserError.Log.e(TAG, "Could not write sendFwInfo: " + throwable);
                                                                            resetFirmwareState(false);
                                                                        }
                                                                );
                                                    }
                                            );
                                }
                        )
                        .flatMap(notificationObservable -> notificationObservable)
                        .subscribe(bytes -> {
                            // incoming notifications
                            if (d)
                                UserError.Log.d(TAG, "Received firmware notification bytes: " + bytesToHex(bytes));
                            processFirmwareCommands(bytes);
                            // changeNextState();
                        }, throwable -> {
                            UserError.Log.d(TAG, "Throwable in firmware Notification: " + throwable);
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
    }

    @SuppressLint("CheckResult")
    private void processFirmwareCommands(byte[] value) {
        RxBleConnection connection = I.connection;
        if (value.length != 3 && value.length != 11) {
            UserError.Log.e(TAG, "Notifications should be 3 or 11 bytes long.");
            return;
        }
        boolean success = value[2] == OperationCodes.SUCCESS;
        if (value[0] == OperationCodes.RESPONSE && success) {
            try {
                switch (value[1]) {
                    case OperationCodes.COMMAND_FIRMWARE_INIT: {
                        sendFirmwareData();
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_START_DATA: {
                        sendFirmwareCommand(firmware.getFirmwareCharacteristicUUID(), firmware.sendChecksum(), "sendChecksum");
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_CHECKSUM: {
                        if (firmware.getFirmwareType() == FirmwareOperations.FirmwareType.FIRMWARE) {
                            //send reboot
                        } else {
                            UserError.Log.e(TAG, "Watch Face has been installed successfully");
                            JoH.static_toast_long("Watch Face has been installed successfully");
                            resetFirmwareState(true);
                        }
                        break;
                    }
                    case OperationCodes.COMMAND_FIRMWARE_REBOOT: {
                        UserError.Log.e(TAG, "Reboot command successfully sent.");
                        resetFirmwareState(true);
                        break;
                    }
                    default: {
                        UserError.Log.e(TAG, "Unexpected response during firmware update: ");
                        resetFirmwareState(false);
                    }
                }
            } catch (Exception ex) {
                resetFirmwareState(false);
            }
        } else {
            UserError.Log.e(TAG, "Unexpected notification during firmware update: ");
            resetFirmwareState(false);
        }
    }

    private void resetFirmwareState(Boolean result) {
        if (!result) JoH.static_toast_long("Error while uploding MiBand watchface");
        else JoH.static_toast_long("MiBand watchface has been uploaded");
        emptyQueue();
        if (subscription != null) {
            subscription.unsubscribe();
        }
        ((MiBandState) mState).setSettingsSequence();
        changeState(INIT);
    }

    private void sendFirmwareData() {
        byte[] fwbytes = firmware.getBytes();
        int len = firmware.getSize();
        final int packetLength = firmware.getPackeLenght();
        int packets = len / packetLength;
        // going from 0 to len
        int firmwareProgress = 0;

        sendFirmwareCommand(firmware.getFirmwareCharacteristicUUID(), firmware.getFirmwareStartCommand(), "Start command");
        for (int i = 0; i < packets; i++) {
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, i * packetLength, i * packetLength + packetLength);
            sendFirmwareCommand(firmware.getFirmwareDataCharacteristicUUID(), fwChunk, "Chunk:" + i);
            firmwareProgress += packetLength;
            int progressPercent = (int) ((((float) firmwareProgress) / len) * 100);
            if ((i > 0) && (i % 50 == 0)) {
                sendFirmwareCommand(firmware.getFirmwareCharacteristicUUID(), new byte[]{OperationCodes.COMMAND_FIRMWARE_UPDATE_SYNC}, "Sync " + progressPercent + "%");
            }
        }
        if (firmwareProgress < len) { //last chunk
            byte[] fwChunk = Arrays.copyOfRange(fwbytes, packets * packetLength, len);
            sendFirmwareCommand(firmware.getFirmwareDataCharacteristicUUID(), fwChunk, "Last chunk");
        }
        sendFirmwareCommand(firmware.getFirmwareCharacteristicUUID(), new byte[]{OperationCodes.COMMAND_FIRMWARE_UPDATE_SYNC}, "Sync");
        sendFirmwareCommand(Const.UUID_CHAR_PERIPHERAL_PREFERRED_CONNECTION_PARAMETERS, new byte[]{(byte) 0xf0, 0x00, (byte) 0xf0, 0x00, 0x00, 0x00, (byte) 0xd0, 0x07}, "set connection interval");
        //changeState(SEND_QUEUE);
    }

    void sendFirmwareCommand(final UUID uuid, final byte[] bytes, String info) {
        new QueueMe()
                .setBytes(bytes)
                .setDescription(info)
                .setQueueWriteCharacterstic(uuid)
                .expireInSeconds(400)
                .setDelayMs(0)
                .send();
    }

    @SuppressLint("CheckResult")
    private void enableNotification() {
        UserError.Log.d(TAG, "Enabling notifications");
        I.connection.setupNotification(Const.UUID_CHARACTERISTIC_DEVICEEVENT)
                .doOnNext(notificationObservable -> {
                    JoH.threadSleep(1000);
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
                            if (!(throwable instanceof TimeoutException)) {
                                UserError.Log.e(TAG, "Throwable inside setup notification: " + throwable);
                            } else {
                                UserError.Log.d(TAG, "OUTER TIMEOUT INSIDE NOTIFICATION LISTENER");
                            }
                        }
                );

    }

    @Override
    protected synchronized boolean automata() {
        extendWakeLock(1000);
        if (d)
            UserError.Log.d(TAG, "Automata called in" + TAG);

        if (shouldServiceRun()) {

            switch (I.state) {

                case INIT:
                    // connect by default
                    changeNextState();
                    break;
                case MiBandState.AUTHENTICATE:
                    if (isNeedToAutentificate) {
                        changeNextState();
                    } else {
                        changeNextState();
                        changeNextState();
                    }
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
                case MiBandState.GET_BATTERY_INFO:
                    getBatteryInfo();
                    changeNextState();
                    break;
                case MiBandState.ENABLE_NOTIFICATIONS:
                    enableNotification();
                    changeNextState();
                    break;
                case MiBandState.SEND_SETTINGS:
                    sendSettings();
                    //periodicVibrateAlert(3, 1000, 300);
                    changeNextState();
                    break;
                case MiBandState.SET_TIME:
                    if (!MiBandEntry.isNeedSendReading()) {
                        changeNextState();
                        break;
                    }
                    if (MiBandEntry.isVibrateOnReadings())
                        vibrateAlert(AlertLevelMessage.AlertLevelType.VibrateAlert);
                    sendBG();
                    changeNextState();
                    break;
                case MiBandState.INSTALL_WATCHFACE:
                    installWatchface();
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
        static final String SET_TIME = "Setting Time";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Sending Alert";
        static final String AUTHENTICATE = "Authenticate";
        static final String AUTHORIZE = "Authorize phase";
        static final String AUTHORIZE_FAILED = "Authorization failed";
        static final String GET_MODEL_NAME = "Getting model name";
        static final String GET_SOFT_REVISION = "Getting software revision";
        static final String ENABLE_NOTIFICATIONS = "Enable notification";
        static final String GET_BATTERY_INFO = "Getting battery info";
        static final String INSTALL_WATCHFACE = "Watchface installation";

        public enum SequenceType {
            TIME,
            QUEUE,
            SETTINGS,
            INSTALL_WATCHFACE;
        }

        private static SequenceType sType;

        private static final String TAG = "MiBandStateSequence";


        public static SequenceType getSequnceType() {
            return sType;
        }

        void setTimeSequence() {
            sType = SequenceType.TIME;
            UserError.Log.d(TAG, "SET TIME SEQUENCE");
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(SET_TIME);
            sequence.add(SEND_QUEUE);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(SLEEP);
        }

        void setQueueSequence() {
            sType = SequenceType.QUEUE;
            UserError.Log.d(TAG, "SET QUEUE SEQUENCE");
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(QUEUE_MESSAGE);
            sequence.add(SEND_QUEUE);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(SLEEP);
        }

        void setSettingsSequence() {
            sType = SequenceType.SETTINGS;
            UserError.Log.d(TAG, "SET SETTINGS SEQUENCE");
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(GET_MODEL_NAME);
            sequence.add(AUTHENTICATE);
            sequence.add(AUTHORIZE);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(ENABLE_NOTIFICATIONS);
            sequence.add(SEND_SETTINGS);
            sequence.add(SET_TIME);
            sequence.add(SEND_QUEUE);
            sequence.add(GET_BATTERY_INFO);
            sequence.add(SLEEP);

            sequence.add(AUTHORIZE_FAILED);
        }

        void setInstallWatchfaceSequence() {
            sType = SequenceType.INSTALL_WATCHFACE;
            UserError.Log.d(TAG, "SET INSTALL WATCHFACE SEQUENCE");
            sequence.clear();
            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(GET_MODEL_NAME);
            sequence.add(GET_SOFT_REVISION);
            sequence.add(INSTALL_WATCHFACE);
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
        l.add(new StatusItem("State", II.state));

        final int qsize = II.getQueueSize();
        if (qsize > 0) {
            l.add(new StatusItem("Queue", qsize + " items"));
        }


        return l;
    }
}
