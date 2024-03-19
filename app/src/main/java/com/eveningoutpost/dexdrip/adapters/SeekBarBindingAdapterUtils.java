package com.eveningoutpost.dexdrip.adapters;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import android.widget.SeekBar;

// jamorham

// make a Seekbar use string instead of int for progress with progressString attribute

public class SeekBarBindingAdapterUtils {

    @BindingAdapter(value = {"progressString"})
    public static void setProgressString(SeekBar view, String progress) {
        int value = 0;
        try {
            value = Integer.parseInt(progress);
        } catch (NumberFormatException e) {
            //
        }
        view.setProgress(value);
    }

    @InverseBindingAdapter(attribute = "progressString")
    public static String getProgressString(SeekBar view) {
        return "" + view.getProgress();
    }

    @BindingAdapter(value = {"onProgressChanged", "progressStringAttrChanged"}, requireAll = false)
    public static void setListeners(SeekBar view, final SeekBar.OnSeekBarChangeListener listener,
                                    final InverseBindingListener attrChange) {
        if (attrChange == null) {
            view.setOnSeekBarChangeListener(listener);
        } else {
            view.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (listener != null) {
                        listener.onProgressChanged(seekBar, progress, fromUser);
                    }
                    attrChange.onChange();
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    if (listener != null) {
                        listener.onStartTrackingTouch(seekBar);
                    }
                    attrChange.onChange();
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    if (listener != null) {
                        listener.onStopTrackingTouch(seekBar);
                    }
                    attrChange.onChange();
                }

            });
        }
    }

}
