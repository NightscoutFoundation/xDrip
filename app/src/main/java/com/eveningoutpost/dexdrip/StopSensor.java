package com.eveningoutpost.dexdrip;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.G5Model.Extensions;
import com.eveningoutpost.dexdrip.G5Model.Transmitter;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Services.G5CollectionService;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Set;

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

                Toast.makeText(getApplicationContext(), "Sensor stopped", Toast.LENGTH_LONG).show();

                //If Sensor is stopped for G5, we need to prevent further BLE scanning.
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String collection_method = prefs.getString("dex_collection_method", "BluetoothWixel");
                if(collection_method.compareTo("DexcomG5") == 0) {
                    Intent serviceIntent = new Intent(getApplicationContext(), G5CollectionService.class);
                    startService(serviceIntent);
                }

                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
