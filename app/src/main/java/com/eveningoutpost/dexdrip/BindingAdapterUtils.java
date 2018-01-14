
package com.eveningoutpost.dexdrip;

import android.databinding.BindingAdapter;
import android.support.annotation.NonNull;
import android.view.View;


/**
 * Created by jamorham on 11/12/2017.
 */


public class BindingAdapterUtils {

    @BindingAdapter(value = {"showIfTrue"}, requireAll = true)
    public static void setShowIfTrue(@NonNull View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

}

