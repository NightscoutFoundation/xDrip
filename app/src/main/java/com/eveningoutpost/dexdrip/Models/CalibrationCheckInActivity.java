package com.eveningoutpost.dexdrip.Models;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.ImportedLibraries.dexcom.SyncingService;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.Sensor;

public class CalibrationCheckInActivity extends Activity {
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_check_in);
        addListenerOnButton();
    }

    public void addListenerOnButton() {

        button = (Button) findViewById(R.id.check_in_calibrations);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (Sensor.isActive()) {
                    SyncingService.startActionCalibrationCheckin(getApplicationContext());
                } else {
                    Log.w("CANNOT CALIBRATE WITHOUT CURRENT SENSOR", "ERROR");
                }
            }
        });

    }
}
