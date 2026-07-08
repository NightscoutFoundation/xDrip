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

    // ===== isLoopbackUrl =========================================================================

    @Test
    public void isLoopbackUrl_trueForIpv4Loopback() {
        assertThat(NightscoutFollowService.isLoopbackUrl("http://127.0.0.1:17580/")).isTrue();
    }

    @Test
    public void isLoopbackUrl_trueForLocalhostAndIpv6() {
        assertThat(NightscoutFollowService.isLoopbackUrl("http://localhost:1979/")).isTrue();
        assertThat(NightscoutFollowService.isLoopbackUrl("http://[::1]:1979/")).isTrue();
    }

    @Test
    public void isLoopbackUrl_falseForRemoteHostAndBlank() {
        assertThat(NightscoutFollowService.isLoopbackUrl("https://my.ns.example.com/")).isFalse();
        assertThat(NightscoutFollowService.isLoopbackUrl("")).isFalse();
        assertThat(NightscoutFollowService.isLoopbackUrl(null)).isFalse();
    }

    // ===== shouldPoll ============================================================================

    @Test
    public void shouldPoll_trueForLoopback_evenWithNoNetwork() {
        // :: Setup — airplane mode: CM present, no active network
        final ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getActiveNetwork()).thenReturn(null);

        // :: Verify — loopback still polls
        assertThat(NightscoutFollowService.shouldPoll(cm, "http://127.0.0.1:17580/")).isTrue();
    }

    @Test
    public void shouldPoll_falseForRemote_whenNoNetwork() {
        // :: Setup — no active network
        final ConnectivityManager cm = mock(ConnectivityManager.class);
        when(cm.getActiveNetwork()).thenReturn(null);

        // :: Verify — remote host is skipped when offline
        assertThat(NightscoutFollowService.shouldPoll(cm, "https://my.ns.example.com/")).isFalse();
    }
}
