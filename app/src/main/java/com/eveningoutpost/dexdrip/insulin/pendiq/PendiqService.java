package com.eveningoutpost.dexdrip.insulin.pendiq;

import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.importedlibraries.usbserial.util.HexDump;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.RxBleProvider;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.InjectionStatusTx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.InsulinLogRx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.InsulinLogTx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.SetInjectTx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.SetTimeTx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.StatusRx;
import com.eveningoutpost.dexdrip.insulin.pendiq.messages.StatusTx;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import lombok.RequiredArgsConstructor;
/*
import rx.Subscription;
import rx.schedulers.Schedulers;
*/
import io.reactivex.schedulers.Schedulers;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.ratelimit;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.INCOMING_CHAR;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.INSULIN_CLASSIFIER;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.OUTGOING_CHAR;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.STATUS_CLASSIFIER;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.PENDIQ_ACTION;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.PENDIQ_COMMAND_DOSE_PREP;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.PENDIQ_INSTRUCTION;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.PENDIQ_PARAMETER;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.PENDIQ_TIMESTAMP;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.checkPin;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.getErrorCode;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.getResultPacketType;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.isPendiqName;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.isProgressPacket;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.isReportPacket;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.isResultPacket;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Pendiq.isResultPacketOk;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.CLOSE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.CLOSED;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.CONNECT;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.CONNECT_NOW;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.DISCOVER;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.DOSE_PREP;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.GET_STATUS;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.INIT;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.SCAN;
import static com.eveningoutpost.dexdrip.insulin.pendiq.PendiqService.STATE.SET_TIME;

// Jamorham

// Driver for the Pendiq 2.0 Insulin pen
// Supports reading dosage log, setting time, priming doses

// TODO shouldservice run - shutdown when disabled etc
// TODO pin support
// TODO multi-pen support
// TODO Check pen cartridge types
// TODO Skip to data exchange if already connected

public class PendiqService extends JamBaseBluetoothService {

    private final RxBleClient rxBleClient = RxBleProvider.getSingleton();

    private static volatile STATE state = INIT;
    private static volatile STATE last_automata_state = CLOSED;

    private static final boolean d = false; // debug flag

    private static final int SCAN_SECONDS = 30;
    private static final int MINIMUM_RSSI = -80; // ignore all quieter than this

    private static volatile String address;
    private static volatile String name;
    private static volatile PowerManager.WakeLock connection_linger;

    private static volatile Subscription scanSubscription;
    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    private static volatile Subscription discoverSubscription;

    private volatile RxBleConnection connection;
    private volatile RxBleDevice bleDevice;

    private volatile long lastProcessedIncomingData = -1;
    private volatile double dose_prep_waiting = -1;
    private volatile int loaded_records;

    private final ConcurrentLinkedQueue<QueueItem> write_queue = new ConcurrentLinkedQueue<>();

    private boolean auto_connect = false;

    {
        TAG = this.getClass().getSimpleName();
    }

    public enum STATE {
        INIT("Initializing"),
        SCAN("Scanning"),
        CONNECT("Waiting connect"),
        CONNECT_NOW("Power connect"),
        DISCOVER("Examining"),
        GET_STATUS("Checking Status"),
        SET_TIME("Setting time"),
        DOSE_PREP("Prepare dose"),
        GET_HISTORY("Getting history"),
        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping"),
        SLEEP("Light Sleep");

        private static List<STATE> sequence = new ArrayList<>();

        private String str;

        STATE(String custom) {
            this.str = custom;
        }

        STATE() {
            this.str = toString();
        }

        static {
            sequence.add(GET_STATUS);
            sequence.add(DOSE_PREP);
            sequence.add(SET_TIME);
            sequence.add(GET_HISTORY);
            sequence.add(SLEEP);
        }

        public STATE next() {
            try {
                return sequence.get(sequence.indexOf(this) + 1);
            } catch (Exception e) {
                return SLEEP;
            }
        }

        public String getString() {
            return str;
        }
    }


    public STATE getState() {
        return state;
    }

    public synchronized void changeState(STATE new_state) {
        if (state == null) return;
        if ((state == new_state) && (state != INIT)) {
            if (state != CLOSE) {
                UserError.Log.d(TAG, "Already in state: " + new_state + " changing to CLOSE");
                changeState(CLOSE);
            }
        } else {
            if ((state == CLOSED || state == CLOSE) && new_state == CLOSE) {
                UserError.Log.d(TAG, "Not closing as already closed");
            } else {
                UserError.Log.d(TAG, "Changing state from: " + state + " to " + new_state);
                state = new_state;
                background_automata();
            }
        }
    }


