package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.G5Model.DexSyncKeeper;
import com.eveningoutpost.dexdrip.G5Model.Ob1G5StateMachine;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.Treatments;
import com.eveningoutpost.dexdrip.Models.UserError;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Experience;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.eveningoutpost.dexdrip.profileeditor.DatePickerFragment;
import com.eveningoutpost.dexdrip.profileeditor.ProfileAdapter;
import com.eveningoutpost.dexdrip.profileeditor.TimePickerFragment;
import com.eveningoutpost.dexdrip.ui.dialog.G6CalibrationCodeDialog;
import com.eveningoutpost.dexdrip.ui.dialog.G6EndOfLifeDialog;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.LocationHelper;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;
import com.eveningoutpost.dexdrip.G5Model.DexTimeKeeper;
import com.eveningoutpost.dexdrip.G5Model.FirmwareCapability;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;
import static com.eveningoutpost.dexdrip.Models.BgReading.AGE_ADJUSTMENT_TIME;
import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.eveningoutpost.dexdrip.Services.Ob1G5CollectionService.getTransmitterID;

public class StartNewSensor extends ActivityWithMenu {
    // public static String menu_name = "Start Sensor";
    private static final String TAG = "StartNewSensor";
    private Button button;
    //private DatePicker dp;
    // private TimePicker tp;
    final Activity activity = this;
    Calendar ucalendar = Calendar.getInstance();
    private static final String TX_EOL = "TX_EOL"; // True when transmitter cannot start more sensors
    private static final String TX_Mod = "TX_Mod"; // True for modified transmitters

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
                        LocationHelper.requestLocationForBluetooth(StartNewSensor.this);
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
        if (Ob1G5CollectionService.usingNativeMode()) {
            if (!DexSyncKeeper.isReady(Pref.getString("dex_txid", "NULL"))) {
                JoH.static_toast_long("Need to connect to transmitter once before we can start sensor");
                MegaStatus.startStatus(MegaStatus.G5_STATUS);
            } else {
                startSensorOrAskForG6Code();   // If we're using native mode, don't bother asking about insertion time
            }
        } else {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(gs(R.string.did_you_insert_it_today));
            builder.setMessage(gs(R.string.we_need_to_know_when_the_sensor_was_inserted_to_improve_calculation_accuracy__was_it_inserted_today));
            builder.setPositiveButton(gs(R.string.yes_today), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    askSensorInsertionTime();
                }
            });
            builder.setNegativeButton(gs(R.string.not_today), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if (DexCollectionType.hasLibre()) {
                        ucalendar.add(Calendar.DAY_OF_MONTH, -1);
                        startSensorOrAskForG6Code();
                    } else {
                        final DatePickerFragment datePickerFragment = new DatePickerFragment();
                        datePickerFragment.setAllowFuture(false);
                        if (!Home.get_engineering_mode()) {
                            datePickerFragment.setEarliestDate(JoH.tsl() - (30L * 24 * 60 * 60 * 1000)); // 30 days
                        }
                        datePickerFragment.setTitle(gs(R.string.which_day_was_it_inserted));
                        datePickerFragment.setDateCallback(new ProfileAdapter.DatePickerCallbacks() {
                            @Override
                            public void onDateSet(int year, int month, int day) {
                                ucalendar.set(year, month, day);
                                // Long enough in the past for age adjustment to be meaningless? Skip asking time
                                if ((!Home.get_engineering_mode()) && (JoH.tsl() - ucalendar.getTimeInMillis() > (AGE_ADJUSTMENT_TIME + (1000 * 60 * 60 * 24)))) {
                                    startSensorOrAskForG6Code();
                                } else {
                                    askSensorInsertionTime();
                                }
                            }
                        });

                        datePickerFragment.show(activity.getFragmentManager(), "DatePicker");
                    }
                }
            });
            builder.create().show();
        }
    }

    private void askSensorInsertionTime() {
        final Calendar calendar = Calendar.getInstance();

        TimePickerFragment timePickerFragment = new TimePickerFragment();
        timePickerFragment.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        timePickerFragment.setTitle(gs(R.string.what_time_was_it_inserted));
        timePickerFragment.setTimeCallback(new ProfileAdapter.TimePickerCallbacks() {
            @Override
            public void onTimeUpdated(int newmins) {
                int min = newmins % 60;
                int hour = (newmins - min) / 60;
                ucalendar.set(ucalendar.get(Calendar.YEAR), ucalendar.get(Calendar.MONTH), ucalendar.get(Calendar.DAY_OF_MONTH), hour, min);

                startSensorOrAskForG6Code();
            }
        });
        timePickerFragment.show(activity.getFragmentManager(), "TimePicker");
    }

    private void startSensorOrAskForG6Code() {
        final int cap = 20;
        if (Ob1G5CollectionService.usingCollector() && Ob1G5StateMachine.usingG6()) {
            if (JoH.pratelimit("dex-stop-start", cap)) {
                int TX_dys = DexTimeKeeper.getTransmitterAgeInDays(getTransmitterID());
                PersistentStore.setBoolean(TX_EOL, false);
                PersistentStore.setBoolean(TX_Mod, false);
                if (FirmwareCapability.isTransmitterModified(getTransmitterID())) { // Transmitter is modified
                    PersistentStore.setBoolean(TX_Mod, true);
                }
                if (TX_dys > 179 || !PersistentStore.getBoolean("TX_Mod") & TX_dys > 99) { // Cannot start a sensor any longer
                    PersistentStore.setBoolean(TX_EOL, true);
                }
                JoH.clearRatelimit("dex-stop-start");
                if (TX_dys < 69 || PersistentStore.getBoolean("TX_Mod") & TX_dys < 149) { // More than 30 days left of starting sensors
                    G6CalibrationCodeDialog.ask(this, this::startSensorAndSetIntent);
                } else { // 30 or less days left of starting sensors
                    G6EndOfLifeDialog.show(activity, () -> {
                        G6CalibrationCodeDialog.ask(this, this::startSensorAndSetIntent);
                    });
                }
            } else {
                JoH.static_toast_long(String.format(Locale.ENGLISH, getString(R.string.please_wait_seconds_before_trying_to_start_sensor), cap));
            }
        } else {
            startSensorAndSetIntent();
        }
    }

    private void startSensorAndSetIntent() {
        long startTime = ucalendar.getTime().getTime();
        Log.d(TAG, "Starting sensor time: " + JoH.dateTimeText(ucalendar.getTime().getTime()));

        if (new Date().getTime() + 15 * 60000 < startTime) {
            Toast.makeText(this, gs(R.string.error_sensor_start_time_in_future), Toast.LENGTH_LONG).show();
            return;
        }

        startSensorForTime(startTime);

        Intent intent;
        if (Pref.getBoolean("store_sensor_location", false) && Experience.gotData()) {
            intent = new Intent(getApplicationContext(), NewSensorLocation.class);
        } else {
            intent = new Intent(getApplicationContext(), Home.class);
        }

        startActivity(intent);
        finish();
    }

    /**
     * Sends command to G5/G6 sensor for sensor start and adds a Sensor Start entry in the xDrip db
     */
    public static void startSensorForTime(long startTime) {
        Sensor.create(startTime);
        UserError.Log.ueh("NEW SENSOR", "Sensor started at " + JoH.dateTimeText(startTime));

        JoH.static_toast_long(gs(R.string.new_sensor_started));

        startWatchUpdaterService(xdrip.getAppContext(), WatchUpdaterService.ACTION_SYNC_SENSOR, TAG);

        LibreAlarmReceiver.clearSensorStats();
        // TODO this is just a timer and could be confusing - consider removing this notification
       // JoH.scheduleNotification(xdrip.getAppContext(), "Sensor should be ready", xdrip.getAppContext().getString(R.string.please_enter_two_calibrations_to_get_started), 60 * 130, Home.SENSOR_READY_ID);

        // Add treatment entry in db
        Treatments.sensorStart(startTime, "Started by xDrip");

        CollectionServiceStarter.restartCollectionServiceBackground();

        Ob1G5StateMachine.startSensor(startTime);
        JoH.clearCache();
        Home.staticRefreshBGCharts();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Ob1G5CollectionService.clearScanError();
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

              Toast.makeText(getApplicationContext(), gs(R.string.new_sensor_started), Toast.LENGTH_LONG).show();
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
