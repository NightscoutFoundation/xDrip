package lwld.glucose.profile;

import static android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
import static lwld.glucose.profile.iface.DataKey.*;
import static lwld.glucose.profile.iface.State.*;
import static lwld.glucose.profile.iface.State.BLUETOOTH_DISABLED;
import static lwld.glucose.profile.config.Uuids.Item.*;
import static lwld.glucose.profile.util.JobScheduler.JobToken.READ_CHARACTERISTICS;
import static lwld.glucose.profile.util.JobScheduler.JobToken.RECONNECT;
import static lwld.glucose.profile.util.JobScheduler.JobToken.SCAN_CANCEL;
import static lwld.glucose.profile.util.Util.bluetoothEnabled;
import static lwld.glucose.profile.util.Util.getDeviceFromMac;
import static lwld.glucose.profile.util.Util.isValidMacAddress;
import static no.nordicsemi.android.ble.common.data.RecordAccessControlPointData.reportLastStoredRecord;
import static no.nordicsemi.android.ble.common.data.RecordAccessControlPointData.reportStoredRecordsFromRange;
import static no.nordicsemi.android.ble.common.data.RecordAccessControlPointData.reportStoredRecordsGreaterThenOrEqualTo;
import static no.nordicsemi.android.ble.common.profile.glucose.GlucoseTypes.SAMPLE_LOCATION_FINGER;
import static no.nordicsemi.android.ble.common.profile.glucose.GlucoseTypes.TYPE_CAPILLARY_WHOLE_BLOOD;


import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import lombok.Setter;
import lombok.val;
import lwld.glucose.profile.config.Flavour;
import lwld.glucose.profile.config.Uuids;
import lwld.glucose.profile.iface.DataKey;
import lwld.glucose.profile.iface.Device;
import lwld.glucose.profile.iface.ILog;
import lwld.glucose.profile.iface.Listener;
import lwld.glucose.profile.iface.State;
import lwld.glucose.profile.packet.Packet;
import lwld.glucose.profile.packet.Status;
import lwld.glucose.profile.util.JobScheduler;
import lwld.glucose.profile.util.Log;
import lwld.glucose.profile.util.Util;
import no.nordicsemi.android.ble.ReadRequest;
//import no.nordicsemi.android.ble.annotation.LogPriority;
import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.common.callback.RecordAccessControlPointDataCallback;
import no.nordicsemi.android.ble.common.callback.cgm.CGMSessionRunTimeDataCallback;
import no.nordicsemi.android.ble.common.callback.cgm.CGMSessionStartTimeDataCallback;
import no.nordicsemi.android.ble.common.callback.cgm.CGMSpecificOpsControlPointDataCallback;
import no.nordicsemi.android.ble.common.callback.cgm.CGMStatusDataCallback;
import no.nordicsemi.android.ble.common.callback.cgm.ContinuousGlucoseMeasurementDataCallback;
import no.nordicsemi.android.ble.common.data.cgm.CGMSpecificOpsControlPointData;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.text.format.DateFormat;

import no.nordicsemi.android.ble.BleManager;

/**
 * JamOrHam
 * <p>
 * Glucose Profile BLE implementation
 */

public class GluProBle {

    public static final String TAG = GluProBle.class.getSimpleName();

    private final JobScheduler jobScheduler = new JobScheduler();

    private volatile State lastState = INIT;


    private final Context context;
    private volatile InternalManager bleManager;
    private volatile ExecutorService multiExecutor = Executors.newFixedThreadPool(3);

    private Listener listener;
    private volatile String targetName;
    private volatile BluetoothDevice connectedDevice;

    private volatile boolean shouldReconnect = false;
    private volatile int reconnectAttempt = 0;

    private final Map<String, Device> scanned = new ConcurrentHashMap<>();

    private final Map<DataKey, String> data = new ConcurrentHashMap<>();

    private final Map<Uuids.Item, BluetoothGattCharacteristic> chars = new ConcurrentHashMap<>();

    @Setter
    private Flavour flavour = Flavour.GENERIC;

    @Setter
    private String pin; // pairing pin hint - currently unused

    private volatile long sensorStartTime = -1; // calculated sensor start time in epoch millis

    private volatile boolean ready = false; // whether device is ready/connected

    @Setter
    private volatile boolean scanConnect = true; // simple alternator for scan connect strategy

    private volatile boolean bondingInProgress = false; // current bonding attempt in progress

    private volatile boolean startTimeStatic = false; // whether we expect start time to remain the same

    public GluProBle(@NonNull Context context) {
        this.context = context.getApplicationContext();

    }

