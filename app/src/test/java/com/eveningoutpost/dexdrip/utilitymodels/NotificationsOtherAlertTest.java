package com.eveningoutpost.dexdrip.utilitymodels;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.models.UserNotification;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import lombok.val;

/**
 * Tests for the alarm category of the "other" alerts raised by {@code Notifications.OtherAlert}.
 * <p>
 * An other-alert that is set to override silent mode is given the alarm category and a high-priority
 * full-screen intent, which is what classifies it for Do-Not-Disturb, lets it take over the screen,
 * and tells a paired watch it is an alarm. The audible part is deliberately out of scope here: the
 * sound is played by {@code AlertPlayer.triggerSoundAndVibration}, which picks the alarm stream from
 * the same setting. The category must follow the per-alert setting, which falls back to the global
 * one for the other alerts, and defaults to off when neither is present.
 *
 * @author Asbjørn Aarrestad - 2026.07
 */
public class NotificationsOtherAlertTest extends RobolectricTestWithConfig {

    private static final String UNCLEAR_ALERT_TYPE = "bg_unclear_readings_alert";
    private static final String ALERT_OVERRIDE_SILENT = UNCLEAR_ALERT_TYPE + "_override_silent";
    private static final String OTHER_ALERTS_OVERRIDE_SILENT = "other_alerts_override_silent";

    @Before
    public void clearAlertStateLeftByOtherTestClasses() {
        // The reraise window stores a future timestamp per alert type in the shared database, and a
        // row for this very type is left behind by other test classes - UserNotificationTest
        // creates one. An unexpired row suppresses the alert these tests need to raise.
        UserNotification.DeleteNotificationByType(UNCLEAR_ALERT_TYPE);
    }

    @After
    public void clearAlertStateLeftForOtherTestClasses() {
        // Both the database and Pref's cached preference store outlive this class: Pref binds to the
        // first store it sees and never rebinds, so a preference written here is still visible to a
        // later test class through Pref even though its own raw preferences are empty.
        UserNotification.DeleteNotificationByType(UNCLEAR_ALERT_TYPE);
        Pref.removeItem(ALERT_OVERRIDE_SILENT);
        Pref.removeItem(OTHER_ALERTS_OVERRIDE_SILENT);
        AlertPlayer.activeTag = ""; // Cleared by a media player callback the test does not control
    }

    /** An alert set to override silent mode is an alarm, and may take over the screen. */
    @Test
    public void otherAlertBecomesAnAlarmWhenItOverridesSilentMode() {
        // :: Setup
        setOtherAlertsOverrideSilent(false);
        setAlertOverrideSilent(true);

        // :: Act
        Notifications.bgUnclearAlert(context());

        // :: Verify
        val notification = postedNotification();
        assertWithMessage("category of an other-alert set to override silent mode")
                .that(notification.category)
                .isEqualTo(NotificationCompat.CATEGORY_ALARM);
        assertWithMessage("full screen intent of an other-alert set to override silent mode")
                .that(notification.fullScreenIntent)
                .isNotNull();
        assertWithMessage("high priority flag, which is what lets the full screen intent launch")
                .that(notification.flags & NotificationCompat.FLAG_HIGH_PRIORITY)
                .isEqualTo(NotificationCompat.FLAG_HIGH_PRIORITY);
    }

    /** An alert set to respect silent mode stays an ordinary notification. */
    @Test
    public void otherAlertIsNotAnAlarmWhenItRespectsSilentMode() {
        // :: Setup
        setOtherAlertsOverrideSilent(true);
        setAlertOverrideSilent(false);

        // :: Act
        Notifications.bgUnclearAlert(context());

        // :: Verify
        val notification = postedNotification();
        assertWithMessage("category of an other-alert left to respect silent mode")
                .that(notification.category)
                .isNull();
        assertWithMessage("full screen intent of an other-alert left to respect silent mode")
                .that(notification.fullScreenIntent)
                .isNull();
    }

    /** On a fresh install, with no setting at all, an other-alert is not an alarm. */
    @Test
    public void otherAlertIsNotAnAlarmWhenNeitherSettingIsPresent() {
        // :: Setup
        // Deliberately writes no preference: this is the default state of a fresh install, and the
        // only case that pins the default of the global setting itself.

        // :: Act
        Notifications.bgUnclearAlert(context());

        // :: Verify
        assertWithMessage("category of an other-alert with neither setting present")
                .that(postedNotification().category)
                .isNull();
    }

    /** With no setting of its own, an alert follows the global one for the other alerts. */
    @Test
    public void otherAlertInheritsTheGlobalSettingWhenItHasNoneOfItsOwn() {
        // :: Setup
        setOtherAlertsOverrideSilent(true);

        // :: Act
        Notifications.bgUnclearAlert(context());

        // :: Verify
        assertWithMessage("category of an other-alert inheriting the global override of silent mode")
                .that(postedNotification().category)
                .isEqualTo(NotificationCompat.CATEGORY_ALARM);
    }

    /** A setting of its own wins over the global one. */
    @Test
    public void otherAlertKeepsItsOwnSettingWhenTheGlobalOneDiffers() {
        // :: Setup
        setOtherAlertsOverrideSilent(true);
        setAlertOverrideSilent(false);

        // :: Act
        Notifications.bgUnclearAlert(context());

        // :: Verify
        assertWithMessage("category of an other-alert overriding the global setting with its own")
                .that(postedNotification().category)
                .isNull();
    }

    private void setAlertOverrideSilent(boolean overrideSilent) {
        prefs().edit().putBoolean(ALERT_OVERRIDE_SILENT, overrideSilent).commit();
    }

    private void setOtherAlertsOverrideSilent(boolean overrideSilent) {
        prefs().edit().putBoolean(OTHER_ALERTS_OVERRIDE_SILENT, overrideSilent).commit();
    }

    /**
     * The store the production code under test reads. It must stay bound to the per-test
     * application: {@code xdrip.getAppContext()} keeps the first application it ever saw, so
     * preferences written through it are visible to later tests in the same run.
     */
    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(context());
    }

    private Notification postedNotification() {
        val manager = (NotificationManager) context().getSystemService(Context.NOTIFICATION_SERVICE);
        final List<Notification> posted = shadowOf(manager).getAllNotifications();
        // The shadow keeps the notifications in a map, so asking for "the last one" would pick an
        // arbitrary one. Each test here raises exactly one alert, and that is worth asserting.
        assertWithMessage("notifications posted by a single other-alert").that(posted).hasSize(1);
        return posted.get(0);
    }

    private Context context() {
        return RuntimeEnvironment.getApplication().getApplicationContext();
    }
}
