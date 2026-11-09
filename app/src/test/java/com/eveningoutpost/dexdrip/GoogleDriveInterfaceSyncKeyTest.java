package com.eveningoutpost.dexdrip;

import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.utils.CipherUtils;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

/**
 * Behavioural tests for the sync identity {@link GoogleDriveInterface} derives from the custom sync key in the
 * settings.
 * <p>
 * Phones that share a sync key share an identity, which is how followers find the master's data. Starting the
 * interface binds it to the current preference store.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class GoogleDriveInterfaceSyncKeyTest extends RobolectricTestWithConfig {

    private static final String KEY = "custom_sync_key";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application); // force re-bind to current Robolectric app
        store().edit().clear().commit();
        new GoogleDriveInterface().onCreate(null); // binds the interface to this test's preference store
    }

    // ===== Sync identity =============================================================================================

    /** The identity is derived from the stored sync key. */
    @Test
    public void identityComesFromTheStoredSyncKey() {
        // :: Setup
        final String key = "abcdef0123456789abcd";
        store().edit().putString(KEY, key).commit();

        // :: Act
        final String identity = GoogleDriveInterface.getDriveIdentityString();

        // :: Verify
        assertThat(identity).isEqualTo(CipherUtils.getSHA256(key).substring(0, 32));
    }

    /** Without a stored key one is made up and kept, and the identity is derived from it. */
    @Test
    public void aMissingSyncKeyIsCreatedAndKept() {
        // :: Act
        final String identity = GoogleDriveInterface.getDriveIdentityString();

        // :: Verify
        final String created = store().getString(KEY, "");
        assertThat(created.length()).isGreaterThan(16);
        assertThat(identity).isEqualTo(CipherUtils.getSHA256(created).substring(0, 32));
    }

    /** A stored key that is too short gives no identity. */
    @Test
    public void aShortSyncKeyGivesNoIdentity() {
        // :: Setup
        store().edit().putString(KEY, "short").commit();

        // :: Act
        final String identity = GoogleDriveInterface.getDriveIdentityString();

        // :: Verify
        assertThat(identity).isNull();
    }

    // =================================================================================================================

    private static SharedPreferences store() {
        return androidx.preference.PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
    }
}
