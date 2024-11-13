package com.eveningoutpost.dexdrip.insulin.diaconnp8;

import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.BIG_LOG_INQUIRE_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.INCARNATION_INQUIRE_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.INCOMING_CHAR;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.INJECTION_EVENT_REPORT_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.LOG_BLOCK_SIZE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.LOG_STATUS_INQUIRE_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.OUTGOING_CHAR;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.SYSTEM_STATUS_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.TIME_INQUIRE_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.Const.TIME_SETTING_RESPONSE_MSG_TYPE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8.isDiaconnP8Name;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.CLOSE;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.CLOSED;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.CONNECT;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.CONNECT_NOW;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.DISCOVER;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.GET_HISTORY;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.GET_STATUS;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.INIT;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.SCAN;
import static com.eveningoutpost.dexdrip.insulin.diaconnp8.DiaconnP8Service.STATE.SET_TIME;
import static com.eveningoutpost.dexdrip.models.JoH.ratelimit;
import static java.lang.Math.ceil;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.ParcelUuid;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.BigLogInquirePacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.ConfirmSettingPacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.DiaconnP8Packet;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.IncarnationInquirePacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.LogStatusInquirePacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.SystemStatusInquirePacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.TimeInquirePacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.packet.TimeSettingPacket;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog.DiaconnLogUtil;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog.LogInjectionFail;
import com.eveningoutpost.dexdrip.insulin.diaconnp8.penlog.LogInjectionSuccess;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.JamBaseBluetoothService;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.RxBleProvider;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.xdrip;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleDisconnectedException;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.schedulers.Schedulers;
import lombok.RequiredArgsConstructor;

// Driver for the Diaconn P8 Insulin pen
// Supports reading dosage log, setting time
public class DiaconnP8Service extends JamBaseBluetoothService {

    private final RxBleClient rxBleClient = RxBleProvider.getSingleton();

    private static volatile STATE state = INIT;
    private static final int SCAN_SECONDS = 10;
    private static final int MINIMUM_RSSI = -80; // ignore all quieter than this
    private static final String DIACONNP8_TAG = "DiaconnP8 Sync";
    private static final String DIACONNP8_HASH_TAG = "DiaconnP8 Sync";
    private static volatile String address;
    private static volatile String name;
    private static volatile PowerManager.WakeLock connection_linger;

    private static volatile Subscription scanSubscription;
    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    private static volatile Subscription discoverSubscription;

    private volatile RxBleConnection connection;
    private volatile RxBleDevice bleDevice;

    private volatile int log_wrapping_count = 0;
    private volatile int log_last_no = 0;
    private volatile int incarnation_no = 0;

    private final ConcurrentLinkedQueue<QueueItem> write_queue = new ConcurrentLinkedQueue<>();


    private final boolean auto_connect = false;

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
        GET_TIME("Checking Pen time"),
        SET_TIME("Setting time"),
        GET_HISTORY("Getting history"),

        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping"),
        SLEEP("Light Sleep");

        private static final List<STATE> sequence = new ArrayList<>();

        private final String str;

        STATE(String custom) {
            this.str = custom;
        }

