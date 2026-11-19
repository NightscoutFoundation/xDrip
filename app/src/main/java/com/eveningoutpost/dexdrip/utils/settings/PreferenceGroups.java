package com.eveningoutpost.dexdrip.utils.settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

/**
 * Adds a setting to a group the way the old {@code android.preference} screen did for the code that shows and hides
 * settings by moving them in and out of groups.
 * <p>
 * The old screen accepted a setting that already sat in another group; androidx throws instead. The settings code
 * adds settings back to groups they were never removed from, often to a group that is not their own, so here such a
 * setting stays where it is.
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
