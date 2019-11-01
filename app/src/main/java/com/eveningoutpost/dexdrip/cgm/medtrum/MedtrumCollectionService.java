package com.eveningoutpost.dexdrip.cgm.medtrum;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.text.SpannableString;
import android.util.Pair;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Prediction;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Services.JamBaseBluetoothService;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.RxBleProvider;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AnnexARx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AuthRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AuthTx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.BackFillRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.BackFillTx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.BaseMessage;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.CalibrateRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.CalibrateTx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.ConnParamRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.ConnParamTx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.InboundStream;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.StatusRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.StatusTx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.TimeRx;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.TimeTx;
import com.eveningoutpost.dexdrip.ui.helpers.Span;
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.DisconnectReceiver;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//import rx.Subscription;
//import rx.schedulers.Schedulers;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertMedtrum;
import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.Models.JoH.quietratelimit;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MEDTRUM_SERVICE_FAILOVER_ID;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MEDTRUM_SERVICE_RETRY_ID;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.UtilityModels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.UtilityModels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.UtilityModels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.UtilityModels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.CGM_CHARACTERISTIC_INDICATE;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_AUTH_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_BACK_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_CALI_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_CONN_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_STAT_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Const.OPCODE_TIME_REPLY;
import static com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum.getSerial;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.CALIBRATE;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.CLOSE;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.CLOSED;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.CONNECT;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.ENABLE;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.INIT;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.SCAN;
import static com.eveningoutpost.dexdrip.cgm.medtrum.MedtrumCollectionService.STATE.SET_CONN_PARAM;
import static com.eveningoutpost.dexdrip.cgm.medtrum.SensorState.NotCalibrated;
import static com.eveningoutpost.dexdrip.cgm.medtrum.SensorState.Ok;
import static com.eveningoutpost.dexdrip.cgm.medtrum.TimeKeeper.timeStampFromTickCounter;

import static com.eveningoutpost.dexdrip.xdrip.gs;
/**
 *
 * jamorham
 *
 * Medtrum A6 collection service
 *
 */

public class MedtrumCollectionService extends JamBaseBluetoothService implements BtCallBack {

    protected String TAG = this.getClass().getSimpleName();
    private static final String STATIC_TAG = MedtrumCollectionService.class.getSimpleName();
    private static final String LAST_RECORD_TIME = "mtc-last-record-time";

    private static final long MINIMUM_RECORD_INTERVAL = 280000; // A6 gives records every 2 minutes but we want per 5 minutes as standard
    private static final long MAX_RETRY_BACKOFF_MS = 60000; // sleep for max ms if we have had no signal
    private static final int LISTEN_STASIS_SECONDS = 7200; // max time to be in listen state
    private volatile static String address = "";
    private static long serial;

    public static volatile String lastState = "Not running";
    public static volatile String lastErrorState = "";

    private static final int DEFAULT_AUTOMATA_DELAY = 100;

    private static volatile STATE state = INIT;
    private static volatile STATE last_automata_state = CLOSED;
    private static volatile boolean listen_connected = false;

    private static Scanner scanner;

    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    private static volatile Subscription indicationSubscription;
    private static volatile Subscription notificationSubscription;

    private final RxBleClient rxBleClient = RxBleProvider.getSingleton();
    private volatile RxBleConnection connection;
    private volatile RxBleDevice bleDevice;


    private static PendingIntent serviceIntent;
    private static PendingIntent serviceFailoverIntent;
    private static long retry_time;
    private static long failover_time;
    private static long last_wake_up_time;
    private static long retry_backoff = 0;
    private static long lastRecordTime = -1;
    private static volatile long lastInteractionTime = -1;
    private static int requestedBackfillSize = 0;

    private static AnnexARx lastAnnex = null;

    private static long wakeup_time = 0;
    private static long wakeup_jitter = 0;
    private static long max_wakeup_jitter = 0;


    // TODO can we use display glucose instead of calculated value anywhere?

    // Internal process state tracking
    public enum STATE {
        INIT("Initializing"),
        SCAN("Scanning"),
        CONNECT("Waiting connect"),
        ENABLE("Enabling"),
        DISCOVER("Examining"),
        SET_TIME("Setting Time"),
        SET_CONN_PARAM("Setting Parameters"),
        CALIBRATE("Check Calibration"),
        LISTEN("Listening"),
        GET_DATA("Getting Data"),
        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping");

        private static List<STATE> sequence = new ArrayList<>();

        private final String str;

        STATE(String custom) {
            this.str = custom;
        }

        STATE() {
            this.str = toString();
        }

        static {
            sequence.add(ENABLE);
            sequence.add(GET_DATA);
            sequence.add(SET_TIME);
            sequence.add(SET_CONN_PARAM);
            sequence.add(CALIBRATE);
            sequence.add(LISTEN);
        }

