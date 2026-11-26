package com.eveningoutpost.dexdrip.utils.settings;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceScreen;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Tests {@link EditTextAttributes}: the text attributes written on each {@code EditTextPreference} in the settings XML
 * reach the androidx text field, as they reached the old one, which was built from the same XML element.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class EditTextAttributesTest extends RobolectricTestWithConfig {

    /** The attributes on the XML elements that belong to the setting rather than to its text field. */
    private static final Set<String> PREFERENCE_ATTRIBUTES = new TreeSet<>(Arrays.asList("key", "title",
            "defaultValue", "summary", "dependency", "dialogMessage", "dialogTitle", "shouldDisableView", "icon"));

    /** The text field attributes the settings XML uses today, each checked by the tests below. */
    private static final Set<String> TEXT_FIELD_ATTRIBUTES = new TreeSet<>(Arrays.asList("inputType", "numeric",
            "maxLines", "singleLine", "maxLength", "digits", "editable", "textIsSelectable", "max"));

    // ===== Matching the XML ==========================================================================================

    /**
     * Every text field in the settings XML ends up with the input type, length limit, accepted characters, line limit,
     * selectability, transformation and font that a field built from its XML element gets.
     */
    @Test
    public void everyFieldMatchesAFieldBuiltFromItsXml() {
        // :: Setup
        final Context context = RuntimeEnvironment.getApplication();
        final Map<String, String> expected = new LinkedHashMap<>();
        SettingsXml.forEach("EditTextPreference",
                (key, attrs) -> expected.put(key, EditTextProbe.describe(new EditText(context, attrs))));

        // :: Act
        final Map<String, EditTextAttributes> parsed = EditTextAttributes.parse(context, SettingsXml.SCREEN_XMLS);

        // :: Verify
        assertThat(parsed.keySet()).containsExactlyElementsIn(expected.keySet());
        for (final Map.Entry<String, String> field : expected.entrySet()) {
            final EditText edit = new EditText(context);
            parsed.get(field.getKey()).applyTo(edit);
            assertWithMessage(field.getKey()).that(EditTextProbe.describe(edit)).isEqualTo(field.getValue());
        }
    }

    /**
     * The settings XML uses no text field attribute beyond the ones known here. A new one may change something the
     * comparison above does not look at, so it has to be added here, and looked at, first.
     */
    @Test
    public void xmlUsesOnlyKnownTextFieldAttributes() {
        // :: Setup
        final Set<String> used = new TreeSet<>();

        // :: Act
        SettingsXml.forEach("EditTextPreference", (key, attrs) -> used.addAll(attributeNames(attrs)));
        used.removeAll(PREFERENCE_ATTRIBUTES);

        // :: Verify
        assertThat(used).isEqualTo(TEXT_FIELD_ATTRIBUTES);
    }

    /**
     * A stored value longer than the field's length limit is cut to the limit when the field is shown, as the old
     * field did, with the cursor at the end.
     */
    @Test
    public void storedTextLongerThanMaxLengthIsCut() {
        // :: Setup
        final Context context = RuntimeEnvironment.getApplication();
        final EditTextAttributes threeDigits = EditTextAttributes.parse(context, SettingsXml.SCREEN_XMLS)
                .get("opennov_prime_units");
        final EditText edit = new EditText(context);
        edit.setText("123456");

        // :: Act
        threeDigits.applyTo(edit);

        // :: Verify
        assertThat(edit.getText().toString()).isEqualTo("123");
        assertThat(edit.getSelectionStart()).isEqualTo(3);
        assertThat(edit.getSelectionEnd()).isEqualTo(3);
    }

    // ===== Binding to the dialog =====================================================================================

    /**
     * When a text field on a nested screen opens, its XML attributes are on the dialog's field before the extra
     * listener for that key runs, so the extra can rely on them.
     */
    @Test
    public void extraListenerRunsAfterAttributes() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final Context context = host.fragment.requireContext();
        final Map<String, EditTextAttributes> attrs = EditTextAttributes.parse(context, SettingsXml.SCREEN_XMLS);
        final PreferenceScreen root = host.screen("root", "Root");
        final PreferenceScreen sub = host.screen("sub", "Sub");
        root.addPreference(sub);
        final EditTextPreference field = textField(context, sub, "opennov_prime_units");
        host.setRoot(root);
        final List<String> seenByExtra = new ArrayList<>();
        final Map<String, EditTextPreference.OnBindEditTextListener> extra = new HashMap<>();
        extra.put("opennov_prime_units", edit -> seenByExtra.add(EditTextProbe.describe(edit)));
        final String expected = describeApplied(context, attrs.get("opennov_prime_units"));

        // :: Act
        EditTextAttributes.bindAll(root, attrs, extra);
        final EditText shown = openDialog(host, field);

        // :: Verify
        assertThat(seenByExtra).containsExactly(expected);
        assertThat(EditTextProbe.describe(shown)).isEqualTo(expected);
    }

    /**
     * A text field with no extra listener still gets its XML attributes when its dialog opens.
     */
    @Test
    public void fieldWithoutExtraGetsAttributes() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final Context context = host.fragment.requireContext();
        final Map<String, EditTextAttributes> attrs = EditTextAttributes.parse(context, SettingsXml.SCREEN_XMLS);
        final PreferenceScreen root = host.screen("root", "Root");
        final EditTextPreference field = textField(context, root, "dex_txid");
        host.setRoot(root);

        // :: Act
        EditTextAttributes.bindAll(root, attrs, Collections.emptyMap());
        final EditText shown = openDialog(host, field);

        // :: Verify
        assertThat(EditTextProbe.describe(shown)).isEqualTo(describeApplied(context, attrs.get("dex_txid")));
    }

    // ===== Helpers ===================================================================================================

    private static EditTextPreference textField(final Context context, final PreferenceScreen parent,
                                                final String key) {
        final EditTextPreference field = new EditTextPreference(context);
        field.setKey(key);
        field.setTitle(key);
        field.setPersistent(false);
        field.setText("1");
        parent.addPreference(field);
        return field;
    }

    private static EditText openDialog(final PreferenceTestHost host, final EditTextPreference field) {
        host.fragment.onDisplayPreferenceDialog(field);
        host.idle();
        final DialogFragment dialogFragment = (DialogFragment) host.fragment.requireFragmentManager()
                .findFragmentByTag("androidx.preference.PreferenceFragment.DIALOG");
        final Dialog dialog = dialogFragment.getDialog();
        return dialog.findViewById(android.R.id.edit);
    }

    private static String describeApplied(final Context context, final EditTextAttributes attributes) {
        final EditText edit = new EditText(context);
        attributes.applyTo(edit);
        return EditTextProbe.describe(edit);
    }

    private static List<String> attributeNames(final AttributeSet attrs) {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < attrs.getAttributeCount(); i++) {
            names.add(attrs.getAttributeName(i));
        }
        return names;
    }
}
