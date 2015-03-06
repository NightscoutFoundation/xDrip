package com.eveningoutpost.dexdrip;

import java.text.DateFormat;
import java.util.Date;

import android.app.ListActivity;
import android.widget.ListView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;


/*

public class SnoozeActivity extends Activity {
    public Button button;
    ListView listView ;
//    public DatePicker dp;
//    public TimePicker tp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);

        addListenerOnButton();
        
        
        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.list);
        
        // Defined Array values to show in ListView
        String[] values = new String[] { "Android List View", 
                                         "Adapter implementation",
                                         "Simple List View In Android",
                                         "Create List View Android", 
                                         "Android Example", 
                                         "List View Source Code", 
                                         "List View Array Adapter", 
                                         "Android Example List View" 
                                        };

        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
          android.R.layout.simple_list_item_1, android.R.id.text1, values);


        // Assign adapter to ListView
        listView.setAdapter(adapter); 
        
        // ListView Item Click Listener
        listView.setOnItemClickListener(new OnItemClickListener() {

              @Override
              public void onItemClick(AdapterView<?> parent, View view,
                 int position, long id) {
                
               // ListView Clicked item index
               int itemPosition     = position;
               
               // ListView Clicked item value
               String  itemValue    = (String) listView.getItemAtPosition(position);
                  
                // Show Alert 
                Toast.makeText(getApplicationContext(),
                  "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                  .show();
             
              }

         }); 

    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.button_snooze);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText value = (EditText) findViewById(R.id.snooze);
                int intValue = Integer.parseInt(value.getText().toString());

                AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);

                
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
        
    }
}

*/

/*
public class SnoozeActivity extends ListActivity {
    public void onCreate(Bundle icicle) {
      super.onCreate(icicle);
      setContentView(R.layout.activity_snooze);
      String[] values = new String[] { "Android",// "iPhone", "WindowsMobile",
//          "Blackberry", "WebOS", "Ubuntu", "Windows7", "Max OS X",
          "Linux", "OS/2" };
      // use your custom layout
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
              android.R.layout.simple_list_item_1, values);
      setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
      String item = (String) getListAdapter().getItem(position);
      Toast.makeText(this, item + " selected", Toast.LENGTH_LONG).show();
    }
  }
  */






import java.util.ArrayList;
import java.util.HashMap;
 




import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.ListView;
import android.widget.SimpleAdapter;
 
public class SnoozeActivity extends Activity {
    ListView lv;
    TextView alert_status;
    Button button;
    final int MAX_ALERT = 600;
    
    private final static String TAG = AlertPlayer.class.getSimpleName();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);
        lv = (ListView) findViewById(R.id.listView1);
        alert_status = (TextView) findViewById(R.id.alert_status);
        
        displayStatus();
        addListenerOnButton();
        
        ArrayList<HashMap<String, String>> feedList= new ArrayList<HashMap<String, String>>();
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("date", "1/7");
        map.put("description", "gift her");
        map.put("price", "23");
        map.put("discount", "25");
        feedList.add(map);
         
        map = new HashMap<String, String>();
        map.put("date", "1/8");
        map.put("description", "nice phone");
        map.put("price", "67");
        map.put("discount", "50");
        feedList.add(map);
         
        map = new HashMap<String, String>();
        map.put("date", "1/6");
        map.put("description", "hello");
        map.put("price", "33");
        map.put("discount", "50");
        feedList.add(map);
         
         
        map = new HashMap<String, String>();
        map.put("date", "1/3");
        map.put("description", "yo");
        map.put("price", "123");
        map.put("discount", "33");
        feedList.add(map);
         
         
         
        map = new HashMap<String, String>();
        map.put("date", "1/2");
        map.put("description", "nice phone");
        map.put("price", "67");
        map.put("discount", "50");
        feedList.add(map);
         
         
         
        map = new HashMap<String, String>();
        map.put("date", "23/12");
        map.put("description", "nice car");
        map.put("price", "6700");
        map.put("discount", "50");
        feedList.add(map);
         
         
        map = new HashMap<String, String>();
        map.put("date", "4/3");
        map.put("description", "nice phone");
        map.put("price", "678");
        map.put("discount", "70");
        feedList.add(map);
         
        for(int i =0; i < 20; i++) {
            map = new HashMap<String, String>();
            map.put("date", "1/12");
            map.put("description", "Ymmy burger " + i);
            map.put("price", "12");
            map.put("discount", "10");
            feedList.add(map);
        }
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, feedList, R.layout.row_alerts, new String[]{"date", "description", "price", "discount"}, new int[]{R.id.textViewDate, R.id.textViewDescription, R.id.textViewDiscount, R.id.textViewPrice});
        lv.setAdapter(simpleAdapter);
        lv.setOnItemClickListener(new OnItemClickListener() {

//            @Override
            public void onItemClick(AdapterView<?> parent, View view,
               int position, long id) {
              
             // ListView Clicked item index
             int itemPosition     = position;
             
             // ListView Clicked item value
/*             
             String  itemValue    = (String) lv.getItemAtPosition(position);
                
              // Show Alert 
              Toast.makeText(getApplicationContext(),
                "Position :"+itemPosition+"  ListItem : " +itemValue , Toast.LENGTH_LONG)
                .show();
           */
             lv.getItemAtPosition(position);
             Log.e("TAG", "Item clicked " + lv.getItemAtPosition(position));
            }
            
             
       }); 
    }
    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.button_snooze);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText value = (EditText) findViewById(R.id.snooze);
                int intValue = 0;
                try {
                    intValue = Integer.parseInt(value.getText().toString());
                } catch (NumberFormatException nfe) {
                    Log.e(TAG, "addListenerOnButton cought exception", nfe);
                    intValue = Integer.MAX_VALUE;
                }
                if(intValue > MAX_ALERT) {
                    Toast.makeText(getApplicationContext(), "Alert must be smaller than " + MAX_ALERT + " minutes",Toast.LENGTH_LONG).show();
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
            alert_status.setText("No active alert exists");
        } else {
            String status = "Active alert exists named \"" + activeBgAlert.name + "\" Alert snoozed until " + 
                    DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(aba.next_alert_at)) + 
                    " (" + (aba.next_alert_at - new Date().getTime()) / 60000 + " minutes left)";
            alert_status.setText(status);
        }
        
    }
/* 
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
 */
}