        public String getString() {
            return str;
        }

        public STATE next() {
            try {
                return sequence.get(sequence.indexOf(this) + 1);
            } catch (Exception e) {
                return LISTEN;
            }
        }

    }


    public synchronized boolean automata() {

        if ((last_automata_state != state) || (JoH.ratelimit("jam-g5-dupe-auto", 2))) {
            last_automata_state = state;
            final PowerManager.WakeLock wl = JoH.getWakeLock("jam-g5-automata", 60000);
            try {
                switch (state) {

                    case INIT:
                        initialize();
                        break;
                    case SCAN:
                        scan_for_device();
                        break;
                    case CONNECT:
                        connect_to_device();
                        break;
                    case ENABLE:
                        retry_backoff = 0; // we have a connection
                        enable_features_and_listen();
                        break;
                    case DISCOVER:
                        changeState(state.next());
                        break;
                    case CALIBRATE:
                        check_calibrate();
                        break;
                    case GET_DATA:
                        get_data();
                        break;
                    case SET_TIME:
                        if (JoH.pratelimit("medtrum-set-time-" + serial, 60)) {
                            sendTx(new TimeTx());
                        } else {
                            changeState(state.next());
                        }
                        break;
                    case SET_CONN_PARAM:
                        sendTx(new ConnParamTx());
                        break;
                    case CLOSE:
                        //prepareToWakeup(); // TODO this skips service wake up
                        stopConnect();
                        setRetryTimer();
                        state = CLOSED;
                        //changeState(INIT);
                        break;
                    case CLOSED:
                        setRetryTimer();
                        break;
                    case LISTEN:
                        status("Listening");
                        if (notificationSubscription == null || notificationSubscription.isUnsubscribed()) {
                            changeState(SCAN);
                        }
                        break;

                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
        } else {
            UserError.Log.d(TAG, "Ignoring duplicate automata state within 2 seconds: " + state);
        }
        return true;
    }

    ///

    private void initialize() {
        serial = getSerial();
        if (serial != 0) {
            changeState(SCAN);
        } else {
            final String msg = "Medtrum cannot start without serial number - please rescan";
            if (JoH.ratelimit("medtrum-cannot-start", 120)) {
                UserError.Log.e(TAG, msg);
            }
            JoH.static_toast_long(msg);
        }
    }

    private void scan_for_device() {
        status(gs(R.string.scanning));
        UserError.Log.d(TAG, "Scanning for device");
        scanner.setAddress(address).scan();
    }

    private void connect_to_device() {
        if (JoH.quietratelimit("medtrum-connect-cooldown", 2)) {
            status("Connecting");
            UserError.Log.d(TAG, "Connecting to device: " + address);
            connect_to_device(false);
        }

    }


    private synchronized void enable_features_and_listen() {
        UserError.Log.d(TAG, "enable features - enter");
        stopListening();
        if (connection != null) {
            notificationSubscription = new Subscription(connection.setupNotification(Const.CGM_CHARACTERISTIC_NOTIFY)
                    .timeout(LISTEN_STASIS_SECONDS, TimeUnit.SECONDS) // WARN
                    .observeOn(Schedulers.newThread())
                    .doOnNext(notificationObservable -> {

                        UserError.Log.d(TAG, "Notifications enabled");


                    })
                    .flatMap(notificationObservable -> notificationObservable)
                    .subscribe(bytes -> {
                                final PowerManager.WakeLock wl = JoH.getWakeLock("medtrum-receive-n", 60000);
                                try {
                                    UserError.Log.d(TAG, "Received notification bytes: " + JoH.bytesToHex(bytes));
                                    lastInteractionTime = JoH.tsl();
                                    setFailOverTimer();
                                    lastAnnex = new AnnexARx(bytes);
                                    UserError.Log.d(TAG, "Notification: " + lastAnnex.toS());
                                    createRecordFromAnnexData(lastAnnex);
                                    backFillIfNeeded(lastAnnex);

                                } finally {
                                    JoH.releaseWakeLock(wl);
                                }
                            }, throwable -> {
                                UserError.Log.d(TAG, "notification throwable: " + throwable);
                            }
                    ));


            final InboundStream inboundStream = new InboundStream();

            indicationSubscription = new Subscription(connection.setupIndication(CGM_CHARACTERISTIC_INDICATE)
                    .timeout(LISTEN_STASIS_SECONDS, TimeUnit.SECONDS) // WARN
                    .observeOn(Schedulers.newThread())
                    .doOnNext(notificationObservable -> {

                        UserError.Log.d(TAG, "Indications enabled");

                        sendTx(new AuthTx(serial));

                    })
                    .flatMap(notificationObservable -> notificationObservable)
                    .subscribe(bytes -> {

                                final PowerManager.WakeLock wl = JoH.getWakeLock("medtrum-receive-i", 60000);
                                try {
                                    UserError.Log.d(TAG, "Received indication bytes: " + JoH.bytesToHex(bytes));
                                    if (inboundStream.hasSomeData() && msSince(lastInteractionTime) > Constants.SECOND_IN_MS * 10) {
                                        UserError.Log.d(TAG, "Resetting stream due to earlier timeout");
                                    }
                                    lastInteractionTime = JoH.tsl();
                                    inboundStream.push(bytes);
                                    if (!checkAndProcessInboundStream(inboundStream)) {
                                        Inevitable.task("mt-reset-stream-no-data", 3000, () -> {
                                            if (inboundStream.hasSomeData()) {
                                                UserError.Log.d(TAG, "Resetting stream as incomplete after 3s");
                                                inboundStream.reset();
                                            }
                                        });
                                    }

                                } finally {
                                    JoH.releaseWakeLock(wl);
                                }
                            }, throwable -> {
                                UserError.Log.d(TAG, "indication throwable: " + throwable);
                            }
                    ));
        } else {
            UserError.Log.e(TAG, "Connection null when trying to set notifications");
        }

    }

    private boolean checkAndProcessInboundStream(final InboundStream inboundStream) {
        if (inboundStream.isComplete()) {
            final byte[] complete = inboundStream.getByteSequence();
            inboundStream.reset();
            Inevitable.task("mt-stream-done:" + JoH.tsl(), 50, () -> parseInboundPacket(complete));
            return true;
        }
        return false;
    }

    private synchronized void connect_to_device(boolean auto) {
        if (state == CONNECT) {
            // TODO check mac
            //UserError.Log.d(TAG, "Address length: " + address.length());
            if (address != null && address.length() > 6) {
                status("Connecting");

                stopConnect();

                bleDevice = rxBleClient.getBleDevice(address);

                // Listen for connection state changes
                stateSubscription = new Subscription(bleDevice.observeConnectionStateChanges()
                        // .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onConnectionStateChange, throwable -> {
                            UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                        }));

                // Attempt to establish a connection
                listen_connected = false;
                auto = false; // auto not allowed due to timeout
                connectionSubscription = new Subscription(bleDevice.establishConnection(auto)
                        .timeout(LISTEN_STASIS_SECONDS, TimeUnit.SECONDS)
                        // .flatMap(RxBleConnection::discoverServices)
                        // .observeOn(AndroidSchedulers.mainThread())
                        // .doOnUnsubscribe(this::clearSubscription)
                        .subscribeOn(Schedulers.io())

                        .subscribe(this::onConnectionReceived, this::onConnectionFailure));

            } else {
                UserError.Log.wtf(TAG, "No transmitter mac address!");
                changeState(SCAN);
            }

        } else {
            UserError.Log.wtf(TAG, "Attempt to connect when not in CONNECT state");
        }
    }

