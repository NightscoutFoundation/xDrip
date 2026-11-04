package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Information-free alert screen shown above the lock screen (without unlocking) when a BG
 * alert is raised while the screen is off or the device is locked.
 *
 * Its only purpose is to bring xDrip to the foreground so that the volume buttons can
 * snooze the alert. It shows the alert name above the xDrip icon and nothing else: no
 * buttons, so nothing can be snoozed by an accidental touch while the phone is in a
 * pocket, and no instructions, so a bystander learns nothing (review feedback in
 * discussion #4748). The volume-key confirmation toasts are suppressed here as well.
 *
 * It is the target of the alert notification's full-screen intent and, when "Display over
 * other apps" is granted, is also started directly by AlertPlayer. It runs in its own task:
 * closing it simply reveals the lock screen again. It closes itself when the alert is
 * snoozed (by the volume buttons here, or in any other way) or ends.
 */
public class SnoozeLockScreenActivity extends Activity {

    private static final String TAG = SnoozeLockScreenActivity.class.getSimpleName();

    /**
     * True when this alert should get the full-screen presentation: the alert overrides
     * silent mode or the Wake Screen setting is enabled.
     */
    public static boolean fullScreenWanted(final boolean overrideSilent) {
        return overrideSilent || Pref.getBooleanDefaultFalse("wake_phone_during_alerts");
    }

    /**
     * True when the device is in the state this screen is for; when the screen is on and
     * the device unlocked, the overlay snooze screen applies instead.
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
     * Directly show the lock-screen alert screen. Needs the "Display over other apps" grant
     * for the background activity start exemption; without it we rely on the notification's
     * full-screen intent alone. launchMode singleInstance dedupes when both mechanisms fire.
     */
    public static void launchIfWanted(final Context context, final boolean overrideSilent) {
        if (!fullScreenWanted(overrideSilent)) return;
        if (!screenOffOrLocked(context)) return;
        if (!Settings.canDrawOverlays(context)) {
            UserError.Log.d(TAG, "Not starting lock-screen alert screen directly as display over other apps is not granted - relying on the full-screen intent");
            return;
        }
        try {
            context.startActivity(new Intent(context, SnoozeLockScreenActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                            | Intent.FLAG_ACTIVITY_NO_USER_ACTION));
        } catch (Exception e) {
            UserError.Log.e(TAG, "Could not show lock-screen alert screen: " + e);
        }
    }

    private TextView alertName;

    // closes this screen as soon as the alert is no longer alerting (snoozed anywhere, or ended)
    private final SnoozeActivity.AlertEndWatcher alertEndWatcher = new SnoozeActivity.AlertEndWatcher(() -> {
        if (!isFinishing()) finish();
    });

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
        setContentView(R.layout.activity_alert_lockscreen);
        alertName = findViewById(R.id.alert_lockscreen_name);
        showAlertName();
        wakeUpScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAlertName();
        alertEndWatcher.start();
    }

    @Override
    protected void onPause() {
        alertEndWatcher.stop();
        super.onPause();
    }

    // a re-alert while this singleInstance activity is already the front task arrives here
    // without onCreate; refresh the name and nudge the screen back on in case it was turned off
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showAlertName();
        wakeUpScreen();
    }

    // the user's own name for the alerting alert type; blank when there is none (test alert)
    private void showAlertName() {
        String name = "";
        try {
            final AlertType alertType = ActiveBgAlert.alertTypegetOnly();
            if (alertType != null && alertType.name != null) name = alertType.name;
        } catch (Exception e) {
            //
        }
        alertName.setText(name);
    }

    private void wakeUpScreen() {
        // some devices need an explicit wake-up nudge when in deep sleep (self-releasing)
        JoH.fullWakeLock("lockscreen-alert-screen", 10 * Constants.SECOND_IN_MS);
    }

    // volume buttons snooze the alert (double press); quiet: no toasts on this screen
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        final boolean consume = SnoozeActivity.volumeKeyConsumed(event.getKeyCode());
        if (SnoozeActivity.volumeKeySnooze(event, true)) {
            finish();
        }
        if (consume) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (SnoozeActivity.volumeKeyConsumed(event.getKeyCode())) return true;
        return super.onKeyUp(keyCode, event);
    }
}
