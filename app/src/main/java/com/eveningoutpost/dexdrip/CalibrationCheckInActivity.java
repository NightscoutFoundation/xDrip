package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.SyncingService;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

public class CalibrationCheckInActivity extends ActivityWithMenu {
    public static String menu_name = "Check in calibration";
   Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_check_in);
        addListenerOnButton();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button = (Button) findViewById(R.id.check_in_calibrations);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (Sensor.isActive()) {
                    SyncingService.startActionCalibrationCheckin(getApplicationContext());
                    Toast.makeText(getApplicationContext(), "Checked in all calibrations", Toast.LENGTH_LONG).show();
                    Intent tableIntent = new Intent(v.getContext(), Home.class);
                    startActivity(tableIntent);
                    finish();
                } else {
                    Log.w("CALIBRATION", "ERROR, sensor not active");
                }
            }
        });

    }
}
