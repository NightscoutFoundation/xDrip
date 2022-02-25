package com.eveningoutpost.dexdrip.ui;


import android.content.res.Configuration;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import lombok.val;

import static com.google.common.truth.Truth.assertWithMessage;

public class TranslationTest extends RobolectricTestWithConfig {

    /**
     * Check that MessageFormat strings are still working after resource translation
     */


    @Test
    public void testFormatStrings() {

        final Configuration config = xdrip.getAppContext().getResources().getConfiguration();
        val internal = xdrip.getAppContext().getResources().getStringArray(R.array.LocaleChoicesValues);
        val extra = new String[]{"ar", "cs", "de", "el", "en", "es", "fi", "fr", "he", "hr", "it", "iw", "ja", "ko", "nb", "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "zh"};
        Set<String> locales = new TreeSet<>(Arrays.asList(internal));
        locales.addAll(Arrays.asList(extra));
        String fmt;
        String result;

        for (val language : locales) {

            System.out.println("Trying choice patterns for language: " + language);
            config.setLocale(new Locale(language, "", ""));
            xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());

            try {
                // check minutes ago days
                fmt = xdrip.gs(R.string.minutes_ago);
                result = MessageFormat.format(fmt, 123);
                assertWithMessage("minutes_ago choice message format failed to contain value").that(result).contains("123");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Failed minutes ago test with language " + language + " with exception: " + e);
            }

            try {
                // check expires days
                fmt = xdrip.gs(R.string.expires_days);
                result = MessageFormat.format(fmt, 123.4f);
                assertWithMessage("expires_days choice message format failed to contain value").that(result).contains("123.4");
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Failed expires days test with language " + language + " with exception: " + e);
            }
        }

        // restore after test
        config.setLocale(new Locale("en", "", ""));
        xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());

    }
}
