package com.eveningoutpost.dexdrip;

import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.Date;
import java.util.concurrent.CopyOnWriteArrayList;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.services.MissedReadingService;
import com.eveningoutpost.dexdrip.utilitymodels.AlertPlayer;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Notifications;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import static com.eveningoutpost.dexdrip.xdrip.gs;

public class SnoozeActivity extends ActivityWithMenu {
    //public static String menu_name = "Snooze Alert";
    private static String status;

    // Snooze types supported by this class
    public enum SnoozeType {
        ALL_ALERTS("alerts_disabled_until"),
        LOW_ALERTS("low_alerts_disabled_until"),
        HIGH_ALERTS("high_alerts_disabled_until")
        ;

        private final String prefKey;
        SnoozeType(String prefKey) {
            this.prefKey = prefKey;
        }

        public String getPrefKey() {
            return prefKey;
        }
    }

    /**
     * Snoozes a given type for the specified number of minutes.
     */
    public static void snoozeForType(long minutes, SnoozeType disableType, SharedPreferences prefs) {
        if (minutes == -1) {
            minutes = infiniteSnoozeValueInMinutes;
        }
        long disableUntil = new Date().getTime() + minutes * 1000 * 60;

        prefs.edit().putLong(disableType.getPrefKey(), disableUntil).apply();

        //check if active bg alert exists and delete it depending on type of alert
        ActiveBgAlert aba = ActiveBgAlert.getOnly();
        if (aba != null) {
            AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
            // CHG7 A1: activeBgAlert can be null (test alert, removed alert type)
            if (disableType == SnoozeType.ALL_ALERTS
                    || (activeBgAlert != null && activeBgAlert.above && disableType == SnoozeType.HIGH_ALERTS)
                    || (activeBgAlert != null && !activeBgAlert.above && disableType == SnoozeType.LOW_ALERTS)
            ) {
                //active bg alert exists which is a type that is being disabled so let's remove it completely from the database
                ActiveBgAlert.ClearData();
            }
        }

        if (disableType == SnoozeType.ALL_ALERTS) {
            //disabling all , after the Snooze time set, all alarms will be re-enabled, inclusive low and high bg alarms
            prefs.edit().putLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0).apply();
            prefs.edit().putLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0).apply();
        }
        recheckAlerts();
    }


    TextView alertStatus;
    Button buttonSnooze;
    Button disableAlerts;
    Button clearDisabled;
    Button disableLowAlerts;
    Button clearLowDisabled;
    Button disableHighAlerts;
    Button clearHighDisabled;
    Button sendRemoteSnooze;
    SharedPreferences prefs;
    boolean doMgdl;

    NumberPicker snoozeValue;

    static final long infiniteSnoozeValueInMinutes = 5256000;//10 years
    //static final int snoozeValues[] = new int []{5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360, 420, 480, 540, 600};
    static final int snoozeValues[] = new int []{ 10, 15, 20, 30, 40, 50, 60, 75, 90, 120, 150, 180, 240, 300, 360, 420, 480, 540, 600, 720};

    static int getSnoozeLocation(int time) {
        for (int i=0; i < snoozeValues.length; i++) {
            if(time == snoozeValues[i]) {
                return i;
            } else if (time < snoozeValues[i]) {
                // we are in the middle of two, return the smaller
                if (i == 0) {
                    return 0;
                }
                return i-1;
            }
        }
        return snoozeValues.length-1;
    }

    static String getNameFromTime(int time) {
        if (time < 120) {
            return time + " minutes";
        }
        return (time / 60.0) + " hours";
    }

    static int getTimeFromSnoozeValue(int pickedNumber) {
        return snoozeValues[pickedNumber];
    }

    static public int getDefaultSnooze(boolean above) {
        if (above) {
            return 120;
        }
        return 35;
    }

    static void SetSnoozePickerValues(NumberPicker picker, boolean above, int default_snooze) {
        String[] values=new String[snoozeValues.length];
        for(int i=0;i<values.length;i++){
            values[i]=getNameFromTime(snoozeValues[i]);
        }

        picker.setMaxValue(values.length -1);
        picker.setMinValue(0);
        picker.setDisplayedValues(values);
        picker.setWrapSelectorWheel(false);
        if(default_snooze != 0) {
            picker.setValue(getSnoozeLocation(default_snooze));
        } else {
            picker.setValue(getSnoozeLocation(getDefaultSnooze(above)));
        }
    }


    private final static String TAG = AlertPlayer.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyThemeChoice(); // CHG1
        JoH.fixActionBar(this);
        setContentView(R.layout.activity_snooze);
        alertStatus = (TextView) findViewById(R.id.alert_status);
        snoozeValue = (NumberPicker) findViewById(R.id.snooze);

        prefs =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        addListenerOnButton();
        if(BgGraphBuilder.isXLargeTablet(getApplicationContext())) {
            alertStatus.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
            buttonSnooze.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }
        displayStatus();
        registerOverlayInstance(); // CHG6 BVD2
    }

    // CHG1: overridden by SnoozeOverlayActivity to keep the floating overlay theme from the manifest
    protected void applyThemeChoice() {
        if (Home.get_holo()) { setTheme(R.style.OldAppThemeNoTitleBar); }
    }

    // CHG1: overridden by SnoozeOverlayActivity so that closing the snooze screen restores the previous app
    protected boolean openHomeAfterSnooze() {
        return true;
    }

    // CHG6 BVD1: the overlay and lock-screen variants disable the navigation drawer; the
    // alarm pop-ups are modal and on the lock screen the menu must not open without unlocking
    protected void lockNavigationDrawer() {
        try {
            ((DrawerLayout) findViewById(R.id.drawer_layout)).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        } catch (Exception e) {
            Log.e(TAG, "Could not lock navigation drawer: " + e);
        }
    }

    // CHG6 BVD2: live overlay-mode snooze screens (openHomeAfterSnooze() == false) so they
    // can be closed when the alert is snoozed or cleared through another channel
    private static final CopyOnWriteArrayList<WeakReference<SnoozeActivity>> overlayInstances = new CopyOnWriteArrayList<>();

    private void registerOverlayInstance() {
        if (!openHomeAfterSnooze()) {
            overlayInstances.add(new WeakReference<SnoozeActivity>(this));
        }
    }

    private void unregisterOverlayInstance() {
        for (WeakReference<SnoozeActivity> ref : overlayInstances) {
            final SnoozeActivity instance = ref.get();
            if (instance == null || instance == this) {
                overlayInstances.remove(ref);
            }
        }
    }

    @Override
    protected void onDestroy() {
        unregisterOverlayInstance(); // CHG6 BVD2
        super.onDestroy();
    }

    /**
     * CHG6 BVD2+BVD3: called by AlertPlayer whenever the alert is snoozed or cleared,
     * through whatever channel (buttons, notification, watch, remote, automation, glucose
     * recovery). Closes any open overlay / lock-screen snooze screen so the previous app
     * or lock screen is restored, and clears the volume-key double-press state.
     */
    public static void alertEnded() {
        resetVolumeKeyConfirmation();
        for (WeakReference<SnoozeActivity> ref : overlayInstances) {
            final SnoozeActivity instance = ref.get();
            if (instance != null && !instance.isFinishing()) {
                // CHG13 ER3: re-check at execution time - on an alert hand-over
                // (startAlert clears the old record and immediately creates a new,
                // un-snoozed one) the screen must stay for the new alert instead of
                // being closed by the clean-up of the old one
                JoH.runOnUiThread(() -> {
                    if (!ActiveBgAlert.currentlyAlerting() && !instance.isFinishing()) {
                        instance.finish();
                    }
                });
            }
        }
    }

    // CHG6 BVD3: clear a pending first press and dismiss its confirmation toast
    public static void resetVolumeKeyConfirmation() {
        pendingVolumeKeyCode = -1;
        pendingVolumeKeyUuid = null; // CHG7 A2
        JoH.runOnUiThread(SnoozeActivity::cancelVolumeKeyConfirmToast);
    }

    // CHG4: double-press confirmation state for the volume-key snooze, shared by all screens
    private static final long VOLUME_KEY_CONFIRM_WINDOW_MS = 1500;
    private static int pendingVolumeKeyCode = -1;
    private static long pendingVolumeKeySince = 0;
    private static String pendingVolumeKeyUuid; // CHG7 A2: the alert the first press was for
    private static Toast volumeKeyConfirmToast;

    /**
     * CHG3: volume-key snooze ("Buttons silence alarms"), shared by Home and all snooze
     * screens. Returns true when an active alert was actually snoozed. Callers use
     * volumeKeyConsumed() to decide whether the key event itself is consumed (CHG4
     * addendum A: no volume changes while an alarm is alerting).
     *
     * CHG4: snoozing requires pressing the same volume button twice within 1.5 seconds
     * while the alert is actually alerting; the first press shows a confirmation toast
     * which is dismissed the moment the second press arrives. Auto-repeat events from a
     * held button do not count as a second press.
     */
    public static boolean volumeKeySnooze(final KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                if (event.getRepeatCount() != 0) break; // held button is not a double press
                final ActiveBgAlert aba = ActiveBgAlert.getOnly();
                if (aba == null) break;
                // CHG7 A1: clean up an orphaned record (alert type missing, e.g. after a test
                // alert or a removed alert type) so the keys return to normal volume control
                // (CHG13 ER4: placed before the enabled check so the cleanup also runs while
                // the overlay option is off)
                if (aba.is_snoozed && ActiveBgAlert.alertTypegetOnly(aba) == null) {
                    ActiveBgAlert.ClearData();
                    recheckAlerts();
                    break;
                }
                if (!volumeKeySnoozeEnabled()) break; // CHG11
                if (aba.is_snoozed) break; // nothing to do when already snoozed
                final long now = JoH.tsl();
                if (event.getKeyCode() == pendingVolumeKeyCode
                        // CHG7 A2: the second press only counts for the same alert
                        && aba.alert_uuid != null && aba.alert_uuid.equals(pendingVolumeKeyUuid)
                        && (now - pendingVolumeKeySince) <= VOLUME_KEY_CONFIRM_WINDOW_MS) {
                    pendingVolumeKeyCode = -1;
                    pendingVolumeKeyUuid = null;
                    cancelVolumeKeyConfirmToast();
                    AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
                    JoH.static_toast_long(volumeKeySnoozedText(event.getKeyCode())); // CHG7 A5 / CHG15
                    Log.ueh(TAG, "Snoozing alert due to double volume button press");
                    return true;
                }
                // first press, a different volume key, or the window expired: (re)arm
                // CHG7 A5: point out explicitly when a different button was pressed within the window
                final boolean wrongButton = pendingVolumeKeyCode != -1
                        && pendingVolumeKeyCode != event.getKeyCode()
                        && (now - pendingVolumeKeySince) <= VOLUME_KEY_CONFIRM_WINDOW_MS;
                pendingVolumeKeyCode = event.getKeyCode();
                pendingVolumeKeyUuid = aba.alert_uuid; // CHG7 A2
                pendingVolumeKeySince = now;
                showVolumeKeyConfirmToast(wrongButton
                        ? "Double press the same volume button to snooze" // CHG15 literal
                        : volumeKeyConfirmText(event.getKeyCode()));
                break;
        }
        return false;
    }

    // CHG7 A5: per-button confirmation text for the first press (CHG15: English literals
    // pending the follow-up strings PR)
    private static String volumeKeyConfirmText(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return "Press the volume DOWN button again to snooze";
            case KeyEvent.KEYCODE_VOLUME_UP:
                return "Press the volume UP button again to snooze";
            default:
                return "Press the same volume button again to snooze the alert";
        }
    }

    // CHG7 A5: per-button text for the snoozing toast (CHG15: English literals; the mute
    // path keeps the existing translated resource)
    private static String volumeKeySnoozedText(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return "Snoozing alert due to double volume DOWN button press";
            case KeyEvent.KEYCODE_VOLUME_UP:
                return "Snoozing alert due to double volume UP button press";
            default:
                return gs(R.string.snoozing_due_button_press);
        }
    }

    /**
     * CHG4 addendum A: while a BG alert is alerting and 'Buttons silence alarms' is
     * enabled, the volume keys are reserved for snoozing: consuming both the DOWN and UP
     * events (including auto-repeats) prevents any change of the phone's sound volume.
     */
    public static boolean volumeKeyConsumed(final int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return volumeKeySnoozeEnabled() // CHG11
                        && ActiveBgAlert.currentlyAlerting();
        }
        return false;
    }

    /**
     * CHG11: 'Buttons silence alarms' depends on 'Snooze screen over other apps' (user
     * decision 2026-07-10). Enforced here as well as in the preference screen, so a stored
     * value cannot activate the feature while the (greyed out) parent option is off.
     * CHG13 ER1: both options default to enabled, matching their XML defaults, so
     * volume-key snooze keeps working out of the box also on upgraded installs.
     */
    public static boolean volumeKeySnoozeEnabled() {
        return Pref.getBoolean(SnoozeOverlayActivity.PREF_SNOOZE_OVER_OTHER_APPS, true)
                && Pref.getBoolean("buttons_silence_alert", true);
    }

    // CHG4: the confirmation toast is kept as a reference so the second press can dismiss it
    // instantly (CHG7 A5: the text varies per button; CHG15: plain text parameter)
    private static void showVolumeKeyConfirmToast(final String text) {
        try {
            cancelVolumeKeyConfirmToast();
            volumeKeyConfirmToast = Toast.makeText(xdrip.getAppContext(),
                    text, Toast.LENGTH_SHORT);
            volumeKeyConfirmToast.show();
        } catch (Exception e) {
            Log.e(TAG, "Could not show volume key confirmation toast: " + e);
        }
    }

    private static void cancelVolumeKeyConfirmToast() {
        try {
            if (volumeKeyConfirmToast != null) {
                volumeKeyConfirmToast.cancel();
                volumeKeyConfirmToast = null;
            }
        } catch (Exception e) {
            //
        }
    }

    // CHG3: volume keys snooze the active alert on the snooze screens too; the overlay and
    // lock-screen variants then close themselves so the previous app or lock screen is restored
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // CHG4 addendum A: decide before the snooze handler flips the alerting state, so
        // the snoozing press itself is consumed as well
        final boolean consume = volumeKeyConsumed(event.getKeyCode());
        if (volumeKeySnooze(event)) {
            if (!openHomeAfterSnooze()) {
                finish();
            } else {
                displayStatus();
            }
        }
        if (consume) return true; // CHG4 addendum A: block the volume change
        return super.onKeyDown(keyCode, event);
    }

    // CHG4 addendum A: also consume the volume-key UP events while blocking is active
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (volumeKeyConsumed(event.getKeyCode())) return true;
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            displayStatus();
        }
    }

    @Override
    public String getMenuName() {
        return getString(R.string.snooze_alert);
    }

    public void addListenerOnButton() {
        buttonSnooze = (Button)findViewById(R.id.button_snooze);

        //low alerts
        disableLowAlerts = (Button)findViewById(R.id.button_disable_low_alerts);
        clearLowDisabled = (Button)findViewById(R.id.enable_low_alerts);

        //high alerts
        disableHighAlerts = (Button)findViewById(R.id.button_disable_high_alerts);
        clearHighDisabled = (Button)findViewById(R.id.enable_high_alerts);

        //all alerts
        disableAlerts = (Button)findViewById(R.id.button_disable_alerts);
        clearDisabled = (Button)findViewById(R.id.enable_alerts);
        sendRemoteSnooze = (Button)findViewById(R.id.send_remote_snooze);

        buttonSnooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int intValue = getTimeFromSnoozeValue(snoozeValue.getValue());
                AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);
                if (openHomeAfterSnooze()) { // CHG1: the overlay variant skips this so the previous app is restored
                    Intent intent = new Intent(getApplicationContext(), Home.class);
                    if (ActiveBgAlert.getOnly() != null) {
                        startActivity(intent);
                    }
                }
                finish();
            }

        });
        showDisableEnableButtons();

        setOnClickListenerOnDisableButton(disableAlerts, SnoozeType.ALL_ALERTS);
        setOnClickListenerOnDisableButton(disableLowAlerts, SnoozeType.LOW_ALERTS);
        setOnClickListenerOnDisableButton(disableHighAlerts, SnoozeType.HIGH_ALERTS);

        setOnClickListenerOnClearDisabledButton(clearDisabled, SnoozeType.ALL_ALERTS);
        setOnClickListenerOnClearDisabledButton(clearLowDisabled, SnoozeType.LOW_ALERTS);
        setOnClickListenerOnClearDisabledButton(clearHighDisabled, SnoozeType.HIGH_ALERTS);
    }

    /**
     * Functionality used at least three times moved to a function. Adds an onClickListener that will re-enable the identified alert
     * @param button to which onclicklistener should be added
     * @param snoozeType identifies the alert, value of the SnoozeType enum
     */
    private void setOnClickListenerOnClearDisabledButton(Button button, SnoozeType snoozeType) {
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit().putLong(snoozeType.getPrefKey(), 0).apply();
                //this is needed to make sure that the missedreading alert will be rechecked, it might have to be raised
                //and if not (ie no missed readings for long enough) then the alarm should be reset because it might have to recheck the missedreading status sooner
                recheckAlerts();
                //also make sure the text in the Activity is changed
                displayStatus();
                showDisableEnableButtons();
            }
        });
    }

    /**
     * Functionality used at least three times moved to a function. Adds an onClickListener that will disable the identified alert<br>
     * Depending on type of disable, also active alarms will be set to inactive<br>
     * - if snoozeType = ALL_ALERTS then the active bg alert will be deleted if any<br>
     * - if snoozeType = LOW_ALERTS and if active low bg alert exists then it will be deleted<br>
     * - if snoozeType = HIGH_ALERTS and if active high bg alert exists then it will be deleted<br>
     * @param button to which onclicklistener should be added
     * @param snoozeType identifies the alert, an enum value of SnoozeType
     */
    private void setOnClickListenerOnDisableButton(Button button, SnoozeType snoozeType) {
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Dialog d = new Dialog(SnoozeActivity.this);
                d.setTitle(R.string.default_snooze);
                d.setContentView(R.layout.snooze_picker);
                Button b1 = (Button) d.findViewById(R.id.button1);
                Button b2 = (Button) d.findViewById(R.id.button2);
                final NumberPicker snoozeValue = (NumberPicker) d.findViewById(R.id.numberPicker1);

                //don't use SetSnoozePickerValues because an additional value must be added
                String[] values = new String[snoozeValues.length + 1];//adding place for "until you re-enable"
                for (int i = 0;i < values.length - 1;i++)
                    values[i] = getNameFromTime(snoozeValues[i]);
                values[values.length - 1] = getString(R.string.until_you_reenable);
                snoozeValue.setMaxValue(values.length - 1);
                snoozeValue.setMinValue(0);
                snoozeValue.setDisplayedValues(values);
                snoozeValue.setWrapSelectorWheel(false);
                snoozeValue.setValue(getSnoozeLocation(60));

                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        long minutes;
                        if (snoozeValue.getValue() == snoozeValue.getMaxValue()) {
                            minutes = infiniteSnoozeValueInMinutes;
                        } else {
                            minutes = SnoozeActivity.getTimeFromSnoozeValue(snoozeValue.getValue());
                        }

                        snoozeForType(minutes, snoozeType, prefs);

                        d.dismiss();
                        //also make sure the text in the Activity is changed
                        displayStatus();
                        showDisableEnableButtons();
                    }
                });
                b2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        d.dismiss();
                        showDisableEnableButtons();
                    }
                });
                d.show();

            }
        });

    }
    public static void recheckAlerts() {
        // CHG8: same service run as Notifications.start() but without the 10-second rate
        // limit, so the notification snooze line updates promptly after a snooze
        Notifications.staticUpdateNotification();
        JoH.startService(MissedReadingService.class); // TODO this should be rate limited or similar as it is polled in various locations leading to excessive cpu
    }

    public void showDisableEnableButtons() {
        if(prefs.getLong(SnoozeType.ALL_ALERTS.getPrefKey(), 0) > new Date().getTime()){
            disableAlerts.setVisibility(View.GONE);
            clearDisabled.setVisibility(View.VISIBLE);
            //all alerts are disabled so no need to show the buttons related to disabling/enabling the low and high alerts
            disableLowAlerts.setVisibility(View.GONE);
            clearLowDisabled.setVisibility(View.GONE);
            disableHighAlerts.setVisibility(View.GONE);
            clearHighDisabled.setVisibility(View.GONE);
        } else {
            clearDisabled.setVisibility(View.GONE);
            disableAlerts.setVisibility(View.VISIBLE);
            if (prefs.getLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0) > new Date().getTime()) {
                disableLowAlerts.setVisibility(View.GONE);
                clearLowDisabled.setVisibility(View.VISIBLE);
            } else {
                disableLowAlerts.setVisibility(View.VISIBLE);
                clearLowDisabled.setVisibility(View.GONE);
            }
            if (prefs.getLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0) > new Date().getTime()) {
                disableHighAlerts.setVisibility(View.GONE);
                clearHighDisabled.setVisibility(View.VISIBLE);
            } else {
                disableHighAlerts.setVisibility(View.VISIBLE);
                clearHighDisabled.setVisibility(View.GONE);
            }
        }
    }


    void displayStatus() {
        ActiveBgAlert aba = ActiveBgAlert.getOnly();
        AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();

        // aba and activeBgAlert should both either exist ot not exist. all other cases are a bug in another place
        if(aba == null && activeBgAlert!= null) {
            Log.wtf(TAG, "ERRRO displayStatus: aba == null, but activeBgAlert != null exiting...");
            return;
        }
        if(aba != null && activeBgAlert== null) {
            // CHG7 A1: an orphaned record (test alert, removed alert type) is no longer
            // deleted during the lookup, so report it instead of logging an error
            alertStatus.setText("Alert type not found"); // CHG15 literal
            return;
        }
        long now = new Date().getTime();
        if(activeBgAlert == null ) {
            sendRemoteSnooze.setVisibility(Pref.getBooleanDefaultFalse("send_snooze_to_remote") ? View.VISIBLE : View.GONE);
            if (prefs.getLong(SnoozeType.ALL_ALERTS.getPrefKey(), 0) > now
                    ||
                    (prefs.getLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0) > now
                            && prefs.getLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0) > now)
                    ) {
                //not useful to show now that there's no active alert because either all alerts are disabled or high and low alerts are disabled
                //there can not be any active alert
                status = "";
            }
            else {
                status = getString(R.string.no_active_alert_exists);
            }
            buttonSnooze.setVisibility(View.GONE);
            snoozeValue.setVisibility(View.GONE);
        } else {
            sendRemoteSnooze.setVisibility(View.GONE);
            if(!aba.ready_to_alarm()) {
                // CHG7 A5: show seconds when under two minutes remain instead of "0 minutes left"
                final long msLeft = aba.next_alert_at - now;
                final String timeLeft = msLeft < 120000
                        ? (msLeft / 1000) + " seconds left"
                        : (msLeft / 60000) + " minutes left";
                status = MessageFormat.format("Active alert exists named \"{0}\" {1,choice,0#Alert will rerise at|1#Alert snoozed until} {2,time} ({3})",
                        activeBgAlert.name, aba.is_snoozed ? 1 : 0, new Date(aba.next_alert_at), timeLeft);
            } else {
                status = getString(R.string.active_alert_exists_named)+" \"" + activeBgAlert.name + "\" "+getString(R.string.bracket_not_snoozed);
            }
            SetSnoozePickerValues(snoozeValue, activeBgAlert.above, activeBgAlert.default_snooze);
        }

        //check if there are disabled alerts and if yes add warning
        if (prefs.getLong(SnoozeType.ALL_ALERTS.getPrefKey(), 0) > now) {
            String textToAdd = MessageFormat.format("{0,choice,0#{1,time}|1#you re-enable}",
                    (prefs.getLong(SnoozeType.ALL_ALERTS.getPrefKey(), 0) > now + (infiniteSnoozeValueInMinutes - 365 * 24 * 60) * 60 * 1000) ? 1 : 0 , new Date(prefs.getLong(SnoozeType.ALL_ALERTS.getPrefKey(), 0)));
            status = getString(R.string.all_alerts_disabled_until) + textToAdd;
        } else {
            if (prefs.getLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0) > now) {
                String textToAdd = MessageFormat.format("{0,choice,0#{1,time}|1#you re-enable}",
                        (prefs.getLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0) > now + (infiniteSnoozeValueInMinutes - 365 * 24 * 60) * 60 * 1000) ? 1 : 0 , new Date(prefs.getLong(SnoozeType.LOW_ALERTS.getPrefKey(), 0)));
                status += "\n\n"+getString(R.string.low_alerts_disabled_until) + textToAdd;
            }
            if (prefs.getLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0) > now) {
                String textToAdd = MessageFormat.format("{0,choice,0#{1,time}|1#you re-enable}",
                        (prefs.getLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0) > now + (infiniteSnoozeValueInMinutes - 365 * 24 * 60) * 60 * 1000) ? 1 : 0 , new Date(prefs.getLong(SnoozeType.HIGH_ALERTS.getPrefKey(), 0)));
                status += "\n\n"+getString(R.string.high_alerts_disabled_until) + textToAdd;
            }
        }

        alertStatus.setText(status);

    }

    public void setSendRemoteSnoozeOnClick(View v) {
        JoH.static_toast_short(gs(R.string.remote_snooze));
        AlertPlayer.getPlayer().Snooze(xdrip.getAppContext(), -1);
    }

}
