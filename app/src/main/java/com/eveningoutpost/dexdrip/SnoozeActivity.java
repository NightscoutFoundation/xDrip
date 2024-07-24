package com.eveningoutpost.dexdrip;

import java.text.MessageFormat;
import java.util.Date;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

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
            if (disableType == SnoozeType.ALL_ALERTS
                    || (activeBgAlert.above && disableType == SnoozeType.HIGH_ALERTS)
                    || (!activeBgAlert.above && disableType == SnoozeType.LOW_ALERTS)
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
        if (Home.get_holo()) { setTheme(R.style.OldAppThemeNoTitleBar); }
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
                Intent intent = new Intent(getApplicationContext(), Home.class);
                if (ActiveBgAlert.getOnly() != null) {
                    startActivity(intent);
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
        Notifications.start();
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
            Log.wtf(TAG, "ERRRO displayStatus: aba != null, but activeBgAlert == null exiting...");
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
                status = MessageFormat.format("Active alert exists named \"{0}\" {1,choice,0#Alert will rerise at|1#Alert snoozed until} {2,time} ({3} minutes left)",
                        activeBgAlert.name, aba.is_snoozed ? 1 : 0 , new Date(aba.next_alert_at),(aba.next_alert_at - now) / 60000);
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