        static {
            sequence.add(GET_STATUS);
            sequence.add(GET_HISTORY);
            sequence.add(SET_TIME);
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
            case SET_TIME:
                setTime();
                break;
            case GET_HISTORY:
                getInsulinLog();
                break;
            case CLOSE:
                if (!auto_connect) stopConnect();
                break;
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
        if (DiaconnP8.enabled()) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("diaconnp8-start-service", 600000);
            try {
                UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP WAKE UP @ " + JoH.dateTimeText(JoH.tsl()));
                changeState(INIT);
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

    private void initialize() {
        String searial5digitsPref = Pref.getString("diaconnp8_searial_5digits", "00000");
        String macAddress = Pref.getString(DiaconnP8.DIACONN_MAC_ADDRESS + searial5digitsPref, "");

        if(DiaconnP8.backgroundSyncEnabled()) {
            Inevitable.task("next-p8-connect-register", DiaconnP8.backgroundSyncTimeMillis(), this::background_automata);
        }

        if (!"".equals(macAddress)) {
            address = macAddress;
            UserError.Log.d(TAG, "connect to address : " + macAddress + " searial5digits : " + searial5digitsPref);
            changeState(CONNECT_NOW);
        } else {
            changeState(SCAN);
        }

    }

    private synchronized void scan_for_device() {
        extendWakeLock((SCAN_SECONDS + 1) * Constants.SECOND_IN_MS);
        stopScan();
        scanSubscription = new Subscription(rxBleClient.scanBleDevices(
                new ScanSettings.Builder()

                        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
                , new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid((Const.DIACONNP8_SERVICE)))
                        .build()
        )
                .timeout(SCAN_SECONDS, TimeUnit.SECONDS) // is unreliable
                .subscribeOn(Schedulers.io())
                .subscribe(this::onScanResult, this::onScanFailure));

        Inevitable.task("stop_diaconnp8_scan", SCAN_SECONDS * Constants.SECOND_IN_MS, this::stopScan);
    }

    private synchronized void stopScan() {
        UserError.Log.d(TAG, "stopScan called");
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
            UserError.Log.d(TAG, "stopScan stopped scan");
            scanSubscription = null;
            Inevitable.kill("stop_diaconnp8_scan");
        }
    }

