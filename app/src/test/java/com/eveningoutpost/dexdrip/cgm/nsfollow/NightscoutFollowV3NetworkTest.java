package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

/**
 * Tests for network availability detection used by the NSFollow service.
 * <p>
 * Verifies {@link NightscoutFollowV3#isNetworkAvailable} with null, no-network,
 * and active-network connectivity manager states.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3NetworkTest extends RobolectricTestWithConfig {

    // ===== isNetworkAvailable ====================================================================

    @Test
    public void isNetworkAvailable_returnsFalse_whenConnectivityManagerIsNull() {
        assertThat(NightscoutFollowV3.isNetworkAvailable(null)).isFalse();
    }

    @Test
    public void isNetworkAvailable_returnsFalse_whenNoActiveNetwork() {
        // :: Setup — CM present but getActiveNetwork() returns null (no data connection)
        final ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getActiveNetwork()).thenReturn(null);

        // :: Verify
        assertThat(NightscoutFollowV3.isNetworkAvailable(cm)).isFalse();
    }

    @Test
    public void isNetworkAvailable_returnsTrue_whenNetworkPresent() {
        // :: Setup — Robolectric provides a default active network via the application context
        final ConnectivityManager cm = (ConnectivityManager)
                RuntimeEnvironment.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

        // :: Verify
        assertThat(NightscoutFollowV3.isNetworkAvailable(cm)).isTrue();
    }
}
