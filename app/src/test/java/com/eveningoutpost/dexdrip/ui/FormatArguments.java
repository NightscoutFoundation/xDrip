package com.eveningoutpost.dexdrip.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The format arguments of a string, read the way {@link java.util.Formatter} reads them.
 * <p>
 * A translated string has to consume the same arguments as the English one it replaces, because the
 * call site passes the same values in either language. When it does not, the locale that carries the
 * broken string is the only one that fails - with a wrong text at best, and an exception out of
 * {@code String.format} at worst. Comparing what this returns for the two strings is how that is
 * detected without running every screen in every language.
 * <p>
 * Whether a specifier is legal is not decided here. Each one is handed back to {@code String.format}
 * with an argument of the type its conversion asks for, and a specifier that throws is reported as
 * {@link #UNREADABLE}. That is the whole point: the rules for which flag may meet which conversion
 * fill a table in the {@code Formatter} javadoc, and a copy of that table here would be a second
 * source of truth that drifts. {@code % s} throws while {@code % d} prints a space and a number, and
 * no rule written in this file has to know why.
 * <p>
 * Three deliberate choices, all so that a legitimate translation is never reported:
 * <ul>
 *   <li>The list is <b>sorted</b>, so a translation that puts {@code %2$s} before {@code %1$s} to
 *       follow its own word order still matches the English original.</li>
 *   <li>The conversion is <b>lower-cased</b> when it has a legal upper-case form, so {@code %S} and
 *       {@code %s} match: they consume the same argument and differ only in what they print. An
 *       upper-case form that does not exist - {@code %D} - throws, and is reported instead. The
 *       suffix of a date conversion is left alone, because there the case selects the conversion
 *       rather than the casing of its output: {@code %tH} is an hour and {@code %th} a month.</li>
 *   <li>Arguments are <b>distinct</b>: naming one twice, by repeating {@code %1$s} or by reusing it
 *       with {@code %&lt;s}, consumes it once. An index read two ways ({@code %1$s %1$d}) stays two
 *       entries, because that combination throws on all but a numeric value.</li>
 * </ul>
 * A percent sign that {@code Formatter} cannot read as a specifier - a trailing {@code %}, or the
 * {@code (%)} that a translator writes to mean the unit - is reported as {@link #UNREADABLE} rather
 * than dropped. Those are the ones that throw when the string is eventually formatted.
 *
 * @author Asbjørn Aarrestad - 2026.09
 */
public final class FormatArguments {

    /**
     * The specifier grammar from {@code java.util.Formatter}, so that this reads a string the same
     * way the class that ultimately formats it does.
     */
    private static final Pattern SPECIFIER =
            Pattern.compile("%(?:(\\d+)\\$)?([-#+ 0,(<]*)(\\d+)?(?:\\.(\\d+))?([tT])?([a-zA-Z%])");

    /** Reported for a percent sign that is not a specifier and not an escaped {@code %%}. */
    public static final String UNREADABLE = "?";

    /** Prints a line separator and consumes nothing, so it neither is nor shifts an argument. */
    private static final char LINE_SEPARATOR = 'n';

    /** Prints a percent sign and consumes nothing, with or without an index. */
    private static final String ESCAPED_PERCENT = "%";

    /** Reuses the argument the previous specifier consumed, instead of taking the next one. */
    private static final char PREVIOUS_ARGUMENT = '<';

    /** An index of more digits than this cannot be an {@code int}, which {@code Formatter} requires. */
    private static final int INDEX_DIGITS_THAT_FIT = 9;

    /** Enough of a width or a precision to decide whether it is legal, without rendering it. */
    private static final int DIGITS_THAT_RENDER = 2;

    private FormatArguments() {
    }

    /**
     * The arguments {@code text} consumes, as distinct {@code index$conversion} entries, sorted.
     * An argument written without an index gets the position it would be given at runtime.
     */
    public static List<String> of(final String text) {
        final TreeSet<String> arguments = new TreeSet<>();
        final Matcher matcher = SPECIFIER.matcher(text);
        int automaticIndex = 0;
        int previousIndex = 0;
        int at = 0;
        while ((at = text.indexOf('%', at)) >= 0) {
            if (!matcher.find(at) || matcher.start() != at) {
                arguments.add(UNREADABLE);
                at++;
                continue;
            }
            at = matcher.end();

            final String flags = matcher.group(2);
            final String dateTime = matcher.group(5) == null ? "" : matcher.group(5);
            if (consumesNoArgument(dateTime, matcher.group(6))) {
                if (!isLegal(matcher)) {
                    arguments.add(UNREADABLE);
                }
                continue;
            }

            final int index = indexOf(matcher.group(1), flags, previousIndex, automaticIndex);
            if (index == 0 || !isLegal(matcher)) {
                arguments.add(UNREADABLE);
                continue;
            }
            if (matcher.group(1) == null && !reusesPreviousArgument(flags)) {
                automaticIndex = index;
            }
            previousIndex = index;
            arguments.add(index + "$" + conversionOf(dateTime, matcher.group(6)));
        }
        return new ArrayList<>(arguments);
    }

    /**
     * Whether this prints without taking a value: a line separator or an escaped percent sign. Both
     * still have to be legal - {@code %5n} and {@code %+%} throw - so they are checked, not trusted.
     */
    private static boolean consumesNoArgument(final String dateTime, final String conversion) {
        return dateTime.isEmpty()
                && (conversion.charAt(0) == LINE_SEPARATOR || ESCAPED_PERCENT.equals(conversion));
    }

    /** The conversion as it identifies an argument, with only the case that changes nothing removed. */
    private static String conversionOf(final String dateTime, final String conversion) {
        return dateTime.isEmpty()
                ? conversion.toLowerCase(Locale.ROOT)
                : Character.toLowerCase(dateTime.charAt(0)) + conversion;
    }

    private static boolean reusesPreviousArgument(final String flags) {
        return flags.indexOf(PREVIOUS_ARGUMENT) >= 0;
    }

    /**
     * The argument this specifier consumes, or {@code 0} when it names one that cannot exist - a
     * {@code %<s} with nothing before it throws for want of an argument to reuse, and so does an
     * index of zero or one too large to be an {@code int}.
     * <p>
     * The {@code <} flag wins over an explicit index, the way {@code Formatter} resolves it: the
     * index is read first and then overwritten. {@code %1$d %2$<d} consumes the first argument
     * twice, so reading it as a second one would call a broken translation identical to English.
     */
    private static int indexOf(final String explicit, final String flags, final int previousIndex,
                               final int automaticIndex) {
        if (reusesPreviousArgument(flags)) {
            return previousIndex;
        }
        if (explicit != null) {
            return explicit.length() > INDEX_DIGITS_THAT_FIT ? 0 : Integer.parseInt(explicit);
        }
        return automaticIndex + 1;
    }

    /**
     * Whether {@code Formatter} accepts this specifier, asked by formatting it on its own. The index
     * is left out: it only decides which argument is consumed, which is settled before this is
     * called, and it would throw here for want of the earlier arguments it names.
     * <p>
     * One {@code <} is left out for the same reason, but only when the specifier takes a value: on
     * {@code %n} and {@code %%} the flag is simply illegal. A second {@code <} is kept, because a
     * repeated flag is what {@code DuplicateFormatFlagsException} exists for.
     */
    private static boolean isLegal(final Matcher specifier) {
        final String dateTime = specifier.group(5) == null ? "" : specifier.group(5);
        final String conversion = specifier.group(6);
        final Object[] value = valuesFor(dateTime, conversion);
        if (value == null) {
            return false;
        }
        final String flags = value.length == 0
                ? specifier.group(2)
                : specifier.group(2).replaceFirst(String.valueOf(PREVIOUS_ARGUMENT), "");
        final String width = shortened(specifier.group(3));
        final String precision = specifier.group(4) == null ? "" : "." + shortened(specifier.group(4));
        try {
            String.format(Locale.ROOT, "%" + flags + width + precision + dateTime + conversion, value);
            return true;
        } catch (RuntimeException notAcceptedByTheFormatter) {
            return false;
        }
    }

    /**
     * A width or precision short enough to render. Whether one is legal never depends on how large
     * it is, only on whether it is there at all - and {@code %2000000000d} really does allocate two
     * billion characters when it is formatted, which is an {@code OutOfMemoryError} rather than a
     * reportable difference.
     */
    private static String shortened(final String number) {
        if (number == null) {
            return "";
        }
        return number.length() <= DIGITS_THAT_RENDER ? number : number.substring(0, DIGITS_THAT_RENDER);
    }

    /**
     * Arguments of the types the conversion asks for - none for the two that print without taking
     * one - or {@code null} for no such conversion.
     */
    private static Object[] valuesFor(final String dateTime, final String conversion) {
        if (!dateTime.isEmpty()) {
            return new Object[]{new Date(0)};
        }
        if (conversion.charAt(0) == LINE_SEPARATOR || ESCAPED_PERCENT.equals(conversion)) {
            return new Object[0];
        }
        switch (Character.toLowerCase(conversion.charAt(0))) {
            case 'b':
            case 'h':
            case 's':
                return new Object[]{"text"};
            case 'c':
                return new Object[]{'c'};
            case 'd':
            case 'o':
            case 'x':
                return new Object[]{1};
            case 'a':
            case 'e':
            case 'f':
            case 'g':
                return new Object[]{1.5d};
            default:
                return null;
        }
    }
}