    private synchronized void stopConnect() {

        UserError.Log.d(TAG, "Stopping connection with: " + address);

        if (connectionSubscription != null) {
            connectionSubscription.unsubscribe();
            connectionSubscription = null;
        }
        if (stateSubscription != null) {
            stateSubscription.unsubscribe();
            stateSubscription = null;
        }

        if(state == CLOSE) {
            state = INIT;
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
            final boolean matches = isDiaconnP8Name(this_name);
            UserError.Log.d(TAG, "Found a device with name: " + this_name + " rssi: " + rssi + "  " + (matches ? "-> MATCH" : ""));
            if (matches) {
                stopScan();
                address = bleScanResult.getBleDevice().getMacAddress();
                name = this_name;
                UserError.Log.d(TAG, "Set address to: " + address);
                String searial5digitsPref = Pref.getString("diaconnp8_searial_5digits", "00000");
                Pref.setString(DiaconnP8.DIACONN_MAC_ADDRESS + searial5digitsPref, address);
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
        stopScan();
        releaseWakeLock();
        background_automata(5000);
    }


    private synchronized void connect_to_device(boolean auto) {
        if ((state == CONNECT) || (state == STATE.CONNECT_NOW)) {
            if (address != null) {
                // msg("Connect request");
                if (state == STATE.CONNECT_NOW) {
                    if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
                    connection_linger = JoH.getWakeLock("jam-diaconn-pconnect", 60000);
                }

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
                UserError.Log.wtf(TAG, "No Diaconn P8 mac address!");
                changeState(SCAN);
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
                break;
            case CONNECTED:
                connection_state = "Connected";
                break;
            case DISCONNECTING:
                connection_state = "Disconnecting";
                break;
            case DISCONNECTED:
                connection_state = "Disconnected";
                break;
        }
        UserError.Log.d(TAG, connection_state);
    }


    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
        connection = this_connection;

        if (ratelimit("diaconn-to-discover", 1)) {
            changeState(DISCOVER);
        }
    }

    private void onConnectionFailure(Throwable throwable) {
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
            if (service.getUuid().equals(Const.DIACONNP8_SERVICE)) {
                JoH.threadSleep(1000);
                enableNotification();
                return;
            }
        }
        UserError.Log.e(TAG, "Could not locate DiaconnP8 service during discovery on " + address + " called: " + name);
    }

    private void onDiscoverFailed(Throwable throwable) {
        UserError.Log.e(TAG, "Discover failure: " + throwable.toString());
        stopConnect();
        changeState(CLOSE);
        // incrementErrors();
    }

    @SuppressLint("CheckResult")
    private void enableNotification() {
        UserError.Log.d(TAG, "Enabling notifications");
        // incoming notifications
        connection.setupNotification(INCOMING_CHAR)
                .timeout(30, TimeUnit.SECONDS)
                //.observeOn(Schedulers.newThread())
                .doOnNext(notificationObservable -> {
                    JoH.threadSleep(1000);
                    changeState(GET_STATUS);
                }).flatMap(notificationObservable -> notificationObservable)
                .observeOn(Schedulers.newThread())
                .subscribe(this::processResponseMessage, throwable -> {
                        if (!(throwable instanceof TimeoutException)) {
                            UserError.Log.e(TAG, "Throwable inside setup notification: " + throwable);
                        } else {
                            UserError.Log.d(TAG, "OUTER TIMEOUT INSIDE NOTIFICATION LISTENER : " + throwable.getMessage() );
                        }
                        stopConnect();
                    }
                );
    }

    // receive a stream of bytes and reassemble fragments to cogent packets, classify and action each
    private void processResponseMessage(byte[] bytes) throws ParseException {
        ByteBuffer byteBuffer = DiaconnP8Packet.prefixDecode(bytes);
        int result = DiaconnP8Packet.getByteToInt(byteBuffer);

        // response
        int receivedCommand = DiaconnP8Packet.getCmd(bytes);
        int receivedType = DiaconnP8Packet.getType(bytes);
        int defectResult = DiaconnP8Packet.defect(bytes);
        if (defectResult != 0) {
            // 실패
            UserError.Log.d(TAG, "DIACONN P8 Packet Error");
            return;
        }

        if (receivedType == 3) {
            UserError.Log.d(TAG, "(Report) Live report packet received: " + DiaconnP8Packet.toNarrowHex(bytes));
            if (receivedCommand == INJECTION_EVENT_REPORT_MSG_TYPE) {
                int injEvent = DiaconnP8Packet.getByteToInt(byteBuffer);
                  /*
                     1. inject start
                     2. inject paused
                     3. inject resume
                     4. inject block
                     5. lack battery
                     6. lack insulin
                     7. inject stop
                     8. inject complete
                  */
                if(injEvent == 8) {
                    UserError.Log.d(TAG, "(Report) injection Complete > go to sleep");
                    changeState(CLOSE);
                }
            }
        } else {
            if (state == GET_STATUS) {
                if (receivedCommand == SYSTEM_STATUS_RESPONSE_MSG_TYPE) {
                    if (DiaconnP8Packet.isSuccInquireResponseResult(result)) {
                        double insulin = DiaconnP8Packet.getShortToInt(byteBuffer) / 100.0;
                        int battery = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int insulinkind = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int cartridgeMaker = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int lastinjectDttm = DiaconnP8Packet.getIntToInt(byteBuffer);
                        double lastInject = DiaconnP8Packet.getShortToInt(byteBuffer) / 100.0;
                        int usbConnectStatus = DiaconnP8Packet.getByteToInt(byteBuffer);
                        UserError.Log.d(TAG, "P8 STATUS  \nresult ==: " + result + ", insulin : " + insulin + ", battery : " + battery + ", insulinkind: " + insulinkind + " , cartridgeMaker : " + cartridgeMaker + " , lastinjectDttm: " + lastinjectDttm + " , lastInject:" + lastInject + ", usbConnectStatus : " + usbConnectStatus);
                    }
                }

                if (receivedCommand == LOG_STATUS_INQUIRE_RESPONSE_MSG_TYPE && result == 16) {
                    if (DiaconnP8Packet.isSuccInquireResponseResult(result)) {
                        log_last_no = DiaconnP8Packet.getShortToInt(byteBuffer);
                        log_wrapping_count = DiaconnP8Packet.getByteToInt(byteBuffer);
                       // UserError.Log.d(TAG, "(INQUIRE) LOG_STATUS_RESPONSE  \nresult ==: " + result + ", lastNo : " + log_last_no + ", wrappingcnt : " + log_wrapping_count);
                    }
                }

                if (receivedCommand == INCARNATION_INQUIRE_RESPONSE_MSG_TYPE && result == 16) {
                    if (DiaconnP8Packet.isSuccInquireResponseResult(result)) {
                        incarnation_no = DiaconnP8Packet.getShortToInt(byteBuffer);
                        UserError.Log.d(TAG, "P8 incarnation :" +incarnation_no);
                        String searial5digitsPref = Pref.getString("diaconnp8_searial_5digits", "00000");
                        int pref_incarnation = Pref.getInt(DiaconnP8.DIACONN_LOG_INCARNATION + searial5digitsPref,0);
                        // pref reset if the pen incarnation num and pref value are different
                        if(incarnation_no != pref_incarnation) {
                            UserError.Log.d(TAG, "Log Reset!!! incarnation_no is different :: pref: "+pref_incarnation +", pen:" +incarnation_no);
                            Pref.setInt(DiaconnP8.DIACONN_LOG_LAST_NUM + searial5digitsPref, 0);
                            Pref.setInt(DiaconnP8.DIACONN_LOG_WRAPPING_COUNT + searial5digitsPref, 0);
                        }
                        Pref.setInt(DiaconnP8.DIACONN_LOG_INCARNATION+ searial5digitsPref, incarnation_no);
                    }
                }

                if (receivedCommand == TIME_INQUIRE_RESPONSE_MSG_TYPE && result == 16) {
                    if (DiaconnP8Packet.isSuccInquireResponseResult(result)) {
                        int year = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int month = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int day = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int hour = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int min = DiaconnP8Packet.getByteToInt(byteBuffer);
                        int second = DiaconnP8Packet.getByteToInt(byteBuffer);

                        long penDate = new DateTime(year + 2000, month, day, hour, min, second).getMillis();
                        if (JoH.absMsSince(penDate) > Constants.SECOND_IN_MS * 30) {
                            changeState(SET_TIME);
                        } else {
                            changeState(GET_HISTORY);
                        }
                    }
                }
            } else if (state == SET_TIME) {
                if (receivedCommand == TIME_SETTING_RESPONSE_MSG_TYPE) {
                    // result check
                    if (DiaconnP8Packet.isSuccSettingResponseResult(result)) {
                        int otp = DiaconnP8Packet.getIntToInt(byteBuffer);
                        setConfirmMessage(TimeSettingPacket.MSG_TYPE, otp);
                        JoH.threadSleep(1000);
                        changeState(GET_HISTORY);
                    }
                }
            } else if (state == GET_HISTORY) {
                if (receivedCommand == BIG_LOG_INQUIRE_RESPONSE_MSG_TYPE) {
                    // result check
                    if (DiaconnP8Packet.isSuccInquireResponseResult(result)) {
                        int log_length = DiaconnP8Packet.getByteToInt(byteBuffer);
                        if (log_length > 0) {
                            for (int i = 0; i < log_length; i++) {
                                int wrappingcnt = DiaconnP8Packet.getByteToInt(byteBuffer);
                                int lognum = DiaconnP8Packet.getShortToInt(byteBuffer);
                                byte[] log_data = {
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer),
                                        DiaconnLogUtil.getByte(byteBuffer)
                                };
                                String logHexString = DiaconnP8Packet.toNarrowHex(log_data);
                                byte log_kind = DiaconnLogUtil.getKind(logHexString);
//                                UserError.Log.d(TAG, "wrappingcnt: +" + wrappingcnt + ", lognum: " + lognum + ", logHexString: " + logHexString + ", lOG_KIND: " + log_kind);
                                if (log_kind == LogInjectionSuccess.PENLOG_KIND) {
                                    LogInjectionSuccess logInjectionSuccess = LogInjectionSuccess.parse(logHexString);
                                    Date logDate = DateUtils.parseDate(logInjectionSuccess.getDttm(), "yyyy-MM-dd HH:mm:ss");
                                    long logDateTime = logDate.getTime();
                                    double injectAmt = logInjectionSuccess.getInjAmt() / 100.0;
                                    final String uuid_template = DIACONNP8_HASH_TAG + address + logDateTime;
                                    final String suggested_uuid = UUID.nameUUIDFromBytes(uuid_template.getBytes(StandardCharsets.UTF_8)).toString();
                                    Treatments existing_by_uuid = Treatments.byuuid(suggested_uuid);
                                    if (existing_by_uuid != null) {
                                        UserError.Log.d(TAG, "Existing record matching uuid: " + suggested_uuid);
                                        continue;
                                    }
                                    // Search for nearby reading not synced by this function
                                    final Treatments existing = Treatments.byTimestamp(logDateTime, 120000);

                                    if (existing != null && (Math.abs(existing.insulin - injectAmt) < 0.01)
                                            && !existing.enteredBy.contains(DIACONNP8_TAG)) {
                                        UserError.Log.d(TAG, "Record: already processed");
                                    } else {
                                        UserError.Log.d(TAG, "NEW record: " + logInjectionSuccess);
                                        final Treatments treatment = Treatments.create(0, injectAmt, logDateTime, suggested_uuid);
                                        if (treatment != null) {
                                            treatment.enteredBy += " " + DIACONNP8_TAG;
                                            treatment.notes = logInjectionSuccess.toNote();
                                            treatment.save();
                                        } else {
                                            UserError.Log.wtf(TAG, "Could not create treatment entry, possible dupe: " + suggested_uuid);
                                        }
                                        if(Pref.getBoolean("diaconnp8_play_sounds", false)) {
                                            if (ratelimit("pendiq-data-in-sound", 1)) {
                                                JoH.playResourceAudio(R.raw.bt_meter_data_in); // might want a new sound for pen records
                                            }
                                        }
                                        Home.staticRefreshBGChartsOnIdle();
                                    }
                                } else if (log_kind == LogInjectionFail.PENLOG_KIND) {
                                    LogInjectionFail logInjectionFail = LogInjectionFail.parse(logHexString);
                                    UserError.Log.d(TAG, logInjectionFail.toString());
                                    Date logDate = DateUtils.parseDate(logInjectionFail.getDttm(), "yyyy-MM-dd HH:mm:ss");
                                    long logDateTime = logDate.getTime();
                                    double injectAmt = logInjectionFail.getInjAmt() / 100.0;
                                    final String uuid_template = DIACONNP8_HASH_TAG + address + logDateTime;
                                    final String suggested_uuid = UUID.nameUUIDFromBytes(uuid_template.getBytes(StandardCharsets.UTF_8)).toString();
                                    Treatments existing_by_uuid = Treatments.byuuid(suggested_uuid);
                                    if (existing_by_uuid != null) {
                                        UserError.Log.d(TAG, "Existing record matching uuid: " + suggested_uuid);
                                        continue;
                                    }
                                    // Search for nearby reading not synced by this function
                                    final Treatments existing = Treatments.byTimestamp(logDateTime, 120000);

                                    if (existing != null && (Math.abs(existing.insulin - injectAmt) < 0.01)
                                            && !existing.enteredBy.contains(DIACONNP8_TAG)) {
                                        UserError.Log.d(TAG, "Record: already processed");
                                    } else {
                                        UserError.Log.d(TAG, "NEW record: " + logInjectionFail);
                                        final Treatments treatment = Treatments.create(0, injectAmt, logDateTime, suggested_uuid);

                                        if (treatment != null) {
                                            treatment.enteredBy += " " + DIACONNP8_TAG;
                                            treatment.notes = logInjectionFail.toNote();
                                            treatment.save();
                                        } else {
                                            UserError.Log.wtf(TAG, "Could not create treatment entry, possible dupe: " + suggested_uuid);
                                        }
                                        if(Pref.getBoolean("diaconnp8_play_sounds", false)) {
                                            if (ratelimit("pendiq-data-in-sound", 1)) {
                                                JoH.playResourceAudio(R.raw.bt_meter_data_in); // might want a new sound for pen records
                                            }
                                        }
                                        Home.staticRefreshBGChartsOnIdle();
                                    }
                                }
//                                // save log into to Pref
                                String searial5digitsPref = Pref.getString("diaconnp8_searial_5digits", "00000");
                                Pref.setInt(DiaconnP8.DIACONN_LOG_LAST_NUM + searial5digitsPref, lognum);
                                Pref.setInt(DiaconnP8.DIACONN_LOG_WRAPPING_COUNT+ searial5digitsPref, wrappingcnt);
                                if(log_last_no-1 == lognum) {
                                    UserError.Log.d(TAG, "log_sync end!");
                                    changeState(CLOSE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void getStatus() {
        SystemStatusInquirePacket systemStatusInquirePacket = new SystemStatusInquirePacket();
        byte[] message = systemStatusInquirePacket.encode(getMsgSequence());

        LogStatusInquirePacket logStatusInquirePacket = new LogStatusInquirePacket();
        byte[] message2 = logStatusInquirePacket.encode(getMsgSequence());

        IncarnationInquirePacket incarnationInquirePacket = new IncarnationInquirePacket();
        byte[] message3 = incarnationInquirePacket.encode(getMsgSequence());

        TimeInquirePacket timeInquirePacket = new TimeInquirePacket();
        byte[] message4 = timeInquirePacket.encode(getMsgSequence());

        List<byte[]> inquirePacketList = new ArrayList<>();
        inquirePacketList.add(message); // pen status request
        inquirePacketList.add(message2); // pen log status request
        inquirePacketList.add(message3); // pen incarnation request
        inquirePacketList.add(message4); // pen time request
        addToWriteQueueWithWakeup(inquirePacketList, 500, 10, "Get Status");
    }
    private void getInsulinLog() {
        // Pref saved info
        String searial5digitsPref = Pref.getString("diaconnp8_searial_5digits", "00000");
        int pref_log_last_no= Pref.getInt(DiaconnP8.DIACONN_LOG_LAST_NUM + searial5digitsPref,0);
        int pref_log_wrapping_count = Pref.getInt(DiaconnP8.DIACONN_LOG_WRAPPING_COUNT + searial5digitsPref,0);
        int pref_incarnation = Pref.getInt(DiaconnP8.DIACONN_LOG_INCARNATION + searial5digitsPref,0);
        UserError.Log.d(TAG, "log sync [ searial : "+searial5digitsPref+", pref_log_last_no : "+ pref_log_last_no + ", pref_log_wrapping_count : " + pref_log_wrapping_count + ", pref_log_incarnation : "+ pref_incarnation +", log_last_no : "+log_last_no + ", log_wrapping_count : " + log_wrapping_count + "]");
        // log status
        int start;
        int end;

        // when first app install or reset pref, then sync starts last 10 logs
        if(pref_log_wrapping_count == 0 && pref_log_last_no == 0 && log_last_no > 10) {
            pref_log_last_no = log_last_no - 10;
        }

        if (log_wrapping_count > pref_log_wrapping_count && pref_log_last_no < 2999) {
            start = pref_log_last_no + 1;
            end = 2999;
        } else if (log_wrapping_count > pref_log_wrapping_count && pref_log_last_no >= 2999) {
            start = 0;
            end = log_last_no;
        } else {
            start = pref_log_last_no + 1;
            end = log_last_no;
        }

        int loop_size = (int)ceil((end-start) / 11.0);
        if (loop_size > 0) {
            List<byte[]> inquirePacketList = new ArrayList<>();
            for (int i= 0; i<loop_size; i++){
                int startLogNo = start + i * LOG_BLOCK_SIZE;
                int endLogNo = startLogNo + min(end - startLogNo, LOG_BLOCK_SIZE);
                UserError.Log.d(TAG,"pen log request : "+startLogNo+" ~ "+endLogNo);
                BigLogInquirePacket bigLogInquirePacket = new BigLogInquirePacket(startLogNo, endLogNo, 100);
                byte[] message = bigLogInquirePacket.encode(getMsgSequence());
                inquirePacketList.add(message);
            }
            addToWriteQueueWithWakeup(inquirePacketList, 200, 30, "Get History");
        } else {
            // no need log sync
            changeState(CLOSE);
        }
    }
    private void setTime() {
        JoH.threadSleep(1000);
        TimeSettingPacket timeSettingPacket = new TimeSettingPacket();
        byte[] message = timeSettingPacket.encode(getMsgSequence());
        List<byte[]> packetList = new ArrayList<>();
        packetList.add(message);
        addToWriteQueueWithWakeup(packetList, 200, 10, "Set Time");
    }

    private void setConfirmMessage(byte msgType, int otp) {
        JoH.threadSleep(500);
        ConfirmSettingPacket confirmSettingPacket = new ConfirmSettingPacket(msgType, otp);
        byte[] message = confirmSettingPacket.encode(getMsgSequence());
        List<byte[]> packetList = new ArrayList<>();
        packetList.add(message);
        addToWriteQueueWithWakeup(packetList, 200, 10, "Set Confirm Otp");
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

    private int _mSequence = 0;
    private int getMsgSequence() {
        int seq = _mSequence % 255;
        _mSequence++;
        if (_mSequence == 255) {
            _mSequence = 0;
        }
        return seq;
    }
    private void addToWriteQueueWithWakeup(List<byte[]> byteslist, long delay_ms, int timeout_seconds, String description) {
        addToWriteQueue(byteslist, delay_ms, timeout_seconds, description);
    }

    private void addToWriteQueue(List<byte[]> byteslist, long delay_ms, int timeout_seconds, String description) {
        UserError.Log.d(TAG, "addToWriteQueue start_now ==" + true);
        for (byte[] bytes : byteslist) {
            write_queue.add(new QueueItem(bytes, timeout_seconds, delay_ms, description));
        }
        writeMultipleFromQueue(write_queue);
    }


    private void writeMultipleFromQueue(final ConcurrentLinkedQueue<QueueItem> queue) {
        final QueueItem item = queue.poll();
        if (item != null) {
            writeQueueItem(queue, item);
        } else {
            UserError.Log.d(TAG, "write queue empty");
        }
    }

    @SuppressLint("CheckResult")
    private void writeQueueItem(final ConcurrentLinkedQueue<QueueItem> queue, final QueueItem item) {
        extendWakeLock(2000);
        connection.writeCharacteristic(OUTGOING_CHAR, item.data)
                .timeout(item.timeoutSeconds, TimeUnit.SECONDS)
                .subscribe(Value -> {
                    UserError.Log.d(TAG, "Wrote request: " + item.description + " -> " + JoH.bytesToHex(Value));
                    if (item.post_delay > 0) {
                        final long sleep_time = item.post_delay + (item.description.contains("WAKE UP") ? 2000 : 0);
                        UserError.Log.d(TAG, "sleeping " + sleep_time);
                        JoH.threadSleep(sleep_time);
                    }
                    writeMultipleFromQueue(queue);
                    throw new OperationSuccess("write complete: " + item.description);
                }, throwable -> {
                    if (!(throwable instanceof OperationSuccess)) {
                        UserError.Log.d(TAG, "Throwable in: " + item.description + " -> " + throwable);
                        if(DiaconnP8Packet.getCmd(item.data) != TimeSettingPacket.MSG_TYPE) {
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
                        }else {
                            changeState(CLOSE);
                        }
                    }
                });
    }
}
