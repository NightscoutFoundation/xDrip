package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Notification;

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
        assertWithMessage("general channel name")
                .that(NotificationChannels.getString(NotificationChannels.GENERAL_CHANNEL))
                .isEqualTo("General");
    }

    /** The sensor-expiry channel resolves to a human-readable name, not its raw id. */
    @Test
    public void sensorExpiryChannelResolvesToAReadableName() {
        assertWithMessage("sensor expiry channel name")
                .that(NotificationChannels.getString(NotificationChannels.SENSOR_EXPIRY_CHANNEL))
                .isEqualTo("Sensor expiry");
    }

    /** An id with no mapping is returned unchanged. */
    @Test
    public void unknownChannelIdIsReturnedUnchanged() {
        assertWithMessage("unknown id passthrough")
                .that(NotificationChannels.getString("no-such-channel-id"))
                .isEqualTo("no-such-channel-id");
    }
}