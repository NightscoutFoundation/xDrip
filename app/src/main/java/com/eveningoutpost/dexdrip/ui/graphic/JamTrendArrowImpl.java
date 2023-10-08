package com.eveningoutpost.dexdrip.ui.graphic;

// jamorham

import android.graphics.ColorFilter;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.ui.helpers.ColorUtil.hueFilter;
import static com.eveningoutpost.dexdrip.ui.helpers.UiHelper.convertDpToPixel;

public class JamTrendArrowImpl extends TrendArrowBase {

    private static final String PREF_JAM_TREND_HUE = "jamTrendArrowHue";

    private static final double LARGE_CHANGE = 2d; // some devices use 3.0
    private static final double MAX_CHANGE = 3.5d; // some devices use 3.0
    private static final float SOURCE_SCALE_ADJUST = 2.0f;
    private static final double BOOST_SCALE_MAX_ADDITION = 0.5;


    private LinearLayout configuratorLayout;
    private ColorFilter colorfilter;
    private int hue = 0;


    JamTrendArrowImpl(ImageView v) {
        super(v);
        setBoostScaleMaxAddition(BOOST_SCALE_MAX_ADDITION);
        setLargeChange(LARGE_CHANGE);
        setMaxChange(MAX_CHANGE);
        setSourceScaleAdjust(SOURCE_SCALE_ADJUST);
        setImage(R.drawable.ic_gtk_go_forward_ltr);
        loadHue();
        updateColorFilter();
    }


    @Override
    public boolean update(final Double mgdl) {
        final ImageView myArrow = getMyArrow();
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
            myArrow.setScaleX(newScale * getSourceScaleAdjust());
            myArrow.setScaleY(newScale * getSourceScaleAdjust());
            myArrow.setVisibility(View.VISIBLE);

            myArrow.setColorFilter(colorfilter);

            return false;
        } else {
            myArrow.setVisibility(View.GONE);
            myArrow.setScaleX(0.0001f);
            myArrow.setScaleY(0.0001f);
        }
        return true;
    }

    // seekbar to change hue
    @Override
    public synchronized View getConfigurator() {
        if (configuratorLayout == null) {
            configuratorLayout = new LinearLayout(xdrip.getAppContext());
            final SeekBar seekBar = new SeekBar(xdrip.getAppContext());
            seekBar.setProgress((int) ((hue + 180f) / 3.6f));
            SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    hue = (int) (-180f + (progress * 3.6f));
                    updateColorFilter();
                    getMyArrow().setColorFilter(colorfilter);
                    saveHue();
                }
            };
            seekBar.setOnSeekBarChangeListener(seekBarChangeListener);

            LinearLayout.LayoutParams seekBarLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            final int marginPx = convertDpToPixel(10);
            seekBarLayoutParams.setMargins(marginPx, marginPx, marginPx, marginPx);
            configuratorLayout.addView(seekBar, seekBarLayoutParams);
        }
        return configuratorLayout;
    }

    private void loadHue() {
        hue = Pref.getStringToInt(PREF_JAM_TREND_HUE, 0);
    }

    private void saveHue() {
        Pref.setString(PREF_JAM_TREND_HUE, "" + hue);
    }

    private void updateColorFilter() {
        colorfilter = hueFilter(hue);
    }

}
