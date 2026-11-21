package xrwd.li3.ble;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.location.LocationManager;
import android.os.Looper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import xrwd.li3.Li3SensorState;
import xrwd.li3.data.HistoricalPage;
import xrwd.li3.data.PatchStatus;
import xrwd.li3.data.RealtimeReading;

/**
 * Tests for the scan in {@link Li3Ble}: Android hands an app without location permission no scan results and no
 * error, so the collector has to say why it cannot find the sensor.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
@RunWith(RobolectricTestRunner.class)
public class Li3BleScanTest {

    private final Recorder recorder = new Recorder();
    private Li3Ble ble;

    /** Robolectric's adapter starts off; these tests are about the scan, so Bluetooth is on. */
    @Before
    public void setUp() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true);
    }

    @After
    public void tearDown() {
        if (ble != null) {
            ble.stop();
        }
    }

    // ===================================================== scan ======================================================

    /** Without location permission the scan is not started; the stage and the error name the permission. */
    @Test
    public void start_noLocationPermission_reportsPermission() {
        // :: Setup
        ble = scanningBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: no location permission");
        assertThat(recorder.errors).containsExactly("Location permission is needed to find the sensor");
    }

    /** Without location permission the collector is told, so it can ask the user. */
    @Test
    public void start_noLocationPermission_asksForLocation() {
        // :: Setup
        ble = scanningBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.locationNeeded).isEqualTo(1);
    }

    /** With location turned off Android finds nothing either, so the scan is not started and the error says why. */
    @Test
    public void start_locationOff_reportsLocationOff() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        setLocationEnabled(false);
        ble = scanningBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: location is off");
        assertThat(recorder.errors).containsExactly("Location is off; Android finds no Bluetooth sensor without it");
        assertThat(recorder.locationNeeded).isEqualTo(1);
    }

    /** With fine location the scan starts. */
    @Test
    public void start_fineLocation_scans() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = scanningBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages.get(0)).startsWith("Scanning for");
        assertThat(recorder.errors).isEmpty();
        assertThat(recorder.locationNeeded).isEqualTo(0);
    }

    /** Coarse location is enough for an app that targets Android 8. */
    @Test
    public void start_coarseLocation_scans() {
        // :: Setup
        grant(Manifest.permission.ACCESS_COARSE_LOCATION);
        ble = scanningBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages.get(0)).startsWith("Scanning for");
        assertThat(recorder.errors).isEmpty();
    }

    // ================================================= scan results ==================================================

    /** A scan that sees the sensor's address stops and connects to it. */
    @Test
    public void scanResult_sensorAddress_connects() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = addressedScanBle();
        ble.start(1);

        // :: Act
        deliverScanResult(SENSOR_ADDRESS);

        // :: Verify
        assertThat(recorder.stages).containsExactly("Scanning for AABBCCDDEEFF", "Connecting").inOrder();
    }

    /** A scan that sees another device keeps looking: the phone never connects to a device that is not the sensor. */
    @Test
    public void scanResult_otherDevice_doesNotConnect() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = addressedScanBle();
        ble.start(1);

        // :: Act
        deliverScanResult("11:22:33:44:55:66");

        // :: Verify
        assertThat(recorder.stages).containsExactly("Scanning for AABBCCDDEEFF");
    }

    /** A scan Android reports as failed is retried at once, naming the error, instead of waiting out its timeout. */
    @Test
    public void scanFailed_reconnectsNamingError() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = addressedScanBle();
        ble.start(1);

        // :: Act
        for (ScanCallback callback : scanCallbacks()) {
            callback.onScanFailed(ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED);
        }
        idleMainLooper();

        // :: Verify
        assertThat(recorder.errors).containsExactly("Scan failed, error 2");
        assertThat(recorder.stages.get(recorder.stages.size() - 1)).endsWith(": scan failed, error 2");
        assertThat(scanCallbacks()).isEmpty();
    }

    /** A scan that does not see the sensor within 30 s stops and tries again later. */
    @Test
    public void scan_sensorNotSeen_reconnectsAfterTimeout() {
        // :: Setup
        grant(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = addressedScanBle();
        ble.start(1);

        // :: Act
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));

        // :: Verify
        assertThat(recorder.stages.get(1)).endsWith(": sensor not seen");
        assertThat(scanCallbacks()).isEmpty();
    }

    // =================================================== restart =====================================================

    /** A failed attempt counts, so a restarted connection can go on from it. */
    @Test
    public void reconnectAttempt_afterFailedAttempt_one() {
        // :: Setup
        ble = scanningBle();
        ble.start();

        // :: Act
        int attempt = ble.reconnectAttempt();

        // :: Verify
        assertThat(attempt).isEqualTo(1);
    }

    /** Started from a later attempt, it waits as long as that attempt would, not as long as the first. */
    @Test
    public void start_fromFourthAttempt_waitsAsFifth() {
        // :: Setup
        ble = scanningBle();

        // :: Act
        ble.start(4);

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 25 s: no location permission");
    }

    // ==================================================== helpers ====================================================

    private static final String SENSOR_ADDRESS = "AA:BB:CC:DD:EE:FF";

    /** A sensor with a Bluetooth address; its second attempt is a scan, with the address as one of the filters. */
    private Li3Ble addressedScanBle() {
        Li3SensorState state = new Li3SensorState("0RRC989AQ", SENSOR_ADDRESS, new byte[]{1, 2, 3, 4});
        return new Li3Ble(RuntimeEnvironment.getApplication(), state, Collections.emptyList(), recorder);
    }

    private static Set<ScanCallback> scanCallbacks() {
        return shadowOf(BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner()).getScanCallbacks();
    }

    /** Hands every running scan a result from the device at this address, without a scan record. */
    private static void deliverScanResult(String address) {
        idleMainLooper();
        assertThat(scanCallbacks()).isNotEmpty();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        for (ScanCallback callback : scanCallbacks()) {
            callback.onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES,
                    new ScanResult(adapter.getRemoteDevice(address), null, -60, 0));
        }
        idleMainLooper();
    }

    private static void idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    /** A sensor without a usable Bluetooth address, so the first attempt is a scan by name. */
    private Li3Ble scanningBle() {
        Li3SensorState state = new Li3SensorState("0RRC989AQ", "", new byte[]{1, 2, 3, 4});
        return new Li3Ble(RuntimeEnvironment.getApplication(), state, Collections.emptyList(), recorder);
    }

    private static void grant(String permission) {
        Application application = RuntimeEnvironment.getApplication();
        shadowOf(application).grantPermissions(permission);
    }

    private static void setLocationEnabled(boolean enabled) {
        LocationManager locationManager = (LocationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.LOCATION_SERVICE);
        shadowOf(locationManager).setLocationEnabled(enabled);
    }

    /** Keeps the stages and errors the collector would show. */
    private static final class Recorder implements Li3Ble.Listener {
        final List<String> stages = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        int locationNeeded;

        @Override
        public void onStage(String stage) {
            stages.add(stage);
        }

        @Override
        public void onPaired(Li3SensorState state) {
        }

        @Override
        public void onReading(RealtimeReading reading) {
        }

        @Override
        public void onStatus(PatchStatus status) {
        }

        @Override
        public void onHistory(List<HistoricalPage.Entry> entries) {
        }

        @Override
        public void onError(String message) {
            errors.add(message);
        }

        @Override
        public void onLocationNeeded() {
            locationNeeded++;
        }
    }
}
