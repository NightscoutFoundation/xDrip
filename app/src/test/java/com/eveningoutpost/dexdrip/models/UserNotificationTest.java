package com.eveningoutpost.dexdrip.models;

import com.activeandroid.query.Delete;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import org.junit.After;
import org.junit.Test;

import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for {@link UserNotification}
 *
 * Found a "feature" shown in {@link #snoozeNewAlert()} where only legacy alerts can be snoozed
 * using this method.
 *
 * @author Asbjørn Aarrestad, asbjorn@aarrestad.com - 2018.03
 */
public class UserNotificationTest extends RobolectricTestWithConfig {

    @After
    public void cleanup() {
        // Clean up to avoid interference between tests
        new Delete()
                .from(UserNotification.class)
                .execute();
        UserNotification.DeleteNotificationByType("testAlert");
    }

    // ===== Test creation of all types of user notifications ======================================

    @Test
    public void createBgAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "bg_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.bg_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createCalibrationAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "calibration_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createDoubleCalibrationAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "double_calibration_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.double_calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createExtraCalibrationAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "extra_calibration_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.extra_calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createBgUnclearReadingsAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "bg_unclear_readings_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.bg_unclear_readings_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createBgMissedAlertsNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "bg_missed_alerts", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.bg_missed_alerts).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createBgRiseAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "bg_rise_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.bg_rise_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createBgFallAlertNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, "bg_fall_alert", timestamp);

        // :: Verify
        assertThat(userNotification).isNotNull();
        assertThat(userNotification.timestamp).isWithin(0).of(timestamp);
        assertThat(userNotification.bg_fall_alert).isTrue();
        assertThat(userNotification.message).isEqualTo(message);
    }

    @Test
    public void createNewTypeNotification() {
        // :: Setup
        long timestamp = System.currentTimeMillis();
        String message = "testMessage";
        String type = "testAlert";

        // :: Act
        UserNotification userNotification = UserNotification
                .create(message, type, timestamp);

        // :: Verify
        assertThat(userNotification).isNull();

        assertThat(PersistentStore.getString("UserNotification:timestamp:" + type))
                .isEqualTo(JoH.qs(timestamp));
        assertThat(PersistentStore.getString("UserNotification:message:" + type))
                .isEqualTo(message);
    }

    // ===== Tests for last**** alerts =============================================================

    @Test
    public void lastBgAlert() {
        // :: Setup
        UserNotification.create("test1", "bg_alert", System.currentTimeMillis());
        UserNotification.create("test2", "bg_alert", System.currentTimeMillis());

        // :: Act
        UserNotification userNotification = UserNotification.lastBgAlert();

        // :: Verify
        assertThat(userNotification.bg_alert).isTrue();
        assertThat(userNotification.message).isEqualTo("test2");
    }

    @Test
    public void lastCalibrationAlert() {
        // :: Setup
        UserNotification.create("test1", "calibration_alert", System.currentTimeMillis());
        UserNotification.create("test2", "calibration_alert", System.currentTimeMillis());

        // :: Act
        UserNotification userNotification = UserNotification.lastCalibrationAlert();

        // :: Verify
        assertThat(userNotification.calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo("test2");
    }

    @Test
    public void lastDoubleCalibrationAlert() {
        // :: Setup
        UserNotification.create("test1", "double_calibration_alert", System.currentTimeMillis());
        UserNotification.create("test2", "double_calibration_alert", System.currentTimeMillis());

        // :: Act
        UserNotification userNotification = UserNotification.lastDoubleCalibrationAlert();

        // :: Verify
        assertThat(userNotification.double_calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo("test2");
    }

    @Test
    public void lastExtraCalibrationAlert() {
        // :: Setup
        UserNotification.create("test1", "extra_calibration_alert", System.currentTimeMillis());
        UserNotification.create("test2", "extra_calibration_alert", System.currentTimeMillis());

        // :: Act
        UserNotification userNotification = UserNotification.lastExtraCalibrationAlert();

        // :: Verify
        assertThat(userNotification.extra_calibration_alert).isTrue();
        assertThat(userNotification.message).isEqualTo("test2");
    }

    // ===== Get Notification By Type ==============================================================

    @Test
    public void getExistingLegacyNotification() {
        // :: Setup
        UserNotification.create("test1", "calibration_alert", System.currentTimeMillis());

        // :: Act
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("calibration_alert");

        // :: Verify
        assertThat(calibrationAlert).isNotNull();
        assertThat(calibrationAlert.message).isEqualTo("test1");
    }

    @Test
    public void getNonExistingLegacyNotification() {
        // :: Act
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("calibration_alert");

        // :: Verify
        assertThat(calibrationAlert).isNull();
    }

    @Test
    public void getExistingNewNotification() {
        // :: Setup
        UserNotification.create("test1", "testAlert", System.currentTimeMillis());

        // :: Act
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("testAlert");

        // :: Verify
        assertThat(calibrationAlert).isNotNull();
        assertThat(calibrationAlert.message).isEqualTo("test1");
    }

    @Test
    public void getNonExistingNewNotification() {
        // :: Act
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("testAlert");

        // :: Verify
        assertThat(calibrationAlert).isNull();
    }

    // ===== Delete Notification By Type ===========================================================

    @Test
    public void deleteExistingLegacyAlert() {
        // :: Setup
        UserNotification.create("test1", "calibration_alert", System.currentTimeMillis());

        // :: Act
        UserNotification.DeleteNotificationByType("calibration_alert");

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("calibration_alert");
        assertThat(calibrationAlert).isNull();
    }

    @Test
    public void deleteNonExistingLegacyAlert() {
        // :: Act
        UserNotification.DeleteNotificationByType("calibration_alert");

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("calibration_alert");
        assertThat(calibrationAlert).isNull();
    }

    @Test
    public void deleteExistingNewAlert() {
        // :: Setup
        UserNotification.create("test1", "testAlert", System.currentTimeMillis());

        // :: Act
        UserNotification.DeleteNotificationByType("testAlert");

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("testAlert");
        assertThat(calibrationAlert).isNull();
    }

    @Test
    public void deleteNonExistingNewAlert() {
        // :: Act
        UserNotification.DeleteNotificationByType("testAlert");

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("testAlert");
        assertThat(calibrationAlert).isNull();
    }

    // ===== SnoozeAlert ===========================================================================

    @Test
    public void snoozeLegacyAlert() {
        // :: Setup
        long initialTimestamp = System.currentTimeMillis();
        UserNotification.create("test1", "calibration_alert", initialTimestamp);

        // :: Act
        UserNotification.snoozeAlert("calibration_alert", 15);

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("calibration_alert");
        assertThat(calibrationAlert).isNotNull();
        assertThat(calibrationAlert.timestamp).isAtLeast(initialTimestamp + 15d * 60_000);
    }

    /**
     * When writing this test, it was not possible to snooze "new" alerts, only legacy alerts.
     * This can be fixed if needed, and if you fix it and this test fails, just remove "expected"
     */
    @Test(expected = AssertionError.class)
    public void snoozeNewAlert() {
        // :: Setup
        long initialTimestamp = System.currentTimeMillis();
        UserNotification.create("test1", "testAlert", initialTimestamp);

        // :: Act
        UserNotification.snoozeAlert("testAlert", 15);

        // :: Verify
        UserNotification newAlert = UserNotification.GetNotificationByType("testAlert");
        assertThat(newAlert).isNotNull();
        assertThat(newAlert.timestamp - initialTimestamp).isAtLeast(15d * 60_000);
    }

    @Test
    public void snoozeNonExistingAlert() {
        // :: Setup
        long initialTimestamp = System.currentTimeMillis();
        UserNotification.create("test1", "testAlert", initialTimestamp);

        // :: Act
        UserNotification.snoozeAlert("testAlert2", 15);

        // :: Verify
        UserNotification calibrationAlert = UserNotification.GetNotificationByType("testAlert");
        assertThat(calibrationAlert).isNotNull();
        assertThat(calibrationAlert.timestamp).isWithin(0).of(initialTimestamp);
    }

    @Test
    public void formatTest() {
        final double tval = 2738568152156d;
        assertThat(Double.parseDouble(String.format(Locale.US, "%d", (long)tval))).isEqualTo(tval);
    }
}