    private void get_data() {
        status("Asking for data");
        sendTx(new StatusTx());
    }

    private void check_calibrate() {
        final Pair<Long, Integer> calibration = Medtrum.getCalibration();
        if (calibration != null) {
            status("Calibrating");
            try {
                sendTx(new CalibrateTx(serial, calibration.first, calibration.second));
            } catch (InvalidAlgorithmParameterException e) {
                UserError.Log.wtf(TAG, "Cannot calibrate: " + e);
            }


        } else {
            changeState(state.next());
        }
    }

    private void sendTx(BaseMessage msg) {
        if (connection != null) {
            try {
                connection.writeCharacteristic(CGM_CHARACTERISTIC_INDICATE, nn(msg.getByteSequence()))
                        .subscribe(
                                characteristicValue -> {
                                    UserError.Log.d(TAG, "Wrote " + msg.getClass().getSimpleName() + " request: ");
                                }, throwable -> {
                                    UserError.Log.e(TAG, "Failed to write " + msg.getClass().getSimpleName() + " " + throwable);
                                });
            } catch (NullPointerException e) {
                UserError.Log.e(TAG, "Race condition when writing characteristic: " + e);
            }
        }
    }

    ///

    private void parseInboundPacket(byte[] packet) {
        if (packet == null) {
            UserError.Log.e(TAG, "Was passed null in parseInbound");
            return;
        }
        //

        if (packet.length < 2) {
            UserError.Log.e(TAG, "Packet too short");
            return;
        }

        final int opcode = packet[1] & 0xff;


        switch (opcode) {
            case OPCODE_AUTH_REPLY:
                final AuthRx authrx = new AuthRx(packet);
                if (authrx.isValid()) {
                    status("Authenticated");
                } else {
                    errorStatus("AUTHENTICATION FAILED!");
                    if (JoH.ratelimit("medtrum-auth-fail", 600)) {
                        UserError.Log.wtf(TAG, "Auth packet failure: " + serial + authrx.toS());
                    }
                }
                changeState(state.next());
                break;

            case OPCODE_STAT_REPLY:
                final StatusRx statusrx = new StatusRx(packet);
                UserError.Log.d(TAG, statusrx.toS());
                boolean asking_backfill = false;
                if (statusrx.isValid()) {
                    lastAnnex = statusrx.getAnnex();
                    statusrx.getAnnex().processForTimeKeeper(serial);

                    createRecordFromAnnexData(statusrx.getAnnex());
                    asking_backfill = backFillIfNeeded(statusrx.getAnnex());
                }

                changeState(state.next(), asking_backfill ? 1500 : DEFAULT_AUTOMATA_DELAY);
                break;

            case OPCODE_TIME_REPLY:
                final TimeRx timeRx = new TimeRx(packet);
                final String msg = "Got time set reply: " + (timeRx.isValid() ? "VALID" : "INVALID");
                if (timeRx.isValid()) {
                    status("Set time");
                    UserError.Log.d(TAG, msg);
                } else {
                    status("Error setting time");
                    UserError.Log.e(TAG, msg);
                }
                changeState(SET_CONN_PARAM);
                break;

            case OPCODE_CONN_REPLY:
                UserError.Log.d(TAG, "Got connection parameter reply");
                final ConnParamRx connParamRx = new ConnParamRx(packet);
                status("Parameter reply");
                if (!connParamRx.isValid()) {
                    UserError.Log.e(TAG, "Got invalid connection parameter reply msg");
                }
                changeState(state.next());
                break;

            case OPCODE_CALI_REPLY:
                UserError.Log.d(TAG, "Got calibration reply");

                Medtrum.clearCalibration();

                final CalibrateRx calibrateRx = new CalibrateRx(packet);
                String thismsg;
                if (calibrateRx.isOk()) {
                    thismsg = "Calibration OK";
                } else {
                    thismsg = "Calibration Error " + calibrateRx.getErrorCode();
                }
                status(thismsg);
                UserError.Log.ueh(TAG, thismsg);
                JoH.static_toast_long(thismsg);
                changeState(state.next());
                break;


            case OPCODE_BACK_REPLY:
                UserError.Log.d(TAG, "Got backfill reply");
                status("Got back fill");
                processBackFillPacket(packet);
                break;

            default:
                UserError.Log.d(TAG, "Unknown inbound opcode: " + opcode);
                UserError.Log.e(TAG, "Received unknown inbound packet: " + HexDump.dumpHexString(packet));
        }
    }


