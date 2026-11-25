package com.eveningoutpost.dexdrip.utils.settings;

import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Xml;

import com.eveningoutpost.dexdrip.R;

import org.robolectric.RuntimeEnvironment;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Reads the settings XMLs that the settings screen is built from, so tests can list every element of one kind.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
final class SettingsXml {

    /** The XMLs the settings screen adds, in order. */
    static final int[] SCREEN_XMLS = {R.xml.pref_license, R.xml.pref_general, R.xml.pref_notifications,
            R.xml.pref_data_source, R.xml.pref_data_sync, R.xml.pref_advanced_settings, R.xml.xdrip_plus_prefs};

    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    private SettingsXml() {
    }

    /**
     * The keys of every element named {@code tag} (simple class name, e.g. {@code EditTextPreference}).
     */
    static List<String> keysOf(final String tag) {
        final List<String> keys = new ArrayList<>();
        forEach(tag, (key, attrs) -> keys.add(key));
        return keys;
    }

    /**
     * Calls {@code action} with the key and the attributes of every keyed element named {@code tag}, while the parser
     * stands on that element.
     */
    static void forEach(final String tag, final BiConsumer<String, AttributeSet> action) {
        for (final int xml : SCREEN_XMLS) {
            try (XmlResourceParser parser = RuntimeEnvironment.getApplication().getResources().getXml(xml)) {
                for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                    if (event != XmlPullParser.START_TAG || !simpleName(parser.getName()).equals(tag)) {
                        continue;
                    }
                    final String key = parser.getAttributeValue(ANDROID_NS, "key");
                    if (key != null) {
                        action.accept(key, Xml.asAttributeSet(parser));
                    }
                }
            } catch (Exception e) {
                throw new AssertionError("cannot read settings XML " + xml, e);
            }
        }
    }

    /**
     * The keyed sub-screens whose tap starts an activity instead of opening, mapped to that activity's class name.
     */
    static Map<String, String> intentScreens() {
        final Map<String, String> out = new LinkedHashMap<>();
        for (final int xml : SCREEN_XMLS) {
            try (XmlResourceParser parser = RuntimeEnvironment.getApplication().getResources().getXml(xml)) {
                String screenKey = null;
                for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                    if (event != XmlPullParser.START_TAG) {
                        continue;
                    }
                    final String tag = simpleName(parser.getName());
                    if (tag.equals("intent") && screenKey != null) {
                        out.put(screenKey, parser.getAttributeValue(ANDROID_NS, "targetClass"));
                    }
                    screenKey = tag.equals("PreferenceScreen") ? parser.getAttributeValue(ANDROID_NS, "key") : null;
                }
            } catch (Exception e) {
                throw new AssertionError("cannot read settings XML " + xml, e);
            }
        }
        return out;
    }

    /**
     * The keyed sub-screens with no child element at all; the code gives them a click action instead.
     */
    static List<String> childlessScreens() {
        final List<String> out = new ArrayList<>();
        for (final int xml : SCREEN_XMLS) {
            try (XmlResourceParser parser = RuntimeEnvironment.getApplication().getResources().getXml(xml)) {
                String screenKey = null;
                for (int event = parser.getEventType(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
                    if (event == XmlPullParser.END_TAG && screenKey != null) {
                        out.add(screenKey);
                    }
                    if (event == XmlPullParser.START_TAG || event == XmlPullParser.END_TAG) {
                        screenKey = event == XmlPullParser.START_TAG
                                && simpleName(parser.getName()).equals("PreferenceScreen")
                                ? parser.getAttributeValue(ANDROID_NS, "key") : null;
                    }
                }
            } catch (Exception e) {
                throw new AssertionError("cannot read settings XML " + xml, e);
            }
        }
        return out;
    }

    private static String simpleName(final String tag) {
        return tag.substring(tag.lastIndexOf('.') + 1);
    }
}
