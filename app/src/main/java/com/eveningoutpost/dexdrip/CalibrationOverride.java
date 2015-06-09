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

import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;


public class CalibrationOverride extends ActivityWithMenu {
        Button button;
    public static String menu_name = "Override Calibration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_calibration_override);
        addListenerOnButton();
    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {
            button = (Button) findViewById(R.id.save_calibration_button);

            button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Sensor.isActive()) {
                    EditText value = (EditText) findViewById(R.id.bg_value);
                    String string_value = value.getText().toString();
                    if (!TextUtils.isEmpty(string_value)){
                        double calValue = Double.parseDouble(string_value);

                        Calibration last_calibration = Calibration.last();
                        last_calibration.sensor_confidence = 0;
                        last_calibration.slope_confidence = 0;
                        last_calibration.save();
                        Calibration.create(calValue, getApplicationContext());

                         Intent tableIntent = new Intent(v.getContext(), Home.class);
                         startActivity(tableIntent);
                         finish();
                    } else {
                        value.setError("Calibration Can Not be blank");
                    }
                } else {
                    Log.w("Calibration", "ERROR, no active sensor");
                }
            }
        });

    }
}
