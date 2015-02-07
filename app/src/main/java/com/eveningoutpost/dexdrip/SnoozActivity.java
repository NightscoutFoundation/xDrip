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

import com.eveningoutpost.dexdrip.Models.ActiveBgAlert;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Models.BgReading;
import com.eveningoutpost.dexdrip.UtilityModels.AlertPlayer;


public class SnoozActivity extends Activity {
    public Button button;
    public DatePicker dp;
    public TimePicker tp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_snooze);

        addListenerOnButton();

    }

    public void addListenerOnButton() {

        button = (Button)findViewById(R.id.button_snooze);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText value = (EditText) findViewById(R.id.snooze);
                int intValue = Integer.parseInt(value.getText().toString());

                AlertPlayer.getPlayer().Snooze(getApplicationContext(), intValue);

                
                Intent intent = new Intent(getApplicationContext(), Home.class);
                startActivity(intent);
                finish();
            }

        });
        
    }
}