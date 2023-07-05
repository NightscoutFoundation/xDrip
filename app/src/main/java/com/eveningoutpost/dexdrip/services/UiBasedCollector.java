package com.eveningoutpost.dexdrip.services;

import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.lastReadingTimestamp;
import static com.eveningoutpost.dexdrip.utilitymodels.Constants.MINUTE_IN_MS;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.UiBased;
import static com.eveningoutpost.dexdrip.utils.DexCollectionType.getDexCollectionType;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import android.app.Activity;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.cgm.dex.BlueTails;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.val;

/**
 * JamOrHam
 * UI Based Collector
 */

public class UiBasedCollector extends NotificationListenerService {

    private static final String TAG = UiBasedCollector.class.getSimpleName();
    private static final String UI_BASED_STORE_LAST_VALUE = "UI_BASED_STORE_LAST_VALUE";
    private static final String UI_BASED_STORE_LAST_REPEAT = "UI_BASED_STORE_LAST_REPEAT";
    private static final String OMNIPOD_IOB_ENABLED_PREFERENCE_KEY = "fetch_iob_from_omnipod_app";
    private static final String OMNIPOD_IOB_VALUE = "OMNIPOD_IOB_VALUE";
    private static final String OMNIPOD_IOB_VALUE_WRITE_TIMESTAMP = "OMNIPOD_IOB_VALUE_WRITE_TIMESTAMP";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final HashSet<String> coOptedPackages = new HashSet<>();
    private static final HashSet<String> omnipodIoBPackages = new HashSet<>();
    private static boolean debug = true;

    @VisibleForTesting
    String lastPackage;

    static {
        coOptedPackages.add("com.dexcom.g6");
        coOptedPackages.add("com.dexcom.g6.region1.mmol");
        coOptedPackages.add("com.dexcom.g6.region3.mgdl");
        coOptedPackages.add("com.dexcom.dexcomone");
        coOptedPackages.add("com.dexcom.g7");
        coOptedPackages.add("com.camdiab.fx_alert.mmoll");
        coOptedPackages.add("com.camdiab.fx_alert.mgdl");
        coOptedPackages.add("com.camdiab.fx_alert.hx.mmoll");
        coOptedPackages.add("com.camdiab.fx_alert.hx.mgdl");
        coOptedPackages.add("com.medtronic.diabetes.guardian");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.eu");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.us");

        omnipodIoBPackages.add("com.insulet.myblue.pdm");
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        val fromPackage = sbn.getPackageName();
        if (coOptedPackages.contains(fromPackage)) {
            if (getDexCollectionType() == UiBased) {
                UserError.Log.d(TAG, "Notification from: " + fromPackage);
                if (sbn.isOngoing() || fromPackage.endsWith("e")) {
                    lastPackage = fromPackage;
                    processNotification(sbn.getNotification());
                    BlueTails.immortality();
                }
            } else {
                if (JoH.pratelimit("warn-notification-access", 7200)) {
                    UserError.Log.wtf(TAG, "Receiving notifications that we are not enabled to process: " + fromPackage);
                }
            }
        }

