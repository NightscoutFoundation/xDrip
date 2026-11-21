package xrwd.li3.ble;

import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import xrwd.li3.Li3SensorState;
import xrwd.li3.crypto.AppIdentity;
import xrwd.li3.data.DataPlaneCrypto;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchControl;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;
import xrwd.li3.pairing.EphemeralExchange;
import xrwd.li3.pairing.Li3Transport;
import xrwd.li3.pairing.PairingFlow;
import xrwd.li3.pairing.PairingResult;
import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.exception.BluetoothDisabledException;
import no.nordicsemi.android.ble.exception.DeviceDisconnectedException;
import no.nordicsemi.android.ble.exception.InvalidRequestException;
import no.nordicsemi.android.ble.exception.RequestFailedException;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/**
 * The Bluetooth side of one Li3 sensor: finds it, runs the pairing handshake on a single worker thread, then
 * decrypts the data plane and hands readings, status and history to a {@link Listener}. It connects by address and
 * scans for the sensor's local name (its address in upper-case hex) on alternate attempts, and backs off up to five
 * minutes between them. Values on the security characteristics never reach the log; see {@link #redact}. Untested on
 * hardware. Shaped like libglupro {@code GluProBle}; the post-handshake notification enable follows RoundWhiteDiscKit
 * {@code SensorSession.refreshDataPlaneNotifications} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class Li3Ble extends BleManager implements Li3Transport {

    private static final String TAG = "Li3Ble";

    private static final long SCAN_TIMEOUT_MS = 30_000;
    private static final long POLL_SLICE_MS = 250;
    /** How long a write, or turning on notifications, waits for the sensor to answer. */
    private static final long GATT_TIMEOUT_MS = 5_000;
    private static final long MAX_RECONNECT_DELAY_MS = 300_000;
    private static final long RECONNECT_STEP_MS = 5_000;
    /** Active attempts before the collector starts waiting in the background. */
    private static final int ACTIVE_ATTEMPTS = 6;
    private static final long BACKGROUND_WAIT_MS = 10 * 60_000;
    private static final long BEFORE_BACKGROUND_MS = 60_000;
    /** Re-asked this often, in the sensor's own minutes, which is how often its history gains a slot. */
    private static final int BACKFILL_RETRY_MINUTES = 5;
    /** How long, in the sensor's own minutes, to keep asking for one hole. */
    private static final int BACKFILL_GIVE_UP_MINUTES = 60;
    /** Data-plane frames in a row that may fail before the connection is dropped and paired again. */
    private static final int MAX_DROPPED_IN_A_ROW = 5;

    private static final List<UUID> REQUIRED = Arrays.asList(Gatt.SEC_CERT_DATA, Gatt.SEC_CHALLENGE_DATA,
            Gatt.SEC_COMMAND_RESPONSE, Gatt.PATCH_CONTROL, Gatt.PATCH_STATUS, Gatt.GLUCOSE_DATA, Gatt.HISTORIC_DATA);
    /**
     * Enabled only after Phase 6: the sensor does not stream until each gets a fresh CCCD write. Clinical data is
     * enabled as xDrip4iOS does, but its frames are not decoded.
     */
    private static final List<UUID> AFTER_PAIRING;

    static {
        List<UUID> afterPairing = new ArrayList<>(Gatt.DATA_PLANE_NOTIFYING);
        afterPairing.add(Gatt.HISTORIC_DATA);
        afterPairing.add(Gatt.CLINICAL_DATA);
        AFTER_PAIRING = afterPairing;
    }

    /** What the collector hears from the sensor. Called on the worker thread. */
    public interface Listener {
        void onStage(String stage);

        /** The state now holds the kAuth and identity label that worked; persist it. */
        void onPaired(Li3SensorState state);

        void onReading(RealtimeReading reading);

        void onStatus(PatchStatus status);

        void onHistory(List<HistoricalPage.Entry> entries);

        void onError(String message);

        /** A scan needs location permission and location turned on; neither is there. */
        default void onLocationNeeded() {
        }

        /** Bluetooth is off, so this attempt waits for the next one; the collector may turn it on in between. */
        default void onBluetoothOff() {
        }

        /** A redacted line of the Bluetooth log. Without an override it goes to logcat only. */
        default void onLog(int priority, String message) {
            Log.println(priority, TAG, message);
        }
    }

    private final Li3SensorState state;
    private final List<AppIdentity> identities;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final SecureRandom random = new SecureRandom();
    private final Map<UUID, BluetoothGattCharacteristic> chars = new ConcurrentHashMap<>();
    private final Map<UUID, BlockingQueue<byte[]>> inbox = new ConcurrentHashMap<>();
    private final GlucoseNotifyAssembler assembler = new GlucoseNotifyAssembler();

    private volatile boolean running;
    private volatile DataPlaneCrypto crypto;
    private int reconnectAttempt;
    /** Whether the pending or current connection was made by a background wait. */
    private boolean background;
    /** Set while a background wait is cancelled, so its disconnect callback does not count as a failed attempt. */
    private boolean cancelling;
    /** Main thread. Whether data decrypted on the current connection, so a drop is retried at once. */
    private boolean streamed;
    private int identityIndex;
    /** Main thread. The retry, scan or background timer that is pending, if any; they never run at the same time. */
    private Runnable timer;
    /** Main thread. When {@link #timer} is due, in real time. */
    private long timerDueAt;
    /** Worker thread only. */
    private int outboundSequence;
    /** Worker thread only. Whether a frame decrypted on this connection, which is what ends the reconnect attempts. */
    private boolean decryptedOnConnection;
    /** Worker thread only. Frames in a row that did not decrypt or parse on this connection. */
    private int droppedInARow;

    /**
     * @param identities the app identities to pair with, in file order; the one named in the state is tried first
     */
    public Li3Ble(@NonNull Context context, Li3SensorState state, List<AppIdentity> identities,
                     Listener listener) {
        super(context);
        this.state = state;
        this.identities = new ArrayList<>(identities);
        this.listener = listener;
        setConnectionObserver(connectionObserver);
    }

    public void start() {
        start(0);
    }

    /**
     * Starts from a later attempt, so a connection restarted after it stalled goes on waiting at low power instead of
     * scanning actively again. Main thread.
     */
    public void start(int fromAttempt) {
        reconnectAttempt = fromAttempt;
        running = true;
        attempt();
    }

    /** @return the attempt the next connect is, counted from 0 since data last decrypted; main thread */
    public int reconnectAttempt() {
        return reconnectAttempt;
    }

    /**
     * Runs the pending retry, scan end or background end now if its time has passed. Their timers count uptime, which
     * stops while the phone sleeps, so a 5-second wait can last until the phone wakes for something else; the
     * collector's wake-up alarm calls this. Main thread.
     */
    public void runOverdue() {
        Runnable due = timer;
        long late = SystemClock.elapsedRealtime() - timerDueAt;
        if (!running || due == null || late < 0) {
            return;
        }
        log(Log.DEBUG, "Wait ran out " + late / 1000 + " s ago while the phone slept; running it now");
        cancelTimer(due);
        due.run();
    }

    /** Disconnects for good; a stopped instance is not restarted. */
    public void stop() {
        running = false;
        handler.removeCallbacksAndMessages(null);
        timer = null;
        stopScan();
        disconnect().enqueue();
        worker.shutdownNow();
        close();
    }

    /**
     * Strips the value from a Nordic log line about a security characteristic, and from every
     * {@code characteristic.setValue(..)} line, which does not say which characteristic it is for.
     */
    public static String redact(String nordicMessage) {
        if (nordicMessage.startsWith("characteristic.setValue(")) {
            return "characteristic.setValue(redacted)";
        }
        for (UUID characteristic : Gatt.HANDSHAKE_NOTIFYING) {
            if (nordicMessage.contains(characteristic.toString())) {
                int at = nordicMessage.indexOf(", value");
                return at < 0 ? nordicMessage : nordicMessage.substring(0, at) + ", value redacted";
            }
        }
        return nordicMessage;
    }

    /** @return a connection observer's reason code, with its name when the library gives it one */
    public static String reasonText(int reason) {
        String name;
        switch (reason) {
            case ConnectionObserver.REASON_UNKNOWN: name = "unknown"; break;
            case ConnectionObserver.REASON_SUCCESS: name = "success"; break;
            case ConnectionObserver.REASON_TERMINATE_LOCAL_HOST: name = "closed by the phone"; break;
            case ConnectionObserver.REASON_TERMINATE_PEER_USER: name = "closed by the sensor"; break;
            case ConnectionObserver.REASON_LINK_LOSS: name = "link loss"; break;
            case ConnectionObserver.REASON_NOT_SUPPORTED: name = "sensor services missing"; break;
            case ConnectionObserver.REASON_CANCELLED: name = "cancelled"; break;
            case ConnectionObserver.REASON_TIMEOUT: name = "timeout"; break;
            default: return "reason " + reason;
        }
        return "reason " + reason + " (" + name + ")";
    }

    @Override
    public int getMinLogPriority() {
        return Log.DEBUG;
    }

    @Override
    public void log(int priority, @NonNull String message) {
        listener.onLog(priority, redact(message));
    }

    // ----------------------------------------------- connect and scan ------------------------------------------------

    private String localName() {
        return state.bleAddress().replace(":", "").toUpperCase(Locale.ROOT);
    }

    @SuppressLint("MissingPermission")
    private void attempt() {
        cancelTimer(attemptRunnable);
        if (!running) {
            return;
        }
        cancelling = false;
        streamed = false;
        AttemptMode mode = attemptMode(reconnectAttempt, BluetoothAdapter.checkBluetoothAddress(state.bleAddress()));
        log(Log.DEBUG, "Attempt " + (reconnectAttempt + 1) + ", " + mode.text);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            // the library fails such a connect without telling the connection observer, so nothing would retry it
            listener.onBluetoothOff();
            scheduleReconnect("Bluetooth is off");
            return;
        }
        if (mode == AttemptMode.BY_SCAN) {
            startScan();
            return;
        }
        if (adapter == null) {
            listener.onError("No Bluetooth adapter");
            scheduleReconnect("no Bluetooth adapter");
            return;
        }
        BluetoothDevice device = adapter.getRemoteDevice(state.bleAddress());
        if (mode == AttemptMode.IN_BACKGROUND) {
            waitInBackground(device);
        } else {
            connectTo(device);
        }
    }

    /** How an attempt looks for the sensor. */
    public enum AttemptMode {
        BY_ADDRESS("by address"), BY_SCAN("by scan"), IN_BACKGROUND("in the background");

        private final String text;

        AttemptMode(String text) {
            this.text = text;
        }
    }

    /**
     * The first attempts take turns connecting by address and scanning. A sensor still not found is most likely out of
     * range for a while, so from then on Android waits for it in the background at low power, with a scan between
     * waits in case it can only be found by name.
     *
     * @param attempt    the attempt, counted from 0 since the last pairing
     * @param hasAddress whether the sensor's Bluetooth address is known and valid
     */
    public static AttemptMode attemptMode(int attempt, boolean hasAddress) {
        if (!hasAddress) {
            return AttemptMode.BY_SCAN;
        }
        if (attempt < ACTIVE_ATTEMPTS) {
            return attempt % 2 == 0 ? AttemptMode.BY_ADDRESS : AttemptMode.BY_SCAN;
        }
        return (attempt - ACTIVE_ATTEMPTS) % 2 == 0 ? AttemptMode.IN_BACKGROUND : AttemptMode.BY_SCAN;
    }

    /** @return how long to wait before this attempt; see {@link #attemptMode} for the arguments */
    public static long reconnectDelayMs(int attempt, boolean hasAddress) {
        if (!hasAddress || attempt < ACTIVE_ATTEMPTS) {
            return Math.min(MAX_RECONNECT_DELAY_MS, attempt * RECONNECT_STEP_MS);
        }
        return attemptMode(attempt, true) == AttemptMode.IN_BACKGROUND ? BEFORE_BACKGROUND_MS : RECONNECT_STEP_MS;
    }

    private void startScan() {
        if (!hasLocationPermission()) {
            listener.onError("Location permission is needed to find the sensor");
            listener.onLocationNeeded();
            scheduleReconnect("no location permission");
            return;
        }
        if (!isLocationEnabled()) {
            listener.onError("Location is off; Android finds no Bluetooth sensor without it");
            listener.onLocationNeeded();
            scheduleReconnect("location is off");
            return;
        }
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setDeviceName(localName()).build());
        if (BluetoothAdapter.checkBluetoothAddress(state.bleAddress())) {
            filters.add(new ScanFilter.Builder().setDeviceAddress(state.bleAddress()).build());
        }
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        log(Log.DEBUG, "Scan filters: name " + localName() + (filters.size() > 1 ? ", address " + state.bleAddress()
                : "") + "; low latency, " + SCAN_TIMEOUT_MS / 1000 + " s");
        try {
            listener.onStage("Scanning for " + localName());
            startTimer(scanTimeout, SCAN_TIMEOUT_MS);
            BluetoothLeScannerCompat.getScanner().startScan(filters, settings, scanCallback);
        } catch (SecurityException e) {
            cancelTimer(scanTimeout);
            listener.onError("No permission to scan: " + e.getMessage());
            scheduleReconnect("no permission to scan");
        } catch (RuntimeException e) {
            cancelTimer(scanTimeout);
            listener.onError("Scan failed: " + e);
            scheduleReconnect("scan failed");
        }
    }

    /** Without it Android returns no scan results and no error, so the sensor would only seem to be missing. */
    private boolean hasLocationPermission() {
        Context context = getContext();
        int granted = PackageManager.PERMISSION_GRANTED;
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == granted
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == granted;
    }

    /** Location turned off hides scan results the same way as a missing permission. */
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        }
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void stopScan() {
        cancelTimer(scanTimeout);
        try {
            BluetoothLeScannerCompat.getScanner().stopScan(scanCallback);
        } catch (RuntimeException ignored) {
            // not scanning, or Bluetooth is off
        }
    }

    private final Runnable scanTimeout = () -> {
        stopScan();
        scheduleReconnect("sensor not seen");
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : null;
            if (localName().equals(name) || state.bleAddress().equalsIgnoreCase(device.getAddress())) {
                log(Log.DEBUG, "Scan found the sensor: name " + name + ", address " + device.getAddress() + ", RSSI "
                        + result.getRssi() + " dBm");
                stopScan();
                connectTo(device);
            }
        }

        /** Without it a failed scan would wait out its timeout and read as "sensor not seen". */
        @Override
        public void onScanFailed(int errorCode) {
            stopScan();
            listener.onError("Scan failed, error " + errorCode);
            scheduleReconnect("scan failed, error " + errorCode);
        }
    };

    private void connectTo(BluetoothDevice device) {
        if (!running) {
            return;
        }
        listener.onStage("Connecting");
        try {
            background = false;
            connect(device).useAutoConnect(false).retry(3, 100).enqueue();
        } catch (SecurityException e) {
            listener.onError("No permission to connect: " + e.getMessage());
            scheduleReconnect("no permission to connect");
        }
    }

    /**
     * Lets Android connect when the sensor comes back in range, at low power and without a direct connect first. The
     * wait is cut off after {@link #BACKGROUND_WAIT_MS} so an active scan still runs now and then.
     */
    private void waitInBackground(BluetoothDevice device) {
        listener.onStage("Waiting in the background for up to " + BACKGROUND_WAIT_MS / 60_000 + " min");
        try {
            background = true;
            startTimer(backgroundTimeout, BACKGROUND_WAIT_MS);
            connect(device).useAutoConnect(true, false).enqueue();
        } catch (SecurityException e) {
            cancelTimer(backgroundTimeout);
            listener.onError("No permission to connect: " + e.getMessage());
            scheduleReconnect("no permission to connect");
        }
    }

    private final Runnable backgroundTimeout = () -> {
        cancelBackground();
        scheduleReconnect("not seen in the background");
    };

    /** Stops a background wait, or the library's own reconnect after a background connection was lost. */
    private void cancelBackground() {
        cancelTimer(backgroundTimeout);
        background = false;
        cancelling = true;
        disconnect().enqueue();
    }

    private void scheduleReconnect(String reason) {
        scheduleAttempt(reconnectAttempt + 1, reason);
    }

    private void scheduleAttempt(int attempt, String reason) {
        if (!running) {
            return;
        }
        reconnectAttempt = attempt;
        long delay = reconnectDelayMs(reconnectAttempt, BluetoothAdapter.checkBluetoothAddress(state.bleAddress()));
        listener.onStage("Reconnecting in " + delay / 1000 + " s: " + reason);
        cancelTimer(attemptRunnable);
        startTimer(attemptRunnable, delay);
    }

    private final Runnable attemptRunnable = this::attempt;

    /** Main thread. Posts a timer and remembers when it is due, for {@link #runOverdue}. */
    private void startTimer(Runnable runnable, long delayMs) {
        timer = runnable;
        timerDueAt = SystemClock.elapsedRealtime() + delayMs;
        handler.postDelayed(runnable, delayMs);
    }

    /** Main thread. Every timer is cancelled here, and each cancels its own as it runs, so none is run twice. */
    private void cancelTimer(Runnable runnable) {
        handler.removeCallbacks(runnable);
        if (timer == runnable) {
            timer = null;
        }
    }

    private final ConnectionObserver connectionObserver = new ConnectionObserver() {
        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            cancelTimer(backgroundTimeout);
            listener.onStage(background ? "Connected from the background wait" : "Connected");
        }

        @Override
        public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
            if (cancelling) {
                return;
            }
            cancelTimer(backgroundTimeout);
            scheduleReconnect("connect failed, " + reasonText(reason));
        }

        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            if (running) {
                worker.execute(Li3Ble.this::session);
            }
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
            crypto = null;
            if (running) {
                worker.execute(assembler::reset);
            }
            if (cancelling) {
                return;
            }
            if (background) {
                // the library would reconnect on its own; the attempts below take over instead
                cancelBackground();
            }
            String why = "disconnected, " + reasonText(reason);
            if (streamed) {
                // most likely a short gap with the sensor still at its address, so the first attempt goes now
                scheduleAttempt(0, why);
            } else {
                scheduleReconnect(why);
            }
        }
    };

    // --------------------------------------------------- GATT ----------------------------------------------------

    @Override
    protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
        chars.clear();
        for (BluetoothGattService service : gatt.getServices()) {
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                chars.put(characteristic.getUuid(), characteristic);
            }
        }
        return chars.keySet().containsAll(REQUIRED);
    }

    @Override
    protected void initialize() {
        inbox.clear();
        for (UUID uuid : Gatt.HANDSHAKE_NOTIFYING) {
            BluetoothGattCharacteristic characteristic = chars.get(uuid);
            setNotificationCallback(characteristic).with((device, data) ->
                    queue(uuid).offer(data.getValue() != null ? data.getValue() : new byte[0]));
            enableNotifications(characteristic).enqueue();
        }
        for (UUID uuid : AFTER_PAIRING) {
            BluetoothGattCharacteristic characteristic = chars.get(uuid);
            if (characteristic != null) {
                setNotificationCallback(characteristic).with((device, data) -> {
                    byte[] value = data.getValue();
                    if (value != null && running) {
                        worker.execute(() -> onDataPlane(uuid, value));
                    }
                });
            }
        }
    }

    @Override
    protected void onServicesInvalidated() {
        chars.clear();
        crypto = null;
    }

    private BlockingQueue<byte[]> queue(UUID characteristic) {
        BlockingQueue<byte[]> queue = inbox.get(characteristic);
        if (queue == null) {
            inbox.putIfAbsent(characteristic, new LinkedBlockingQueue<>());
            queue = inbox.get(characteristic);
        }
        return queue;
    }

    @Override
    public void write(UUID characteristic, byte[] value) throws IOException {
        BluetoothGattCharacteristic target = chars.get(characteristic);
        if (target == null) {
            throw new IOException("Characteristic " + characteristic + " is not available");
        }
        try {
            writeCharacteristic(target, value, WRITE_TYPE_DEFAULT).timeout(GATT_TIMEOUT_MS).await();
        } catch (RequestFailedException e) {
            throw new IOException("Write failed with status " + e.getStatus());
        } catch (DeviceDisconnectedException | BluetoothDisabledException | InvalidRequestException e) {
            throw new IOException("Write failed: " + e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            // Nordic's await throws this when the timeout runs out; while waiting it does not see interrupts
            throw new IOException("No answer to a write within " + GATT_TIMEOUT_MS + " ms");
        }
    }

    @Override
    public byte[] awaitNotify(UUID characteristic, long timeoutMs) throws IOException {
        BlockingQueue<byte[]> queue = queue(characteristic);
        long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        while (true) {
            if (!isConnected()) {
                throw new IOException("Disconnected while waiting for a notify");
            }
            long left = deadline - SystemClock.elapsedRealtime();
            if (left <= 0) {
                throw new IOException("No notify within " + timeoutMs + " ms");
            }
            try {
                byte[] value = queue.poll(Math.min(left, POLL_SLICE_MS), TimeUnit.MILLISECONDS);
                if (value != null) {
                    return value;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while waiting for a notify");
            }
        }
    }

    // ------------------------------------------------- session ---------------------------------------------------

    /** The identity named in the state first, then the rest in file order; advanced after a failed first pair. */
    private AppIdentity currentIdentity() {
        List<AppIdentity> ordered = new ArrayList<>();
        for (AppIdentity identity : identities) {
            if (identity.label().equals(state.identityLabel())) {
                ordered.add(identity);
            }
        }
        for (AppIdentity identity : identities) {
            if (!ordered.contains(identity)) {
                ordered.add(identity);
            }
        }
        return ordered.isEmpty() ? null : ordered.get(identityIndex % ordered.size());
    }

    private void session() {
        crypto = null;
        outboundSequence = 0;
        decryptedOnConnection = false;
        droppedInARow = 0;
        AppIdentity identity = currentIdentity();
        if (identity == null) {
            listener.onError("No Li3 app identity loaded");
            running = false;
            disconnect().enqueue();
            return;
        }
        byte[] cached = identity.label().equals(state.identityLabel()) ? state.kAuth() : null;
        PairingFlow flow = new PairingFlow(this, identity, () -> EphemeralExchange.generate(random));
        try {
            listener.onStage("Pairing with identity " + identity.label() + (cached != null ? ", cached key" : ""));
            PairingResult result = cached != null
                    ? flow.reconnect(cached, state.blePin())
                    : flow.firstPair(state.blePin(), state.sensorKey());
            state.setKAuth(result.kAuth());
            state.setIdentityLabel(identity.label());
            // the identity that paired now comes first; the next connection starts from it
            identityIndex = 0;
            if (result.sensorKey65() != null) {
                // the first pairing pins the key; a later one only gets here with the same key
                state.setSensorKey(result.sensorKey65());
            }
            listener.onStage("Paired at " + result.lastStage() + (result.usedCache() ? ", cached key" : ", fresh key"));
            listener.onPaired(state);
            for (UUID uuid : AFTER_PAIRING) {
                BluetoothGattCharacteristic characteristic = chars.get(uuid);
                if (characteristic != null) {
                    enableNotifications(characteristic).timeout(GATT_TIMEOUT_MS).await();
                }
            }
            // Frames queued meanwhile wait on this worker; with keys set only now, none is read and no backfill is
            // asked for unless every channel, history included, is on
            crypto = new DataPlaneCrypto(result.kEnc(), result.ivEnc());
            listener.onStage("Streaming");
        } catch (GeneralSecurityException | IOException e) {
            if (cached == null) {
                identityIndex++;
            } else if (e instanceof GeneralSecurityException) {
                // the sensor no longer holds this key; the next connection pairs afresh
                state.setKAuth(null);
            }
            listener.onError(e.getMessage());
            disconnect().enqueue();
        } catch (RequestFailedException | DeviceDisconnectedException | BluetoothDisabledException
                 | InvalidRequestException e) {
            listener.onError("Enabling data notifications failed: " + e.getClass().getSimpleName());
            disconnect().enqueue();
        } catch (InterruptedException e) {
            // Nordic's await throws this when the timeout runs out; while waiting it does not see interrupts
            listener.onError("Enabling data notifications got no answer within " + GATT_TIMEOUT_MS + " ms");
            disconnect().enqueue();
        } catch (RuntimeException e) {
            listener.onError("Pairing failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            disconnect().enqueue();
        }
    }

    /** Worker thread. */
    private void onDataPlane(UUID characteristic, byte[] value) {
        DataPlaneCrypto keys = crypto;
        DataPlaneCrypto.Channel channel = DataPlaneCrypto.Channel.forCharacteristic(characteristic);
        if (keys == null || channel == null) {
            return;
        }
        byte[] frame = channel == DataPlaneCrypto.Channel.GLUCOSE_DATA ? assembler.add(value) : value;
        if (frame == null) {
            return;
        }
        Runnable handOver;
        try {
            handOver = parse(keys, channel, keys.decrypt(channel, frame));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            listener.onError("Dropped a " + channel + " frame: " + e.getMessage());
            droppedInARow++;
            if (droppedInARow == MAX_DROPPED_IN_A_ROW) {
                // keys that no longer fit; reconnecting pairs again
                listener.onError("Disconnecting after " + MAX_DROPPED_IN_A_ROW + " dropped frames in a row");
                disconnect().enqueue();
            }
            return;
        } catch (RuntimeException e) {
            // no frame is known to get here; uncaught it would end the app, so it is reported and not counted
            listener.onError("Could not handle a " + channel + " frame: " + e);
            return;
        }
        droppedInARow = 0;
        if (!decryptedOnConnection) {
            decryptedOnConnection = true;
            handler.post(() -> {
                reconnectAttempt = 0;
                streamed = true;
            });
        }
        try {
            handOver.run();
        } catch (RuntimeException e) {
            // the collector failed with a frame that opened, say storing it; uncaught it would end the app, and it
            // says nothing about the keys, so it is not counted as dropped
            listener.onError("Could not handle a " + channel + " frame: " + e);
        }
    }

    /**
     * Worker thread. Parses an opened frame.
     *
     * @return what hands the record to the collector
     * @throws IllegalArgumentException if the frame does not parse
     */
    private Runnable parse(DataPlaneCrypto keys, DataPlaneCrypto.Channel channel, byte[] plain) {
        switch (channel) {
            case PATCH_STATUS:
                PatchStatus status = PatchStatus.parse(plain);
                return () -> {
                    requestBackfill(keys, status.currentLifeCount());
                    listener.onStatus(status);
                };
            case GLUCOSE_DATA:
                RealtimeReading reading = RealtimeReading.parse(plain);
                return () -> {
                    noteMissingMinutes(reading.lifeCount());
                    requestBackfill(keys, reading.lifeCount());
                    listener.onReading(reading);
                };
            default:
                List<HistoricalPage.Entry> entries = HistoricalPage.parse(plain);
                return () -> {
                    backfillCovered(entries);
                    listener.onHistory(entries);
                };
        }
    }

    /**
     * Worker thread. A reading that does not follow on from the last one held leaves the minutes in between to
     * history, which holds one every five minutes, so the hole ends at the last grid minute at or before this
     * reading. A gap with no grid minute in it is left alone: history has nothing for it. Only a realtime reading
     * opens a hole, because only it says which minute was the last one held; a status frame's life count only moves
     * the asking on.
     */
    private void noteMissingMinutes(int lifeCount) {
        Integer last = state.lastLifeCount();
        if (last != null && lifeCount > last + 1) {
            state.openHole(last + 1, lifeCount - lifeCount % HistoricalPage.SPACING);
        }
    }

    /**
     * Worker thread. Asks for history from the oldest minute the sensor still owes, and asks again as the sensor's
     * clock moves on until a value comes back for the newest minute of the hole. One request is not enough: the
     * answer can be lost with the link or the write go unanswered, and the sensor has not written the newest part of
     * its history yet when the gap shows up. The hole and the last ask are kept in the sensor state rather than here,
     * so a connection that drops, a collector that restarts or a phone that sleeps takes the chase up where it was,
     * and the sensor's own clock, not the phone's, says whether a minute has had its chances.
     *
     * @param lifeCount where the sensor's clock has got to, from a reading or from its status
     */
    private void requestBackfill(DataPlaneCrypto keys, int lifeCount) {
        int from = state.backfillFrom();
        if (from == 0 || lifeCount < from) {
            return; // the sensor's clock has not reached the hole, so it cannot answer for it yet
        }
        if (lifeCount > state.backfillTo() + BACKFILL_GIVE_UP_MINUTES) {
            state.closeHole(); // the sensor has had its chances at these minutes, or they are older than this session
            return;
        }
        int askedAt = state.backfillAskedAt();
        if (askedAt >= 0 && lifeCount - askedAt < BACKFILL_RETRY_MINUTES) {
            return;
        }
        state.markHoleAskedAt(lifeCount);
        outboundSequence++;
        try {
            write(Gatt.PATCH_CONTROL, keys.encrypt(PatchControl.WRITE_KIND, outboundSequence,
                    PatchControl.backfillFrom(from)));
            listener.onStage("Backfill requested from life count " + from + " to " + state.backfillTo());
        } catch (GeneralSecurityException | IOException e) {
            listener.onError("Backfill request failed: " + e.getMessage());
        }
    }

    /**
     * Worker thread. A page holding a value at or after the newest minute of the hole means the whole hole came back.
     * A page whose newest slots are the empty ones the sensor writes later leaves those minutes in the hole, and moves
     * its oldest end up to the first of them, so the next ask starts there instead of re-sending pages already held.
     */
    private void backfillCovered(List<HistoricalPage.Entry> entries) {
        if (state.backfillFrom() == 0) {
            return;
        }
        for (HistoricalPage.Entry entry : entries) {
            if (entry.mgdl == null) {
                continue;
            }
            if (entry.lifeCount >= state.backfillTo()) {
                state.closeHole();
                return;
            }
            if (entry.lifeCount >= state.backfillFrom()) {
                state.advanceHoleFrom(entry.lifeCount + HistoricalPage.SPACING);
            }
        }
    }
}
