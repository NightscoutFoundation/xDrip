package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Tests for {@link TranslationDifferences}, the rule {@link TranslationPlaceholderTest} applies.
 * <p>
 * These use strings that are not in the app, on purpose. The sweep over the real tree is green when
 * nothing is broken, and would be just as green if the rule stopped comparing anything at all - so
 * it cannot show that a broken translation would be caught. Every test here breaks one on purpose
 * and insists that it is.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class TranslationDifferencesTest {

    /** The case the whole check exists for: the translator dropped the argument. */
    @Test
    public void aDroppedArgumentIsReported() {
        // :: Setup
        final Map<String, String> english = singleString("steps", "Steps:%1$d");
        final Map<String, String> translated = singleString("steps", "Antall skritt");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a translation that dropped its argument")
                .that(differences.keySet())
                .containsExactly("steps");
    }

    /** An argument English does not pass throws when the string is formatted. */
    @Test
    public void anAddedArgumentIsReported() {
        // :: Setup
        final Map<String, String> english = singleString("error", "Error: %s");
        final Map<String, String> translated = singleString("error", "Feil %s nummer %s");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a translation that added an argument")
                .that(differences.keySet())
                .containsExactly("error");
    }

    /** The failure has to name both sides, or the reader cannot tell what to correct. */
    @Test
    public void theDifferenceNamesTheEnglishAndTheTranslatedWording() {
        // :: Setup
        final Map<String, String> english = singleString("steps", "Steps:%1$d");
        final Map<String, String> translated = singleString("steps", "Antall skritt");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("the description of a dropped argument")
                .that(differences.get("steps"))
                .isEqualTo("[1$d] in English \"Steps:%1$d\" but [] in \"Antall skritt\"");
    }

    /**
     * The scope rule: English decides what is a placeholder. A percent sign in a translation of a
     * string the code never formats is a percent sign, and reporting it would put the check at odds
     * with every language that writes its units that way.
     */
    @Test
    public void aPercentSignIsNotReportedWhenEnglishHasNoArgument() {
        // :: Setup
        final Map<String, String> english = singleString("battery", "Low battery level");
        final Map<String, String> translated = singleString("battery", "Batteriet er under 20%");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a percent sign English does not have").that(differences).isEmpty();
    }

    /**
     * The other half of the scope rule. English {@code "Value in %"} has an argument only in the
     * sense that something unreadable is there; treating that as a placeholder would demand one of
     * every translation of a string that is never formatted.
     */
    @Test
    public void anUnreadableEnglishArgumentDoesNotPutTheKeyInScope() {
        // :: Setup
        final Map<String, String> english = singleString("turnoff", "Value in %");
        final Map<String, String> translated = singleString("turnoff", "Taso prosentteina");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a key English cannot state an argument for")
                .that(differences)
                .isEmpty();
    }

    /** Word order is the translator's to choose, so an indexed argument may move. */
    @Test
    public void reorderingAnIndexedArgumentIsNotADifference() {
        // :: Setup
        final Map<String, String> english = singleString("range", "%1$s to %2$s");
        final Map<String, String> translated = singleString("range", "fra %2$s ned til %1$s");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a translation that reordered its arguments")
                .that(differences)
                .isEmpty();
    }

    /** A translated key with no English original is not ours to judge; nothing formats it. */
    @Test
    public void aKeyThatEnglishDoesNotHaveIsNotCompared() {
        // :: Setup
        final Map<String, String> translated = singleString("removed_long_ago", "Noe med %d i");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(new HashMap<>(), translated).differences();

        // :: Verify
        assertWithMessage("differences for a key English no longer has").that(differences).isEmpty();
    }

    /**
     * English has {@code one} and {@code other} only. A language with {@code few} would go
     * uncompared against nothing at all, and those are the forms a translator most often gets wrong.
     */
    @Test
    public void aPluralQuantityEnglishLacksIsComparedAgainstTheOtherItem() {
        // :: Setup
        final Map<String, String> english = singleString("sensor_age/other", "Age: %sd");
        final Map<String, String> translated = singleString("sensor_age/few", "Alder: dager");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a plural quantity English does not define")
                .that(differences.keySet())
                .containsExactly("sensor_age/few");
    }

    /** The quantity English does define is compared against that item, not against {@code other}. */
    @Test
    public void aPluralQuantityEnglishHasIsComparedAgainstItsOwnItem() {
        // :: Setup
        final Map<String, String> english = new HashMap<>();
        english.put("sensor_age/one", "Age: %sd");
        english.put("sensor_age/other", "Age: %sd");
        final Map<String, String> translated = singleString("sensor_age/one", "Alder: %sd");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a plural quantity English defines").that(differences).isEmpty();
    }

    private static Map<String, String> singleString(final String name, final String text) {
        final Map<String, String> strings = new HashMap<>();
        strings.put(name, text);
        return strings;
    }

    /**
     * Two arguments in, two arguments out, and still broken: {@code String.format} is given an
     * {@code int} and asked for a string. Comparing how many arguments a translation consumes would
     * call this identical to English.
     */
    @Test
    public void anArgumentReadAsAnotherTypeIsReported() {
        // :: Setup
        final Map<String, String> english = singleString("steps", "Steps: %1$d today");
        final Map<String, String> translated = singleString("steps", "Skritt: %1$s i dag");

        // :: Act
        final SortedMap<String, String> differences =
                TranslationDifferences.compare(english, translated).differences();

        // :: Verify
        assertWithMessage("differences for a translation that changed the conversion")
                .that(differences.keySet())
                .containsExactly("steps");
    }

    /**
     * What was compared is reported alongside what differed, because the two look the same from the
     * outside: a rule that silently stopped comparing anything would report no differences either.
     */
    @Test
    public void onlyTheKeysEnglishAsksAnArgumentForAreCompared() {
        // :: Setup
        final Map<String, String> english = new HashMap<>();
        english.put("formatted", "Steps:%1$d");
        english.put("plain", "Settings");
        english.put("percentage", "Value in %");
        final Map<String, String> translated = new HashMap<>();
        translated.put("formatted", "Skritt:%1$d");
        translated.put("plain", "Innstillinger");
        translated.put("percentage", "Verdi i %");
        translated.put("only_in_norwegian", "Noe annet");

        // :: Act
        final SortedSet<String> compared = TranslationDifferences.compare(english, translated).comparedKeys();

        // :: Verify
        assertWithMessage("the keys a comparison says it measured")
                .that(compared)
                .containsExactly("formatted");
    }
}
