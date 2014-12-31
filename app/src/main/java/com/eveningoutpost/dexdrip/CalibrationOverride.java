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
import android.widget.EditText;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.Calibration;


public class CalibrationOverride extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
        Button button;
    private String menu_name = "Override Calibration";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_calibration_override);
            addListenerOnButton();
            }

    @Override
    protected void onResume(){
                super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
            }
    
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    public void addListenerOnButton() {
            button = (Button) findViewById(R.id.save_calibration_button);

            button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Sensor.isActive()) {
                    EditText value = (EditText) findViewById(R.id.bg_value);
                    String string_value = value.getText().toString();
                    Log.w("STRING VALUE", "" + string_value);
                    if (!string_value.matches("")) {
                        int intValue = Integer.parseInt(string_value);

                         Calibration.last().overrideCalibration(intValue, getApplicationContext());

                         Intent tableIntent = new Intent(v.getContext(), Home.class);
                         startActivity(tableIntent);
                         finish();
                    } else {
                        Toast.makeText(getParent(), "You must enter a value to continue", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.w("CANNOT CALIBRATE WITHOUT CURRENT SENSOR", "ERROR");
                }
            }
        });

    }
}
