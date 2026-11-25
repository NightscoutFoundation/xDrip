package com.eveningoutpost.dexdrip.utils.settings;

import android.content.Context;
import android.content.SharedPreferences;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.google.common.truth.Truth.assertThat;

/**
 * Pins the default value of every setting that the app writes on first start.
 * <p>
 * {@link xdrip#loadDefaultPreferences} reads the settings XMLs and stores each default that is not
 * stored yet. The result, sorted as {@code key=Type:value} lines, must equal
 * {@code src/test/resources/settings/defaults-golden.txt}. A changed default, a changed stored type
 * or a lost key all show up as a diff against that file.
 * <p>
 * To regenerate the golden file after an intended change, run this test with the environment
 * variable {@code XDRIP_WRITE_GOLDEN=1} set, and review the diff of the file before committing it.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class SettingsDefaultsTest extends RobolectricTestWithConfig {

    private static final String GOLDEN = "settings/defaults-golden.txt";

    // ===== Setup =====================================================================================================

    @Before
    @Override
    public void setUp() {
        super.setUp();
        xdrip.setContextAlways(RuntimeEnvironment.application);
        defaultPrefs().edit().clear().commit();
    }

    // ===== Defaults ==================================================================================================

    /**
     * Starting from an empty store, loading the defaults gives exactly the golden set of keys, types and values.
     */
    @Test
    public void defaultsMatchGolden() throws IOException {
        // :: Act
        xdrip.loadDefaultPreferences(context());

        // :: Verify
        final String actual = render(defaultPrefs().getAll());
        if ("1".equals(System.getenv("XDRIP_WRITE_GOLDEN"))) {
            Files.write(new File("src/test/resources/" + GOLDEN).toPath(), actual.getBytes(StandardCharsets.UTF_8));
        }
        assertThat(actual).isEqualTo(readGolden());
    }

    /**
     * The four ringtone settings store their XML default {@code "default"} as a string. The new ringtone preference
     * must keep writing exactly this, or alerts read a different sound on first start.
     */
    @Test
    public void ringtoneKeysStoreTheirXmlDefault() {
        // :: Act
        xdrip.loadDefaultPreferences(context());

        // :: Verify
        final SharedPreferences prefs = defaultPrefs();
        for (final String key : new String[]{"other_alerts_sound", "calibration_notification_sound",
                "persistent_high_alert_sound", "bg_predict_alert_sound"}) {
            assertThat(prefs.getAll().get(key)).isEqualTo("default");
        }
    }

    // ===== Helpers ===================================================================================================

    private static Context context() {
        return RuntimeEnvironment.application;
    }

    private static SharedPreferences defaultPrefs() {
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context());
    }

    private static String render(final Map<String, ?> all) {
        final StringBuilder out = new StringBuilder();
        for (final Map.Entry<String, ?> entry : new TreeMap<>(all).entrySet()) {
            final Object value = entry.getValue();
            out.append(entry.getKey()).append('=')
                    .append(value == null ? "null" : value.getClass().getSimpleName()).append(':')
                    .append(value instanceof Set ? new TreeSet<>((Set<?>) value).toString() : String.valueOf(value))
                    .append('\n');
        }
        return out.toString();
    }

    private static String readGolden() throws IOException {
        try (InputStream in = SettingsDefaultsTest.class.getClassLoader().getResourceAsStream(GOLDEN)) {
            assertThat(in).isNotNull();
            return new String(readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(final InputStream in) throws IOException {
        final java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        final byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, n);
        }
        return buffer.toByteArray();
    }
}
