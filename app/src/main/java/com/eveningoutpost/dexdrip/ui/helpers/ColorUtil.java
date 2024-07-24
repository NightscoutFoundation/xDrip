package com.eveningoutpost.dexdrip.ui.helpers;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;

public class ColorUtil {

    // originally from https://stackoverflow.com/a/17490633 with extra wrapper methods added by jamorham

    public static ColorFilter hueFilter(float value) {
        return new ColorMatrixColorFilter(adjustHue(value));
    }

    public static ColorMatrix adjustHue(float value) {
        final ColorMatrix cm = new ColorMatrix();
        adjustHue(cm, value);
        return cm;
    }

    public static void adjustHue(ColorMatrix cm, float value) {
        value = cleanValue(value, 180f) / 180f * (float) Math.PI;
        if (value == 0) {
            return;
        }
        float cosVal = (float) Math.cos(value);
        float sinVal = (float) Math.sin(value);
        float lumR = 0.213f;
        float lumG = 0.715f;
        float lumB = 0.072f;
        float[] mat = new float[]
                {
                        lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0, 0,
                        lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0, 0,
                        0f, 0f, 0f, 1f, 0f,
                        0f, 0f, 0f, 0f, 1f};
        cm.postConcat(new ColorMatrix(mat));
    }

    private static float cleanValue(float p_val, float p_limit) {
        return Math.min(p_limit, Math.max(-p_limit, p_val));
    }

    public static int blendColor(@ColorInt int color1, @ColorInt int color2) {
        return blendColor(color1, color2, 0.5f);
    }

    public static int blendColor(@ColorInt int color1, @ColorInt int color2, float ratio) {
        return ColorUtils.blendARGB(color1, color2, ratio);
    }

    @ColorInt
    public static int adjustHue(@ColorInt int color, float rotation) {
        final float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[0] = (hsv[0] + rotation) % 360;
        // copy alpha from source
        return Color.HSVToColor(hsv) & 0x00FFFFFF | (color & 0xFF000000);
    }

    public static String colorIntToHex(@ColorInt int color) {
        return String.format("#%08X", color);
    }

}
