package com.eveningoutpost.dexdrip.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.profileeditor.TimePickerFragment;

import java.util.Locale;

/**
 * jamorham
 *
 * Start via an intent containing the pref-name for a string preference
 * which will store time of day in seconds 0-86399
 *
 * Picker shows current and automatically saves result
 */

public class TimePickerPrefActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker_pref);
    }

    @Override
    protected void onResume() {
        super.onResume();
        processIncomingIntent();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    private void processIncomingIntent() {
        final Intent intent = getIntent();
        if (intent == null) {
            finish();
            return;
        }
        final String prefName = intent.getStringExtra("pref-name");
        if (prefName == null) {
            finish();
            return;
        }

        final String prefValue = Pref.getString(prefName, "0");
        final int mins = JoH.tolerantParseInt(prefValue, 0) / 60;

        final TimePickerFragment timePickerFragment = new TimePickerFragment().setCloseRunnable(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
        timePickerFragment.setTime(mins / 60, mins % 60);
        timePickerFragment.setTitle(getString(R.string.what_time_of_day_question));
        timePickerFragment.setTimeCallback(newmins -> {
            Pref.setString(prefName, String.format(Locale.US, "%d", newmins * 60));
            finish();
        });
        timePickerFragment.show(this.getFragmentManager(), "TimePicker");
    }
}
