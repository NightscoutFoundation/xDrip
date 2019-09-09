package com.eveningoutpost.dexdrip.watch.lefun;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Pair;

import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.store.FastStore;
import com.eveningoutpost.dexdrip.store.KeyStore;
import com.eveningoutpost.dexdrip.utils.framework.IncomingCallsReceiver;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.watch.lefun.messages.BaseRx;
import com.eveningoutpost.dexdrip.watch.lefun.messages.BaseTx;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxFind;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxPong;
import com.eveningoutpost.dexdrip.watch.lefun.messages.RxShake;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxAlert;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxPing;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetFeatures;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetLocaleFeature;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetScreens;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxSetTime;
import com.eveningoutpost.dexdrip.watch.lefun.messages.TxShakeDetect;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleDeviceServices;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

//import rx.schedulers.Schedulers;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


import static com.eveningoutpost.dexdrip.Models.ActiveBgAlert.currentlyAlerting;
import static com.eveningoutpost.dexdrip.Models.JoH.bytesToHex;
import static com.eveningoutpost.dexdrip.Models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.Models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.Models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.CLOSE;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.DISCOVER;
import static com.eveningoutpost.dexdrip.Services.JamBaseBluetoothSequencer.BaseState.INIT;
import static com.eveningoutpost.dexdrip.UtilityModels.Unitized.mmolConvert;
import static com.eveningoutpost.dexdrip.watch.lefun.Const.REPLY_CHARACTERISTIC;
import static com.eveningoutpost.dexdrip.watch.lefun.Const.WRITE_CHARACTERISTIC;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFun.shakeToSnooze;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFunService.LeFunState.ENABLE_NOTIFICATIONS;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFunService.LeFunState.PROTOTYPE;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFunService.LeFunState.QUEUE_MESSAGE;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFunService.LeFunState.SEND_SETTINGS;
import static com.eveningoutpost.dexdrip.watch.lefun.LeFunService.LeFunState.SET_TIME;

/**
 * Jamorham
 *
 * Data communication with Lefun compatible bands/watches
 */

public class LeFunService extends JamBaseBluetoothSequencer {

    private static final String MESSAGE = "LeFun-Message";
    private static final String MESSAGE_TYPE = "LeFun-Message-Type";
    private final KeyStore keyStore = FastStore.getInstance();
    private static final boolean d = true;
    private static final long MAX_RETRY_BACKOFF_MS = Constants.SECOND_IN_MS * 300; // sleep for max ms if we have had no signal


    final Runnable canceller = () -> {
        if (!currentlyAlerting() && !IncomingCallsReceiver.isRingingNow()) {
            UserError.Log.d(TAG, "Clearing queue as alert / call ceased");
            emptyQueue();
        }
    };

