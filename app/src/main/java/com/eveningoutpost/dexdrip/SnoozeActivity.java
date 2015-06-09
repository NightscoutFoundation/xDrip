package com.eveningoutpost.dexdrip;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.UtilityModels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;


public class SnoozeActivity extends ActivityWithMenu {
    public static String menu_name = "Snooze Alert";

    TextView alertStatus;
    Button buttonSnooze;
    Button disableAlerts;
    Button clearDisabled;
    SharedPreferences prefs;
    boolean doMgdl;

    NumberPicker snoozeValue;

    static final int snoozeValues[] = new int []{5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 75, 90, 105, 120, 150, 180, 240, 300, 360, 420, 480, 540, 600};

    static int getSnoozeLocatoin(int time) {
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
        return 30;
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
            picker.setValue(getSnoozeLocatoin(default_snooze));
        } else {
            picker.setValue(getSnoozeLocatoin(getDefaultSnooze(above)));
        }
    }


    private final static String TAG = AlertPlayer.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {
        buttonSnooze = (Button)findViewById(R.id.button_snooze);
        disableAlerts = (Button)findViewById(R.id.button_disable_alerts);
        clearDisabled = (Button)findViewById(R.id.enable_alerts);
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

        disableAlerts.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final Dialog d = new Dialog(SnoozeActivity.this);
                d.setTitle("Default Snooze");
                d.setContentView(R.layout.snooze_picker);
                Button b1 = (Button) d.findViewById(R.id.button1);
                Button b2 = (Button) d.findViewById(R.id.button2);
                final NumberPicker snoozeValue = (NumberPicker) d.findViewById(R.id.numberPicker1);

                SnoozeActivity.SetSnoozePickerValues(snoozeValue, false, 60);
                b1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Long disableUntil = new Date().getTime() + (SnoozeActivity.getTimeFromSnoozeValue(snoozeValue.getValue()) * 1000 * 60);
                        prefs.edit().putLong("alerts_disabled_until", disableUntil).apply();
                        d.dismiss();
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

        clearDisabled.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit().putLong("alerts_disabled_until", 0).apply();
                showDisableEnableButtons();
            }
        });
    }

    public void showDisableEnableButtons() {
        if(prefs.getLong("alerts_disabled_until", 0) > new Date().getTime()){
            disableAlerts.setVisibility(View.GONE);
            clearDisabled.setVisibility(View.VISIBLE);
        } else {
            clearDisabled.setVisibility(View.GONE);
            disableAlerts.setVisibility(View.VISIBLE);
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
        String status;
        if(activeBgAlert == null ) {
            status = "No active alert exists";
            alertStatus.setText(status);
            buttonSnooze.setVisibility(View.GONE);
            snoozeValue.setVisibility(View.GONE);
        } else {
            if(!aba.ready_to_alarm()) {
                status = "Active alert exists named \"" + activeBgAlert.name + "\" Alert snoozed until " +
                    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(aba.next_alert_at)) +
                    " (" + (aba.next_alert_at - new Date().getTime()) / 60000 + " minutes left)";
            } else {
                status = "Active alert exists named \"" + activeBgAlert.name + "\" (not snoozed)";
            }
            SetSnoozePickerValues(snoozeValue, activeBgAlert.above, activeBgAlert.default_snooze);
            alertStatus.setText(status);
        }

    }

}
