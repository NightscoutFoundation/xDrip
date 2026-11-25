package com.eveningoutpost.dexdrip.utils.settings;

import android.content.SharedPreferences;
import android.util.AttributeSet;
import android.widget.EditText;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.watch.miband.Const;
import com.eveningoutpost.dexdrip.watch.miband.MiBandEntry;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Pins that every text field in settings opens with the keyboard and limits its XML asks for.
 * <p>
 * The framework passes all XML attributes of an {@code EditTextPreference} to its text field. androidx does not, so
 * after the move the code must apply them itself. This test compares the field of the real edit dialog with a
 * reference {@link EditText} built from the same XML element.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SettingsEditTextTest extends RobolectricTestWithConfig {

    /** Mac address of a pretend Mi Band, so its model can be stored. */
    private static final String MIBAND_MAC = "AA:BB:CC:DD:EE:FF";

    /** Text fields that the settings code removes in every state, so no one can open them. */
    private static final Set<String> NEVER_SHOWN = Collections.singleton("share_test_key");

    /** Settings states that between them show every text field, each as the preference values it sets. */
    private static final Map<String, Map<String, String>> STATES = new LinkedHashMap<>();

    static {
        STATES.put("defaults", Collections.emptyMap());
        STATES.put("engineering mode", prefs("engineering_mode", "true", "enable_bugfender", "true"));
        STATES.put("G5 source", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "DexcomG5", "engineering_mode", "true"));
        STATES.put("Share source", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "DexcomShare"));
        STATES.put("Libre wifi source",
                prefs(DexCollectionType.DEX_COLLECTION_METHOD, "LibreWifi", "engineering_mode", "true",
                        "nfc_show_age", "false"));
        STATES.put("Nightscout follower", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "NSFollower"));
        STATES.put("Share follower", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "SHFollower"));
        STATES.put("Web follower", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "WebFollower"));
        STATES.put("CareLink follower", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "CLFollower"));
        STATES.put("Medtrum source", prefs(DexCollectionType.DEX_COLLECTION_METHOD, "Medtrum"));
        STATES.put("Mi Band 4",
                prefs(MiBandEntry.PREF_MIBAND_MAC, MIBAND_MAC, MiBandEntry.PREF_MIBAND_ENABLED, "true"));
    }

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
    }

    // ===== Fields ====================================================================================================

    /**
     * Each text field in settings has the input type, length limit, key listener, line limit and selectability that
     * its XML element gives a plain {@link EditText}.
     * <p>
     * Many fields only show for one data source, in engineering mode or for one Mi Band model, so the screen is opened
     * once per state in {@link #STATES}. Every text field in the XMLs must be compared in at least one of them, except
     * those in {@link #NEVER_SHOWN}, which must stay hidden in all of them.
     */
    @Test
    public void everyFieldMatchesItsXml() {
        // :: Setup
        final Map<String, String> expected = new LinkedHashMap<>();
        SettingsXml.forEach("EditTextPreference", (key, attrs) -> expected.put(key, EditTextProbe.describe(reference(attrs))));
        final Set<String> compared = new TreeSet<>();
        final Set<String> shown = new TreeSet<>();

        // :: Act
        for (final Map.Entry<String, Map<String, String>> state : STATES.entrySet()) {
            startFrom(state.getValue());
            final SettingsScreenDriver driver = SettingsScreenDriver.open(null);
            for (final Map.Entry<String, String> field : expected.entrySet()) {
                final String key = field.getKey();
                if (!driver.exists(key)) {
                    continue;
                }
                shown.add(key);
                if (compared.contains(key)) {
                    continue;
                }
                assertWithMessage("text field " + key + " in state " + state.getKey())
                        .that(EditTextProbe.describe(driver.editTextOf(key))).isEqualTo(field.getValue());
                compared.add(key);
            }
        }

        // :: Verify
        final Set<String> showable = new TreeSet<>(expected.keySet());
        showable.removeAll(NEVER_SHOWN);
        assertWithMessage("text fields compared in some state").that(compared).containsExactlyElementsIn(showable);
        assertWithMessage("text fields shown in some state").that(shown).containsNoneIn(NEVER_SHOWN);
    }

    // ===== Transmitter id ============================================================================================

    /**
     * The transmitter id field shows a stored id in capitals, as typed ids are, so pressing OK stores it in capitals.
     */
    @Test
    public void transmitterIdShowsStoredIdInCapitals() {
        // :: Setup
        startFrom(prefs(DexCollectionType.DEX_COLLECTION_METHOD, "DexcomG5", "dex_txid", "8abc12"));
        final SettingsScreenDriver driver = SettingsScreenDriver.open(null);

        // :: Act
        final EditText field = driver.editTextOf("dex_txid");

        // :: Verify
        assertWithMessage("transmitter id field text").that(field.getText().toString()).isEqualTo("8ABC12");
    }

    // ===== Helpers ===================================================================================================

    private static Map<String, String> prefs(final String... keysAndValues) {
        final Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            values.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        return values;
    }

    /**
     * Resets every setting to its default, then applies the state. {@code "true"} and {@code "false"} are stored as
     * booleans, everything else as a string. The Mi Band model lives in the persistent store under the band's mac
     * address.
     */
    private static void startFrom(final Map<String, String> state) {
        final SharedPreferences prefs = android.preference.PreferenceManager
                .getDefaultSharedPreferences(RuntimeEnvironment.application);
        prefs.edit().clear().commit();
        xdrip.loadDefaultPreferences(RuntimeEnvironment.application);
        final SharedPreferences.Editor editor = prefs.edit();
        for (final Map.Entry<String, String> value : state.entrySet()) {
            if ("true".equals(value.getValue()) || "false".equals(value.getValue())) {
                editor.putBoolean(value.getKey(), Boolean.parseBoolean(value.getValue()));
            } else {
                editor.putString(value.getKey(), value.getValue());
            }
        }
        editor.commit();
        if (state.containsKey(MiBandEntry.PREF_MIBAND_MAC)) {
            PersistentStore.setString("miband_model_" + MIBAND_MAC, Const.MIBAND_NAME_4);
        }
    }

    private static EditText reference(final AttributeSet attrs) {
        return new EditText(RuntimeEnvironment.getApplication(), attrs);
    }
}
