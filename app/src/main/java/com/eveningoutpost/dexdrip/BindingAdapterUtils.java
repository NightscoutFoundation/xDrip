package com.eveningoutpost.dexdrip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import androidx.databinding.BindingAdapter;
import android.graphics.Typeface;
import androidx.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;

import static com.eveningoutpost.dexdrip.utilitymodels.Constants.FINAL_VISIBILITY_ID;


/**
 * Created by jamorham on 11/12/2017.
 */


public class BindingAdapterUtils {

    private static volatile long endTime;

    @BindingAdapter(value = {"showIfTrueAnimated"}, requireAll = true)
    public static void setShowIfTrueAnimated(@NonNull View view, boolean isVisible) {
        setVisibility(view, isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrueAnimated"}, requireAll = true)
    public static void setShowIfTrueAnimatedBoolean(@NonNull View view, Boolean isVisible) {
        setShowIfTrueAnimated(view, isVisible != null ? isVisible : false);
    }


    @BindingAdapter(value = {"showIfTrueAnimatedFastOff"}, requireAll = true)
    public static void setShowIfTrueAnimatedFastOff(@NonNull View view, boolean isVisible) {
        setVisibility(view, isVisible ? View.VISIBLE : View.GONE, 50, 600);
    }

    @BindingAdapter(value = {"showIfTrueAnimatedFastOff"}, requireAll = true)
    public static void setShowIfTrueAnimatedBooleanFastOff(@NonNull View view, Boolean isVisible) {
        setVisibility(view, isVisible ? View.VISIBLE : View.GONE, 50, 600);
    }


    @BindingAdapter(value = {"showIfTrue"}, requireAll = true)
    public static void setShowIfTrue(@NonNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter(value = {"showIfTrue"}, requireAll = true)
    public static void setShowIfTrue(@NonNull View view, Boolean isVisible) {
        if (isVisible == null) isVisible = false;
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @BindingAdapter("boldIfTrue")
    public static void boldIfTrue(TextView view, boolean value) {
        if (value) {
            view.setTypeface(null, Typeface.BOLD);
        } else {
            view.setTypeface(null, Typeface.NORMAL);
        }
    }

    @BindingAdapter("boldIfTrue")
    public static void boldIfTrueButton(CompoundButton view, boolean value) {
        if (value) {
            view.setTypeface(null, Typeface.BOLD);
        } else {
            view.setTypeface(null, Typeface.NORMAL);
        }
    }

    @BindingAdapter("boldIfTrue")
    public static void boldIfTrueButtonBoolean(CompoundButton view, Boolean value) {
        boldIfTrue(view, value != null ? value : false);
    }

    @BindingAdapter(value = {"showIfTrueInRecycler"}, requireAll = true)
    public static void setShowIfTrueInRecycler(@NonNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        view.getLayoutParams().height = isVisible ? LinearLayout.LayoutParams.WRAP_CONTENT : 0;
    }

    @BindingAdapter(value = {"invisibleIfFalse"}, requireAll = true)
    public static void setInvisibleIfFalse(@NonNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    @BindingAdapter(value = {"src"})
    public static void setImageViewResource(ImageView imageView, int resource) {
        if (resource != 0) imageView.setImageResource(resource);
    }

    @BindingAdapter(value = {"button_indicate"})
    public static void buttonIndicate(Button button, boolean set) {
        button.setAlpha(set ? 0.5f : 1.0f);
    }


    @BindingAdapter(value = {"animatedVisibility"})
    public static synchronized void setVisibility(@NonNull final View view,
                                                  final int visibility) {
        setVisibility(view, visibility, 600, 600);
    }

    public static synchronized void setVisibility(@NonNull final View view,
                                                  final int visibility, int offSpeed, int onSpeed) {

        // Were we animating before? If so, what was the visibility?
        Integer endAnimVisibility = (Integer) view.getTag(FINAL_VISIBILITY_ID);
        int oldVisibility = endAnimVisibility == null
                ? view.getVisibility()
                : endAnimVisibility;

        if (oldVisibility == visibility) {
            // just let it finish any current animation.
            return;
        }

        boolean isVisibile = oldVisibility == View.VISIBLE;
        boolean willBeVisible = visibility == View.VISIBLE;

        float startAlpha = isVisibile ? 1f : 0f;
        if (endAnimVisibility != null) {
            startAlpha = view.getAlpha();
        }
        float endAlpha = willBeVisible ? 1f : 0f;

        view.setAlpha(startAlpha);
        view.setVisibility(View.VISIBLE);

        // Create the animator
        final ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, startAlpha, endAlpha);

        final long duration = willBeVisible ? onSpeed : offSpeed;
        final long stagger = duration / 4;
        alpha.setDuration(duration);

        // Stagger animations keyed at the same moment
        long now = JoH.tsl();
        if (now <= endTime) {
            alpha.setStartDelay(JoH.msTill(endTime));
            endTime += stagger;
        } else {
            endTime = now + stagger;
        }
        alpha.setAutoCancel(true);

        alpha.addListener(new AnimatorListenerAdapter() {
            private boolean isCanceled;

            @Override
            public void onAnimationStart(Animator anim) {
                view.setTag(FINAL_VISIBILITY_ID, visibility);
            }

            @Override
            public void onAnimationCancel(Animator anim) {
                isCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator anim) {
                view.setTag(FINAL_VISIBILITY_ID, null);
                if (!isCanceled) {
                    view.setAlpha(1f);
                    view.setVisibility(visibility);
                }
            }
        });
        alpha.start();
    }
}

