package com.eveningoutpost.dexdrip;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.eveningoutpost.dexdrip.utilitymodels.Intents;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.StatusItem;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowApplication;

import java.util.function.BooleanSupplier;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for {@link LibreReceiver#registerForImplicitBroadcasts(Context)}.
 * <p>
 * Since targetSdk 26, Android no longer delivers an implicit broadcast (one that does not name xDrip) to a receiver
 * declared only in the manifest. Apps that forward LibreLink readings without naming xDrip therefore need a receiver
 * registered at runtime. The receiver handles each broadcast on its own thread, so the tests wait for the effect
 * they expect: the connection state shown on the status page.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class LibreReceiverRuntimeRegistrationTest extends RobolectricTestWithConfig {

    private static final String CONNECTION_STATE = "connectionState";
    private static final String BLUETOOTH_LINK = "Bluetooth Link";
    private static final String FOUND_DEVICE = "com.librelink.app.ThirdPartyIntegration.FOUND_DEVICE";

    private BroadcastReceiver receiver;

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        DexCollectionType.setDexCollectionType(DexCollectionType.LibreReceiver);
    }

    @After
    public void tearDown() {
        if (receiver != null) {
            RuntimeEnvironment.getApplication().unregisterReceiver(receiver);
        }
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
    }

    // ===== Implicit broadcasts =======================================================================================

    /**
     * A LibreLink broadcast that does not name xDrip is handled by the runtime receiver. Robolectric delivers implicit
     * broadcasts to manifest receivers regardless of targetSdk, so the platform restriction itself is checked on an
     * emulator, not here.
     */
    @Test
    public void implicitBroadcastIsHandled() {
        // :: Setup
        receiver = LibreReceiver.registerForImplicitBroadcasts(RuntimeEnvironment.getApplication());
        final String state = "implicit-" + System.nanoTime();

        // :: Act
        receiver.onReceive(RuntimeEnvironment.getApplication(), connectionIntent(state));

        // :: Verify
        assertThat(waitUntil(() -> state.equals(bluetoothLink()))).isTrue();
    }

    /** The runtime receiver listens for the same five LibreLink actions as the manifest receiver. */
    @Test
    public void listensForAllFiveLibreLinkActions() {
        // :: Act
        receiver = LibreReceiver.registerForImplicitBroadcasts(RuntimeEnvironment.getApplication());

        // :: Verify
        final ShadowApplication.Wrapper registration = shadowOf(RuntimeEnvironment.getApplication())
                .getRegisteredReceivers().stream()
                .filter(wrapper -> wrapper.getBroadcastReceiver() == receiver)
                .findFirst()
                .orElseThrow(AssertionError::new);
        assertThat(registration.getIntentFilter().countActions()).isEqualTo(5);
        assertThat(registration.getIntentFilter().hasAction(Intents.LIBRE2_BG)).isTrue();
        assertThat(registration.getIntentFilter().hasAction(Intents.LIBRE2_ACTIVATION)).isTrue();
        assertThat(registration.getIntentFilter().hasAction(Intents.LIBRE2_SCAN)).isTrue();
        assertThat(registration.getIntentFilter().hasAction(Intents.LIBRE2_CONNECTION)).isTrue();
        assertThat(registration.getIntentFilter().hasAction(FOUND_DEVICE)).isTrue();
    }

    // ===== Explicit broadcasts =======================================================================================

    /**
     * A broadcast that names xDrip is left to the manifest receiver, so the runtime receiver ignores it and it is not
     * handled twice.
     */
    @Test
    public void explicitBroadcastIsIgnored() {
        // :: Setup
        receiver = LibreReceiver.registerForImplicitBroadcasts(RuntimeEnvironment.getApplication());
        final String state = "explicit-" + System.nanoTime();
        final Intent explicit = connectionIntent(state)
                .setComponent(new ComponentName(RuntimeEnvironment.getApplication(), LibreReceiver.class));

        // :: Act
        receiver.onReceive(RuntimeEnvironment.getApplication(), explicit);

        // :: Verify
        assertThat(waitUntil(() -> state.equals(bluetoothLink()), 1000)).isFalse();
    }

    /**
     * A broadcast addressed to xDrip's package reaches the manifest receiver despite the targetSdk 26 restriction, so
     * the runtime receiver ignores it and it is not handled twice.
     */
    @Test
    public void packageTargetedBroadcastIsIgnored() {
        // :: Setup
        receiver = LibreReceiver.registerForImplicitBroadcasts(RuntimeEnvironment.getApplication());
        final String state = "package-" + System.nanoTime();
        final Intent packageTargeted = connectionIntent(state)
                .setPackage(RuntimeEnvironment.getApplication().getPackageName());

        // :: Act
        receiver.onReceive(RuntimeEnvironment.getApplication(), packageTargeted);

        // :: Verify
        assertThat(waitUntil(() -> state.equals(bluetoothLink()), 1000)).isFalse();
    }

    // ===== Helpers ===================================================================================================

    private static Intent connectionIntent(final String state) {
        return new Intent(Intents.LIBRE2_CONNECTION).putExtra(CONNECTION_STATE, state);
    }

    /** The connection state as shown on the status page, or null when none is shown. */
    private static String bluetoothLink() {
        for (final StatusItem item : LibreReceiver.megaStatus()) {
            if (BLUETOOTH_LINK.equals(item.name)) return item.value;
        }
        return null;
    }

    private static boolean waitUntil(final BooleanSupplier condition) {
        return waitUntil(condition, 5000);
    }

    private static boolean waitUntil(final BooleanSupplier condition, final long timeoutMs) {
        final long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return true;
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }
}
