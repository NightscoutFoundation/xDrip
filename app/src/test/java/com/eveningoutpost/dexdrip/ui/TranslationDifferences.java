package com.eveningoutpost.dexdrip.ui;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Compares the format arguments of translated strings against the English ones they replace.
 * <p>
 * This is the rule {@link TranslationPlaceholderTest} applies to the resource tree, kept apart from
 * the tree so that it can be asked about strings that do not exist in it. A sweep over real files
 * can only show that nothing is broken today; it cannot show that a broken string would be caught.
 * {@link TranslationDifferencesTest} does that, by handing this pairs it makes up.
 * <p>
 * <b>Only a key whose English string has a readable argument is compared.</b> Both halves matter.
 * A translation is measured against what English asks the call site for, so a {@code %} in a
 * translation of a string English never formats is a percent sign, not a lost placeholder - and
 * "has an argument" has to mean one {@link FormatArguments} could actually read, or English
 * {@code "Value in %"} would put four languages in scope for a string that is never formatted at
 * all (it is read at {@code Preferences:2058} and concatenated).
 * <p>
 * Keys are the string name, or {@code name/quantity} for one item of a {@code <plurals>}. A
 * quantity English does not have - Slavic {@code few} and {@code many}, which English lacks
 * entirely - is compared against the English {@code other} item, because that is the wording the
 * translator was given and {@code getQuantityString} passes it the same argument either way.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public final class TranslationDifferences {

    /** The item every language has, and the one a quantity English lacks is measured against. */
    private static final String OTHER = "other";

    private TranslationDifferences() {
    }

    /**
     * What comparing one set of translated strings against English found: which keys were compared
     * at all, and which of those differ.
     * <p>
     * The keys that were compared are reported alongside the differences because a sweep that
     * compares nothing finds nothing, and the two are indistinguishable from the outside. Counting
     * them is what lets the sweep say how much it actually looked at.
     */
    public static Comparison compare(final Map<String, String> english,
                                     final Map<String, String> translated) {
        final SortedMap<String, String> differences = new TreeMap<>();
        final SortedSet<String> compared = new TreeSet<>();
        for (final Map.Entry<String, String> entry : translated.entrySet()) {
            final String source = sourceFor(english, entry.getKey());
            if (source == null) {
                continue; // nothing in English to compare against, so nothing formats it either
            }
            final List<String> expected = FormatArguments.of(source);
            if (!hasReadableArgument(expected)) {
                continue; // English asks for nothing readable here; a % in the translation is a percent sign
            }
            compared.add(entry.getKey());
            final List<String> actual = FormatArguments.of(entry.getValue());
            if (!expected.equals(actual)) {
                differences.put(entry.getKey(),
                        expected + " in English \"" + source + "\""
                                + " but " + actual + " in \"" + entry.getValue() + "\"");
            }
        }
        return new Comparison(compared, differences);
    }

    /** The outcome of one comparison: what was looked at, and what was wrong with it. */
    public static final class Comparison {

        private final SortedSet<String> comparedKeys;
        private final SortedMap<String, String> differences;

        private Comparison(final SortedSet<String> comparedKeys,
                           final SortedMap<String, String> differences) {
            this.comparedKeys = comparedKeys;
            this.differences = differences;
        }

        /** The keys English asks a readable argument for, and which were therefore measured. */
        public SortedSet<String> comparedKeys() {
            return comparedKeys;
        }

        /** The compared keys whose arguments differ, each described with both wordings. */
        public SortedMap<String, String> differences() {
            return differences;
        }
    }

    /** The English wording a translated key replaces, falling back to {@code other} for a plural. */
    private static String sourceFor(final Map<String, String> english, final String key) {
        final String source = english.get(key);
        if (source != null) {
            return source;
        }
        final int quantity = key.indexOf(StringResources.QUANTITY_SEPARATOR);
        return quantity < 0
                ? null
                : english.get(key.substring(0, quantity) + StringResources.QUANTITY_SEPARATOR + OTHER);
    }

    /** Whether English asks for at least one argument that {@link FormatArguments} could read. */
    private static boolean hasReadableArgument(final List<String> arguments) {
        for (final String argument : arguments) {
            if (!FormatArguments.UNREADABLE.equals(argument)) {
                return true;
            }
        }
        return false;
    }
}
