package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Checks that no xDrip log call logs text read from a translatable string resource.
 * <p>
 * Logs are read by the people who help users, not by the users themselves: the Event Log in a
 * screenshot, a logcat attached to an issue. A line in Bulgarian or Japanese is a line the support
 * group cannot read or search for. So what is logged stays English, and what the user sees - a
 * toast, a notification, a status line - stays translated. Where both are wanted, the log call
 * gets the English text written out and the resource stays for the user.
 * <p>
 * The Event Log ({@code ueh}, {@code uel}) is read by both: the user, who is helped by it directly,
 * and the support group. A line there may be translated as long as the English follows it, which
 * is what {@code xdrip.gsWithEnglish} gives.
 * <p>
 * <b>Why this reads the source tree.</b> Under the {@code fast} flavor the unit tests only have
 * English resources (see {@link TranslationPlaceholderTest}), so a log line looks right in every
 * test whether it came from a resource or not. The source is the only place the difference shows.
 * <p>
 * What counts as a hit is {@link TranslatedLogScanner}, and its limits are this test's limits: it
 * follows a variable to the end of the block that declares it, but not text returned from another
 * method or handed to a helper that logs its argument. Today that lets translated text through
 * {@code Home.toaststaticnext} (to the Event Log, from {@code Home}, {@code NSClientReceiver} and
 * {@code AidexReceiver}), {@code Home.toast}, {@code ShareFollowDownload.msg},
 * {@code MedtrumCollectionService.status} and {@code Reminders.setFloaterText}. Nor does it see
 * inside a Kotlin string template ({@code "${getString(...)}"}) or a {@code StringBuilder}. A
 * string declared {@code translatable="false"} in {@code values} is not counted. Only {@code app}
 * is read; {@code wear} has no translations.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class NoTranslatedStringsInLogsTest {

    /**
     * Below this many source files the tree has moved, and a green sweep would mean nothing. Like the
     * two below it sits under what is there today, so that code coming and going never fails it;
     * should a large removal make it fail, it is lowered by hand.
     */
    private static final int SOURCE_FILES_EXPECTED = 900;

    /**
     * Below this many log calls, counted by the scanner's own pattern, the scanner has stopped
     * recognising them. A tripwire against it going blind, not a count of the tree.
     */
    private static final int LOG_CALLS_EXPECTED = 5000;

    /** The same tripwire for the other half of a hit: string-resource reads the scanner recognises. */
    private static final int RESOURCE_READS_EXPECTED = 700;

    /** Fewer strings declared untranslatable than this and the resource files were not found. */
    private static final int UNTRANSLATABLE_EXPECTED = 40;

    /** One string declaration's opening tag, attributes in any order. */
    private static final Pattern STRING_TAG = Pattern.compile("<string\\b[^>]*>");

    private static final Pattern NAME = Pattern.compile("\\bname\\s*=\\s*\"(\\w+)\"");

    /** Nothing in {@code app} may log translated text, unless to the Event Log with the English after it. */
    @Test
    public void noLogCallLogsTranslatedText() {
        // :: Act
        final Sweep sweep = sweep();

        // :: Verify
        assertWithMessage("log calls that log translated text. Logs are read by the support group"
                + " and must stay English: write the English text in the log call, and keep the"
                + " translated string for what the user sees. In the Event Log (ueh, uel), use"
                + " xdrip.gsWithEnglish to log the translated text with the English after it.\n%s",
                report(sweep.findings))
                .that(sweep.findings)
                .isEmpty();
    }

    /**
     * A guard on the sweep itself. The test above passes when nothing is wrong and would pass just
     * as well if the files stopped being found, so what was actually read has to be stated.
     */
    @Test
    public void theSweepReadsTheTreeItIsMeantTo() {
        // :: Act
        final Sweep sweep = sweep();

        // :: Verify
        assertWithMessage("source files read under %s", sourceDirectory().toAbsolutePath())
                .that(sweep.files)
                .isAtLeast(SOURCE_FILES_EXPECTED);
        assertWithMessage("log calls the scanner recognised")
                .that(sweep.logCalls)
                .isAtLeast(LOG_CALLS_EXPECTED);
        assertWithMessage("string-resource reads the scanner recognised")
                .that(sweep.resourceReads)
                .isAtLeast(RESOURCE_READS_EXPECTED);
        assertWithMessage("strings declared untranslatable under %s", valuesDirectory().toAbsolutePath())
                .that(sweep.untranslatable.size())
                .isAtLeast(UNTRANSLATABLE_EXPECTED);
    }

    /** Every Java and Kotlin file under {@code app/src/main/java}, scanned. */
    private static Sweep sweep() {
        final Path root = sourceDirectory();
        final List<Path> files;
        try (Stream<Path> tree = Files.walk(root)) {
            files = tree.filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".java") || file.toString().endsWith(".kt"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("Could not walk " + root.toAbsolutePath(), e);
        }
        final Sweep sweep = new Sweep();
        sweep.untranslatable.addAll(untranslatableStrings());
        for (final Path file : files) {
            final String source = read(file);
            sweep.files++;
            sweep.logCalls += TranslatedLogScanner.logCallsIn(source);
            sweep.resourceReads += TranslatedLogScanner.resourceReadsIn(source);
            sweep.findings.addAll(
                    TranslatedLogScanner.scan(root.relativize(file).toString(), source, sweep.untranslatable));
        }
        return sweep;
    }

    /** The names of the English strings declared {@code translatable="false"}. */
    private static Set<String> untranslatableStrings() {
        final Set<String> names = new TreeSet<>();
        try (Stream<Path> values = Files.list(valuesDirectory())) {
            for (final Path file : values.filter(f -> f.toString().endsWith(".xml")).collect(Collectors.toList())) {
                final Matcher tag = STRING_TAG.matcher(read(file));
                while (tag.find()) {
                    final Matcher name = NAME.matcher(tag.group());
                    if (tag.group().contains("translatable=\"false\"") && name.find()) {
                        names.add(name.group(1));
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Could not list " + valuesDirectory().toAbsolutePath(), e);
        }
        return names;
    }

    /** What one pass over the tree found, so that the guard can assert on the same pass. */
    private static final class Sweep {
        private final List<TranslatedLogScanner.Finding> findings = new ArrayList<>();
        private final Set<String> untranslatable = new TreeSet<>();
        private int files;
        private int logCalls;
        private int resourceReads;
    }

    private static String read(final Path file) {
        try {
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + file.toAbsolutePath(), e);
        }
    }

    private static String report(final List<TranslatedLogScanner.Finding> findings) {
        final StringBuilder report = new StringBuilder();
        for (final TranslatedLogScanner.Finding finding : findings) {
            report.append("  ").append(finding).append('\n');
        }
        return report.toString();
    }

    /**
     * The source tree, found relative to wherever the tests are run from: the module directory
     * when Gradle runs them, the project root when a development tool does.
     */
    private static Path sourceDirectory() {
        return mainDirectory().resolve("java");
    }

    /** The English resource files, found the same way. */
    private static Path valuesDirectory() {
        return mainDirectory().resolve("res").resolve("values");
    }

    private static Path mainDirectory() {
        final Path fromModule = Paths.get("src", "main");
        return Files.isDirectory(fromModule) ? fromModule : Paths.get("app", "src", "main");
    }
}