    public synchronized void start(String deviceName, String deviceMac, Listener listener) {
        this.targetName = deviceName;
        this.listener = listener;
        initManager();

        if (bluetoothEnabled()) {
            this.shouldReconnect = true;
            if (targetName != null && !targetName.isEmpty()) {
                Log.d(TAG, "Searching for: " + targetName);
                if (targetName.equals(data.getOrDefault(DEVICE_NAME, ""))) {
                    Log.d(TAG, "New target: " + targetName + " clearing data map");
                    data.clear();
                }
                if (isValidMacAddress(deviceMac)) {
                    connect(getDeviceFromMac(deviceMac));
                    // connect
                } else {
                    // search by name
                    startScan();
                }
            } else {
                Log.d(TAG, "General scan");
                startScan();
            }
        } else {
            setState(BLUETOOTH_DISABLED);
        }
    }


    public synchronized void stop() {
        shouldReconnect = false;
        stopScan();
        if (bleManager != null) {
            jobScheduler.close();
            bleManager.disconnect().enqueue();
            stopThreads();
            bleManager.close();
            bleManager = null;
        } else {
            Log.e(TAG, "BleManager is already null");
        }
        setState(SHUTDOWN);
    }

    public void setLogger(ILog logger) {
        Log.setLogger(logger);
    }

    private synchronized void initManager() {
        if (bleManager == null) {
            Log.d(TAG, "Initializing BLE manager");
            bleManager = new InternalManager(context);
        }
    }

    private synchronized void startThreads() {
        if (multiExecutor == null || multiExecutor.isShutdown() || multiExecutor.isTerminated()) {
            multiExecutor = Executors.newFixedThreadPool(3);
        }
    }

    private synchronized void stopThreads() {
        if (multiExecutor != null) {
            multiExecutor.shutdownNow();
            multiExecutor = null;
        }
    }

