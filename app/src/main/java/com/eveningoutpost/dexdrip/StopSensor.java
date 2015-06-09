package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.util.Date;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;
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
                Sensor sensor = Sensor.currentSensor();
                sensor.stopped_at = new Date().getTime();
                Log.w("NEW SENSOR", "Sensor stopped at " + sensor.stopped_at);
                sensor.save();
                AlertPlayer.getPlayer().stopAlert(getApplicationContext(),true, false);

                Toast.makeText(getApplicationContext(), "Sensor stopped", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
