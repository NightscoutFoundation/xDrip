package com.eveningoutpost.dexdrip.services;

import static com.eveningoutpost.dexdrip.Home.get_engineering_mode;
import static com.eveningoutpost.dexdrip.g5model.BatteryInfoRxMessage.battery0VException;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.Advertisement;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.ExtraData;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.Mask16;
import static com.eveningoutpost.dexdrip.g5model.BluetoothServices.getUUIDName;
import static com.eveningoutpost.dexdrip.g5model.CalibrationState.Ok;
import static com.eveningoutpost.dexdrip.g5model.CalibrationState.Unknown;
import static com.eveningoutpost.dexdrip.g5model.FirmwareCapability.isTransmitterModified;
import static com.eveningoutpost.dexdrip.g5model.G6CalibrationParameters.getCurrentSensorCode;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.CLOSED_OK_TEXT;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.doKeepAlive;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.evaluateG6Settings;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.pendingCalibration;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.pendingStart;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.pendingStop;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.shortTxId;
import static com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine.usingAlt;
import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.models.JoH.joinBytes;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.quietratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.tolerantHexStringToByteArray;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.models.JoH.upForAtLeastMins;
import static com.eveningoutpost.dexdrip.plugin.Dialog.txIdMatch;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.BOND;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CLOSE;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CLOSED;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CONNECT;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.CONNECT_NOW;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.DISCOVER;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.GET_DATA;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.INIT;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.PREBOND;
import static com.eveningoutpost.dexdrip.services.Ob1G5CollectionService.STATE.SCAN;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.DAY_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_CALIBRATION_REQUEST;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_FAILED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_RESTARTED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.G5_SENSOR_STARTED;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.SECOND_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.DexcomG5;
import static com.eveningoutpost.dexdrip.utils.bt.Subscription.addErrorHandler;
import static com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry.isNative;
import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.polidea.rxandroidble2.scan.ScanSettings.CALLBACK_TYPE_ALL_MATCHES;
import static com.polidea.rxandroidble2.scan.ScanSettings.CALLBACK_TYPE_FIRST_MATCH;
import static com.polidea.rxandroidble2.scan.ScanSettings.SCAN_MODE_BALANCED;
import static com.polidea.rxandroidble2.scan.ScanSettings.SCAN_MODE_LOW_LATENCY;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;

import com.eveningoutpost.dexdrip.AddCalibration;
import com.eveningoutpost.dexdrip.DoubleCalibrationActivity;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.BatteryInfoRxMessage;
import com.eveningoutpost.dexdrip.g5model.BluetoothServices;
import com.eveningoutpost.dexdrip.g5model.CalibrationState;
import com.eveningoutpost.dexdrip.g5model.DexPairKeeper;
import com.eveningoutpost.dexdrip.g5model.DexSyncKeeper;
import com.eveningoutpost.dexdrip.g5model.DexTimeKeeper;
import com.eveningoutpost.dexdrip.g5model.FirmwareCapability;
import com.eveningoutpost.dexdrip.g5model.Ob1DexTransmitterBattery;
import com.eveningoutpost.dexdrip.g5model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.g5model.TransmitterStatus;
import com.eveningoutpost.dexdrip.g5model.VersionRequest1RxMessage;
import com.eveningoutpost.dexdrip.g5model.VersionRequest2RxMessage;
import com.eveningoutpost.dexdrip.g5model.VersionRequestRxMessage;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.Treatments;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.plugin.IPluginDA;
import com.eveningoutpost.dexdrip.plugin.Loader;
import com.eveningoutpost.dexdrip.plugin.Registry;
import com.eveningoutpost.dexdrip.ui.helpers.Span;
import com.eveningoutpost.dexdrip.utilitymodels.BroadcastGlucose;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.RxBleProvider;
import com.eveningoutpost.dexdrip.utilitymodels.SendFeedBack;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight;
import com.eveningoutpost.dexdrip.utilitymodels.UpdateActivity;
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.bt.Subscription;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.common.collect.Sets;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleCustomOperation;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.exceptions.BleGattCallbackTimeoutException;
import com.polidea.rxandroidble2.exceptions.BleScanException;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.polidea.rxandroidble2.internal.connection.RxBleGattCallback;
import com.polidea.rxandroidble2.scan.ScanFilter;
import com.polidea.rxandroidble2.scan.ScanResult;
import com.polidea.rxandroidble2.scan.ScanSettings;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import lombok.Getter;
import lombok.Setter;
import lombok.val;


/**
 * OB1 G5/G6/G7 collector
 * Created by jamorham on 16/09/2017.
 * <p>
 * App version is master, best to avoid editing wear version directly
 */


public class Ob1G5CollectionService extends G5BaseService {

    public static final String TAG = Ob1G5CollectionService.class.getSimpleName();
    public static final String OB1G5_PREFS = "use_ob1_g5_collector_service";
    private static final String OB1G5_MACSTORE = "G5-mac-for-txid-";
    private static final String OB1G5_STATESTORE = "ob1-state-store-";
    private static final String OB1G5_STATESTORE_TIME = "ob1-state-store-time";
    private static final int DEFAULT_AUTOMATA_DELAY = 100;
    private static final String BUGGY_SAMSUNG_ENABLED = "buggy-samsung-enabled";
    private static final String STOP_SCAN_TASK_ID = "ob1-g5-scan-timeout_scan";
    private static final String KEKS = "keks";
    private static final String KEKS_ONE = "keks1_";
    private static volatile STATE state = INIT;
    private static volatile STATE last_automata_state = CLOSED;

    private static RxBleClient rxBleClient;
    private static volatile PendingIntent pendingIntent;

    private static volatile String transmitterID;
    private static volatile String transmitterMAC;
    private static volatile String historicalTransmitterMAC;
    private static String transmitterIDmatchingMAC;

    private static volatile String lastScanError = null;
    private static volatile int lastScanException = -1;
    public static volatile String lastSensorStatus = null;
    public static volatile CalibrationState lastSensorState = null;
    public static volatile long lastUsableGlucosePacketTime = 0;
    private static volatile String static_connection_state = null;
    public static volatile long static_last_connected = 0;
    @Setter
    @Getter
    private static long last_transmitter_timestamp = 0;
    private static long lastStateUpdated = 0;
    private static long wakeup_time = 0;
    private static long wakeup_jitter = 0;
    private static long max_wakeup_jitter = 0;
    private static volatile long connecting_time = 0;


    public static boolean keep_running = true;

    public static boolean android_wear = false;
    public static boolean wear_broadcast = false;

    private static volatile Subscription scanSubscription;
    private static volatile Subscription connectionSubscription;
    private static volatile Subscription stateSubscription;
    private Subscription discoverSubscription;
    private RxBleDevice bleDevice;
    private RxBleConnection connection;
    public volatile IPluginDA plugin;

    private PowerManager.WakeLock connection_linger;
    private volatile PowerManager.WakeLock scanWakeLock;
    private volatile PowerManager.WakeLock floatingWakeLock;
    private PowerManager.WakeLock fullWakeLock;

    private final DexPairKeeper pairKeeper = new DexPairKeeper();
    private volatile boolean background_launch_waiting = false;
    private static volatile long last_scan_started = -1;
    private static volatile long last_connect_started = -1;
    private static volatile long last_mega_status_read = -1;
    private static volatile int error_count = 0;
    private static volatile int retry_count = 0;
    private static volatile int connectNowFailures = 0;
    private static volatile int connectFailures = 0;
    private static volatile int scanTimeouts = 0;
    private static volatile boolean lastConnectFailed = false;
    private static volatile boolean preScanFailureMarker = false;
    private static boolean auth_succeeded = false;
    private int error_backoff_ms = 1000;
    private static final int max_error_backoff_ms = 10000;
    private static final long TOLERABLE_JITTER = 10000;
    private static volatile String wasBonded = "";
    private static volatile int skippedConnects = 0;
    private static final boolean d = false;

    private static volatile boolean allow_scan_by_mac = false;
    private static volatile boolean use_auto_connect = false;
    private static volatile boolean minimize_scanning = false; // set by preference
    private static volatile boolean always_scan = false;
    private static volatile boolean scan_next_run = true;
    private static final boolean always_discover = true;
    private static boolean always_connect = false;
    private static boolean do_discovery = true;
    private static final boolean do_auth = true;
    //private static boolean initiate_bonding = false;

    private static final Set<String> alwaysScanModels = Sets.newHashSet("SM-N910V", "G Watch");
    private static final List<String> alwaysScanModelFamilies = Arrays.asList("SM-N910");
    private static final Set<String> alwaysConnectModels = Sets.newHashSet("G Watch");
    private static final Set<String> alwaysBuggyWakeupModels = Sets.newHashSet("Jelly-Pro", "SmartWatch 3");
    private static final HashMap<String, Long> failureTally = new HashMap<>();

    // Internal process state tracking
    public enum STATE {
        INIT("Initializing"),
        SCAN("Scanning"),
        CONNECT("Waiting connect"),
        CONNECT_NOW("Power connect"),
        DISCOVER("Examining"),
        CHECK_AUTH("Checking Auth"),
        PREBOND("Bond Prepare"),
        BOND("Bonding"),
        UNBOND("UnBonding"),
        RESET("Reseting"),
        GET_DATA("Getting Data"),
        CLOSE("Sleeping"),
        CLOSED("Deep Sleeping");


        private String str;

        STATE(String custom) {
            this.str = custom;
        }

        STATE() {
            this.str = toString();
        }

        public String getString() {
            return str;
        }
    }

    public void authResult(boolean good) {
        auth_succeeded = good;
    }

    private synchronized void backoff_automata() {
        background_automata(error_backoff_ms);
        if (error_backoff_ms < max_error_backoff_ms) error_backoff_ms += 100;
    }

    public void background_automata() {
        background_automata(DEFAULT_AUTOMATA_DELAY);
    }

    public synchronized void background_automata(final int timeout) {
        if (background_launch_waiting) {
            UserError.Log.d(TAG, "Blocked by existing background automata pending");
            return;
        }
        final PowerManager.WakeLock wl = JoH.getWakeLock("jam-g5-background", timeout + 5000);
        background_launch_waiting = true;
        new Thread(() -> {
            JoH.threadSleep(timeout);
            background_launch_waiting = false;
            automata();
            JoH.releaseWakeLock(wl);
        }).start();
    }

    private static boolean specialPairingWorkaround() {
        return Pref.getBooleanDefaultFalse("ob1_special_pairing_workaround");
    }

    private static boolean getTrustAutoConnect() {
        return Pref.getBoolean("bluetooth_trust_autoconnect", true);
    }


