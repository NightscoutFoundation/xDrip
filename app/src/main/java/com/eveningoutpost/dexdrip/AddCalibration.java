package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.Models.Calibration;
import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;


public class AddCalibration extends AppCompatActivity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    Button button;
    public static String menu_name = "Add Calibration";
    private static final String TAG = "AddCalibration";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_add_calibration);
        addListenerOnButton();
        automatedCalibration();
    }

    protected void onResume() {
        super.onResume();
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), menu_name, this);
        automatedCalibration();
    }
    @Override
    public void onNavigationDrawerItemSelected(int position) {
        mNavigationDrawerFragment.swapContext(position);
    }

    // jamorham - receive automated calibration via broadcast intent / tasker receiver
    public void automatedCalibration() {

        final long estimatedInterstitialLagSeconds = 600; // how far behind venous glucose do we estimate

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            final String string_value = extras.getString("bg_string");
            final String bg_age = extras.getString("bg_age");
            final String from_external = extras.getString("from_external", "false");

            if ((Sensor.isActive()
                    || PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("dex_collection_method", "").equals("Follower"))) {

                if (!TextUtils.isEmpty(string_value)) {
                    if (!TextUtils.isEmpty(bg_age)) {
                        final double calValue = Double.parseDouble(string_value);
                        new Thread() {
                            @Override
                            public void run() {
                                long bgAgeNumber = Long.parseLong(bg_age);

                                // most appropriate raw value to calculate calibration
                                // from should be some time after venous glucose reading
                                // adjust timestamp for this if we can
                                if (bgAgeNumber > estimatedInterstitialLagSeconds) {
                                    bgAgeNumber = bgAgeNumber - estimatedInterstitialLagSeconds;
                                }
                                // Sanity checking can go here


                                Calibration calibration = Calibration.create(calValue, bgAgeNumber, getApplicationContext());

                                if (from_external.equals("true")) {
                                    Log.d("jamorham calib", "Relaying tasker pushed calibration");
                                    GcmActivity.pushCalibration(string_value, bg_age);
                                }
                            }
                        }.start();

                    } else {
                        Log.w("CALLERROR", "ERROR during automated calibration - no valid bg age");
                    }
                } else {
                    Log.w("CALERROR", "ERROR during automated calibration - no valid value");
                }
            } else {
                Log.w("CALERROR", "ERROR during automated calibration - no active sensor");
            }
            finish();
        }


    }


    public void addListenerOnButton() {

        button = (Button) findViewById(R.id.save_calibration_button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {

                if ((Sensor.isActive()
                        || PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString("dex_collection_method", "").equals("Follower"))) {
                    EditText value = (EditText) findViewById(R.id.bg_value);
                    final String string_value = value.getText().toString();
                    if (!TextUtils.isEmpty(string_value)) {

                        //new Thread() {
                        //    @Override
                        //    public void run() {

                                try {
                                    double calValue = Double.parseDouble(string_value);
                                    // sanity check the number?

                                    Calibration calibration = Calibration.create(calValue, getApplicationContext());

                                    Intent tableIntent = new Intent(v.getContext(), Home.class);
                                    startActivity(tableIntent);
                                    GcmActivity.pushCalibration(string_value, "0");
                                } catch (NumberFormatException e) {
                                    Log.e(TAG, "Number format exception ", e);
                                    Home.toaststatic("Got error parsing number in calibration");
                                }
                           // }
                       // }.start();
                        finish();
                    } else {
                        value.setError("Calibration Can Not be blank");
                    }
                } else {
                    Log.w("CALERROR", "Sensor is not active, cannot calibrate");
                }
            }
        });

    }
}
