package com.eveningoutpost.dexdrip.healthconnect;

import static com.google.common.truth.Truth.assertThat;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;

import androidx.health.connect.client.HealthConnectClient;

import com.eveningoutpost.dexdrip.BuildConfig;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link HealthGamut#openPermissionManager()}: which screens it tries to open and in
 * which order, depending on the Android version and on which screens Android refuses.
 * Android 15 and 16 refuse the permission screen of the app; that refusal is simulated here,
 * so SDK 34 covers the code path for Android 14 and later.
 */
public class HealthGamutPermissionManagerTest extends RobolectricTestWithConfig {

    private static final String MANAGE_PERMISSIONS = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS";
    private static final String MANAGE_DATA = "android.health.connect.action.MANAGE_HEALTH_DATA";

    /** Records every started intent and refuses the actions in {@link #refused}. */
    public static class RecordingActivity extends Activity {
        final List<Intent> started = new ArrayList<>();
        final Set<String> refused = new HashSet<>();
        boolean refuseAll;

        @Override
        public void startActivity(final Intent intent) {
            started.add(intent);
            if (refuseAll || refused.contains(intent.getAction())) {
                throw new SecurityException("Permission Denial: starting " + intent);
            }
        }

        List<String> actions() {
            final List<String> actions = new ArrayList<>();
            for (final Intent intent : started) actions.add(intent.getAction());
            return actions;
        }
    }

    @Test
    @Config(sdk = 34)
    public void whenAllowed_opensThePermissionScreenOfThisApp() {
        final RecordingActivity activity = open(new String[0], false);

        assertThat(activity.actions()).containsExactly(MANAGE_PERMISSIONS);
        assertThat(activity.started.get(0).getStringExtra(Intent.EXTRA_PACKAGE_NAME))
                .isEqualTo(BuildConfig.APPLICATION_ID);
        assertThat(ShadowToast.getTextOfLatestToast()).isNull();
    }

    @Test
    @Config(sdk = 34)
    public void whenThePermissionScreenIsRefused_opensHealthConnectWithHint() {
        final RecordingActivity activity = open(new String[]{MANAGE_PERMISSIONS}, false);

        assertThat(activity.actions()).hasSize(2);
        assertThat(activity.actions().get(0)).isEqualTo(MANAGE_PERMISSIONS);
        assertThat(activity.actions().get(1)).isAnyOf(MANAGE_DATA, HealthConnectClient.getHealthConnectSettingsAction());
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(string(R.string.health_connect_open_app_permissions_hint));
    }

    @Test
    @Config(sdk = 34)
    public void whenTheFirstFallbackIsRefusedToo_triesTheSecond() {
        final RecordingActivity activity = open(new String[]{MANAGE_PERMISSIONS, MANAGE_DATA}, false);

        assertThat(activity.actions()).contains(HealthConnectClient.getHealthConnectSettingsAction());
        assertThat(activity.actions().get(0)).isEqualTo(MANAGE_PERMISSIONS);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(string(R.string.health_connect_open_app_permissions_hint));
    }

    @Test
    @Config(sdk = 33)
    public void android13_opensTheHealthConnectSettingsAsBefore() {
        final RecordingActivity activity = open(new String[0], false);

        assertThat(activity.actions()).containsExactly("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS");
    }

    @Test
    @Config(sdk = 34)
    public void whenNothingCanBeOpened_showsAMessage() {
        final RecordingActivity activity = open(new String[0], true);

        assertThat(activity.actions()).hasSize(3);
        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(string(R.string.health_connect_cannot_open_settings));
    }

    private static RecordingActivity open(final String[] refused, final boolean refuseAll) {
        final RecordingActivity activity = Robolectric.buildActivity(RecordingActivity.class).create().get();
        activity.refused.addAll(Arrays.asList(refused));
        activity.refuseAll = refuseAll;
        new HealthGamut(activity).openPermissionManager();
        shadowOf(Looper.getMainLooper()).idle();
        return activity;
    }

    private static String string(final int id) {
        return RuntimeEnvironment.getApplication().getString(id);
    }
}