    private void processBackFillPacket(final byte[] packet) {
        final BackFillRx backFillRx = new BackFillRx(packet);
        UserError.Log.d(TAG, backFillRx.toS());
        if (backFillRx.isOk()) {
            boolean changed = false;
            final List<Integer> backsies = backFillRx.getRawList();
            if (backsies != null) {
                for (int index = 0; index < backsies.size(); index++) {
                    final long timestamp = timeStampFromTickCounter(serial, backFillRx.sequenceStart + index);
                    UserError.Log.d(TAG, "Backsie:  id:" + (backFillRx.sequenceStart + index) + " raw:" + backsies.get(index) + " @ " + JoH.dateTimeText(timestamp));
                    final long since = msSince(timestamp);
                    if ((since > HOUR_IN_MS * 6) || (since < 0)) {
                        UserError.Log.wtf(TAG, "Backfill timestamp unrealistic: " + JoH.dateTimeText(timestamp) + " (ignored)");
                    } else {

                        final double glucose = backFillRx.getGlucose(backsies.get(index));
                        final int scaled_raw_data = backFillRx.getSensorRawEmulateDex(backsies.get(index));
                        if (BgReading.getForPreciseTimestamp(timestamp, (long)(Constants.MINUTE_IN_MS * 2.5)) == null) {

                            if (isNative()) {
                                // Native version
                                if (glucose > 0) {
                                    BgReading.bgReadingInsertMedtrum(glucose, timestamp, "Backfill", scaled_raw_data);
                                    UserError.Log.d(TAG, "Adding native backfilled reading: " + JoH.dateTimeText(timestamp) + " " + BgGraphBuilder.unitized_string_static(glucose));

                                }
                                final BgReading bgReadingTemp = BgReading.createFromRawNoSave(null, null, scaled_raw_data, scaled_raw_data, timestamp);
                                if (bgReadingTemp.calculated_value > 0) {
                                    Prediction.create(bgReadingTemp.timestamp, (int) bgReadingTemp.calculated_value, "Medtrum2nd").save();
                                }
                            } else {
                                if (glucose > 0) {
                                    Prediction.create(timestamp, (int) glucose, "Medtrum2nd").save();
                                }
                                // xDrip as primary
                                final BgReading bgreading = BgReading.create(scaled_raw_data, scaled_raw_data, xdrip.getAppContext(), timestamp);
                                if (bgreading != null) {
                                    UserError.Log.d(TAG, "Backfilled BgReading created: " + bgreading.uuid + " " + JoH.dateTimeText(bgreading.timestamp));
                                } else {
                                    UserError.Log.d(TAG, "BgReading null!");
                                }
                            }

                            Inevitable.task("backfill-ui-update", 3000, Home::staticRefreshBGChartsOnIdle);
                            changed = true;
                        }
                    }
                }
                if (!changed && backsies.size() < requestedBackfillSize) {
                    if (JoH.ratelimit("mt-backfill-repeat", 60)) {
                        UserError.Log.d(TAG, "Requesting additional backfill with offset: " + backsies.size());
                        backFillIfNeeded(lastAnnex, backsies.size());
                    }
                }
            }
        } else {
            UserError.Log.e(TAG, "Backfill data reports not ok");
        }
    }

