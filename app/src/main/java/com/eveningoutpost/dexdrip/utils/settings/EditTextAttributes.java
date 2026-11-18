package com.eveningoutpost.dexdrip.utils.settings;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.method.KeyListener;
import android.text.method.TransformationMethod;
import android.util.AttributeSet;
import android.util.Xml;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.annotation.XmlRes;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The text field attributes written on an {@code EditTextPreference} in the settings XML, such as
 * {@code android:inputType}, {@code android:digits} and {@code android:maxLength}.
 * <p>
 * The old {@code EditTextPreference} built its text field from the XML element, so these attributes reached the field.
 * The androidx one builds its field from a fixed layout and ignores them. This class builds a field from the element,
 * the way the old one did, keeps what that field ended up with, and puts the same on the androidx field when its dialog
 * opens.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class EditTextAttributes {

    private static final String ANDROID = "http://schemas.android.com/apk/res/android";

    private final boolean singleLine;
    private final int inputType;
    @Nullable
    private final KeyListener keyListener;
    private final InputFilter[] filters;
    private final int maxLines;
    @Nullable
    private final TransformationMethod transformation;
    private final boolean textIsSelectable;
    private final Typeface typeface;

    private EditTextAttributes(final boolean singleLine, final EditText reference) {
        this.singleLine = singleLine;
        this.inputType = reference.getInputType();
        this.keyListener = reference.getKeyListener();
        this.filters = reference.getFilters();
        this.maxLines = reference.getMaxLines();
        this.transformation = reference.getTransformationMethod();
        this.textIsSelectable = reference.isTextSelectable();
        this.typeface = reference.getTypeface();
    }

    /**
     * Reads the text field attributes of every {@code EditTextPreference} in {@code xmls}, by key.
     */
    public static Map<String, EditTextAttributes> parse(final Context context, @XmlRes final int... xmls) {
        final Map<String, EditTextAttributes> byKey = new HashMap<>();
        for (final int xml : xmls) {
            try (XmlResourceParser parser = context.getResources().getXml(xml)) {
                for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                    if (event != XmlPullParser.START_TAG || !isEditTextPreference(parser.getName())) {
                        continue;
                    }
                    final String key = parser.getAttributeValue(ANDROID, "key");
                    if (key != null) {
                        byKey.put(key, from(context, Xml.asAttributeSet(parser)));
                    }
                }
            } catch (XmlPullParserException | IOException e) {
                throw new IllegalStateException("cannot read settings xml " + xml, e);
            }
        }
        return byKey;
    }

    /**
     * The attributes a text field built from {@code attrs} ends up with.
     */
    static EditTextAttributes from(final Context context, final AttributeSet attrs) {
        return new EditTextAttributes(attrs.getAttributeBooleanValue(ANDROID, "singleLine", false),
                new EditText(context, attrs));
    }

    /**
     * Puts these attributes on {@code edit}, and shortens its text to the length limit, as the old field did.
     */
    public void applyTo(final EditText edit) {
        if (singleLine) {
            edit.setSingleLine(true);
        }
        edit.setKeyListener(keyListener);
        // setKeyListener changes the input type to suit the listener, so the input type goes on after it.
        edit.setRawInputType(inputType);
        edit.setFilters(filters);
        edit.setMaxLines(maxLines);
        edit.setTransformationMethod(transformation);
        edit.setTextIsSelectable(textIsSelectable);
        edit.setTypeface(typeface);
        // The dialog sets the text before this runs. Setting it again passes it through the length filter.
        // setTransformationMethod does that too today, but this does not rest on it.
        edit.setText(edit.getText().toString());
        edit.setSelection(edit.length());
    }

    /**
     * Makes every text field under {@code root}, on nested screens too, put on its attributes when its dialog opens,
     * and then run its listener from {@code extra}, if it has one.
     */
    public static void bindAll(final PreferenceGroup root, final Map<String, EditTextAttributes> attrs,
                               final Map<String, EditTextPreference.OnBindEditTextListener> extra) {
        for (int i = 0; i < root.getPreferenceCount(); i++) {
            final Preference preference = root.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                bindAll((PreferenceGroup) preference, attrs, extra);
            } else if (preference instanceof EditTextPreference) {
                bind((EditTextPreference) preference, attrs.get(preference.getKey()), extra.get(preference.getKey()));
            }
        }
    }

    private static void bind(final EditTextPreference field, @Nullable final EditTextAttributes attributes,
                             @Nullable final EditTextPreference.OnBindEditTextListener extra) {
        if (attributes == null && extra == null) {
            return;
        }
        field.setOnBindEditTextListener(edit -> {
            if (attributes != null) {
                attributes.applyTo(edit);
            }
            if (extra != null) {
                extra.onBindEditText(edit);
            }
        });
    }

    private static boolean isEditTextPreference(final String tag) {
        return tag.equals("EditTextPreference") || tag.endsWith(".EditTextPreference");
    }
}
