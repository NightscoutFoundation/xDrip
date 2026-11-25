package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A time of day setting, stored as a long. Its dialog is {@link TimePreferenceDialogFragment}.
 */
public class TimePreference extends DialogPreference {
    private Calendar calendar;

    public TimePreference(Context ctxt) {
        this(ctxt, null);
    }

    public TimePreference(Context ctxt, AttributeSet attrs) {
        super(ctxt, attrs);
        init();
    }

    public TimePreference(Context ctxt, AttributeSet attrs, int defStyle) {
        super(ctxt, attrs, defStyle);
        init();
    }

    private void init() {
        setPositiveButtonText("Set");
        setNegativeButtonText("Close");
        calendar = new GregorianCalendar();
    }

    /** The hour of day the dialog opens on. */
    int getHour() {
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /** The minute the dialog opens on. */
    int getMinute() {
        return calendar.get(Calendar.MINUTE);
    }

    /**
     * Takes the time set in the dialog, as the old dialog's positive button did.
     */
    void onTimeSet(final int hour, final int minute) {
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        setSummary(getSummary());
        if (callChangeListener(calendar.getTimeInMillis())) {
            persistLong(calendar.getTimeInMillis());
            notifyChanged();
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return (a.getString(index));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {

        if (restoreValue) {
            if (defaultValue == null) {
                calendar.setTimeInMillis(getPersistedLong(System.currentTimeMillis()));
            } else {
                calendar.setTimeInMillis(Long.parseLong(getPersistedString((String) defaultValue)));
            }
        } else {
            if (defaultValue == null) {
                calendar.setTimeInMillis(System.currentTimeMillis());
            } else {
                calendar.setTimeInMillis(Long.parseLong((String) defaultValue));
            }
        }
        setSummary(getSummary());
    }

    @Override
    public CharSequence getSummary() {
        if (calendar == null) {
            return null;
        }
        return DateFormat.getTimeFormat(getContext()).format(new Date(calendar.getTimeInMillis()));
    }
}