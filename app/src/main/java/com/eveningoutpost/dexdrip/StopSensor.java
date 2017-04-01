package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

public class StopSensor extends ActivityWithMenu {
    public static String menu_name = "Stop Sensor";
   public Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Sensor.isActive() == false) {
            Intent intent = new Intent(this, StartNewSensor.class);
            startActivity(intent);
            finish();
        } else {
            JoH.fixActionBar(this);
            setContentView(R.layout.activity_stop_sensor);
            button = (Button)findViewById(R.id.stop_sensor);
            addListenerOnButton();
        }
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.stop_sensor);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Sensor.stopSensor();
                AlertPlayer.getPlayer().stopAlert(getApplicationContext(), true, false);

                Toast.makeText(getApplicationContext(), R.string.sensor_stopped, Toast.LENGTH_LONG).show();

                LibreAlarmReceiver.clearSensorStats();
                PluggableCalibration.invalidateAllCaches();

                //If Sensor is stopped for G5, we need to prevent further BLE scanning.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
                if(collection_method.compareTo("DexcomG5") == 0) {
                    Intent serviceIntent = new Intent(getApplicationContext(), G5CollectionService.class);
                    startService(serviceIntent);
                }

                final Intent intent = new Intent(getApplicationContext(), Home.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Home.startIntentThreadWithDelayedRefresh(intent);

                finish();
            }

        });
    }

    public void resetAllCalibrations(View v) {

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.are_you_sure2);
        builder.setMessage(R.string.want_deleting_calibrations);

        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Calibration.invalidateAllForSensor();
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();


    }
}
