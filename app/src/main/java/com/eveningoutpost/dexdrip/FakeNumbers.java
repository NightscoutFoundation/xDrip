package com.eveningoutpost.dexdrip;

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

import java.util.Calendar;


public class FakeNumbers extends Activity {
    public Button button;
    public DatePicker dp;
    public TimePicker tp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fake_numbers);

        button = (Button)findViewById(R.id.log);
        dp = (DatePicker)findViewById(R.id.datePicker);
        tp = (TimePicker)findViewById(R.id.timePicker);
        addListenerOnButton();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_fake_numbers, menu);
        return true;
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

        button = (Button)findViewById(R.id.log);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Calendar calendar = Calendar.getInstance();
                calendar.set(dp.getYear(), dp.getMonth(), dp.getDayOfMonth(),
                        tp.getCurrentHour(), tp.getCurrentMinute(), 0);
                long startTime = calendar.getTime().getTime();
                EditText value = (EditText) findViewById(R.id.bg_value);
                int intValue = Integer.parseInt(value.getText().toString());

                BgReading bgReading = BgReading.create(intValue * 1000);
                bgReading.save();
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
    }
}