    @Override
    protected synchronized boolean automata() {

        UserError.Log.d(TAG, "automata state: " + state);
        extendWakeLock(3000);
        try {
            switch (state) {
                case INIT:
                    initialize();
                    break;
                case SCAN:
                    scan_for_device();
                    break;
                case CONNECT:
                    connect_to_device(true);
                    break;
                case CONNECT_NOW:
                    connect_to_device(false);
                    break;
                case DISCOVER:
                    discover_services();
                    break;
                case GET_STATUS:
                    getStatus();
                    break;
                case DOSE_PREP:
                    dosePrep();
                    break;
                case SET_TIME:
                    setTime();
                    break;
                case GET_HISTORY:
                    loaded_records = 0; // reset counter
                    getInsulinLog();
                    break;
                case CLOSE:
                    if (!auto_connect) stopConnect();
                    break;

            }
        } finally {
            //
        }
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        stopScan();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        xdrip.checkAppContext(getApplicationContext());
        if (Pendiq.enabled()) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("pendiq-start-service", 600000);
            try {
                UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP WAKE UP @ " + JoH.dateTimeText(JoH.tsl()));
                if (intent != null) {
                    final String action = intent.getAction();
                    if (action != null && action.equals(PENDIQ_ACTION)) {
                        final String command = intent.getStringExtra(PENDIQ_INSTRUCTION);
                        UserError.Log.d(TAG, "Processing remote command: " + command);
                        if (JoH.msSince(intent.getLongExtra(PENDIQ_TIMESTAMP, 0)) < Constants.SECOND_IN_MS * 10) {

                            switch (command) {
                                case PENDIQ_COMMAND_DOSE_PREP:
                                    try {
                                        dose_prep_waiting = Double.parseDouble(intent.getStringExtra(PENDIQ_PARAMETER));
                                        decideServiceStartStateChange();
                                    } catch (NumberFormatException e) {
                                        UserError.Log.wtf(TAG, "Could not process dosage prep: " + intent.getStringExtra(PENDIQ_PARAMETER));
                                    }
                                    break;
                                default:
                                    UserError.Log.e(TAG, "Unknown remote command: " + command);

                            }
                        } else {
                            UserError.Log.wtf(TAG, "Received service start request out of time: " + action);
                        }
                    } else {
                        decideServiceStartStateChange();
                    }
                }
            } finally {
                // TODO wrong place for release here?
                JoH.releaseWakeLock(wl);
            }
            return START_STICKY;
        } else {
            UserError.Log.d(TAG, "Should not be running so shutting down");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    // which state do we transition to when we receive a service start
    private void decideServiceStartStateChange() {

        switch (state) {

            //case CONNECT_NOW:
            //case DISCOVER:
            //case GET_STATUS:
            //    break;

            default:
                changeState(INIT);
                //background_automata();
        }
    }


    /// functions

    private void initialize() {
        changeState(SCAN);
    }

    private synchronized void scan_for_device() {
        extendWakeLock((SCAN_SECONDS + 1) * Constants.SECOND_IN_MS);
        stopScan();
        scanSubscription = new Subscription(rxBleClient.scanBleDevices(
                new ScanSettings.Builder()

                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()//,  //new ScanFilter.Builder()
                //
                // add custom filters if needed
                //  .build()
        )
                .timeout(SCAN_SECONDS, TimeUnit.SECONDS) // is unreliable
                .subscribeOn(Schedulers.io())
                .subscribe(this::onScanResult, this::onScanFailure));

        Inevitable.task("stop_pendiq_scan", SCAN_SECONDS * Constants.SECOND_IN_MS, this::stopScan);
    }

    private synchronized void stopScan() {
        UserError.Log.d(TAG, "stopScan called");
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
            UserError.Log.d(TAG, "stopScan stopped scan");
            scanSubscription = null;
            Inevitable.kill("stop_pendiq_scan");
        }
        //   if (scanWakeLock != null) {
        //        JoH.releaseWakeLock(scanWakeLock);
        //    }
    }

