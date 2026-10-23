package com.eveningoutpost.dexdrip.utilitymodels;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;


import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.ui.NumberGraphic;
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import lombok.val;

/**
 * Created by jwoglom on 10/15/2017.
 * <p>
 * Contains setup for creation of notification channels, and constants for
 * channelId values used when creating notifications.
 * <p>
 * modified for dynamic channels by jamorham
 */

public class NotificationChannels {
    public static final String TAG = NotificationChannels.class.getSimpleName();
    private static HashMap<String, String> map;

    public static final String BG_ALERT_CHANNEL = "bgAlertChannel";
    public static final String ONGOING_CHANNEL = "ongoingChannel";
    public static final String GENERAL_CHANNEL = "generalChannel"; // This should be used for all existing notifications that have null for their channel.
    public static final String OTHER_ALERTS_CHANNEL = "otherAlertsChannel"; // This is the channel for all Other alerts.

    // get a localized string for each channel / group name
    public static String getString(String id) {
        if (map == null) initialize_name_map();
        if (!map.containsKey(id)) return id;
        return map.get(id);
    }

    // create string lookup map singleton
    private static synchronized void initialize_name_map() {
        if (map != null) return;
        map = new HashMap<>();
        map.put(BG_ALERT_CHANNEL, xdrip.getAppContext().getString(R.string.glucose_level_notifications));
        map.put(ONGOING_CHANNEL, xdrip.getAppContext().getString(R.string.ongoing_notification));
        map.put(GENERAL_CHANNEL, xdrip.getAppContext().getString(R.string.general_notifications));
        map.put(OTHER_ALERTS_CHANNEL, xdrip.getAppContext().getString(R.string.other_alert_notifications));
    }


