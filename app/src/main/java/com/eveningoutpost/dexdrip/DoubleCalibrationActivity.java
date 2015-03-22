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


public class DoubleCalibrationActivity  extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    Button button;
    private String menu_name = "Add Double Calibration";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(CollectionServiceStarter.isBTShare(getApplicationContext())) {
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.activity_double_calibration);
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
                    EditText value_1 = (EditText) findViewById(R.id.bg_value_1);
                    EditText value_2 = (EditText) findViewById(R.id.bg_value_2);
                    String string_value_1 = value_1.getText().toString();
                    String string_value_2 = value_2.getText().toString();

                    if (!TextUtils.isEmpty(string_value_1)){
                        if(!TextUtils.isEmpty(string_value_2)) {
                            double calValue_1 = Double.parseDouble(string_value_1);
                            double calValue_2 = Double.parseDouble(string_value_2);
                            Calibration.initialCalibration(calValue_1, calValue_2, getApplicationContext());
                            Intent tableIntent = new Intent(v.getContext(), Home.class);
                            startActivity(tableIntent);
                            finish();
                        } else {
                            value_2.setError("Calibration Can Not be blank");
                        }
                    } else {
                        value_1.setError("Calibration Can Not be blank");
                    }
                } else {
                    Log.w("DoubleCalibration", "ERROR");
                }
            }
        });

    }
}
