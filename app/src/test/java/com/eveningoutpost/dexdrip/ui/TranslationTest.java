package com.eveningoutpost.dexdrip.ui;


import static com.eveningoutpost.dexdrip.xdrip.gs;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.file.FileVisitResult.CONTINUE;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.xdrip;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import lombok.val;

public class TranslationTest extends RobolectricTestWithConfig {

    /**
     * Check that MessageFormat strings are still working after resource translation
     */


    @Test
    public void testFormatStrings() throws IOException {
        val config = xdrip.getAppContext().getResources().getConfiguration();
        val internal = xdrip.getAppContext().getResources().getStringArray(R.array.LocaleChoicesValues);
        val extra = new String[]{"ar", "cs", "de", "el", "en", "es", "fi", "fr", "he", "hr", "it", "iw", "ja", "ko", "nb", "nl", "no", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "zh"};
        val inset = "^values-";
        Set<String> locales = new TreeSet<>(Arrays.asList(internal));
        class ResourceLocaleParser implements FileVisitor<Path> {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) {
                val s = dir.getFileName().toString();
                if (s.matches(inset + "[a-z][a-z]($|-r[a-zA-Z][a-zA-Z]$)")) {
                    val locale = s.replaceFirst(inset, "").replace("-r", "-");
                    locales.add(locale);
                }
                return CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                return CONTINUE;
            }
            @Override
            public FileVisitResult visitFileFailed(Path file, IOException e) {
                return CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                return CONTINUE;
            }
        }

        val resourcePath = RuntimeEnvironment.application
                .getPackageResourcePath()
                .replaceFirst("apk_for_local_test(?:\\\\|/).*", "merged-not-compiled-resources");
        val size = locales.size();
        Files.walkFileTree(Paths.get(resourcePath), new ResourceLocaleParser());
        assertWithMessage("No locales added from resources - this seems unlikely").that(locales.size()).isGreaterThan(size);
        locales.addAll(Arrays.asList(extra));
        String fmt;
        String result;

        for (val language : locales) {

            System.out.println("Trying choice patterns for language: " + language);
            val langCountry = language.split("-");
            config.setLocale(new Locale(langCountry[0], langCountry.length > 1 ? langCountry[1] : "", ""));
            xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());

            try {
                // check minutes ago days
                fmt = gs(R.string.minutes_ago);
                result = MessageFormat.format(fmt, 123);
                assertWithMessage("minutes_ago choice message format failed to contain value").that(result).contains("123");
            } catch (IllegalArgumentException e) {
                restoreLocale();
                throw new RuntimeException("Failed minutes ago test with language " + language + " with exception: " + e);
            }

            try {
                // check expires days
                fmt = gs(R.string.expires_days);
                result = MessageFormat.format(fmt, 123.4f);
                assertWithMessage("expires_days choice message format failed to contain value").that(result).contains("123.4");
            } catch (IllegalArgumentException e) {
                restoreLocale();
                throw new RuntimeException("Failed expires days test with language " + language + " with exception: " + e);
            }

            assertWithMessage("megabyte format " + language).that(gs(R.string.megabyte_format_string)).contains("%.1f");
            assertWithMessage("time format " + language).that(gs(R.string.format_string_ago)).contains("%s");

        }

        restoreLocale();
    }

    private void restoreLocale() {
        // restore after test
        val config = xdrip.getAppContext().getResources().getConfiguration();
        config.setLocale(new Locale("en", "", ""));
        xdrip.getAppContext().getResources().updateConfiguration(config, xdrip.getAppContext().getResources().getDisplayMetrics());
    }
}
