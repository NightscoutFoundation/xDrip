package com.eveningoutpost.dexdrip;

import java.util.Date;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.eveningoutpost.dexdrip.models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.models.AlertType;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

public class FakeNumbers extends ActivityWithMenu {
    public static String menu_name = "Fake Numbers";

    public Button button;
    public DatePicker dp;
    public TimePicker tp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_numbers);

        button = (Button)findViewById(R.id.log);
        addListenerOnButton();

    }
    @Override
    public String getMenuName() {
        return menu_name;
    }
    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.log);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText value = (EditText) findViewById(R.id.bg_value);
                int intValue = Integer.parseInt(value.getText().toString());
                int filterdValue = intValue;
                if (intValue > 200) {
                    filterdValue = (int)(filterdValue * 1.2);
                }

                BgReading bgReading = BgReading.create(intValue * 1000, filterdValue* 1000,  getApplicationContext(), new Date().getTime());
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });

        button = (Button)findViewById(R.id.StartTest);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ActiveBgAlert aba = ActiveBgAlert.getOnly();
                ActiveBgAlert.ClearData();
                ActiveBgAlert.Create("some string", true, new Date().getTime());


            }
        });

        button = (Button)findViewById(R.id.StartTestAlerts);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertType.testAll(getApplicationContext());
                BgReading.TestgetUnclearTimes();

            }
        });
    }
}
