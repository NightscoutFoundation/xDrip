package com.eveningoutpost.dexdrip.utils.settings;

import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView;

import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;

import java.util.ArrayList;
import java.util.List;

import static org.robolectric.Shadows.shadowOf;

/**
 * A bare androidx settings page in an {@link AppCompatActivity}, so the settings building blocks can be tested on their
 * own, before the real settings screen uses them.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
final class PreferenceTestHost {

    /** Title the activity starts with, so a test can see it come back. */
    static final String ROOT_TITLE = "Root title";

    final ActivityController<HostActivity> controller;
    final HostFragment fragment;

    private PreferenceTestHost(final ActivityController<HostActivity> controller, final HostFragment fragment) {
        this.controller = controller;
        this.fragment = fragment;
    }

    /**
     * Starts a visible activity that holds an empty settings page.
     */
    static PreferenceTestHost start() {
        final ActivityController<HostActivity> controller = Robolectric.buildActivity(HostActivity.class).setup();
        final HostFragment fragment = new HostFragment();
        controller.get().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, fragment).commitNow();
        final PreferenceTestHost host = new PreferenceTestHost(controller, fragment);
        host.idle();
        return host;
    }

    // ===== Building a tree ===========================================================================================

    PreferenceScreen screen(final String key, final String title) {
        final PreferenceScreen screen = fragment.getPreferenceManager().createPreferenceScreen(fragment.requireContext());
        screen.setKey(key);
        screen.setTitle(title);
        return screen;
    }

    CheckBoxPreference checkBox(final PreferenceGroup parent, final String key, final boolean checked) {
        final CheckBoxPreference box = new CheckBoxPreference(fragment.requireContext());
        box.setKey(key);
        box.setTitle(key);
        box.setPersistent(false);
        box.setChecked(checked);
        parent.addPreference(box);
        return box;
    }

    Preference plain(final PreferenceGroup parent, final String key) {
        final Preference preference = new Preference(fragment.requireContext());
        preference.setKey(key);
        preference.setTitle(key);
        preference.setPersistent(false);
        parent.addPreference(preference);
        return preference;
    }

    /**
     * Makes {@code root} the page's tree and lays it out. androidx looks a dependency up as soon as it is set in code,
     * so set dependencies after this.
     */
    void setRoot(final PreferenceScreen root) {
        fragment.setPreferenceScreen(root);
        idle();
    }

    // ===== Reading the list ==========================================================================================

    /**
     * The titles of the rows on screen, top to bottom.
     */
    List<String> shownTitles() {
        idle();
        final List<String> titles = new ArrayList<>();
        final RecyclerView list = fragment.getListView();
        for (int i = 0; i < list.getChildCount(); i++) {
            titles.add(String.valueOf(titleOf(list.getChildAt(i)).getText()));
        }
        return titles;
    }

    /**
     * Whether the row titled {@code title} is drawn enabled. Fails if no such row is on screen.
     */
    boolean isRowEnabled(final String title) {
        idle();
        final RecyclerView list = fragment.getListView();
        for (int i = 0; i < list.getChildCount(); i++) {
            final TextView view = titleOf(list.getChildAt(i));
            if (title.contentEquals(view.getText())) {
                return view.isEnabled();
            }
        }
        throw new AssertionError("no row titled " + title + " on screen; shown: " + shownTitles());
    }

    String activityTitle() {
        return String.valueOf(controller.get().getTitle());
    }

    void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static TextView titleOf(final View row) {
        return row.findViewById(android.R.id.title);
    }

    // ===== Host classes ==============================================================================================

    /**
     * An AppCompat activity with the title {@link #ROOT_TITLE}.
     */
    public static class HostActivity extends AppCompatActivity {
        @Override
        protected void onCreate(@Nullable final Bundle savedInstanceState) {
            setTheme(androidx.appcompat.R.style.Theme_AppCompat);
            super.onCreate(savedInstanceState);
            setTitle(ROOT_TITLE);
        }
    }

    /**
     * A settings page with no tree of its own, which hands out the adapter it would build for a screen.
     */
    public static class HostFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(@Nullable final Bundle savedInstanceState, @Nullable final String rootKey) {
        }

        public RecyclerView.Adapter<?> adapterFor(final PreferenceScreen screen) {
            return onCreateAdapter(screen);
        }
    }
}
