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
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;

import java.util.Calendar;
import java.util.List;


public class StartNewSensor extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    private String menu_name = "Start Sensor";
    private NavigationDrawerFragment mNavigationDrawerFragment;
    public Button button;
    public DatePicker dp;
    public TimePicker tp;

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
    protected void onResume(){
        super.onResume();
        NavigationDrawerFragment mNavigationDrawerFragment;
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        NavDrawerBuilder navDrawerBuilder = new NavDrawerBuilder(getApplicationContext());
        List<String> menu_option_list = navDrawerBuilder.nav_drawer_options;
        int menu_position = menu_option_list.indexOf(menu_name);

        if (position != menu_position) {
            List<Intent> intent_list = navDrawerBuilder.nav_drawer_intents;
            Intent[] intent_array = intent_list.toArray(new Intent[intent_list.size()]);
            startActivity(intent_array[position]);
            finish();
        }

    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.startNewSensor);

        button.setOnClickListener(new View.OnClickListener() {
          public void onClick(View v) {

              Calendar calendar = Calendar.getInstance();
              calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(),
              tp.getCurrentHour(), tp.getCurrentMinute(), 0);
              long startTime = calendar.getTime().getTime();

              Sensor sensor = Sensor.create(startTime);
              Log.w("NEW SENSOR", "Sensor started at " + startTime);

              Toast.makeText(getApplicationContext(), "NEW SENSOR STARTED", Toast.LENGTH_LONG).show();
              Intent intent = new Intent(getApplicationContext(), Home.class);
              CollectionServiceStarter.newStart(getApplicationContext());
              startActivity(intent);
              finish();
          }

        });
    }
}
