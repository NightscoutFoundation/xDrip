package com.eveningoutpost.dexdrip.utils.settings;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceScreen;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests {@link SubScreenNavigator}: it opens a nested settings screen in place, while the whole tree stays attached, so
 * dependencies and lookups work as they did when each sub-screen was a dialog over the same tree.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SubScreenNavigatorTest extends RobolectricTestWithConfig {

    // ===== Dependencies and lookups ==================================================================================

    /**
     * While a sub-screen is shown, its rows follow the settings they depend on, both a setting on the same sub-screen
     * and one on the root.
     */
    @Test
    public void showKeepsDependencyLive() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = tree(host);
        final SubScreenNavigator navigator = navigator(host);
        navigator.show(sub(root));

        // :: Act
        final boolean innerDepBefore = host.isRowEnabled("inner_dep");
        final boolean slaveBefore = host.isRowEnabled("slave");
        ((CheckBoxPreference) root.findPreference("inner")).setChecked(false);
        ((CheckBoxPreference) root.findPreference("master")).setChecked(false);

        // :: Verify
        assertThat(innerDepBefore).isTrue();
        assertThat(slaveBefore).isTrue();
        assertThat(host.isRowEnabled("inner_dep")).isFalse();
        assertThat(host.isRowEnabled("slave")).isFalse();
    }

    /**
     * Showing a sub-screen leaves the page's tree as it was, so a lookup by key still finds settings on every screen.
     */
    @Test
    public void showKeepsFindPreferenceGlobal() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = tree(host);
        final SubScreenNavigator navigator = navigator(host);

        // :: Act
        navigator.show(sub(root));

        // :: Verify
        assertThat(host.fragment.getPreferenceScreen()).isSameInstanceAs(root);
        assertThat((Object) host.fragment.findPreference("master")).isNotNull();
        assertThat((Object) host.fragment.findPreference("deep_item")).isNotNull();
        assertThat(host.shownTitles()).containsExactly("inner", "inner_dep", "slave", "Deep").inOrder();
    }

    // ===== Back ======================================================================================================

    /**
     * Back from a sub-screen two levels down shows its parent, then the root, and then reports that there is nothing
     * left to go back to.
     */
    @Test
    public void backPopsToParentThenRoot() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = tree(host);
        final SubScreenNavigator navigator = navigator(host);
        navigator.show(sub(root));
        navigator.show((PreferenceScreen) root.findPreference("deep"));

        // :: Act
        final boolean first = navigator.back();
        final String keyAfterFirst = navigator.shownKey();
        final List<String> rowsAfterFirst = host.shownTitles();
        final boolean second = navigator.back();
        final String keyAfterSecond = navigator.shownKey();
        final List<String> rowsAfterSecond = host.shownTitles();
        final boolean third = navigator.back();

        // :: Verify
        assertThat(first).isTrue();
        assertThat(keyAfterFirst).isEqualTo("sub");
        assertThat(rowsAfterFirst).containsExactly("inner", "inner_dep", "slave", "Deep").inOrder();
        assertThat(second).isTrue();
        assertThat(keyAfterSecond).isNull();
        assertThat(rowsAfterSecond).containsExactly("master", "Sub").inOrder();
        assertThat(third).isFalse();
    }

    /**
     * Back returns to the row the list was scrolled to before the sub-screen opened, and puts back the title.
     */
    @Test
    public void backRestoresScrollAndTitle() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = host.screen("root", "Root");
        for (int i = 0; i < 40; i++) {
            host.plain(root, "row_" + i);
        }
        final PreferenceScreen sub = host.screen("sub", "Sub");
        root.addPreference(sub);
        host.plain(sub, "sub_item");
        host.setRoot(root);
        final SubScreenNavigator navigator = navigator(host);
        host.fragment.getListView().scrollToPosition(40);
        final String topRowBefore = host.shownTitles().get(0);

        // :: Act
        navigator.show(sub);
        final String titleInSub = host.activityTitle();
        navigator.back();

        // :: Verify
        assertThat(topRowBefore).isNotEqualTo("row_0");
        assertThat(titleInSub).isEqualTo("Sub");
        assertThat(host.shownTitles().get(0)).isEqualTo(topRowBefore);
        assertThat(host.activityTitle()).isEqualTo(PreferenceTestHost.ROOT_TITLE);
    }

    // ===== Recreation ================================================================================================

    /**
     * Leaving a page two screens deep gives the activity back the root's title and leaves nothing to go back to.
     */
    @Test
    public void leaveRestoresRootTitle() {
        // :: Setup
        final PreferenceTestHost host = PreferenceTestHost.start();
        final PreferenceScreen root = host.screen("root", "Root");
        final PreferenceScreen sub = host.screen("sub", "Sub");
        root.addPreference(sub);
        final PreferenceScreen inner = host.screen("inner", "Inner");
        sub.addPreference(inner);
        host.plain(inner, "inner_item");
        host.setRoot(root);
        final SubScreenNavigator navigator = navigator(host);
        navigator.show(sub);
        navigator.show(inner);

        // :: Act
        navigator.leave();

        // :: Verify
        assertThat(host.activityTitle()).isEqualTo(PreferenceTestHost.ROOT_TITLE);
        assertThat(navigator.shownKey()).isNull();
        assertThat(navigator.back()).isFalse();
    }

    /**
     * The saved path of a sub-screen two levels down reopens it on a new page, and back from there lands on the level
     * above it.
     */
    @Test
    public void restoreReopensWholePath() {
        // :: Setup
        final PreferenceTestHost before = PreferenceTestHost.start();
        final PreferenceScreen oldRoot = tree(before);
        final SubScreenNavigator oldNavigator = navigator(before);
        oldNavigator.show(sub(oldRoot));
        oldNavigator.show((PreferenceScreen) oldRoot.findPreference("deep"));
        final List<String> saved = oldNavigator.keyStack();
        final PreferenceTestHost after = PreferenceTestHost.start();
        tree(after);
        final SubScreenNavigator navigator = navigator(after);

        // :: Act
        navigator.restore(saved);
        final String restoredKey = navigator.shownKey();
        final List<String> restoredRows = after.shownTitles();
        final String restoredTitle = after.activityTitle();
        navigator.back();

        // :: Verify
        assertThat(saved).isEqualTo(Arrays.asList("sub", "deep"));
        assertThat(restoredKey).isEqualTo("deep");
        assertThat(restoredRows).containsExactly("deep_item");
        assertThat(restoredTitle).isEqualTo("Deep");
        assertThat(navigator.shownKey()).isEqualTo("sub");
        assertThat(after.activityTitle()).isEqualTo("Sub");
    }

    // ===== Helpers ===================================================================================================

    /**
     * Root: {@code master} and sub-screen {@code sub}. {@code sub}: {@code inner}, {@code inner_dep} (depends on
     * {@code inner}), {@code slave} (depends on {@code master}) and sub-screen {@code deep} with {@code deep_item}.
     */
    private static PreferenceScreen tree(final PreferenceTestHost host) {
        final PreferenceScreen root = host.screen("root", "Root");
        host.checkBox(root, "master", true);
        final PreferenceScreen sub = host.screen("sub", "Sub");
        root.addPreference(sub);
        host.checkBox(sub, "inner", true);
        host.plain(sub, "inner_dep");
        host.plain(sub, "slave");
        final PreferenceScreen deep = host.screen("deep", "Deep");
        sub.addPreference(deep);
        host.plain(deep, "deep_item");
        host.setRoot(root);
        root.findPreference("inner_dep").setDependency("inner");
        root.findPreference("slave").setDependency("master");
        return root;
    }

    private static PreferenceScreen sub(final PreferenceScreen root) {
        return root.findPreference("sub");
    }

    private static SubScreenNavigator navigator(final PreferenceTestHost host) {
        return new SubScreenNavigator(host.fragment, host.fragment::adapterFor);
    }
}
