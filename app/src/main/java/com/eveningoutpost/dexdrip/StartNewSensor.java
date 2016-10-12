package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.Calendar;
import java.util.Date;


public class StartNewSensor extends ActivityWithMenu {
   // public static String menu_name = "Start Sensor";
    private Button button;
    private DatePicker dp;
    private TimePicker tp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!Sensor.isActive()) {
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
        return getString(R.string.start_sensor);
    }

    public void addListenerOnButton() {
        button = (Button) findViewById(R.id.startNewSensor);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && DexCollectionType.hasBluetooth()) {
                    if (!LocationHelper.locationPermission(StartNewSensor.this)) {
                        JoH.static_toast_long("Location permission needed to use Bluetooth!");
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                    } else {
                        sensorButtonClick();
                    }
                } else {
                    sensorButtonClick();
                }
            }
        });
    }

    private void sensorButtonClick() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(), tp.getCurrentHour(), tp.getCurrentMinute(), 0);
        long startTime = calendar.getTime().getTime();

        if(new Date().getTime() + 15 * 60000 < startTime ) {
            Toast.makeText(getApplicationContext(), "ERROR: SENSOR START TIME IN FUTURE", Toast.LENGTH_LONG).show();
            return;
        }

        Sensor.create(startTime);
        Log.d("NEW SENSOR", "Sensor started at " + startTime);

        Toast.makeText(getApplicationContext(), "NEW SENSOR STARTED", Toast.LENGTH_LONG).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // TODO add link pickers feature
        //prefs.edit().putBoolean("start_sensor_link_pickers", linkPickers.isChecked()).apply();

        final boolean wear_integration = Home.getPreferencesBoolean("wear_sync", false);//KS
        if (wear_integration) {
            android.util.Log.d("StartNewSensor", "start WatchUpdaterService with ACTION_SYNC_SENSOR");
            startService(new Intent(this, WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_SYNC_SENSOR));
        }

        LibreAlarmReceiver.clearSensorStats();

        CollectionServiceStarter.newStart(getApplicationContext());
        Intent intent;
        if(prefs.getBoolean("store_sensor_location",true)) {
            intent = new Intent(getApplicationContext(), NewSensorLocation.class);
        } else {
            intent = new Intent(getApplicationContext(), Home.class);
        }

        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        sensorButtonClick();
                    }
                }
            }
        }
    }

    public void oldaddListenerOnButton() {

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
