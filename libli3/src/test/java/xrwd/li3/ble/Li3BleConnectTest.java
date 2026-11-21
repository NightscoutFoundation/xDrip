package xrwd.li3.ble;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Looper;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowSystemClock;

import java.time.Duration;
import java.util.Collections;

import xrwd.li3.Li3SensorState;

/**
 * Tests for how {@link Li3Ble} connects and retries, against Robolectric's Bluetooth adapter.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
@RunWith(RobolectricTestRunner.class)
public class Li3BleConnectTest {

    private static final String ADDRESS = "AA:BB:CC:DD:EE:FF";

    private final RecordingListener recorder = new RecordingListener();
    private Li3Ble ble;

    @After
    public void tearDown() {
        if (ble != null) {
            ble.stop();
        }
    }

    // ================================================= Bluetooth off =================================================

    /** With Bluetooth off nothing tells the collector the connect failed, so the attempt waits and says why. */
    @Test
    public void start_bluetoothOff_reconnectsLater() {
        // :: Setup
        setBluetoothEnabled(false);
        ble = addressedBle();

        // :: Act
        ble.start();
        idleMainLooper();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: Bluetooth is off");
    }

    /** A scan with Bluetooth off finds nothing either, so it is not started and the reason is Bluetooth. */
    @Test
    public void start_bluetoothOffWithoutAddress_reconnectsLater() {
        // :: Setup
        setBluetoothEnabled(false);
        Li3SensorState state = new Li3SensorState("0RRC989AQ", "", new byte[]{1, 2, 3, 4});
        ble = new Li3Ble(RuntimeEnvironment.getApplication(), state, Collections.emptyList(), recorder);

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: Bluetooth is off");
        assertThat(recorder.errors).isEmpty();
    }

    /** An attempt that finds Bluetooth off tells the collector, which may turn it on before the next attempt. */
    @Test
    public void start_bluetoothOff_tellsListener() {
        // :: Setup
        setBluetoothEnabled(false);
        ble = addressedBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.bluetoothOff).isEqualTo(1);
    }

    /** With Bluetooth on the collector is not told it is off. */
    @Test
    public void start_bluetoothOn_doesNotTellListener() {
        // :: Setup
        setBluetoothEnabled(true);
        ble = addressedBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.bluetoothOff).isEqualTo(0);
    }

    /** With Bluetooth on the first attempt connects by address. */
    @Test
    public void start_bluetoothOn_connects() {
        // :: Setup
        setBluetoothEnabled(true);
        ble = addressedBle();

        // :: Act
        ble.start();

        // :: Verify
        assertThat(recorder.stages.get(0)).isEqualTo("Connecting");
    }

    // ================================================= overdue waits =================================================

    /**
     * A retry wait runs on uptime, which stops while the phone sleeps; a wait that ran out in real time is run when
     * the service wakes up.
     */
    @Test
    public void runOverdue_retryWaitPassed_retriesNow() {
        // :: Setup
        setBluetoothEnabled(false);
        ble = addressedBle();
        ble.start();
        sleep(Duration.ofSeconds(6));

        // :: Act
        ble.runOverdue();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: Bluetooth is off",
                "Reconnecting in 10 s: Bluetooth is off").inOrder();
    }

    /** A wait that has not run out is left to its timer. */
    @Test
    public void runOverdue_retryWaitNotPassed_waits() {
        // :: Setup
        setBluetoothEnabled(false);
        ble = addressedBle();
        ble.start();
        sleep(Duration.ofSeconds(4));

        // :: Act
        ble.runOverdue();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: Bluetooth is off");
    }

    /** A scan whose 30 seconds ran out while the phone slept ends, and the next attempt is scheduled. */
    @Test
    public void runOverdue_scanTimePassed_endsScan() {
        // :: Setup
        setBluetoothEnabled(true);
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        ble = addressedBle();
        ble.start(1);
        sleep(Duration.ofSeconds(31));

        // :: Act
        ble.runOverdue();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Scanning for AABBCCDDEEFF",
                "Reconnecting in 10 s: sensor not seen").inOrder();
    }

    /** A background wait whose ten minutes ran out while the phone slept is cut off, so a scan runs next. */
    @Test
    public void runOverdue_backgroundWaitPassed_endsWait() {
        // :: Setup
        setBluetoothEnabled(true);
        ble = addressedBle();
        ble.start(6);
        sleep(Duration.ofMinutes(10).plusSeconds(1));

        // :: Act
        ble.runOverdue();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Waiting in the background for up to 10 min",
                "Reconnecting in 5 s: not seen in the background").inOrder();
    }

    /** A stopped connection does not come back on a wake-up. */
    @Test
    public void runOverdue_afterStop_doesNothing() {
        // :: Setup
        setBluetoothEnabled(false);
        ble = addressedBle();
        ble.start();
        ble.stop();
        sleep(Duration.ofSeconds(6));

        // :: Act
        ble.runOverdue();

        // :: Verify
        assertThat(recorder.stages).containsExactly("Reconnecting in 5 s: Bluetooth is off");
    }

    // ==================================================== helpers ====================================================

    /** A sensor with a valid Bluetooth address, so the first attempt connects by address. */
    private Li3Ble addressedBle() {
        Li3SensorState state = new Li3SensorState("0RRC989AQ", ADDRESS, new byte[]{1, 2, 3, 4});
        return new Li3Ble(RuntimeEnvironment.getApplication(), state, Collections.emptyList(), recorder);
    }

    private static void setBluetoothEnabled(boolean enabled) {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(enabled);
    }

    /** Moves the clock on without running the main looper, the way a timer is held back while the phone sleeps. */
    private static void sleep(Duration duration) {
        ShadowSystemClock.advanceBy(duration);
    }

    private static void idleMainLooper() {
        shadowOf(Looper.getMainLooper()).idle();
    }
}
