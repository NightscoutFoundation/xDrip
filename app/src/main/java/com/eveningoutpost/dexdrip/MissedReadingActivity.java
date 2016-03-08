package com.eveningoutpost.dexdrip;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.Models.UserError.Log;
import com.eveningoutpost.dexdrip.Models.AlertType;
import com.eveningoutpost.dexdrip.Sensor;
import com.eveningoutpost.dexdrip.UtilityModels.CollectionServiceStarter;
import com.eveningoutpost.dexdrip.utils.ActivityWithMenu;

import java.util.Calendar;


public class MissedReadingActivity extends ActivityWithMenu {
    public static String menu_name = "Missed reading";
    private Context mContext;
    
    
    private CheckBox checkboxAllDay;
    
    private LinearLayout layoutTimeBetween;
    private LinearLayout timeInstructions;
    private TextView viewTimeStart;
    private TextView viewTimeEnd;
    private TextView timeInstructionsStart;
    private TextView timeInstructionsEnd;
    
    
    private int startHour = 0;
    private int startMinute = 0;
    private int endHour = 23;
    private int endMinute = 59;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_missed_readings);
        mContext = this;
        
        viewTimeStart = (TextView) findViewById(R.id.missed_reading_time_start);
        viewTimeEnd = (TextView) findViewById(R.id.missed_reading_time_end);
        checkboxAllDay = (CheckBox) findViewById(R.id.missed_reading_all_day);
        
        layoutTimeBetween = (LinearLayout) findViewById(R.id.missed_reading_time_between);
        timeInstructions = (LinearLayout) findViewById(R.id.missed_reading_instructions);
        timeInstructionsStart = (TextView) findViewById(R.id.missed_reading_time_start);
        timeInstructionsEnd = (TextView) findViewById(R.id.missed_reading_time_end);
        

        // Set the different controls
        checkboxAllDay.setChecked(true /*at.all_day*/);
        startHour = AlertType.time2Hours(/*at.start_time_minutes*/ 300);
        startMinute = AlertType.time2Minutes(/*at.start_time_minutes*/ 300);
        endHour = AlertType.time2Hours(/*at.end_time_minutes*/ 450);
        endMinute = AlertType.time2Minutes(/*at.end_time_minutes*/ 450);
        
        addListenerOnButtons();
        

    }

    @Override
    public String getMenuName() {
        return menu_name;
    }
    
    void enableAllDayControls() {
        boolean allDay = checkboxAllDay.isChecked();
        if(allDay) {
            layoutTimeBetween.setVisibility(View.GONE);
            timeInstructions.setVisibility(View.GONE);
        } else {
            setTimeRanges();
        }
    }
    
    //??? on destroy ???
    
    
    public void addListenerOnButtons() {
        checkboxAllDay.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            //          @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enableAllDayControls();
            }
        });
        
        View.OnClickListener startTimeListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        startHour = selectedHour;
                        startMinute = selectedMinute;
                        setTimeRanges();
                    }
                }, startHour, startMinute, DateFormat.is24HourFormat(mContext));
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        } ;
        
        View.OnClickListener endTimeListener = new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                TimePickerDialog mTimePicker = new TimePickerDialog(mContext, AlertDialog.THEME_HOLO_DARK, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        endHour = selectedHour;
                        endMinute = selectedMinute;
                        setTimeRanges();
                    }
                }, endHour, endMinute, DateFormat.is24HourFormat(mContext));
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        };
        
        viewTimeStart.setOnClickListener(startTimeListener);
        timeInstructionsStart.setOnClickListener(startTimeListener);
        viewTimeEnd.setOnClickListener(endTimeListener);
        timeInstructionsEnd.setOnClickListener(endTimeListener);

    }
    
    public void setTimeRanges() {
        timeInstructions.setVisibility(View.VISIBLE);
        layoutTimeBetween.setVisibility(View.VISIBLE);
        viewTimeStart.setText(EditAlertActivity.timeFormatString(mContext,startHour, startMinute));
        viewTimeEnd.setText(EditAlertActivity.timeFormatString(mContext, endHour, endMinute));
    }

}
