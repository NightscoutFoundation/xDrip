package com.eveningoutpost.dexdrip.cgm.medtrum;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Pair;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.Models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Prediction;
import com.eveningoutpost.dexdrip.Models.TransmitterData;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Services.JamBaseBluetoothService;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.UtilityModels.RxBleProvider;
import com.eveningoutpost.dexdrip.UtilityModels.StatusItem;
import com.eveningoutpost.dexdrip.cgm.medtrum.messages.AnnexARx;
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
import com.eveningoutpost.dexdrip.utils.BtCallBack;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.DisconnectReceiver;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;

import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Subscription;
import rx.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.Models.BgReading.bgReadingInsertMedtrum;
import static com.eveningoutpost.dexdrip.Models.JoH.msSince;
import static com.eveningoutpost.dexdrip.Models.JoH.msTill;
import static com.eveningoutpost.dexdrip.UtilityModels.Constants.HOUR_IN_MS;
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
import static com.eveningoutpost.dexdrip.cgm.medtrum.SensorState.Ok;
import static com.eveningoutpost.dexdrip.cgm.medtrum.TimeKeeper.timeStampFromTickCounter;

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

    private static final long MINIMUM_RECORD_INTERVAL = 280000; // A6 gives records every 2 minutes but we want per 5 minutes as standard
    private static final long MAX_RETRY_BACKOFF_MS = 60000; // sleep for max ms if we have had no signal
    private static String address = "";
    private static long serial;

    public static String lastState = "Not running";
    public static String lastError = null;

    private static final int DEFAULT_AUTOMATA_DELAY = 100;

    private static volatile STATE state = INIT;
    private static volatile STATE last_automata_state = CLOSED;

    private static Scanner scanner;

    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    //private static volatile Subscription discoverSubscription;
    private static volatile Subscription indicationSubscription;
    private static volatile Subscription notificationSubscription;

    private final RxBleClient rxBleClient = RxBleProvider.getSingleton();
    private volatile RxBleConnection connection;
    private volatile RxBleDevice bleDevice;


    private static PendingIntent serviceIntent;
    private static PendingIntent serviceFailoverIntent;
    private static long retry_time;
    private static long retry_backoff = 0;
    private static long lastRecordTime = -1;
    private static long lastInteractionTime = -1;
    private static int requestedBackfillSize = 0;

    private static AnnexARx lastAnnex = null;


    // TODO service wake up healthchecker alarm - check last notification within time - try reconnect if not

    // Internal process state tracking
    public enum STATE {
        INIT("Initializing"),
        SCAN("Scanning"),
        CONNECT("Waiting connect"),
        ENABLE("Enabling"),
        DISCOVER("Examining"),
        CHECK_AUTH("Checking Auth"),
        SET_TIME("Setting Time"),
        SET_CONN_PARAM("Setting Parameters"),
        CALIBRATE("Check Calibration"),
        BOND("Bonding"),
        LISTEN("Listening"),
        GET_DATA("Getting Data"),
        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping");

        private static List<STATE> sequence = new ArrayList<>();

        private String str;

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


    public synchronized void automata() {

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
                    case CHECK_AUTH:
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
                        //prepareToWakeup();
                        changeState(INIT);
                        break;
                    case CLOSED:
                        //handleWakeup();
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
        status("Scanning");
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

    private final int LISTEN_STASIS_SECONDS = 3600;

    private synchronized void enable_features_and_listen() {
        UserError.Log.d(TAG, "enable features - enter");
        stopListening();

        notificationSubscription = connection.setupNotification(Const.CGM_CHARACTERISTIC_NOTIFY)
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
                );


        final InboundStream inboundStream = new InboundStream();

        indicationSubscription = connection.setupIndication(CGM_CHARACTERISTIC_INDICATE)
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
                                lastInteractionTime = JoH.tsl();
                                inboundStream.push(bytes);
                                if (!checkAndProcessInboundStream(inboundStream)) {
                                    Inevitable.task("mt-reset-stream-no-data", 2000, () -> {
                                        if (inboundStream.hasSomeData()) {
                                            UserError.Log.d(TAG, "Resetting stream as incomplete after 2s");
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
                );

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
            if (address != null) {
                status("Connecting");

                stopConnect();

                bleDevice = rxBleClient.getBleDevice(address);

                // Listen for connection state changes
                stateSubscription = bleDevice.observeConnectionStateChanges()
                        // .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onConnectionStateChange, throwable -> {
                            UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                        });

                // Attempt to establish a connection
                connectionSubscription = bleDevice.establishConnection(auto)
                        .timeout(7, TimeUnit.MINUTES)
                        // .flatMap(RxBleConnection::discoverServices)
                        // .observeOn(AndroidSchedulers.mainThread())
                        // .doOnUnsubscribe(this::clearSubscription)
                        .subscribeOn(Schedulers.io())

                        .subscribe(this::onConnectionReceived, this::onConnectionFailure);

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
            connection.writeCharacteristic(CGM_CHARACTERISTIC_INDICATE, nn(msg.getByteSequence()))
                    .subscribe(
                            characteristicValue -> {
                                UserError.Log.d(TAG, "Wrote " + msg.getClass().getSimpleName() + " request: ");
                            }, throwable -> {
                                UserError.Log.e(TAG, "Failed to write " + msg.getClass().getSimpleName() + " " + throwable);
                            });
        }
    }

    ///

    private void parseInboundPacket(byte[] packet) {
        if (packet == null) {
            UserError.Log.e(TAG, "Was passed null in parseInbound");
            return;
        }
        //
        UserError.Log.d(TAG, "Received inbound packet: " + HexDump.dumpHexString(packet));

        if (packet.length < 2) {
            UserError.Log.e(TAG, "Packet too short");
            return;
        }

        final int opcode = packet[1] & 0xff;


        switch (opcode) {
            case OPCODE_AUTH_REPLY:
                // TODO decode packet, check ok
                status("Authenticated");
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
                                if (BgReading.getForPreciseTimestamp(timestamp, Constants.MINUTE_IN_MS * 2.5) == null) {
                                    BgReading.bgReadingInsertMedtrum(glucose, timestamp, "Backfill", backFillRx.getSensorRawEmulateDex(backsies.get(index)));
                                    UserError.Log.d(TAG, "Adding backfilled reading: " + JoH.dateTimeText(timestamp) + " " + BgGraphBuilder.unitized_string_static(glucose));
                                    Inevitable.task("backfill-ui-update", 3000, Home::staticRefreshBGCharts);
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
                break;

            default:
                UserError.Log.d(TAG, "Unknown inbound opcode: " + opcode);
        }
    }

    private static final int MAX_BACKFILL_ENTRIES = 10;

    private boolean backFillIfNeeded(final AnnexARx annex) {
        return backFillIfNeeded(annex, 0);
    }

    private boolean backFillIfNeeded(final AnnexARx annex, int offset) {
        if (annex == null) return false;
        final Pair<Long, Long> backfillTimes = BackfillAssessor.check();
        if (backfillTimes != null) {
            int startTick = TimeKeeper.tickCounterFromTimeStamp(serial, backfillTimes.first);
            int endTick = TimeKeeper.tickCounterFromTimeStamp(serial, backfillTimes.second);
            if (endTick >= annex.sensorAge) endTick = annex.sensorAge - 1;
            if (startTick < 1) startTick = 1;
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


    private void createRecordFromAnnexData(AnnexARx annex) {

        if (annex == null) return;
        // TODO check annex sanity
        status("Got data");

        if (msSince(lastRecordTime) > MINIMUM_RECORD_INTERVAL) {
            // TODO check existing record time to avoid too many insertions
            UserError.Log.d(TAG, "Creating transmitter data from record annex");
            final TransmitterData transmitterData = TransmitterData.create(annex.getSensorRawEmulateDex(), annex.getSensorRawEmulateDex(), annex.getBatteryPercent(), JoH.tsl());
            if (transmitterData != null) {
                lastRecordTime = transmitterData.timestamp;

                if (transmitterData.raw_data > 0) {
                    // TODO sanity check raw data etc before creating

                    // TODO sensor good flag???
                    if (annex.getState() == Ok) {
                        final double glucose = annex.calculatedGlucose();
                        final boolean use_native = Pref.getBooleanDefaultFalse("medtrum_use_native");
                        if (use_native) {
                            if (glucose > 0) {
                                final BgReading bgReading = bgReadingInsertMedtrum(glucose, JoH.tsl(), null, transmitterData.raw_data);
                            }
                            // xDrip calibration as secondary trace
                            final BgReading bgReadingTemp = BgReading.createFromRawNoSave(null, null, transmitterData.raw_data, transmitterData.raw_data, transmitterData.timestamp);
                            Prediction.create(JoH.tsl(), (int) bgReadingTemp.calculated_value, "Medtrum2nd").save();
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
                    }
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
                // JoH.releaseWakeLock(floatingWakeLock);
                status("Disconnected");
                changeState(CLOSE);
                setRetryTimer();
                break;
        }
        status(connection_state);

        if (connection_state.equals("Disconnecting")) {
            tryGattRefresh(connection);
        }

    }

    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        status("Connected");
        // TODO close off existing connection?
        connection = this_connection;
        changeState(ENABLE);

    }

    private void onConnectionFailure(Throwable throwable) {
        status("Connection failure");
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
                retry_time = 0; // we have woken up
                try {
                    address = ActiveBluetoothDevice.first().address;
                } catch (NullPointerException e) {
                    // bluetooth device not set - launch scan ui???
                }
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

    @Override
    public void btCallback(String address, String status) {
        UserError.Log.d(TAG, "Processing callback: " + address + " :: " + status);
        if (address.equals(MedtrumCollectionService.address)) {
            switch (status) {
                case "DISCONNECTED":
                    changeState(CLOSE);
                    setRetryTimer();
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


    @SuppressLint("ObsoleteSdkInt")
    private static boolean shouldServiceRun() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && DexCollectionType.getDexCollectionType().equals(DexCollectionType.Medtrum);
    }

    private void setRetryTimer() {
        if (shouldServiceRun()) {
            final long retry_in = whenToRetryNext();
            UserError.Log.d(TAG, "setRetryTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            serviceIntent = PendingIntent.getService(this, Constants.MEDTRUM_SERVICE_RETRY_ID,
                    new Intent(this, this.getClass()), 0);
            retry_time = JoH.wakeUpIntent(this, retry_in, serviceIntent);
        } else {
            UserError.Log.d(TAG, "Not setting retry timer as service should not be running");
        }
    }

    private void setFailOverTimer() {
        if (shouldServiceRun()) {
            final long retry_in = Constants.MINUTE_IN_MS * 7;
            UserError.Log.d(TAG, "setFailOverTimer: Restarting in: " + (retry_in / Constants.SECOND_IN_MS) + " seconds");
            serviceIntent = PendingIntent.getService(this, Constants.MEDTRUM_SERVICE_FAILOVER_ID,
                    new Intent(this, this.getClass()), 0);
            retry_time = JoH.wakeUpIntent(this, retry_in, serviceIntent);
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


    // Mega Status
    public static List<StatusItem> megaStatus() {

        if (lastAnnex == null) {
            lastAnnex = com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum.getLastAdvertAnnex();
            // add some advert only marker
        }

        final List<StatusItem> l = new ArrayList<>();

        l.add(new StatusItem("Phone Service State", lastState));
        if (lastAnnex != null) {
            l.add(new StatusItem("Battery", lastAnnex.getBatteryPercent() + "%"));
            if (lastAnnex.charging) {
                l.add(new StatusItem("Charging", lastAnnex.charged ? "Charged" : "On charge"));
            }
            l.add(new StatusItem("Sensor State", lastAnnex.getState().getDescription(), lastAnnex.getState() == Ok ? StatusItem.Highlight.GOOD : StatusItem.Highlight.NORMAL));

            if (lastAnnex.getState() == SensorState.WarmingUp2) {
                final long warmupMinsLeft = ((2 * HOUR_IN_MS) - lastAnnex.getSensorAgeInMs()) / Constants.MINUTE_IN_MS;
                if (warmupMinsLeft > -1 && warmupMinsLeft < 121) {
                    l.add(new StatusItem("Warmup left", warmupMinsLeft + " mins", StatusItem.Highlight.NOTICE));
                }
            }

            if (lastAnnex.getSensorAgeInMs() > 0) {
                l.add(new StatusItem("Sensor Age", lastAnnex.getNiceSensorAge()));
            }

            if (lastAnnex.sensorGood) {
                l.add(new StatusItem("Sensor", "Good", StatusItem.Highlight.GOOD));
            } else {
                if (lastAnnex.getState() == Ok) {
                    l.add(new StatusItem("Sensor", "Unclear", StatusItem.Highlight.NORMAL));
                }
            }

            if (lastAnnex.sensorError) {
                l.add(new StatusItem("Sensor Error", "Error", StatusItem.Highlight.BAD));
            }

            if (lastAnnex.sensorFail) {
                l.add(new StatusItem("Sensor Fail", "FAILED", StatusItem.Highlight.CRITICAL));
            }

            if (lastAnnex.calibrationErrorA) {
                l.add(new StatusItem("Calibration Error", "Error A", StatusItem.Highlight.BAD));
            }
            if (lastAnnex.calibrationErrorB) {
                l.add(new StatusItem("Calibration Error", "Error B", StatusItem.Highlight.BAD));
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
                l.add(new StatusItem("Last Glucose", BgGraphBuilder.unitized_string_with_units_static(lastAnnex.calculatedGlucose()) + (lastAnnex.recent() ? "" : " @ " + JoH.niceTimeScalarShort(JoH.msSince(lastAnnex.created))), lastAnnex.recent() ? StatusItem.Highlight.NORMAL : StatusItem.Highlight.NOTICE));
            }

            if (retry_time != 0) {
                l.add(new StatusItem("Wake up in", JoH.niceTimeScalar(msTill(retry_time))));
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
