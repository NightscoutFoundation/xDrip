package com.eveningoutpost.dexdrip.receivers.aidex;

import android.content.Intent;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.function.Supplier;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for glucose estimates broadcast by the Aidex app to {@link AidexReceiver}.
 * <p>
 * The receiver handles each broadcast on its own thread, so each test waits until the reading has been saved.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class AidexReceiverBgEstimateTest extends RobolectricTestWithConfig {

    private static final String SENSOR_ID = "batch8-aidex";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        DexCollectionType.setDexCollectionType(DexCollectionType.AidexReceiver);
        Sensor.stopSensor();
        BgReading.deleteALL();
    }

    @After
    public void tearDown() {
        BgReading.deleteALL();
        Sensor.stopSensor();
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
    }

    // ===== Units =====================================================================================================

    /** An estimate in mg/dL is saved as is. */
    @Test
    public void mgdlEstimateIsSavedAsIs() {
        // :: Setup
        long time = JoH.tsl();

        // :: Act
        receiveEstimate(AidexBroadcastIntents.UNIT_MG_DL, 123d, time);

        // :: Verify
        BgReading reading = waitFor(BgReading::lastNoSenssor);
        assertThat(reading).isNotNull();
        assertThat(reading.timestamp).isEqualTo(time);
        assertThat(reading.calculated_value).isEqualTo(123d);
    }

    /** An estimate in mmol/L is converted to whole mg/dL before it is saved. */
    @Test
    public void mmolEstimateIsConvertedToMgdl() {
        // :: Setup
        long time = JoH.tsl();

        // :: Act
        receiveEstimate(AidexBroadcastIntents.UNIT_MMOL_L, 6.0d, time);

        // :: Verify
        BgReading reading = waitFor(BgReading::lastNoSenssor);
        assertThat(reading).isNotNull();
        assertThat(reading.calculated_value).isEqualTo(108d); // 6.0 mmol/L = 108.09 mg/dL, truncated
    }

    // ===== Helpers ===================================================================================================

    private void receiveEstimate(String unit, double value, long time) {
        Intent intent = new Intent(AidexBroadcastIntents.ACTION_NEW_BG_ESTIMATE)
                .putExtra(AidexBroadcastIntents.AIDEX_SENSOR_ID, SENSOR_ID)
                .putExtra(AidexBroadcastIntents.AIDEX_TIMESTAMP, time)
                .putExtra(AidexBroadcastIntents.AIDEX_BG_TYPE, unit)
                .putExtra(AidexBroadcastIntents.AIDEX_BG_VALUE, value);
        new AidexReceiver().onReceive(xdrip.getAppContext(), intent);
    }

    /** Polls for up to ten seconds, since the receiver works on a thread the test can't join. */
    private static <T> T waitFor(Supplier<T> probe) {
        for (int i = 0; i < 100; i++) {
            T value = probe.get();
            if (value != null) return value;
            JoH.threadSleep(100);
        }
        return probe.get();
    }
}
