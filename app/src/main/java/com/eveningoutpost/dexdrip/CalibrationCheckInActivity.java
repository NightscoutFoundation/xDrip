package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.SyncingService;
import com.eveningoutpost.dexdrip.models.Sensor;
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
                    Log.d("CALIBRATION", "ERROR, sensor not active");
                }
            }
        });

    }
}