    private synchronized void automata() {

        if ((last_automata_state != state) || state == INIT || (JoH.ratelimit("jam-g5-dupe-auto", 2))) {
            last_automata_state = state;
            final PowerManager.WakeLock wl = JoH.getWakeLock("jam-g5-automata", 60000);
            try {
                switch (state) {

                    case INIT:
                        initialize();
                        break;
                    case SCAN:
                        // no connection? lets try a restart
                        if (msSince(static_last_connected) > 30 * MINUTE_IN_MS) {
                            if (JoH.pratelimit("ob1-collector-restart", 1200)) {
                                CollectionServiceStarter.restartCollectionServiceBackground();
                                break;
                            }
                        }
                        // TODO check if we know mac!!! Sync as part of wear sync?? - TODO preload transmitter mac??

                        if (useMinimizeScanningStrategy()) {
                            UserError.Log.d(TAG, "Skipping Scanning! : Changing state due to minimize_scanning flags");
                            changeState(CONNECT_NOW);
                        } else {
                           /* if ((Build.VERSION.SDK_INT >= 29) && (Pref.getBooleanDefaultFalse("ob1_android10workaround"))) {
                                UserError.Log.d(TAG, "Attempting Android 10+ workaround unbonding");
                                unbondIfAllowed();
                            }*/
                            scan_for_device();
                        }
                        break;
                    case CONNECT_NOW:
                        if (specialPairingWorkaround()) {
                            val locallyBonded = isDeviceLocallyBonded();
                            UserError.Log.d(TAG, "wasbonded = " + wasBonded + " local: " + locallyBonded);
                            if (wasBonded.equals(getTransmitterID()) && !locallyBonded && skippedConnects < 10) {
                                skippedConnects++;
                                UserError.Log.wtf(TAG, "Appears to have lost bonding, skipping this connect! @ " + skippedConnects);
                                changeState(CLOSE);
                            } else {
                                wasBonded = locallyBonded ? getTransmitterID() : "";
                                skippedConnects = 0;
                                connect_to_device(specialPairingWorkaround() && getTrustAutoConnect());
                            }
                        } else {
                            connect_to_device(false);
                        }
                        break;
                    case CONNECT:
                        connect_to_device(true);
                        break;
                    case DISCOVER:
                        if ((wear_broadcast && usingAlt()) || specialPairingWorkaround()) {
                            msg("Pausing");
                            UserError.Log.d(TAG, "Pausing for alt: ");
                            JoH.threadSleep(1000);
                        }
                        if (do_discovery) {
                            discover_services();
                        } else {
                            UserError.Log.d(TAG, "Skipping discovery");
                            changeState(STATE.CHECK_AUTH);
                        }
                        break;
                    case CHECK_AUTH:
                        if (do_auth) {
                            final PowerManager.WakeLock linger_wl_connect = JoH.getWakeLock("jam-g5-check-linger", 6000);
                            if (plugin != null) {
                                if (!Ob1G5StateMachine.doCheckAuth2(this, connection)) {
                                    resetState();
                                }
                            } else {
                                if (!Ob1G5StateMachine.doCheckAuth(this, connection)) {
                                    resetState();
                                }
                            }
                        } else {
                            UserError.Log.d(TAG, "Skipping authentication");
                            changeState(GET_DATA);
                        }
                        break;
                    case PREBOND:
                        if (!getInitiateBondingFlag()) {
                            final PowerManager.WakeLock linger_wl_prebond = JoH.getWakeLock("jam-g5-prebond-linger", 16000);
                            if (!Ob1G5StateMachine.doKeepAliveAndBondRequest(this, connection))
                                resetState();
                        } else {
                            UserError.Log.d(TAG, "Going directly to initiate bond");
                            changeState(BOND);
                        }
                        break;
                    case BOND:
                        if (getInitiateBondingFlag()) {
                            UserError.Log.d(TAG, "State bond attempting to create bond");
                        } else {
                            UserError.Log.d(TAG, "State bond currently does nothing as setting disabled");
                        }
                        create_bond();
                        break;
                    case UNBOND:
                        UserError.Log.d(TAG, "Unbond state");
                        Ob1G5StateMachine.doUnBond(this, connection);
                        Inevitable.task("unbond close", 10000, () -> changeState(Ob1G5CollectionService.STATE.CLOSE));
                        break;

                    case RESET:
                        UserError.Log.d(TAG, "Entering hard reset state");
                        Ob1G5StateMachine.doReset(this, connection);
                        break;
                    case GET_DATA:
                        if (hardResetTransmitterNow && isTransmitterModified(getTransmitterID())) {
                            send_reset_command();
                            DexSyncKeeper.clear(transmitterID);
                        } else {
                            final PowerManager.WakeLock linger_wl_get_data = JoH.getWakeLock("jam-g5-get-linger", 6000);
                            if (!Ob1G5StateMachine.doGetData(this, connection)) resetState();
                        }
                        break;
                    case CLOSE:
                        prepareToWakeup();
                        break;
                    case CLOSED:
                        handleWakeup();
                        break;
                }
            } finally {
                JoH.releaseWakeLock(wl);
            }
        } else {
            UserError.Log.d(TAG, "Ignoring duplicate automata state within 2 seconds: " + state);
        }
    }

    private boolean useMinimizeScanningStrategy() {
        tryLoadingSavedMAC();
        final int modulo = (connectNowFailures + scanTimeouts) % 2;
        UserError.Log.d(TAG, "minimize: " + minimize_scanning + " mac: " + transmitterMAC + " lastfailed:" + lastConnectFailed + " nowfail:" + connectNowFailures + " stimeout:" + scanTimeouts + " modulo:" + modulo);
        final boolean wholeHouse = WholeHouse.isLive();
        boolean alwaysMinimize = false;
        if (wholeHouse) {
            estimateAnticipateFromLinkedData();
            alwaysMinimize = !preScanFailureMarker;
        }
        if (!alwaysMinimize) {
            alwaysMinimize = Pref.getBooleanDefaultFalse("ob1_avoid_scanning");
            if (alwaysMinimize && !upForAtLeastMins(15)) {
                UserError.Log.d(TAG, "Not avoiding scanning as phone has recently rebooted and clock may be inaccurate");
                alwaysMinimize = false;
            }
            if (alwaysMinimize && connectNowFailures > 4 && connectNowFailures % 10 == 1) {
                alwaysMinimize = false;
                UserError.Log.d(TAG, "Not avoiding scanning due to connect failure level: " + connectNowFailures);
                connectNowFailures++;
            }
        }
        if (transmitterMAC == null) {
            UserError.Log.d(TAG, "Do not know transmitter mac inside minimize scanning!!");
        }
        return minimize_scanning && transmitterMAC != null && (!lastConnectFailed || (modulo == 1) || alwaysMinimize)
                && (DexSyncKeeper.isReady(transmitterID));
    }

    private void estimateAnticipateFromLinkedData() {
        final BgReading bg = BgReading.last();
        if (bg != null && bg.timestamp > static_last_connected && msSince(bg.timestamp) < HOUR_IN_MS * 3) {
            final long ts = bg.timestamp - Constants.SECOND_IN_MS;
            UserError.Log.d(TAG, "Updating Sync Keeper with network learned timestamp: " + JoH.dateTimeText(ts));
            DexSyncKeeper.store(transmitterID, ts);
        }
    }

    private void resetState() {
        UserError.Log.e(TAG, "Resetting sequence state to INIT");
        changeState(INIT);
    }

    public STATE getState() {
        return state;
    }

    public void changeState(STATE new_state) {
        if (shouldServiceRun()) {
            changeState(new_state, DEFAULT_AUTOMATA_DELAY);
        } else {
            UserError.Log.d(TAG, "Stopping service due to having being disabled in preferences");
            stopSelf();
        }
    }

    public void changeState(STATE new_state, int timeout) {
        if ((state == CLOSED || state == CLOSE) && new_state == CLOSE) {
            UserError.Log.d(TAG, "Not closing as already closed");
        } else {
            UserError.Log.d(TAG, "Changing state from: " + state + " to " + new_state);
            state = new_state;
            if (android_wear && wear_broadcast) {
                msg(new_state.toString());
            }
            background_automata(timeout);
        }
    }

    private synchronized void initialize() {
        if (state == INIT) {
            msg("Initializing");
            static_connection_state = null;
            if (rxBleClient == null) {
                //rxBleClient = RxBleClient.create(xdrip.getAppContext());
                rxBleClient = RxBleProvider.getSingleton();
            }
            init_tx_id();
            // load prefs etc
            changeState(SCAN);
        } else {
            UserError.Log.wtf(TAG, "Attempt to initialize when not in INIT state");
        }
    }

    private static void init_tx_id() {
        val TXID_PREF = "dex_txid";
        val txid = Pref.getString(TXID_PREF, "NULL");
        val txid_filtered = txid.trim();
        transmitterID = txid_filtered;
        if (!txid.equals(txid_filtered)) {
            Pref.setString(TXID_PREF, txid_filtered);
            UserError.Log.wtf(TAG, "Had to fix invalid txid: :" + txid + ": -> :" + txid_filtered + ":");
        }
    }

    private volatile boolean lastWasScanByMac = false;

