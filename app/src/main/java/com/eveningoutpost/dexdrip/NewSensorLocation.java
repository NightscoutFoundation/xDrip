package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.LinkedList;
import java.util.List;

class Location {
    Location(String location, int location_id) {
        this.location = location;
        this.location_id = location_id;
    }
    public String location;
    public int location_id;
}


public class NewSensorLocation extends ActivityWithMenu {
    public static String menu_name = "New sensor location";
    private Button button;
    private Button buttonCancel;
    private RadioGroup radioGroup;
    private EditText sensor_location_other;
    CheckBox DontAskAgain;
    List<Location> locations;

    final int PRIVATE_ID = 200;
    final int OTHER_ID = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_sensor_location);
        button = (Button)findViewById(R.id.saveSensorLocation);
        buttonCancel = (Button)findViewById(R.id.saveSensorLocationCancel);
        sensor_location_other = (EditText) findViewById(R.id.edit_sensor_location);
        sensor_location_other.setEnabled(false);
        DontAskAgain = (CheckBox)findViewById(R.id.sensorLocationDontAskAgain);
        radioGroup = (RadioGroup) findViewById(R.id.myRadioGroup);
        addListenerOnButton();

        locations = new LinkedList<Location>();

        locations.add(new Location("I don't wish to share", PRIVATE_ID));
        locations.add(new Location("Upper arm", 1));
        locations.add(new Location("Thigh", 2));
        locations.add(new Location("Belly (abdomen)", 3));
        locations.add(new Location("Lower back", 4));
        locations.add(new Location("Buttocks", 5));
        locations.add(new Location("Other", OTHER_ID));

        for(Location location : locations) {
            AddButton(location.location, location.location_id);
        }
        radioGroup.check(PRIVATE_ID);

    }


    private void AddButton(String text, int id) {
        RadioButton newRadioButton = new RadioButton(this);
        newRadioButton.setText(text);
        newRadioButton.setId(id);
        LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT);
        radioGroup.addView(newRadioButton);

    }

    @Override
    public String getMenuName() {
        return menu_name;
    }

    public void addListenerOnButton() {

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                int selectedId = radioGroup.getCheckedRadioButtonId();
                String location = new String();

                if (selectedId == OTHER_ID) {
                    location = sensor_location_other.getText().toString();;
                } else {
                    for(Location it : locations) {
                        if(selectedId == it.location_id) {
                            location = it.location;
                            break;
                        }
                    }
                }
                Toast.makeText(getApplicationContext(), "Sensor locaton is " + location, Toast.LENGTH_LONG).show();


                Log.d("NEW SENSOR", "Sensor location is " + location);
                Sensor.updateSensorLocation(location);

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            //    prefs.edit().putBoolean("store_sensor_location", !DontAskAgain.isChecked()).apply();

                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            //    prefs.edit().putBoolean("store_sensor_location", !DontAskAgain.isChecked()).apply();

                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }
        });

        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == OTHER_ID) {
                    sensor_location_other.setEnabled(true);
                    sensor_location_other.requestFocus();
                } else {
                    sensor_location_other.setEnabled(false);
                }
            }
        });


    }
}
