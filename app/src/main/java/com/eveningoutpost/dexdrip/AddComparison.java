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


public class AddComparison extends Activity implements NavigationDrawerFragment.NavigationDrawerCallbacks {
    Button button;
    private String menu_name = "Add Comparison";
    private NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_comparison);
        addListenerOnButton();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_add_calibration, menu);
        return true;
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
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void addListenerOnButton() {

        button = (Button) findViewById(R.id.save_calibration_button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if (Sensor.isActive()) {
                    EditText value = (EditText) findViewById(R.id.bg_value);
                    int intValue = Integer.parseInt(value.getText().toString());

                    Comparison comparison = Comparison.create(intValue);

                    Log.w("Comparison ENTERED: ", "" + intValue);
                    Intent tableIntent = new Intent(v.getContext(), Home.class);
                    startActivity(tableIntent);
                    finish();
                } else {
                    Log.w("CANNOT CALIBRATE WITHOUT CURRENT SENSOR", "ERROR");
                }
            }
        });

    }
}