    private static final int MAX_BACKFILL_ENTRIES = 30;

    private boolean backFillIfNeeded(final AnnexARx annex) {
        return backFillIfNeeded(annex, 0);
    }

    private boolean backFillIfNeeded(final AnnexARx annex, int offset) {
        if (annex == null) return false;
        if (!annex.isStateOkForBackFill()) return false;
        final Pair<Long, Long> backfillTimes = BackfillAssessor.check();
        if (backfillTimes != null) {
            int startTick = TimeKeeper.tickCounterFromTimeStamp(serial, backfillTimes.first);
            int endTick = TimeKeeper.tickCounterFromTimeStamp(serial, backfillTimes.second);
            if (endTick >= annex.sensorAge) endTick = annex.sensorAge - 1;
            if (startTick < 62) startTick = 62; // after warmup only
            if (endTick < 1) endTick = 1;
            startTick += offset;
            if ((startTick != endTick) && (endTick > startTick)) {
                if (endTick - startTick > MAX_BACKFILL_ENTRIES) {
                    endTick = startTick + MAX_BACKFILL_ENTRIES; // only ask this many at once
                }
                UserError.Log.d(TAG, "Ask backfill: start: " + startTick + "  end: " + endTick);
                requestedBackfillSize = endTick - startTick;
                sendTx(new BackFillTx(startTick, endTick));
                return true;
            } else {
                UserError.Log.d(TAG, "Not backfilling with start and end tick at: " + startTick + " " + endTick);
            }
        }
        return false;
    }

    private boolean isNative() {
        return Pref.getBooleanDefaultFalse("medtrum_use_native");
    }


    private void createRecordFromAnnexData(AnnexARx annex) {

        if (annex == null) return;
        // TODO check annex sanity
        status("Got data");

        if (lastRecordTime == 0) {
            lastRecordTime = PersistentStore.getLong(LAST_RECORD_TIME);
        }
        if (msSince(lastRecordTime) > MINIMUM_RECORD_INTERVAL) {
            UserError.Log.d(TAG, "Creating transmitter data from record annex");
            final TransmitterData transmitterData = TransmitterData.create(annex.getSensorRawEmulateDex(), annex.getSensorRawEmulateDex(), annex.getBatteryPercent(), JoH.tsl());
            if (transmitterData != null) {
                lastRecordTime = transmitterData.timestamp;
                PersistentStore.setLong(LAST_RECORD_TIME, transmitterData.timestamp);
                if (transmitterData.raw_data > 0) {
                    // TODO sanity check raw data etc before creating

                    // TODO sensor good flag???
                    if (annex.getState() == Ok || annex.getState() == NotCalibrated) {
                        final double glucose = annex.calculatedGlucose();
                        if (isNative()) {
                            if (glucose > 0) {
                                final BgReading bgReading = bgReadingInsertMedtrum(glucose, JoH.tsl(), null, transmitterData.raw_data);
                            } else {
                                if (annex.getState() == NotCalibrated) {
                                    // just add raw data
                                    UserError.Log.d(TAG,"Just adding raw data");
                                    final BgReading bgreading = BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
                                }
                            }
                            // xDrip calibration as secondary trace
                            final BgReading bgReadingTemp = BgReading.createFromRawNoSave(null, null, transmitterData.raw_data, transmitterData.raw_data, transmitterData.timestamp);
                            if (bgReadingTemp.calculated_value > 0) {
                                Prediction.create(bgReadingTemp.timestamp, (int) bgReadingTemp.calculated_value, "Medtrum2nd").save();
                                UserError.Log.d(TAG, "Created secondary trace for value: " + bgReadingTemp.calculated_value);
                            }
                        } else {

                            if (glucose > 0) {
                                Prediction.create(JoH.tsl(), (int) glucose, "Medtrum2nd").save();
                                UserError.Log.d(TAG, "Saving extra data");
                            }
                            // xDrip as primary
                            final BgReading bgreading = BgReading.create(transmitterData.raw_data, transmitterData.filtered_data, xdrip.getAppContext(), transmitterData.timestamp);
                            if (bgreading != null) {
                                UserError.Log.d(TAG, "BgReading created: " + bgreading.uuid + " " + JoH.dateTimeText(bgreading.timestamp));
                            } else {
                                UserError.Log.d(TAG, "BgReading null!");
                            }
                        }
                    } else {
                        UserError.Log.d(TAG, "Ignoring due to sensor state: " + annex.getState());
                    }
                } else {
                    UserError.Log.d(TAG, "Raw data was invalid so not proceeding");
                }
            }
        } else {
            UserError.Log.d(TAG, "Not creating data record so close to previous");
        }
    }

