package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowPowerManager;

import java.util.function.BooleanSupplier;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for broadcasts from LibreAlarm reaching {@link LibreAlarmReceiver}.
 * <p>
 * The receiver handles each broadcast on its own thread, so each test waits until the effect it expects
 * has happened.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class LibreAlarmReceiverBroadcastTest extends RobolectricTestWithConfig {

    private static final String BRIDGE_BATTERY = "bridge_battery";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        Pref.removeItem(BRIDGE_BATTERY);
    }

    @After
    public void tearDown() {
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        Pref.removeItem(BRIDGE_BATTERY);
    }

    // ===== Collector selection =======================================================================================

    /** A broadcast from LibreAlarm while a non-Libre collector is selected switches the collector to LibreAlarm. */
    @Test
    public void switchesToLibreAlarmWhenCollectorHasNoLibre() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.BluetoothWixel);

        // :: Act
        receive(new Intent(Intents.LIBRE_ALARM_TO_XDRIP_PLUS));

        // :: Verify
        assertThat(waitUntil(() -> DexCollectionType.getDexCollectionType() == DexCollectionType.LibreAlarm))
                .isTrue();
    }

    /**
     * A broadcast while a Libre collector is already selected keeps that collector. The bridge battery level in
     * the same broadcast is stored after the collector check, so it shows the broadcast has been handled.
     */
    @Test
    public void keepsLibreCollectorAlreadySelected() {
        // :: Setup
        DexCollectionType.setDexCollectionType(DexCollectionType.LimiTTer);

        // :: Act
        receive(new Intent(Intents.LIBRE_ALARM_TO_XDRIP_PLUS).putExtra(BRIDGE_BATTERY, 42));

        // :: Verify
        assertThat(waitUntil(() -> Pref.getInt(BRIDGE_BATTERY, 0) == 42)).isTrue();
        assertThat(waitUntil(LibreAlarmReceiverBroadcastTest::receiverFinished)).isTrue();
        assertThat(DexCollectionType.getDexCollectionType()).isEqualTo(DexCollectionType.LimiTTer);
    }

    // ===== Helpers ===================================================================================================

    private void receive(Intent intent) {
        new LibreAlarmReceiver().onReceive(xdrip.getAppContext(), intent);
    }

    /**
     * True once the receiver has released its wake lock, which is the last thing it does with a broadcast. Waiting
     * for this keeps the rest of its work (here a failed parse of the missing reading data) inside the test.
     */
    private static boolean receiverFinished() {
        PowerManager.WakeLock wakeLock = ShadowPowerManager.getLatestWakeLock();
        return wakeLock != null && !wakeLock.isHeld();
    }

    /** Polls for up to ten seconds, since the receiver works on a thread the test can't join. */
    private static boolean waitUntil(BooleanSupplier condition) {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) return true;
            JoH.threadSleep(100);
        }
        return condition.getAsBoolean();
    }
}
