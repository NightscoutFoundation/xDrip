package com.eveningoutpost.dexdrip.ui.graphic;

import android.widget.ImageView;

import com.eveningoutpost.dexdrip.R;

// jamorham

// typically you should extend TrendArrowBase instead of JamTrendArrowImpl, but in this case we only
// want to change the image source and are happy to inherit whatever JamTrendArrow does in the future

public class JamTrendArrow2bImpl extends JamTrendArrowImpl {

    public JamTrendArrow2bImpl(ImageView v) {
        super(v);
        setImage(R.drawable.ic_arrow_forward_color_24dp);
    }

}
