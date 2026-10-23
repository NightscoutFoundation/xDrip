package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.services.DexCollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.pebble.PebbleWatchSync;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowLog;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * Behavioural tests for the "sync to wear" settings that {@link CollectionServiceStarter} reads when it starts the
 * phone's collector.
 * <p>
 * The restart runs on background tasks, so each test waits until the start has logged its last line. By then every
 * service it decided to start has been started.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class CollectionServiceStarterWearSyncTest extends RobolectricTestWithConfig {

    private static final String PEBBLE_TYPE = "broadcast_to_pebble_type";
    private static final String COLLECTOR = DexCollectionType.BluetoothWixel.toString();

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        defaultPrefs().edit().clear().commit();
        // PebbleWatchSync only starts if the flags are read from this store
        defaultPrefs().edit().putBoolean("broadcast_to_pebble", true).commit();
        Pref.setString(PEBBLE_TYPE, "2"); // read through Pref by PebbleUtil
        DexCollectionType.setDexCollectionType(DexCollectionType.BluetoothWixel);
        JoH.clearRatelimit("collection-queue-restart");
        shadowOf(RuntimeEnvironment.application).clearStartedServices();
        ShadowLog.clear();
    }

    @After
    public void tearDown() {
        defaultPrefs().edit().clear().commit();
        Pref.removeItem(PEBBLE_TYPE);
        Pref.removeItem(DexCollectionType.DEX_COLLECTION_METHOD);
        JoH.clearRatelimit("collection-queue-restart");
        shadowOf(RuntimeEnvironment.application).clearStartedServices();
    }

    // ===== Wear sync =================================================================================================

    /** With "sync to wear" off, a restart starts the phone's own collector and not the watch updater. */
    @Test
    public void wearSyncOffStartsPhoneCollectorOnly() {
        // :: Act
        List<String> started = restartAndWait();

        // :: Verify
        assertThat(started).contains(DexCollectionService.class.getName());
        assertThat(started).doesNotContain(WatchUpdaterService.class.getName());
    }

    /** With "sync to wear" on, a restart starts the watch updater and still starts the phone's collector. */
    @Test
    public void wearSyncStartsWatchUpdaterAndPhoneCollector() {
        // :: Setup
        defaultPrefs().edit().putBoolean("wear_sync", true).commit();

        // :: Act
        List<String> started = restartAndWait();

        // :: Verify
        assertThat(started).contains(WatchUpdaterService.class.getName());
        assertThat(started).contains(DexCollectionService.class.getName());
    }

    /** When the watch is forced to be the collector, a restart leaves the phone's collector off. */
    @Test
    public void forcedWearCollectorKeepsPhoneCollectorOff() {
        // :: Setup
        defaultPrefs().edit()
                .putBoolean("wear_sync", true)
                .putBoolean("enable_wearG5", true)
                .putBoolean("force_wearG5", true)
                .commit();

        // :: Act
        List<String> started = restartAndWait();

        // :: Verify
        assertThat(started).contains(WatchUpdaterService.class.getName());
        assertThat(started).doesNotContain(DexCollectionService.class.getName());
    }

    // =================================================================================================================

    private static SharedPreferences defaultPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }

    /** Restarts collection and waits up to 30 s for the start to finish, then returns the started class names. */
    private static List<String> restartAndWait() {
        CollectionServiceStarter.restartCollectionService(xdrip.getAppContext());
        for (int i = 0; i < 300 && !startFinished(); i++) {
            JoH.threadSleep(100);
        }
        assertThat(startFinished()).isTrue();
        final List<String> started = startedServices();
        assertThat(started).contains(PebbleWatchSync.class.getName()); // read from the same store as the wear flags
        return started;
    }

    /** True once this test's start has logged its first line and, after it, its last line. */
    private static boolean startFinished() {
        final List<String> lines = ShadowLog.getLogsForTag("CollectionServiceStarter").stream()
                .map(l -> l.msg)
                .collect(Collectors.toList());
        final int first = lines.indexOf("start called: " + COLLECTOR);
        return first >= 0 && lines.subList(first + 1, lines.size()).contains(COLLECTOR);
    }

    private static List<String> startedServices() {
        return shadowOf(RuntimeEnvironment.application).getAllStartedServices().stream()
                .map(Intent::getComponent)
                .filter(c -> c != null)
                .map(c -> c.getClassName())
                .collect(Collectors.toList());
    }
}