    private synchronized void scan_for_device() {
        if (state == SCAN) {
            msg(gs(R.string.scanning));
            stopScan();
            tryLoadingSavedMAC(); // did we already find it?
            expireFailures(false);
            unBondAllG7notCurrentAsNeeded();
            if (always_scan || scan_next_run || (transmitterMAC == null) || (!transmitterID.equals(transmitterIDmatchingMAC)) || (static_last_timestamp < 1)) {
                scan_next_run = false; // reset if set
                transmitterMAC = null; // reset if set
                last_scan_started = tsl();
                scanWakeLock = JoH.getWakeLock("xdrip-jam-g5-scan", (int) MINUTE_IN_MS * 7);


                historicalTransmitterMAC = PersistentStore.getString(OB1G5_MACSTORE + transmitterID); // "" if unset

                boolean macFilter = false;
                ScanFilter filter;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && historicalTransmitterMAC.length() > 5 && allow_scan_by_mac) {
                    filter = new ScanFilter.Builder().setDeviceAddress(historicalTransmitterMAC).build();
                    UserError.Log.d(TAG, "Using mac filter " + historicalTransmitterMAC);
                    macFilter = true;
                } else {
                    final String localTransmitterID = transmitterID;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && localTransmitterID != null && localTransmitterID.length() > 4) {
                        filter = new ScanFilter.Builder().setDeviceName(getTransmitterBluetoothName()).build();
                    } else {
                        filter = new ScanFilter.Builder().setServiceUuid(new ParcelUuid(Advertisement), new ParcelUuid(Mask16)).build();
                    }
                }

                if (lastScanException == BleScanException.LOCATION_PERMISSION_MISSING) {
                    UserError.Log.d(TAG, "Clearing location permission error as we will get it again when we scan now if it is still a problem");
                    lastScanException = -1;
                    lastScanError = null;
                }

                lastWasScanByMac = macFilter;
                scanSubscription = new Subscription(rxBleClient.scanBleDevices(
                                new ScanSettings.Builder()
                                        //.setScanMode(static_last_timestamp < 1 ? ScanSettings.SCAN_MODE_LOW_LATENCY : ScanSettings.SCAN_MODE_BALANCED)
                                        //.setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH)
                                        .setCallbackType(macFilter ? CALLBACK_TYPE_FIRST_MATCH : CALLBACK_TYPE_ALL_MATCHES)
                                        .setScanMode(android_wear ? SCAN_MODE_BALANCED :
                                                historicalTransmitterMAC.length() <= 5 ? SCAN_MODE_LOW_LATENCY :
                                                        minimize_scanning ? SCAN_MODE_BALANCED : SCAN_MODE_LOW_LATENCY)
                                        // .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                                        .build(),

                                // scan filter doesn't work reliable on android sdk 23+
                                filter
                        )
                        // observe on?
                        // do unsubscribe?
                        //.doOnUnsubscribe(this::clearSubscription)
                        .subscribeOn(Schedulers.io())
                        .subscribe(this::onScanResult, this::onScanFailure));
                if (minimize_scanning) {
                    // Must be less than fail over timeout
                    Inevitable.task(STOP_SCAN_TASK_ID, 320 * Constants.SECOND_IN_MS, this::stopScanWithTimeoutAndReschedule);
                }

                UserError.Log.d(TAG, "Scanning for: " + getTransmitterBluetoothName());
            } else {
                UserError.Log.d(TAG, "Transmitter mac already known: " + transmitterMAC);
                changeState(CONNECT);

            }
        } else {
            UserError.Log.wtf(TAG, "Attempt to scan when not in SCAN state");
        }
    }

    private void stopScanWithTimeoutAndReschedule() {
        stopScan();
        UserError.Log.d(TAG, "Stopped scan due to timeout at: " + JoH.dateTimeText(tsl()));
        //noinspection NonAtomicOperationOnVolatileField
        scanTimeouts++;
        tryLoadingSavedMAC();
        prepareToWakeup();
    }


    private synchronized void connect_to_device(boolean auto) {
        if ((state == CONNECT) || (state == CONNECT_NOW)) {
            // TODO check mac
            if (transmitterMAC == null) {
                tryLoadingSavedMAC();
            }
            final String localTransmitterMAC = transmitterMAC;
            if (localTransmitterMAC != null) {

                if (inPurdah()) {
                    msg("Purdah " + niceTimeScalar(purdahMs()));
                    changeState(CLOSE);
                    return;
                }

                unBondAllG7notCurrentAsNeeded();

                msg("Connect request");
                if (state == CONNECT_NOW) {
                    if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
                    connection_linger = JoH.getWakeLock("jam-g5-pconnect", 60000);
                }
                if (d)
                    UserError.Log.d(TAG, "Local bonding state: " + (isDeviceLocallyBonded() ? "BONDED" : "NOT Bonded"));
                stopConnect();

                try {
                    bleDevice = rxBleClient.getBleDevice(localTransmitterMAC);

                    /// / Listen for connection state changes
                    stateSubscription = new Subscription(bleDevice.observeConnectionStateChanges()
                            // .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .doFinally(this::releaseFloating)
                            .subscribe(this::onConnectionStateChange, throwable -> {
                                UserError.Log.wtf(TAG, "Got Error from state subscription: " + throwable);
                            }));

                    last_connect_started = tsl();
                    // Attempt to establish a connection // TODO does this need different connection timeout for auto vs normal?
                    UserError.Log.d(TAG, "Connecting with auto: " + auto);
                    connectionSubscription = new Subscription(bleDevice.establishConnection(auto)
                            .timeout(7, TimeUnit.MINUTES)
                            // .flatMap(RxBleConnection::discoverServices)
                            // .observeOn(AndroidSchedulers.mainThread())
                            // .doOnUnsubscribe(this::clearSubscription)
                            .subscribeOn(Schedulers.io())

                            .subscribe(this::onConnectionReceived, this::onConnectionFailure));
                } catch (IllegalArgumentException e) {
                    UserError.Log.e(TAG, "Caught IllegalArgument Exception: " + e + " retry on next run");
                    // TODO if this is due to concurrent access then changing state again may be a bad idea
                    state = SCAN;
                    backoff_automata(); // note backoff
                }

            } else {
                UserError.Log.wtf(TAG, "No transmitter mac address!");

                state = SCAN;
                backoff_automata(); // note backoff
            }

        } else {
            UserError.Log.wtf(TAG, "Attempt to connect when not in CONNECT state");
        }
    }

    private synchronized void discover_services() {
        if (state == DISCOVER) {
            if (connection != null) {
                if (d)
                    UserError.Log.d(TAG, "Local bonding state: " + (isDeviceLocallyBonded() ? "BONDED" : "NOT Bonded"));
                stopDiscover();
                discoverSubscription = new Subscription(connection.discoverServices(10, TimeUnit.SECONDS).subscribe(this::onServicesDiscovered, this::onDiscoverFailed));
            } else {
                UserError.Log.e(TAG, "No connection when in DISCOVER state - reset");
                state = INIT;
                background_automata();
            }
        } else {
            UserError.Log.wtf(TAG, "Attempt to discover when not in DISCOVER state");
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private synchronized void create_bond() {
        if (state == BOND) {
            try {
                msg("Bonding");
                do_create_bond();
                DexSyncKeeper.clear(getTransmitterID());
                //state = STATE.CONNECT_NOW;
                //background_automata(15000);
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Exception creating bond: " + e);
            }
        } else {
            UserError.Log.wtf(TAG, "Attempt to bond when not in BOND state");
        }
    }

    public synchronized void reset_bond(boolean allow) {
        if (allow || (JoH.pratelimit("ob1-bond-cycle", 7200))) {
            UserError.Log.e(TAG, "Attempting to refresh bond state");
            msg("Resetting Bond");
            unBond();
            do_create_bond();
        }
    }

    private synchronized void do_create_bond() {
        if (!isBluetoothEnabled()) {
            UserError.Log.e(TAG, "Bluetooth not enabled when attempting to create bond");
            return;
        }
        final boolean isDeviceLocallyBonded = isDeviceLocallyBonded();
        UserError.Log.d(TAG, "Attempting to create bond, device is : " + (isDeviceLocallyBonded ? "BONDED" : "NOT Bonded"));

        if (isDeviceLocallyBonded && getInitiateBondingFlag()) {
            UserError.Log.e(TAG, "Device is marked as bonded but we are being asked to bond so attempting to unbond first");
            unbondIfAllowed();
            changeState(CLOSE);
        } else {
            try {
                if (transmitterID.length() > 4) {
                    doKeepAlive(this, connection, this::startInitiateBondReal);
                } else {
                    startInitiateBondReal();
                }
                //background_automata(10000);
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Got exception in do_create_bond() " + e);
            }
        }
    }

    private void startInitiateBondReal() {
        try {
            weInitiatedBondConfirmation = 1;
            instantCreateBondIfAllowed();
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception in startInitiateBondReal() " + e);
        }
    }

    private synchronized void send_reset_command() {
        hardResetTransmitterNow = false;
        getBatteryStatusNow = true;
        if (JoH.ratelimit("reset-command", 1200)) {
            UserError.Log.e(TAG, "Issuing reset command!");
            changeState(STATE.RESET);
        } else {
            UserError.Log.e(TAG, "Reset command blocked by 20 minute timer");
        }
    }

    private String getTransmitterBluetoothName() {
        if (transmitterID == null || transmitterID.length() <= 4) return null;
        final String transmitterIdLastTwo = getLastTwoCharacters(transmitterID);
        // todo check for bad config
        return "Dexcom" + transmitterIdLastTwo;
    }

    public static String getMac() {
        return transmitterMAC;
    }

    private void tryLoadingSavedMAC() {
        if ((transmitterMAC == null) || (!transmitterIDmatchingMAC.equals(transmitterID))) {
            if (transmitterID != null) {
                final String this_mac = PersistentStore.getString(OB1G5_MACSTORE + transmitterID);
                if (this_mac.length() == 17) {
                    UserError.Log.d(TAG, "Loaded stored MAC for: " + transmitterID + " " + this_mac);
                    transmitterMAC = this_mac;
                    transmitterIDmatchingMAC = transmitterID;
                } else {
                    UserError.Log.d(TAG, "Did not find any saved MAC for: " + transmitterID);
                }
            } else {
                UserError.Log.e(TAG, "Could not load saved mac as transmitter id isn't set!");
            }
        } else {
            UserError.Log.d(TAG, "MAC for transmitter id already populated: " + transmitterID + " " + transmitterMAC);
        }
    }

    // should this service be running? Used to decide when to shut down
    private static boolean shouldServiceRun() {
        if (!Pref.getBooleanDefaultFalse(OB1G5_PREFS)) return false;
        if (!(DexCollectionType.getDexCollectionType() == DexcomG5)) return false;

        if (!android_wear) {
            if (Home.get_forced_wear()) {
                if (JoH.quietratelimit("forced-wear-notice", 3))
                    UserError.Log.d(TAG, "Not running due to forced wear");
                return false;
            }

            if (BlueJayEntry.isPhoneCollectorDisabled()) {
                UserError.Log.d(TAG, "Not running as BlueJay is collector");
                return false;
            }

        } else {
            // android wear code
            if (!PersistentStore.getBoolean(CollectionServiceStarter.pref_run_wear_collector))
                return false;
        }
        return true;
    }

    // check required permissions and warn the user if they are wrong
    private static void checkPermissions() {

    }

    public static synchronized boolean isDeviceLocallyBonded() {
        if (transmitterMAC == null) return false;
        final Set<RxBleDevice> pairedDevices = rxBleClient.getBondedDevices();
        if ((pairedDevices != null) && (pairedDevices.size() > 0)) {
            for (RxBleDevice device : pairedDevices) {
                if ((device.getMacAddress() != null) && (device.getMacAddress().equals(transmitterMAC))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean immediateBonding() {
        return Pref.getBooleanDefaultFalse("engineering_ob1_bonding_test") || isNative();
    }

    public static boolean ignoreBonding() {
        return Pref.getBooleanDefaultFalse("engineering_ob1_ignore_bonding");
    }

    private byte[] fwChalCache(boolean prefix) {
        final long chal = Pref.getLong("engineering_ob1_chal_cache" + (prefix ? "_1" : ""), 0);
        if (chal != 0 && shortTxId()) {
            final byte[] bytes = new byte[8];
            for (int i = 0; i < 8; i++) {
                bytes[i] = (byte) (chal >> (8 - i - 1) * 8);
            }
            return bytes;
        } else {
            return new byte[0];
        }
    }

    private synchronized void checkAndEnableBT() {
        try {
            if (Pref.getBoolean("automatically_turn_bluetooth_on", true)) {
                if (!isBluetoothEnabled()) {
                    if (JoH.ratelimit("g5-enabling-bluetooth", 30)) {
                        JoH.setBluetoothEnabled(this, true);
                        UserError.Log.e(TAG, "Enabling bluetooth");
                    }
                }
            }

        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception checking BT: " + e);
        }
    }

    public boolean isBluetoothEnabled() {
        final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public synchronized void unBond() {

        UserError.Log.d(TAG, "unBond() start");
        if (transmitterMAC == null) return;
        if (!isBluetoothEnabled()) {
            UserError.Log.e(TAG, "Bluetooth not enabled when attempting to unbond");
            return;
        }

        final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

        final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (final BluetoothDevice device : pairedDevices) {
                if (device.getAddress() != null) {
                    if (device.getAddress().equals(transmitterMAC)) {
                        try {

                            UserError.Log.e(TAG, "removingBond: " + transmitterMAC);
                            final Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                            m.invoke(device, (Object[]) null);
                            // TODO interpret boolean response
                            break;

                        } catch (Exception e) {
                            UserError.Log.e(TAG, e.getMessage(), e);
                        }
                    }

                }
            }
        }
        UserError.Log.d(TAG, "unBond() finished");
    }

    private void unBondAllG7notCurrentAsNeeded() {
        if (shortTxId() && get_engineering_mode()) {
            if (pratelimit("unbond-g7-as-needed", 86400)) {
                unBondAllG7notCurrent();
            }
        }
    }

    // remove any bonded devices that we previously paired but are not the current transmitter mac
    public synchronized void unBondAllG7notCurrent() {
        try {
            UserError.Log.d(TAG, "unBondAllG7notCurrent() start");
            if (transmitterMAC == null) {
                UserError.Log.d(TAG, "unBondAllG7notCurrent() no transmitter mac");
                return;
            }
            if (!isBluetoothEnabled()) {
                UserError.Log.e(TAG, "unBondAllG7notCurrent() bluetooth not enabled");
                return;
            }
            final BluetoothAdapter mBluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();

            final Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (final BluetoothDevice device : pairedDevices) {
                    if (device.getAddress() != null) {
                        if (pairKeeper.macEnded(device.getAddress()) && !device.getAddress().equals(transmitterMAC)) {

                            try {
                                UserError.Log.e(TAG, "unBondAllG7notCurrent() removing old Bond: " + device.getAddress());
                                final Method m = device.getClass().getMethod("removeBond", (Class[]) null);
                                m.invoke(device, (Object[]) null);

                            } catch (Exception e) {
                                UserError.Log.e(TAG, "unBondAllG7notCurrent() " + e.getMessage(), e);
                            }
                        }
                    }
                }
            } else {
                UserError.Log.d(TAG, "unBondAllG7notCurrent() Could not read any paired devices");
            }
            UserError.Log.d(TAG, "unBondAllG7notCurrent() finished");
        } catch (Exception e) {
            UserError.Log.e(TAG, "unBondAllG7notCurrent() exception " + e);
        }
    }


    public static String getTransmitterID() {
        if (transmitterID == null) {
            init_tx_id();
        }
        return transmitterID;
    }

    private void handleWakeup() {
        if (always_scan) {
            UserError.Log.d(TAG, "Always scan mode");
            changeState(SCAN);
        } else {
            if (connectFailures > 0 || (!use_auto_connect && connectNowFailures > 0)) {
                always_scan = true;
                UserError.Log.e(TAG, "Switching to scan always mode due to connect failures metric: " + connectFailures);
                changeState(SCAN);
            } else if (use_auto_connect && (connectNowFailures > 1) && (connectFailures < 0)) {
                UserError.Log.d(TAG, "Avoiding power connect due to failure metric: " + connectNowFailures + " " + connectFailures);
                changeState(CONNECT);
            } else {
                changeState(CONNECT_NOW);
            }
        }
    }


    private synchronized void prepareToWakeup() {
        if (JoH.ratelimit("g5-wakeup-timer", 5)) {
            final long when = DexSyncKeeper.anticipate(transmitterID);
            if (when > 0) {
                final long when_offset = when - tsl();
                UserError.Log.d(TAG, "(" + JoH.dateTimeText(tsl()) + ")  Wake up time anticipated at: " + JoH.dateTimeText(when));
                scheduleWakeUp(when_offset - Constants.SECOND_IN_MS * 15, "anticipate");
            } else {
                scheduleWakeUp(Constants.SECOND_IN_MS * 285, "anticipate");
            }
        }

        if ((android_wear && wakeup_jitter > TOLERABLE_JITTER) || always_connect) {
            // TODO should be max_wakeup_jitter perhaps or set always_connect flag
            UserError.Log.d(TAG, "Not stopping connect due to " + (always_connect ? "always_connect flag" : "unreliable wake up"));
            state = CONNECT;
            background_automata(6000);
        } else {
            state = CLOSED; // Don't poll automata as we want to do this on waking
            stopConnect();
        }

        if (plugin != null) {
            UserError.Log.d(TAG, "Saving persistent data for keks");
            savePersist();
        }

    }

    public synchronized void savePersist() {
        if (plugin != null) {
            PersistentStore.cleanupOld(KEKS_ONE);
            PersistentStore.setBytes(KEKS_ONE + transmitterMAC, plugin.getPersistence(1));
        }
    }

    public static void clearPersistStore() {
        PersistentStore.cleanupOld(KEKS_ONE);
        PersistentStore.cleanupOld(OB1G5_MACSTORE);
    }

    public static void clearPersist() {
        clearPersistStore();
        expireFailures(true);
        transmitterID = null;
        transmitterMAC = null;
    }

    private void scheduleWakeUp(long future, final String info) {
        if (future <= 0) future = 5000;
        UserError.Log.d(TAG, "Scheduling wakeup @ " + JoH.dateTimeText(tsl() + future) + " (" + info + ")");
        if (pendingIntent == null)
            //pendingIntent = PendingIntent.getService(this, 0, new Intent(this, this.getClass()), 0);
            pendingIntent = WakeLockTrampoline.getPendingIntent(this.getClass());
        wakeup_time = tsl() + future;
        JoH.wakeUpIntent(this, future, pendingIntent);
    }

    public void incrementErrors() {
        error_count++;
        if (error_count > 1) {
            UserError.Log.e(TAG, "Error count reached: " + error_count);
        }
    }

    public int incrementRetry() {
        retry_count++;
        return retry_count;
    }

    public void clearErrors() {
        error_count = 0;
    }

    public void clearRetries() {
        retry_count = 0;
    }

    private void checkAlwaysScanModels() {
        final String this_model = Build.MODEL;
        UserError.Log.d(TAG, "Checking model: " + this_model);

        if ((JoH.isSamsung() && PersistentStore.getLong(BUGGY_SAMSUNG_ENABLED) > 4)) {
            UserError.Log.d(TAG, "Enabling wake workaround due to persistent metric");
            JoH.buggy_samsung = true;
        }

        always_connect = alwaysConnectModels.contains(this_model);

        if (alwaysBuggyWakeupModels.contains(this_model)) {
            UserError.Log.e(TAG, "Always buggy wakeup exact match for " + this_model);
            JoH.buggy_samsung = true;
        }

        if (alwaysScanModels.contains(this_model)) {
            UserError.Log.e(TAG, "Always scan model exact match for: " + this_model);
            always_scan = true;
            return;
        }

        for (String check : alwaysScanModelFamilies) {
            if (this_model.startsWith(check)) {
                UserError.Log.e(TAG, "Always scan model fuzzy match for: " + this_model);
                always_scan = true;
                return;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            UserError.Log.wtf(TAG, "Not high enough Android version to run: " + Build.VERSION.SDK_INT);
        } else {

            try {
                registerReceiver(mBondStateReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not register bond state receiver: " + e);
            }

            final IntentFilter pairingRequestFilter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingRequestFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
            try {
                if (Build.VERSION.SDK_INT < 26) {
                    registerReceiver(mPairingRequestRecevier, pairingRequestFilter);
                } else {
                    UserError.Log.d(TAG, "Not registering pairing receiver on Android 8+");
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Could not register pairing request receiver:" + e);
            }

            checkAlwaysScanModels();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                android_wear = JoH.areWeRunningOnAndroidWear();
                if (android_wear) {
                    UserError.Log.d(TAG, "We are running on Android Wear");
                    wear_broadcast = Pref.getBooleanDefaultFalse("ob1_wear_broadcast");
                }
            }
        }
        if (d) RxBleClient.setLogLevel(RxBleLog.DEBUG);
        addErrorHandler(TAG);
        listenForChangeInSettings(true);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        xdrip.checkAppContext(getApplicationContext());
        final PowerManager.WakeLock wl = JoH.getWakeLock("g5-start-service", 310000);
        try {
            UserError.Log.d(TAG, "WAKE UP WAKE UP WAKE UP WAKE UP @ " + JoH.dateTimeText(tsl()));
            msg("Wake up");
            if (wakeup_time > 0) {
                wakeup_jitter = msSince(wakeup_time);
                if (wakeup_jitter < 0) {
                    UserError.Log.d(TAG, "Woke up Early..");
                } else {
                    if (wakeup_jitter > 1000) {
                        UserError.Log.d(TAG, "Wake up, time jitter: " + niceTimeScalar(wakeup_jitter));
                        if ((wakeup_jitter > TOLERABLE_JITTER) && (!JoH.buggy_samsung) && JoH.isSamsung()) {
                            UserError.Log.wtf(TAG, "Enabled wake workaround due to jitter of: " + niceTimeScalar(wakeup_jitter));
                            JoH.buggy_samsung = true;
                            PersistentStore.incrementLong(BUGGY_SAMSUNG_ENABLED);
                            max_wakeup_jitter = 0;
                        } else {
                            max_wakeup_jitter = Math.max(max_wakeup_jitter, wakeup_jitter);
                        }

                    }
                }
            }
            if (!shouldServiceRun()) {
                UserError.Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                msg("Stopping");
                stopSelf();
                return START_NOT_STICKY;
            }


            scheduleWakeUp(MINUTE_IN_MS * 6, "fail-over");
            if ((state == BOND) || (state == PREBOND) || (state == DISCOVER) || (state == CONNECT))
                state = SCAN;

            checkAndEnableBT();

            Ob1G5StateMachine.restoreQueue();

            if (JoH.quietratelimit("evaluateG6Settings", 600)) {
                evaluateG6Settings();
            }

            minimize_scanning = Pref.getBooleanDefaultFalse("ob1_minimize_scanning");
            // allow_scan_by_mac = Build.VERSION.SDK_INT >= 32 && shortTxId();
            automata(); // sequence logic

            UserError.Log.d(TAG, "Releasing service start");
            return START_STICKY;
        } finally {
            JoH.releaseWakeLock(wl);
        }
    }

    @Override
    public void onDestroy() {
        msg("Shutting down");
        if (pendingIntent != null) {
            JoH.cancelAlarm(this, pendingIntent);
            pendingIntent = null;
            wakeup_time = 0;
        }
        stopScan();
        stopDiscover();
        stopConnect();
        scanSubscription = null;
        connectionSubscription = null;
        stateSubscription = null;
        discoverSubscription = null;

        listenForChangeInSettings(false);
        unregisterPairingReceiver();

        try {
            unregisterReceiver(mBondStateReceiver);
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got exception unregistering pairing receiver: " + e);
        }

        state = INIT; // Should be STATE.END ?
        last_automata_state = CLOSED;
        msg("Service Stopped");
        super.onDestroy();
    }

    public void unregisterPairingReceiver() {
        try {
            unregisterReceiver(mPairingRequestRecevier);
        } catch (Exception e) {
            UserError.Log.d(TAG, "Got exception unregistering pairing receiver: " + e);
        }
    }

    private synchronized void stopScan() {
        if (scanSubscription != null) {
            scanSubscription.unsubscribe();
        }
        UserError.Log.d(TAG, "DEBUG: killing stop scan task");
        Inevitable.kill(STOP_SCAN_TASK_ID);
        if (scanWakeLock != null) {
            JoH.releaseWakeLock(scanWakeLock);
        }
        last_scan_started = 0;
    }

    private synchronized void stopConnect() {
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

    private boolean isScanMatch(final String this_address, final String historical_address, final String this_name, final String search_name) {
        if (search_name == null && (this_address.equalsIgnoreCase(historical_address) || this_name == null ||
                (emptyString(historical_address) && this_name.startsWith("DXCM")) ||
                (emptyString(historical_address) && this_name.startsWith("DX02")) ||
                (emptyString(historical_address) && this_name.startsWith("DX01")))) {
            return !inFailureTally(this_address) && pairKeeper.check(getTransmitterID(), this_address);
        }

        boolean result = this_address.equalsIgnoreCase(historical_address) || (this_name != null && this_name.equalsIgnoreCase(search_name));
        if (result) {
            if (historical_address.length() == this_address.length()
                    && !this_address.equalsIgnoreCase(historical_address)) {
                if (JoH.ratelimit("ob1-address-change-error", 30)) {
                    UserError.Log.wtf(TAG, "Bluetooth device: " + search_name + " apparently changed from mac: " + historical_address + " to: " + this_address + " :: There appears to be confusion between devices - ignoring this scan result");
                }
                result = false;
            }
        }
        return result;
    }

    public static void clearScanError() {
        lastScanError = null;
    }

    // Successful result from our bluetooth scan
    private synchronized void onScanResult(final ScanResult bleScanResult) {
        // TODO MIN RSSI
        final int this_rssi = bleScanResult.getRssi();
        final String this_name = bleScanResult.getBleDevice().getName();
        final String this_address = bleScanResult.getBleDevice().getMacAddress();
        final String search_name = getTransmitterBluetoothName();
        val mdata = bleScanResult.getScanRecord().getManufacturerSpecificData(0xD << 4);
        if (isScanMatch(this_address, historicalTransmitterMAC, this_name, search_name)) {
            stopScan(); // we got one!
            last_scan_started = 0; // clear scanning for time
            lastScanError = null; // error should be cleared
            UserError.Log.d(TAG, "Got scan result match: " + bleScanResult.getBleDevice().getName() + " " + this_address + " rssi: " + this_rssi);
            transmitterMAC = bleScanResult.getBleDevice().getMacAddress();
            transmitterIDmatchingMAC = transmitterID;
            if (search_name != null) {
                saveTransmitterMac();
            }
            //if (JoH.ratelimit("ob1-g5-scan-to-connect-transition", 3)) {
            if (state == SCAN) {
                //  if (always_scan) {
                changeState(CONNECT_NOW, 500);
                //   } else {
                //       changeState(STATE.CONNECT);
                //  }
            } else {
                UserError.Log.e(TAG, "Skipping apparent duplicate connect transition, current state = " + state);
            }
        } else {
            String this_mac = bleScanResult.getBleDevice().getMacAddress();
            if (this_mac == null) this_mac = "NULL";
            if (JoH.quietratelimit("bt-obi1-null-match" + this_mac, 15)) {
                UserError.Log.d(TAG, "Bluetooth scanned device doesn't match (" + search_name + ") found: " + this_name + " " + bleScanResult.getBleDevice().getMacAddress());
            }
        }
    }

    public void saveTransmitterMac() {
        UserError.Log.d(TAG, "Saving transmitter mac: " + transmitterID + " = " + transmitterMAC);
        PersistentStore.cleanupOld(OB1G5_MACSTORE);
        PersistentStore.setString(OB1G5_MACSTORE + transmitterID, transmitterMAC);
    }

    // Failed result from our bluetooth scan
    private synchronized void onScanFailure(Throwable throwable) {

        if (throwable instanceof BleScanException) {
            lastScanException = ((BleScanException) throwable).getReason();
            final String info = handleBleScanException((BleScanException) throwable);
            lastScanError = info;
            UserError.Log.d(TAG, info);

            final int reason = ((BleScanException) throwable).getReason();

            if (reason == BleScanException.SCAN_FAILED_INTERNAL_ERROR) {
                if (allow_scan_by_mac && lastWasScanByMac) {
                    allow_scan_by_mac = false;
                    UserError.Log.wtf(TAG, "Turning scan by by mac off");
                    if (JoH.ratelimit("bluetooth-internal-error-register", 120)) {
                        if (Pref.getBooleanDefaultFalse("automatically_turn_bluetooth_on")) {
                            UserError.Log.wtf(TAG, "Android bluetooth appears broken with scan by mac - attempting to turn off and on");
                            JoH.niceRestartBluetooth(xdrip.getAppContext());
                        } else {
                            UserError.Log.e(TAG, "Cannot reset bluetooth due to preference being disabled");
                        }
                    }
                }
            }

            if (reason == BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {
                if (JoH.ratelimit("bluetooth-cannot-register", 120)) {
                    if (Pref.getBooleanDefaultFalse("automatically_turn_bluetooth_on")) {
                        UserError.Log.wtf(TAG, "Android bluetooth appears broken - attempting to turn off and on");
                        JoH.niceRestartBluetooth(xdrip.getAppContext());
                    } else {
                        UserError.Log.e(TAG, "Cannot reset bluetooth due to preference being disabled");
                    }
                }
            }

            if (reason == BleScanException.BLUETOOTH_DISABLED) {
                // Attempt to turn bluetooth on
                if (JoH.ratelimit("bluetooth_toggle_on", 30)) {
                    UserError.Log.d(TAG, "Pause before Turn Bluetooth on");
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        //
                    }

                    if (WholeHouse.isRpi()) {
                        UserError.Log.e(TAG, "Trying to turn off/on bluetooth");
                        JoH.niceRestartBluetooth(xdrip.getAppContext());
                    } else {
                        UserError.Log.e(TAG, "Trying to Turn Bluetooth on");
                        JoH.setBluetoothEnabled(xdrip.getAppContext(), true);
                    }
                }
            }
            // TODO count scan duration
            stopScan();

            // Note that this is not reached on timeout only failure
            if (minimize_scanning) {
                UserError.Log.d(TAG, "Preparing to wake up at next expected time");
                prepareToWakeup();
            } else {
                backoff_automata();
            }
        }
    }

    private void unbondIfAllowed() {
        if (Pref.getBoolean("ob1_g5_allow_resetbond", true)) {
            unBond();
        } else {
            UserError.Log.e(TAG, "Would have tried to unpair but preference setting prevents it. (unbond)");
        }
    }

    private boolean resetBondIfAllowed(boolean force) {
        if (!isBluetoothEnabled()) {
            UserError.Log.e(TAG, "Bluetooth is not enabled, cannot reset bond");
            return false;
        }
        if (Pref.getBoolean("ob1_g5_allow_resetbond", false)
                && !specialPairingWorkaround()) {
            reset_bond(force);
            return true;
        } else {
            UserError.Log.w(TAG, "Would have tried to unpair but preference setting prevents it. (resetbond) Allow OB1 unbonding setting and disable pairing workaround");
            return false;
        }
    }

    // Connection has been terminated or failed
    // - quite normal when device switches to sleep between readings
    private void onConnectionFailure(final Throwable throwable) {
        // msg("Connection failure");
        // TODO under what circumstances should we change state or do something here?
        UserError.Log.d(TAG, "Connection Disconnected/Failed: " + throwable);

        if (state == DISCOVER) {
            // possible encryption failure
            if (!resetBondIfAllowed(false) && android_wear) {
                UserError.Log.d(TAG, "Trying alternate reconnection strategy");
                changeState(CONNECT_NOW);
            }
            return;
        }

        if (state == CONNECT_NOW) {
            connectNowFailures++;
            lastConnectFailed = true;

            if ((connectNowFailures % 12 == 7) && genericBluetoothWatchdog()) {
                UserError.Log.e(TAG, "Initiating bluetooth watchdog reset");
                JoH.niceRestartBluetooth(xdrip.getAppContext());
            }

            if (throwable instanceof BleGattCallbackTimeoutException) {
                if (throwable.getMessage().contains("BleGattOperation{description='CONNECTION_STATE'")) {
                    UserError.Log.d(TAG, "Setting pre-scan failure marker enabled due to exception type");
                    preScanFailureMarker = true;
                }
            } else if (msSince(last_connect_started) < Constants.SECOND_IN_MS) {
                UserError.Log.d(TAG, "Setting pre-scan failure marker enabled due to exception timing");
                preScanFailureMarker = true;
            }

            UserError.Log.d(TAG, "Connect Now failures incremented to: " + connectNowFailures);
            if (minimize_scanning && DexSyncKeeper.anticipate(transmitterID) > 0) {
                // TODO under what circumstances does this CLOSE state not get executed? and anticipate not scheduled?
                changeState(CLOSE);
            } else {
                changeState(CONNECT);
            }
            return;
        }

        if (state == CONNECT) {
            connectFailures++;
            lastConnectFailed = true;
            // TODO check bluetooth on or in connect section
            if (JoH.ratelimit("ob1-restart-scan-on-connect-failure", 10)) {
                UserError.Log.d(TAG, "Restarting scan due to connect failure");
                tryGattRefresh();
                changeState(SCAN);
            }
        }

    }

    public void tryGattRefresh() {
        if (JoH.ratelimit("ob1-gatt-refresh", 60)) {
            if (Pref.getBoolean("use_gatt_refresh", true)) {
                try {
                    if (connection != null)
                        UserError.Log.d(TAG, "Trying gatt refresh queue");
                    connection.queue((new GattRefreshOperation(0))).timeout(2, TimeUnit.SECONDS).subscribe(
                            readValue -> {
                                UserError.Log.d(TAG, "Refresh OK: " + readValue);
                            }, throwable -> {
                                UserError.Log.d(TAG, "Refresh exception: " + throwable);
                            });
                } catch (NullPointerException e) {
                    UserError.Log.d(TAG, "Probably harmless gatt refresh exception: " + e);
                } catch (Exception e) {
                    UserError.Log.d(TAG, "Got exception trying gatt refresh: " + e);
                }
            } else {
                UserError.Log.d(TAG, "Gatt refresh block by preference");
            }
        } else {
            UserError.Log.d(TAG, "Gatt refresh rate limited");
        }
    }

    // We have connected to the device!
    private void onConnectionReceived(RxBleConnection this_connection) {
        msg("Connected");

        if (shouldServiceRun()) {
            static_last_connected = tsl();
            lastConnectFailed = false;
            preScanFailureMarker = false;
            if (!shortTxId() || !DexSyncKeeper.isReady(transmitterID)) {
                DexSyncKeeper.store(transmitterID, static_last_connected);
            } else {
                UserError.Log.d(TAG, "onConnectionReceived slc:" + ((static_last_connected / 1000) % 300) + " dsk:" + ((DexSyncKeeper.get(transmitterID) / 1000) % 300) + "   " + JoH.dateTimeText(DexSyncKeeper.anticipate(transmitterID)));
            }
            // TODO check connection already exists - close etc?
            if (connection_linger != null) JoH.releaseWakeLock(connection_linger);
            connection = this_connection;

            if (state == CONNECT_NOW) {
                connectNowFailures = -3; // mark good
            }
            if (state == CONNECT) {
                connectFailures = -1; // mark good
            }

            scanTimeouts = 0; // reset counter
            clearRetries();

            if (JoH.ratelimit("g5-to-discover", 1)) {
                changeState(DISCOVER);
            }
        } else {
            msg("Shutdown");
            stopSelf();
        }
    }

    private synchronized void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {
        String connection_state = "Unknown";
        switch (newState) {
            case CONNECTING:
                connection_state = "Connecting";
                connecting_time = tsl();
                break;
            case CONNECTED:
                connection_state = "Connected";
                JoH.releaseWakeLock(floatingWakeLock);
                floatingWakeLock = JoH.getWakeLock("floating-connected", 40000);
                final long since_connecting = msSince(connecting_time);
                if ((connecting_time > static_last_timestamp) && (since_connecting > Constants.SECOND_IN_MS * 310) && (since_connecting < Constants.SECOND_IN_MS * 620)) {
                    if (!always_scan) {
                        UserError.Log.e(TAG, "Connection time shows missed reading, switching to always scan, metric: " + niceTimeScalar(since_connecting));
                        always_scan = true;
                    } else {
                        UserError.Log.e(TAG, "Connection time shows missed reading, despite always scan, metric: " + niceTimeScalar(since_connecting));
                    }
                }
                break;
            case DISCONNECTING:
                connection_state = "Disconnecting";
                break;
            case DISCONNECTED:
                connection_state = "Disconnected";
                JoH.releaseWakeLock(floatingWakeLock);
                break;
        }
        static_connection_state = connection_state;
        UserError.Log.d(TAG, "Bluetooth connection: " + static_connection_state);
        if (connection_state.equals("Disconnecting")) {
            //tryGattRefresh();
        }
    }

    private void releaseFloating() {
        val wl = floatingWakeLock;
        if (wl != null) {
            if (wl.isHeld()) {
                JoH.releaseWakeLock(wl);
            }
        }
    }

    public void connectionStateChange(String connection_state) {
        static_connection_state = connection_state;
        if (connection_state.equals(CLOSED_OK_TEXT)) {
            JoH.releaseWakeLock(floatingWakeLock);
        }
    }


    private void onServicesDiscovered(RxBleDeviceServices services) {
        for (BluetoothGattService service : services.getBluetoothGattServices()) {
            if (d) UserError.Log.d(TAG, "Service: " + getUUIDName(service.getUuid()));
            if (service.getUuid().equals(BluetoothServices.CGMService)) {
                if (d) UserError.Log.i(TAG, "Found CGM Service!");
                if (!always_discover) {
                    do_discovery = false;
                }

                if (txIdMatch(getTransmitterID()) && service.getCharacteristic(ExtraData) != null) {
                    try {
                        plugin = Loader.getLocalInstance(Registry.get(KEKS), getTransmitterID());
                        if (plugin == null) {
                            val msg = "Unable to load keks plugin - please re-enter transmitter id";
                            UserError.Log.wtf(TAG, msg);
                            JoH.static_toast_long(msg);
                        } else {
                            plugin.setPersistence(2, PersistentStore.getBytes(KEKS_ONE + transmitterMAC));
                            if (immediateBonding())
                                needsBonding(!isDeviceLocallyBonded() || ignoreBonding());
                            try {
                                for (int i = 1; i < 4; i++) {
                                    plugin.setPersistence(7 + i, tolerantHexStringToByteArray(Pref.getStringDefaultBlank(KEKS + "_p" + i)));
                                }
                            } catch (Exception e) {
                                UserError.Log.e(TAG, "Problem with persistence");
                            }
                        }
                    } catch (Exception e) {
                        UserError.Log.e(TAG, "Exception getting instance: " + e);
                        e.printStackTrace();
                    }
                } else {
                    plugin = null;
                }

                if (specialPairingWorkaround()) {
                    UserError.Log.d(TAG, "Samsung additional delay");
                    Inevitable.task("samsung delay", 1000, () -> changeState(STATE.CHECK_AUTH));
                } else {
                    changeState(STATE.CHECK_AUTH);
                }

                return;
            }
        }
        UserError.Log.e(TAG, "Could not locate CGM service during discovery");
        incrementErrors();
    }

    private void onDiscoverFailed(Throwable throwable) {
        UserError.Log.e(TAG, "Discover failure: " + throwable.toString());
        incrementErrors();
        prepareToWakeup();
    }

    private void clearSubscription() {
        scanSubscription = null;

    }

    private boolean g5BluetoothWatchdog() {
        return Pref.getBoolean("g5_bluetooth_watchdog", true);
    }

    private boolean genericBluetoothWatchdog() {
        return Pref.getBoolean("bluetooth_watchdog", true);
    }

    public static void updateLast(long timestamp) {
        if ((static_last_timestamp == 0) && (transmitterID != null)) {
            final String ref = "last-ob1-data-" + transmitterID;
            if (PersistentStore.getLong(ref) == 0) {
                PersistentStore.setLong(ref, timestamp);
                if (!android_wear) JoH.playResourceAudio(R.raw.labbed_musical_chime);
            }
        }
        static_last_timestamp = timestamp;
    }

    private String handleBleScanException(BleScanException bleScanException) {
        final String text;

        switch (bleScanException.getReason()) {
            case BleScanException.BLUETOOTH_NOT_AVAILABLE:
                text = "Bluetooth is not available";
                break;
            case BleScanException.BLUETOOTH_DISABLED:
                text = "Enable bluetooth and try again";
                break;
            case BleScanException.LOCATION_PERMISSION_MISSING:
                text = "On Android 6.0+ location permission is required. Implement Runtime Permissions";
                break;
            case BleScanException.LOCATION_SERVICES_DISABLED:
                text = "Location services needs to be enabled on Android 6.0+";
                break;
            case BleScanException.SCAN_FAILED_ALREADY_STARTED:
                text = "Scan with the same filters is already started";
                break;
            case BleScanException.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                text = "Failed to register application for bluetooth scan - Nearby devices permission disabled?";
                break;
            case BleScanException.SCAN_FAILED_FEATURE_UNSUPPORTED:
                text = "Scan with specified parameters is not supported";
                break;
            case BleScanException.SCAN_FAILED_INTERNAL_ERROR:
                text = "Scan failed due to internal error";
                break;
            case BleScanException.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES:
                text = "Scan cannot start due to limited hardware resources";
                break;
            case BleScanException.UNDOCUMENTED_SCAN_THROTTLE:
                text = String.format(
                        Locale.getDefault(),
                        "Android 7+ does not allow more scans. Try in %d seconds",
                        secondsTill(bleScanException.getRetryDateSuggestion())
                );
                break;
            case BleScanException.UNKNOWN_ERROR_CODE:
            case BleScanException.BLUETOOTH_CANNOT_START:
            default:
                text = "Unable to start scanning";
                break;
        }
        UserError.Log.w(TAG, text + " " + bleScanException);
        return text;

    }

    private static class GattRefreshOperation implements RxBleCustomOperation<Void> {
        private long delay_ms = 500;

        GattRefreshOperation() {
        }

        GattRefreshOperation(long delay_ms) {
            this.delay_ms = delay_ms;
        }

        @NonNull
        @Override
        public Observable<Void> asObservable(BluetoothGatt bluetoothGatt,
                                             RxBleGattCallback rxBleGattCallback,
                                             Scheduler scheduler) throws Throwable {

            return Observable.fromCallable(() -> refreshDeviceCache(bluetoothGatt))
                    .delay(delay_ms, TimeUnit.MILLISECONDS, Schedulers.computation())
                    .subscribeOn(scheduler);
        }

        private Void refreshDeviceCache(final BluetoothGatt gatt) {
            UserError.Log.d(TAG, "Gatt Refresh " + (JoH.refreshDeviceCache(TAG, gatt) ? "succeeded" : "failed"));
            return null;
        }
    }

    private int currentBondState = 0;
    public volatile int waitingBondConfirmation = 0; // 0 = not waiting, 1 = waiting, 2 = received
    public volatile int weInitiatedBondConfirmation = 0; // 0 = not waiting, 1 = waiting, 2 = received
    final BroadcastReceiver mBondStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!keep_running) {
                try {
                    UserError.Log.e(TAG, "Rogue bond state receiver still active - unregistering");
                    unregisterReceiver(mBondStateReceiver);
                } catch (Exception e) {
                    //
                }
                return;
            }
            final String action = intent.getAction();
            UserError.Log.d(TAG, "BondState: onReceive ACTION: " + action);
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                final BluetoothDevice parcel_device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                currentBondState = parcel_device.getBondState();
                final int bond_state_extra = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                final int previous_bond_state_extra = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);

                UserError.Log.e(TAG, "onReceive UPDATE Name " + parcel_device.getName() + " Value " + parcel_device.getAddress()
                        + " Bond state " + parcel_device.getBondState() + bondState(parcel_device.getBondState()) + " "
                        + "bs: " + bondState(bond_state_extra) + " was " + bondState(previous_bond_state_extra));
                try {
                    if (parcel_device.getAddress().equals(transmitterMAC)) {
                        msg(bondState(bond_state_extra).replace(" ", ""));
                        if (parcel_device.getBondState() == BluetoothDevice.BOND_BONDED) {
                            if (shortTxId()) {
                                pairKeeper.add(getTransmitterID(), parcel_device.getAddress());
                            }
                            if (waitingBondConfirmation == 1) {
                                waitingBondConfirmation = 2; // received
                                UserError.Log.e(TAG, "Bond confirmation received!");
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                    UserError.Log.d(TAG, "Sleeping before create bond");
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        //
                                    }
                                    instantCreateBondIfAllowed();
                                }
                            }

                            if (weInitiatedBondConfirmation == 1) {
                                weInitiatedBondConfirmation = 2;
                                changeState(GET_DATA);
                            }
                        } else if (parcel_device.getBondState() == BluetoothDevice.BOND_BONDING) {
                            if (Build.VERSION.SDK_INT >= 26) {
                                JoH.playResourceAudio(R.raw.bt_meter_connect);
                                UserError.Log.uel(TAG, "Prompting user to notice pairing request with sound - On Android 8+ you have to manually pair when requested");
                            }
                        }
                    }
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Got exception trying to process bonded confirmation: ", e);
                }
            }
        }
    };

    public void instantCreateBondIfAllowed() {
        if (getInitiateBondingFlag()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    UserError.Log.d(TAG, "instantCreateBond() called");
                    bleDevice.getBluetoothDevice().createBond();
                }
            } catch (Exception e) {
                UserError.Log.e(TAG, "Got exception in instantCreateBond() " + e);
            }
        } else {
            UserError.Log.e(TAG, "instantCreateBond blocked by lack of initiate_bonding flag");
        }
    }


    private boolean getInitiateBondingFlag() {
        return true; // There is no reason not to initiate bonding
    }


    private final BroadcastReceiver mPairingRequestRecevier = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!keep_running) {
                try {
                    UserError.Log.e(TAG, "Rogue pairing request receiver still active - unregistering");
                    unregisterReceiver(mPairingRequestRecevier);
                } catch (Exception e) {
                    //
                }
                return;
            }
            if ((bleDevice != null) && (bleDevice.getBluetoothDevice().getAddress() != null)) {
                UserError.Log.e(TAG, "Processing mPairingRequestReceiver !!!");
                JoH.releaseWakeLock(fullWakeLock);
                fullWakeLock = JoH.fullWakeLock("pairing-screen-wake", 30 * Constants.SECOND_IN_MS);
                if (!android_wear) Home.startHomeWithExtra(context, Home.HOME_FULL_WAKEUP, "1");
                if (!JoH.doPairingRequest(context, this, intent, bleDevice.getBluetoothDevice().getAddress())) {
                    if (!android_wear) {
                        unregisterPairingReceiver();
                        UserError.Log.e(TAG, "Pairing failed so removing pairing automation"); // todo use flag
                    }
                }
            } else {
                UserError.Log.e(TAG, "Received pairing request but device was null !!!");
            }
        }
    };

    private static long secondsTill(Date retryDateSuggestion) {
        return TimeUnit.MILLISECONDS.toSeconds(retryDateSuggestion.getTime() - System.currentTimeMillis());
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private static final String NEEDING_CALIBRATION = "G5_NEEDING_CALIBRATION";
    private static final String IS_STARTED = "G5_IS_STARTED";
    private static final String IS_FAILED = "G5_IS_FAILED";

    private static volatile long lastProcessCalibrationState;

    public static void processCalibrationStateLite(final CalibrationState state, final long incomingTimestamp) {
        if (incomingTimestamp > lastProcessCalibrationState) {
            processCalibrationStateLite(state);
        } else {
            UserError.Log.d(TAG, "Ignoring calibration state as it is: " + JoH.dateTimeText(incomingTimestamp) + " vs local: " + JoH.dateTimeText(lastProcessCalibrationState));
        }
    }

    public static boolean processCalibrationStateLite(final CalibrationState state) {
        if (state == CalibrationState.Unknown) {
            UserError.Log.d(TAG, "Not processing push of unknown state as this is the unset state");
            return false;
        }

        if (msSince(lastProcessCalibrationState) < MINUTE_IN_MS) {
            UserError.Log.d(TAG, "Ignoring duplicate processCalibration State");
            return false;
        }
        lastProcessCalibrationState = tsl();

        lastSensorStatus = state.getExtendedText();
        lastSensorState = state;
        return true;
    }

    public static void processCalibrationState(final CalibrationState state) {

        if (!processCalibrationStateLite(state)) {
            UserError.Log.d(TAG, "Not processing more calibration state as lite returned false");
            return;
        }

        storeCalibrationState(state);

        final boolean needs_calibration = state.needsCalibration();
        final boolean was_needing_calibration = PersistentStore.getBoolean(NEEDING_CALIBRATION);

        final boolean is_started = state.sensorStarted();
        final boolean was_started = PersistentStore.getBoolean(IS_STARTED);

        final boolean is_failed = state.sensorFailed();
        final boolean was_failed = PersistentStore.getBoolean(IS_FAILED);


        if (needs_calibration && !was_needing_calibration) {
            final Class c;
            switch (state) {
                case NeedsFirstCalibration:
                    c = DoubleCalibrationActivity.class;
                    break;
                default:
                    c = AddCalibration.class;
                    break;
            }

            Inevitable.task("ask initial calibration", SECOND_IN_MS * 30, () -> {
                final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_CALIBRATION_REQUEST, JoH.getStartActivityIntent(c), PendingIntent.FLAG_UPDATE_CURRENT);
                // pending intent not used on wear
                JoH.showNotification(state.getText(), "Calibration Required", android_wear ? null : pi, G5_CALIBRATION_REQUEST, state == CalibrationState.NeedsFirstCalibration, true, false);
                UserError.Log.uel(TAG, "Calibration Required");

            });
        } else if (!needs_calibration && was_needing_calibration) {
            JoH.cancelNotification(G5_CALIBRATION_REQUEST);
        }


        if (!is_started && was_started) {
            if (Sensor.isActive()) {
                if (Pref.getBooleanDefaultFalse("ob1_g5_restart_sensor")) {
                    if (state.ended()) {
                        UserError.Log.uel(TAG, "Requesting time-travel restart");
                        Ob1G5StateMachine.restartSensorWithTimeTravel();
                    } else {
                        UserError.Log.uel(TAG, "Attempting to auto-start sensor");
                        Ob1G5StateMachine.startSensor(tsl());
                    }
                    final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_RESTARTED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
                    JoH.showNotification("Auto Start", "Sensor Requesting Restart", pi, G5_SENSOR_RESTARTED, true, true, false);
                    UserError.Log.uel(TAG, "Sensor Requesting Restart");
                } else {
                    UserError.Log.uel(TAG, "Marking sensor session as stopped");
                    Sensor.stopSensor();
                }
            }
            final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_STARTED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
            JoH.showNotification(state.getText(), "Sensor Stopped", pi, G5_SENSOR_STARTED, true, true, false);
            UserError.Log.ueh(TAG, "Native Sensor is now Stopped: " + state.getExtendedText());
            Treatments.sensorStop(null, "Stopped by transmitter: " + state.getExtendedText());
        } else if (is_started && !was_started) {
            JoH.cancelNotification(G5_SENSOR_STARTED);
            UserError.Log.ueh(TAG, "Native Sensor is now Started: " + state.getExtendedText());
            Treatments.sensorStartIfNeeded();
        }

        if (is_failed && !was_failed) {
            final PendingIntent pi = PendingIntent.getActivity(xdrip.getAppContext(), G5_SENSOR_FAILED, JoH.getStartActivityIntent(Home.class), PendingIntent.FLAG_UPDATE_CURRENT);
            JoH.showNotification(state.getText(), "Sensor FAILED", pi, G5_SENSOR_FAILED, true, true, false);
            UserError.Log.ueh(TAG, "Native Sensor is now marked FAILED: " + state.getExtendedText());
        }
        // we can't easily auto-cancel a failed notice as auto-restart may mean the user is not aware of it?


        updateG5State(needs_calibration, was_needing_calibration, NEEDING_CALIBRATION);
        updateG5State(is_started, was_started, IS_STARTED);
        updateG5State(is_failed, was_failed, IS_FAILED);

        if (is_started && shortTxId() && JoH.pratelimit("update_alternate_dex_session_start_time", 60 * 60)) { // Once an hour if there is a local session
            Treatments.sensorUpdateStartTimeIfNeeded();
        }
    }


    private static void updateG5State(boolean now, boolean previous, String reference) {
        if (now != previous) {
            PersistentStore.setBoolean(reference, now);
        }
    }

    private static void storeCalibrationState(final CalibrationState state) {
        PersistentStore.setByte(OB1G5_STATESTORE, state.getValue());
        PersistentStore.setLong(OB1G5_STATESTORE_TIME, tsl());
    }

    private static CalibrationState getStoredCalibrationState() {
        if (msSince(PersistentStore.getLong(OB1G5_STATESTORE_TIME)) < HOUR_IN_MS * 2) {
            return CalibrationState.parse(PersistentStore.getByte(OB1G5_STATESTORE));
        }
        return CalibrationState.Unknown;
    }

    private static void loadCalibrationStateAsRequired() {
        if ((lastSensorState == null) && JoH.quietratelimit("ob1-load-sensor-state", 5)) {
            final CalibrationState savedState = getStoredCalibrationState();
            if (savedState != Unknown) {
                lastSensorState = savedState;
            }
        }
    }

    public static boolean isG5ActiveButUnknownState() {
        loadCalibrationStateAsRequired();
        return (lastSensorState == null || lastSensorState == CalibrationState.Unknown)
                && usingNativeMode();
    }

    public static boolean isG5WarmingUp() {
        loadCalibrationStateAsRequired();
        return lastSensorState != null
                && lastSensorState == CalibrationState.WarmingUp
                && usingNativeMode();
    }

    public static boolean isG5SensorStarted() {
        loadCalibrationStateAsRequired();
        return lastSensorState != null
                && lastSensorState.sensorStarted()
                && usingNativeMode()
                && !pendingStop()
                && !pendingStart();
    }

    public static boolean isPendingStart() {
        return pendingStart() && usingNativeMode();
    }

    public static boolean isPendingStop() {
        return pendingStop() && usingNativeMode();
    }

    public static boolean isPendingCalibration() {
        return pendingCalibration() && usingNativeMode();
    }

    public static boolean isG5WantingInitialCalibration() {
        loadCalibrationStateAsRequired();
        return lastSensorState != null
                && lastSensorState == CalibrationState.NeedsFirstCalibration
                && usingNativeMode();
    }

    public static boolean isG5WantingCalibration() {
        loadCalibrationStateAsRequired();
        try {
            return lastSensorState != null
                    && lastSensorState.needsCalibration()
                    && usingNativeMode();
        } catch (NullPointerException e) {
            return false;
        }
    }

    // are we using the G5 Transmitter to evaluate readings
    public static boolean usingNativeMode() {
        return usingCollector()
                && Pref.getBooleanDefaultFalse("ob1_g5_use_transmitter_alg")
                && Pref.getBooleanDefaultFalse(OB1G5_PREFS);
    }

    public static boolean onlyUsingNativeMode() {
        return (usingNativeMode() && !fallbackToXdripAlgorithm())
                || usingMockPreCalibrated();
    }

    public static boolean usingMockPreCalibrated() {
        return Pref.getBooleanDefaultFalse("fake_data_pre_calibrated")
                && DexCollectionType.getDexCollectionType() == DexCollectionType.Mock;
    }

    public static boolean isProvidingNativeGlucoseData() {
        // TODO check age of data?
        loadCalibrationStateAsRequired();
        return usingNativeMode() && lastSensorState != null && lastSensorState.usableGlucose();
    }

    public static boolean fallbackToXdripAlgorithm() {
        return Pref.getBooleanDefaultFalse("ob1_g5_fallback_to_xdrip");
    }

    public static void msg(String msg) {
        lastState = msg + " " + JoH.hourMinuteString();
        UserError.Log.d(TAG, "Status: " + lastState);
        lastStateUpdated = tsl();
        if (android_wear && wear_broadcast) {
            BroadcastGlucose.sendLocalBroadcast(null);
        }
    }

    /* public static void setWatchStatus(DataMap dataMap) {
         lastStateWatch = dataMap.getString("lastState", "");
         static_last_timestamp_watch = dataMap.getLong("timestamp", 0);
     }

     public static DataMap getWatchStatus() {
         DataMap dataMap = new DataMap();
         dataMap.putString("lastState", lastState);
         dataMap.putLong("timestamp", static_last_timestamp);
         return dataMap;
     }

 */
    // data for NanoStatus
    public static SpannableString nanoStatus() {
        if (android_wear) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(lastSensorStatus != null ? lastSensorStatus + "\n" : "");
            builder.append(state.getString());
            return new SpannableString(builder);
        } else {
            if (usingNativeMode()) {
                if (lastSensorState != null && lastSensorState != CalibrationState.Ok) {
                    if (!lastSensorState.sensorStarted() && isPendingStart()) {
                        return Span.colorSpan("Starting Sensor", NOTICE.color());
                    } else if (lastSensorState.sensorStarted() && isPendingStop()) {
                        return Span.colorSpan("Stopping Sensor", NOTICE.color());
                    } else if (lastSensorState.needsCalibration() && pendingCalibration()) {
                        return Span.colorSpan("Sending calibration", NOTICE.color());
                    } else {
                        return Span.colorSpan(lastSensorState.getExtendedText(), lastSensorState.transitional() ? NOTICE.color() : lastSensorState.sensorFailed() ? CRITICAL.color() : BAD.color());
                    }
                } else {
                    return Span.colorSpan("", NORMAL.color()); // non native blank
                }
            } else {
                return null;
            }
        }
    }

    private static final String PREF_PURDAH = "ob1g5-purdah-time";

    private boolean requiresPurdah() {
        return false;
    }

    private boolean inPurdah() {
        return purdahMs() > 0;
    }

    private long purdahMs() {
        final long purdahTime = PersistentStore.getLong(PREF_PURDAH);
        final long purdahMs = JoH.msTill(purdahTime);
        if (purdahMs < 0 || purdahMs > Constants.HOUR_IN_MS * 4) {
            return 0;
        }
        return purdahMs;
    }

    void setPurdah(final long duration) {
        if (duration > 0) {
            PersistentStore.setLong(PREF_PURDAH, tsl() + duration);
        }
    }

    void needsBonding(final boolean required) {
        plugin.setPersistence(6, joinBytes(new byte[]{(byte) ((required ? 0 : 1) << 1)}, fwChalCache(required)));
    }

    private static void handleUnknownFirmwareClick() {
        UserError.Log.d(TAG, "handleUnknownFirmwareClick()");
        if (UpdateActivity.testAndSetNightly(true)) {
            val vr1 = (VersionRequest1RxMessage) Ob1G5StateMachine.getFirmwareXDetails(getTransmitterID(), 1);
            UserError.Log.d(TAG, "Starting feedback activity");
            xdrip.getAppContext().startActivity(new Intent(xdrip.getAppContext(), SendFeedBack.class).putExtra("generic_text", "Automated Report of unknown firmware version\n" + vr1.toString()).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    // data for MegaStatus
    public static List<StatusItem> megaStatus() {

        init_tx_id(); // needed if we have not passed through local INIT state

        last_mega_status_read = JoH.tsl();

        final List<StatusItem> l = new ArrayList<>();

        if (!DexSyncKeeper.isReady(transmitterID)) {
            l.add(new StatusItem("Hunting Transmitter", "Stay on this page", CRITICAL));
        }

        if (isVolumeSilent() && !isDeviceLocallyBonded()) {
            l.add(new StatusItem("Turn Sound On!", "You will not hear pairing request with volume set low or do not disturb enabled!", CRITICAL));
        }

        l.add(new StatusItem("Phone Service State", lastState + (BlueJayEntry.isPhoneCollectorDisabled() ? "\nDisabled by BlueJay option" : ""), msSince(lastStateUpdated) < 300000 ? (lastState.startsWith("Got data") ? Highlight.GOOD : NORMAL) : (isWatchRunning() ? Highlight.GOOD : CRITICAL)));
        if (last_scan_started > 0) {
            final long scanning_time = msSince(last_scan_started);
            l.add(new StatusItem("Time scanning", niceTimeScalar(scanning_time), scanning_time > MINUTE_IN_MS * 5 ? (scanning_time > MINUTE_IN_MS * 10 ? BAD : NOTICE) : NORMAL));
        }
        if (lastScanError != null) {
            l.add(new StatusItem("Scan Error", lastScanError, BAD));
        }
        if ((lastSensorStatus != null)) {
            l.add(new StatusItem("Sensor Status", lastSensorStatus, lastSensorState != Ok ? NOTICE : NORMAL));
        }

        if (hardResetTransmitterNow) {
            l.add(new StatusItem("Hard Reset", "Attempting - please wait", Highlight.CRITICAL));
        }

        if (transmitterID != null) {
            l.add(new StatusItem("Transmitter ID", transmitterID + ((transmitterMAC != null && get_engineering_mode()) ? "\n" + transmitterMAC : "")));
        }

        if (static_connection_state != null) {
            l.add(new StatusItem("Bluetooth Link", static_connection_state));
        }

        if (static_last_connected > 0) {
            long since = msSince(static_last_connected);
            l.add(new StatusItem("Last Connected", niceTimeScalar(since) + " ago", since < MINUTE_IN_MS * 5 ? Highlight.NORMAL : NOTICE));
        }

        if ((!lastState.startsWith("Service Stopped")) && (!lastState.startsWith("Not running")))
            l.add(new StatusItem("Brain State", state.getString() + (error_count > 1 ? " Errors: " + error_count : ""), error_count > 1 ? NOTICE : error_count > 4 ? BAD : NORMAL));

        if (lastUsableGlucosePacketTime != 0) {
            if (msSince(lastUsableGlucosePacketTime) < MINUTE_IN_MS * 15) {
                l.add(new StatusItem("Native Algorithm", "Data Received " + JoH.hourMinuteString(lastUsableGlucosePacketTime), Highlight.GOOD));
            }
        }

        final int queueSize = Ob1G5StateMachine.queueSize();
        if (queueSize > 0) {
            l.add(new StatusItem("Queue Items", "(" + queueSize + ") " + Ob1G5StateMachine.getFirstQueueItemName()));
        }

        if (max_wakeup_jitter > 5000) {
            l.add(new StatusItem("Slowest Wakeup ", niceTimeScalar(max_wakeup_jitter), max_wakeup_jitter > Constants.SECOND_IN_MS * 10 ? CRITICAL : NOTICE));
        }

        if (JoH.buggy_samsung) {
            l.add(new StatusItem("Buggy handset", "Using workaround", max_wakeup_jitter < TOLERABLE_JITTER ? Highlight.GOOD : BAD));
        }

        final String tx_id = getTransmitterID();

        if (Pref.getBooleanDefaultFalse("wear_sync") &&
                Pref.getBooleanDefaultFalse("enable_wearG5")) {
            l.add(new StatusItem("Watch Service State", lastStateWatch));
            if (static_last_timestamp_watch > 0) {
                l.add(new StatusItem("Watch got Glucose", JoH.niceTimeSince(static_last_timestamp_watch) + " ago"));
            }
        }
        final String sensorCode = getCurrentSensorCode();
        if (sensorCode != null) {
            if (usingG6() && FirmwareCapability.isTransmitterG6(getTransmitterID())) {
                l.add(new StatusItem("Calibration Code", sensorCode));
            }
        }

        if (get_engineering_mode() || FirmwareCapability.isTransmitterPreemptiveRestartCapable(getTransmitterID())) {
            l.add(new StatusItem("Preemptive restarts", !FirmwareCapability.isTransmitterPreemptiveRestartCapable(getTransmitterID()) ? "Not capable" :
                    (Pref.getBooleanDefaultFalse("ob1_g5_preemptive_restart") ? "Enabled" : "Disabled")
                            + (Ob1G5StateMachine.useExtendedTimeTravel() ? " (extended)" : "")));
        }

        final VersionRequest1RxMessage vr1 = (VersionRequest1RxMessage) Ob1G5StateMachine.getFirmwareXDetails(tx_id, 1);
        final VersionRequest2RxMessage vr2 = (VersionRequest2RxMessage) Ob1G5StateMachine.getFirmwareXDetails(tx_id, 2);
        final VersionRequest2RxMessage vr3 = (VersionRequest2RxMessage) Ob1G5StateMachine.getFirmwareXDetails(tx_id, 3);
        try {
            if (vr1 != null) {
                val known = FirmwareCapability.isKnownFirmware(vr1.firmware_version_string);
                if (!known) {
                    if (quietratelimit("log ob1 unknown firmware outer", 3600)) {
                        if (pratelimit("log ob1 unknown firmware", 86400 * 7)) {
                            JoH.logMessage("Unknown firmware: " + DexCollectionType.getBestCollectorHardwareName() + " " + vr1.firmware_version_string);
                        }
                    }
                }
                val unknown = !known ? (" " + "Unknown!" + "\n" + (UpdateActivity.testAndSetNightly(false) ? "Tap to report" : "Tap to use nightly version")) : "";

                l.add(new StatusItem("Firmware Version", vr1.firmware_version_string + unknown, !known ? CRITICAL : NORMAL, !known ? "long-press" : null, !known ? (Runnable) Ob1G5CollectionService::handleUnknownFirmwareClick : null));
                //l.add(new StatusItem("Build Version", "" + vr1.build_version));
                if (vr1.version_code != 3 && get_engineering_mode()) {
                    l.add(new StatusItem("Compat Version", "" + vr1.version_code, NORMAL));
                }
                if (vr1.max_runtime_days != 110 && vr1.max_runtime_days != 112 && vr1.max_runtime_days != 0) {
                    l.add(new StatusItem("Transmitter Life", "" + vr1.max_runtime_days + " " + gs(R.string.days)));
                }

                if (vr1.serial != 0 && get_engineering_mode()) {
                    l.add(new StatusItem("Serial", "" + vr1.serial));
                }

            }
        } catch (Exception e) {
            // TODO add message?
        }

        try {
            if (vr2 != null) {
                if (vr2.typicalSensorDays != 10 && vr2.typicalSensorDays != 7 && vr2.typicalSensorDays != 15) {
                    l.add(new StatusItem("Sensor Period", niceTimeScalar(vr2.typicalSensorDays * DAY_IN_MS), Highlight.NOTICE));
                }

                if (get_engineering_mode()) {
                    if (vr2.lifeSeconds > 0) {
                        l.add(new StatusItem("Sensor Life", "" + niceTimeScalar(vr2.lifeSeconds * SECOND_IN_MS)));
                    }
                    if (vr2.version1 > 0) {
                        l.add(new StatusItem("Version 1", "" + vr2.version1));
                    }
                    if (vr2.version2 > 0) {
                        l.add(new StatusItem("Version 2", "" + vr2.version2));
                    }
                }

                if (vr3 == null && vr2.version1 != 0 && vr2.warmupSeconds != 0 && vr2.warmupSeconds != 7200 && vr2.warmupSeconds != 1620 && vr2.warmupSeconds != 3600) {
                    l.add(new StatusItem("Warm Up Time", niceTimeScalar(vr2.warmupSeconds * SECOND_IN_MS), Highlight.NOTICE));
                }

                //l.add(new StatusItem("Feature mask", vr2.featureBits));
            }
        } catch (Exception e) {
            //
        }

        try {
            if (vr3 != null) {
                if (vr3.warmupSeconds != 7200 && vr3.warmupSeconds != 3600) {
                    l.add(new StatusItem("Warm Up Time", niceTimeScalar(vr3.warmupSeconds * SECOND_IN_MS), Highlight.NOTICE));
                }
            }
        } catch (Exception e) {
            //
        }


        // firmware hardware details
        final VersionRequestRxMessage vr = (VersionRequestRxMessage) Ob1G5StateMachine.getFirmwareXDetails(tx_id, 0);
        try {
            if ((vr != null) && (vr.firmware_version_string.length() > 0)) {

                if (get_engineering_mode()) {
                    if (vr1 != null && !vr.firmware_version_string.equals(vr1.firmware_version_string)) {
                        l.add(new StatusItem("2nd Firmware Version", vr.firmware_version_string, FirmwareCapability.isG6Rev2(vr.firmware_version_string) ? NOTICE : NORMAL));
                    }
                    if (vr1 != null && !vr.bluetooth_firmware_version_string.equals(vr1.firmware_version_string)) {
                        l.add(new StatusItem("Bluetooth Version", vr.bluetooth_firmware_version_string));
                    }
                    l.add(new StatusItem("Other Version", vr.other_firmware_version));
                    //  l.add(new StatusItem("Hardware Version", vr.hardwarev));
                    if (vr.asic != 61440 && vr.asic != 16705 && vr.asic != 243 && vr.asic != 74 && vr.asic != 226 && vr.asic != 0)
                        l.add(new StatusItem("ASIC", vr.asic, NOTICE));
                }
            }
        } catch (NullPointerException e) {
            l.add(new StatusItem("Version", "Information corrupted", BAD));
        }

        // battery details
        boolean bat_request_shown_already = false;
        final BatteryInfoRxMessage bt = Ob1G5StateMachine.getBatteryDetails(tx_id);
        long last_battery_query = PersistentStore.getLong(G5_BATTERY_FROM_MARKER + tx_id);
        if (getBatteryStatusNow) {
            l.add(new StatusItem("Battery Status Request Queued", "Will attempt to read battery status on next sensor reading", NOTICE, "long-press",
                    new Runnable() {
                        @Override
                        public void run() {
                            getBatteryStatusNow = false;
                        }
                    }));
            bat_request_shown_already = true;
        }

        if (JoH.quietratelimit("update-g5-battery-warning", 10)) {
            updateBatteryWarningLevel();
        }

        if (vr1 != null && get_engineering_mode() && vr1.max_inactive_days > 0) {
            l.add(new StatusItem("Shelf Life", "" + vr1.inactive_days + " / " + vr1.max_inactive_days));
        }

        if ((bt != null) && (last_battery_query > 0)) {
            Ob1DexTransmitterBattery parsedBattery = new Ob1DexTransmitterBattery(tx_id, bt, vr);

            if (!bat_request_shown_already) { // Only if we have not already requested an update.
                if (battery0VException) {
                    l.add(new StatusItem("Battery status unavailable", "tap to request update", NORMAL, "long-press",
                            new Runnable() {
                                @Override
                                public void run() {
                                    getBatteryStatusNow = true;
                                }
                            }));
                } else {
                    l.add(new StatusItem("Battery Last queried", JoH.niceTimeSince(last_battery_query) + " " + "ago", NORMAL, "long-press",
                            new Runnable() {
                                @Override
                                public void run() {
                                    getBatteryStatusNow = true;
                                }
                            }));
                }
            }
            if (vr != null) {
                final String battery_status = TransmitterStatus.getBatteryLevel(vr.status).toString();
                if (!battery_status.equals("OK"))
                    l.add(new StatusItem("Transmitter Status", battery_status, BAD));
            }
            Highlight TX_dys_highlight = NORMAL; // Set the default transmitter days highlight to normal
            final int TX_dys = DexTimeKeeper.getTransmitterAgeInDays(tx_id); // Transmitter days
            if (vr != null) {
                if (FirmwareCapability.isTransmitterModified(getTransmitterID())) { // Modified Firefly
                    if (TX_dys > 179) { // No more starts as transmitter days > 179
                        TX_dys_highlight = BAD;
                    } else if (TX_dys > 148) { // Transmitter days approaching 180 - may be time to reset
                        TX_dys_highlight = NOTICE;
                    }
                } else if (FirmwareCapability.isTransmitterRawIncapable(getTransmitterID())) { // Unmodified Firefly
                    if (TX_dys > 99) { // No more starts as transmitter days > 99
                        TX_dys_highlight = BAD;
                    } else if (TX_dys > 68) { // Transmitter days approaching 100 - may be time to order a new one
                        TX_dys_highlight = NOTICE;
                    }
                }
            }
            l.add(new StatusItem("Transmitter Days", parsedBattery.daysEstimate(), TX_dys_highlight));
            if (!battery0VException) { // Only show voltages if they are not 0
                l.add(new StatusItem("Voltage A", parsedBattery.voltageA(), parsedBattery.voltageAWarning() ? BAD : NORMAL));
                l.add(new StatusItem("Voltage B", parsedBattery.voltageB(), parsedBattery.voltageBWarning() ? BAD : NORMAL));
            }
            if (vr != null && FirmwareCapability.isFirmwareResistanceCapable(vr.firmware_version_string)) {
                if (parsedBattery.resistance() != 0) {
                    l.add(new StatusItem("Resistance", parsedBattery.resistance(), parsedBattery.resistanceStatus().highlight));
                }
            }
            if (vr != null && FirmwareCapability.isFirmwareTemperatureCapable(vr.firmware_version_string)) {
                if (parsedBattery.temperature() > 0) {
                    l.add(new StatusItem("Temperature", parsedBattery.temperature() + " \u2103"));
                }
            }
        } else {
            if (!bat_request_shown_already) { // Only if we have not already requested an update
                l.add(new StatusItem("Battery status unavailable", "tap to request update", NORMAL, "long-press",
                        new Runnable() {
                            @Override
                            public void run() {
                                getBatteryStatusNow = true;
                            }
                        }));
            }
        }

        return l;
    }

    public static void resetSomeInternalState() {
        UserError.Log.d(TAG, "Resetting internal state by request");
        transmitterMAC = null; // probably gets reloaded from cache
        state = INIT;
        scan_next_run = true;
    }

    public synchronized void logFailure() {
        val localMac = transmitterMAC;
        if (localMac == null) {
            UserError.Log.e(TAG, "Could not log failure as mac is null");
            return;
        }
        val tsl = tsl();
        failureTally.put(localMac, tsl);
        UserError.Log.d(TAG, "Adding " + localMac + " to failure tally " + JoH.dateTimeText(tsl));
        resetSomeInternalState();
    }

    private static void expireFailures(final boolean all) {
        val remove = new ArrayList<String>();
        for (val entry : failureTally.entrySet()) {
            if (all || msSince(entry.getValue()) > (MINUTE_IN_MS * 30)) {
                remove.add(entry.getKey());
            }
        }
        for (val entry : remove) {
            UserError.Log.d(TAG, "Removing " + entry + " from failure tally");
            failureTally.remove(entry);
        }
    }

    private static boolean inFailureTally(final String mac) {
        if (mac == null) return false;
        return (failureTally.containsKey(mac));
    }

    public void listenForChangeInSettings(boolean listen) {
        try {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if (listen) {
                prefs.registerOnSharedPreferenceChangeListener(prefListener);
            } else {
                prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error with preference listener: " + e + " " + listen);
        }
    }

    public final SharedPreferences.OnSharedPreferenceChangeListener prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
            checkPreferenceKey(key, prefs);
        }
    };


    // remember needs proguard exclusion due to access by reflection
    public static boolean isCollecting() {
        return (state == CONNECT_NOW && msSince(static_last_timestamp) < MINUTE_IN_MS * 30) || msSince(static_last_timestamp) < MINUTE_IN_MS * 6;
    }

    public static boolean usingCollector() {
        return Pref.getBooleanDefaultFalse(OB1G5_PREFS) && DexCollectionType.getDexCollectionType() == DexcomG5;
    }

    // TODO may want to move this to utility method in the future
    private static boolean isVolumeSilent() {
        final AudioManager am = (AudioManager) xdrip.getAppContext().getSystemService(Context.AUDIO_SERVICE);
        return (am.getRingerMode() != AudioManager.RINGER_MODE_NORMAL);
    }

    public static void clearDataWhenTransmitterIdEntered(final String txid) {
        try {
            UserError.Log.e(TAG, "Clearing data when new transmitter is entered: " + txid);
            Ob1G5StateMachine.emptyQueue();
            try {
                DexSyncKeeper.clear(txid);
            } catch (Exception e) {
                //
            }
            Ob1G5CollectionService.clearPersist();
        } catch (Exception e) {
            UserError.Log.e(TAG, "Got error when clearing data: " + e);
        }
    }
}
