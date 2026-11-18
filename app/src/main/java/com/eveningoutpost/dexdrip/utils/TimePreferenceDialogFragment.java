package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TimePicker;

import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * The dialog of a {@link TimePreference}: a time picker, in the theme's picker style, that stores the time on "Set".
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class TimePreferenceDialogFragment extends PreferenceDialogFragmentCompat {

    private TimePicker picker;

    /**
     * The dialog for the time setting with {@code key}.
     */
    public static TimePreferenceDialogFragment newInstance(final String key) {
        final TimePreferenceDialogFragment fragment = new TimePreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected View onCreateDialogView(final Context context) {
        picker = new TimePicker(context);
        return picker;
    }

    @Override
    protected void onBindDialogView(final View view) {
        super.onBindDialogView(view);
        final TimePreference preference = (TimePreference) getPreference();
        picker.setCurrentHour(preference.getHour());
        picker.setCurrentMinute(preference.getMinute());
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            ((TimePreference) getPreference()).onTimeSet(picker.getCurrentHour(), picker.getCurrentMinute());
        }
    }
}
