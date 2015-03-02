package com.eveningoutpost.dexdrip;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.eveningoutpost.dexdrip.Models.BgReading;


public class FakeNumbers extends Activity {
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

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.log);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText value = (EditText) findViewById(R.id.bg_value);
                int intValue = Integer.parseInt(value.getText().toString());

                BgReading bgReading = BgReading.create(intValue * 1000, getApplicationContext(), new Date().getTime());
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
