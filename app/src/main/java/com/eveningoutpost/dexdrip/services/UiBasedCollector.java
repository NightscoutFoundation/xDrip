package com.eveningoutpost.dexdrip.services;

import static com.eveningoutpost.dexdrip.cgm.dex.ClassifierAction.lastReadingTimestamp;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
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
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.alert.Persist;
import com.eveningoutpost.dexdrip.cgm.dex.BlueTails;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Unitized;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
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
    private static final String COMPANION_APP_IOB_ENABLED_PREFERENCE_KEY = "fetch_iob_from_companion_app";
    private static final Persist.DoubleTimeout iob_store =
            new Persist.DoubleTimeout("COMPANION_APP_IOB_VALUE", Constants.MINUTE_IN_MS * 5);
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final HashSet<String> coOptedPackages = new HashSet<>();
    private static final HashSet<String> coOptedPackagesAll = new HashSet<>();
    private static final HashSet<String> companionAppIoBPackages = new HashSet<>();
    private static final HashSet<Pattern> companionAppIoBRegexes = new HashSet<>();
    private static boolean debug = false;

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
        coOptedPackages.add("com.medtronic.diabetes.guardianconnect");
        coOptedPackages.add("com.medtronic.diabetes.guardianconnect.us");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.eu");
        coOptedPackages.add("com.medtronic.diabetes.minimedmobile.us");

        coOptedPackagesAll.add("com.dexcom.dexcomone");
        coOptedPackagesAll.add("com.medtronic.diabetes.guardian");

        companionAppIoBPackages.add("com.insulet.myblue.pdm");

        // The IoB value should be captured into the first match group.
        // English localization of the Omnipod 5 App
        companionAppIoBRegexes.add(Pattern.compile("IOB: ([\\d\\.,]+) U"));
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        val fromPackage = sbn.getPackageName();
        if (coOptedPackages.contains(fromPackage)) {
            if (getDexCollectionType() == UiBased) {
                UserError.Log.d(TAG, "Notification from: " + fromPackage);
                if (sbn.isOngoing() || coOptedPackagesAll.contains(fromPackage)) {
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

        if (companionAppIoBPackages.contains(fromPackage)) {
            processCompanionAppIoBNotification(sbn.getNotification());
        }
    }

    private void processCompanionAppIoBNotification(final Notification notification) {
        if (notification == null) {
            UserError.Log.e(TAG, "Null notification");
            return;
        }
        if (notification.contentView != null) {
            processCompanionAppIoBNotificationCV(notification.contentView);
        } else {
            UserError.Log.e(TAG, "Content is empty");
        }
    }

    private void processCompanionAppIoBNotificationCV(final RemoteViews cview) {
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
                iob = parseIoB(text);
                if (iob != null) {
                    break;
                }
            }

            if (iob != null) {
                if (debug) UserError.Log.d(TAG, "Inserting new IoB value: " + iob);
                iob_store.set(iob);
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "exception in processCompanionAppIoBNotificationCV: " + e);
        }

        texts.clear();
    }
    Double parseIoB(final String value) {
        for (Pattern pattern : companionAppIoBRegexes) {
            Matcher matcher = pattern.matcher(value);

            if (matcher.find()) {
                return JoH.tolerantParseDouble(matcher.group(1));
            }
        }

        return null;
    }

    public static Double getCurrentIoB() {
        return iob_store.get();
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
                    return (basicFilterString(arrowFilterString(value)))
                        .trim();
        }
    }

    String basicFilterString(final String value) {
        return value
                .replace("\u00a0"," ")
                .replace("\u2060","")
                .replace("\\","/")
                .replace("mmol/L", "")
                .replace("mmol/l", "")
                .replace("mg/dL", "")
                .replace("mg/dl", "")
                .replace("≤", "")
                .replace("≥", "");
    }

    String arrowFilterString(final String value) {
        return filterUnicodeRange(filterUnicodeRange(filterUnicodeRange(filterUnicodeRange(value,
                '\u2190', '\u21FF'),
                '\u2700', '\u27BF'),
                '\u2900', '\u297F'),
                '\u2B00', '\u2BFF');
    }

    public String filterUnicodeRange(final String input, final char bottom, final char top) {
        if (bottom > top) {
            throw new RuntimeException("bottom and top of character range invalid");
        }
        val filtered = new StringBuilder(input.length());
        for (final char c : input.toCharArray()) {
            if (c < bottom || c > top) {
                filtered.append(c);
            }
        }
        return filtered.toString();
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
            val timestamp = JoH.tsl();
            handleNewValue(timestamp, mgdl);
        }
        texts.clear();
    }

    boolean handleNewValue(final long timestamp, final int mgdl) {
        Sensor.createDefaultIfMissing();

        UserError.Log.d(TAG, "Found specific value: " + mgdl);

        if ((mgdl >= 40 && mgdl <= 405)) {
            val grace = DexCollectionType.getCurrentSamplePeriod() * 4;
            val recentbt = msSince(lastReadingTimestamp) < grace;
            val dedupe = (!recentbt && isDifferentToLast(mgdl)) ? Constants.SECOND_IN_MS * 10
                    : DexCollectionType.getCurrentDeduplicationPeriod();
            val period = recentbt ? grace : dedupe;
            val existing = BgReading.getForPreciseTimestamp(timestamp, period, false);
            if (existing == null) {
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
                        return true;
                    }
                }
            } else {
                UserError.Log.d(TAG, "Duplicate value: "+existing.timeStamp());
            }
        } else {
            UserError.Log.wtf(TAG, "Glucose value outside acceptable range: " + mgdl);
        }
        return false;
    }

    static boolean isValidMmol(final String text) {
        return text.matches("[0-9]+[.,][0-9]+");
    }

    private boolean shouldAllowTimeOffsetChange(final int mgdl) {
        return isDifferentToLast(mgdl); // TODO do we need to rate limit this or not?
    }
    // note this method only checks existing stored data
    private boolean isDifferentToLast(final int mgdl) {
            val previousValue = PersistentStore.getLong(UI_BASED_STORE_LAST_VALUE);
            return previousValue != mgdl;
    }

    // note this method actually updates the stored value
    private boolean isJammed(final int mgdl) {
        val previousValue = PersistentStore.getLong(UI_BASED_STORE_LAST_VALUE);
        if (previousValue == mgdl) {
            PersistentStore.incrementLong(UI_BASED_STORE_LAST_REPEAT);
        } else {
            PersistentStore.setLong(UI_BASED_STORE_LAST_REPEAT, 0);
        }
        val lastRepeat = PersistentStore.getLong(UI_BASED_STORE_LAST_REPEAT);
        UserError.Log.d(TAG, "Last repeat: " + lastRepeat);
        return lastRepeat > jamThreshold();
    }

    private int jamThreshold() {
        if (lastPackage != null) {
            if (lastPackage.startsWith("com.medtronic")) return 9;
        }
        return 6;
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
            if (key.equals(COMPANION_APP_IOB_ENABLED_PREFERENCE_KEY)) {
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
