package com.eveningoutpost.dexdrip.ui.graphic;

import android.widget.ImageView;

import com.eveningoutpost.dexdrip.R;

// jamorham

// typically you should extend TrendArrowBase instead of JamTrendArrowImpl, but in this case we only
// want to change the image source and are happy to inherit whatever JamTrendArrow does in the future

class JamTrendArrow3Impl extends JamTrendArrowImpl {

    private static final float ROTATION_OFFSET = 0;
    private static final float SOURCE_SCALE_ADJUST = 0.40f;

    JamTrendArrow3Impl(ImageView v) {
        super(v);
        setImage(R.drawable.candy_cane_cropped);
        setSourceScaleAdjust(SOURCE_SCALE_ADJUST);

        // the candy cane image needs shifting like this
        setMargins(-30, 14, -10, 0);
    }

    // example of how to offset rotation of source images
    @Override
    float calculateRotation(final double mgdl_by_minute) {
        return super.calculateRotation(mgdl_by_minute) + ROTATION_OFFSET;
    }

}
