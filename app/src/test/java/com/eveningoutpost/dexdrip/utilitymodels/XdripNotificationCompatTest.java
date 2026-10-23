package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.BG_ALERT_CHANNEL;
import static com.google.common.truth.Truth.assertWithMessage;

import android.app.Notification;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import lombok.val;

/**
 * Tests for {@link XdripNotificationCompat}.
 * <p>
 * The alarm category is what tells a paired watch that a notification is an alarm, and some watches
 * drop notifications carrying it. Deciding whether an alert is an alarm belongs to the caller:
 * {@code AlertPlayer} sets the category only when the alert overrides silent mode. This helper must
 * therefore carry a caller's category through untouched, and never add one of its own - on either
 * of its two paths, since it rebuilds the notification after supplying a default channel.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class XdripNotificationCompatTest extends RobolectricTestWithConfig {

    /** A notification the caller left uncategorised comes out uncategorised. */
    @Test
    public void buildDoesNotAddACategoryOfItsOwn() {
        // :: Setup
        val builder = alertBuilder(BG_ALERT_CHANNEL);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("category of a notification the caller left uncategorised")
                .that(notification.category).isNull();
    }

    /** A category the caller did set is carried through untouched. */
    @Test
    public void buildKeepsTheCategorySetByTheCaller() {
        // :: Setup
        val builder = alertBuilder(BG_ALERT_CHANNEL);
        builder.setCategory(NotificationCompat.CATEGORY_ALARM);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("category set by the caller, as AlertPlayer does when overriding silent mode")
                .that(notification.category).isEqualTo(NotificationCompat.CATEGORY_ALARM);
    }

    /** A caller that named no channel gets the general one, and still no category. */
    @Test
    public void buildFallsBackToTheGeneralChannelWithoutAddingACategory() {
        // :: Setup
        val builder = alertBuilder(null);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("channel of a notification the caller gave none")
                .that(notification.getChannelId()).isEqualTo(NotificationChannels.GENERAL_CHANNEL);
        assertWithMessage("category of a notification built on the default-channel path")
                .that(notification.category).isNull();
    }

    /** Grouping is stripped, so an alert is never bundled away behind a summary. */
    @Test
    public void buildStripsTheGroupAndTheGroupSummary() {
        // :: Setup
        val builder = alertBuilder(BG_ALERT_CHANNEL);
        builder.setGroup("xdrip-alerts");
        builder.setGroupSummary(true);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("group of a notification the caller grouped")
                .that(NotificationCompat.getGroup(notification)).isNull();
        assertWithMessage("group summary flag of a notification the caller marked as a summary")
                .that(notification.flags & Notification.FLAG_GROUP_SUMMARY).isEqualTo(0);
    }

    private NotificationCompat.Builder alertBuilder(String channelId) {
        val builder = new NotificationCompat.Builder(
                RuntimeEnvironment.getApplication().getApplicationContext(), channelId);
        builder.setContentTitle("title");
        builder.setContentText("content");
        return builder;
    }
}