    {
        mState = new LeFunState().setLI(I);
        I.queue_write_characterstic = WRITE_CHARACTERISTIC;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final PowerManager.WakeLock wl = JoH.getWakeLock("lefun service", 60000);
        try {
            if (shouldServiceRun()) {

                final String mac = LeFun.getMac();
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
                                    changeState(INIT);
                                    break;
                                case "prototype":
                                    changeState(PROTOTYPE);
                                    break;
                                case "message":
                                    final String message = intent.getStringExtra("message");
                                    final String message_type = intent.getStringExtra("message_type");
                                    if (message != null) {
                                        keyStore.putS(MESSAGE, message);
                                        keyStore.putS(MESSAGE_TYPE, message_type != null ? message_type : "");
                                        changeState(QUEUE_MESSAGE);
                                    }
                            }
                        } else {
                            // no specific function
                            UserError.Log.d(TAG, "SET TIME CALLED");
                            changeState(SET_TIME);
                        }
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


    private static final UUID[] huntCharacterstics = new UUID[]{REPLY_CHARACTERISTIC};

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
            enableNotification();
        } else {
            UserError.Log.e(TAG, "Could not find characteristic during service discovery. This is very unusual");
        }
    }


    private void enableNotification() {
        UserError.Log.d(TAG, "Enabling notifications");
        I.isNotificationEnabled = false;
        if (I.connection == null) {
            UserError.Log.d(TAG, "Cannot enable as connection is null!");
            return;
        }
        if (I.readCharacteristic == null) {
            UserError.Log.d(TAG, "Cannot enable as read characterstic is null");
            return;
        }
        I.connection.setupNotification(I.readCharacteristic)
                .timeout(630, TimeUnit.SECONDS) // WARN
                //.observeOn(Schedulers.newThread()) // needed?
                .doOnNext(notificationObservable -> {
                    I.isNotificationEnabled = true;
                    // change to queue send state
                    changeState(mState.next());
                }).flatMap(notificationObservable -> notificationObservable)
                //.timeout(5, TimeUnit.SECONDS)
                .observeOn(Schedulers.newThread())
                .subscribe(bytes -> {
                            // incoming notifications
                            if (d)
                                UserError.Log.d(TAG, "Received data notification bytes: " + HexDump.dumpHexString(bytes));

                            processAndAction(bytes);

                        }, throwable -> {
                            if (!(throwable instanceof TimeoutException)) {
                                UserError.Log.e(TAG, "Throwable inside setup notification: " + throwable);
                            } else {
                                UserError.Log.d(TAG, "OUTER TIMEOUT INSIDE NOTIFICATION LISTENER");
                            }
                            I.isNotificationEnabled = false;
                            changeState(CLOSE);
                            // stopConnect();
                        }
                );

    }

    private void processAndAction(final byte[] bytes) {

        final String incomingHex = bytesToHex(bytes);

        switch (incomingHex) {

            // non classified literal values can go here

            default:
                final BaseRx packet = Classifier.classify(bytes);
                if (packet != null) {
                    UserError.Log.d(TAG, "Classified: " + packet.getClass().getSimpleName());
                    if (packet instanceof RxPong) {
                        LeFun.setModel(((RxPong) packet).getModel());
                    } else if (packet instanceof RxShake) {
                        shakeDetected();
                    } else if (packet instanceof RxFind) {
                        findPhone();
                    }
                }
        }
    }

    private void shakeDetected() {
        UserError.Log.d(TAG, "Shake detected");
        if (shakeToSnooze()) {
            AlertPlayer.getPlayer().OpportunisticSnooze();
            emptyQueue();
            UserError.Log.ueh(TAG, "Alert snoozed by Shake");
        }
    }

    private void findPhone() {
        UserError.Log.d(TAG, "Find phone function triggered");
        if (!AlertPlayer.getPlayer().OpportunisticSnooze()) {
            JoH.showNotification("Find Phone", "Activated from Lefun band", null, 5, true, true, false);
        } else {
            emptyQueue();
            UserError.Log.ueh(TAG, "Alert snoozed by Find feature");
        }
    }


    private void sendSettings() {

        probeModelTypeIfUnknown();

        for (Pair<Integer, Boolean> lState : PrefBinding.getInstance().getStates("lefun_locale_")) {
            new QueueMe()
                    .setBytes(new TxSetLocaleFeature(lState.first, lState.second).getBytes())
                    .setDescription("Set Locale Features")
                    .expectReply().expireInSeconds(30)
                    .queue();
        }

        BaseTx screens = new TxSetScreens();

        for (int screen : PrefBinding.getInstance().getEnabled("lefun_screen")) {
            screens.enable(screen);
        }
        new QueueMe()
                .setBytes(screens.getBytes())
                .setDescription("Set screens for: ")
                .expectReply().expireInSeconds(30)
                .queue();

        BaseTx features = new TxSetFeatures();
        for (int feature : PrefBinding.getInstance().getEnabled("lefun_feature")) {
            features.enable(feature);
        }
        new QueueMe()
                .setBytes(features.getBytes())
                .setDescription("Set features for: ")
                .expectReply().expireInSeconds(30)
                .send();

    }


    private void prototype() {

        LeFun.sendAlert("TEST", "12.3");
      /*  new QueueMe()
                .setBytes(new TxSetLang().getBytes())
                .setDescription("Set prototype lang")
                .expectReply().expireInSeconds(30)
                .queue();
*/
        startQueueSend();

    }

    private void probeModelTypeIfUnknown() {
        if (emptyString(LeFun.getModel())) {
            new QueueMe()
                    .setBytes(new TxPing().getBytes())
                    .setDescription("Set Probe model type")
                    .expectReply().expireInSeconds(30)
                    .queue();
        }
    }


    private void sendBG() {

        // TODO use DisplayGlucose 100% and avoid rounding errors

        final BgReading last = BgReading.last();
        FunAlmanac.Reply rep;
        if (last == null || last.isStale()) {
            rep = FunAlmanac.getRepresentation(0);
        } else {
            final double mmol_value = roundDouble(mmolConvert(last.getDg_mgdl()), 1);
            rep = FunAlmanac.getRepresentation(mmol_value);
        }

        UserError.Log.uel(TAG, "Representation for: " + rep.input);

        probeModelTypeIfUnknown();

        new QueueMe()
                .setBytes(new TxSetTime(rep.timestamp, rep.zeroMonth, rep.zeroDay).getBytes())
                .setDescription("Set display for: " + rep.input)
                .expectReply().expireInSeconds(290)
                .send();

    }

    private void queueMessage() {
        final String alert = keyStore.getS(MESSAGE);
        final String type = keyStore.getS(MESSAGE_TYPE);

        UserError.Log.d(TAG,"Queuing message alert of type: "+type+" "+alert);

        if (!emptyString(alert)) {

            probeModelTypeIfUnknown();

            switch (type != null ? type : "null") {
                case "call":
                    for (int repeats = 0; repeats < 25; repeats++) {
                        new QueueMe()
                                .setBytes(new TxAlert(alert, TxAlert.ICON_CALL).getBytes())
                                .setDescription("Send call alert: " + alert)
                                .expectReply().expireInSeconds(60)
                                .setDelayMs(5000)
                                .setRunnable(canceller)
                                .queue();
                    }
                    UserError.Log.d(TAG, "Queued call alert: " + alert);
                    break;

                default: // glucose
                    for (int repeats = 0; repeats < 5; repeats++) {
                        new QueueMe()
                                .setBytes(new TxShakeDetect(false).getBytes())
                                .setDescription("Disable Shake detection")
                                .expectReply().expireInSeconds(60)
                                .setRunnable(canceller)
                                .queue();

                        new QueueMe()
                                .setBytes(new TxAlert(alert).getBytes())
                                .setDescription("Send alert: " + alert)
                                .expectReply().expireInSeconds(60)
                                .setDelayMs(shakeToSnooze() ? 1500 : 200)
                                .queue();

                        if (shakeToSnooze()) {
                            new QueueMe()
                                    .setBytes(new TxShakeDetect(true).getBytes())
                                    .setDescription("Enable Shake detection")
                                    .expectReply().expireInSeconds(60)
                                    .setDelayMs(10000)
                                    .queue();

                        }
                    }
                    break;
            }
            // this parent method might get called multiple times
           Inevitable.task("lefun-s-queue", 200, () -> changeState(mState.next()));

        } else {
            UserError.Log.e(TAG, "Alert message requested but no message set");
        }
    }


    static class LeFunState extends JamBaseBluetoothSequencer.BaseState {
        static final String SET_TIME = "Setting Time";
        static final String SEND_SETTINGS = "Updating Settings";
        static final String QUEUE_MESSAGE = "Sending Alert";
        static final String PROTOTYPE = "Prototype Test";
        static final String ENABLE_NOTIFICATIONS = "Enabling notify";

        {
            sequence.clear();

            sequence.add(INIT);
            sequence.add(CONNECT_NOW);
            sequence.add(SEND_QUEUE);
            sequence.add(SEND_SETTINGS);
            sequence.add(SET_TIME);
            sequence.add(SLEEP);
            //
            sequence.add(QUEUE_MESSAGE);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
            //
            sequence.add(PROTOTYPE);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);
            //
            sequence.add(ENABLE_NOTIFICATIONS);
            sequence.add(SEND_QUEUE);
            sequence.add(SLEEP);

        }
    }


    @Override
    protected synchronized boolean automata() {
        extendWakeLock(1000);
        UserError.Log.d(TAG, "Automata called in LeFun");

        if (I.state.equals(QUEUE_MESSAGE) || alwaysConnected()) {
            if ((I.isConnected) && !I.state.equals(CLOSE)) {
                if (!I.isDiscoveryComplete) {
                    UserError.Log.d(TAG, "Services not discovered");
                    I.state = DISCOVER;
                } else if ((!I.isNotificationEnabled) && (JoH.ratelimit("lefun-enable-notifications", 2))) {
                    UserError.Log.d(TAG, "Notifications not enabled");
                    I.state = ENABLE_NOTIFICATIONS;
                }
            }

            switch (I.state) {

                case INIT:
                    // connect by default
                    changeState(mState.next());
                    break;

                case ENABLE_NOTIFICATIONS:
                    enableNotification();
                    break;

                case SEND_SETTINGS:
                    sendSettings();
                    break;

                case SET_TIME:
                    sendBG();
                    break;

                case PROTOTYPE:
                    prototype();
                    break;

                case QUEUE_MESSAGE:
                    queueMessage();
                    break;

                default:
                    return super.automata();
            }

        }
        return true; // lies
    }

    private boolean shouldServiceRun() {
        return LeFunEntry.isEnabled();
    }

    @Override
    protected void setRetryTimerReal() {
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            I.serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), Constants.LEFUN_SERVICE_RETRY_ID);
            //PendingIntent.getService(xdrip.getAppContext(), Constants.LEFUN_SERVICE_RETRY_ID,
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


    // Mega Status
    public static List<StatusItem> megaStatus() {

        final List<StatusItem> l = new ArrayList<>();
        final Inst II = Inst.get(LeFunService.class.getSimpleName());

        l.add(new StatusItem("Model", LeFun.getModel()));
        l.add(new StatusItem("Mac address", LeFun.getMac()));

        l.add(new StatusItem("Connected", II.isConnected ? "Yes" : "No"));
        if (II.wakeup_time != 0) {
            final long till = msTill(II.wakeup_time);
            if (till > 0) l.add(new StatusItem("Wake Up", niceTimeScalar(till)));
        }
        // if (II.retry_time != 0) {
        //    l.add(new StatusItem("Retry", niceTimeScalar(msTill(II.retry_time))));
        //  }
        l.add(new StatusItem("State", II.state));

        final int qsize = II.getQueueSize();
        if (qsize > 0) {
            l.add(new StatusItem("Queue", qsize + " items"));
        }

        return l;
    }
}
