package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError.Log;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.models.Calibration;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PrefsViewImpl;
import com.eveningoutpost.dexdrip.databinding.ActivityDoubleCalibrationBinding;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import static com.eveningoutpost.dexdrip.Home.startWatchUpdaterService;


public class DoubleCalibrationActivity extends ActivityWithMenu {
    Button button;
    private static final String TAG = "DoubleCalib";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        xdrip.checkForcedEnglish(this);
        super.onCreate(savedInstanceState);
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        ActivityDoubleCalibrationBinding binding = ActivityDoubleCalibrationBinding.inflate(getLayoutInflater());
        binding.setPrefs(new PrefsViewImpl());
        setContentView(binding.getRoot());
        addListenerOnButton();
    }

    @Override
    protected void onResume() {
        xdrip.checkForcedEnglish(this);
        super.onResume();
    }

    @Override
    public String getMenuName() {
        return getString(R.string.initial_calibration);
    }

    public void addListenerOnButton() {

        button = findViewById(R.id.save_calibration_button);
        final Activity activity = this;
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                try {
                    View view = activity.getCurrentFocus();
                    if (view != null) {
                        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                } catch (Exception e) {
                    // failed to close keyboard
                }

                if (Sensor.isActive()) {
                    final EditText value_1 = (EditText) findViewById(R.id.bg_value_1);
                    final EditText value_2 = (EditText) findViewById(R.id.bg_value_2);
                    String string_value_1 = value_1.getText().toString();
                    String string_value_2 = value_2.getText().toString();

                    if (!TextUtils.isEmpty(string_value_1)) {
                        if (TextUtils.isEmpty(string_value_2)) {
                            string_value_2 = string_value_1; // just use single calibration if all that is entered
                        }
                        if (!TextUtils.isEmpty(string_value_2)) {
                            try {
                                final double calValue_1 = Double.parseDouble(string_value_1);
                                final double calValue_2 = Double.parseDouble(string_value_2);

                                final double multiplier = Pref.getString("units", "mgdl").equals("mgdl") ? 1 : Constants.MMOLL_TO_MGDL;
                                if ((calValue_1 * multiplier < 40) || (calValue_1 * multiplier > 400)
                                        || (calValue_2 * multiplier < 40) || (calValue_2 * multiplier > 400)) {
                                    JoH.static_toast_long(getString(R.string.calibration_out_of_range));
                                } else {
                                    Calibration.initialCalibration(calValue_1, calValue_2, getApplicationContext());

                                    //startWatchUpdaterService(v.getContext(), WatchUpdaterService.ACTION_SYNC_CALIBRATION, TAG);

                                    Intent tableIntent = new Intent(v.getContext(), Home.class);
                                    startActivity(tableIntent);
                                    finish();
                                }
                            } catch (NumberFormatException e) {
                                JoH.static_toast_long(getString(R.string.invalid_calibration_number));
                            }
                        } else {
                            value_2.setError(getString(R.string.calibration_can_not_be_blank));
                        }
                    } else {
                        value_1.setError(getString(R.string.calibration_can_not_be_blank));
                    }
                } else {
                    Log.w("DoubleCalibration", "ERROR, sensor is not active");
                }
            }
        });

    }
}
