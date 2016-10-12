package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.Models.Sensor;
import com.eveningoutpost.dexdrip.Models.UserError.Log;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;


public class DoubleCalibrationActivity extends ActivityWithMenu {
    Button button;
    public static String menu_name = "Add Double Calibration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_double_calibration);
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
                    EditText value_1 = (EditText) findViewById(R.id.bg_value_1);
                    EditText value_2 = (EditText) findViewById(R.id.bg_value_2);
                    String string_value_1 = value_1.getText().toString();
                    String string_value_2 = value_2.getText().toString();

                    if (!TextUtils.isEmpty(string_value_1)) {
                        if (!TextUtils.isEmpty(string_value_2)) {
                            final double calValue_1 = Double.parseDouble(string_value_1);
                            final double calValue_2 = Double.parseDouble(string_value_2);

                            final double multiplier = Home.getPreferencesStringWithDefault("units", "mgdl").equals("mgdl") ? 1 : Constants.MMOLL_TO_MGDL;
                            if ((calValue_1 * multiplier < 40) || (calValue_1 * multiplier > 400)
                                    || (calValue_2 * multiplier < 40) || (calValue_2 * multiplier > 400)) {
                                JoH.static_toast_long("Calibration out of range");
                            } else {
                                Calibration.initialCalibration(calValue_1, calValue_2, getApplicationContext());

                                final boolean wear_integration = Home.getPreferencesBoolean("wear_sync", false);//KS
                                if (wear_integration) {
                                    android.util.Log.d("DoubleCalibration", "start WatchUpdaterService with ACTION_SYNC_CALIBRATION");
                                    startService(new Intent(v.getContext(), WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_SYNC_CALIBRATION));
                                }

                                Intent tableIntent = new Intent(v.getContext(), Home.class);
                                startActivity(tableIntent);
                                finish();
                            }
                        } else {
                            value_2.setError("Calibration Can Not be blank");
                        }
                    } else {
                        value_1.setError("Calibration Can Not be blank");
                    }
                } else {
                    Log.w("DoubleCalibration", "ERROR, sensor is not active");
                }
            }
        });

    }
}
