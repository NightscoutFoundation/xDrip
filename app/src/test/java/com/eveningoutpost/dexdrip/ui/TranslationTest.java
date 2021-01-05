package com.eveningoutpost.dexdrip.ui;


import android.content.res.Configuration;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;

import java.text.MessageFormat;
import java.util.Locale;

import lombok.val;

import static com.google.common.truth.Truth.assertWithMessage;

public class TranslationTest extends RobolectricTestWithConfig {

    /**
     * Check that MessageFormat strings are still working after resource translation
     */


    @Test
    public void testFormatStrings() {

        final Configuration config = xdrip.getAppContext().getResources().getConfiguration();
        val locales = xdrip.getAppContext().getResources().getStringArray(R.array.LocaleChoicesValues);

        String fmt;
        String result;

        for (val language : locales) {

            System.out.println("Trying choice patterns for language: " + language);
            config.setLocale(new Locale(language, "", ""));
            xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());

            // check minutes ago days
            fmt = xdrip.gs(R.string.minutes_ago);
            result = MessageFormat.format(fmt, 123);
            assertWithMessage("minutes_ago choice message format failed to contain value").that(result).contains("123");

            // check expires days
            fmt = xdrip.gs(R.string.expires_days);
            result = MessageFormat.format(fmt, 123.4f);
            assertWithMessage("expires_days choice message format failed to contain value").that(result).contains("123.4");

        }

        // restore after test
       config.setLocale(new Locale("en", "", ""));
       xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());

    }
}
