package com.eveningoutpost.dexdrip.ProfileEditor;

/**
 * Created by jamorham on 22/06/2016.
 */

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.TimePicker;


public class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private int hour;
    private int minute;
    private Integer timeobj;
    ProfileAdapter.ProfileCallBacks callback;

    public void setTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
    }

    public void setTimeObject(Integer timeobj) {
        this.timeobj = timeobj;
        this.hour = timeobj / 60;
        this.minute = timeobj % 60;
    }

    public void setTimeCallback(ProfileAdapter.ProfileCallBacks callback) {
        this.callback = callback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        return new TimePickerDialog(getActivity(), this, this.hour, this.minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    //onTimeSet() callback method
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        if (timeobj != null) {
            timeobj = (hourOfDay * 60) + minute;
        }
        if (callback != null) {
            callback.onTimeUpdated((hourOfDay * 60) + minute);
        }
    }
}