        if (omnipodIoBPackages.contains(fromPackage)) {
            processOmnipodOmnipodNotification(sbn.getNotification());
        }
    }

    private void processOmnipodOmnipodNotification(final Notification notification) {
        if (notification == null) {
            UserError.Log.e(TAG, "Null notification");
            return;
        }
        if (notification.contentView != null) {
            processOmnipodNotificationCV(notification.contentView);
        } else {
            UserError.Log.e(TAG, "Content is empty");
        }
    }

    private void processOmnipodNotificationCV(final RemoteViews cview) {
        if (cview == null) return;
        val applied = cview.apply(this, null);
        val root = (ViewGroup) applied.getRootView();
        val texts = new ArrayList<TextView>();
        getTextViews(texts, root);
        if (debug) UserError.Log.d(TAG, "Text views: " + texts.size());
        Double iob = null;
        try {
            for (val view : texts) {
                val tv = (TextView) view;
                String text = tv.getText() != null ? tv.getText().toString() : "";
                val desc = tv.getContentDescription() != null ? tv.getContentDescription().toString() : "";
                if (debug) UserError.Log.d(TAG, "Examining: >" + text + "< : >" + desc + "<");
                iob = parseOmnipodIoB(text);
                if (iob != null) {
                    break;
                }
            }

            if (iob != null) {
                if (debug) UserError.Log.d(TAG, "Inserting new IoB value: " + iob);
                PersistentStore.setDouble(OMNIPOD_IOB_VALUE, iob);
                long now = System.currentTimeMillis();
                PersistentStore.setLong(OMNIPOD_IOB_VALUE_WRITE_TIMESTAMP, now);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "exception in processOmnipodNotificationCV: " + e);
        }

        texts.clear();
    }
    Double parseOmnipodIoB(final String value) {
        if (!value.contains("IOB:")) {
            return null;
        }

        Pattern pattern = Pattern.compile("IOB: ([\\d\\.]+) U");
        Matcher matcher = pattern.matcher(value);

        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }

        return 0.0;
    }

    public static Double getCurrentIoB() {
        Long iobWriteTimestamp = PersistentStore.getLong(OMNIPOD_IOB_VALUE_WRITE_TIMESTAMP);
        if (debug) {
            Date date = new Date(iobWriteTimestamp);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(date);
            UserError.Log.d(TAG, "iow write time: " + timestamp);
        }

        Long now = System.currentTimeMillis();

        if (iobWriteTimestamp < now - 5 * MINUTE_IN_MS) {
            return null;
        }

        Double iob = PersistentStore.getDouble(OMNIPOD_IOB_VALUE);
        return iob;
    }

    @Override
    public void onNotificationRemoved(final StatusBarNotification sbn) {
        //
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    private void processNotification(final Notification notification) {
        if (notification == null) {
            UserError.Log.e(TAG, "Null notification");
            return;
        }
        JoH.dumpBundle(notification.extras, TAG);
        if (notification.contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val cid = notification.getChannelId();
                UserError.Log.d(TAG, "Channel ID: " + cid);
            }
            processRemote(notification.contentView);
        } else {
            UserError.Log.e(TAG, "Content is empty");
        }
    }

    String filterString(final String value) {
        if (lastPackage == null) return value;
        switch (lastPackage) {
            default:
                return value
                        .replace("mmol/L", "")
                        .replace("mmol/l", "")
                        .replace("mg/dL", "")
                        .replace("mg/dl", "")
                        .replace("≤", "")
                        .replace("≥", "")
                        .trim();
        }
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    private void processRemote(final RemoteViews cview) {
        if (cview == null) return;
        val applied = cview.apply(this, null);
        val root = (ViewGroup) applied.getRootView();
        val texts = new ArrayList<TextView>();
        getTextViews(texts, root);
        UserError.Log.d(TAG, "Text views: " + texts.size());
        int matches = 0;
        int mgdl = 0;
        for (val view : texts) {
            try {
                val tv = (TextView) view;
                val text = tv.getText() != null ? tv.getText().toString() : "";
                val desc = tv.getContentDescription() != null ? tv.getContentDescription().toString() : "";
                UserError.Log.d(TAG, "Examining: >" + text + "< : >" + desc + "<");
                val ftext = filterString(text);
                if (Unitized.usingMgDl()) {
                    mgdl = Integer.parseInt(ftext);
                    if (mgdl > 0) {
                        matches++;
                    }
                } else {
                    if (isValidMmol(ftext)) {
                        val result = JoH.tolerantParseDouble(ftext, -1);
                        if (result != -1) {
                            mgdl = (int) Math.round(Unitized.mgdlConvert(result));
                            if (mgdl > 0) {
                                matches++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                //
            }
        }
        if (matches == 0) {
            UserError.Log.e(TAG, "Did not find any matches");
        } else if (matches > 1) {
            UserError.Log.e(TAG, "Found too many matches: " + matches);
        } else {
            Sensor.createDefaultIfMissing();
            val timestamp = JoH.tsl();
            UserError.Log.d(TAG, "Found specific value: " + mgdl);

            if ((mgdl >= 40 && mgdl <= 405)) {
                val grace = DexCollectionType.getCurrentSamplePeriod() * 4;
                val recent = msSince(lastReadingTimestamp) < grace;
                val period = recent ? grace : DexCollectionType.getCurrentDeduplicationPeriod();
                if (BgReading.getForPreciseTimestamp(timestamp, period, false) == null) {
                    if (isJammed(mgdl)) {
                        UserError.Log.wtf(TAG, "Apparently value is jammed at: " + mgdl);
                    } else {
                        UserError.Log.d(TAG, "Inserting new value");
                        PersistentStore.setLong(UI_BASED_STORE_LAST_VALUE, mgdl);
                        val bgr = BgReading.bgReadingInsertFromG5(mgdl, timestamp);
                        if (bgr != null) {
                            bgr.find_slope();
                            bgr.noRawWillBeAvailable();
                            bgr.injectDisplayGlucose(BestGlucose.getDisplayGlucose());
                        }
                    }
                } else {
                    UserError.Log.d(TAG, "Duplicate value");
                }
            } else {
                UserError.Log.wtf(TAG, "Glucose value outside acceptable range: " + mgdl);
            }
        }
        texts.clear();
    }

    static boolean isValidMmol(final String text) {
        return text.matches("[0-9]+[.,][0-9]+");
    }

    private boolean isJammed(final int mgdl) {
        val previousValue = PersistentStore.getLong(UI_BASED_STORE_LAST_VALUE);
        if (previousValue == mgdl) {
            PersistentStore.incrementLong(UI_BASED_STORE_LAST_REPEAT);
        } else {
            PersistentStore.setLong(UI_BASED_STORE_LAST_REPEAT, 0);
        }
        val lastRepeat = PersistentStore.getLong(UI_BASED_STORE_LAST_REPEAT);
        UserError.Log.d(TAG, "Last repeat: " + lastRepeat);
        return lastRepeat > 3;
    }

    private void getTextViews(final List<TextView> output, final ViewGroup parent) {
        val children = parent.getChildCount();
        for (int i = 0; i < children; i++) {
            val view = parent.getChildAt(i);
            if (view.getVisibility() == View.VISIBLE) {
                if (view instanceof TextView) {
                    output.add((TextView) view);
                } else if (view instanceof ViewGroup) {
                    getTextViews(output, (ViewGroup) view);
                }
            }
        }
    }

    public static void onEnableCheckPermission(final Activity activity) {
        if (DexCollectionType.getDexCollectionType() == UiBased) {
            UserError.Log.d(TAG, "Detected that we are enabled");
            switchToAndEnable(activity);
        }
    }

    public static SharedPreferences.OnSharedPreferenceChangeListener getListener(final Activity activity) {
        return (prefs, key) -> {
            if (key.equals(DexCollectionType.DEX_COLLECTION_METHOD)) {
                try {
                    onEnableCheckPermission(activity);
                } catch (Exception e) {
                    //
                }
            }
            if (key.equals(OMNIPOD_IOB_ENABLED_PREFERENCE_KEY)) {
                try {
                    enableNotificationService(activity);
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Exception when enabling NotificationService: " + e);
                }
            }
        };
    }

    public static void switchToAndEnable(final Activity activity) {
        DexCollectionType.setDexCollectionType(UiBased);
        Sensor.createDefaultIfMissing();
        enableNotificationService(activity);
    }

    private static void enableNotificationService(final Activity activity) {
        if (!isNotificationServiceEnabled()) {
            JoH.show_ok_dialog(activity, gs(R.string.please_allow_permission),
                    "Permission is needed to receive data from other applications. xDrip does not do anything beyond this scope. Please enable xDrip on the next screen",
                    () -> activity.startActivity(new Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        }
    }

    private static boolean isNotificationServiceEnabled() {
        val pkgName = xdrip.getAppContext().getPackageName();
        val flat = Settings.Secure.getString(xdrip.getAppContext().getContentResolver(),
                ENABLED_NOTIFICATION_LISTENERS);
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":");
            for (val name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
