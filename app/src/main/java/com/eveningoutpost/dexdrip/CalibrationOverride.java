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
import com.eveningoutpost.dexdrip.UtilityModels.UndoRedo;
import com.eveningoutpost.dexdrip.UtilityModels.CalibrationSendQueue;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;
import com.eveningoutpost.dexdrip.wearintegration.WatchUpdaterService;


public class CalibrationOverride extends ActivityWithMenu {
        Button button;
    public static final String menu_name = "Override Calibration";
    private static final String TAG = "OverrideCalib";

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
                    if (!TextUtils.isEmpty(string_value)) {
                        try {
                            final double calValue = JoH.tolerantParseDouble(string_value);

                            Calibration last_calibration = Calibration.lastValid();
                            last_calibration.sensor_confidence = 0;
                            last_calibration.slope_confidence = 0;
                            last_calibration.save();
                            CalibrationSendQueue.addToQueue(last_calibration, getApplicationContext());
                            // TODO we need to push the nixing of this last calibration

                            final Calibration calibration = Calibration.create(calValue, getApplicationContext());
                            if (calibration != null) {
                                UndoRedo.addUndoCalibration(calibration.uuid);
                                GcmActivity.pushCalibration(string_value, "0");

                                final boolean wear_integration = Home.getPreferencesBoolean("wear_sync", false);//KS
                                if (wear_integration) {
                                    android.util.Log.d("CalibrationOverride", "start WatchUpdaterService with ACTION_SYNC_CALIBRATION");
                                    startService(new Intent(v.getContext(), WatchUpdaterService.class).setAction(WatchUpdaterService.ACTION_SYNC_CALIBRATION));
                                }

                            } else {
                                Log.e(TAG, "Calibration creation resulted in null");
                                JoH.static_toast_long("Could not create calibration!");
                            }
                            Intent tableIntent = new Intent(v.getContext(), Home.class);
                            startActivity(tableIntent);
                            finish();
                        } catch (NumberFormatException e) {
                            value.setError("Number error: " + e);
                        }
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
