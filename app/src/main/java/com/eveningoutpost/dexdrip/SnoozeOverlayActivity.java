package com.eveningoutpost.dexdrip;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import java.util.List;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * CHG1: Floating variant of the snooze screen, shown on top of the app the user is
 * currently using when a BG alert is raised ("Display over other apps").
 *
 * It runs in its own task with a floating (non-fullscreen) theme: the app behind it keeps
 * running and keeps control of the display orientation, and closing this screen simply
 * reveals the task below it, which restores the previously used app automatically.
 *
 * See Documentation/changes/CHG1-snooze-over-other-apps.md
 */
public class SnoozeOverlayActivity extends SnoozeActivity {

    private static final String TAG = SnoozeOverlayActivity.class.getSimpleName();

    public static final String PREF_SNOOZE_OVER_OTHER_APPS = "snooze_over_other_apps";

    /**
     * Called whenever a BG alert is raised or re-raised. Shows the snooze screen over the
     * current foreground app when the feature is enabled and the situation allows it.
     */
    public static void launchIfEnabled(final Context context) {
        // CHG7 A5: when xDrip itself is the foreground app, no overlay permission or
        // preference is needed (also covers testing alerts from within the app)
        if (!isXdripForeground(context)) {
            if (!Pref.getBoolean(PREF_SNOOZE_OVER_OTHER_APPS, true)) return; // CHG13 ER1: default on
            if (!Settings.canDrawOverlays(context)) {
                UserError.Log.d(TAG, "Not showing snooze screen over other apps as the permission is not granted");
                return;
            }
        }
        if (!JoH.isScreenOn()) return; // screen off: notification / full-screen intent path covers this
        try {
            final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null && keyguardManager.isKeyguardLocked()) return; // never float above the lock screen
        } catch (Exception e) {
            // continue without keyguard information
        }
        if (Pref.getBooleanDefaultFalse("no_alarms_during_calls") && JoH.isOngoingCall()) return;
        try {
            // the SYSTEM_ALERT_WINDOW grant exempts us from background activity start restrictions (Android 10+)
            context.startActivity(new Intent(context, SnoozeOverlayActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Could not show snooze screen over other apps: " + e);
        }
    }

    // CHG7 A5: true when xDrip's own process is the foreground app
    private static boolean isXdripForeground(final Context context) {
        try {
            final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager == null) return false;
            final List<ActivityManager.RunningAppProcessInfo> processes = activityManager.getRunningAppProcesses();
            if (processes == null) return false;
            for (ActivityManager.RunningAppProcessInfo process : processes) {
                if (process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                        && context.getPackageName().equals(process.processName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            //
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // The floating window measures its content as wrap_content but the DrawerLayout in
        // the snooze layout must be measured exactly, so size the window to the full screen.
        getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
    }

    // CHG6 BVD1: the alarm pop-up is modal - no navigation drawer
    @Override
    protected void onResume() {
        super.onResume();
        lockNavigationDrawer();
    }

    @Override
    protected void applyThemeChoice() {
        // keep the floating SnoozeOverlayTheme from the manifest; an opaque fullscreen theme
        // would stop the app behind us and take over its display orientation
    }

    @Override
    protected boolean openHomeAfterSnooze() {
        return false; // finishing this task reveals the app the user was using
    }
}
