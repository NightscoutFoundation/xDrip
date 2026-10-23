package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Checks that no translation has lost, gained or broken a format argument of the English string it
 * replaces.
 * <p>
 * A translator has no way of knowing that {@code %1$d} is a number the code will pass in. When one
 * is dropped, the number silently disappears from the text in that language only; when one is
 * added, {@code String.format} throws where it never throws in English. Both are invisible until
 * somebody runs the app in that language, which is why this compares the files instead.
 * <p>
 * <b>Why this reads the source tree rather than resources.</b> Unit tests are given
 * {@code apk_for_local_test/.../apk-for-local-test.ap_}, and under the {@code fast} flavor that
 * archive is English-only: {@code resConfigs "en", "xxhdpi"} (app/build.gradle:233) drops the other
 * languages when AAPT2 links it. The merged resources still hold every language, so the loss
 * happens at link time rather than at merge time - either way, a test that asked the resource
 * framework for a Bulgarian string here would be told it does not exist. The files are the only
 * place every language is present in every variant.
 * <p>
 * What is read is {@link StringResources}; what is compared against what, and which keys are
 * compared at all, is {@link TranslationDifferences}. {@code values-en} is read like any other
 * language, so a divergence between it and {@code values} would be reported as one.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class TranslationPlaceholderTest {

    /** Below this many English strings the tree has moved, and a green sweep would mean nothing. */
    private static final int ENGLISH_STRINGS_EXPECTED = 1000;

    /**
     * Below this many compared translations the sweep has stopped reaching most of the tree. It sits
     * well under what is there today, so that translations coming and going never fails it - it is a
     * tripwire against the sweep shrinking, not a count of the tree.
     */
    private static final int COMPARISONS_EXPECTED = 700;

    /** Fewer languages than this and a directory has gone missing rather than a language. */
    private static final int LANGUAGES_EXPECTED = 30;

    /** The directory holding the English strings every translation is measured against. */
    private static final String ENGLISH = "values";

    /** Every directory whose name starts with this holds one language's translations. */
    private static final String LANGUAGE_PREFIX = "values-";

    /** Nothing may differ from English in the arguments it consumes. */
    @Test
    public void everyTranslationConsumesTheSameArgumentsAsTheEnglishString() {
        // :: Act
        final Sweep sweep = sweep();

        // :: Verify
        assertWithMessage("translations whose format arguments no longer match English\n%s",
                report(sweep.differences))
                .that(sweep.differences.keySet())
                .isEmpty();
    }

    /**
     * A guard on the sweep itself. The test above passes when nothing is broken and would pass just
     * as well if the files stopped being found, so what was actually read has to be stated.
     * <p>
     * The languages it expects are found by listing the resource tree a second time, by a different
     * route than the sweep uses, so that narrowing the sweep's own search makes the two disagree
     * instead of moving them together.
     */
    @Test
    public void everyLanguageThatCarriesStringsIsCompared() {
        // :: Act
        final Sweep sweep = sweep();

        // :: Verify
        assertWithMessage("languages holding strings that the sweep did not read")
                .that(sweep.languages)
                .containsExactlyElementsIn(languagesCarryingStrings());
        assertWithMessage("languages compared")
                .that(sweep.languages.size())
                .isAtLeast(LANGUAGES_EXPECTED);
    }

    /**
     * And a guard on how much of each language was read. A sweep can keep every language and still
     * compare almost nothing - by losing the plurals, by reading one file per directory, or by
     * narrowing what counts as an argument - and it reports no differences either way.
     */
    @Test
    public void theSweepComparesTheTranslationsItIsMeantTo() {
        // :: Act
        final Sweep sweep = sweep();

        // :: Verify
        assertWithMessage("translations compared against an English string with an argument")
                .that(sweep.comparisons)
                .isAtLeast(COMPARISONS_EXPECTED);
    }

    /**
     * Every language measured against English.
     * <p>
     * The English side is checked before anything is compared, so that this cannot quietly return
     * nothing: were the tree to move, every comparison would be against an empty map and every
     * caller would pass.
     */
    private static Sweep sweep() {
        final Map<String, String> english = StringResources.in(resourceDirectory().resolve(ENGLISH));
        if (english.size() < ENGLISH_STRINGS_EXPECTED) {
            throw new IllegalStateException("Only " + english.size() + " English strings in "
                    + resourceDirectory().resolve(ENGLISH).toAbsolutePath() + "; the tree has moved");
        }
        final Sweep sweep = new Sweep();
        for (final Path language : languageDirectories()) {
            final Map<String, String> translated = StringResources.in(language);
            if (translated.isEmpty()) {
                continue; // values-v14 and the like: a resource directory, but not a language
            }
            final String locale = language.getFileName().toString();
            sweep.languages.add(locale);
            final TranslationDifferences.Comparison comparison =
                    TranslationDifferences.compare(english, translated);
            sweep.comparisons += comparison.comparedKeys().size();
            for (final Map.Entry<String, String> difference : comparison.differences().entrySet()) {
                sweep.differences.put(locale + "/" + difference.getKey(), difference.getValue());
            }
        }
        return sweep;
    }

    /** What one pass over the tree found, so that the guards can assert on the same pass. */
    private static final class Sweep {
        private final SortedMap<String, String> differences = new TreeMap<>();
        private final SortedSet<String> languages = new TreeSet<>();
        private int comparisons;
    }

    private static String report(final SortedMap<String, String> differences) {
        final StringBuilder report = new StringBuilder();
        for (final Map.Entry<String, String> difference : differences.entrySet()) {
            report.append("  ").append(difference.getKey())
                    .append(": ").append(difference.getValue()).append('\n');
        }
        return report.toString();
    }

    /** The language directories, as the sweep finds them. */
    private static List<Path> languageDirectories() {
        final List<Path> directories = new ArrayList<>();
        try (DirectoryStream<Path> locales =
                     Files.newDirectoryStream(resourceDirectory(), LANGUAGE_PREFIX + "*")) {
            for (final Path locale : locales) {
                directories.add(locale);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not list languages under " + resourceDirectory(), e);
        }
        Collections.sort(directories);
        return directories;
    }

    /** The same languages, found by walking the tree rather than by asking it for a pattern. */
    private static SortedSet<String> languagesCarryingStrings() {
        final SortedSet<String> languages = new TreeSet<>();
        try (Stream<Path> tree = Files.list(resourceDirectory())) {
            tree.filter(Files::isDirectory)
                    .filter(directory -> directory.getFileName().toString().startsWith(LANGUAGE_PREFIX))
                    .filter(directory -> !StringResources.in(directory).isEmpty())
                    .forEach(directory -> languages.add(directory.getFileName().toString()));
        } catch (Exception e) {
            throw new IllegalStateException("Could not walk " + resourceDirectory(), e);
        }
        return languages;
    }

    /**
     * The resource tree, found relative to wherever the tests are run from: the module directory
     * when Gradle runs them, the project root when a development tool does.
     */
    private static Path resourceDirectory() {
        final Path fromModule = Paths.get("src", "main", "res");
        return Files.isDirectory(fromModule) ? fromModule : Paths.get("app", "src", "main", "res");
    }
}
