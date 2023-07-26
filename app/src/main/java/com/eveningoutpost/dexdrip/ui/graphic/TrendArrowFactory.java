package com.eveningoutpost.dexdrip.ui.graphic;

import android.os.Build;
import android.widget.ImageView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

/*
 * created by jamorham
 *
 * Handle object creation for named TrendArrow type
 * Also handle persistent selection/storage
 *
 */


public class TrendArrowFactory {


    private static final String PREF_TREND_ARROW_TYPE = "trend_arrow_type";

    public static ITrendArrow create(ImageView imageView) {
        final String arrow_type = Pref.getString(PREF_TREND_ARROW_TYPE, "");

        // yucky workaround for broken LG lack of gradient fill.
        if (Build.VERSION.SDK_INT == 23 && Build.MANUFACTURER.startsWith("LG")) {
            if (arrow_type.equals("JamTrendArrowImpl")) {
                JoH.static_toast_long("LG Phones have problems with some arrows, choose another");
                return create("JamTrendArrow2Impl", imageView);
            }
        }
        return create(arrow_type, imageView);
    }

    public static void setType(String type) {
        Pref.setString(PREF_TREND_ARROW_TYPE, type);
    }

    public static boolean setType(int position) {
        if (getArrayPosition() == position) return false;
        final String[] types = getArrayValues();
        setType(types[position]);
        return true;
    }

    public static int getArrayPosition() {
        final String stored = Pref.getString(PREF_TREND_ARROW_TYPE, null);
        if (stored == null) return 0;
        final String[] values = getArrayValues();
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(stored)) return i;
        }
        return 0;
    }


    public static ITrendArrow create(String type, ImageView imageView) {
        switch (type) {

            case "JamTrendArrowImpl":
                return new JamTrendArrowImpl(imageView);

            case "JamTrendArrow2Impl":
                return new JamTrendArrow2Impl(imageView);

            case "JamTrendArrow2bImpl":
                return new JamTrendArrow2bImpl(imageView);

            case "JamTrendArrow3Impl":
                return new JamTrendArrow3Impl(imageView);


            // add any other arrow types here


            // try to handle app downgrades as gracefully as possible
            default:
                return new JamTrendArrowImpl(imageView);


        }
    }

    private static String[] getArrayValues() {
        return xdrip.getAppContext().getResources().getStringArray(R.array.TrendArrowValues);
    }

}
