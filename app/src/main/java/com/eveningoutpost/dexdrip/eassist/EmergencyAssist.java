package com.eveningoutpost.dexdrip.eassist;

import androidx.databinding.ObservableField;
import android.os.PowerManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.SMS;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.List;

/*
 * jamorham
 *
 * Emergency Assist Message feature
 *
 * Send out a text message containing handset location and reason
 *
 */


public class EmergencyAssist {

    public static final String EMERGENCY_ASSIST_PREF = "emergency_assist_enabled";
    public static final String EMERGENCY_LOW_MINS_PREF = "emergency_assist_low_alert_minutes";
    public static final String EMERGENCY_LOWEST_MINS_PREF = "emergency_assist_lowest_alert_minutes";
    public static final String EMERGENCY_HIGH_MINS_PREF = "emergency_assist_high_alert_minutes";

    private static final String TAG = EmergencyAssist.class.getSimpleName();
    private final Reason reason;
    private final long since;
    private final String extra;

    public final ObservableField<String> lastExtendedText = new ObservableField<>();

    public EmergencyAssist(Reason reason, long since) {
        this(reason, since, "");
    }

    public EmergencyAssist(Reason reason, long since, String extra) {
        this.reason = reason;
        this.since = since;
        this.extra = (extra != null) ? extra : "";
        getExtendedReasonText();
    }


    public static void test(final Reason reason, long since) {
        new EmergencyAssist(reason, since).activate();
    }


    public static void checkAndActivate(final Reason reason, long since) {
        checkAndActivate(reason, since, "");
    }

    private static final int MIN_MESSAGE_FREQUENCY = 10800; // 10800 = 3 hours in seconds, cannot activate more than once per this period

    public static void checkAndActivate(final Reason reason, long since, String extra) {
        if (isEnabled(reason)) {
            if (since > timeThreshold(reason)) {
                if (JoH.pratelimit("ea-check-and-activate-limit", MIN_MESSAGE_FREQUENCY)) {
                    UserError.Log.e(TAG, "Triggering " + reason + " since: " + JoH.niceTimeScalar(since) + " " + extra);
                    new EmergencyAssist(reason, since, extra).activate();
                }
            }
        }
    }

    public static boolean isEnabled() {
        return Pref.getBooleanDefaultFalse(EMERGENCY_ASSIST_PREF);
    }

    public static boolean isEnabled(Reason reason) {
        if (!isEnabled()) return false;
        switch (reason) {
            case DID_NOT_ACKNOWLEDGE_LOWEST_ALERT:
                return Pref.getBooleanDefaultFalse("emergency_assist_lowest_alert");
            case DID_NOT_ACKNOWLEDGE_LOW_ALERT:
                return Pref.getBooleanDefaultFalse("emergency_assist_low_alert");
            case DID_NOT_ACKNOWLEDGE_HIGH_ALERT:
                return Pref.getBooleanDefaultFalse("emergency_assist_high_alert");
            case DEVICE_INACTIVITY:
                return Pref.getBooleanDefaultFalse("emergency_assist_inactivity");

            case EXTENDED_MISSED_READINGS:
            case REQUESTED_ASSISTANCE:
            case TESTING_FEATURE:
            case UNSPECIFIED:
            default:
                UserError.Log.e(TAG, "Unknown reason in isEnabled: " + reason);
                return false;
        }
    }

    // TODO check parameters set

    public String getUsername() {
        return Pref.getString("ea-username", "");
    }

    public void setUsername(String value) {
        Pref.setString("ea-username", value);
        getExtendedReasonText();
    }

    private static String getReasonText(Reason reason) {

        switch (reason) {
            case DID_NOT_ACKNOWLEDGE_LOWEST_ALERT:
            case DID_NOT_ACKNOWLEDGE_LOW_ALERT:
                return getString(R.string.did_not_acknowledge_a_low_glucose_alert);
            case DID_NOT_ACKNOWLEDGE_HIGH_ALERT:
                return getString(R.string.did_not_acknowledge_a_high_glucose_alert);
            case EXTENDED_MISSED_READINGS:
                return getString(R.string.has_not_had_glucose_data_received_for);
            case DEVICE_INACTIVITY:
                return getString(R.string.phone_has_not_been_used_for);
            case REQUESTED_ASSISTANCE:
                return getString(R.string.is_requesting_assistance);
            case TESTING_FEATURE:
                return getString(R.string.is_testing_the_assistance_request_feature);
            default:
                return "";
        }
    }

