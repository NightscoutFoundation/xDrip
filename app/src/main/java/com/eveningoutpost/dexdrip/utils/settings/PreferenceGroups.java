package com.eveningoutpost.dexdrip.utils.settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

/**
 * Adds a setting to a group without crashing when the setting already sits in another group.
 * <p>
 * The code that shows and hides settings sometimes adds a setting to a group that is not its own. The old
 * {@code android.preference} screen accepted that and showed the setting in both groups until the screen was rebuilt;
 * androidx throws instead. Here such a setting stays only in its own group.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class PreferenceGroups {

    private PreferenceGroups() {
    }

    /**
     * Adds {@code preference} to {@code group}, unless it already sits in another group, where it then stays.
     */
    public static void add(final PreferenceGroup group, final Preference preference) {
        final PreferenceGroup parent = preference.getParent();
        if (parent != null && parent != group) {
            return;
        }
        group.addPreference(preference);
    }
}
