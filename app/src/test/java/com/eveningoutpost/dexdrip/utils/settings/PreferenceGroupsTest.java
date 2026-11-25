package com.eveningoutpost.dexdrip.utils.settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link PreferenceGroups}: adding a setting to a group works as on the old screen, and a setting that already
 * sits in another group stays there instead of crashing the screen.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class PreferenceGroupsTest extends RobolectricTestWithConfig {

    /**
     * A setting that sits in no group is added to the group.
     */
    @Test
    public void addsFreeSetting() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = host.screen("root", "Root");
        final PreferenceScreen sub = host.screen("sub", "Sub");
        root.addPreference(sub);
        final Preference plain = host.plain(sub, "plain");
        sub.removePreference(plain);

        // :: Act
        PreferenceGroups.add(sub, plain);

        // :: Verify
        assertThat(plain.getParent()).isSameInstanceAs(sub);
        assertThat(sub.getPreferenceCount()).isEqualTo(1);
    }

    /**
     * Adding a setting to the group it already sits in leaves it there once.
     */
    @Test
    public void addToOwnGroupKeepsOneCopy() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = host.screen("root", "Root");
        final Preference plain = host.plain(root, "plain");

        // :: Act
        PreferenceGroups.add(root, plain);

        // :: Verify
        assertThat(plain.getParent()).isSameInstanceAs(root);
        assertThat(root.getPreferenceCount()).isEqualTo(1);
    }

    /**
     * A setting that sits in another group stays there, and the group it was added to is unchanged.
     */
    @Test
    public void settingInAnotherGroupStaysThere() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = host.screen("root", "Root");
        final PreferenceScreen first = host.screen("first", "First");
        final PreferenceScreen second = host.screen("second", "Second");
        root.addPreference(first);
        root.addPreference(second);
        final Preference plain = host.plain(first, "plain");

        // :: Act
        PreferenceGroups.add(second, plain);

        // :: Verify
        assertThat(plain.getParent()).isSameInstanceAs(first);
        assertThat(second.getPreferenceCount()).isEqualTo(0);
    }
}
