package com.eveningoutpost.dexdrip;

import android.content.Intent;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.function.Supplier;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for glucose entries broadcast to {@link NSEmulatorReceiver}.
 * <p>
 * The receiver handles each broadcast on its own thread, so each test waits until the reading has been saved.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class NSEmulatorReceiverEntriesTest extends RobolectricTestWithConfig {

    private static final long ENTRY_TIME = 1_700_000_000_000L;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        DexCollectionType.setDexCollectionType(DexCollectionType.NSEmulator);
        Sensor.create(1000);
        BgReading.deleteALL();
    }

    @After
    public void tearDown() {
        BgReading.deleteALL();
        Sensor.stopSensor();
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
    }

    // ===== Entries ===================================================================================================

    /** An sgv entry is saved as a reading with its value and time. */
    @Test
    public void sgvEntryIsSavedAsReading() {
        // :: Act
        receiveEntry("{\"type\":\"sgv\",\"date\":" + ENTRY_TIME + ",\"sgv\":123,\"direction\":\"FortyFiveDown\"}");

        // :: Verify
        BgReading reading = waitFor(BgReading::lastNoSenssor);
        assertThat(reading).isNotNull();
        assertThat(reading.timestamp).isEqualTo(ENTRY_TIME);
        assertThat(reading.calculated_value).isEqualTo(123d);
        assertThat(reading.slopeName()).isEqualTo("FortyFiveDown");
    }

    /** An sgv entry without a direction is saved as flat. */
    @Test
    public void sgvEntryWithoutDirectionIsSavedFlat() {
        // :: Act
        receiveEntry("{\"type\":\"sgv\",\"date\":" + ENTRY_TIME + ",\"sgv\":123}");

        // :: Verify
        BgReading reading = waitFor(BgReading::lastNoSenssor);
        assertThat(reading).isNotNull();
        assertThat(reading.slopeName()).isEqualTo("Flat");
    }

    // ===== Helpers ===================================================================================================

    private void receiveEntry(String entryJson) {
        Intent intent = new Intent(Intents.XDRIP_PLUS_NS_EMULATOR)
                .putExtra("collection", "entries")
                .putExtra("data", "[" + entryJson + "]");
        new NSEmulatorReceiver().onReceive(xdrip.getAppContext(), intent);
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
