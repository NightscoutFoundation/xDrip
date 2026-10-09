package com.eveningoutpost.dexdrip;

import androidx.preference.PreferenceManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the collection-method preference {@link Home#get_follower()} reads.
 * <p>
 * {@code get_follower()} caches its answer in a static for the life of the process. Every test here
 * resets that cache first, and {@link #tearDown()} leaves it cached as "not a follower", so a stored
 * {@code Follower} never leaks into another test.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class HomeFollowerPreferenceTest extends RobolectricTestWithConfig {

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        Home.resetFollowerCacheForTests();
    }

    /**
     * Clears the store, then caches "not a follower" from it. Later tests in the shared JVM then see
     * the cached answer they would have seen without this class, instead of re-reading a store that
     * other tests may have left a collection method in.
     */
    @After
    public void tearDown() {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext()).edit().clear().commit();
        Home.resetFollowerCacheForTests();
        Home.get_follower();
    }

    // ===== Collection method decides follower mode ===================================================================

    /** With the collection method stored as Follower, the app runs as a follower. */
    @Test
    public void isFollowerWhenCollectionMethodIsFollower() {
        // :: Setup
        storeCollectionMethod("Follower");

        // :: Act
        boolean follower = Home.get_follower();

        // :: Verify
        assertThat(follower).isTrue();
    }

    /** Any other collection method means the app is not a follower. */
    @Test
    public void isNotFollowerWhenCollectionMethodIsSomethingElse() {
        // :: Setup
        storeCollectionMethod("BluetoothWixel");

        // :: Act
        boolean follower = Home.get_follower();

        // :: Verify
        assertThat(follower).isFalse();
    }

    /** With nothing stored the app is not a follower. */
    @Test
    public void isNotFollowerWhenNothingIsStored() {
        // :: Act
        boolean follower = Home.get_follower();

        // :: Verify
        assertThat(follower).isFalse();
    }

    // ===== Helpers ===================================================================================================

    private void storeCollectionMethod(String method) {
        PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext())
                .edit().putString("dex_collection_method", method).commit();
    }
}
