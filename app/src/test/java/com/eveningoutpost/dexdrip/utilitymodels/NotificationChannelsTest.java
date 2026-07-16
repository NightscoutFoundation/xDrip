package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

public class NotificationChannelsTest extends RobolectricTestWithConfig {
    private static final long[] pattern = {123, 456, 789};

    @Test
    public void getNotificationFromInsideBuilderTest() {
        val builder = new NotificationCompat.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), (String)null);
        builder.setVibrate(pattern);
        val mNotification = NotificationChannels.getNotificationFromInsideBuilder(builder);
        assertWithMessage("got builder by reflection 1").that(mNotification).isNotNull();
        assertWithMessage("got builder by reflection 2").that(mNotification.getClass()).isEqualTo(Notification.class);
        assertWithMessage("got builder by reflection 3").that(mNotification.vibrate).isEqualTo(pattern);
    }

    // ---- name resolution ----------------------------------------------------

    /** The general fallback channel resolves to a human-readable name, not its raw id. */
    @Test
    public void generalChannelResolvesToAReadableName() {
        // Act and verify
        assertWithMessage("general channel name")
                .that(NotificationChannels.getString(NotificationChannels.GENERAL_CHANNEL))
                .isEqualTo("General");
    }

    /** The sensor-expiry channel resolves to a human-readable name, not its raw id. */
    @Test
    public void sensorExpiryChannelResolvesToAReadableName() {
        // Act and verify
        assertWithMessage("sensor expiry channel name")
                .that(NotificationChannels.getString(NotificationChannels.SENSOR_EXPIRY_CHANNEL))
                .isEqualTo("Sensor expiry");
    }

    /** An id with no mapping is returned unchanged. */
    @Test
    public void unknownChannelIdIsReturnedUnchanged() {
        // Act and verify
        assertWithMessage("unknown id passthrough")
                .that(NotificationChannels.getString("no-such-channel-id"))
                .isEqualTo("no-such-channel-id");
    }

    // ---- customisation property (why the per-hash channels exist) -----------

    private NotificationManager nm() {
        return (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private NotificationCompat.Builder builder() {
        return new NotificationCompat.Builder(
                RuntimeEnvironment.getApplication().getApplicationContext(),
                NotificationChannels.GENERAL_CHANNEL);
    }

    /**
     * Notifications that differ in vibration must land on different channels, so a user can
     * customise them independently. This is the behaviour the dynamic-hash channels exist for
     * — asserted without reference to the hash encoding itself.
     */
    @Test
    public void differentVibrationYieldsDifferentChannels() {
        // Act
        final NotificationChannel vibrating = NotificationChannels.getChan(builder().setVibrate(pattern));
        final NotificationChannel silent = NotificationChannels.getChan(builder());

        // Verify
        assertWithMessage("distinct channels for distinct settings")
                .that(vibrating.getId())
                .isNotEqualTo(silent.getId());
    }

    /** Identical settings must reuse one channel, not spawn duplicates. */
    @Test
    public void identicalSettingsShareOneChannel() {
        // Act
        final NotificationChannel first = NotificationChannels.getChan(builder().setVibrate(pattern));
        final NotificationChannel second = NotificationChannels.getChan(builder().setVibrate(pattern));

        // Verify
        assertWithMessage("shared channel for identical settings")
                .that(first.getId())
                .isEqualTo(second.getId());
    }

    // ---- setup methods create named channels --------------------------------

    /** Each setup method registers its channel under the intended readable name. */
    @Test
    public void setupMethodsCreateNamedChannels() {
        // Act
        NotificationChannels.setupGeneralChannel();
        NotificationChannels.setupSensorExpiryChannel();
        NotificationChannels.setupTestChannel();

        // Verify
        assertWithMessage("general").that(nameOf(NotificationChannels.GENERAL_CHANNEL)).isEqualTo("General");
        assertWithMessage("sensor expiry").that(nameOf(NotificationChannels.SENSOR_EXPIRY_CHANNEL)).isEqualTo("Sensor expiry");
        assertWithMessage("icon test").that(nameOf(NotificationChannels.ICON_TEST_CHANNEL)).isEqualTo("xDrip Icon Test");
    }

    private String nameOf(String channelId) {
        final NotificationChannel c = nm().getNotificationChannel(channelId);
        assertWithMessage("channel " + channelId + " exists").that(c).isNotNull();
        return c.getName().toString();
    }

    // ---- ongoing channel is unobtrusive -------------------------------------

    /** The ongoing/foreground channel must be silent and must not vibrate. */
    @Test
    public void ongoingChannelIsSilentAndDoesNotVibrate() {
        // Act
        final NotificationChannel ongoing = NotificationChannels.getChan(
                new android.app.Notification.Builder(RuntimeEnvironment.getApplication().getApplicationContext()));

        // Verify
        assertWithMessage("ongoing id").that(ongoing.getId()).isEqualTo(NotificationChannels.ONGOING_CHANNEL);
        assertWithMessage("ongoing is silent").that(ongoing.getSound()).isNull();
        assertWithMessage("ongoing does not vibrate").that(ongoing.shouldVibrate()).isFalse();
    }
}