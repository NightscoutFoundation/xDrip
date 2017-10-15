package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Experience;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.profileeditor.TimePickerFragment;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;

import java.util.Calendar;
import java.util.Date;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.Models.BgReading.AGE_ADJUSTMENT_TIME;


public class StartNewSensor extends ActivityWithMenu {
    // public static String menu_name = "Start Sensor";
    private static final String TAG = "StartNewSensor";
    private Button button;
    //private DatePicker dp;
    // private TimePicker tp;
    final Activity activity = this;
    Calendar ucalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Sensor.isActive()) {
            JoH.fixActionBar(this);
            setContentView(R.layout.activity_start_new_sensor);
            button = (Button) findViewById(R.id.startNewSensor);
            //dp = (DatePicker)findViewById(R.id.datePicker);
            //tp = (TimePicker)findViewById(R.id.timePicker);
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
                        JoH.show_ok_dialog(activity, "Please Allow Permission", "Location permission needed to use Bluetooth!", new Runnable() {
                            @Override
                            public void run() {
                                activity.requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                            }
                        });
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


        ucalendar = Calendar.getInstance();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Did you insert it today?");
        builder.setMessage("We need to know when the sensor was inserted to improve calculation accuracy.\n\nWas it inserted today?");
        builder.setPositiveButton("YES, today", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                askSesorInsertionTime();
            }
        });
        builder.setNegativeButton("Not today", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (DexCollectionType.hasLibre()) {
                    ucalendar.add(Calendar.DAY_OF_MONTH, -1);
                    realStartSensor();
                } else {
                    final DatePickerFragment datePickerFragment = new DatePickerFragment();
                    datePickerFragment.setAllowFuture(false);
                    if (!Home.get_engineering_mode()) {
                        datePickerFragment.setEarliestDate(JoH.tsl() - (30L * 24 * 60 * 60 * 1000)); // 30 days
                    }
                    datePickerFragment.setTitle("Which day was it inserted?");
                    datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
                        @Override
                        public void onDateSet(int year, int month, int day) {
                            ucalendar.set(year, month, day);
                            // Long enough in the past for age adjustment to be meaningless? Skip asking time
                            if ((!Home.get_engineering_mode()) && (JoH.tsl() - ucalendar.getTimeInMillis() > (AGE_ADJUSTMENT_TIME + (1000 * 60 * 60 * 24)))) {
                                realStartSensor();
                            } else {
                                askSesorInsertionTime();
                            }
                        }
                    });

                    datePickerFragment.show(activity.getFragmentManager(), "DatePicker");
                }
            }
        });
        builder.create().show();
    }

    private void askSesorInsertionTime() {
        final Calendar calendar = Calendar.getInstance();

        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        timePickerFragment.setTitle("What time was it inserted?");
        timePickerFragment.setTimeCallback(new ProfileAdapter.TimePickerCallbacks() {
            @Override
            public void onTimeUpdated(int newmins) {
                int min = newmins % 60;
                int hour = (newmins - min) / 60;
                ucalendar.set(ucalendar.get(Calendar.YEAR), ucalendar.get(Calendar.MONTH), ucalendar.get(Calendar.DAY_OF_MONTH), hour, min);
                if (DexCollectionType.hasLibre()) {
                    ucalendar.add(Calendar.HOUR_OF_DAY, -1); // hack for warmup time
                }

                realStartSensor();
            }
        });
        timePickerFragment.show(activity.getFragmentManager(), "TimePicker");
    }

    private void realStartSensor() {
        long startTime = ucalendar.getTime().getTime();
        Log.d(TAG, "Starting sensor time: " + JoH.dateTimeText(ucalendar.getTime().getTime()));

        if (new Date().getTime() + 15 * 60000 < startTime) {
            Toast.makeText(this, "ERROR: SENSOR START TIME IN FUTURE", Toast.LENGTH_LONG).show();
            return;
        }

        Sensor.create(startTime);
        Log.d("NEW SENSOR", "Sensor started at " + startTime);

        Toast.makeText(this, "NEW SENSOR STARTED", Toast.LENGTH_LONG).show();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // TODO add link pickers feature
        //prefs.edit().putBoolean("start_sensor_link_pickers", linkPickers.isChecked()).apply();

        startWatchUpdaterService(this, WatchUpdaterService.ACTION_SYNC_SENSOR, TAG);

        LibreAlarmReceiver.clearSensorStats();
        JoH.scheduleNotification(this, "Sensor should be ready", getString(R.string.please_enter_two_calibrations_to_get_started), 60 * 130, Home.SENSOR_READY_ID);

        // reverse libre hacky workaround
        Treatments.SensorStart((DexCollectionType.hasLibre() ? startTime + (3600000) : startTime));

        CollectionServiceStarter.newStart(getApplicationContext());
        Intent intent;
        if (prefs.getBoolean("store_sensor_location", false) && Experience.gotData()) {
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

    /*public void oldaddListenerOnButton() {

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

    }*/
}
