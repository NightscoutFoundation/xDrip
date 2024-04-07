package com.eveningoutpost.dexdrip.adapters;

import androidx.databinding.BindingAdapter;
import androidx.databinding.InverseBindingAdapter;
import androidx.databinding.InverseBindingListener;
import androidx.databinding.InverseBindingMethod;
import androidx.databinding.InverseBindingMethods;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;


@InverseBindingMethods({
        @InverseBindingMethod(type = CompoundButton.class, attribute = "checked", method = "getChecked"),
})
public class CheckBoxBindingAdapterUtils {

    @BindingAdapter(value = {"checked"})
    public static void setChecked(CompoundButton view, Boolean checked) {
        if (checked == null) checked = false;
        setChecked(view, (boolean) checked);
    }

    @BindingAdapter(value = {"checked"})
    public static void setChecked(CompoundButton view, boolean checked) {
        if (view.isChecked() != checked) {
            view.setChecked(checked);
        }
    }

    @InverseBindingAdapter(attribute = "checked")
    public static boolean getChecked(CompoundButton button) {
        return button.isChecked();
    }

    @BindingAdapter(value = {"onCheckedChanged", "checkedAttrChanged"},
            requireAll = false)
    public static void setListeners(CompoundButton view, final OnCheckedChangeListener listener,
                                    final InverseBindingListener attrChange) {
        if (attrChange == null) {
            view.setOnCheckedChangeListener(listener);
        } else {
            view.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (listener != null) {
                        listener.onCheckedChanged(buttonView, isChecked);
                    }
                    attrChange.onChange();
                }
            });
        }
    }

}
