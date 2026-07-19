package com.eveningoutpost.dexdrip.utilitymodels;

import static com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels.BG_ALERT_CHANNEL;
import static com.google.common.truth.Truth.assertWithMessage;

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
 * therefore carry a caller's category through untouched, and never add one of its own.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class XdripNotificationCompatTest extends RobolectricTestWithConfig {

    @Test
    public void buildDoesNotAddACategoryOfItsOwn() {
        // :: Setup
        val builder = alertBuilder();

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("category of a notification the caller left uncategorised")
                .that(notification.category).isNull();
    }

    @Test
    public void buildKeepsTheCategorySetByTheCaller() {
        // :: Setup
        val builder = alertBuilder();
        builder.setCategory(NotificationCompat.CATEGORY_ALARM);

        // :: Act
        val notification = XdripNotificationCompat.build(builder);

        // :: Verify
        assertWithMessage("category set by the caller, as AlertPlayer does when overriding silent mode")
                .that(notification.category).isEqualTo(NotificationCompat.CATEGORY_ALARM);
    }

    private NotificationCompat.Builder alertBuilder() {
        val builder = new NotificationCompat.Builder(RuntimeEnvironment.getApplication().getApplicationContext(), BG_ALERT_CHANNEL);
        builder.setContentTitle("title");
        builder.setContentText("content");
        builder.setVibrate(new long[]{123, 456, 789});
        return builder;
    }
}
