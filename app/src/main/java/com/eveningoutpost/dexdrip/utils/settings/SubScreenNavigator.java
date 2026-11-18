package com.eveningoutpost.dexdrip.utils.settings;

import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/**
 * Opens a nested settings screen in place, on the same page, and goes back from it.
 * <p>
 * The old settings screen opened each nested screen as a dialog over the same tree. androidx would instead make the
 * nested screen the page's whole tree, which detaches the rest: dependencies on a setting elsewhere stop working, and
 * lookups by key stop finding settings on other screens. This class never replaces the tree. It only gives the list an
 * adapter for the nested screen.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SubScreenNavigator {

    private final PreferenceFragmentCompat host;
    private final Function<PreferenceScreen, RecyclerView.Adapter<?>> adapterFactory;
    /** The screens shown on top of the root, the last one opened first. */
    private final Deque<Level> stack = new ArrayDeque<>();
    private CharSequence rootTitle;

    /**
     * @param host           the settings page whose list shows the screens
     * @param adapterFactory builds a list adapter for a screen, normally the page's own {@code onCreateAdapter}
     */
    public SubScreenNavigator(final PreferenceFragmentCompat host,
                              final Function<PreferenceScreen, RecyclerView.Adapter<?>> adapterFactory) {
        this.host = host;
        this.adapterFactory = adapterFactory;
    }

    /**
     * Shows {@code screen} in the page's list and its title in the activity. Call only once the list exists.
     */
    public void show(final PreferenceScreen screen) {
        final RecyclerView list = host.getListView();
        if (stack.isEmpty()) {
            rootTitle = host.requireActivity().getTitle();
        }
        stack.push(new Level(screen, list.getLayoutManager().onSaveInstanceState()));
        list.setAdapter(adapterFactory.apply(screen));
        host.requireActivity().setTitle(screen.getTitle());
    }

    /**
     * Goes back to the screen the shown one was opened from, scrolled as it was.
     *
     * @return false if the root is shown, so there is nothing to go back to
     */
    public boolean back() {
        if (stack.isEmpty()) {
            return false;
        }
        final Level left = stack.pop();
        final PreferenceScreen parent = stack.isEmpty() ? host.getPreferenceScreen() : stack.peek().screen;
        final RecyclerView list = host.getListView();
        // A new adapter, because the settings on the parent now report their changes to the adapter that was left.
        list.setAdapter(adapterFactory.apply(parent));
        list.getLayoutManager().onRestoreInstanceState(left.parentScroll);
        host.requireActivity().setTitle(stack.isEmpty() ? rootTitle : parent.getTitle());
        return true;
    }

    /**
     * Forgets the open screens and gives the activity back the root's title, for a page about to be replaced.
     */
    public void leave() {
        if (stack.isEmpty()) {
            return;
        }
        stack.clear();
        host.requireActivity().setTitle(rootTitle);
    }

    /**
     * The key of the screen shown, or null when the root is shown.
     */
    @Nullable
    public String shownKey() {
        return stack.isEmpty() ? null : stack.peek().screen.getKey();
    }

    /**
     * The keys of the screens open on top of the root, outermost first, so {@link #restore} can reopen them.
     */
    public ArrayList<String> keyStack() {
        final ArrayList<String> keys = new ArrayList<>();
        for (final Iterator<Level> it = stack.descendingIterator(); it.hasNext(); ) {
            keys.add(it.next().screen.getKey());
        }
        return keys;
    }

    /**
     * Reopens the screens saved by {@link #keyStack}, outermost first, on a page that shows its root. Stops at the
     * first key that is no longer a screen in the tree.
     */
    public void restore(final List<String> keys) {
        for (final String key : keys) {
            final Preference found = host.findPreference(key);
            if (!(found instanceof PreferenceScreen)) {
                return;
            }
            show((PreferenceScreen) found);
        }
    }

    /**
     * A shown screen, and how its parent was scrolled when it opened.
     */
    private static final class Level {
        final PreferenceScreen screen;
        @Nullable
        final Parcelable parentScroll;

        Level(final PreferenceScreen screen, @Nullable final Parcelable parentScroll) {
            this.screen = screen;
            this.parentScroll = parentScroll;
        }
    }
}
