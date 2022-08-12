package com.eveningoutpost.dexdrip.ui.classifier;

// jamorham

import android.graphics.Color;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.UtilityModels.PointValueExtended;
import com.eveningoutpost.dexdrip.ui.helpers.BitmapLoader;
import com.eveningoutpost.dexdrip.ui.helpers.ColorUtil;

import lecho.lib.hellocharts.model.PointValue;

public class NoteClassifier {

    // TODO add to colorpickerx
    private static final int red = ColorUtil.blendColor(Color.parseColor("#FF2929"), Color.TRANSPARENT, 0.2f);
    private static final int amber = ColorUtil.blendColor(Color.parseColor("#FF9400"), Color.TRANSPARENT, 0.2f);
    private static final int green = ColorUtil.blendColor(Color.parseColor("#2eb82e"), Color.TRANSPARENT, 0.2f);
    private static final int grey = ColorUtil.blendColor(Color.parseColor("#666666"), Color.TRANSPARENT, 0.2f);

    public static PointValue noteToPointValue(final String note) {

        final String haystack = note.toLowerCase();
        if (haystack.contains("battery low")) {
            return red(R.drawable.alert_icon, note);

        } else if (haystack.contains("stopped")) {
            return red(R.drawable.flag_variant, note);

        } else if (haystack.contains("paused")) {
            return amber(R.drawable.flag_variant, note);

        } else if (haystack.contains("started")) {
            return green(R.drawable.flag_variant, note);

        } else if (haystack.contains("cartridge low")) {
            return red(R.drawable.alert_icon, note);

        } else if (haystack.equals("connection timed out")) {
            return red(R.drawable.alert_icon, note);

        } else if (haystack.startsWith("warning")) {
            return amber(R.drawable.alert_icon, note);

        } else if (haystack.startsWith("maintenance")) {
            return grey(R.drawable.wrench_icon, note);

        } else if (haystack.startsWith("reminder")) {
            return amber(R.drawable.note_text_icon, note);

        } else if (haystack.startsWith("priming")) {
            return grey(R.drawable.dropper, note);
        }
        return grey(R.drawable.note_text_icon, note);
    }


    private static PointValue amber(@DrawableRes int id, String note) {
        return icon(id, amber, note);
    }

    private static PointValue red(@DrawableRes int id, String note) {
        return icon(id, red, note);
    }

    private static PointValue green(@DrawableRes int id, String note) {
        return icon(id, green, note);
    }

    private static PointValue grey(@DrawableRes int id, String note) {
        return icon(id, grey, note);
    }

    private static PointValue icon(@DrawableRes int id, @ColorInt int color, String note) {
        final PointValueExtended pv = new PointValueExtended();
        BitmapLoader.loadAndSetKey(pv, id, 0);
        pv.setBitmapTint(color);
        pv.setBitmapScale(1f);
        pv.note = note;
        return pv;
    }

}
