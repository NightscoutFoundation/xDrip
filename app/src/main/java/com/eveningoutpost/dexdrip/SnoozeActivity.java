package com.eveningoutpost.dexdrip;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
 

public class SnoozeActivity extends Activity {
    ListView listViewLow;
    ListView listViewHigh;
    TextView alertStatus;
    Button buttonSnooze;
    Button createLowAlert;
    Button createHighAlert;
    
    EditText snoozeValue;
    final int MAX_SNOOZE = 600;
    final int ADD_ALERT = 1;
    final int EDIT_ALERT = 1;
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    
    HashMap<String, String> createAlertMap(AlertType alert) {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("alertName", alert.name);
        map.put("alertThreshold", String.valueOf((int)alert.threshold));
        map.put("alertTime", "all day"); //??????????????????????????????????????
        map.put("alertMp3File", alert.mp3_file);
        map.put("uuid", alert.uuid);
        
        return map;
    }
    
    ArrayList<HashMap<String, String>> createAlertsMap(boolean above) {
        ArrayList<HashMap<String, String>> feedList= new ArrayList<HashMap<String, String>>();

        List<AlertType> alerts = AlertType.getAll(above);
        for (AlertType alert : alerts) {
            Log.e(TAG, alert.toString());
            feedList.add(createAlertMap(alert));
        }
        return feedList;
    }
    
    
    class AlertsOnItemLongClickListener implements OnItemLongClickListener {
//      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        
           
           ListView lv = (ListView)parent;
           @SuppressWarnings("unchecked")
           HashMap<String, String> item = (HashMap<String, String>)lv.getItemAtPosition(position);
           Log.e(TAG, "Item clicked " + listViewLow.getItemAtPosition(position) + item.get("uuid"));
           
           //The XML for each item in the list (should you use a custom XML) must have android:longClickable="true" 
           // as well (or you can use the convenience method lv.setLongClickable(true);). This way you can have a list 
           // with only some items responding to longclick. (might be used for non removable alerts)
           
           Intent myIntent = new Intent(SnoozeActivity.this, EditAlertActivity.class);
           myIntent.putExtra("uuid", item.get("uuid")); //Optional parameters
           SnoozeActivity.this.startActivityForResult(myIntent, EDIT_ALERT);
           
           
           return true;
       
      }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);
        listViewLow = (ListView) findViewById(R.id.listView_low);
        listViewHigh = (ListView) findViewById(R.id.listView_high);
        alertStatus = (TextView) findViewById(R.id.alert_status);
        snoozeValue = (EditText) findViewById(R.id.snooze);
        
        addListenerOnButton();
        displayStatus();
        FillLists();

        listViewLow.setOnItemLongClickListener(new AlertsOnItemLongClickListener());
        listViewHigh.setOnItemLongClickListener(new AlertsOnItemLongClickListener());
    }
    

    public void addListenerOnButton() {

        buttonSnooze = (Button)findViewById(R.id.button_snooze);
        createLowAlert = (Button)findViewById(R.id.button_create_low);
        createHighAlert = (Button)findViewById(R.id.button_create_high);

        buttonSnooze.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int intValue = 0;
                try {
                    intValue = Integer.parseInt(snoozeValue.getText().toString());
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "addListenerOnButton cought exception", nfe);
                    intValue = Integer.MAX_VALUE;
                }
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
        
        
        createLowAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(SnoozeActivity.this, EditAlertActivity.class);
                myIntent.putExtra("above", "false");
                SnoozeActivity.this.startActivityForResult(myIntent, ADD_ALERT);
            }

        });
        
        createHighAlert.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(SnoozeActivity.this, EditAlertActivity.class);
                myIntent.putExtra("above", "true");
                SnoozeActivity.this.startActivityForResult(myIntent, ADD_ALERT);
            }
        });
        
    }

    
    void displayStatus() {
        ActiveBgAlert aba = ActiveBgAlert.getOnly();
        AlertType activeBgAlert = ActiveBgAlert.alertTypegetOnly();
        
        // aba and activeBgAlert should both either exist ot not exist. all other casses are a bug in another place
        if(aba == null && activeBgAlert!= null) {
            Log.wtf(TAG, "ERRRO displayStatus: aba == null, but activeBgAlert != null exiting...");
            return;
        }
        if(aba != null && activeBgAlert== null) {
            Log.wtf(TAG, "ERRRO displayStatus: aba != null, but activeBgAlert == null exiting...");
            return;
        }

        if(activeBgAlert == null ) {
            String status = "No active alert exists";
            alertStatus.setText(status);
            buttonSnooze.setVisibility(View.GONE);
            snoozeValue.setVisibility(View.GONE);
        } else {
            String status = "Active alert exists named \"" + activeBgAlert.name + "\" Alert snoozed until " + 
                    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(aba.next_alert_at)) + 
                    " (" + (aba.next_alert_at - new Date().getTime()) / 60000 + " minutes left)";
            alertStatus.setText(status);
        }
        
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult called ");
        if (requestCode == ADD_ALERT || requestCode == EDIT_ALERT) {
            if(resultCode == RESULT_OK) {
                Log.e(TAG, "onActivityResult called invalidating...");
                FillLists();
            }
            if (resultCode == RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }
    
    
    void FillLists() {
        ArrayList<HashMap<String, String>> feedList;
        feedList = createAlertsMap(false);
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, feedList, R.layout.row_alerts, new String[]{"alertName", "alertThreshold", "alertTime", "alertMp3File"}, new int[]{R.id.alertName, R.id.alertThreshold, R.id.alertTime, R.id.alertMp3File});
        listViewLow.setAdapter(simpleAdapter);
        
        feedList = createAlertsMap(true);
        SimpleAdapter simpleAdapterHigh = new SimpleAdapter(this, feedList, R.layout.row_alerts, new String[]{"alertName", "alertThreshold", "alertTime", "alertMp3File"}, new int[]{R.id.alertName, R.id.alertThreshold, R.id.alertTime, R.id.alertMp3File});
        listViewHigh.setAdapter(simpleAdapterHigh);
        
        
    }

}