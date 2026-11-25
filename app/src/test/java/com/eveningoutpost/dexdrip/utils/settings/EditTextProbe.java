package com.eveningoutpost.dexdrip.utils.settings;

import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.SpannableStringBuilder;
import android.text.method.KeyListener;
import android.text.method.TransformationMethod;
import android.widget.EditText;

/**
 * Describes a text field by what its XML attributes control, so two fields can be compared in one line.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
final class EditTextProbe {

    /** Characters typed into both fields to compare which ones each key listener lets through. */
    private static final String PROBE = "0123456789.,-+ eE:/abcXYZ_@#";

    private EditTextProbe() {
    }

    /**
     * One line with the input type, length limit, key listener and the characters it accepts, line limit,
     * selectability, transformation (password dots, single line) and whether the font is monospace.
     */
    static String describe(final EditText field) {
        final KeyListener listener = field.getKeyListener();
        final TransformationMethod transformation = field.getTransformationMethod();
        return "inputType=0x" + Integer.toHexString(field.getInputType())
                + " maxLength=" + maxLength(field)
                + " keyListener=" + (listener == null ? "null" : listener.getClass().getSimpleName())
                + " accepts=" + accepted(listener)
                + " maxLines=" + field.getMaxLines()
                + " selectable=" + field.isTextSelectable()
                + " transformation=" + (transformation == null ? "null" : transformation.getClass().getSimpleName())
                + " monospace=" + (field.getTypeface() == Typeface.MONOSPACE);
    }

    private static String maxLength(final EditText field) {
        for (final InputFilter filter : field.getFilters()) {
            if (filter instanceof InputFilter.LengthFilter) {
                return String.valueOf(((InputFilter.LengthFilter) filter).getMax());
            }
        }
        return "none";
    }

    private static String accepted(final KeyListener listener) {
        if (!(listener instanceof InputFilter)) {
            return "all";
        }
        final CharSequence kept = ((InputFilter) listener).filter(PROBE, 0, PROBE.length(),
                new SpannableStringBuilder(), 0, 0);
        return kept == null ? PROBE : kept.toString();
    }
}
