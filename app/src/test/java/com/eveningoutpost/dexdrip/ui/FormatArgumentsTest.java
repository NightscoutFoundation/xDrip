package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.util.List;

/**
 * Tests for {@link FormatArguments}, the reader that {@link TranslationPlaceholderTest} compares
 * translations with.
 * <p>
 * Without these, a reader that quietly found nothing would leave that sweep green forever: every
 * string would have the same empty argument list as every other, and no broken translation could
 * ever fail it. Each test below pins one thing the reader has to keep telling apart.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public class FormatArgumentsTest {

    /** An argument written without an index gets the position it is given at runtime. */
    @Test
    public void argumentsWithoutAnIndexAreNumberedInTheOrderTheyAppear() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%s went from %d to %d");

        // :: Verify
        assertWithMessage("arguments of a string with three unindexed specifiers")
                .that(arguments)
                .containsExactly("1$s", "2$d", "3$d");
    }

    /** An index written into the string wins over the position, which is what lets a translation reorder. */
    @Test
    public void anExplicitIndexIsKept() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%2$d mg/dL at %1$s");

        // :: Verify
        assertWithMessage("arguments of a string that indexes its specifiers")
                .that(arguments)
                .containsExactly("1$s", "2$d");
    }

    /**
     * The point of sorting: a translation may need the arguments in another order than English, and
     * that is not a defect, so the two have to compare equal.
     */
    @Test
    public void reorderingIndexedArgumentsDoesNotChangeTheResult() {
        // :: Setup
        final String english = "%1$s is %2$d";
        final String translated = "%2$d er verdien for %1$s";

        // :: Act
        final List<String> arguments = FormatArguments.of(translated);

        // :: Verify
        assertWithMessage("a translation that moves an indexed argument")
                .that(arguments)
                .isEqualTo(FormatArguments.of(english));
    }

    /** {@code %%} prints a percent sign and consumes nothing, so it is not an argument. */
    @Test
    public void anEscapedPercentSignIsNotAnArgument() {
        // :: Act
        final List<String> arguments = FormatArguments.of("battery at %d%%");

        // :: Verify
        assertWithMessage("arguments of a string ending in an escaped percent sign")
                .that(arguments)
                .containsExactly("1$d");
    }

    /**
     * The case that motivates the whole check: a translator writes a literal percent sign that
     * {@code String.format} cannot read. It must be reported, not silently dropped, because the
     * English string it replaces has nothing there.
     */
    @Test
    public void aPercentSignThatIsNotASpecifierIsReported() {
        // :: Act
        final List<String> arguments = FormatArguments.of("Low battery level (%)");

        // :: Verify
        assertWithMessage("arguments of a string with a bare percent sign")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** {@code %S} and {@code %s} consume the same argument, so a translation may use either. */
    @Test
    public void theCaseOfTheConversionDoesNotMatter() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%S");

        // :: Verify
        assertWithMessage("arguments of a string using an upper-case conversion")
                .that(arguments)
                .isEqualTo(FormatArguments.of("%s"));
    }

    /** A string with no formatting at all consumes nothing - the common case, and it must stay quiet. */
    @Test
    public void aStringWithoutFormattingHasNoArguments() {
        // :: Act
        final List<String> arguments = FormatArguments.of("Settings");

        // :: Verify
        assertWithMessage("arguments of a plain string").that(arguments).isEmpty();
    }

    /**
     * A space is a legal flag, but only for a conversion that prints a sign. On a string conversion
     * {@code String.format} throws, so the specifier has to be reported rather than read as a plain
     * {@code %s} that matches the English original.
     */
    @Test
    public void aSpaceFlagOnAStringConversionIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("% s");

        // :: Verify
        assertWithMessage("arguments of a space flag on a string conversion")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /**
     * The control for the test above: a space on a numeric conversion is ordinary, legal formatting
     * and must keep reading as the argument it consumes. Nine strings in the tree look like this.
     */
    @Test
    public void aSpaceFlagOnANumericConversionIsAnArgument() {
        // :: Act
        final List<String> arguments = FormatArguments.of("% d");

        // :: Verify
        assertWithMessage("arguments of a space flag on a numeric conversion")
                .that(arguments)
                .containsExactly("1$d");
    }

    /**
     * Only some conversions have an upper-case form. Lower-casing {@code %1$D} would turn a
     * specifier that throws into one that matches the English {@code %1$d} exactly.
     */
    @Test
    public void aConversionWithNoUpperCaseFormIsUnreadableInUpperCase() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%1$D");

        // :: Verify
        assertWithMessage("arguments of an upper-case conversion that has no upper-case form")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /**
     * {@code -} and {@code 0} are only legal with a width. Without one the string throws, so
     * {@code %-ban} - a translator writing "in %" in Hungarian - is not the argument {@code %b}.
     */
    @Test
    public void aFlagThatNeedsAWidthIsUnreadableWithoutOne() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%-ban");

        // :: Verify
        assertWithMessage("arguments of a left-justify flag with no width")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** The control for the test above: the same flag with a width is ordinary formatting. */
    @Test
    public void aFlagThatNeedsAWidthIsAnArgumentWithOne() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%-5s");

        // :: Verify
        assertWithMessage("arguments of a left-justify flag with a width")
                .that(arguments)
                .containsExactly("1$s");
    }

    /**
     * {@code %n} prints a line separator and consumes nothing. Counting it would also shift every
     * argument after it, so a translation using it would disagree with English twice over.
     */
    @Test
    public void aLineSeparatorConsumesNoArgumentAndDoesNotShiftTheOthers() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%s%n%s");

        // :: Verify
        assertWithMessage("arguments of a string with a line separator between them")
                .that(arguments)
                .containsExactly("1$s", "2$s");
    }

    /** {@code %1$%} prints a percent sign and consumes nothing, exactly as a bare {@code %%} does. */
    @Test
    public void anIndexedEscapedPercentSignIsNotAnArgument() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%1$%");

        // :: Verify
        assertWithMessage("arguments of an indexed escaped percent sign").that(arguments).isEmpty();
    }

    /** The {@code <} flag reuses the previous argument, so the two specifiers are one argument. */
    @Test
    public void thePreviousArgumentFlagConsumesNoNewArgument() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%s %<s");

        // :: Verify
        assertWithMessage("arguments of a string that reuses its previous argument")
                .that(arguments)
                .containsExactly("1$s");
    }

    /** Reusing an argument must not renumber the ones after it either. */
    @Test
    public void thePreviousArgumentFlagDoesNotShiftTheArgumentsAfterIt() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%s %<s %s");

        // :: Verify
        assertWithMessage("arguments of a string that reuses an argument and then takes a new one")
                .that(arguments)
                .containsExactly("1$s", "2$s");
    }

    /**
     * Naming the same argument twice is legal and consumes it once, so a translation that repeats a
     * value English states once is not a defect.
     */
    @Test
    public void repeatingAnExplicitIndexConsumesTheArgumentOnce() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%1$s ... %1$s");

        // :: Verify
        assertWithMessage("arguments of a string naming the same index twice")
                .that(arguments)
                .containsExactly("1$s");
    }

    /**
     * The same index read two ways is not the same argument: {@code String.format("%1$s %1$d", "a")}
     * throws, so a translation that does this to a string English formats as text has to be caught.
     */
    @Test
    public void theSameIndexWithTwoConversionsIsTwoArguments() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%1$s %1$d");

        // :: Verify
        assertWithMessage("arguments of a string reading one index two ways")
                .that(arguments)
                .containsExactly("1$d", "1$s");
    }

    /**
     * {@code <} overrides an index written in the same specifier, the way {@code Formatter} resolves
     * it. Reading {@code %2$<d} as a second argument would make this identical to {@code %1$d %2$d},
     * so a translation that breaks a two-argument string this way would pass unseen.
     */
    @Test
    public void thePreviousArgumentFlagWinsOverAnExplicitIndex() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%1$d %2$<d");

        // :: Verify
        assertWithMessage("arguments of a string whose second specifier reuses the first")
                .that(arguments)
                .containsExactly("1$d");
    }

    /** An explicit index is not counted, so the unindexed specifier after it still takes the first. */
    @Test
    public void anExplicitIndexDoesNotShiftTheArgumentsAfterIt() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%2$s %s");

        // :: Verify
        assertWithMessage("arguments of a string mixing an indexed and an unindexed specifier")
                .that(arguments)
                .containsExactly("1$s", "2$s");
    }

    /** There is no earlier argument to reuse, and {@code String.format} says so. */
    @Test
    public void aPreviousArgumentFlagWithNothingBeforeItIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%<s");

        // :: Verify
        assertWithMessage("arguments of a string that reuses an argument it never took")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** One {@code <} says which argument, a second one is a repeated flag and throws. */
    @Test
    public void aRepeatedPreviousArgumentFlagIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%s %<<s");

        // :: Verify
        assertWithMessage("arguments of a string repeating the reuse flag")
                .that(arguments)
                .containsExactly("1$s", FormatArguments.UNREADABLE);
    }

    /** A line separator takes no argument, but a width on one is still rejected by the formatter. */
    @Test
    public void aLineSeparatorWithAWidthIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%5n");

        // :: Verify
        assertWithMessage("arguments of a string giving a line separator a width")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** The same for the escaped percent sign: a width is legal on it, a sign flag is not. */
    @Test
    public void anEscapedPercentSignIsReadOnlyWithTheFlagsItAccepts() {
        // :: Act
        final List<String> withAWidth = FormatArguments.of("%5%");
        final List<String> withASignFlag = FormatArguments.of("%+%");

        // :: Verify
        assertWithMessage("arguments of an escaped percent sign given a width")
                .that(withAWidth)
                .isEmpty();
        assertWithMessage("arguments of an escaped percent sign given a sign flag")
                .that(withASignFlag)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** An index the formatter cannot hold in an {@code int} is a broken string, not a crash here. */
    @Test
    public void anIndexTooLargeToBeANumberIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%99999999999$s");

        // :: Verify
        assertWithMessage("arguments of a string naming an index that cannot exist")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** A width is legal however large it is, and deciding that must not try to render it. */
    @Test
    public void aWidthTooLargeToRenderIsStillAnArgument() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%2000000000d");

        // :: Verify
        assertWithMessage("arguments of a string with a width no text would ever fill")
                .that(arguments)
                .containsExactly("1$d");
    }

    /** A precision is not legal on an integer, so a translation that adds one has to be caught. */
    @Test
    public void aPrecisionOnAnIntegerIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%.2d");

        // :: Verify
        assertWithMessage("arguments of a string giving an integer a precision")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /** Flags that only change how a number is written keep it the same argument. */
    @Test
    public void flagsThatOnlyChangeTheNotationAreStillArguments() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%,d %(d %#x");

        // :: Verify
        assertWithMessage("arguments of a string using the grouping, parenthesis and alternate flags")
                .that(arguments)
                .containsExactly("1$d", "2$d", "3$x");
    }

    /**
     * The case of a date suffix picks the conversion rather than the casing of the output, so
     * {@code %tH} and {@code %th} are not the same argument while {@code %TH} and {@code %tH} are.
     */
    @Test
    public void theCaseOfADateSuffixIsKept() {
        // :: Act
        final List<String> anHour = FormatArguments.of("%tH");
        final List<String> aMonth = FormatArguments.of("%th");
        final List<String> anUpperCasedHour = FormatArguments.of("%TH");

        // :: Verify
        assertWithMessage("argument of an hour").that(anHour).containsExactly("1$tH");
        assertWithMessage("argument of a month name").that(aMonth).containsExactly("1$th");
        assertWithMessage("argument of an upper-cased hour").that(anUpperCasedHour).isEqualTo(anHour);
    }

    /** A date conversion that does not exist throws, and is not mistaken for a line separator. */
    @Test
    public void aDateSuffixThatDoesNotExistIsUnreadable() {
        // :: Act
        final List<String> arguments = FormatArguments.of("%tn");

        // :: Verify
        assertWithMessage("arguments of a string with an unknown date suffix")
                .that(arguments)
                .containsExactly(FormatArguments.UNREADABLE);
    }

    /**
     * A stray percent sign is reported even when a valid specifier follows it. This is the shape a
     * translator actually writes - a percentage in the text, and the argument further along - and
     * reading past it would make the string look exactly like the English one.
     */
    @Test
    public void aStrayPercentSignIsReportedAlongsideTheArgumentsAfterIt() {
        // :: Act
        final List<String> arguments = FormatArguments.of("5%. Value %d");

        // :: Verify
        assertWithMessage("arguments of a string with a percentage before its argument")
                .that(arguments)
                .containsExactly("1$d", FormatArguments.UNREADABLE);
    }
}