    private synchronized void stopConnect() {

        UserError.Log.d(TAG, "Stopping connection with: " + address);

        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
        }
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
        }
    }

    private synchronized void stopDiscover() {
        if (discoverSubscription != null) {
            discoverSubscription.unsubscribe();
        }
    }


    // Successful result from our bluetooth scan
    private synchronized void onScanResult(ScanResult bleScanResult) {
        final int rssi = bleScanResult.getRssi();
        if (rssi > MINIMUM_RSSI) {
            final String this_name = bleScanResult.getBleDevice().getName();
            final boolean matches = isPendiqName(this_name);

            // TODO build list of candidates for processing and start inevitable task to poll them
            // TODO use AutoConnect if only one?
            UserError.Log.d(TAG, "Found a device with name: " + this_name + " rssi: " + rssi + "  " + (matches ? "-> MATCH" : ""));
            if (matches) {
                stopScan();
                address = bleScanResult.getBleDevice().getMacAddress();
                name = this_name;
                UserError.Log.d(TAG, "Set address to: " + address);
                if (auto_connect) {
                    changeState(CONNECT);
                } else {
                    changeState(CONNECT_NOW);
                }
            }
        } else {
            if (JoH.quietratelimit("log-low-rssi", 2)) {
                UserError.Log.d(TAG, "Low rssi device: " + bleScanResult.getBleDevice().getMacAddress());
            }
        }
    }


    // Failed result from our bluetooth scan
    private synchronized void onScanFailure(Throwable throwable) {
        UserError.Log.d(TAG, "onScanFailure: " + throwable);
        if (throwable instanceof BleScanException) {
            final String info = handleBleScanException((BleScanException) throwable);
            //   lastScanError = info;
            UserError.Log.d(TAG, info);
            if (((BleScanException) throwable).getReason() == BleScanException.BLUETOOTH_DISABLED) {
                // Attempt to turn bluetooth on
                if (ratelimit("bluetooth_toggle_on", 30)) {
                    UserError.Log.d(TAG, "Pause before Turn Bluetooth on");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        //
                    }
                    UserError.Log.e(TAG, "Trying to Turn Bluetooth on");
                    JoH.setBluetoothEnabled(xdrip.getAppContext(), true);
                }
            }
        }
        // TODO count scan duration
        stopScan();
        releaseWakeLock();
        background_automata(5000);
    }


    private synchronized void connect_to_device(boolean auto) {
        if ((state == CONNECT) || (state == STATE.CONNECT_NOW)) {
            // TODO check mac
            if (address != null) {
                // msg("Connect request");
                if (state == STATE.CONNECT_NOW) {
                    if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
                    connection_linger = JoH.getWakeLock("jam-pendiq-pconnect", 60000);
                }
                //if (d)
                //    UserError.Log.d(TAG, "Local bonding state: " + (isDeviceLocallyBonded() ? "BONDED" : "NOT Bonded"));
                stopConnect();

                bleDevice = rxBleClient.getBleDevice(address);

                /// / Listen for connection state changes
                stateSubscription = new Subscription(bleDevice.observeConnectionStateChanges()
                        // .observeOn(AndroidSchedulers.mainThread())
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onConnectionStateChange, throwable -> {
                            UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                        }));

                // Attempt to establish a connection
                connectionSubscription = new Subscription(bleDevice.establishConnection(auto)
                        .timeout(7, TimeUnit.MINUTES)
                        // .flatMap(RxBleConnection::discoverServices)
                        // .observeOn(AndroidSchedulers.mainThread())
                        // .doOnUnsubscribe(this::clearSubscription)
                        .subscribeOn(Schedulers.io())

                        .subscribe(this::onConnectionReceived, this::onConnectionFailure));

            } else {
                UserError.Log.wtf(TAG, "No transmitter mac address!");

                changeState(SCAN);
                //state = STATE.SCAN;
                //backoff_automata(); // note backoff
            }

        } else {
            UserError.Log.wtf(TAG, "Attempt to connect when not in CONNECT state");
        }
    }

    private synchronized void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        String connection_state = "Unknown";
        switch (newState) {
            case CONNECTING:
                connection_state = "Connecting";
                // connecting_time = JoH.tsl();
                break;
            case CONNECTED:
                connection_state = "Connected";

                break;
            case DISCONNECTING:
                connection_state = "Disconnecting";
                break;
            case DISCONNECTED:
                connection_state = "Disconnected";
                // JoH.releaseWakeLock(floatingWakeLock);
                break;
        }
        UserError.Log.d(TAG, connection_state);
        //static_connection_state = connection_state;
        // UserError.Log.d(TAG, "Bluetooth connection: " + static_connection_state);
        if (connection_state.equals("Disconnecting")) {
            //tryGattRefresh();
        }
    }


    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        //msg("Connected");
        // static_last_connected = JoH.tsl();
        // TODO check connection already exists - close etc?
        if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
        connection = this_connection;

        if (ratelimit("pendiq-to-discover", 1)) {
            changeState(DISCOVER);
        }
    }

    private void onConnectionFailure(Throwable throwable) {
        // msg("Connection failure");
        // TODO under what circumstances should we change state or do something here?
        UserError.Log.d(TAG, "Connection Disconnected/Failed: " + throwable);
        stopConnect();

        changeState(CLOSE);
        JoH.releaseWakeLock(connection_linger);
    }


    private synchronized void discover_services() {
        if (state == DISCOVER) {
            if (connection != null) {
                //   if (d)
                //        UserError.Log.d(TAG, "Local bonding state: " + (isDeviceLocallyBonded() ? "BONDED" : "NOT Bonded"));
                stopDiscover();
                discoverSubscription = new Subscription(connection.discoverServices(10, TimeUnit.SECONDS)
                        .subscribe(this::onServicesDiscovered, this::onDiscoverFailed));
            } else {
                UserError.Log.e(TAG, "No connection when in DISCOVER state - reset");
                changeState(INIT);

            }
        } else {
            UserError.Log.wtf(TAG, "Attempt to discover when not in DISCOVER state");
        }
    }


    private void onServicesDiscovered(RxBleDeviceServices services) {
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            //  UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            if (service.getUuid().equals(Const.PENDIQ_SERVICE)) {

                enableNotification();
                return;
            }
        }
        UserError.Log.e(TAG, "Could not locate Pendiq service during discovery on " + address + " called: " + name);
    }

    private void onDiscoverFailed(Throwable throwable) {
        UserError.Log.e(TAG, "Discover failure: " + throwable.toString());
        stopConnect();
        changeState(CLOSE);
        // incrementErrors();
    }

    private void enableNotification() {
        UserError.Log.d(TAG, "Enabling notifications");
        connection.setupNotification(INCOMING_CHAR)
                // .timeout(10, TimeUnit.SECONDS)
                .timeout(15, TimeUnit.SECONDS) // WARN
                //.observeOn(Schedulers.newThread()) // needed?
                .doOnNext(notificationObservable -> {
                    JoH.threadSleep(1000);
                    changeState(GET_STATUS);
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
                            stopConnect();
                        }
                );

    }

    private boolean gotData() {
        return (msSince(lastProcessedIncomingData) < Constants.SECOND_IN_MS * 15);
    }

    // receive a stream of bytes and reassemble fragments to cogent packets, classify and action each
    private void processAndAction(byte[] bytes) {
        lastProcessedIncomingData = JoH.tsl();
        final List<byte[]> results = PacketStream.addBytes(bytes);
        if (results != null) {
            for (byte[] result : results) {
                if (result != null) {

                    UserError.Log.d(TAG, "Received decoded: " + HexDump.dumpHexString(result) + " " + result.length);
                    final int code = getErrorCode(result);
                    if (code > 0) {
                        UserError.Log.e(TAG, "Got error code: " + code + " on " + HexDump.dumpHexString(result));
                    } else {
                        // packet ok
                        if (isResultPacket(result)) {
                            switch (getResultPacketType(result)) {
                                case STATUS_CLASSIFIER:
                                    handleStatusRecord(result);

                                    break;
                                case INSULIN_CLASSIFIER:
                                    handleInsulinRecord(result);
                                    Inevitable.task("pendiq-logs-done", 2000, () -> changeState(state.next()));
                                    break;
                                default:
                                    UserError.Log.e(TAG, "Unhandled result packet type: " + getResultPacketType(result) + " " + HexDump.dumpHexString(result));
                                    changeState(state.next());
                            }

                        } else if (isProgressPacket(result)) {
                            UserError.Log.d(TAG, "Progress packet classified - success");
                            changeState(state.next());

                        } else if (isReportPacket(result)) {
                            UserError.Log.d(TAG, "Live report packet received");
                            if (ratelimit("pendiq-restart-poll", 60)) {
                                UserError.Log.d(TAG, "Retstarting sequence in 5 seconds");
                                Inevitable.task("pendiq-restart-sequence", 5000, () -> changeState(GET_STATUS));
                            }
                        }

                        // TODO handle live processing packets REPORT_PACKET and restart position to getting status + history
                    }

                }
            }
        }
    }

    private void handleStatusRecord(byte[] result) {
        UserError.Log.d(TAG, "Received status record");
        final StatusRx status = new StatusRx(result);
        UserError.Log.d(TAG, status.getLastDateString() + status.toS());

        if (checkPin(status.pin)) {
            if (ratelimit("status-record-no-wakeup-dupe", 2)) {
                if (dose_prep_waiting > 0) {
                    changeState(DOSE_PREP);
                } else {
                    changeState(SET_TIME); // because wake-up can duplicate
                }
            } else {
                UserError.Log.d(TAG, "Ignoring duplicate status transition due to possible dupe from wake up");
            }
        } else {
            if (ratelimit("pendiq pin mismatch", 600)) {
                UserError.Log.wtf(TAG, "Pin doesn't match on device: " + address + " called: " + name);
                changeState(CLOSE);
            }
        }

    }

    private static final String PENDIQ_TAG = "Pendiq Sync";
    private static final String PENDIQ_HASH_TAG = "Pendiq Sync"; // can never be changed

    private void handleInsulinRecord(final byte[] result) {
        final boolean recordOk = isResultPacketOk(result);
        UserError.Log.d(TAG, "Received " + (recordOk ? "OK" : "NOT OK") + " insulin record");
        if (recordOk) {
            final InsulinLogRx record = new InsulinLogRx(result);
            UserError.Log.d(TAG, record.getTimeStampString() + record.toS());
            if (record.timestamp > JoH.tsl()) {
                UserError.Log.wtf(TAG, "Rejecting injection record in the future! " + record.getSummary());
            } else {
                if (JoH.msSince(record.timestamp) < (Constants.DAY_IN_MS * 2)) {

                    // exact match based on device + time hash
                    final String uuid_template = PENDIQ_HASH_TAG + address + record.timestamp;
                    final String suggested_uuid = UUID.nameUUIDFromBytes(uuid_template.getBytes(Charset.forName("UTF-8"))).toString();
                    Treatments existing_by_uuid = Treatments.byuuid(suggested_uuid);
                    if (existing_by_uuid != null) {
                        UserError.Log.d(TAG, "Existing record matching uuid: " + suggested_uuid);
                        return;
                    }
                    // Search for nearby reading not synced by this function
                    final Treatments existing = Treatments.byTimestamp(record.timestamp, 120000);

                    if (existing != null && (Math.abs(existing.insulin - record.insulin) < 0.01)
                            && !existing.enteredBy.contains(PENDIQ_TAG)) {
                        UserError.Log.d(TAG, "Record: " + record.getSummary() + " already processed");
                    } else {
                        UserError.Log.d(TAG, "NEW record: " + record.getSummary());

                        getInsulinLog(); // ask for next record

                        final Treatments treatment = Treatments.create(0, record.insulin, record.timestamp, suggested_uuid);
                        if (treatment != null) {
                            treatment.enteredBy += " " + PENDIQ_TAG;
                            treatment.save();
                        } else {
                            UserError.Log.wtf(TAG, "Could not create treatment entry, possible dupe: " + suggested_uuid);
                        }
                        if (ratelimit("pendiq-data-in-sound", 1)) {
                            JoH.playResourceAudio(R.raw.bt_meter_data_in); // might want a new sound for pen records
                        }
                        Home.staticRefreshBGChartsOnIdle();
                    }
                } else {
                    UserError.Log.d(TAG, "Rejecting injection record too far in the past >2 days: " + record.getSummary());
                }
            }
        }
    }


    /// Device requests and commands

    private synchronized void dosePrep() {
        if (dose_prep_waiting > 0) {
            setInsulinDose(dose_prep_waiting);
            dose_prep_waiting = 0;
        } else {
            changeState(state.next());
        }
    }


    private void getStatus() {
        addToWriteQueueWithWakeup(new StatusTx().getFragmentStream(), 50, 10, true, "Get Status");
    }


    private void getInjectionStatus() {
        addToWriteQueueWithWakeup(new InjectionStatusTx().getFragmentStream(), 50, 10, true, "Get Injection Status");
    }


    private void getInsulinLog() {
        //noinspection NonAtomicOperationOnVolatileField
        if (loaded_records++ < 100) {
            // we seem to get back records in order newest to oldest with the next one each time we ask regardless of parameter
            // looks a little like device implementation is incomplete. Might change in future firmware so
            // worth keeping an eye on as 'since' is always set to 0 - for a complete implementation this should be
            // tracked with most recent updated with inevitable delay.
            addToWriteQueueWithWakeup(new InsulinLogTx(0).getFragmentStream(), 50, 10, true, "Get Insulin Log");
        } else {
            UserError.Log.wtf(TAG, "Attempted to exceed maximum record loading");
        }
    }

    private void setInsulinDose(double insulin) {
        addToWriteQueueWithWakeup(new SetInjectTx(insulin).getFragmentStream(), 50, 10, true, "Set Insulin Dose");
    }

    private void setTime() {
        JoH.threadSleep(1000);
        addToWriteQueueWithWakeup(new SetTimeTx().getFragmentStream(), 50, 10, true, "Set Time");
    }


    /// Queue Handling

    @RequiredArgsConstructor
    private class QueueItem {
        final byte[] data;
        final int timeoutSeconds;
        final long post_delay;
        public final String description;

        int retries = 0;

    }

    private static final int MAX_QUEUE_RETRIES = 3;

    private void addToWriteQueueWithWakeup(List<byte[]> byteslist, long delay_ms, int timeout_seconds, boolean start_now, String description) {
        if (gotData()) {
            addToWriteQueue(byteslist, delay_ms, timeout_seconds, start_now, description);
        } else {
            addToWriteQueue(byteslist, delay_ms, timeout_seconds, false, description + " :: WAKE UP");
            addToWriteQueue(byteslist, delay_ms, timeout_seconds, start_now, description);
        }
    }


    private void addToWriteQueue(List<byte[]> byteslist, long delay_ms, int timeout_seconds, boolean start_now, String description) {
        for (byte[] bytes : byteslist) {
            write_queue.add(new QueueItem(bytes, timeout_seconds, delay_ms, description));
        }
        if (start_now) writeMultipleFromQueue(write_queue);
    }


    private void writeMultipleFromQueue(final ConcurrentLinkedQueue<QueueItem> queue) {
        final QueueItem item = queue.poll();
        if (item != null) {
            writeQueueItem(queue, item);
        } else {
            UserError.Log.d(TAG, "write queue empty");
        }

    }


    private void writeQueueItem(final ConcurrentLinkedQueue<QueueItem> queue, final QueueItem item) {
        extendWakeLock(2000);
        connection.writeCharacteristic(OUTGOING_CHAR, item.data)
                .timeout(item.timeoutSeconds, TimeUnit.SECONDS)
                .subscribe(Value -> {
                    UserError.Log.d(TAG, "Wrote request: " + item.description + " -> " + JoH.bytesToHex(Value));
                    expectReply(queue, item);
                    if (item.post_delay > 0) {
                        // always sleep if set as new item might appear in queue
                        final long sleep_time = item.post_delay + (item.description.contains("WAKE UP") ? 2000 : 0);
                        UserError.Log.d(TAG, "sleeping " + sleep_time);
                        JoH.threadSleep(sleep_time);
                    }
                    writeMultipleFromQueue(queue); // recurse
                    throw new OperationSuccess("write complete: " + item.description);
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Throwable in: " + item.description + " -> " + throwable);
                        item.retries++;
                        if (!(throwable instanceof BleDisconnectedException)) {
                            if (item.retries > MAX_QUEUE_RETRIES) {
                                UserError.Log.d(TAG, item.description + " failed max retries @ " + item.retries + " shutting down queue");
                                queue.clear();
                                changeState(CLOSE);
                            } else {
                                writeQueueItem(queue, item);
                            }
                        } else {
                            UserError.Log.d(TAG, "Disconnected so not attempting retries");
                        }
                    } else {
                        // not disconnecting on success
                    }
                });
    }

    private void expectReply(final ConcurrentLinkedQueue<QueueItem> queue, final QueueItem item) {
        final long wait_time = 3000;
        Inevitable.task("pendiq-expect-reply-" + item.description, wait_time, new Runnable() {
            @Override
            public void run() {
                if (JoH.msSince(lastProcessedIncomingData) > wait_time) {
                    UserError.Log.d(TAG, "GOT NO REPLY FOR: " + item.description + " @ " + item.retries);
                    item.retries++;
                    if (item.retries <= MAX_QUEUE_RETRIES) {
                        UserError.Log.d(TAG, "Retrying due to no reply: " + item.description);
                        writeQueueItem(queue, item);
                    }
                }
            }
        });
    }
}
