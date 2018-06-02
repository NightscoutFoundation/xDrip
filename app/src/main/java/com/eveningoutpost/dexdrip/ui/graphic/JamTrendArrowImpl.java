package com.eveningoutpost.dexdrip.ui.graphic;

// jamorham

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.xdrip;

public class JamTrendArrowImpl implements ITrendArrow {

    private static final double LARGE_CHANGE = 2d; // some devices use 3.0
    private static final double MAX_CHANGE = 3.5d; // some devices use 3.0
    private static final float SOURCE_SCALE_ADJUST = 2.0f;
    private static final double BOOST_SCALE_MAX_ADDITION = 0.5;

    private static float lastRotation = -1000;

    private ImageView myArrow;


    public JamTrendArrowImpl() {
        this(null);
    }

    public JamTrendArrowImpl(ImageView v) {
        init(v);
        myArrow.setImageResource(R.drawable.ic_gtk_go_forward_ltr);
        update(null);
    }


    public void init(ImageView view) {
        if (view == null) {
            myArrow = new ImageView(xdrip.getAppContext());
        } else {
            myArrow = view;
        }
    }

    @Override
    public View get() {
        return myArrow;
    }

    @Override
    public boolean update(final Double mgdl) {

        if (mgdl != null) {

            final float newRotation = calculateRotation(mgdl);

            // not initialized
            if (lastRotation == -1000) {
                lastRotation = newRotation;
            }

            final RotateAnimation rotateAnimation = new RotateAnimation(
                    lastRotation,
                    newRotation,
                    Animation.RELATIVE_TO_SELF,
                    0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);
            rotateAnimation.setDuration(1000);
            rotateAnimation.setInterpolator(new DecelerateInterpolator(1.5f));
            rotateAnimation.setFillAfter(true);

            lastRotation = newRotation;

            myArrow.startAnimation(rotateAnimation);
            final float newScale = calculateScale(mgdl);
            myArrow.setScaleX(newScale * SOURCE_SCALE_ADJUST);
            myArrow.setScaleY(newScale * SOURCE_SCALE_ADJUST);
            myArrow.setVisibility(View.VISIBLE);

            return false;
        } else {
            myArrow.setVisibility(View.GONE);
            myArrow.setScaleX(0.0001f);
            myArrow.setScaleY(0.0001f);
        }
        return true;
    }


    private static float calculateRotation(double mgdl_by_minute) {
        double degrees = mgdl_by_minute * -45;
        if (degrees < -90) degrees = -90;
        if (degrees > 90) degrees = 90;
        return (float) degrees;
    }

    // when above LARGE_CHANGE then boost scale up to MAX_CHANGE on scale based by BOOST_SCALE_MAX_ADDITION
    static float calculateScale(double mgdl_by_minute) {
        final double abs = Math.abs(mgdl_by_minute);
        if (abs >= LARGE_CHANGE) {
            return (float) ((1 - (MAX_CHANGE - Math.min(MAX_CHANGE, abs)) / (MAX_CHANGE - LARGE_CHANGE)) * BOOST_SCALE_MAX_ADDITION + 1);
        } else {
            return 1;
        }
    }

}