    private void startScan() {

        Log.d(TAG, "Scanning for: " + targetName);

        if (targetName == null) {
            shouldReconnect = false;
        }
        bleManager.disconnect().enqueue();

        val settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        final List<ScanFilter> filters = new ArrayList<>();
        filters.add(new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(uuid(GLUCOSE_SERVICE).toString())).build());
        // if (targetName != null) {
        // this may not always work
        //    filters.add(new ScanFilter.Builder().setDeviceName(targetName).build());
        // }
        try {
            stopScan();
            setState(SCANNING);
            jobScheduler.postDedupedDelayed(SCAN_CANCEL, this::stopScan, 30_000);
            BluetoothLeScannerCompat.getScanner()
                    .startScan(filters, settings, scannerCallback);
        } catch (SecurityException e) {
            Log.e(TAG, "Permission Error scanning: " + e);
            setState(INSUFFICIENT_PERMISSIONS);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Got error with scanning: " + e);
            setState(SCANNING_ERROR);
        } catch (IllegalStateException e) {
            Log.d(TAG, "Got state exception error with scanning: " + e);
            setState(SCANNING_ERROR);
        } catch (Exception e) {
            setState(SCANNING_ERROR);
            Log.wtf(TAG, "Error starting scan: " + e);
        }
    }

    private synchronized void stopScan() {
        Log.d(TAG, "Stopping scan");
        jobScheduler.cancel(SCAN_CANCEL);
        try {
            BluetoothLeScannerCompat.getScanner().stopScan(scannerCallback);
        } catch (Exception ignore) {
        }
        if (lastState == SCANNING) {
            setState(SCAN_STOPPED);
        }
    }

    public synchronized void backFill(long start, long end) {
        if (ready && sensorStartTime > 0) {
            val b = bleManager;
            if (start == -1 && end == -1) {
                if (b != null) {
                    b.backFillLastRecord(); // didn't seem to work
                }
            } else {
                val backfillStart = Math.max(start, sensorStartTime);
                val backfillEnd = Math.min(end, System.currentTimeMillis());
                val startPosition = (int) Math.max(0, (((backfillStart - sensorStartTime) / 60_000)) - 5);
                val endPosition = (int) Math.max(0, ((backfillEnd - sensorStartTime) / 60_000)) + 5;
                Log.d(TAG, "Asking for backfill from " + Util.dateTimeText(backfillStart) + " at position " + startPosition + " to " + Util.dateTimeText(backfillEnd) + " position " + endPosition);
                if (b != null) {
                    b.backFillBetween(startPosition, endPosition);
                } else {
                    Log.e(TAG, "BLE Manager is null, cannot backfill");
                }
            }

        }
    }

    private final ScanCallback scannerCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, @NonNull ScanResult result) {
            BluetoothDevice device = result.getDevice();
            Log.d(TAG, "Scan found a device: " + device.getAddress());
            try {
                scanned.put(device.getAddress(), new Device(device.getName(), device.getAddress()));
                publishScanResults();
            } catch (Exception e) {
                Log.e(TAG, "Error adding device to map: " + e + " for " + device.getAddress());
            }
            if (targetName != null
                    && (targetName.equals(device.getName()) || targetName.equals(device.getAddress()))) {
                Log.d(TAG, "Found target: " + targetName + " stopping scan and connecting");
                stopScan();
                reconnectAttempt = 0;
                connect(device);
            }
        }
    }; // end scanner callback

    @SuppressLint("MissingPermission")
    private void connect(final BluetoothDevice device) {
        if (device == null) {
            Log.d(TAG, "Device is null, cannot connect");
            return;
        }
        connectedDevice = device;
        shouldReconnect = true;

        bleManager.setConnectionObserver(connectionObserver);
        bleManager.setBondingObserver(bondingObserver);

        stopScan();

        try {
            if (useScanConnectStrategy()) {
                Log.d(TAG, "Using scan connect strategy...");
                if (Util.isDeviceBonded(device)) {
                    targetName = device.getAddress();
                    Log.d(TAG, "Device is bonded so scanning for mac " + targetName);
                } else {
                    targetName = device.getName();
                    Log.d(TAG, "Device is not bonded so scanning for name " + targetName);

                    if (targetName == null || targetName.isEmpty()) {
                        Log.d(TAG, "Device name is null or empty so scanning for mac actually " + targetName);
                        targetName = device.getAddress();
                    }
                }
                scheduleReconnect(20_000, "Scan connect strategy");
                startScan();
            } else {
                Log.d(TAG, "BLE Connect request...");
                setState(CONNECTING);
                bleManager.connect(device)
                        .useAutoConnect(false)
                        .retry(3, 100)
                        .enqueue();

            }

        } catch (SecurityException e) {
            Log.e(TAG, "Permission Error bonding/connecting: " + e);
            setState(INSUFFICIENT_PERMISSIONS);
        }
    }

    private void scheduleReconnect(String reason) {
        if (!shouldReconnect || connectedDevice == null) {
            Log.d(TAG, "Refusing to reconnect as shouldReconnect is false or connectedDevice is null: " + reason);
            return;
        }

        reconnectAttempt++;
        final int delay = Math.min(300_000, reconnectAttempt * 5000);

        if (listener != null) {
            listener.onReconnecting(reconnectAttempt);
        }

        Log.d(TAG, "Reconnect attempt " + reconnectAttempt + " in " + delay + "ms");

        bleManager.disconnect().enqueue();
        Log.d(TAG, "Sleeping before reconnecting... " + delay + "ms");

        scheduleReconnect(delay, reason);
    }

    private synchronized void scheduleReconnect(final long delay, final String reason) {
        Log.d(TAG, "Scheduling reconnect in " + delay + "ms - " + reason);
        jobScheduler.postDedupedDelayed(RECONNECT, () -> {
            Log.d(TAG, "Reconnecting...  " + reason);
            if (shouldReconnect) {
                if (bluetoothEnabled()) {
                    connect(connectedDevice);
                } else {
                    Log.d(TAG, "Bluetooth currently disabled...");
                    setState(BLUETOOTH_DISABLED);
                    scheduleReconnect("Bluetooth was disabled during reconnect");
                }
            } else {
                Log.d(TAG, "Not reconnecting as shouldReconnect is false");
            }
        }, delay);
    }

    private final ConnectionObserver connectionObserver = new ConnectionObserver() {

        @Override
        public void onDeviceConnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnecting " + device.getAddress());
            setState(CONNECTING);
        }

        @Override
        public void onDeviceConnected(@NonNull BluetoothDevice device) {
            Log.d(TAG, "onDeviceConnected " + device.getAddress());
            jobScheduler.cancel(RECONNECT);
            reconnectAttempt = 0;
            connectedDevice = device;
            setState(CONNECTED);
            if (listener != null) {
                listener.onConnected(device);
            }
        }

        @Override
        public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) {
            Log.d(TAG, "Failed to connect: " + device.getAddress() + " reason:" + reason);
            setState(CONNECT_FAILED);
            if (listener != null) {
                listener.onError("Failed to connect: " + reason);
            }
            scheduleReconnect("Failed to connect: " + reason);
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        public void onDeviceReady(@NonNull BluetoothDevice device) {
            if (!bondingInProgress) {
                try {
                    if (Util.isDeviceBonded(device)) {
                        Log.d(TAG, "Device ready: " + device.getAddress());
                        data.put(DEVICE_ADDRESS, device.getAddress());
                        setState(READY);
                        ready = true;
                        jobScheduler.postDeduped(READ_CHARACTERISTICS, bleManager::readStaticParameters);
                    } else {
                        Log.d(TAG, "Device ready, but not bonded and not bonding in progress so disconnecting");
                        val lbleManager = bleManager;
                        if (lbleManager!= null) {
                            lbleManager.disconnect().enqueue();
                        }
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception in onDeviceReady: " + e);
                    setState(INSUFFICIENT_PERMISSIONS);
                }

            } else {
                Log.d(TAG, "Device ready, but bonding in progress: " + device.getAddress());
            }
        }

        @Override
        public void onDeviceDisconnecting(@NonNull BluetoothDevice device) {
            Log.d(TAG, "Disconnecting... " + device.getAddress());
            ready = false;
        }

        @Override
        public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) {
            Log.d(TAG, "Disconnected: " + device.getAddress() + " reason:" + reason);
            ready = false;
            if (listener != null) {
                setState(DISCONNECTED);
                listener.onDisconnected(device, reason);
            }
            scheduleReconnect("Device disconnected");
        }
    }; // end connection observer

    private synchronized void setState(State state) {
        if (state == lastState) return;
        lastState = state;
        val l = listener;
        if (l != null) {
            l.onState(state);
            Log.d(TAG, "State changed to " + state);
        }
    }

    private final RecordAccessControlPointDataCallback recordAccessControlPointCallback =
            new RecordAccessControlPointDataCallback() {
                @Override
                public void onRecordAccessOperationCompleted(@NonNull BluetoothDevice device, int requestCode) {
                    Log.d(TAG, "RACP: Completed request " + requestCode);
                }

                @Override
                public void onRecordAccessOperationCompletedWithNoRecordsFound(@NonNull BluetoothDevice device, int requestCode) {
                    Log.d(TAG, "RACP: Completed with no records for request " + requestCode);
                }

                @Override
                public void onNumberOfRecordsReceived(@NonNull BluetoothDevice device, int numberOfRecords) {
                    Log.d(TAG, "RACP: Number of records = " + numberOfRecords);
                }

                @Override
                public void onRecordAccessOperationError(@NonNull BluetoothDevice device, int requestCode, int errorCode) {
                    Log.e(TAG, "RACP Error: request " + requestCode + " error " + errorCode);
                }
            }; // end record access control point callback

    private final CGMSpecificOpsControlPointDataCallback cgmSpecificOpsControlPointDataCallback = new CGMSpecificOpsControlPointDataCallback() {
        @Override
        public void onCGMSpecificOpsOperationCompleted(@NonNull BluetoothDevice device, int requestCode, boolean secured) {
            Log.d(TAG, "CGM operation completed - requestCode: " + requestCode + " secured: " + secured);
        }

        @Override
        public void onCGMSpecificOpsOperationError(@NonNull BluetoothDevice device, int requestCode, int errorCode, boolean secured) {
            Log.e(TAG, "CGM operation error - requestCode: " + requestCode + " errorCode: " + errorCode + " secured: " + secured);
        }

        @Override
        public void onCGMSpecificOpsResponseReceivedWithCrcError(@NonNull BluetoothDevice device, @NonNull Data data) {
            Log.e(TAG, "CGM CRC error in response from device: " + device.getAddress());
            super.onCGMSpecificOpsResponseReceivedWithCrcError(device, data);
        }

        @Override
        public void onContinuousGlucoseCommunicationIntervalReceived(@NonNull BluetoothDevice device, int interval, boolean secured) {
            Log.d(TAG, "CGM communication interval received: " + interval + " secured: " + secured);
            data.put(COMMUNICATION_INTERVAL, interval + "");
            updateOnDataInBackground();
            super.onContinuousGlucoseCommunicationIntervalReceived(device, interval, secured);
        }

        @Override
        public void onContinuousGlucoseCalibrationValueReceived(@NonNull BluetoothDevice device, float glucoseConcentrationOfCalibration, int calibrationTime, int nextCalibrationTime, int type, int sampleLocation, int calibrationDataRecordNumber, @NonNull CGMCalibrationStatus status, boolean secured) {
            Log.d(TAG, "CGM calibration received - glucose: " + glucoseConcentrationOfCalibration + " calibTime: " + calibrationTime + " nextTime: " + nextCalibrationTime + " type: " + type + " location: " + sampleLocation + " record: " + calibrationDataRecordNumber + " status: " + status + " secured: " + secured);
            super.onContinuousGlucoseCalibrationValueReceived(device, glucoseConcentrationOfCalibration, calibrationTime, nextCalibrationTime, type, sampleLocation, calibrationDataRecordNumber, status, secured);
        }

    };


    private final BondingObserver bondingObserver = new BondingObserver() {
        @Override
        public void onBondingRequired(@NonNull BluetoothDevice device) {
            Log.d(TAG, "Bonding required for: " + device.getAddress());
        }

        @Override
        public void onBonded(@NonNull BluetoothDevice device) {
            Log.d(TAG, "Device onBonded: " + device.getAddress());
            bondingInProgress = false;
            setState(BONDED);
            bleManager.connect(device)
                    .useAutoConnect(false)
                    .retry(3, 100)
                    .enqueue();
        }

        @Override
        public void onBondingFailed(@NonNull BluetoothDevice device) {
            Log.e(TAG, "Bonding failed for: " + device.getAddress());
            bondingInProgress = false;
            setState(BONDING_FAILED);
            if (listener != null) {
                listener.onError("Bonding failed");
            }
            scheduleReconnect("Bonding failed");
        }
    }; // end bonding observer

    private synchronized void publishScanResults() {
        executeInBackgroundMulti(() -> {
            List<Device> recentDevices = scanned.values().stream()
                    // keep only those seen within the last 60 seconds
                    .filter(d -> System.currentTimeMillis() - d.getFirstSeen() <= 60_000)
                    // newest first
                    .sorted(Comparator.comparingLong(Device::getFirstSeen).reversed())
                    .collect(Collectors.toList());
            if (listener != null) {
                listener.onScan(recentDevices);
            }
        });

    }

    private UUID uuid(Uuids.Item item) {
        return Uuids.get(flavour, item);
    }

    public void calibrate(long timestamp, int glucose) {
        val timeDiff = System.currentTimeMillis() - timestamp;
        if (glucose > 0 && timeDiff >= 0 && timeDiff < 8 * 3600_000) {
            Log.d(TAG, "Calibrate request: " + Util.dateTimeText(timestamp) + " glucose:" + glucose + " mgdl");
            val whenOffset = calculateTimeOffsetFromUtc(timestamp);
            val nextOffset = calculateTimeOffsetFromUtc(timestamp + (12 * 3600_000));
            Log.d(TAG, "Calibration offset: " + whenOffset + " next:" + nextOffset);
            if (whenOffset > 0) {
                bleManager.calibrate(glucose, whenOffset, nextOffset);
            } else {
                Log.e(TAG, "Invalid time offset for calibration: " + whenOffset);
            }
        } else {
            Log.e(TAG, "Invalid calibrate request: " + Util.dateTimeText(timestamp) + " glucose:" + glucose + " mgdl");
        }
    }

    public void changeReportingPeriod(int period) {
        bleManager.readOrChangeReportingPeriod(period);
    }

    private synchronized void updateOnDataInBackground() {
        val snapshot = new EnumMap<>(data); // create snapshot
        executeInBackgroundMulti(() -> {
            val llistener = listener;
            if (llistener != null) {
                llistener.onData(snapshot);
            }
        });
    }

    private void executeInBackgroundMulti(Runnable task) {
        try {
            startThreads();
            multiExecutor.execute(task);
        } catch (Exception e) {
            Log.wtf(TAG, "Error scheduling task " + e);
        }
    }

    private class InternalManager extends BleManager {

        public InternalManager(@NonNull Context ctx) {
            super(ctx);
        }

        private final ContinuousGlucoseMeasurementDataCallback glucoseCallback = new ContinuousGlucoseMeasurementDataCallback() {
            @Override
            public synchronized void onContinuousGlucoseMeasurementReceived(@NonNull BluetoothDevice device, float glucoseConcentration, @Nullable Float cgmTrend, @Nullable Float cgmQuality, @Nullable CGMStatus status, int timeOffset, boolean secured) {
                val ourStatus = Status.fromCGMStatus(status);
                Log.d(TAG, "CGM measurement read: glucose:" + glucoseConcentration + " trend:" + cgmTrend + " quality:" + cgmQuality + " status:" + Status.getStatusString(ourStatus) + " timeoffset:" + timeOffset + " secured:" + secured);

                handleStatus(ourStatus);

                if (sensorStartTime > 0) { // TODO also validate drift??
                    val timestamp = calculateUtcFromTimeOffset(timeOffset);
                    Log.d(TAG, "Received glucose reading at " + timestamp + " with value: " + glucoseConcentration);
                    if (!Float.isNaN(glucoseConcentration)) {
                        data.put(MGDL, glucoseConcentration + "");
                        data.put(TREND, cgmTrend + "");
                        data.put(QUALITY, cgmQuality + "");
                        data.put(TIMESTAMP, timestamp + "");
                    }
                    data.put(STATUS_STRING, Status.getStatusString(ourStatus));
                    updateOnDataInBackground();

                } else {
                    Log.e(TAG, "Sensor start time not set yet, skipping data");
                    // TODO try to re-read
                }

            }
        };


        private void handleStatus(HashSet<Status> ourStatus) {
            if (ourStatus == null) {
                Log.e(TAG, "Null status received from device");
                return;
            }
            data.put(CALIBRATION_ALLOWED, !ourStatus.contains(Status.CALIBRATION_NOT_ALLOWED) + "");
            data.put(CALIBRATION_RECOMMENDED, ourStatus.contains(Status.CALIBRATION_RECOMMENDED) + "");
            data.put(CALIBRATION_REQUIRED, ourStatus.contains(Status.CALIBRATION_REQUIRED) + "");
            data.put(SESSION_STOPPED, ourStatus.contains(Status.SESSION_STOPPED) + "");
            data.put(BATTERY_LOW, ourStatus.contains(Status.DEVICE_BATTERY_LOW) + "");
            data.put(SENSOR_MALFUNCTION, ourStatus.contains(Status.SENSOR_MALFUNCTION) + "");
            data.put(VALUE_TOO_LOW, ourStatus.contains(Status.SENSOR_RESULT_LOWER_THEN_DEVICE_CAN_PROCESS) + "");
            data.put(VALUE_TOO_HIGH, ourStatus.contains(Status.SENSOR_RESULT_HIGHER_THEN_DEVICE_CAN_PROCESS) + "");
            data.put(DEVICE_ALERT, ourStatus.contains(Status.DEVICE_SPECIFIC_ALERT) + "");
            data.put(DEVICE_FAULT, ourStatus.contains(Status.GENERAL_DEVICE_FAULT) + "");
            data.put(TEMPERATURE_TOO_HIGH, ourStatus.contains(Status.SENSOR_TEMPERATURE_TOO_HIGH) + "");
            data.put(TEMPERATURE_TOO_LOW, ourStatus.contains(Status.SENSOR_TEMPERATURE_TOO_LOW) + "");

            updateOnDataInBackground();
        }

        @Override
        public boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
            BluetoothGattCharacteristic ch = null;
            val gService = gatt.getService(uuid(GLUCOSE_SERVICE));
            if (gService != null) {
                ch = gService.getCharacteristic(uuid(GLUCOSE));
                chars.put(GLUCOSE, ch);

                chars.put(RECORD_ACCESS, gService.getCharacteristic(uuid(RECORD_ACCESS)));
                chars.put(CGM_STATUS, gService.getCharacteristic(uuid(CGM_STATUS)));
                chars.put(CONTROL_POINT, gService.getCharacteristic(uuid(CONTROL_POINT)));

                chars.put(SESSION_START_TIME, gService.getCharacteristic(uuid(SESSION_START_TIME)));
                chars.put(SESSION_RUN_TIME, gatt.getService(uuid(GLUCOSE_SERVICE))
                        .getCharacteristic(uuid(SESSION_RUN_TIME)));

            } // end glucose service present

            val dService = gatt.getService(uuid(DEVICE_INFORMATION_SERVICE));
            if (dService != null) {
                chars.put(FIRMWARE_CHAR, dService.getCharacteristic(uuid(FIRMWARE_CHAR)));
                chars.put(MANUFACTURER_CHAR, dService.getCharacteristic(uuid(MANUFACTURER_CHAR)));
                chars.put(MODEL_NUMBER_CHAR, dService.getCharacteristic(uuid(MODEL_NUMBER_CHAR)));
                chars.put(SERIAL_NUMBER_CHAR, dService.getCharacteristic(uuid(SERIAL_NUMBER_CHAR)));
                chars.put(HARDWARE_REVISION_CHAR, dService.getCharacteristic(uuid(HARDWARE_REVISION_CHAR)));
                chars.put(SYSTEM_ID_CHAR, dService.getCharacteristic(uuid(SYSTEM_ID_CHAR)));
            } // end device info service present

            return ch != null;
        }

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        @Override
        protected void initialize() {

            val ldevice = connectedDevice;
            if (ldevice != null) {
                try {
                    if (ldevice.getBondState() != BluetoothDevice.BOND_BONDED
                            && !Util.isDeviceBonded(ldevice)) {
                        // secondary search
                        Log.d(TAG, "Device not bonded, initiating bonding...");
                        if (pin != null && !pin.isEmpty()) {
                            Log.d(TAG, "Setting pairing pin to " + pin + "");
                            ldevice.setPin(pin.getBytes());
                            //ldevice.setPairingConfirmation(true);
                        }
                        setState(BONDING);
                        bondingInProgress = true;
                        boolean result = ldevice.createBond();
                        if (!result) {
                            Log.e(TAG, "Failed to bond device: " + ldevice.getAddress());
                            result = ldevice.createBond();
                            if (!result) {
                                setState(BONDING_FAILED);
                                bondingInProgress = false;
                            }
                        }

                    } else {

                        // already bonded here
                        Log.d(TAG, "Configuring...");
                        setState(CONFIGURING);

                        val glucoseNotify = chars.get(GLUCOSE);
                        if (glucoseNotify == null) {
                            Log.e(TAG, "No glucose characteristic found");
                            setState(SETUP_FAILED);
                        }


                        setNotificationCallback(glucoseNotify).with(glucoseCallback);

                        enableNotifications(glucoseNotify)
                                .fail((device, status) -> {
                                    if (listener != null)
                                        listener.onError("Failed to enable notifications, status=" + status);
                                })
                                .done(device -> {
                                    // TODO doesn't work
                                    //readCharacteristic(glucoseNotify)
                                    //       .with(glucoseCallback)
                                    //       .enqueue();
                                })
                                .enqueue();


                        setIndicationCallback(chars.get(RECORD_ACCESS)).with(recordAccessControlPointCallback);
                        enableIndications(chars.get(RECORD_ACCESS)).enqueue();
                        setIndicationCallback(chars.get(CONTROL_POINT)).with(cgmSpecificOpsControlPointDataCallback);
                        enableIndications(chars.get(CONTROL_POINT)).enqueue();
                    }
                } catch (SecurityException e) {
                    Log.e(TAG, "Security Exception: " + e);
                }
            } else {
                Log.d(TAG, "No device connected yet, skipping configuration - shouldn't happen");
            }
        }

        @Override
        protected void onServicesInvalidated() {
            chars.clear();
        }

        public void readStaticParameters() {
            Log.d(TAG, "Reading static parameters...");

            safeReadCharacteristic(chars.get(CGM_STATUS), new CGMStatusDataCallback() {

                @Override
                public void onContinuousGlucoseMonitorStatusChanged(@NonNull BluetoothDevice device, @NonNull CGMStatus status, int timeOffset, boolean secured) {
                    Log.d(TAG, "CGM status: Current clock: " + timeOffset);
                    sensorStartTime = System.currentTimeMillis() - (timeOffset * 60_000L); // TODO watch for time changes
                    data.put(START_TIME_MILLIS, sensorStartTime + "");
                    val ourStatus = Status.fromCGMStatus(status);
                    Log.d(TAG, "Current status: " + Status.getStatusString(ourStatus));
                    handleStatus(ourStatus);
                }
            });

            readCharToDataIfNotPresent(FIRMWARE_CHAR, FIRMWARE_VERSION);
            readCharToDataIfNotPresent(MANUFACTURER_CHAR, MANUFACTURER);
            readCharToDataIfNotPresent(MODEL_NUMBER_CHAR, MODEL_NUMBER);
            readCharToDataIfNotPresent(SERIAL_NUMBER_CHAR, SERIAL_NUMBER);
            readCharToDataIfNotPresent(HARDWARE_REVISION_CHAR, HARDWARE_REVISION);
            // readCharToDataIfNotPresent(SYSTEM_ID_CHAR, SYSTEM_ID);

            if (!startTimeStatic) {
                safeReadCharacteristic(chars.get(SESSION_START_TIME), new CGMSessionStartTimeDataCallback() {
                    @Override
                    public void onContinuousGlucoseMonitorSessionStartTimeReceived(@NonNull BluetoothDevice device, @NonNull Calendar calendar, boolean secured) {

                        val result = DateFormat.getMediumDateFormat(context)
                                .format(calendar.getTime()) + " " + DateFormat
                                .getTimeFormat(context).format(calendar.getTime());
                        Log.d(TAG, "Session start time: " + result);
                        data.put(START_TIME, result);
                        updateOnDataInBackground();

                        if (sensorStartTime > 0) {
                            if (Math.abs(sensorStartTime - calendar.getTimeInMillis()) > 120_000) {
                                Log.d(TAG, "Sensor start time is more than 120 seconds off from device time");
                                safeWriteCharacteristic(chars.get(SESSION_START_TIME), new Data(Packet.setCgmSessionStartTime(System.currentTimeMillis())));
                            } else {
                                Log.d(TAG, "Sensor start time is within 120 seconds of device time");
                                startTimeStatic = true; // should not change now
                            }

                        } else {
                            Log.d(TAG, "Sensor start time not set yet, skipping session start time evaluation");
                        }

                    }
                });
            }

            if (!data.containsKey(RUN_TIME)) {
                safeReadCharacteristic(chars.get(SESSION_RUN_TIME), new CGMSessionRunTimeDataCallback() {
                    @Override
                    public void onContinuousGlucoseMonitorSessionRunTimeReceived(@NonNull BluetoothDevice device, int sessionRunTime, boolean secured) {
                        data.put(RUN_TIME, sessionRunTime + "");
                        Log.d(TAG, "Run time set to: " + sessionRunTime + " minutes");
                        updateOnDataInBackground();
                    }
                });
            }

            updateOnDataInBackground();

            if (!data.containsKey(COMMUNICATION_INTERVAL)) {
                readOrChangeReportingPeriod(-1);
            }
        }

        public void calibrate(int glucose, int whenOffset, int nextOffset) {
            if (whenOffset <= 0) {
                return;
            }
            val calibration = CGMSpecificOpsControlPointData.setCalibrationValue(glucose,
                    TYPE_CAPILLARY_WHOLE_BLOOD, SAMPLE_LOCATION_FINGER, whenOffset, nextOffset,
                    true);
            bleManager.safeWriteCharacteristic(chars.get(CONTROL_POINT), calibration);
        }

        public void readOrChangeReportingPeriod(int period) {
            val r = CGMSpecificOpsControlPointData.getCommunicationInterval(true);
            safeWriteCharacteristic(chars.get(CONTROL_POINT), r);
            if (period != -1) {
                Log.d(TAG, "Setting reporting period to " + period + " minutes");
                val w = CGMSpecificOpsControlPointData.setCommunicationInterval(period, true);
                safeWriteCharacteristic(chars.get(CONTROL_POINT), w);
                safeWriteCharacteristic(chars.get(CONTROL_POINT), r);
            }
        }

        private void readCharToDataIfNotPresent(Uuids.Item item, DataKey key) {
            if (!data.containsKey(key)) {
                Log.d(TAG, "Reading characteristic: " + key);
                readCharToData(chars.get(item), key);
            }
        }

        private void readCharToData(BluetoothGattCharacteristic characteristic, DataKey key) {
            safeReadCharacteristic(characteristic,
                    (device, rdata) -> {
                        String value = rdata.getStringValue(0);
                        Log.d(TAG, "Read characteristic: " + key + " value:" + value);
                        data.put(key, value);
                        updateOnDataInBackground();
                    });
        }

        protected void safeReadCharacteristic(@Nullable BluetoothGattCharacteristic characteristic,
                                              @Nullable DataReceivedCallback callback) {

            if (characteristic == null) {
                Log.d(TAG, "safeReadCharacteristic: characteristic is null — skipping read request.");
                return;
            }

            ReadRequest request = readCharacteristic(characteristic);

            if (callback != null) {
                request.with(callback);
            }

            request.enqueue();
        }

        protected void safeWriteCharacteristic(@Nullable final BluetoothGattCharacteristic characteristic,
                                               @Nullable final Data data) {
            if (characteristic != null) {
                writeCharacteristic(characteristic, data, WRITE_TYPE_DEFAULT).enqueue();
            } else {
                Log.d(TAG, "Null characteristic passed to safeWriteCharacteristic, skipping write request.");
            }
        }


        public void backFillFrom(int position) {
            safeWriteCharacteristic(chars.get(RECORD_ACCESS),
                    reportStoredRecordsGreaterThenOrEqualTo(position));

        }

        public void backFillBetween(int start, int end) {
            safeWriteCharacteristic(chars.get(RECORD_ACCESS),
                    reportStoredRecordsFromRange(start, end));

        }

        public void backFillLastRecord() {
            safeWriteCharacteristic(chars.get(RECORD_ACCESS), reportLastStoredRecord());

        }

        @Override
        public void log(final int priority, @NonNull final String message) {
            Log.d(TAG, message);
        }
    }


    private synchronized boolean useScanConnectStrategy() {
        try {
            return scanConnect; // current value
        } finally {
            scanConnect = !scanConnect; // toggle
        }
    }

    // Can't be in the future
    private long calculateUtcFromTimeOffset(int timeOffset) {
        if (sensorStartTime > 0) {
            return Math.min(sensorStartTime + (timeOffset * 60_000L), System.currentTimeMillis());
        } else {
            return -1;
        }
    }

    private int calculateTimeOffsetFromUtc(long utc) {
        if (sensorStartTime <= 0) return -1;
        return (int) ((utc - sensorStartTime) / 60_000L);
    }
}