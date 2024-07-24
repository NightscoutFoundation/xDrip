package com.eveningoutpost.dexdrip.utils.framework;

// jamorham

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import androidx.core.app.ActivityCompat;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.watch.lefun.LeFun;
import com.eveningoutpost.dexdrip.watch.lefun.LeFunEntry;
import com.eveningoutpost.dexdrip.watch.miband.MiBand;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.watch.thinjam.BlueJayEntry;
import com.eveningoutpost.dexdrip.xdrip;

import lombok.Getter;

import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CALL;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_CANCEL;
import static com.eveningoutpost.dexdrip.watch.thinjam.Const.THINJAM_NOTIFY_TYPE_CALL;

public class IncomingCallsReceiver extends BroadcastReceiver {

    private static final String TAG = "IncomingCalls";
    @Getter
    private static volatile boolean ringingNow = false;

    public static void checkPermission(final Activity activity) {

        // TODO call log permission - especially for Android 9+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((xdrip.getAppContext().checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED)
                    || xdrip.getAppContext().checkSelfPermission(Manifest.permission.READ_CONTACTS)
                    != PackageManager.PERMISSION_GRANTED
                    || ((Build.VERSION.SDK_INT > Build.VERSION_CODES.O && xdrip.getAppContext().checkSelfPermission(Manifest.permission.READ_CALL_LOG)
                    != PackageManager.PERMISSION_GRANTED))) {

                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.READ_CONTACTS},
                        Constants.GET_PHONE_READ_PERMISSION);
            }
        }
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getExtras() == null) return;
        final String stateExtra = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        final String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);

        // TODO lookup contacts
        UserError.Log.d(TAG, "Call State: " + stateExtra + " " + number);
        if (stateExtra != null && stateExtra.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
            ringingNow = true;

            // Lefun
            if (JoH.quietratelimit("lefun-call-debounce", 10)) {
                if (LeFunEntry.areCallAlertsEnabled()) {
                    UserError.Log.d(TAG, "Sending call alert: " + number);
                    // TODO extract to generic notifier
                    final String caller = number != null ? number.substring(Math.max(0, number.length() - 8)) : "CALL";
                    LeFun.sendAlert(true, caller);
                }
            }
            // MiBand
            if (JoH.quietratelimit("miband-call-debounce", 10)) {
                if (MiBandEntry.areCallAlertsEnabled()) {
                    // TODO extract to generic notifier
                    final String caller = number != null ? "Incoming Call " + getContactDisplayNameByNumber(number) + " " + bestPhoneNumberFormatter(number) + " " : "CALL";
                    UserError.Log.d(TAG, "Sending call alert: " + caller);
                    MiBand.sendCall(MIBAND_NOTIFY_TYPE_CALL, caller);
                }
            }

            // BlueJay
            if (JoH.quietratelimit("bluejay-call-debounce" + number, 10)) {
                if (BlueJayEntry.areCallAlertsEnabled()) {
                    // TODO extract to generic notifier
                    final String caller = number != null ? "Incoming Call " + getContactDisplayNameByNumber(number) + " " + bestPhoneNumberFormatter(number) + " " : "CALL";
                    UserError.Log.d(TAG, "Sending call alert: " + caller);
                    final String task_reference = "bluejay-wait-caller-id";
                    Inevitable.kill(task_reference);
                    Inevitable.task(task_reference, 200, () -> BlueJayEntry.sendNotifyIfEnabled(THINJAM_NOTIFY_TYPE_CALL, caller));
                }
            }

        } else {
            if (ringingNow) {
                ringingNow = false;
                UserError.Log.d(TAG, "Ringing stopped: " + stateExtra);
                if (JoH.ratelimit("incoming-call-stopped", 10)) {
                    if (BlueJayEntry.areCallAlertsEnabled()) {
                        BlueJayEntry.cancelNotifyIfEnabled();
                        MiBand.sendCall(MIBAND_NOTIFY_TYPE_CANCEL, "");
                    }
                }
            }
        }
    }


    // TODO this is still very incomplete
    private static String bestPhoneNumberFormatter(final String number) {
        if (number == null) return null;
        final String formatted = PhoneNumberUtils.formatNumber(number);
        if (formatted.contains(" ")) {
            return formatted;
        } else {
            final StringBuilder spaced = new StringBuilder();
            final char[] array = number.toCharArray();
            boolean first = true;
            int counter = 0;
            for (char c : array) {
                spaced.append(c);
                first = false;
                counter++;
                if (counter == 5) {
                    spaced.append(" ");
                }
            }
            return spaced.toString();
        }
    }

    public String getContactDisplayNameByNumber(final String number) {
        String name = "Unknown";
        try {
            final Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            final ContentResolver contentResolver = xdrip.getAppContext().getContentResolver();
            final Cursor cursor = contentResolver.query(uri, null, null, null, null);

            try {
                if (cursor != null && cursor.getCount() > 0) {
                    cursor.moveToNext();
                    name = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

        } catch (SecurityException e) {
            JoH.static_toast_long("xDrip needs contacts permission to get names from numbers");
        }
        return name;
    }

}

