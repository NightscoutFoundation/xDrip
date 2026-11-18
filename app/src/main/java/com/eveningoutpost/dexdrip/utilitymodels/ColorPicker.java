package com.eveningoutpost.dexdrip.utilitymodels;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.DialogPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.WidgetUpdateService;
import com.eveningoutpost.dexdrip.utils.ExampleChartPreferenceView;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * Created by jamorham on 12/03/2016.
 * <p>
 * Was a {@code com.rarepebble.colorpicker.ColorPreference}, which only exists for the framework settings. That class's
 * behaviour is kept here unchanged: the {@code colorpicker_*} attributes, the colour thumbnail, the summary when no
 * colour is stored, and int storage. Its dialog is {@link ColorPickerDialogFragment}.
 */
public class ColorPicker extends DialogPreference {

    /** The colour the library falls back to when there is no default. */
    private static final int FALLBACK_COLOR = 0xFF888888;
    /** The example graph on the colour settings screen, drawn with these colours. */
    private static final String EXAMPLE_CHART_KEY = "example_chart";

    // From the library's ColorPreference.
    final String selectNoneButtonText;
    Integer defaultColor;
    private final String noneSelectedSummaryText;
    private final CharSequence summaryText;
    final boolean showAlpha;
    final boolean showHex;
    private View thumbnail;

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
        // From the library's ColorPreference.
        summaryText = super.getSummary();
        if (attrs != null) {
            final TypedArray a = context.getTheme()
                    .obtainStyledAttributes(attrs, com.rarepebble.colorpicker.R.styleable.ColorPicker, 0, 0);
            selectNoneButtonText = a.getString(
                    com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_selectNoneButtonText);
            noneSelectedSummaryText = a.getString(
                    com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_noneSelectedSummaryText);
            defaultColor = a.hasValue(com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_defaultColor)
                    ? a.getColor(com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_defaultColor,
                    FALLBACK_COLOR) : null;
            showAlpha = a.getBoolean(com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_showAlpha, true);
            showHex = a.getBoolean(com.rarepebble.colorpicker.R.styleable.ColorPicker_colorpicker_showHex, true);
        } else {
            selectNoneButtonText = null;
            defaultColor = null;
            noneSelectedSummaryText = null;
            showAlpha = true;
            showHex = true;
        }

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
                        defaultColor = localDefaultValue;
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

    /**
     * Called by the dialog however it closes, so the charts, widgets and notification take the new colour.
     */
    void onDialogClosed(boolean positiveResult) {
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
        // the old list redrew every row on a change; androidx redraws only this one, so ask the graph to redraw
        final Preference chart = getPreferenceManager() == null ? null
                : getPreferenceManager().findPreference(EXAMPLE_CHART_KEY);
        if (chart instanceof ExampleChartPreferenceView) {
            ((ExampleChartPreferenceView) chart).refresh();
        }

        // TODO probably should check whether data has actually changed before updating all the graphics
        Home.staticRefreshBGCharts(true);
        WidgetUpdateService.staticRefreshWidgets();
        Notifications.staticUpdateNotification();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        mParent = (ViewGroup) holder.itemView.getParent();
        // From the library's ColorPreference.
        thumbnail = addThumbnail(holder.itemView);
        showColor(getPersistedIntDefaultOrNull());
        super.onBindViewHolder(holder);
    }

    // ===== From the library's ColorPreference ===================================================================

    private View addThumbnail(View view) {
        final ViewGroup widgetFrameView = view.findViewById(android.R.id.widget_frame);
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.removeAllViews();
        LayoutInflater.from(getContext()).inflate(isEnabled()
                ? com.rarepebble.colorpicker.R.layout.color_preference_thumbnail
                : com.rarepebble.colorpicker.R.layout.color_preference_thumbnail_disabled, widgetFrameView);
        return widgetFrameView.findViewById(com.rarepebble.colorpicker.R.id.thumbnail);
    }

    private Integer getPersistedIntDefaultOrNull() {
        return getSharedPreferences().contains(getKey())
                ? Integer.valueOf(getPersistedInt(FALLBACK_COLOR))
                : defaultColor;
    }

    private void showColor(Integer color) {
        final Integer thumbColor = color == null ? defaultColor : color;
        if (thumbnail != null) {
            thumbnail.setVisibility(thumbColor == null ? View.GONE : View.VISIBLE);
            thumbnail.findViewById(com.rarepebble.colorpicker.R.id.colorPreview)
                    .setBackgroundColor(thumbColor == null ? 0 : thumbColor);
        }
        if (noneSelectedSummaryText != null) {
            setSummary(thumbColor == null ? noneSelectedSummaryText : summaryText);
        }
    }

    /**
     * The colour the dialog opens on: the stored one, else the default.
     */
    int getPickerStartColor() {
        return getPersistedInt(defaultColor == null ? FALLBACK_COLOR : defaultColor);
    }

    /**
     * Takes a colour from the dialog, or null for "revert to default", if the change listener accepts it.
     */
    void onColorChosen(Integer color) {
        if (callChangeListener(color)) {
            setColor(color);
        }
    }

    /**
     * The library's ColorPreference.setColor. Its null branch, which removed the setting, is left out: {@link #setColor}
     * turns null into the default colour first, so it never ran.
     */
    private void setColorAndShow(int color) {
        persistInt(color);
        showColor(color);
    }

    public Integer getColor() {
        return getPersistedIntDefaultOrNull();
    }

    // ============================================================================================================

    public void setColor(Integer color) {
        if (color == null) {
            // don't nuke the setting
            color = localDefaultValue;
        }
        setColorAndShow(color);
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
        defaultColor = localDefaultValue;

        setColorAndShow(localDefaultValue);
        return localDefaultValue;
    }
}
