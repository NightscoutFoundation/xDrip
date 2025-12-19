package com.eveningoutpost.dexdrip.cgm.glupro;

import static com.eveningoutpost.dexdrip.cgm.glupro.GluPro.clearCalibration;
import static com.eveningoutpost.dexdrip.cgm.glupro.GluPro.getCalibration;
import static com.eveningoutpost.dexdrip.cgm.glupro.GluPro.getStart;
import static com.eveningoutpost.dexdrip.cgm.glupro.GluPro.setStart;
import static com.eveningoutpost.dexdrip.models.JoH.dateTimeText;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.niceTimeScalar;
import static com.eveningoutpost.dexdrip.models.JoH.pratelimit;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.HOUR_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.*;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import static lwld.glucose.profile.iface.DataKey.*;
import static lwld.glucose.profile.iface.State.*;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.text.SpannableString;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.g5model.SensorDays;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.models.UserError.*;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.framework.ForegroundService;
import com.eveningoutpost.dexdrip.utils.framework.WakeLockTrampoline;
import com.eveningoutpost.dexdrip.xdrip;


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import lwld.glucose.profile.GluProBle;

import lombok.val;
import lwld.glucose.profile.iface.DataKey;
import lwld.glucose.profile.iface.Device;
import lwld.glucose.profile.config.Flavour;
import lwld.glucose.profile.iface.ILog;
import lwld.glucose.profile.iface.Listener;
import lwld.glucose.profile.iface.State;
import lwld.glucose.profile.util.Util;

/**
 * JamOrHam
 * <p>
 * Glucose Profile collector service class
 */
public class GluProService extends ForegroundService {

    public static final String TAG = GluProService.class.getSimpleName();

    private static final String PREFS_LAST_GLUPRO_NAME = "LAST_GLUPRO_NAME";
    private static final String PREFS_LAST_GLUPRO_MAC = "LAST_GLUPRO_MAC";

    private static final ConcurrentHashMap<DataKey, String> lastData = new ConcurrentHashMap<>();

    // last received glucose timestamp
    private volatile long lastTimestamp = -1;

    // last received onData timestamp
    private static volatile long lastOnData = -1;

    // shared view model
    public final ViewModel viewModel = ViewModelProvider.get();

    // next scheduled wake up time
    long wakeup_time = -1;

    // last service wake up
    long last_wakeup = -1;

    // whether bluetooth is connected to device
    private static volatile boolean connected = false;

    // GluPro module client instance
    GluProBle client;

    // last received state
    static volatile State lastState = UNKNOWN;

    // last reconnect attempt counter
    static volatile int lastReconnectAttempt = -1;

    // last requested backfill
    static volatile Pair<Long, Long> lastBackfill = new Pair<>(-1L, -1L);