    private static NotificationManager getNotifManager() {
        return (NotificationManager) xdrip.getAppContext().getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @TargetApi(26)
    private static int myhashcode(NotificationChannel x) {

        int result = x.getId() != null ? x.getId().hashCode() : 0;
        //result = 31 * result + (getName() != null ? getName().hashCode() : 0);
        //result = 31 * result + (getDescription() != null ? getDescription().hashCode() : 0);
        //result = 31 * result + getImportance();
        //result = 31 * result + (mBypassDnd ? 1 : 0);
        //result = 31 * result + getLockscreenVisibility();
        result = 31 * result + (x.getSound() != null ? x.getSound().hashCode() : 0);
        //result = 31 * result + (x.mLights ? 1 : 0);
        result = 31 * result + x.getLightColor();
        result = 31 * result + Arrays.hashCode(x.getVibrationPattern());
        //result = 31 * result + getUserLockedFields();
        //result = 31 * result + (mVibrationEnabled ? 1 : 0);
        //result = 31 * result + (mShowBadge ? 1 : 0);
        //result = 31 * result + (isDeleted() ? 1 : 0);
        //result = 31 * result + (getGroup() != null ? getGroup().hashCode() : 0);
        //result = 31 * result + (getAudioAttributes() != null ? getAudioAttributes().hashCode() : 0);
        //result = 31 * result + (isBlockableSystem() ? 1 : 0);
        return result;

    }

    @TargetApi(26)
    private static String my_text_hash(NotificationChannel x) {
        String res = "";
        if (x.getSound() != null) res += "\uD83C\uDFB5"; // �
        if (x.shouldVibrate()) res += "\uD83D\uDCF3"; // �
        if (x.shouldShowLights()) res += "\uD83D\uDCA1"; // �
        res = (res.equals("")) ? res : "  " + res;

        int counter = 1;
        while (counter < 10 && isSoundDifferent(x.getId() + res + ((counter > 1) ? counter : ""), x)) {
            counter++;
        }
        if (counter != 1) res += "" + counter;
        return res;

    }

    @TargetApi(26)
    public static boolean isSoundDifferent(String id, NotificationChannel x) {
        if (x.getSound() == null) return false; // this does not have a sound
        final NotificationChannel c = getNotifManager().getNotificationChannel(id);
        if (c == null) return false; // no channel with this id
        if (c.getSound() == null)
            return false; // this maybe will only happen if user disables sound so lets not create a new one in that case

        final String original_sound = PersistentStore.getString("original-channel-sound-" + id);
        if (original_sound.equals("")) {
            PersistentStore.setString("original-channel-sound-" + id, x.getSound().toString());
            return false; // no existing record so save the original and do nothing else
        }
        if (original_sound.equals(x.getSound().toString()))
            return false; // its the same sound still
        return true; // the sound has changed vs the original
    }

    @TargetApi(26)
    public static void cleanAllNotificationChannels() {
        // TODO this isn't right yet
        List<NotificationChannel> channels = getNotifManager().getNotificationChannels();
        for (NotificationChannel channel : channels) {
            getNotifManager().deleteNotificationChannel(channel.getId());


        }
        List<NotificationChannelGroup> groups = getNotifManager().getNotificationChannelGroups();
        for (NotificationChannelGroup group : groups) {
            getNotifManager().deleteNotificationChannel(group.getId());
        }

    }

    @TargetApi(26)
    public static NotificationChannel getChan(NotificationCompat.Builder wip) {

        final Notification temp = wip.build();
        if (temp.getChannelId() == null) return null;
        final int importance = NotificationManager.IMPORTANCE_HIGH;

        // create generic audio attributes
        final AudioAttributes generic_audio = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                .build();

        // create notification channel for hashing purposes from the existing notification builder
        NotificationChannel template = new NotificationChannel(
                temp.getChannelId(),
                getString(temp.getChannelId()),
                NotificationManager.IMPORTANCE_DEFAULT);


        // mirror the notification parameters in the channel

        val mNotification = getNotificationFromInsideBuilder(wip);
        if (mNotification != null) {
            template.setVibrationPattern(mNotification.vibrate);
            template.setSound(mNotification.sound, generic_audio);
            template.setLightColor(mNotification.ledARGB);
            if (mNotification.ledOnMS != 0 && mNotification.ledOffMS != 0)
                template.enableLights(true); // weird how this doesn't work like vibration pattern
        }

        template.setDescription(temp.getChannelId() + " " + wip.hashCode());

        // get a nice string to identify the hash
        final String mhash = my_text_hash(template);
        final String channelId = temp.getChannelId();
        final String baseName = getBaseDisplayName(channelId);

        // create another notification channel using the hash because id is immutable
        final NotificationChannel channel = new NotificationChannel(
                template.getId() + mhash,
                baseName + mhash,
                importance); // Change from IMPORTANCE_DEFAULT

        // mirror the settings from the previous channel
        channel.setSound(template.getSound(), generic_audio);
        channel.setDescription(template.getDescription());
        channel.setVibrationPattern(template.getVibrationPattern());

        if (mNotification != null) {
            template.setLightColor(mNotification.ledARGB);
            if ((mNotification.ledOnMS != 0) && (mNotification.ledOffMS != 0))
                template.enableLights(true); // weird how this doesn't work like vibration pattern
        }

        template.setDescription(temp.getChannelId() + " " + wip.hashCode());

        // create this channel if it doesn't exist or update text
        getNotifManager().createNotificationChannel(channel);
        return mNotification != null ? channel : null; // Note we return null to fallback old behavior if we can't get reflected access
    }

    @TargetApi(26)
    public static NotificationChannel getChan(Notification.Builder wip) {
        /*
        This method should only be used for the ongoing notification.
        No alert should use this method.
         */
        final String id = ONGOING_CHANNEL;
        final int importance = NotificationManager.IMPORTANCE_LOW;

        // Simplify: Create the channel directly using the static ID
        final NotificationChannel channel = new NotificationChannel(
                id,
                getBaseDisplayName(id),
                importance);

        // Ongoing service should always be silent and not vibrate
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        getNotifManager().createNotificationChannel(channel); // This is where we dynamically create the ongoing notification channel.
        return channel;
    }

    private static String getBaseDisplayName(String channelId) {
        return getString(channelId);
    }

    static Notification getNotificationFromInsideBuilder(final NotificationCompat.Builder builder) {
        try {
            final Class<?> builderClass = builder.getClass();
            final Field mNotificationField = builderClass.getDeclaredField("mNotification");
            mNotificationField.setAccessible(true);
            return (Notification) mNotificationField.get(builder);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (JoH.ratelimit("notification-workaround", 1800)) {
                UserError.Log.wtf(TAG, "Workaround being used for notification channels no longer works - please report");
            }
            return null;
        }
    }

    private static void setupChannel(String id, String name, int importance, int lightColor, boolean useVibration, long[] vibratePattern, boolean showBadge) {
        AudioAttributes attr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).build();
        NotificationChannel chan = new NotificationChannel(id, name, importance);
        chan.setSound(null, attr);
        chan.setShowBadge(showBadge);
        if (lightColor != 0) {
            chan.enableLights(true);
            chan.setLightColor(lightColor);
        }
        chan.enableVibration(useVibration);
        if (useVibration && vibratePattern != null) {
            chan.setVibrationPattern(vibratePattern);
        }
        getNotifManager().createNotificationChannel(chan);
    }


    /**
     * Creates required notification channels and cleans up legacy ones.
     */
    public static void setupAllChannels() {
        // Create the required notification channels that do not need to be created dynamically
        // The ongoing channel is the only channel that we create dynamically. Otherwise, the ongoing notification will be grouped with the other notifications (alerts)!
        setupChannel(BG_ALERT_CHANNEL, getString(BG_ALERT_CHANNEL), NotificationManager.IMPORTANCE_HIGH, 0xffff0000, false, null, true);
        setupChannel(OTHER_ALERTS_CHANNEL, getString(OTHER_ALERTS_CHANNEL), NotificationManager.IMPORTANCE_HIGH, 0xffffbf00, false, null, true);
        setupChannel(GENERAL_CHANNEL, getString(GENERAL_CHANNEL), NotificationManager.IMPORTANCE_DEFAULT, 0xff00ff00, false, null, true);

        // Delete legacy or zombie channels that are no longer part of our map
        cleanupOldChannels();
    }

    private static void cleanupOldChannels() {
        if (map == null) initialize_name_map();

        final NotificationManager manager = getNotifManager();
        if (manager == null) return;

        final Set<String> activeIds = map.keySet();

        for (NotificationChannel channel : manager.getNotificationChannels()) {
            if (!activeIds.contains(channel.getId())) {
                manager.deleteNotificationChannel(channel.getId());
            }
        }
    }

}
