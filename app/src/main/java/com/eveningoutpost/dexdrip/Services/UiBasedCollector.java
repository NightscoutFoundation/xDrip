package com.eveningoutpost.dexdrip.Services;

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

import com.eveningoutpost.dexdrip.BestGlucose;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Unitized;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import lombok.val;

/**
 * JamOrHam
 * UI Based Collector
 */

public class UiBasedCollector extends NotificationListenerService {

    private static final String TAG = UiBasedCollector.class.getSimpleName();
    private static final String UI_BASED_STORE_LAST_VALUE = "UI_BASED_STORE_LAST_VALUE";
    private static final String UI_BASED_STORE_LAST_REPEAT = "UI_BASED_STORE_LAST_REPEAT";
    private static final String ENABLED_NOTIFICATION_LISTENERS = "enabled_notification_listeners";
    private static final String ACTION_NOTIFICATION_LISTENER_SETTINGS = "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";

    private static final HashSet<String> coOptedPackages = new HashSet<>();

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
    }

    @Override
    public void onNotificationPosted(final StatusBarNotification sbn) {
        val fromPackage = sbn.getPackageName();
        if (coOptedPackages.contains(fromPackage)) {
            if (getDexCollectionType() == UiBased) {
                UserError.Log.d(TAG, "Notification from: " + fromPackage);
                if (sbn.isOngoing()) {
                    processNotification(sbn.getNotification());
                }
            } else {
                if (JoH.pratelimit("warn-notification-access", 7200)) {
                    UserError.Log.wtf(TAG, "Receiving notifications that we are not enabled to process");
                }
            }
        }
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
                if (Unitized.usingMgDl()) {
                    mgdl = Integer.parseInt(text);
                    if (mgdl > 0) {
                        matches++;
                    }
                } else {
                    if (isValidMmol(text)) {
                        val result = JoH.tolerantParseDouble(text, -1);
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

            if ((mgdl >= 40 && mgdl <= 400)) {
                if (BgReading.getForPreciseTimestamp(timestamp, DexCollectionType.getCurrentDeduplicationPeriod(), false) == null) {
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
        };
    }

    public static void switchToAndEnable(final Activity activity) {
        DexCollectionType.setDexCollectionType(UiBased);
        Sensor.createDefaultIfMissing();
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
