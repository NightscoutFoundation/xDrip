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
import com.eveningoutpost.dexdrip.xdrip;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    public static final String LOW_BRIDGE_BATTERY_CHANNEL = "lowBridgeBattery";
    public static final String LOW_TRANSMITTER_BATTERY_CHANNEL = "lowTransmitterBattery";
    public static final String NIGHTSCOUT_UPLOADER_CHANNEL = "nightscoutUploaderChannel";
    public static final String PARAKEET_STATUS_CHANNEL = "parakeetStatusChannel";
    public static final String REMINDER_CHANNEL = "reminderChannel";
    public static final String BG_ALERT_CHANNEL = "bgAlertChannel";
    public static final String BG_MISSED_ALERT_CHANNEL = "bgMissedAlertChannel";
    public static final String BG_RISE_DROP_CHANNEL = "bgRiseDropChannel";
    public static final String BG_PREDICTED_LOW_CHANNEL = "bgPredictedLowChannel";
    public static final String BG_PERSISTENT_HIGH_CHANNEL = "bgPersistentHighChannel";
    public static final String CALIBRATION_CHANNEL = "calibrationChannel";
    public static final String ONGOING_CHANNEL = "ongoingChannel";
    public static final String ICON_TEST_CHANNEL = "numberIconTestChannel";
    public static final String GENERAL_CHANNEL = "generalChannel"; // This should be used for all existing notifications that have null for their channel.
    public static final String SENSOR_EXPIRY_CHANNEL = "sensorExpiryChannel";

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
        map.put(LOW_BRIDGE_BATTERY_CHANNEL, xdrip.getAppContext().getString(R.string.low_bridge_battery));
        map.put(LOW_TRANSMITTER_BATTERY_CHANNEL, xdrip.getAppContext().getString(R.string.transmitter_battery));
        map.put(NIGHTSCOUT_UPLOADER_CHANNEL, "Nightscout");
        map.put(PARAKEET_STATUS_CHANNEL, xdrip.getAppContext().getString(R.string.parakeet_related_alerts));
        map.put(REMINDER_CHANNEL, xdrip.getAppContext().getString(R.string.reminders));
        map.put(BG_ALERT_CHANNEL, xdrip.getAppContext().getString(R.string.glucose_alerts_settings));
        map.put(BG_MISSED_ALERT_CHANNEL, xdrip.getAppContext().getString(R.string.missed_reading_alert));
        map.put(BG_RISE_DROP_CHANNEL, xdrip.getAppContext().getString(R.string.bg_rising_fast));
        map.put(BG_PREDICTED_LOW_CHANNEL, xdrip.getAppContext().getString(R.string.low_predicted));
        map.put(BG_PERSISTENT_HIGH_CHANNEL, xdrip.getAppContext().getString(R.string.persistent_high_alert));
        map.put(CALIBRATION_CHANNEL, xdrip.getAppContext().getString(R.string.calibration_alerts));
        map.put(ONGOING_CHANNEL, "Ongoing Notification");
        map.put(GENERAL_CHANNEL, "General");
        map.put(SENSOR_EXPIRY_CHANNEL, "Sensor expiry");
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
        final int importance = Pref.getBooleanDefaultFalse("use_number_icon") ?
                NotificationManager.IMPORTANCE_DEFAULT : NotificationManager.IMPORTANCE_LOW;

        // Simplify: Create the channel directly using the static ID
        final NotificationChannel channel = new NotificationChannel(
                id,
                getBaseDisplayName(id),
                importance);

        // Ongoing service should always be silent and not vibrate
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.setShowBadge(false);

        getNotifManager().createNotificationChannel(channel);
        return channel;
    }

    private static String getBaseDisplayName(String channelId) {
        if ("bgAlertChannel".equals(channelId)) {
            return "Glucose Level Alert";
        }
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

    public static void setupTestChannel() {
        NotificationChannel channel = new NotificationChannel(
                ICON_TEST_CHANNEL,
                "xDrip Icon Test",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableVibration(true);
        channel.setSound(null, null); // Keep it silent
        getNotifManager().createNotificationChannel(channel);
    }

    public static void setupGeneralChannel() {
        NotificationChannel channel = new NotificationChannel(
                GENERAL_CHANNEL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableVibration(true);
        getNotifManager().createNotificationChannel(channel);
    }

    public static void setupSensorExpiryChannel() {
        NotificationChannel channel = new NotificationChannel(
                SENSOR_EXPIRY_CHANNEL,
                "Sensor expiry",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.enableVibration(true);
        getNotifManager().createNotificationChannel(channel);
    }

}