    private static long minutesToMs(String mins) {
        try {
            return Integer.parseInt(mins) * Constants.MINUTE_IN_MS;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long timeThreshold(Reason reason) {
        switch (reason) {
            case DID_NOT_ACKNOWLEDGE_LOWEST_ALERT:
                return minutesToMs(Pref.getString(EMERGENCY_LOWEST_MINS_PREF, "60"));
            case DID_NOT_ACKNOWLEDGE_LOW_ALERT:
                return minutesToMs(Pref.getString(EMERGENCY_LOW_MINS_PREF, "60"));
            case DID_NOT_ACKNOWLEDGE_HIGH_ALERT:
                return minutesToMs(Pref.getString(EMERGENCY_HIGH_MINS_PREF, "60"));
            default:
                return 0;
        }
    }

    private void activate() {
        UserError.Log.ueh(TAG, "Emergency Assist activated: reason: " + reason);
        if (destinationsDefined()) {
            final PowerManager.WakeLock wl = JoH.getWakeLock("emergency-activate", 120000); // linger
            JoH.static_toast_long("Sending assistance message in 15 seconds");
            Inevitable.task("prep location-ea", 200, GetLocation::prepareForLocation);
            Inevitable.task("get location-ea", 5000, GetLocation::getLocation);
            Inevitable.task("send assist message", 15000, () -> sendAssistMessage(true));
        } else {
            final String err = "No emergency contacts defined! Cannot activate!";
            JoH.static_toast_long(err);
            UserError.Log.wtf(TAG, err);
        }
    }

    public void getLocation() {
        Inevitable.task("prep location-ea", 100, GetLocation::prepareForLocation);
        Inevitable.task("get location-ea", 300, GetLocation::getLocation);
        Inevitable.task("stream location-ea", 300, this::streamLocation);
        //Inevitable.task("update extended message", 15000, this::getExtendedReasonText);
    }


    private boolean destinationsDefined() {
        return EmergencyContact.load().size() > 0;
    }

    private boolean sendMessageReal(String msg) {
        final List<EmergencyContact> destinations = EmergencyContact.load();
        for (EmergencyContact dest : destinations) {
            UserError.Log.wtf(TAG, "Sending SMS to: " + dest.number + " :: " + msg);
            try {
                SMS.sendSMS(dest.number, msg);
            } catch (Exception e) {
                UserError.Log.wtf(TAG, "Exception sending sms: " + e);
            }
        }
        return true;
    }

    private void sendAssistMessage(boolean firstRun) {
        final String locationText = GetLocation.getBestLocation();
        final String msg = getExtendedReasonText();

        UserError.Log.wtf(TAG, "Sending Assistance message: " + msg);
        sendMessageReal(msg);

        if (firstRun) {
            Inevitable.task("check-emergency-location", GetLocation.getGPS_ACTIVE_TIME(), () -> {
                if (!locationText.equals(GetLocation.getBestLocation())) {
                    UserError.Log.d(TAG, "Location has changed: " + locationText + " vs " + GetLocation.getBestLocation());
                    sendAssistMessage(false);
                } else {
                    UserError.Log.d(TAG, "Location has not changed from: " + locationText);
                }
            });
        }

    }

    private void streamLocation() {
        final long endTime = JoH.tsl() + GetLocation.getGPS_ACTIVE_TIME() + 15000;
        while (JoH.tsl() < endTime) {
            JoH.threadSleep(1000);
            getExtendedReasonText();
        }
    }

    public String getExtendedReasonText() {
        final String timeText = JoH.niceTimeScalar(since);
        final String userText = getUsername();
        final String reasonText = String.format(getReasonText(reason), userText.length() > 0 ? userText : "Name not set", timeText);
        final String locationText = GetLocation.getBestLocation();
        final String mapText = GetLocation.getMapUrl();
        final String signatureText = getString(R.string.automatic_message_from_xdrip);
        lastExtendedText.set(String.format(getString(R.string.emergency_message_near_format_string),
                reasonText, locationText, mapText, extra, signatureText).trim());
        return lastExtendedText.get();
    }

    private static String getString(int id) {
        return xdrip.getAppContext().getString(id);
    }

    public enum Reason {
        DID_NOT_ACKNOWLEDGE_LOWEST_ALERT,
        DID_NOT_ACKNOWLEDGE_LOW_ALERT,
        DID_NOT_ACKNOWLEDGE_HIGH_ALERT,
        EXTENDED_MISSED_READINGS,
        DEVICE_INACTIVITY,
        REQUESTED_ASSISTANCE,
        TESTING_FEATURE,
        UNSPECIFIED,
    }

}
