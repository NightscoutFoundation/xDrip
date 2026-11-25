package com.eveningoutpost.dexdrip.utils.settings;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;
import androidx.recyclerview.widget.RecyclerView;

import com.bytehamster.lib.preferencesearch.SearchPreferenceFragment;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.Preferences;

import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.fail;
import static org.robolectric.Shadows.shadowOf;

/**
 * Drives the settings screen the way a person does: open it, tap a sub-screen, go back, tap a switch, open a field.
 * <p>
 * The pinning tests use only this class, never a preference API, so the same tests run unchanged before and after
 * the move to androidx. Only this class knows which preference API is underneath.
 * <p>
 * Every method fails the test with a message naming the key, instead of returning null.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class SettingsScreenDriver {

    /** androidx's tag for a setting's dialog. */
    private static final String DIALOG_TAG = "androidx.preference.PreferenceFragment.DIALOG";

    private final ActivityController<Preferences> controller;
    private Intent lastStarted;

    /** The text last typed into search, typed again while the search index is still being built. */
    private String lastQuery;

    private SettingsScreenDriver(final ActivityController<Preferences> controller) {
        this.controller = controller;
    }

    // ===== Opening ===================================================================================================

    /**
     * Launches the settings screen. A non-null {@code action} is the deep link to that sub-screen.
     */
    public static SettingsScreenDriver open(@Nullable final String action) {
        mirrorIntoPref();
        final Intent intent = new Intent(org.robolectric.RuntimeEnvironment.getApplication(), Preferences.class);
        if (action != null) {
            intent.setAction(action);
        }
        final SettingsScreenDriver driver =
                new SettingsScreenDriver(Robolectric.buildActivity(Preferences.class, intent).setup());
        driver.idle();
        return driver;
    }

    /**
     * Copies the default preference store into the one {@link Pref} holds, so that both hold the test's settings.
     * <p>
     * The settings code reads some settings, such as engineering mode and the data source, through {@link Pref}.
     * {@link Pref} keeps the first store it is given in a static for the rest of the test run, and in a full run that
     * store belongs to an earlier test. Without the copy the screen would show that test's state.
     */
    private static void mirrorIntoPref() {
        final SharedPreferences store = PreferenceManager
                .getDefaultSharedPreferences(org.robolectric.RuntimeEnvironment.getApplication());
        final SharedPreferences cached = Pref.getInstance();
        if (cached == null || cached == store) {
            return;
        }
        final SharedPreferences.Editor editor = cached.edit().clear();
        for (final Map.Entry<String, ?> entry : store.getAll().entrySet()) {
            final Object value = entry.getValue();
            if (value instanceof Boolean) {
                editor.putBoolean(entry.getKey(), (Boolean) value);
            } else if (value instanceof Integer) {
                editor.putInt(entry.getKey(), (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(entry.getKey(), (Long) value);
            } else if (value instanceof Float) {
                editor.putFloat(entry.getKey(), (Float) value);
            } else if (value instanceof Set) {
                @SuppressWarnings("unchecked") final Set<String> strings = (Set<String>) value;
                editor.putStringSet(entry.getKey(), strings);
            } else {
                editor.putString(entry.getKey(), (String) value);
            }
        }
        editor.commit();
    }

    /**
     * Recreates the activity, as a rotation does.
     */
    public void recreate() {
        controller.recreate();
        idle();
    }

    /**
     * Starts the open settings screen again with {@code action}, as another screen does when it brings settings to
     * the front.
     */
    public void deliverNewIntent(final String action) {
        controller.newIntent(new Intent(org.robolectric.RuntimeEnvironment.getApplication(), Preferences.class)
                .setAction(action));
        idle();
    }

    // ===== Where am I ================================================================================================

    /**
     * The key of the sub-screen on top, or null when the root is shown.
     */
    @Nullable
    public String shownScreenKey() {
        final PreferenceScreen shown = shownScreen();
        return shown == root() ? null : shown.getKey();
    }

    /**
     * The title of the screen on top.
     */
    public String title() {
        final PreferenceScreen shown = shownScreen();
        if (shown == root()) {
            return String.valueOf(activity().getTitle());
        }
        return String.valueOf(shown.getTitle());
    }

    /**
     * The title the activity shows in its action bar.
     */
    public String activityTitle() {
        return String.valueOf(activity().getTitle());
    }

    /**
     * The title the setting {@code key} has now, wherever it is in the tree.
     */
    public String titleOf(final String key) {
        return String.valueOf(find(key).getTitle());
    }

    /**
     * The values the list setting {@code key} offers, in the order it offers them.
     */
    public List<String> entryValuesOf(final String key) {
        final Preference pref = find(key);
        if (!(pref instanceof ListPreference)) {
            fail(key + " is not a list but " + pref.getClass().getSimpleName());
        }
        final List<String> values = new ArrayList<>();
        for (final CharSequence value : ((ListPreference) pref).getEntryValues()) {
            values.add(value.toString());
        }
        return values;
    }

    /**
     * True when {@code key} is on the screen on top (not inside one of its sub-screens).
     */
    public boolean isShown(final String key) {
        return ownerScreen(find(key)) == shownScreen();
    }

    /**
     * True when {@code key} exists anywhere in the live settings tree.
     */
    public boolean exists(final String key) {
        return root().findPreference(key) != null;
    }

    public boolean isEnabled(final String key) {
        return find(key).isEnabled();
    }

    // ===== Navigation ================================================================================================

    /**
     * Taps the sub-screen {@code key}, which must be on the screen on top.
     */
    public void openScreen(final String key) {
        final Preference pref = find(key);
        if (!(pref instanceof PreferenceScreen)) {
            fail(key + " is not a sub-screen but " + pref.getClass().getSimpleName());
        }
        tap(key);
    }

    /**
     * Goes back to the root, then opens each sub-screen on the path to {@code key}, so that {@code key} is shown.
     */
    public void showScreenOf(final String key) {
        final List<PreferenceScreen> path = new ArrayList<>();
        for (PreferenceScreen screen = ownerScreen(find(key)); screen != root(); screen = ownerScreen(screen)) {
            path.add(0, screen);
        }
        while (shownScreen() != root()) {
            back();
        }
        for (final PreferenceScreen screen : path) {
            tap(screen); // by object: a nested screen may reuse its parent's key
        }
        if (!isShown(key)) {
            fail("could not reach the screen of " + key + "; shown is " + shownScreenKey());
        }
    }

    /**
     * Goes back one screen, as the back button does.
     */
    public void back() {
        final PreferenceScreen shown = shownScreen();
        if (shown == root()) {
            fail("back pressed on the root screen");
        }
        activity().onBackPressed();
        idle();
    }

    /**
     * Presses back on whatever is shown, the root included, as the back button does.
     */
    public void pressBack() {
        activity().onBackPressed();
        idle();
    }

    /**
     * Whether the settings screen has closed, as it does when back is pressed on the root.
     */
    public boolean isClosed() {
        return activity().isFinishing();
    }

    /**
     * Whether a dialog of a setting is shown on the settings screen.
     */
    public boolean hasOpenDialog() {
        for (final Fragment fragment : activity().getSupportFragmentManager().getFragments()) {
            if (fragment instanceof DialogFragment) {
                final Dialog dialog = ((DialogFragment) fragment).getDialog();
                if (dialog != null && dialog.isShowing()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ===== Interaction ===============================================================================================

    /**
     * Taps {@code key}, which must be on the screen on top.
     */
    public void click(final String key) {
        tap(key);
    }

    /**
     * Sets the switch or checkbox {@code key} by tapping it when its state differs, so the change listener runs.
     */
    public void setChecked(final String key, final boolean on) {
        final Preference pref = find(key);
        if (!(pref instanceof TwoStatePreference)) {
            fail(key + " is not a switch or checkbox but " + pref.getClass().getSimpleName());
        }
        if (((TwoStatePreference) pref).isChecked() != on) {
            tap(key);
        }
        if (((TwoStatePreference) pref).isChecked() != on) {
            fail(key + " did not change to " + on);
        }
    }

    /**
     * Opens the edit dialog of the text field {@code key}, from wherever it is, and returns the field in it. A field
     * that is disabled by its dependency gets that switch tapped first, as a person would.
     */
    public EditText editTextOf(final String key) {
        final Preference pref = find(key);
        if (!(pref instanceof EditTextPreference)) {
            fail(key + " is not a text field but " + pref.getClass().getSimpleName());
        }
        enable(key, 5);
        showScreenOf(key);
        tap(key);
        final Fragment opened = activity().getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        final Dialog dialog = opened instanceof DialogFragment ? ((DialogFragment) opened).getDialog() : null;
        if (dialog == null || !dialog.isShowing()) {
            fail("no edit dialog opened for " + key);
        }
        final View field = dialog.findViewById(android.R.id.edit);
        if (!(field instanceof EditText)) {
            fail("edit dialog of " + key + " has no text field");
        }
        final EditText editText = (EditText) field;
        ((DialogFragment) opened).dismiss();
        idle();
        return editText;
    }

    /**
     * Opens the dialog of the setting {@code key}, from wherever it is, and returns it while it is shown. A setting
     * that is disabled by its dependency gets that switch tapped first, as a person would.
     */
    public Dialog dialogOf(final String key) {
        enable(key, 5);
        showScreenOf(key);
        tap(key);
        final Fragment opened = activity().getSupportFragmentManager().findFragmentByTag(DIALOG_TAG);
        final Dialog dialog = opened instanceof DialogFragment ? ((DialogFragment) opened).getDialog() : null;
        if (dialog == null || !dialog.isShowing()) {
            fail("no dialog opened for " + key);
        }
        return dialog;
    }

    /**
     * Presses {@code button} ({@code DialogInterface.BUTTON_*}) of a dialog from {@link #dialogOf}.
     */
    public void press(final Dialog dialog, final int button) {
        if (!(dialog instanceof androidx.appcompat.app.AlertDialog)) {
            fail("not a button dialog: " + dialog.getClass().getSimpleName());
        }
        ((androidx.appcompat.app.AlertDialog) dialog).getButton(button).performClick();
        idle();
    }

    /**
     * Taps the switch {@code key} depends on, and that switch's own dependency first, until {@code key} is enabled.
     */
    private void enable(final String key, final int depth) {
        final Preference pref = find(key);
        if (pref.isEnabled()) {
            return;
        }
        final String dependency = pref.getDependency();
        if (dependency == null || depth == 0 || !(find(dependency) instanceof TwoStatePreference)) {
            fail(key + " is disabled and has no switch to enable it");
        }
        enable(dependency, depth - 1);
        showScreenOf(dependency);
        tap(dependency);
        if (!pref.isEnabled()) {
            fail(key + " stayed disabled after tapping " + dependency);
        }
    }

    /**
     * The intent the last tap started for a result. Fails when none was started.
     */
    public Intent nextStartedActivityForResult() {
        final ShadowActivity.IntentForResult started = shadowOf(activity()).getNextStartedActivityForResult();
        if (started == null) {
            fail("no activity was started for a result");
        }
        lastStarted = started.intent;
        return started.intent;
    }

    /**
     * The intent the last tap started without a result. Fails when none was started.
     */
    public Intent nextStartedActivity() {
        final Intent started = shadowOf(activity()).getNextStartedActivity();
        if (started == null) {
            fail("no activity was started");
        }
        return started;
    }

    /**
     * Delivers a result to the activity that {@link #nextStartedActivityForResult} returned.
     */
    public void deliverResult(final int resultCode, @Nullable final Intent data) {
        if (lastStarted == null) {
            fail("deliverResult called before nextStartedActivityForResult");
        }
        shadowOf(activity()).receiveResult(lastStarted, resultCode, data);
        idle();
    }

    // ===== Search ====================================================================================================

    /**
     * Opens the search page from the toolbar and types {@code query}.
     */
    public void search(final String query) {
        activity().showSearch(null);
        idle();
        lastQuery = query;
        searchFragment().setSearchTerm(query);
        idle();
    }

    /**
     * Taps the one search result that shows {@code text} as its title or its summary, as a person picks a result by
     * what it says. Fails unless exactly one result matches.
     * <p>
     * The search index is built in the background. The search page waits at most five seconds for it and then shows
     * no results until the text changes, so while nothing matches this types the search text again, as a person
     * would.
     */
    public void clickSearchResult(final String text) {
        final long deadline = JoH.tsl() + 30_000;
        while (true) {
            final RecyclerView list = findView(searchFragment().getView(), RecyclerView.class);
            layOutAll(list);
            final List<View> matches = new ArrayList<>();
            for (int i = 0; i < list.getChildCount(); i++) {
                final View item = list.getChildAt(i);
                for (final TextView line : texts(item)) {
                    if (text.contentEquals(line.getText())) {
                        matches.add(item);
                        break;
                    }
                }
            }
            if (matches.size() == 1) {
                matches.get(0).performClick();
                idle();
                return;
            }
            if (matches.size() > 1) {
                fail(matches.size() + " search results show " + text);
            }
            if (JoH.tsl() > deadline) {
                final List<String> shown = new ArrayList<>();
                for (int i = 0; i < list.getChildCount(); i++) {
                    for (final TextView line : texts(list.getChildAt(i))) {
                        shown.add(String.valueOf(line.getText()));
                    }
                }
                fail("no search result shows " + text + "; the " + list.getChildCount() + " rows show " + shown);
            }
            sleep();
            searchFragment().setSearchTerm(lastQuery);
            idle();
        }
    }

    /**
     * Every title and summary the search page shows, once it shows any. Types the search text again while the index is
     * still being built, as {@link #clickSearchResult} does, and fails when nothing shows within thirty seconds.
     */
    public List<String> searchResultTexts() {
        final long deadline = JoH.tsl() + 30_000;
        while (true) {
            final RecyclerView list = findView(searchFragment().getView(), RecyclerView.class);
            layOutAll(list);
            final List<String> shown = new ArrayList<>();
            for (int i = 0; i < list.getChildCount(); i++) {
                for (final TextView line : texts(list.getChildAt(i))) {
                    shown.add(String.valueOf(line.getText()));
                }
            }
            if (!shown.isEmpty()) {
                return shown;
            }
            if (JoH.tsl() > deadline) {
                fail("the search page shows no results for " + lastQuery);
            }
            sleep();
            searchFragment().setSearchTerm(lastQuery);
            idle();
        }
    }

    // ===== Internals =================================================================================================

    private Preferences activity() {
        return controller.get();
    }

    private PreferenceScreen root() {
        return settingsFragment().getPreferenceScreen();
    }

    /**
     * The settings fragment; the search page may sit on top of it in the same container.
     */
    private PreferenceFragmentCompat settingsFragment() {
        final List<Fragment> found = new ArrayList<>();
        for (final Fragment fragment : activity().getSupportFragmentManager().getFragments()) {
            if (fragment instanceof PreferenceFragmentCompat && fragment.isAdded()) {
                found.add(fragment);
            }
        }
        if (found.size() != 1) {
            fail("expected one settings fragment, found " + found);
        }
        return (PreferenceFragmentCompat) found.get(0);
    }

    private Preference find(final String key) {
        final Preference pref = root().findPreference(key);
        if (pref == null) {
            fail("no setting with key " + key);
        }
        return pref;
    }

    /**
     * The screen whose settings the list shows: the one that lists the first row.
     */
    private PreferenceScreen shownScreen() {
        final PreferenceGroupAdapter adapter = adapter();
        if (adapter.getItemCount() == 0) {
            fail("the settings list is empty");
        }
        return ownerScreen(adapter.getItem(0));
    }

    private PreferenceGroupAdapter adapter() {
        final RecyclerView.Adapter<?> adapter = settingsFragment().getListView().getAdapter();
        if (!(adapter instanceof PreferenceGroupAdapter)) {
            fail("the settings list has no preference adapter but " + adapter);
        }
        return (PreferenceGroupAdapter) adapter;
    }

    private static List<PreferenceScreen> childScreens(final PreferenceGroup group) {
        final List<PreferenceScreen> out = new ArrayList<>();
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference child = group.getPreference(i);
            if (child instanceof PreferenceScreen) {
                out.add((PreferenceScreen) child);
            } else if (child instanceof PreferenceGroup) {
                out.addAll(childScreens((PreferenceGroup) child));
            }
        }
        return out;
    }

    /**
     * The screen that lists {@code pref} directly, through categories but not through sub-screens.
     */
    private PreferenceScreen ownerScreen(final Preference pref) {
        final PreferenceScreen owner = ownerIn(root(), pref);
        if (owner == null) {
            fail("setting " + pref.getKey() + " is not in the live tree");
        }
        return owner;
    }

    @Nullable
    private static PreferenceScreen ownerIn(final PreferenceScreen screen, final Preference pref) {
        if (listsDirectly(screen, pref)) {
            return screen;
        }
        for (final PreferenceScreen child : childScreens(screen)) {
            final PreferenceScreen found = ownerIn(child, pref);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static boolean listsDirectly(final PreferenceGroup group, final Preference pref) {
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            final Preference child = group.getPreference(i);
            if (child == pref) {
                return true;
            }
            if (!(child instanceof PreferenceScreen) && child instanceof PreferenceGroup
                    && listsDirectly((PreferenceGroup) child, pref)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Taps {@code key} through the list of the screen on top, so enabled state and click listeners apply as for a
     * finger.
     */
    private void tap(final String key) {
        tap(find(key));
    }

    private void tap(final Preference pref) {
        rowOf(pref).performClick();
        idle();
    }

    /**
     * The row the list shows for {@code key} on the screen on top, as it is drawn now.
     */
    public View rowOf(final String key) {
        return rowOf(find(key));
    }

    private View rowOf(final Preference pref) {
        final String key = pref.getKey();
        if (ownerScreen(pref) != shownScreen()) {
            fail(key + " is not on the shown screen " + shownScreenKey());
        }
        final RecyclerView list = settingsFragment().getListView();
        final int pos = adapter().getPreferenceAdapterPosition(pref);
        if (pos < 0) {
            fail(key + " is not in the list of the shown screen " + shownScreenKey());
        }
        layOutAll(list);
        final RecyclerView.ViewHolder row = list.findViewHolderForAdapterPosition(pos);
        if (row == null) {
            fail(key + " has no row in the list of the shown screen " + shownScreenKey());
        }
        return row.itemView;
    }

    /**
     * Lays a list out tall enough that every row is attached, so its click handler sees a valid position, as on a
     * phone after scrolling to it.
     */
    private static void layOutAll(final RecyclerView list) {
        final int width = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY);
        final int height = View.MeasureSpec.makeMeasureSpec(100_000, View.MeasureSpec.EXACTLY);
        list.measure(width, height);
        list.layout(0, 0, 1080, 100_000);
    }

    private SearchPreferenceFragment searchFragment() {
        for (final Fragment fragment : activity().getSupportFragmentManager().getFragments()) {
            if (fragment instanceof SearchPreferenceFragment) {
                return (SearchPreferenceFragment) fragment;
            }
        }
        fail("search page is not open");
        return null;
    }

    @Nullable
    private static <T extends View> T findView(final View view, final Class<T> type) {
        if (type.isInstance(view)) {
            return type.cast(view);
        }
        if (view instanceof ViewGroup) {
            final ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                final T found = findView(group.getChildAt(i), type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static List<TextView> texts(final View view) {
        final List<TextView> out = new ArrayList<>();
        if (view instanceof TextView) {
            out.add((TextView) view);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                out.addAll(texts(((ViewGroup) view).getChildAt(i)));
            }
        }
        return out;
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void sleep() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        idle();
    }
}
