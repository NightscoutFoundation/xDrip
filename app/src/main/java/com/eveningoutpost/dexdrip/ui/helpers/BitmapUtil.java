package com.eveningoutpost.dexdrip.ui.helpers;

// jamorham

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;

import com.eveningoutpost.dexdrip.xdrip;

public class BitmapUtil {

    public static Bitmap getTiled(final Bitmap source, final int xSize, final int ySize) {
        final Bitmap tiledBitmap = Bitmap.createBitmap(xSize, ySize, Bitmap.Config.ARGB_8888);
        if (source != null) {
            final Canvas canvas = new Canvas(tiledBitmap);
            final BitmapDrawable drawable = new BitmapDrawable(xdrip.getAppContext().getResources(), source);
            drawable.setBounds(0, 0, xSize, ySize);
            drawable.setTileModeX(Shader.TileMode.REPEAT);
            drawable.setTileModeY(Shader.TileMode.REPEAT);
            drawable.draw(canvas);
        } // returns blank bitmap if source is null
        return tiledBitmap;
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    public static int getScreenDpi() {
        return Resources.getSystem().getDisplayMetrics().densityDpi;
    }

}
