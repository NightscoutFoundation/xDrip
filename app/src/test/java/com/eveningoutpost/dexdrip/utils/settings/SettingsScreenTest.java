package com.eveningoutpost.dexdrip.utils.settings;

import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AlertDialog;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.calibrations.PluggableCalibration;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.time.TimeRangeUtils;
import com.eveningoutpost.dexdrip.watch.miband.MiBand;
import com.eveningoutpost.dexdrip.xdrip;
import com.rarepebble.colorpicker.ColorPickerView;

import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.view.LineChartView;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins how the settings screen behaves for a person: deep links, sub-screens, back, dependencies, search and the
 * ringtone picker. Most tests were written against the old screen first and stayed green unchanged through the
 * move to androidx; the rest pin behaviour the old screen did not have or that only the new one needs.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SettingsScreenTest extends RobolectricTestWithConfig {

    private static final String SPEAK_SCREEN = "xdrip_speak_readings_settings";
    private static final String SPEAK_THRESHOLD = "speak_readings_change_threshold";
    private static final String SPEAK_SWITCH = "bg_to_speech";
    private static final String SPEECH_SPEED = "speech_speed";
    private static final String ALERT_SOUND = "other_alerts_sound";
    /** Hidden {@code RingtoneManager.EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS}. */
    private static final String API_ENABLE = "cloud_storage_api_enable";
    private static final String API_DOWNLOAD = "cloud_storage_api_download_enable";
    /** Matches the hidden test-mode serial number and settings that are still on the screen. */
    private static final String SEARCH_QUERY = "serial number";
    private static final String NIGHT_START = "miband_nightmode_start";
    private static final String MIBAND_MAC = "AA:BB:CC:DD:EE:FF";
    private static final String HIGH_VALUE = "highValue";
    private static final String LOW_VALUE = "lowValue";
    private static final String UNITS = "units";
    private static final String DATA_SOURCE = "dex_collection_method";
    private static final String BLUEJAY_TIMEOUT = "bluejay_screen_timeout";
    private static final String MIBAND_NIGHT_INTERVAL = "miband_nightmode_interval";
    private static final String NUMBER_WALL_START = "pick_numberwall_start";
    private static final String CALIBRATION_PLUGIN = "current_calibration_plugin";
    private static final String HIGH_COLOUR = "color_high_values";
    private static final String EXAMPLE_CHART = "example_chart";
    private static final String AUDIO_ATTRIBUTES_FLAGS = "android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        android.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application)
                .edit().clear().commit();
        xdrip.loadDefaultPreferences(RuntimeEnvironment.application);
    }

    // ===== Navigation ================================================================================================

    /**
     * Opening settings without an action shows the root screen.
     */
    @Test
    public void opensRoot() {
        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.shownScreenKey()).isNull();
    }

    /**
     * The deep link of the speak-readings action opens that sub-screen with its own settings on it.
     */
    @Test
    public void deepLinkOpensSubScreen() {
        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Verify
        assertThat(driver.shownScreenKey()).isEqualTo(SPEAK_SCREEN);
        assertThat(driver.isShown(SPEAK_THRESHOLD)).isTrue();
    }

    /**
     * Back from a deep-linked sub-screen returns to the root.
     */
    @Test
    public void backReturnsToRoot() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        driver.back();

        // :: Verify
        assertThat(driver.shownScreenKey()).isNull();
    }

    /**
     * Settings brought to the front again from another screen, while a sub-screen is open, shows the root with the
     * root's title.
     */
    @Test
    public void newIntentOnSubScreenShowsRootTitle() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final String rootTitle = driver.activityTitle();
        driver.openScreen(SPEAK_SCREEN);

        // :: Act
        driver.deliverNewIntent(SPEAK_SCREEN);

        // :: Verify
        assertThat(driver.shownScreenKey()).isNull();
        assertThat(driver.activityTitle()).isEqualTo(rootTitle);
    }

    /**
     * Every keyed sub-screen of the settings XMLs that is in the live tree opens when tapped. Screens that only start
     * an activity (an {@code <intent>} child, or no children and a click listener in code, like the QR scanner) are
     * not sub-screens a person navigates into, so they are left out here. A disabled sub-screen (its dependency is off)
     * stays shut.
     */
    @Test
    public void everyScreenKeyOpens() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final List<String> opened = new ArrayList<>();

        // :: Act
        final Map<String, String> intentScreens = SettingsXml.intentScreens();
        final List<String> actionScreens = SettingsXml.childlessScreens();
        for (final String key : SettingsXml.keysOf("PreferenceScreen")) {
            if (!driver.exists(key) || intentScreens.containsKey(key) || actionScreens.contains(key)) {
                continue;
            }
            driver.showScreenOf(key);
            final String before = driver.shownScreenKey();
            final boolean enabled = driver.isEnabled(key);
            driver.openScreen(key);
            assertWithMessage("sub-screen " + key + " (enabled " + enabled + ")").that(driver.shownScreenKey())
                    .isEqualTo(enabled ? key : before);
            if (enabled) {
                opened.add(key);
            }
        }

        // :: Verify
        assertThat(opened.size()).isAtLeast(50);
    }

    /**
     * A sub-screen that holds an intent starts that activity when tapped, and stays on the screen it was tapped on.
     */
    @Test
    public void intentScreensStartTheirActivity() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final List<String> started = new ArrayList<>();

        // :: Act
        for (final Map.Entry<String, String> screen : SettingsXml.intentScreens().entrySet()) {
            final String key = screen.getKey();
            if (!driver.exists(key)) {
                continue;
            }
            driver.showScreenOf(key);
            final String before = driver.shownScreenKey();
            driver.click(key);
            final Intent intent = driver.nextStartedActivity();
            assertWithMessage("activity of " + key).that(intent.getComponent().getClassName())
                    .isEqualTo(screen.getValue());
            assertWithMessage("screen after " + key).that(driver.shownScreenKey()).isEqualTo(before);
            started.add(key);
        }

        // :: Verify
        assertThat(started).isNotEmpty();
    }

    /**
     * While a sub-screen is shown, the settings of every other screen stay in the live tree, and a switch on one
     * screen still enables a setting on another: the cloud upload switch enables treatment download one level deeper.
     */
    @Test
    public void subScreenKeepsWholeTreeLive() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        final boolean otherScreenLive = driver.exists(ALERT_SOUND) && driver.exists(API_DOWNLOAD);
        driver.showScreenOf(API_ENABLE);
        driver.setChecked(API_ENABLE, false);
        final boolean enabledWhenOff = driver.isEnabled(API_DOWNLOAD);
        driver.setChecked(API_ENABLE, true);
        final boolean enabledWhenOn = driver.isEnabled(API_DOWNLOAD);

        // :: Verify
        assertThat(otherScreenLive).isTrue();
        assertThat(enabledWhenOff).isFalse();
        assertThat(enabledWhenOn).isTrue();
    }

    /**
     * Opening every sub-screen and going back again changes no stored setting.
     */
    @Test
    public void storedValuesSurviveOpeningEveryScreen() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Map<String, ?> before = new HashMap<>(store().getAll());
        final Map<String, String> intentScreens = SettingsXml.intentScreens();
        final List<String> actionScreens = SettingsXml.childlessScreens();

        // :: Act
        for (final String key : SettingsXml.keysOf("PreferenceScreen")) {
            if (!driver.exists(key) || intentScreens.containsKey(key) || actionScreens.contains(key)
                    || !driver.isEnabled(key)) {
                continue;
            }
            driver.showScreenOf(key);
            driver.openScreen(key);
            driver.back();
        }

        // :: Verify
        assertThat(store().getAll()).isEqualTo(before);
    }

    /**
     * A slider value stored below the {@code android:min} in the XML stays as stored through an app start and
     * through showing its screen. The old screen ignored {@code android:min}, so 0 was a value a person could set.
     */
    @Test
    public void sliderValueBelowXmlMinIsKept() {
        // :: Setup
        final Map<String, Integer> belowMin = new HashMap<>();
        belowMin.put("blueReader_turn_off_value", 0);
        belowMin.put("speech_speed", 0);
        belowMin.put("speech_pitch", 1);
        final SharedPreferences.Editor editor = store().edit();
        for (final Map.Entry<String, Integer> entry : belowMin.entrySet()) {
            editor.putInt(entry.getKey(), entry.getValue());
        }
        editor.commit();

        // :: Act
        xdrip.loadDefaultPreferences(RuntimeEnvironment.application);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        for (final String key : belowMin.keySet()) {
            driver.showScreenOf(key);
        }

        // :: Verify
        for (final Map.Entry<String, Integer> entry : belowMin.entrySet()) {
            assertWithMessage(entry.getKey()).that(store().getInt(entry.getKey(), -1)).isEqualTo(entry.getValue());
        }
    }

    /**
     * Recreating the screen two sub-screens deep, as a rotation does, shows the root again, as today's screen does, and
     * the screen keeps working: another sub-screen opens.
     */
    @Test
    public void recreateKeepsScreenUsable() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.openScreen("whole_alerts_screen");
        driver.openScreen("bg_alerts_screen");

        // :: Act
        driver.recreate();
        final String shownAfterRecreate = driver.shownScreenKey();
        driver.showScreenOf(SPEAK_THRESHOLD);

        // :: Verify
        assertThat(shownAfterRecreate).isNull();
        assertThat(driver.shownScreenKey()).isEqualTo(SPEAK_SCREEN);
    }

    /**
     * Recreating the screen while a setting's dialog is open closes the dialog, so nothing typed afterwards goes to
     * a screen that is gone.
     */
    @Test
    public void recreateClosesOpenDialog() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.dialogOf(HIGH_VALUE);

        // :: Act
        driver.recreate();

        // :: Verify
        assertThat(driver.hasOpenDialog()).isFalse();
    }

    /**
     * Bringing settings to the front again while a setting's dialog is open closes the dialog.
     */
    @Test
    public void newIntentClosesOpenDialog() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.dialogOf(HIGH_VALUE);

        // :: Act
        driver.deliverNewIntent(SPEAK_SCREEN);

        // :: Verify
        assertThat(driver.hasOpenDialog()).isFalse();
    }

    // ===== Dependencies ==============================================================================================

    /**
     * The speak threshold follows the speak switch on the same sub-screen: off disables it, on enables it.
     */
    @Test
    public void dependencyFollowsSwitchInSubScreen() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        driver.setChecked("bg_to_speech", false);
        final boolean enabledWhenOff = driver.isEnabled(SPEAK_THRESHOLD);
        driver.setChecked("bg_to_speech", true);
        final boolean enabledWhenOn = driver.isEnabled(SPEAK_THRESHOLD);

        // :: Verify
        assertThat(enabledWhenOff).isFalse();
        assertThat(enabledWhenOn).isTrue();
    }

    // ===== Search ====================================================================================================

    /**
     * A search result for a setting in a sub-screen opens that sub-screen. The speak threshold has no title, so it is
     * found and picked by its summary. Search shows at most ten results and a broad word such as "speak" matches more
     * than ten, so the search uses the summary itself.
     */
    @Test
    public void searchOpensSubScreen() {
        // :: Setup
        final String summary = RuntimeEnvironment.getApplication().getString(R.string.title_When_changes_by);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.search(summary);

        // :: Act
        driver.clickSearchResult(summary);

        // :: Verify
        assertThat(driver.shownScreenKey()).isEqualTo(SPEAK_SCREEN);
    }

    /**
     * After a search result opens its sub-screen, back goes to the root and back again closes settings, as without
     * the search: the closed search page leaves no extra back step.
     */
    @Test
    public void searchResultLeavesNoExtraBackStep() {
        // :: Setup
        final String summary = RuntimeEnvironment.getApplication().getString(R.string.title_When_changes_by);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.search(summary);
        driver.clickSearchResult(summary);

        // :: Act
        driver.back();
        driver.pressBack();

        // :: Verify
        assertThat(driver.isClosed()).isTrue();
    }

    /**
     * A setting the screen removes from its tree, such as the hidden test-mode serial number, does not show in search
     * results, though its sibling in the XML is searchable text.
     */
    @Test
    public void searchResultForRemovedKeyDoesNotCrash() {
        // :: Setup
        final String removed = RuntimeEnvironment.getApplication().getString(R.string.dexcom_test_mode_serial);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Act
        driver.search(SEARCH_QUERY);
        final List<String> shown = driver.searchResultTexts();

        // :: Verify
        assertThat(driver.exists("share_test_key")).isFalse();
        assertThat(shown).doesNotContain(removed);
    }

    // ===== Ringtone ==================================================================================================

    /**
     * Tapping the alert sound starts the system ringtone picker with today's extras.
     */
    @Test
    public void ringtonePickerIntent() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.showScreenOf(ALERT_SOUND);

        // :: Act
        driver.click(ALERT_SOUND);

        // :: Verify
        final Intent intent = driver.nextStartedActivityForResult();
        assertThat(intent.getAction()).isEqualTo(RingtoneManager.ACTION_RINGTONE_PICKER);
        assertThat(intent.getIntExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, -1)).isEqualTo(RingtoneManager.TYPE_ALL);
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)).isTrue();
        assertThat(intent.getBooleanExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, false)).isTrue();
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI))
                .isEqualTo(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALL));
        assertThat((Uri) intent.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI))
                .isEqualTo(Uri.parse("default"));
        assertThat(String.valueOf(intent.getCharSequenceExtra(RingtoneManager.EXTRA_RINGTONE_TITLE)))
                .isEqualTo(RuntimeEnvironment.getApplication().getString(R.string.alert_sound));
        assertThat(intent.getIntExtra(AUDIO_ATTRIBUTES_FLAGS, -1)).isEqualTo(0x40); // FLAG_BYPASS_INTERRUPTION_POLICY
    }

    /**
     * A picked sound is stored as its uri string, "no sound" is stored as an empty string, and a cancelled picker
     * (no data) leaves the stored value alone.
     */
    @Test
    public void ringtoneResultIsStoredAsUriString() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        driver.showScreenOf(ALERT_SOUND);
        final Uri picked = Settings.System.DEFAULT_NOTIFICATION_URI;

        // :: Act
        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK,
                new Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, picked));
        final String afterPick = storedSound();

        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK, new Intent());
        final String afterSilent = storedSound();

        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_CANCELED, null);
        final String afterCancel = storedSound();

        // :: Verify
        assertThat(afterPick).isEqualTo(picked.toString());
        assertThat(afterSilent).isEqualTo("");
        assertThat(afterCancel).isEqualTo("");
    }

    /**
     * The sound picker's result reaches the sound setting, and a scanner result started afterwards from the same
     * screen still reaches the screen: a scanned receiver serial is stored.
     */
    @Test
    public void ringtoneAndActivityResultsBothArrive() {
        // :: Setup
        Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA);
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Uri picked = Settings.System.DEFAULT_NOTIFICATION_URI;

        // :: Act
        driver.showScreenOf(ALERT_SOUND);
        driver.click(ALERT_SOUND);
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK,
                new Intent().putExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, picked));
        driver.showScreenOf("auto_configure");
        driver.click("auto_configure");
        driver.nextStartedActivityForResult();
        driver.deliverResult(android.app.Activity.RESULT_OK, new Intent()
                .putExtra("SCAN_RESULT", "SM12345678").putExtra("SCAN_RESULT_FORMAT", "CODE_128"));

        // :: Verify
        assertThat(storedSound()).isEqualTo(picked.toString());
        assertThat(store().getString("share_key", "<absent>")).isEqualTo("SM12345678");
    }

    // ===== Time and colour dialogs ===================================================================================

    /**
     * Setting a time in the night mode start dialog stores it as a time stamp with that hour and minute.
     */
    @Test
    public void timeDialogStoresSetTime() {
        // :: Setup
        useMiBand4();
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(NIGHT_START);
        final TimePicker picker = find(dialog.getWindow().getDecorView(), TimePicker.class);

        // :: Act
        picker.setCurrentHour(7);
        picker.setCurrentMinute(30);
        driver.press(dialog, DialogInterface.BUTTON_POSITIVE);

        // :: Verify
        final Calendar stored = Calendar.getInstance();
        stored.setTimeInMillis(store().getLong(NIGHT_START, -1));
        assertThat(stored.get(Calendar.HOUR_OF_DAY)).isEqualTo(7);
        assertThat(stored.get(Calendar.MINUTE)).isEqualTo(30);
    }

    /**
     * Closing the time dialog stores nothing.
     */
    @Test
    public void timeDialogCloseStoresNothing() {
        // :: Setup
        useMiBand4();
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(NIGHT_START);
        find(dialog.getWindow().getDecorView(), TimePicker.class).setCurrentHour(7);

        // :: Act
        driver.press(dialog, DialogInterface.BUTTON_NEGATIVE);

        // :: Verify
        assertThat(store().contains(NIGHT_START)).isFalse();
    }

    /**
     * A colour picked in the high values dialog is stored as an int.
     */
    @Test
    public void colourDialogStoresPickedColour() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(HIGH_COLOUR);

        // :: Act
        find(dialog.getWindow().getDecorView(), ColorPickerView.class).setColor(0xFF112233);
        driver.press(dialog, DialogInterface.BUTTON_POSITIVE);

        // :: Verify
        assertThat(store().getInt(HIGH_COLOUR, 0)).isEqualTo(0xFF112233);
    }

    /**
     * "Revert to default" in the colour dialog stores the setting's XML default colour.
     */
    @Test
    public void colourDialogRevertStoresDefault() {
        // :: Setup
        store().edit().putInt(HIGH_COLOUR, 0xFF112233).commit();
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(HIGH_COLOUR);

        // :: Act
        driver.press(dialog, DialogInterface.BUTTON_NEUTRAL);

        // :: Verify
        assertThat(store().getInt(HIGH_COLOUR, 0)).isEqualTo(0xFFFFBB33);
    }

    /**
     * Picking a colour draws the example graph above the colour settings again, so it shows the new colour.
     * <p>
     * The graph's colours are read through {@code Pref}, which in a test run can hold another store than the one the
     * dialog writes to, so this checks that the graph was drawn again rather than which colour it got.
     */
    @Test
    public void colourDialogRedrawsExampleChart() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(HIGH_COLOUR);
        final LineChartData before = find(driver.rowOf(EXAMPLE_CHART), LineChartView.class).getLineChartData();

        // :: Act
        find(dialog.getWindow().getDecorView(), ColorPickerView.class).setColor(0xFF112233);
        driver.press(dialog, DialogInterface.BUTTON_POSITIVE);

        // :: Verify
        final LineChartData after = find(driver.rowOf(EXAMPLE_CHART), LineChartView.class).getLineChartData();
        assertThat(after).isNotSameInstanceAs(before);
    }

    // ===== Titles and lists other classes fill in ====================================================================

    /**
     * The BlueJay screen timeout title shows the stored timeout in seconds.
     */
    @Test
    public void blueJayTimeoutTitleShowsSeconds() {
        // :: Setup
        store().edit().putInt(BLUEJAY_TIMEOUT, 2).commit();

        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.titleOf(BLUEJAY_TIMEOUT)).endsWith("  (6 seconds)");
    }

    /**
     * The Mi Band night mode interval title shows the stored interval in minutes.
     */
    @Test
    public void miBandNightModeTitleShowsMinutes() {
        // :: Setup
        useMiBand4();
        store().edit().putInt(MIBAND_NIGHT_INTERVAL, 2).commit();

        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.titleOf(MIBAND_NIGHT_INTERVAL)).isEqualTo("Interval (15 minutes)");
    }

    /**
     * The number wall start time title shows the time range the number wall is set to.
     */
    @Test
    public void numberWallTitleShowsTimeRange() {
        // :: Setup
        final String range = TimeRangeUtils.getNiceStartStopString("number_wall");

        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.titleOf(NUMBER_WALL_START)).endsWith("  (" + range + ")");
    }

    /**
     * The calibration plugin list offers every calibration plugin, in the order the plugins are declared.
     */
    @Test
    public void calibrationPluginListOffersEveryPlugin() {
        // :: Setup
        final List<String> plugins = new ArrayList<>();
        for (final PluggableCalibration.Type type : PluggableCalibration.Type.values()) {
            plugins.add(type.toString());
        }

        // :: Act
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Verify
        assertThat(driver.entryValuesOf(CALIBRATION_PLUGIN)).containsExactlyElementsIn(plugins).inOrder();
    }

    // ===== Glucose range =============================================================================================

    /**
     * A number typed into the high value field and confirmed with OK is stored as typed.
     */
    @Test
    public void highValueStoresTypedNumber() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(HIGH_VALUE);

        // :: Act
        ((EditText) dialog.findViewById(android.R.id.edit)).setText("180");
        driver.press(dialog, DialogInterface.BUTTON_POSITIVE);

        // :: Verify
        assertThat(store().getString(HIGH_VALUE, "<absent>")).isEqualTo("180");
    }

    /**
     * Text that is not a number, typed into the low value field and confirmed with OK, is not stored: the value
     * stays as it was.
     */
    @Test
    public void lowValueRejectsText() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final Dialog dialog = driver.dialogOf(LOW_VALUE);

        // :: Act
        ((EditText) dialog.findViewById(android.R.id.edit)).setText("abc");
        driver.press(dialog, DialogInterface.BUTTON_POSITIVE);

        // :: Verify
        assertThat(store().getString(LOW_VALUE, "<absent>")).isEqualTo("70");
    }

    /**
     * Picking mmol/L in the units list stores it and converts the high and low values from mg/dL to mmol/L.
     */
    @Test
    public void unitsChangeConvertsRange() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
        final AlertDialog dialog = (AlertDialog) driver.dialogOf(UNITS);

        // :: Act
        final ListView list = dialog.getListView();
        list.performItemClick(list.getChildAt(1), 1, list.getItemIdAtPosition(1));
        Shadows.shadowOf(Looper.getMainLooper()).idle();

        // :: Verify
        assertThat(store().getString(UNITS, "<absent>")).isEqualTo("mmol");
        assertThat(store().getString(HIGH_VALUE, "<absent>")).isEqualTo("9.4");
        assertThat(store().getString(LOW_VALUE, "<absent>")).isEqualTo("3.9");
    }

    // ===== Hardware data source ======================================================================================

    /**
     * Every hardware data source can be chosen from its list, and the choice is stored. The sources are chosen one
     * after the other, so each choice starts from the source chosen before it.
     */
    @Test
    public void everyHardwareDataSourceCanBeChosen() {
        final List<String> sources = SettingsScreenDriver.open(null).entryValuesOf(DATA_SOURCE);
        for (int position = 0; position < sources.size(); position++) {
            // :: Setup
            final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

            // :: Act
            chooseDataSource(driver, position);

            // :: Verify
            final String source = sources.get(position);
            assertWithMessage(source).that(store().getString(DATA_SOURCE, "<absent>")).isEqualTo(source);
        }
    }

    /**
     * Choosing a hardware data source shows the settings that belong to it and hides those of other sources: the Share
     * key only for Share, the NFC settings only for sources that scan by NFC, and the Nightscout follower URL for that
     * follower.
     * The sources are chosen one after the other, so each choice starts from the source chosen before it.
     */
    @Test
    public void hardwareDataSourceShowsItsOwnSettings() {
        final List<String> sources = SettingsScreenDriver.open(null).entryValuesOf(DATA_SOURCE);
        for (int position = 0; position < sources.size(); position++) {
            // :: Setup
            final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
            final String source = sources.get(position);
            final DexCollectionType type = DexCollectionType.getType(source);

            // :: Act
            chooseDataSource(driver, position);

            // :: Verify
            assertWithMessage(source + " share_key").that(driver.exists("share_key"))
                    .isEqualTo(type == DexCollectionType.DexcomShare);
            assertWithMessage(source + " nfc settings").that(driver.exists("xdrip_plus_nfc_settings"))
                    .isEqualTo(DexCollectionType.hasNfcScan(type));
            if (type == DexCollectionType.NSFollow) {
                assertWithMessage(source + " nsfollow_url").that(driver.exists("nsfollow_url")).isTrue();
            }
        }
    }

    /**
     * Taps row {@code position} in the hardware data source list, the way a user picks a source.
     */
    private static void chooseDataSource(final SettingsScreenDriver driver, final int position) {
        final ListView list = ((AlertDialog) driver.dialogOf(DATA_SOURCE)).getListView();
        list.performItemClick(list.getAdapter().getView(position, null, list), position,
                list.getItemIdAtPosition(position));
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    // ===== Look ======================================================================================================

    /**
     * Setting titles are drawn at 18sp, the medium text size the old screen used, on plain rows and on slider rows.
     */
    @Test
    public void titlesUseMediumTextSize() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);
        final float mediumPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18,
                RuntimeEnvironment.application.getResources().getDisplayMetrics());

        // :: Act
        final TextView switchTitle = driver.rowOf(SPEAK_SWITCH).findViewById(android.R.id.title);
        final TextView sliderTitle = driver.rowOf(SPEECH_SPEED).findViewById(android.R.id.title);

        // :: Verify
        assertThat(switchTitle.getTextSize()).isEqualTo(mediumPx);
        assertThat(sliderTitle.getTextSize()).isEqualTo(mediumPx);
    }

    /**
     * Titles of settings that can be used are white, as on the old screen, and titles of disabled settings are dimmed.
     */
    @Test
    public void titlesAreWhiteUnlessDisabled() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        final TextView usable = driver.rowOf(SPEAK_SWITCH).findViewById(android.R.id.title);
        final TextView disabled = driver.rowOf(SPEAK_THRESHOLD).findViewById(android.R.id.title);

        // :: Verify
        assertThat(usable.isEnabled()).isTrue();
        assertThat(usable.getCurrentTextColor()).isEqualTo(0xFFFFFFFF);
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(disabled.getCurrentTextColor() >>> 24).isLessThan(0xFF);
    }

    /**
     * A slider row on a phone-sized screen reserves no space for an icon it does not have, so its title lines up
     * with the other rows.
     */
    @Test
    @Config(qualifiers = "w411dp-h891dp")
    public void sliderRowReservesNoIconSpace() {
        // :: Setup
        final SettingsScreenDriver driver = SettingsScreenDriver.open(SPEAK_SCREEN);

        // :: Act
        final View iconFrame = driver.rowOf(SPEAK_THRESHOLD).findViewById(androidx.preference.R.id.icon_frame);

        // :: Verify
        assertThat(iconFrame.getVisibility()).isEqualTo(View.GONE);
    }

    /**
     * The ON/OFF text of a switch is white when the switch can be used and dimmed when it is disabled, as the old
     * switches were.
     */
    @Test
    public void switchTextDimsWhenDisabled() {
        // :: Setup
        final ContextThemeWrapper themed = new ContextThemeWrapper(RuntimeEnvironment.application,
                R.style.SettingsPref_SwitchText);

        // :: Act
        final TypedArray attrs = themed.obtainStyledAttributes(new int[]{android.R.attr.textColor});
        final ColorStateList colours = attrs.getColorStateList(0);
        attrs.recycle();

        // :: Verify
        final int enabled = colours.getColorForState(new int[]{android.R.attr.state_enabled}, 0);
        final int disabled = colours.getColorForState(new int[]{-android.R.attr.state_enabled}, 0);
        assertThat(enabled).isEqualTo(0xFFFFFFFF);
        assertThat(disabled >>> 24).isLessThan(0xFF);
    }

    // ===== Helpers ===================================================================================================

    /**
     * Stores a paired, enabled Mi Band 4, which is what puts the night mode settings on the screen and enables them.
     */
    private static void useMiBand4() {
        store().edit().putBoolean("miband_enabled", true).putString("miband_data_mac", MIBAND_MAC).commit();
        PersistentStore.setString("miband_model_" + MIBAND_MAC, MiBand.MiBandType.MI_BAND4.toString());
    }

    /**
     * The first view of {@code type} in {@code view}'s tree. Fails when there is none.
     */
    private static <T extends View> T find(final View view, final Class<T> type) {
        if (type.isInstance(view)) {
            return type.cast(view);
        }
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                final View child = ((ViewGroup) view).getChildAt(i);
                if (type.isInstance(child) || child instanceof ViewGroup) {
                    try {
                        return find(child, type);
                    } catch (AssertionError e) {
                        // not in this branch
                    }
                }
            }
        }
        throw new AssertionError("no " + type.getSimpleName() + " in the dialog");
    }

    private static String storedSound() {
        return store().getString(ALERT_SOUND, "<absent>");
    }

    private static SharedPreferences store() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
    }
}
