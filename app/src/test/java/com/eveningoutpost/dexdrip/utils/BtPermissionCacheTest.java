package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.Shadows.shadowOf;

import android.Manifest;
import android.app.Application;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;

/**
 * Exercises the freshness / cache-hit-vs-live-fallback logic of {@link BtPermissionCache}.
 * The permission reads go through ContextCompat/Build so this needs Robolectric; the
 * behaviour under test is the cache gate itself.
 */
public class BtPermissionCacheTest extends RobolectricTestWithConfig {

    private Application app;

    @Before
    public void resetCache() throws Exception {
        app = RuntimeEnvironment.getApplication();
        // Force a cold cache so tests are independent of ordering (lastRefreshedMs is private).
        final Field f = BtPermissionCache.class.getDeclaredField("lastRefreshedMs");
        f.setAccessible(true);
        f.setLong(null, 0L);
    }

    @Test
    public void coldCacheFallsBackToLiveCheck() {
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        assertWithMessage("cold cache, permission denied -> live check false")
                .that(BtPermissionCache.isLocationGranted(app)).isFalse();

        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        assertWithMessage("cold cache, permission granted -> live check true")
                .that(BtPermissionCache.isLocationGranted(app)).isTrue();
    }

    @Test
    public void freshCacheServesCachedValueNotLive() {
        shadowOf(app).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        BtPermissionCache.refresh(app); // caches granted == true and marks fresh

        // Revoke the live permission; while the cache is still fresh the cached value must win.
        shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION);
        assertWithMessage("fresh cache serves cached grant, ignoring live revoke")
                .that(BtPermissionCache.isLocationGranted(app)).isTrue();
    }
}
