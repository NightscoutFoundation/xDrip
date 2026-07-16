package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

/**
 * Behaviour of the single funnel every notification passes through before delivery.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class XdripNotificationCompatTest extends RobolectricTestWithConfig {

    private NotificationManager notificationManager() {
        return (NotificationManager) RuntimeEnvironment.getApplication()
                .getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private NotificationCompat.Builder builderWithChannel(String channelId) {
        return new NotificationCompat.Builder(
                RuntimeEnvironment.getApplication().getApplicationContext(), channelId)
                .setContentTitle("t").setContentText("c");
    }

    // ---- fallback path (regression) -----------------------------------------

    /**
     * When the channel guesser cannot resolve a channel, the notification must still be
     * deliverable: it falls back to the general channel AND that channel exists. Without the
     * fix the fallback points at a channel that was never created, so it is silently dropped.
     */
    @Test
    public void unresolvableBuilderFallsBackToAnExistingGeneralChannel() {
        // Setup and act
        final Notification n = XdripNotificationCompat.build(builderWithChannel(null));

        // Verify
        assertWithMessage("fallback channel id")
                .that(n.getChannelId())
                .isEqualTo(NotificationChannels.GENERAL_CHANNEL);
        assertWithMessage("fallback channel exists")
                .that(notificationManager().getNotificationChannel(NotificationChannels.GENERAL_CHANNEL))
                .isNotNull();
    }

    // ---- invariants ---------------------------------------------------------

    /** build() never emits a notification without a channel, on any path. */
    @Test
    public void buildNeverLeavesANullChannelId() {
        // Act and verify
        assertWithMessage("resolved builder has a channel")
                .that(XdripNotificationCompat.build(builderWithChannel(NotificationChannels.GENERAL_CHANNEL)).getChannelId())
                .isNotNull();
        assertWithMessage("unresolvable builder has a channel")
                .that(XdripNotificationCompat.build(builderWithChannel(null)).getChannelId())
                .isNotNull();
    }

    /** A resolved notification is delivered on a channel that exists, as an independent alert. */
    @Test
    public void resolvedBuilderIsDeliverableAndNotAGroupSummary() {
        // Setup and act
        final Notification n = XdripNotificationCompat.build(builderWithChannel(NotificationChannels.GENERAL_CHANNEL));

        // Verify
        assertWithMessage("channel exists")
                .that(notificationManager().getNotificationChannel(n.getChannelId()))
                .isNotNull();
        assertWithMessage("not a group summary")
                .that((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0)
                .isFalse();
    }
}
