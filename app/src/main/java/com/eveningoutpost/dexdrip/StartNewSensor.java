package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.Calendar;


public class StartNewSensor extends ActivityWithMenu {
    public static String menu_name = "Start Sensor";
    private Button button;
    private DatePicker dp;
    private TimePicker tp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Sensor.isActive() == false) {
            setContentView(R.layout.activity_start_new_sensor);
            button = (Button)findViewById(R.id.startNewSensor);
            dp = (DatePicker)findViewById(R.id.datePicker);
            tp = (TimePicker)findViewById(R.id.timePicker);
            addListenerOnButton();
        } else {
            Intent intent = new Intent(this, StopSensor.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.startNewSensor);

        button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {

              Calendar calendar = Calendar.getInstance();
              calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(),
              tp.getCurrentHour(), tp.getCurrentMinute(), 0);
              long startTime = calendar.getTime().getTime();

              Sensor.create(startTime);
              Log.d("NEW SENSOR", "Sensor started at " + startTime);

              Toast.makeText(getApplicationContext(), "NEW SENSOR STARTED", Toast.LENGTH_LONG).show();
              CollectionServiceStarter.newStart(getApplicationContext());
              SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
              Intent intent;
              if(prefs.getBoolean("store_sensor_location",true)) {
                  intent = new Intent(getApplicationContext(), NewSensorLocation.class);
              } else {
                  intent = new Intent(getApplicationContext(), Home.class);
              }

              startActivity(intent);
              finish();
          }

        });

    }
}