    // callback listener
    private final Listener listener = new Listener() {
        @Override
        public void onConnected(BluetoothDevice device) {
            connected = true;
            lastReconnectAttempt = 0;
        }

        @Override
        public void onDisconnected(BluetoothDevice device, int reason) {
            connected = false;
        }

        @Override
        public void onReconnecting(int attempt) {
            lastReconnectAttempt = attempt;
        }

        @Override
        public synchronized void onData(final Map<DataKey, String> incomingData) {
            val data = new EnumMap<>(incomingData); // create snapshot

            lastOnData = tsl();
            for (Map.Entry<DataKey, String> entry : data.entrySet()) {
                if (!Objects.equals(lastData.getOrDefault(entry.getKey(), ""), entry.getValue())) {
                    Log.d(TAG, "Data - Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    // TODO use this for handling changes?
                }
            }
            lastData.clear();
            lastData.putAll(data);


            if (data.containsKey(MGDL) && data.containsKey(TIMESTAMP)) {
                Sensor.createDefaultIfMissing();
                val timestamp = Long.parseLong(data.get(TIMESTAMP)); // TODO sanity check
                if (timestamp != lastTimestamp) {
                    lastTimestamp = timestamp;
                    try {
                        val reading = BgReading.bgReadingInsertFromGluPro(Double.parseDouble(data.get(MGDL)), timestamp, "GluPro");

                        if (reading != null && reading.timestamp == timestamp) {
                            // new record was created
                            checkAndExecuteBackfill();
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error inserting glucose reading: " + e.getMessage());
                    }

                } else {
                    Log.d(TAG, "Duplicate timestamp");
                }
            }

            if (data.containsKey(DEVICE_ADDRESS)) {
                val oldmac = Pref.getString(PREFS_LAST_GLUPRO_MAC, "");
                val mac = data.getOrDefault(DEVICE_ADDRESS, "");
                if (!oldmac.equals(mac)) {
                    Pref.setString(PREFS_LAST_GLUPRO_MAC, mac);
                    Log.d(TAG, "Device MAC address changed: " + mac);
                }
            }

            if (data.containsKey(START_TIME_MILLIS)) {
                val startTime = Long.parseLong(data.getOrDefault(START_TIME_MILLIS, "0"));
                if (startTime > 0 && getStart() != startTime) {
                    Log.d(TAG, "Start time millis updated to " + dateTimeText(startTime) + " vs old: " + dateTimeText(getStart()));
                    setStart(startTime); // save
                    if (pratelimit("glu-pro-time-update-backfill", 600)) {
                        checkAndExecuteBackfill();
                    } else {
                        Log.d(TAG, "Skipping backfill as rate limited");
                    }
                }
            }

            scheduleWakeUp(); // push back wakeup if we got data call
        }

        @Override
        public void onScan(List<Device> scannedDevices) {
            Log.d(TAG, "onScan received: " + scannedDevices.size());

            // update if we got new devices and they are different
            if (!scannedDevices.isEmpty() && !scannedDevices.equals(viewModel.scannedDevices)) {
                JoH.runOnUiThread(() -> {
                    viewModel.scannedDevices.clear();
                    viewModel.scannedDevices.addAll(scannedDevices);
                });

            }
            Log.d(TAG, "onScan finished: " + viewModel.scannedDevices.size());
        }

        @Override
        public synchronized void onState(State state) {
            switch (state) {

                // scanning so we need to select device
                case SCANNING:
                    viewModel.scanning.set(true);
                    // if we don't have current device, open the selector screen automatically if we can
                    if (pratelimit("glu-pro-start-control", 60)) {
                        if (Pref.getString(PREFS_LAST_GLUPRO_NAME, "").isEmpty()) {
                            GluPro.startControlActivity();
                        }
                    }
                    break;
                case BONDING:
                    // play sound to alert that bonding is about to happen
                    if (pratelimit("glu-pro-bonding", 30)) {
                        JoH.playResourceAudio(R.raw.bt_meter_connect);
                    }
                    break;

                case BONDED:
                    // play success noise on bonding being achieved
                    if (pratelimit("glu-pro-bonded", 30)) {
                        JoH.playResourceAudio(R.raw.labbed_musical_chime);
                    }

            }

            // update view model scanning state when scanning stops
            if (!state.equals(SCANNING) && viewModel.scanning.get()) {
                viewModel.scanning.set(false);
            }

            lastState = state;
            viewModel.lastState.set(state);

        }

        @Override
        public void onError(String message) {
            Log.d(TAG, "Error: " + message);
        }
    }; // end listener


    @Override
    public void onCreate() {
        connected = false;
        lastReconnectAttempt = 0;

        client = new GluProBle(xdrip.getAppContext());

        // inject logger callback
        client.setLogger(new ILog() {
            @Override
            public int d(String tag, String msg) {
                UserError.Log.d(tag, msg);
                return 1;
            }

            @Override
            public int e(String tag, String msg) {
                UserError.Log.e(tag, msg);
                return 1;
            }

            @Override
            public int i(String tag, String msg) {
                UserError.Log.uel(tag, msg);
                return 1;
            }

            @Override
            public int wtf(String tag, String msg) {
                UserError.Log.wtf(tag, msg);
                return 1;
            }
        }); // end logger callback

        // inject our service callback to the view model
        viewModel.setServiceCallback(new ViewModel.ServiceCallback() {

            @Override
            public void onDeviceSelected(final String name, final String address) {
                if (shouldServiceRun()) {
                    Pref.setString(PREFS_LAST_GLUPRO_NAME, name);
                    Pref.setString(PREFS_LAST_GLUPRO_MAC, address); // invalidate mac
                    setStart(0L); // invalidate start
                    lastReconnectAttempt = 0;
                    if (name != null && address != null) {
                        ActiveBluetoothDevice.setDevice(name, address); // cosmetic only
                    }
                    client.start(name, address, listener);
                    if (name == null && address == null) {
                        // calling twice seems helpful
                        Inevitable.task("glu pro scan twice", 200, () -> client.start(name, address, listener));
                    }
                } else {
                    // service should not be running
                    viewModel.setServiceCallback(null); // stop any further callbacks
                }
            }

            @Override
            public void onPin(String pin) {
                client.setPin(pin);
            }

        }); // end service callback

        Sensor.createDefaultIfMissing();
        startInitialConnect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final PowerManager.WakeLock wl = JoH.getWakeLock(TAG + "-osc", 60_000);
        try {

            Log.d(TAG, "WAKE UP WAKE UP WAKE UP");

            // Check service should be running
            if (!shouldServiceRun()) {
                Log.d(TAG, "Stopping service due to shouldServiceRun() result");
                //   msg("Stopping");
                stopSelf();
                return START_NOT_STICKY;
            }
            //    buggySamsungCheck();

            last_wakeup = tsl();
            startIfDisconnected();
            eachCycle();

            scheduleWakeUp();
        } finally {
            JoH.releaseWakeLock(wl);
        }
        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroying service");
        if (client != null) {
            Log.d(TAG, "Stopping client");
            client.stop();
            // client = null;
        }
        super.onDestroy();
    }

    private void checkAndExecuteBackfill() {
        if (pratelimit("glu-pro-backfill-check", 250)) {
            val result = GluProBackFill.check();
            if (result != null) {
                Log.d(TAG, "Backfill assessment: " + dateTimeText(result.first) + " -> " + dateTimeText(result.second));
                if (!lastBackfill.equals(result) || pratelimit("glu-pro-backfill-repeat", 3600)) {
                    client.backFill(result.first, result.second);
                    lastBackfill = result;
                } else {
                    Log.d(TAG, "Skipping same backfill as already done in last hour");
                }
            } else {
                Log.d(TAG, "No backfill result (null)");
            }
        }
    }

    private void startIfDisconnected() {
        if (!connected) {
            Log.d(TAG, "Attempting initial connect due to not connected");
            startInitialConnect();
            return;
        }
        if (isCriticalError(lastState)) {
            Log.d(TAG, "Attempting initial connect due to critical error");
            startInitialConnect();
            return;
        }

        // may already be handled by above..
        switch (lastState) {
            case DISCONNECTED:
            case SCANNING_ERROR:
            case BONDING_FAILED:
                Log.d(TAG, "Attempting initial connect due to disconnected state: " + lastState);
                startInitialConnect();
                break;
        }
    }

    // attempt a connection or scan
    private void startInitialConnect() {
        val lastName = Pref.getString(PREFS_LAST_GLUPRO_NAME, null);
        val mac = Pref.getString(PREFS_LAST_GLUPRO_MAC, "");
        if (lastName != null && !lastName.isEmpty()) {
            Log.d(TAG, "Using last device MAC address: " + mac);
            client.start(lastName, mac, listener);
        } else {
            Log.d(TAG, "No known name so trying open scan");
            client.start(null, null, listener);
        }
    }

    private void eachCycle() {
        client.setFlavour(Flavour.ACCUCHEK_SMARTGUIDE);

        // send any calibration pending
        val calibration = getCalibration();
        if (calibration != null) {
            if (lastState.equals(READY)) {
                client.calibrate(calibration.first, calibration.second);
                clearCalibration();
            } else {
                Log.d(TAG, "Not calibrating as not in READY state - retry on next wakeup");
            }
        }

    }

    // get the best estimate of runtime in ms
    public static long getRunTime() {
        val defaultRunHours = 336;
        long runHours = 0;
        try {
            runHours = Long.parseLong(lastData.getOrDefault(RUN_TIME, "" + defaultRunHours));
        } catch (NumberFormatException | NullPointerException e) {
            //
        }
        if (runHours <= 0 || runHours >= 720) {
            runHours = defaultRunHours;
        }

        return runHours * HOUR_IN_MS;
    }

    void scheduleWakeUp() {

        val next = tsl() + Constants.MINUTE_IN_MS * 15;
        // schedule wakeup time if different to our existing one
        if (Math.abs(next - wakeup_time) > Constants.SECOND_IN_MS * 10) {
            wakeup_time = next;

            JoH.wakeUpIntent(xdrip.getAppContext(), JoH.msTill(next), WakeLockTrampoline.getPendingIntent(GluProService.class, Constants.GLUPRO_SERVICE_FAILOVER_ID));
        }
    }

    // whether this service should be running
    private static boolean shouldServiceRun() {
        return DexCollectionType.getDexCollectionType() == DexCollectionType.GluPro;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * MegaStatus
     */
    public static List<StatusItem> megaStatus() {
        val si = new ArrayList<StatusItem>();
        val device = Pref.getString(PREFS_LAST_GLUPRO_NAME, "");
        val serial = lastData.getOrDefault(SERIAL_NUMBER, "");
        if (!device.isEmpty()
                && (!serial.isEmpty() && !device.endsWith(serial))
                && (!Util.isValidMacAddress(device) || Home.get_engineering_mode())) {
            si.add(new StatusItem(gs(R.string.glupro_device), device));
        }
        if (lastDataContains(SERIAL_NUMBER)) {
            si.add(new StatusItem(gs(R.string.glupro_sensor_serial), lastData.get(SERIAL_NUMBER)));
        }
        si.add(new StatusItem(gs(R.string.glupro_collector_state), localisedState(lastState), colorForState(lastState)));

        if (!connected && lastReconnectAttempt > 2) {
            si.add(new StatusItem(gs(R.string.glupro_reconnect_attempt), "" + lastReconnectAttempt, (lastReconnectAttempt > 10) ? BAD : NOTICE));
        }

        if (lastDataTrue(SENSOR_MALFUNCTION)) {
            si.add(new StatusItem(gs(R.string.glupro_sensor_malfunction), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(BATTERY_LOW)) {
            si.add(new StatusItem(gs(R.string.glupro_battery_low), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(SESSION_STOPPED)) {
            si.add(new StatusItem(gs(R.string.glupro_session_stopped), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(VALUE_TOO_LOW)) {
            si.add(new StatusItem(gs(R.string.glupro_value_too_low), gs(R.string.yes), BAD));
        }

        if (lastDataTrue(VALUE_TOO_HIGH)) {
            si.add(new StatusItem(gs(R.string.glupro_value_too_high), gs(R.string.yes), BAD));
        }

        if (lastDataTrue(DEVICE_ALERT)) {
            si.add(new StatusItem(gs(R.string.glupro_sensor_alert), gs(R.string.yes), BAD));
        }

        if (lastDataTrue(DEVICE_FAULT)) {
            si.add(new StatusItem(gs(R.string.glupro_sensor_fault), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(TEMPERATURE_TOO_HIGH)) {
            si.add(new StatusItem(gs(R.string.glupro_temperature_too_high), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(TEMPERATURE_TOO_LOW)) {
            si.add(new StatusItem(gs(R.string.glupro_temperature_too_low), gs(R.string.yes), CRITICAL));
        }

        if (lastDataTrue(CALIBRATION_RECOMMENDED)) {
            si.add(new StatusItem(gs(R.string.glupro_calibration_recommended), gs(R.string.yes), NOTICE));
        }

        if (lastDataTrue(CALIBRATION_REQUIRED)) {
            si.add(new StatusItem(gs(R.string.glupro_calibration_required), gs(R.string.yes), BAD));
        }

        if (Home.get_engineering_mode()) {
            if (lastDataContains(STATUS_STRING)) {
                si.add(new StatusItem(gs(R.string.glupro_collector_status), lastData.get(STATUS_STRING)));
            }
        }

        // TODO consolidate last reading with quality etc here
        if (lastDataContains(QUALITY)) {
            si.add(new StatusItem(gs(R.string.glupro_reading_quality), lastData.get(QUALITY).replaceAll(".0$", "") + "%")); // TODO evaluate
        }

        val period = lastData.getOrDefault(COMMUNICATION_INTERVAL, "5");
        if (!Objects.equals(period, "5")) {
            si.add(new StatusItem(gs(R.string.glupro_reading_frequency), period, NOTICE));
        }

        val modelNumber = lastData.getOrDefault(MODEL_NUMBER, "");
        if (!modelNumber.isEmpty() && !modelNumber.equals("303")) { // TODO list of knowns
            si.add(new StatusItem(gs(R.string.glupro_model_number), lastData.get(MODEL_NUMBER), NOTICE));
        }

        if (lastDataContains(HARDWARE_REVISION)) {
            si.add(new StatusItem(gs(R.string.glupro_hardware_revision), lastData.get(HARDWARE_REVISION)
                    .replace(lastData.getOrDefault(SERIAL_NUMBER, "bogus_text"), "")
                    .replaceAll("_0+$", "")));
        }

        if (lastDataContains(FIRMWARE_VERSION)) {
            si.add(new StatusItem(gs(R.string.glupro_firmware_version), lastData.get(FIRMWARE_VERSION)));
        }
        if (lastDataContains(MANUFACTURER)) {
            si.add(new StatusItem(gs(R.string.glupro_manufacturer), lastData.get(MANUFACTURER)));
        }

        if (lastDataContains(START_TIME)) {
            si.add(new StatusItem(gs(R.string.glupro_start_time), lastData.get(START_TIME)));
        }

        if (lastDataContains(RUN_TIME)) {
            si.add(new StatusItem(gs(R.string.glupro_total_sensor_period), niceTimeScalar(getRunTime())));
            if (getStart() > 0) {
                si.add(new StatusItem(gs(R.string.glupro_period_remaining), niceTimeScalar(SensorDays.get().getRemainingSensorPeriodInMs())));
            }
        }

        si.add(new StatusItem(gs(R.string.glupro_search_sensor_screen), gs(R.string.glupro_open), NORMAL, "button-press", GluPro::startControlActivity));

        return si;
    }

    public static boolean calibrationPossible() {
        return lastDataTrue(CALIBRATION_ALLOWED);
    }

    private static boolean lastDataTrue(DataKey key) {
        try {
            return Boolean.parseBoolean(lastData.getOrDefault(key, "false"));
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean lastDataContains(DataKey key) {
        return lastData.get(key) != null;
    }

    private static StatusItem.Highlight colorForState(State state) {
        if (State.isGood(state)) return GOOD;
        if (State.isWarning(state)) return NOTICE;
        if (isError(state)) return BAD;
        if (isCriticalError(state)) return CRITICAL;
        return NORMAL;

    }

    private static String localisedState(State lastState) {
        switch (lastState) {
            case SCANNING:
                return gs(R.string.glupro_bluetooth_scanning);
            case DISCONNECTED:
                return gs(R.string.glupro_disconnected);
            case CONNECTING:
                return gs(R.string.glupro_connecting);
            case CONNECTED:
                return gs(R.string.glupro_connected);
            case CONFIGURING:
                return gs(R.string.glupro_configuring);
            case CONFIGURED:
                return gs(R.string.glupro_configured);
            case CONNECT_FAILED:
                return gs(R.string.glupro_connect_failed);
            case BONDING:
                return gs(R.string.glupro_bonding);
            case BONDED:
                return gs(R.string.glupro_bonded);
            case BONDING_FAILED:
                return gs(R.string.glupro_bonding_failed);
            case SCANNING_ERROR:
                return gs(R.string.glupro_scanning_error);
            case INSUFFICIENT_PERMISSIONS:
                return gs(R.string.glupro_insufficient_permissions);
            case BLUETOOTH_DISABLED:
                return gs(R.string.glupro_bluetooth_disabled);
            case SCAN_STOPPED:
                return gs(R.string.glupro_scan_stopped);
            case SETUP_FAILED:
                return gs(R.string.glupro_setup_failed);
            case SHUTDOWN:
                return gs(R.string.glupro_shut_down);
            case INIT:
                return gs(R.string.glupro_starting_up);
            case READY:
                return gs(R.string.glupro_operational);
            default:
                return lastState.toString();
        }

    }

    private static StringBuilder append(StringBuilder sb, String s) {
        if (sb.length() > 0) {
            sb.append("\n");
        }
        sb.append(s);
        return sb;
    }

    private static String bestStatusString() {
        val sb = new StringBuilder();
        if (isError(lastState) || isCriticalError(lastState)) {
            append(sb, localisedState(lastState));
        }
        if (lastDataTrue(SENSOR_MALFUNCTION)) {
            append(sb, gs(R.string.glupro_sensor_malfunction));
        }
        if (lastDataTrue(BATTERY_LOW)) {
            append(sb, gs(R.string.glupro_battery_low));
        }
        if (lastDataTrue(VALUE_TOO_LOW)) {
            append(sb, gs(R.string.glupro_value_too_low));
        }
        if (lastDataTrue(VALUE_TOO_HIGH)) {
            append(sb, gs(R.string.glupro_value_too_low));
        }
        if (lastDataTrue(DEVICE_ALERT)) {
            append(sb, gs(R.string.glupro_sensor_alert));
        }
        if (lastDataTrue(DEVICE_FAULT)) {
            append(sb, gs(R.string.glupro_sensor_fault));
        }
        if (lastDataTrue(TEMPERATURE_TOO_HIGH)) {
            append(sb, gs(R.string.glupro_temperature_too_high));
        }
        if (lastDataTrue(TEMPERATURE_TOO_LOW)) {
            append(sb, gs(R.string.glupro_temperature_too_low));
        }
        if (lastDataTrue(SESSION_STOPPED)) {
            append(sb, gs(R.string.glupro_session_stopped));
        }
        if (lastDataTrue(CALIBRATION_REQUIRED)) {
            append(sb, gs(R.string.glupro_calibration_required));
        }
        if (lastDataTrue(CALIBRATION_RECOMMENDED)) {
            append(sb, gs(R.string.glupro_calibration_recommended));
        }

        // TODO low quality?

        val result = sb.toString();
        return !result.isEmpty() ? result : "";
    }


    // accessed via reflection
    public static boolean isCollecting() {
        return lastState.equals(READY) && msSince(lastOnData) < MINUTE_IN_MS * 12;
    }

    // accessed via reflection
    public static SpannableString nanoStatus() {
        val bestStatusString = bestStatusString();
        return bestStatusString == null ? null : new SpannableString(bestStatusString);
    }

}
