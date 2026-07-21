package com.eveningoutpost.dexdrip;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * CHG2: Full-screen variant of the snooze screen that turns the screen on and is shown
 * above the lock screen (without unlocking) when a BG alert is raised while the screen is
 * off or the device is locked.
 *
 * It is the target of the alert notification's full-screen intent and, when "Display over
 * other apps" is granted, is also started directly by AlertPlayer. It runs in its own
 * task: closing it simply reveals the lock screen again.
 *
 * See Documentation/changes/CHG2-fullscreen-snooze-on-lockscreen.md
 */
public class SnoozeLockScreenActivity extends SnoozeActivity {

    private static final String TAG = SnoozeLockScreenActivity.class.getSimpleName();

    /**
     * True when this alert should get the full-screen snooze presentation: the alert
     * overrides silent mode or the Wake Screen setting is enabled.
     */
    public static boolean fullScreenWanted(final boolean overrideSilent) {
        return overrideSilent || Pref.getBooleanDefaultFalse("wake_phone_during_alerts");
    }

    /**
     * True when the device is in the state this variant is for; when the screen is on and
     * the device unlocked, the CHG1 overlay variant applies instead.
     */
    public static boolean screenOffOrLocked(final Context context) {
        if (!JoH.isScreenOn()) return true;
        try {
            final KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return keyguardManager != null && keyguardManager.isKeyguardLocked();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Directly show the lock-screen snooze screen. Needs the "Display over other apps"
     * grant for the background activity start exemption; without it we rely on the
     * notification's full-screen intent alone. launchMode singleInstance dedupes when both
     * mechanisms fire.
     */
    public static void launchIfWanted(final Context context, final boolean overrideSilent) {
        if (!fullScreenWanted(overrideSilent)) return;
        if (!screenOffOrLocked(context)) return;
        if (!Settings.canDrawOverlays(context)) {
            UserError.Log.d(TAG, "Not starting lock-screen snooze directly as display over other apps is not granted - relying on the full-screen intent");
            return;
        }
        try {
            context.startActivity(new Intent(context, SnoozeLockScreenActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Could not show lock-screen snooze screen: " + e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Show above the keyguard without unlocking and turn the screen on - the same
        // pattern as Reminders.wakeUpScreen(). Deliberately no FLAG_DISMISS_KEYGUARD:
        // the device itself stays locked.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        wakeUpScreen();
    }

    // CHG6 BVD1: the alarm pop-up is modal and shown without unlocking - no navigation drawer
    @Override
    protected void onResume() {
        super.onResume();
        lockNavigationDrawer();
    }

    // CHG6 BVD5: a re-alert while this singleInstance activity is already the front task
    // arrives here without onCreate; nudge the screen back on in case it was turned off
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        wakeUpScreen();
    }

    private void wakeUpScreen() {
        // some devices need an explicit wake-up nudge when in deep sleep (self-releasing)
        JoH.fullWakeLock("chg2-lockscreen-snooze", 10 * Constants.SECOND_IN_MS);
    }

    @Override
    protected boolean openHomeAfterSnooze() {
        return false; // finishing reveals the lock screen (or whatever was underneath)
    }
}
