package com.eveningoutpost.dexdrip.utils.settings;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.TextUtils;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.preference.Preference;

/**
 * A sound setting that opens the system sound picker, for androidx, which has no ringtone setting of its own.
 * <p>
 * It reads {@code android:ringtoneType}, {@code android:showDefault} and {@code android:showSilent} with the same
 * defaults as the old {@code android.preference.RingtonePreference}, starts the picker with the same extras, and
 * stores the result the same way: the picked sound's uri as a string, or an empty string for silence. Its XML default
 * is stored when the setting is first added, as before. The settings page
 * starts the picker from {@link #buildPickerIntent} and hands the result to {@link #onPickerResult}.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class RingtonePickerPreference extends Preference {

    /** Hidden {@code RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS}, which the old setting set. */
    private static final String EXTRA_AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";
    /** Hidden {@code AudioAttributes.FLAG_BYPASS_INTERRUPTION_POLICY}: the preview plays through Do Not Disturb. */
    private static final int FLAG_BYPASS_INTERRUPTION_POLICY = 0x40;
    /** Sorted by attribute id, as {@link Context#obtainStyledAttributes} needs. */
    private static final int[] ATTRS = {android.R.attr.ringtoneType, android.R.attr.showDefault,
            android.R.attr.showSilent};

    private final int ringtoneType;
    private final boolean showDefault;
    private final boolean showSilent;

    public RingtonePickerPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        final TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        try {
            ringtoneType = a.getInt(0, RingtoneManager.TYPE_RINGTONE);
            showDefault = a.getBoolean(1, true);
            showSilent = a.getBoolean(2, true);
        } finally {
            a.recycle();
        }
    }

    /**
     * The intent that opens the system sound picker on the stored sound.
     */
    public Intent buildPickerIntent() {
        final String stored = getPersistedString(null);
        final Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                TextUtils.isEmpty(stored) ? null : Uri.parse(stored));
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, showDefault);
        if (showDefault) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(ringtoneType));
        }
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, showSilent);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getTitle());
        intent.putExtra(EXTRA_AUDIO_ATTRIBUTES_FLAGS, FLAG_BYPASS_INTERRUPTION_POLICY);
        return intent;
    }

    /**
     * Stores what the picker returned. No data means the picker was cancelled, and nothing changes.
     */
    public void onPickerResult(@Nullable final Intent data) {
        if (data == null) {
            return;
        }
        final Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
        final String value = uri == null ? "" : uri.toString();
        if (callChangeListener(value)) {
            persistString(value);
        }
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    /**
     * Stores the XML default the first time, as the old setting did, parsed as a uri like a picked sound.
     */
    @Override
    protected void onSetInitialValue(final boolean restorePersistedValue, @Nullable final Object defaultValue) {
        if (restorePersistedValue) {
            return;
        }
        final String value = (String) defaultValue;
        if (!TextUtils.isEmpty(value)) {
            persistString(Uri.parse(value).toString());
        }
    }
}
