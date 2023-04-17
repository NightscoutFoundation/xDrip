package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.xdrip;
import com.rarepebble.colorpicker.ColorPreference;

/**
 * Created by jamorham on 12/03/2016.
 */
public class ColorPicker extends ColorPreference {

    private final Integer DEFAULT_VALUE = 4567;
    private final String TAG = "jamorham colorpicker";
    private final boolean debug = false;
    private Integer mCurrentValue = DEFAULT_VALUE;
    private Integer localDefaultValue = DEFAULT_VALUE;
    private ViewGroup mParent;

    public ColorPicker(Context context) {
        this(context, null);
    }

    public ColorPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (attrs != null) {
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                if (attrs.getAttributeName(i).equals("defaultValue")) {
                    final String this_value = attrs.getAttributeValue(i);

                    if (this_value.length() > 0) {
                        if (debug)
                            Log.d(TAG, "Attribute debug: " + i + " " + attrs.getAttributeName(i) + " " + this_value);
                        if (this_value.startsWith("@")) {
                            localDefaultValue = Color.parseColor(getStringResourceByName(this_value));
                        } else {
                            localDefaultValue = Color.parseColor(this_value);
                        }
                        localDefaultValue = Color.parseColor(attrs.getAttributeValue(i));
                        super.defaultColor = localDefaultValue;
                    } else {
                        Log.w(TAG, "No default value for colorpicker");
                    }
                    break;
                }

            }
        }
    }


    private String getStringResourceByName(String aString) {
        String packageName = xdrip.getAppContext().getPackageName();
        final int resId = xdrip.getAppContext().getResources().getIdentifier(aString, "string", packageName);
        return xdrip.getAppContext().getResources().getString(resId);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        ColorCache.invalidateCache();

        // TODO maybe a better way to send this message to the chart
        if (mParent != null) {
            Log.d(TAG, "Calling refresh on parent");
            try {
                mParent.refreshDrawableState();
            } catch (Exception e) {
                Log.e(TAG, "Got exception refreshing drawablestate: " + e);
            }
        } else {
            Log.d(TAG, "mparent is null :(");
        }
        notifyChanged();

        // TODO probably should check whether data has actually changed before updating all the graphics
        Home.staticRefreshBGCharts(true);
        WidgetUpdateService.staticRefreshWidgets();
        Notifications.staticUpdateNotification();
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        mParent = parent;
        return super.onCreateView(parent);
    }

    @Override
    public void setColor(Integer color) {
        if (color == null) {
            // don't nuke the setting
            color = localDefaultValue;
        }
        super.setColor(color);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        //Log.d(TAG, "default value: " + defaultValue.toString());
        if (restorePersistedValue) {
            // Restore existing state
            try {
                mCurrentValue = this.getPersistedInt(DEFAULT_VALUE);
            } catch (ClassCastException e) {
                Log.e(TAG, "Cannot cast - invalid preference type saved? Should be Int!! ", e);
            }
        } else {
            // Set default state from the XML attribute
            mCurrentValue = (Integer) defaultValue;
            persistInt(mCurrentValue);
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        if (debug) Log.d(TAG, "onGetDefault Value called: " + a.getString(index));

        localDefaultValue = Color.parseColor(a.getString(index));
        super.defaultColor = localDefaultValue;

        super.setColor(localDefaultValue);
        return localDefaultValue;
    }
}
