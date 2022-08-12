package com.eveningoutpost.dexdrip.Models;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.Services.DexCollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests for {@link blueReader}. Verifying commands and bg readings.
 * <p>
 * Got great input from @SandraK82 on how the BlueReader works.
 *
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.04
 */
public class BlueReaderTest extends RobolectricTestWithConfig {

    // ===== Initial setup and reset ===============================================================
    private ByteArrayOutputStream _out;

    @Before
    public void setUp() {
        xdrip.checkAppContext(RuntimeEnvironment.application);
    }

    /**
     * Fetch log output to be able to assert code-paths
     */
    @Before
    public void initShadowLog() {
        _out = new ByteArrayOutputStream();
        ShadowLog.stream = new PrintStream(_out);
    }

    /**
     * Reset hibernated counter to ensure stable tests. Using invalid data.
     * Remove all mock BgReadings
     * Remove all mock Active Bluetooth Devices
     */
    @Before
    public void reset() {
        blueReader.decodeblueReaderPacket("0".getBytes(), 1);
        BgReading.deleteALL();
        new Delete().from(ActiveBluetoothDevice.class).execute();
        new Delete().from(TransmitterData.class).execute();
        new Delete().from(Calibration.class).execute();

        PersistentStore.setLong("blueReader_Full_Battery", 0);
    }

    @Test
    public void tt() {
        assertThat(xdrip.getAppContext().getString(R.string.battery)).isEqualTo("Battery");
    }

    // ===== Command-tests =============================================================================
    @Test
    public void decodeBlueReaderPacket_TransF() {
        // :: Setup
        byte[] buffer = "TRANS_FAILED".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("W/blueReader: Attention: check position of blueReader on the sensor, as it was not able to read!");
    }

    @Test
    public void decodeBlueReaderPacket_Wake() {
        // :: Setup
        byte[] buffer = "WAKE".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("D/blueReader: blueReader was set to wakeup-mode manually...");
    }

    @Test
    public void decodeBlueReaderPacket_Echo() {
        // :: Setup
        byte[] buffer = "ECHO".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("D/blueReader: blueReader was set to Echo-Mode manually...");
    }

    @Test
    public void decodeBlueReaderPacket_NfcReady() {
        // :: Setup
        byte[] buffer = "NFC READY".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("D/blueReader: blueReader notice that NFC is active...");
    }

    @Test
    public void decodeBlueReaderPacket_NfcDisabled() {
        // :: Setup
        byte[] buffer = "NFC_DISABLED".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("D/blueReader: blueReader notice that NFC is now hibernated...");
    }


    @Test
    public void decodeBlueReaderPacket_HybernateSuccess_HibernatedCounterZero() {
        // :: Setup
        byte[] buffer = "HYBERNATE SUCCESS".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("I/blueReader: blueReader notice that NFC is now really hibernated...");
    }

    @Test
    public void decodeBlueReaderPacket_HybernateSuccess_HibernatedCounterNotZero() {
        // :: Setup
        byte[] buffer = "HYBERNATE SUCCESS".getBytes();
        blueReader.decodeblueReaderPacket("not ready for".getBytes(), -1);

        // :: Act

        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("l");
        String log = getLogs();
        assertThat(log).contains("I/blueReader: blueReader notice that NFC is now really hibernated...");
        assertThat(log).contains("W/blueReader: Found hibernation after wrong read. Resend read-command...");
    }

    @Test
    public void decodeBlueReaderPacket_Unknown() {
        // :: Setup
        byte[] buffer = "-r 0:ASDF".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("D/blueReader: blueReader sends an unknown reaction: '-r 0:ASDF'");
    }

    @Test
    public void decodeBlueReaderPacket_Battery_NoLastBgReading() {
        // :: Setup
        byte[] buffer = "battery: 222".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("l");
    }

    @Test
    public void decodeBlueReaderPacket_Battery_OldBgReading() {
        // :: Setup
        byte[] buffer = "battery: 4000 980".getBytes();

        // Add mock bg readings
        Sensor mockSensor = createMockSensor();
        addMockBgReading(125, 10, mockSensor);

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("l");
    }

    @Test
    public void decodeBlueReaderPacket_Battery_NewBgReading() {
        // :: Setup
        byte[] buffer = "battery: 4000 980".getBytes();

        // Add mock bg readings
        Sensor mockSensor = createMockSensor();
        addMockBgReading(125, 2, mockSensor);

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNull();
    }

