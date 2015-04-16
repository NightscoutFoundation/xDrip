package com.eveningoutpost.dexdrip;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
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


public class SnoozeActivity extends Activity {
    TextView alertStatus;
    Button buttonSnooze;
    boolean doMgdl;

    NumberPicker snoozeValue;
    final int MAX_SNOOZE = 600;

    private final static String TAG = AlertPlayer.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);
        alertStatus = (TextView) findViewById(R.id.alert_status);
        snoozeValue = (NumberPicker) findViewById(R.id.snooze);
        SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        doMgdl = (prefs.getString("units", "mgdl").compareTo("mgdl") == 0);

        addListenerOnButton();
        displayStatus();
    }

    public void addListenerOnButton() {
        buttonSnooze = (Button)findViewById(R.id.button_snooze);
        buttonSnooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            int intValue = snoozeValue.getValue()*5;
            if(intValue > MAX_SNOOZE) {
                Toast.makeText(getApplicationContext(), "Alert must be smaller than " + MAX_SNOOZE + " minutes",Toast.LENGTH_LONG).show();
                return;
            } else {
                AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }
            }

        });
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
            String[] values=new String[40];
            for(int i=0;i<values.length;i++){
                values[i]=Integer.toString((i+1)*5);
            }
            snoozeValue.setMaxValue(values.length);
            snoozeValue.setMinValue(1);
            snoozeValue.setDisplayedValues(values);
            snoozeValue.setWrapSelectorWheel(false);
            if(activeBgAlert.default_snooze != 0) {
                snoozeValue.setValue(activeBgAlert.default_snooze/5);
            } else {
                snoozeValue.setValue(4);
            }
            alertStatus.setText(status);
        }

    }
}
