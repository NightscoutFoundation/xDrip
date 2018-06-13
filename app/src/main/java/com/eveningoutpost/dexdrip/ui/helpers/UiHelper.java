package com.eveningoutpost.dexdrip.ui.helpers;

import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.eveningoutpost.dexdrip.xdrip;

// jamorham

public class UiHelper {

    public static int convertDpToPixel(float dp){
        Resources resources = xdrip.getAppContext().getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        int px = (int) (dp * (metrics.densityDpi / 160f));
        return px;
    }

}
