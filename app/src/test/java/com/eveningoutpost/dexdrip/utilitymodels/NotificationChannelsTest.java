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

    // ===== channel name map =============================================================================================

    /**
     * Each of the channels this branch still defines resolves to a display name rather than falling
     * back to its raw channel id. The list is short because this branch collapses the channel set to
     * four; on master the same test covers eleven.
     */
    @Test
    public void mappedChannelsResolveToDisplayNames() {
        // :: Setup
        val mappedChannels = new String[]{
                NotificationChannels.BG_ALERT_CHANNEL,
                NotificationChannels.OTHER_ALERTS_CHANNEL,
                NotificationChannels.GENERAL_CHANNEL,
                NotificationChannels.ONGOING_CHANNEL,
        };

        // :: Act & Verify
        for (val channel : mappedChannels) {
            val name = NotificationChannels.getString(channel);
            assertWithMessage("channel " + channel + " has a display name").that(name).isNotEmpty();
            assertWithMessage("channel " + channel + " is not displayed as its raw id")
                    .that(name).isNotEqualTo(channel);
        }
    }
}
