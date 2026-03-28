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
 * Tests for network availability detection in the NSFollow service.
 * Verifies {@link NightscoutFollowService#isNetworkAvailable} with null,
 * no-network, and active-network connectivity manager states.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowServiceNetworkTest extends RobolectricTestWithConfig {

    // ===== isNetworkAvailable ====================================================================

    @Test
    public void isNetworkAvailable_returnsFalse_whenConnectivityManagerIsNull() {
        assertThat(NightscoutFollowService.isNetworkAvailable(null)).isFalse();
    }

    @Test
    public void isNetworkAvailable_returnsFalse_whenNoActiveNetwork() {
        // :: Setup — CM present but getActiveNetwork() returns null (airplane mode, etc.)
        final ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getActiveNetwork()).thenReturn(null);

        // :: Verify
        assertThat(NightscoutFollowService.isNetworkAvailable(cm)).isFalse();
    }

    @Test
    public void isNetworkAvailable_returnsTrue_whenNetworkPresent() {
        // :: Setup — Robolectric provides a default active network via the application context
        final ConnectivityManager cm = (ConnectivityManager)
                RuntimeEnvironment.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);

        // :: Verify
        assertThat(NightscoutFollowService.isNetworkAvailable(cm)).isTrue();
    }
}
