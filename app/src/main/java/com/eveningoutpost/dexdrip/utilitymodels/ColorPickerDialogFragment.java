package com.eveningoutpost.dexdrip.utilitymodels;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import com.rarepebble.colorpicker.ColorPickerView;

/**
 * The dialog of a {@link ColorPicker}, built as the colour library's own dialog was: no title, the library's colour
 * picker, the positive button to take the colour, and a neutral button to go back to the default when the setting
 * names one.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class ColorPickerDialogFragment extends PreferenceDialogFragmentCompat {

    /**
     * The dialog for the colour setting with {@code key}.
     */
    public static ColorPickerDialogFragment newInstance(final String key) {
        final ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(final AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        final ColorPicker preference = (ColorPicker) getPreference();
        final ColorPickerView picker = new ColorPickerView(requireContext());
        picker.setColor(preference.getPickerStartColor());
        picker.showAlpha(preference.showAlpha);
        picker.showHex(preference.showHex);
        builder.setTitle(null);
        builder.setView(picker);
        builder.setPositiveButton(preference.getPositiveButtonText(),
                (dialog, which) -> preference.onColorChosen(picker.getColor()));
        if (preference.selectNoneButtonText != null) {
            builder.setNeutralButton(preference.selectNoneButtonText,
                    (dialog, which) -> preference.onColorChosen(null));
        }
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        ((ColorPicker) getPreference()).onDialogClosed(positiveResult);
    }
}
