package com.eveningoutpost.dexdrip.ui.helpers;

import androidx.annotation.ColorInt;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

/**
 * jamorham
 *
 * Helper class for working with SpannableStrings
 */

public class Span {

    public static SpannableString wholeSpanColor(SpannableString ret, @ColorInt int color) {
        return wholeSpan(ret, new ForegroundColorSpan(color));
    }

    public static SpannableString colorSpan(String ret, @ColorInt int color) {
        return wholeSpan(new SpannableString(" " + ret + " "), new BackgroundColorSpan(color));
    }

    public static void textSpanColor(String str, @ColorInt int color) {
        wholeSpan(new SpannableString(str), new ForegroundColorSpan(color));
    }

    // set the whole spannable string to whatever this span is
    public static SpannableString wholeSpan(SpannableString ret, Object what) {
        ret.setSpan(what, 0, ret.length(), 0);
        return ret;
    }

    public static CharSequence join(SpannableString... args) {
        return join(false, args);
    }

    public static SpannableString join(boolean add_spaces, SpannableString... args) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (SpannableString arg : args) {
            if (arg == null) continue;
            if (add_spaces && builder.length() > 0) {
                builder.append(" ");
            }
            builder.append(arg);
        }
        return new SpannableString(builder);
    }

}
