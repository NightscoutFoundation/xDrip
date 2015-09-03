package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.Calendar;
import java.util.List;


public class NewSensorLocation extends ActivityWithMenu {
    public static String menu_name = "New sensor location";
    private Button button;
    private RadioGroup radioGroup;
    private EditText sensor_location;
    CheckBox DontAskAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sensor_location);
        button = (Button)findViewById(R.id.startNewSensor);
        sensor_location = (EditText) findViewById(R.id.edit_sensor_location);
        sensor_location.setEnabled(false);
        DontAskAgain = (CheckBox)findViewById(R.id.sensorLocationDontAskAgain);
        addListenerOnButton();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.startNewSensor);

        button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {

              
              int selectedId = radioGroup.getCheckedRadioButtonId();
              String location = new String();
              if(selectedId == R.id.sensor_location_private) {
                  location = "private";
              } else if(selectedId == R.id.sensor_location_hand) {
                  location = "hand";
              } else if (selectedId == R.id.sensor_location_bottom) {
                  location = "bottom";
              } else if (selectedId == R.id.sensor_location_other) {
                  location = sensor_location.getText().toString();
              }
              
              Log.w("NEW SENSOR", "Sensor location is " + location);
              Sensor.updateSensorLocation(location);
              
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
              
              prefs.edit().putBoolean("store_sensor_location", !DontAskAgain.isChecked()).apply();

              Intent intent = new Intent(getApplicationContext(), Home.class);
              startActivity(intent);
              finish();
          }

        });
        
        radioGroup = (RadioGroup) findViewById(R.id.myRadioGroup);
        
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.sensor_location_other) {
                    sensor_location.setEnabled(true);
                    sensor_location.requestFocus();
                } else {
                    sensor_location.setEnabled(false);
                }
            }
        });
        
    }
}