    @Test
    public void decodeBlueReaderPacket_IDR_noBgReading() {
        // :: Setup
        String inputString = "IDR0|blue131-a1";
        Double resultVersion = 131d;

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(inputString.getBytes(), -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("l");
        assertThat(PersistentStore.getString("blueReaderFirmware")).isEqualTo(inputString);
        assertThat(PersistentStore.getDouble("blueReaderFirmwareValue")).isWithin(0.01d).of(resultVersion) ;
    }

    @Test
    public void decodeBlueReaderPacket_IDR_OldBgReading() {
        // :: Setup
        String inputString = "IDR0|blue131-a1";

        // Add mock bg readings
        Sensor mockSensor = createMockSensor();
        addMockBgReading(125, 10, mockSensor);

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(inputString.getBytes(), -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("l");
        assertThat(PersistentStore.getString("blueReaderFirmware")).isEqualTo(inputString);
    }

    @Test
    public void decodeBlueReaderPacket_IDR_NewBgReading() {
        // :: Setup
        String inputString = "IDR0|blue131-a1";

        // Add mock bg readings
        Sensor mockSensor = createMockSensor();
        addMockBgReading(125, 2, mockSensor);

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(inputString.getBytes(), -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(PersistentStore.getString("blueReaderFirmware")).isEqualTo(inputString);
    }

    @Test
    public void decodeBlueReaderPacket_null() {
        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(null, -1);

        // :: Verify
        assertThat(reply).isNull();
        assertThat(getLogs()).contains("E/blueReader: null buffer passed to decodeblueReaderPacket");
    }

    // ===== Hibernate / reset tests ===============================================================

    @Test
    public void decodeBlueReaderPacket_notReadyFor_oneTime() {
        // :: Setup
        byte[] buffer = "not ready for".getBytes();

        // :: Act
        byte[] reply = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply).isNotNull();
        assertThat(new String(reply)).isEqualTo("h");
        assertThat(getLogs()).contains("E/blueReader: Found blueReader in a ugly State (1/3), send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");
    }

    /**
     * Found a bug in the code which increase hibernate count twice pr. invocation instead of once.
     * This test shows how it works. If This test fails because the bug has been fixed, rewrite the
     * test.
     */
    @Test
    public void decodeBlueReaderPacket_notReadyFor_threeTime() {
        // :: Setup
        byte[] buffer = "not ready for".getBytes();
        Pref.setBoolean("blueReader_suppressuglystatemsg", true);  // set to prevent notification as Context is not established

        // :: Act
        byte[] reply1 = blueReader.decodeblueReaderPacket(buffer, -1);
        byte[] reply2 = blueReader.decodeblueReaderPacket(buffer, -1);
        byte[] reply3 = blueReader.decodeblueReaderPacket(buffer, -1);

        // :: Verify
        assertThat(reply1).isNotNull();
        assertThat(reply2).isNotNull();
        assertThat(reply3).isNotNull();
        assertThat(new String(reply1)).isEqualTo("h");
        assertThat(new String(reply2)).isEqualTo("h");
        assertThat(new String(reply3)).isEqualTo("k");

        String log = getLogs();
        assertThat(log).contains("E/blueReader: Found blueReader in a ugly State (1/3), send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");
        assertThat(log).contains("E/blueReader: Found blueReader in a ugly State (2/3), send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");
        assertThat(log).contains("E/blueReader: Found blueReader in a ugly State (3/3), send hibernate to reset! If this does not help in the next 5 Minutes, then turn the bluereader manually off and on!");

        assertThat(log).contains("A/blueReader: Ugly state not resolveable. Bluereader will be shut down! Please restart it!");
    }

    // ===== Initialize Tests ======================================================================

    @Test
    public void initialize() {
        // :: Act
        ByteBuffer ackMessage = blueReader.initialize();

        // :: Verify
        assertThat(getLogs()).contains("I/blueReader: initialize blueReader!");
        assertThat(Pref.getInt("bridge_battery", -1)).isEqualTo(0);
        assertThat(PersistentStore.getDouble("blueReaderFirmwareValue")).isWithin(0.0d).of(0d);
        assertThat(new String(ackMessage.array())).isEqualTo("IDN");
    }

    // ===== isBlueReader ==========================================================================

    @Test
    public void isBlueReader_True() {
        // :: Setup
        ActiveBluetoothDevice mock = new ActiveBluetoothDevice();
        mock.name = "blueReader";
        mock.save();

        // :: Act
        boolean isBlueReader = blueReader.isblueReader();

        // :: Verify
        assertThat(isBlueReader).isTrue();
    }

    @Test
    public void isBlueReader_False() {
        // :: Setup
        ActiveBluetoothDevice mock = new ActiveBluetoothDevice();
        mock.name = "someOtherDevice";
        mock.save();

        // :: Act
        boolean isBlueReader = blueReader.isblueReader();

        // :: Verify
        assertThat(isBlueReader).isFalse();
    }

    @Test
    public void isBlueReader_NoDevice() {
        // :: Act
        boolean isBlueReader = blueReader.isblueReader();

        // :: Verify
        assertThat(isBlueReader).isFalse();
    }

    // ===== Process New Transmitter Data ==========================================================

    @Test
    public void processNewTransmitterData_NormalBg_FullBattery() {
        // Setup
        String message = "150000 4300";
        Sensor mockSensor = createMockSensor();

        addMockBgReading(150, 15, mockSensor);
        addMockBgReading(150, 10, mockSensor);
        addMockBgReading(150, 5, mockSensor);
        Calibration.initialCalibration(150, 150, RuntimeEnvironment.application.getApplicationContext());

        // Act
        byte[] bytes = blueReader.decodeblueReaderPacket(message.getBytes(), message.length());

        // Verify
        assertThat(bytes).isNull();
        assertThat(PersistentStore.getLong("blueReader_Full_Battery")).isEqualTo(4300);
        assertThat(PersistentStore.getString("bridge_battery_days")).isEqualTo("6.1");
        assertThat(Pref.getInt("bridge_battery", -1)).isEqualTo(100);
        assertThat(getLogs()).contains("BgReading.create: new BG reading at ");

        assertThat(Sensor.currentSensor().latest_battery_level).isEqualTo(100);

        TransmitterData lastTransmitterData = DexCollectionService.last_transmitter_Data;
        assertThat(lastTransmitterData.filtered_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.raw_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.sensor_battery_level).isEqualTo(4300);

        BgReading lastBgReading = BgReading.last();
        assertThat(lastBgReading.raw_data).isEqualTo(150.0d);
        assertThat(lastBgReading.filtered_data).isEqualTo(150.0d);
    }

    @Test
    public void processNewTransmitterData_NormalBg_SomeBattery() {
        // Setup
        String message = "150000 3900";
        Sensor mockSensor = createMockSensor();
        addMockBgReading(150, 15, mockSensor);
        addMockBgReading(150, 10, mockSensor);
        addMockBgReading(150, 5, mockSensor);
        Calibration.initialCalibration(150, 150, RuntimeEnvironment.application.getApplicationContext());

        // Act
        byte[] bytes = blueReader.decodeblueReaderPacket(message.getBytes(), message.length());

        // Verify
        assertThat(bytes).isNull();
        assertThat(PersistentStore.getLong("blueReader_Full_Battery")).isEqualTo(4100);
        assertThat(PersistentStore.getString("bridge_battery_days")).isEqualTo("5.5");
        assertThat(Pref.getInt("bridge_battery", -1)).isEqualTo(75);
        assertThat(getLogs()).contains("BgReading.create: new BG reading at ");

        assertThat(Sensor.currentSensor().latest_battery_level).isEqualTo(75);

        TransmitterData lastTransmitterData = DexCollectionService.last_transmitter_Data;
        assertThat(lastTransmitterData.filtered_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.raw_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.sensor_battery_level).isEqualTo(3900);

        BgReading lastBgReading = BgReading.last();
        assertThat(lastBgReading.raw_data).isEqualTo(150.0d);
        assertThat(lastBgReading.filtered_data).isEqualTo(150.0d);
    }

    @Test
    public void processNewTransmitterData_NormalBg_LowBattery() {
        // Setup
        Pref.setBoolean("blueReader_turn_off", false);
        String message = "150000 3300";
        Sensor mockSensor = createMockSensor();
        addMockBgReading(150, 15, mockSensor);
        addMockBgReading(150, 10, mockSensor);
        addMockBgReading(150, 5, mockSensor);
        Calibration.initialCalibration(150, 150, RuntimeEnvironment.application.getApplicationContext());

        // Act
        byte[] bytes = blueReader.decodeblueReaderPacket(message.getBytes(), message.length());

        // Verify
        assertThat(bytes).isNull();
        assertThat(PersistentStore.getLong("blueReader_Full_Battery")).isEqualTo(4100);
        assertThat(PersistentStore.getString("bridge_battery_days")).isEqualTo("0.1");
        assertThat(Pref.getInt("bridge_battery", -1)).isEqualTo(0);
        assertThat(getLogs()).contains("BgReading.create: new BG reading at ");

        assertThat(Sensor.currentSensor().latest_battery_level).isEqualTo(0);

        TransmitterData lastTransmitterData = DexCollectionService.last_transmitter_Data;
        assertThat(lastTransmitterData.filtered_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.raw_data).isEqualTo(150000.0);
        assertThat(lastTransmitterData.sensor_battery_level).isEqualTo(3300);

        BgReading lastBgReading = BgReading.last();
        assertThat(lastBgReading.raw_data).isEqualTo(150.0d);
        assertThat(lastBgReading.filtered_data).isEqualTo(150.0d);
    }

    @Test
    public void processNewTransmitterData_NormalBg_LowBatteryShutdown() {
        // Setup
        Pref.setBoolean("blueReader_turn_off", true);
        String message = "150000 3300";
        Sensor mockSensor = createMockSensor();
        addMockBgReading(150, 15, mockSensor);
        addMockBgReading(150, 10, mockSensor);
        addMockBgReading(150, 5, mockSensor);
        Calibration.initialCalibration(150, 150, RuntimeEnvironment.application.getApplicationContext());

        // Act
        byte[] reply = blueReader.decodeblueReaderPacket(message.getBytes(), message.length());

        // Verify
        assertThat(new String(reply)).isEqualTo("k");

    }

    /**
     * Loop through battery levels from high to low, verifying calculations along the way
     */
    @Test
    public void processNewTransmitterData_NormalBg_BatteryLifeTest() {
        // Setup
        Sensor mockSensor = createMockSensor();
        addMockBgReading(150, 15, mockSensor);
        addMockBgReading(150, 10, mockSensor);
        addMockBgReading(150, 5, mockSensor);
        Calibration.initialCalibration(150, 150, RuntimeEnvironment.application.getApplicationContext());

        int bgLevelBase = 150000;

        int minBatteryLevel = 3300;
        int maxBatteryLevel = 4500;
        int batteryRange = maxBatteryLevel - minBatteryLevel;

        for (int batteryLevel = maxBatteryLevel; batteryLevel >= minBatteryLevel; batteryLevel -= 50) {
            // Creating different bgLevels to avoid reading being rejected in TramsmitterData.
            String message = (bgLevelBase + batteryLevel) + " " + batteryLevel;

            // Act
            blueReader.decodeblueReaderPacket(message.getBytes(), message.length());

            // Verify
            assertWithMessage("batteryLevel: " + batteryLevel)
                    .that(PersistentStore.getLong("blueReader_Full_Battery"))
                    .isEqualTo(maxBatteryLevel);

            // Simplified calculation for days
            double days = 6.13d / (1d + Math.pow(batteryLevel / 3764d, -61d));
            if (batteryLevel < 3600) {
                days += 0.1;
            }

            String compare = BigDecimal.valueOf(days)
                    .setScale(1, RoundingMode.HALF_UP)
                    .toString();
            assertWithMessage("batteryLevel: " + batteryLevel)
                    .that(PersistentStore.getString("bridge_battery_days"))
                    .isEqualTo(compare);

            // Battery Percentage check
            int batteryPercentage = ((batteryLevel - minBatteryLevel) * 100 / batteryRange);
            assertWithMessage("batteryLevel: " + batteryLevel)
                    .that(Sensor.currentSensor().latest_battery_level)
                    .isEqualTo(batteryPercentage);
        }
    }

    // ===== Internal Helpers ======================================================================

    private String getLogs() {
        return new String(_out.toByteArray());
    }

    private Sensor createMockSensor() {
        Sensor mockSensor = new Sensor();
        mockSensor.started_at = System.currentTimeMillis() - (1000 * 60 * 20);
        mockSensor.uuid = UUID.randomUUID().toString();
        mockSensor.save();
        return mockSensor;
    }

    private void addMockBgReading(int raw_data, int minutes, Sensor sensor) {
        BgReading mockReading = new BgReading();
        mockReading.raw_data = raw_data;
        mockReading.calculated_value = raw_data;
        mockReading.timestamp = System.currentTimeMillis() - (1000 * 60 * minutes);
        mockReading.sensor = sensor;
        mockReading.age_adjusted_raw_value = raw_data + 0.1;
        mockReading.save();
    }
}
