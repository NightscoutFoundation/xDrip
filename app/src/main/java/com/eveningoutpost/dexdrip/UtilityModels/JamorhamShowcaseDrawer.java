package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jamorham on 20/06/2016.
 */

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;
import android.util.Log;

import com.eveningoutpost.dexdrip.R;


public class JamorhamShowcaseDrawer extends StandardShowcaseDrawer {

    private final static String TAG = "jamorhamshowcasedrawer";
    private static final int ALPHA_60_PERCENT = 153;
    private static final int ALPHA_HIGH_PERCENT = 223;
    private final float outerRadius;
    private final float innerRadius;


    public JamorhamShowcaseDrawer(Resources resources, Resources.Theme theme) {
        super(resources, theme);
        outerRadius = resources.getDimension(R.dimen.showcase_radius_outer);
        innerRadius = resources.getDimension(R.dimen.showcase_radius_inner);
    }

    public JamorhamShowcaseDrawer(Resources resources, Resources.Theme theme, float outerRadiusp, float innerRadiusp) {
        super(resources, theme);

        final float scale = resources.getDisplayMetrics().density;
        outerRadius = outerRadiusp*scale;
        innerRadius = innerRadiusp*scale;
    }

    @Override
    public void drawShowcase(Bitmap buffer, float x, float y, float scaleMultiplier) {
        Canvas bufferCanvas = new Canvas(buffer);
        eraserPaint.setAlpha(ALPHA_HIGH_PERCENT);
        bufferCanvas.drawCircle(x, y, outerRadius, eraserPaint);
        eraserPaint.setAlpha(0);
        bufferCanvas.drawCircle(x, y, innerRadius, eraserPaint);

        int halfW = getShowcaseWidth() / 2;
        int halfH = getShowcaseHeight() / 2;
        int left = (int) (x - halfW);
        int top = (int) (y - halfH);
        showcaseDrawable.setBounds(left, top, left + getShowcaseWidth(), top + getShowcaseHeight());
        showcaseDrawable.draw(bufferCanvas);
    }


    @Override
    public int getShowcaseWidth() {
        return (int) (outerRadius);
    }

    @Override
    public int getShowcaseHeight() {
        return (int) (outerRadius);
    }

    @Override
    public float getBlockedRadius() {
        return innerRadius;
    }

}


