package com.eveningoutpost.dexdrip.models;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

/**
 * The public notification entry point must deliver, and must never post to a channel that
 * does not exist — the failure mode behind the sensor-expiry drop.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class JoHShowNotificationTest extends RobolectricTestWithConfig {

    private NotificationManager notificationManager() {
        return (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /** Assert exactly one notification was delivered and return the one posted under {@code id}. */
    private Notification delivered(int id) {
        assertWithMessage("exactly one notification delivered")
                .that(shadowOf(notificationManager()).size())
                .isEqualTo(1);
        final Notification n = shadowOf(notificationManager()).getNotification(id);
        assertWithMessage("a notification was posted under id " + id).that(n).isNotNull();
        return n;
    }

    // ---- null channel: the general fallback path ----------------------------

    /** A caller that supplies no channel still gets a notification, on a channel that exists. */
    @Test
    public void nullChannelNotificationIsDeliveredOnAnExistingChannel() {
        // Act
        JoH.showNotification("title", "body", null, 4242, null, false, false, null, null, null, false);

        // Verify
        final Notification n = delivered(4242);
        assertWithMessage("channel it landed on exists")
                .that(notificationManager().getNotificationChannel(n.getChannelId()))
                .isNotNull();
    }

    // ---- named channel: the sensor-expiry shape -----------------------------

    /**
     * Mirrors the real sensor-expiry call: delivered, on an existing channel, and the channel
     * carries a human-readable name rather than the raw id.
     */
    @Test
    public void sensorExpiryChannelNotificationIsDeliveredWithAReadableName() {
        // Act
        JoH.showNotification("Sensor expiring", "soon", null, 4243,
                NotificationChannels.SENSOR_EXPIRY_CHANNEL, true, true, null, null, null, true);

        // Verify
        final Notification n = delivered(4243);
        assertWithMessage("channel exists")
                .that(notificationManager().getNotificationChannel(n.getChannelId()))
                .isNotNull();
        assertWithMessage("channel name is readable, not the raw id")
                .that(notificationManager().getNotificationChannel(n.getChannelId()).getName().toString())
                .startsWith("Sensor expiry");
    }

    // ---- shorter overloads still deliver -----------------------------------

    /**
     * A shorter overload that omits the channel id still delivers, on a channel that exists —
     * i.e. the null → General path is reachable through the whole public surface, not just the
     * widest overload.
     */
    @Test
    public void shorterOverloadWithoutChannelIsDeliveredOnAnExistingChannel() {
        // Act
        JoH.showNotification("title", "body", null, 4244, false, false, false);

        // Verify
        final Notification n = delivered(4244);
        assertWithMessage("channel it landed on exists")
                .that(notificationManager().getNotificationChannel(n.getChannelId()))
                .isNotNull();
    }
}