    ///


    private synchronized void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        String connection_state = "Unknown";
        switch (newState) {
            case CONNECTING:
                connection_state = "Connecting";
                // connecting_time = JoH.tsl();
                break;
            case CONNECTED:
                connection_state = "Connected";
                retry_backoff = 0;
                break;
            case DISCONNECTING:
                connection_state = "Disconnecting";
                break;
            case DISCONNECTED:
                connection_state = "Disconnected";
                status("Disconnected");
                changeState(CLOSE);
                break;
        }
        status(connection_state);

        if (connection_state.equals("Disconnecting")) {
            tryGattRefresh(connection);
        }

    }

    // We have connected to the device!
    private void onConnectionReceived(final RxBleConnection this_connection) {
        listen_connected = true;
        status("Connected");
        // TODO close off existing connection?
        connection = this_connection;
        if (this_connection != null) {
            changeState(ENABLE);
        } else {
            UserError.Log.d(TAG, "New connection null!");
            changeState(CLOSE);
        }
    }

    private void onConnectionFailure(Throwable throwable) {
        if (listen_connected) {
            status("Disconnected");
        } else {
            status("Connection failure");
        }
        // TODO under what circumstances should we change state or do something here?
        UserError.Log.d(TAG, "Connection Disconnected/Failed: " + throwable);
        stopConnect();
        changeState(CLOSE);
        setRetryTimer();
    }

    private synchronized void stopConnect() {

        UserError.Log.d(TAG, "Stopping connection with: " + address);
        stopListening();

        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
        }
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
        }

    }

    private void stopListening() {
        if (indicationSubscription != null) {
            indicationSubscription.unsubscribe();
        }
        if (notificationSubscription != null) {
            notificationSubscription.unsubscribe();
        }
    }

    public STATE getState() {
        return state;
    }

    public void changeState(STATE new_state) {
        changeState(new_state, DEFAULT_AUTOMATA_DELAY);
    }

    public void changeState(STATE new_state, int timeout) {
        if ((state == CLOSED || state == CLOSE) && new_state == CLOSE) {
            UserError.Log.d(TAG, "Not closing as already closed");
        } else {
            if (new_state != state) {
                UserError.Log.d(TAG, "Changing state from: " + state + " to " + new_state);
                state = new_state;
                background_automata(timeout);
            } else {
                UserError.Log.d(TAG, "Not changing state as already in state: " + new_state);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (scanner == null) {
            scanner = new Scanner();
        }
        scanner.setTag(TAG);
        scanner.addCallBack(this, TAG);
        DisconnectReceiver.addCallBack(this, TAG);
        UserError.Log.d(TAG, "SERVICE CREATED - SERVICE CREATED");
        enableBuggySamsungIfNeeded(TAG);
    }


    @Override
    public void onDestroy() {
        try {
            scanner.stop();
        } catch (Exception e) {
            //
        }
        stopConnect();
        DisconnectReceiver.removeCallBack(TAG);
        wakeup_time = 0;
        last_automata_state = CLOSED;
        status("Stopped");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        xdrip.checkAppContext(getApplicationContext());
        if (shouldServiceRun()) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("medtrum-start-service", 600000);
            try {
                UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP WAKE UP @ " + JoH.dateTimeText(JoH.tsl()) + " State: " + state);
                setFailOverTimer();

                if (wakeup_time > 0) {
                    wakeup_jitter = JoH.msSince(wakeup_time);
                    if (wakeup_jitter < 0) {
                        UserError.Log.d(TAG, "Woke up Early..");
                    } else {
                        if (wakeup_jitter > 1000) {
                            UserError.Log.d(TAG, "Wake up, time jitter: " + JoH.niceTimeScalar(wakeup_jitter));
                            if ((wakeup_jitter > TOLERABLE_JITTER) && (!JoH.buggy_samsung) && JoH.isSamsung()) {
                                UserError.Log.wtf(TAG, "Enabled Buggy Samsung workaround due to jitter of: " + JoH.niceTimeScalar(wakeup_jitter));
                                JoH.buggy_samsung = true;
                                PersistentStore.incrementLong(BUGGY_SAMSUNG_ENABLED);
                                max_wakeup_jitter = 0;
                            } else {
                                max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
                            }

                        }
                    }
                    wakeup_time = 0; // don't check this one again
                }


                retry_time = 0; // we have woken up
                last_wake_up_time = JoH.tsl();
                try {
                    address = ActiveBluetoothDevice.first().address;
                } catch (NullPointerException e) {
                    // bluetooth device not set - launch scan ui???
                }

                processInitialState();
                background_automata();

                if (intent != null) {

                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
            return START_STICKY;
        } else {
            UserError.Log.d(TAG, "Should not be running so shutting down");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    private void processInitialState() {
        switch (state) {
            case INIT:
            case SCAN:
                return;
            case CLOSE:
            case CLOSED:
                UserError.Log.d(TAG, "Changing from initial state of " + state + " to INIT");
                state = INIT;
                return;
        }
        if (msSince(lastInteractionTime) > MINUTE_IN_MS * 5) {
            UserError.Log.d(TAG, "Changing from initial state of " + state + " to INIT due to interaction timeout");
            state = INIT;
        }
    }

    @Override
    public void btCallback(String address, String status) {
        UserError.Log.d(TAG, "Processing callback: " + address + " :: " + status);
        if (address.equals(MedtrumCollectionService.address)) {
            switch (status) {
                case "DISCONNECTED":
                    changeState(CLOSE);
                    break;
                case "SCAN_FOUND":
                    changeState(CONNECT);
                    break;
                case "SCAN_TIMEOUT":
                    status("Scan timed out");
                    changeState(CONNECT);
                    // setRetryTimer();
                    break;
                case "SCAN_FAILED":
                    status("Scan Failed!");
                    changeState(CONNECT); // if location services off etc
                    break;

                default:
                    UserError.Log.e(TAG, "Unknown status callback for: " + address + " with " + status);
            }
        } else {
            UserError.Log.d(TAG, "Ignoring: " + status + " for " + address + " as we are using: " + MedtrumCollectionService.address);
        }
    }


    private static void status(String msg) {
        lastState = msg + " " + JoH.hourMinuteString();
        UserError.Log.d(STATIC_TAG, "Status: " + lastState);
    }

    private static void errorStatus(String msg) {
        lastErrorState = msg + " " + JoH.hourMinuteString();
        UserError.Log.e(STATIC_TAG, lastErrorState);
    }


    @SuppressLint("ObsoleteSdkInt")
    private static boolean shouldServiceRun() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && DexCollectionType.getDexCollectionType().equals(DexCollectionType.Medtrum);
    }

    private void setRetryTimer() {
        Inevitable.task("mt-set-retry", 500, this::setRetryTimerReal);
    }

    private void setRetryTimerReal() {
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            // serviceIntent = PendingIntent.getService(this, MEDTRUM_SERVICE_RETRY_ID,
            //         new Intent(this, this.getClass()), 0);
            serviceIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), MEDTRUM_SERVICE_RETRY_ID);
            retry_time = JoH.wakeUpIntent(this, retry_in, serviceIntent);
            wakeup_time = JoH.tsl() + retry_in;
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void setFailOverTimer() {
        if (shouldServiceRun()) {
            if (quietratelimit("mt-failover-cooldown", 30)) {
                final long retry_in = Constants.MINUTE_IN_MS * 7;
                UserError.Log.d(TAG, "setFailOverTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
             //   serviceFailoverIntent = PendingIntent.getService(this, MEDTRUM_SERVICE_FAILOVER_ID,
             //           new Intent(this, this.getClass()), 0);
                serviceFailoverIntent = WakeLockTrampoline.getPendingIntent(this.getClass(), MEDTRUM_SERVICE_FAILOVER_ID);
                failover_time = JoH.wakeUpIntent(this, retry_in, serviceFailoverIntent);
            }
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private static long whenToRetryNext() {
        retry_backoff += Constants.SECOND_IN_MS;
        if (retry_backoff > MAX_RETRY_BACKOFF_MS) {
            retry_backoff = MAX_RETRY_BACKOFF_MS;
        }
        return Constants.SECOND_IN_MS * 10 + retry_backoff;
    }


    public static SpannableString nanoStatus() {
        if (JoH.emptyString(lastErrorState)) return null;
        return Span.colorSpan(lastErrorState, CRITICAL.color());
    }

    // Mega Status
    public static List<StatusItem> megaStatus() {

        if (lastAnnex == null) {
            lastAnnex = com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum.getLastAdvertAnnex();
            // add some advert only marker
        }

        final List<StatusItem> l = new ArrayList<>();

        l.add(new StatusItem("Phone Service State", lastState));
        if (!JoH.emptyString(lastErrorState)) {
            l.add(new StatusItem("Error", lastErrorState, BAD));
        }
        if (lastAnnex != null) {
            l.add(new StatusItem("Battery", lastAnnex.getBatteryPercent() + "%"));
            if (lastAnnex.charging) {
                l.add(new StatusItem("Charging", lastAnnex.charged ? "Charged" : "On charge"));
            }
            l.add(new StatusItem("Sensor State", lastAnnex.getState().getDescription(), lastAnnex.getState() == Ok ? GOOD : NORMAL));

            if (lastAnnex.getState() == SensorState.WarmingUp1) {
                l.add(new StatusItem("Warm up", "Initial warm up", StatusItem.Highlight.NOTICE));
            } else if (lastAnnex.getState() == SensorState.WarmingUp2) {
                final long warmupMinsLeft = ((2 * HOUR_IN_MS) - lastAnnex.getSensorAgeInMs()) / Constants.MINUTE_IN_MS;
                if (warmupMinsLeft > -1 && warmupMinsLeft < 121) {
                    l.add(new StatusItem("Warmup left", warmupMinsLeft + " mins", StatusItem.Highlight.NOTICE));
                }
            }

            if (lastAnnex.getSensorAgeInMs() > 0) {
                l.add(new StatusItem("Sensor Age", lastAnnex.getNiceSensorAge()));
            }

            if (lastAnnex.sensorGood) {
                l.add(new StatusItem("Sensor", "Good", GOOD));
                // } else {
                // if (lastAnnex.getState() == Ok) {
                //     l.add(new StatusItem("Sensor", "Ok", StatusItem.Highlight.NORMAL));
                // }
            }

            if (lastAnnex.sensorError) {
                l.add(new StatusItem("Sensor Error", "Error", BAD));
            }

            if (lastAnnex.sensorFail) {
                l.add(new StatusItem("Sensor Fail", "FAILED", CRITICAL));
            }

            if (lastAnnex.calibrationErrorA) {
                l.add(new StatusItem("Calibration Error", "Error A", BAD));
            }
            if (lastAnnex.calibrationErrorB) {
                l.add(new StatusItem("Calibration Error", "Error B", BAD));
            }

            final Pair<Long, Integer> calibration = Medtrum.getCalibration();
            if (calibration != null) {
                l.add(new StatusItem("Blood test", BgGraphBuilder.unitized_string_with_units_static(calibration.second) + " @ " + JoH.hourMinuteString(calibration.first), StatusItem.Highlight.NOTICE));
            }

            if (serial != 0) {
                final int version = Medtrum.getVersion(serial);
                l.add(new StatusItem("Serial", "" + serial));
                if (version > 0) {
                    final String versionString = "v" + (JoH.qs(version / 100d, 2));
                    l.add(new StatusItem("Firmware", versionString));
                }
            }

            if (lastAnnex.getState() == Ok || lastAnnex.getState() == SensorState.NotCalibrated) {
                l.add(new StatusItem("Slope", JoH.qs(lastAnnex.calibrationSlope / 1000d, 3) + " (" + JoH.qs(1d / (lastAnnex.calibrationSlope / 1000d), 2) + ")"));
                l.add(new StatusItem("Intercept", lastAnnex.calibrationIntercept));
                l.add(new StatusItem("Raw Data", lastAnnex.sensorRaw));
            }

            if (lastAnnex.getState() == Ok) {
                l.add(new StatusItem("Last Glucose", BgGraphBuilder.unitized_string_with_units_static(lastAnnex.calculatedGlucose()) + (lastAnnex.recent() ? "" : " @ " + JoH.niceTimeScalarShort(JoH.msSince(lastAnnex.created))), lastAnnex.recent() ? NORMAL : StatusItem.Highlight.NOTICE));
            }

            if (retry_time != 0) {
                l.add(new StatusItem("Wake up in", JoH.niceTimeScalar(msTill(retry_time))));
            }
            if (failover_time != 0) {
                l.add(new StatusItem("System check in", JoH.niceTimeScalar(msTill(failover_time))));
            }

            if (Home.get_engineering_mode()) {
                l.add(new StatusItem("Brain State", state.getString()));
                l.add(new StatusItem("Last Interaction", JoH.niceTimeScalar(msSince(lastInteractionTime))));
                l.add(new StatusItem("Last Wake Up", JoH.niceTimeScalar(msSince(last_wake_up_time))));
            }

        } else {
            l.add(new StatusItem("Status Information", "Nothing Yet", StatusItem.Highlight.NOTICE));
        }
        return l;
    }

    // accessed via reflection
    public static boolean isRunning() {
        return !lastState.equals("Not Running") && !lastState.startsWith("Stop");
    }

    // accessed via reflection
    public static boolean isCollecting() {
        return JoH.msSince(lastInteractionTime) < (Constants.MINUTE_IN_MS * 5);

    }

    public static void calibratePing() {
        if (shouldServiceRun()) {
            state = CALIBRATE;
            JoH.startService(MedtrumCollectionService.class);
        }
    }

}
