package com.eveningoutpost.dexdrip.utilitymodels.pebble;

import androidx.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.getpebble.android.kit.util.PebbleDictionary;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the preference-backed accessors on {@link PebbleDisplayAbstract}.
 * <p>
 * getBatteryString() and getDexCollectionType() translate stored preferences into the values sent
 * to a Pebble watch. Context is injected through the real initDisplay() entry point rather than by
 * touching the protected field, so the tests assert the public contract and survive the
 * android.preference -> androidx PreferenceManager swap only if that contract is unchanged.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class PebbleDisplayAbstractTest extends RobolectricTestWithConfig {

    private TestPebbleDisplay display;

    // --- Setup ---

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        display = new TestPebbleDisplay();
        display.initDisplay(xdrip.getAppContext(), null, null); // public injection point
    }

    // --- getBatteryString ---

    /** An unset battery key reads as "0"; a stored level is rendered as its decimal string. */
    @Test
    public void getBatteryString_rendersStoredLevel() {
        // :: Act
        String unset = display.getBatteryString("batch2-unset-battery");

        // :: Verify
        assertThat(unset).isEqualTo("0");

        // :: Setup
        storeInt("batch2-battery", 42);

        // :: Act
        String stored = display.getBatteryString("batch2-battery");

        // :: Verify
        assertThat(stored).isEqualTo("42");
    }

    /** A different stored level yields a different string — the read is not a constant. */
    @Test
    public void getBatteryString_tracksChangesToStoredLevel() {
        // :: Setup
        storeInt("batch2-battery-tracking", 7);

        // :: Act
        String first = display.getBatteryString("batch2-battery-tracking");

        // :: Verify
        assertThat(first).isEqualTo("7");

        // :: Setup
        storeInt("batch2-battery-tracking", 93);

        // :: Act
        String second = display.getBatteryString("batch2-battery-tracking");

        // :: Verify
        assertThat(second).isEqualTo("93");
    }

    // --- getDexCollectionType ---

    /** With no stored collection method the documented DexbridgeWixel default applies. */
    @Test
    public void getDexCollectionType_defaultsToDexbridgeWixel() {
        // :: Setup
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().remove("dex_collection_method").commit();

        // :: Act
        DexCollectionType type = display.getDexCollectionType();

        // :: Verify
        assertThat(type).isEqualTo(DexCollectionType.DexbridgeWixel);
    }

    /** A stored collection method is resolved to its enum value rather than the default. */
    @Test
    public void getDexCollectionType_readsStoredMethod() {
        // :: Setup
        storeString("dex_collection_method", "Follower");

        // :: Act
        DexCollectionType type = display.getDexCollectionType();

        // :: Verify
        assertThat(type).isEqualTo(DexCollectionType.Follower);
    }

    /** isDexBridgeWixel follows the stored collection method in both directions. */
    @Test
    public void isDexBridgeWixel_followsStoredMethod() {
        // :: Setup
        storeString("dex_collection_method", "DexbridgeWixel");

        // :: Act
        boolean whenWixel = display.isDexBridgeWixel();

        // :: Verify
        assertThat(whenWixel).isTrue();

        // :: Setup
        storeString("dex_collection_method", "Follower");

        // :: Act
        boolean whenFollower = display.isDexBridgeWixel();

        // :: Verify
        assertThat(whenFollower).isFalse();
    }

    // --- Helpers ---

    private void storeInt(String key, int value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putInt(key, value).commit();
    }

    private void storeString(String key, String value) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString(key, value).commit();
    }

    /**
     * Minimal concrete subclass. PebbleDisplayAbstract already implements 5 of the 7
     * PebbleDisplayInterface methods; only these two remain abstract.
     */
    private static class TestPebbleDisplay extends PebbleDisplayAbstract {

        @Override
        public void startDeviceCommand() {
            // no-op: not under test
        }

        @Override
        public void receiveData(int transactionId, PebbleDictionary data) {
            // no-op: not under test
        }
    }
}
