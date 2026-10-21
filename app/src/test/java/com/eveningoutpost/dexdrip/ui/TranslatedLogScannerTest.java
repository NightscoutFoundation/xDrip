package com.eveningoutpost.dexdrip.ui;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tests for {@link TranslatedLogScanner}, which decides what {@link NoTranslatedStringsInLogsTest}
 * gets to see at all.
 * <p>
 * The tree test can only say that nothing was found, and a scanner that found nothing anywhere
 * would say the same. So the scanner is tested here against small sources whose answer is known:
 * every shape a hit takes, and every look-alike that must not be one.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class TranslatedLogScannerTest {

    // ================================================== Direct hits ==================================================

    /** The plain case: a resource read inside the arguments of a log call. */
    @Test
    public void aResourceReadInsideALogCallIsFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    UserError.Log.e(TAG, xdrip.gs(R.string.some_text));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:2  Log.e");
    }

    /** A call written over several lines is followed to its closing parenthesis. */
    @Test
    public void aLogCallOverSeveralLinesIsFoundOnTheLineItStarts() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.i(TAG,",
                "            \"prefix \"",
                "                    + getString(R.string.some_text));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:2  Log.i");
    }

    /** Every way the code base reads a resource, with any receiver in front of it. */
    @Test
    public void everyFormOfResourceReadIsFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.d(TAG, xdrip.gs(R.string.a));",
                "    Log.d(TAG, context.getString(R.string.b, x));",
                "    Log.d(TAG, getResources().getText( R.string.c ).toString());",
                "    Log.d(TAG, xdrip.getAppContext().getString(R . string . d));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings))
                .containsExactly("A.java:2  Log.d", "A.java:3  Log.d", "A.java:4  Log.d", "A.java:5  Log.d")
                .inOrder();
    }

    /** The Event Log methods and the platform logger are log calls like any other. */
    @Test
    public void everyLogMethodIsCovered() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    UserError.Log.ueh(TAG, xdrip.gs(R.string.a));",
                "    UserError.Log.uel(TAG, xdrip.gs(R.string.a));",
                "    android.util.Log.w(TAG, xdrip.gs(R.string.a));",
                "    Log.v(TAG, xdrip.gs(R.string.a));",
                "    Log.wtf(TAG, xdrip.gs(R.string.a));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings))
                .containsExactly("A.java:2  Log.ueh", "A.java:3  Log.uel", "A.java:4  Log.w",
                        "A.java:5  Log.v", "A.java:6  Log.wtf")
                .inOrder();
    }

    /** What the failure message shows: the call as written, on one line. */
    @Test
    public void aFindingCarriesTheCallAsWritten() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.e(TAG,",
                "            xdrip.gs(R.string.a));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(findings).hasSize(1);
        assertWithMessage("snippet").that(findings.get(0).snippet())
                .isEqualTo("Log.e(TAG, xdrip.gs(R.string.a))");
        assertWithMessage("report line").that(findings.get(0).toString())
                .isEqualTo("A.java:2  Log.e  Log.e(TAG, xdrip.gs(R.string.a))");
    }

    // ============================================== Through a variable ===============================================

    /** Text put in a local first and logged a few lines later is still the translated text. */
    @Test
    public void aLocalHoldingTranslatedTextIsFoundWhereItIsLogged() {
        // :: Setup
        final String source = source(
                "boolean f() {",
                "    final String invalid = xdrip.gs(R.string.a);",
                "    msg(invalid);",
                "    UserError.Log.e(TAG, invalid);",
                "    return false;",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:4  Log.e");
    }

    /** The declaration forms in use: a typed one, Lombok's {@code val}, and Kotlin's {@code val}. */
    @Test
    public void everyDeclarationFormIsTraced() {
        // :: Setup
        final String java = source(
                "void f() {",
                "    CharSequence a = getText(R.string.a);",
                "    val b = xdrip.gs(R.string.b);",
                "    Log.d(TAG, \"a=\" + a + \" b=\" + b);",
                "}");
        final String kotlin = source(
                "fun f() {",
                "    val c: String = xdrip.gs(R.string.c)",
                "    Log.d(TAG, c)",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> javaFindings = TranslatedLogScanner.scan("A.java", java);
        final List<TranslatedLogScanner.Finding> kotlinFindings = TranslatedLogScanner.scan("B.kt", kotlin);

        // :: Verify
        assertWithMessage("Java findings").that(lines(javaFindings)).containsExactly("A.java:4  Log.d");
        assertWithMessage("Kotlin findings").that(lines(kotlinFindings)).containsExactly("B.kt:3  Log.d");
    }

    /** A call that logs translated text twice over is still one call, and is reported once. */
    @Test
    public void aCallWithSeveralReasonsIsReportedOnce() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    final String a = xdrip.gs(R.string.a);",
                "    Log.d(TAG, a + xdrip.gs(R.string.b));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:3  Log.d");
    }

    /**
     * Text put in a variable after it was declared, inside an {@code if}, is followed out of the
     * {@code if} to the end of the block that declares the variable.
     */
    @Test
    public void aVariableAssignedTranslatedTextLaterIsFoundWhereItIsLogged() {
        // :: Setup
        final String source = source(
                "String f(double value) {",
                "    String text = \"\";",
                "    if (value >= 400) {",
                "        text = xdrip.getAppContext().getString(R.string.high);",
                "    } else {",
                "        text = format(value);",
                "    }",
                "    Log.d(TAG, \"calculated text: \" + text);",
                "    return text;",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:8  Log.d");
    }

    /** Appending translated text to a variable puts translated text in it, as assigning does. */
    @Test
    public void translatedTextAppendedToAVariableIsFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    String text = \"value\";",
                "    text += xdrip.gs(R.string.a);",
                "    Log.d(TAG, text);",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:4  Log.d");
    }

    /** A call that logs a variable does not show the resource, so the report names where it came from. */
    @Test
    public void aFindingThroughAVariableSaysWhereTheTextCameFrom() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    String text = \"\";",
                "    text = xdrip.gs(R.string.a);",
                "    Log.d(TAG, text);",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(findings).hasSize(1);
        assertWithMessage("report line").that(findings.get(0).toString())
                .isEqualTo("A.java:4  Log.d  Log.d(TAG, text)  (text set from a string resource on line 3)");
    }

    // ==================================================== No hit =====================================================

    /** A call in a comment is not code, whichever kind of comment it is in. */
    @Test
    public void aCommentedOutCallIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    // Log.e(TAG, xdrip.gs(R.string.a));",
                "    /* Log.e(TAG, xdrip.gs(R.string.a)); */",
                "    /*",
                "     * Log.e(TAG, xdrip.gs(R.string.a));",
                "     */",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** Text that only mentions a resource is English text, not a read of one. */
    @Test
    public void aResourceNameInsideAStringIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.e(TAG, \"could not gs(R.string.a) \\\" getString(R.string.b)\");",
                "    Log.e(TAG, \"\"\"",
                "        getString(R.string.c)",
                "        \"\"\");",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** Preferences and bundles are read with {@code getString} too, but by key, and keys are English. */
    @Test
    public void aGetStringThatIsNotAResourceIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.d(TAG, Pref.getString(\"units\", \"mgdl\"));",
                "    Log.d(TAG, bundle.getString(\"x\") + prefs.getString(KEY, null));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** A local belongs to its own method, so the same name logged in the next method is another value. */
    @Test
    public void aLocalIsNotTracedOutOfItsMethod() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    final String text = xdrip.gs(R.string.a);",
                "    toast(text);",
                "}",
                "void g(String text) {",
                "    Log.d(TAG, text);",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** Translated text is meant for the user; showing it is the point, and only logging it is wrong. */
    @Test
    public void translatedTextThatIsShownButNotLoggedIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    final String text = xdrip.gs(R.string.a);",
                "    JoH.static_toast_long(text);",
                "    msg(xdrip.gs(R.string.b));",
                "    Log.d(TAG, \"toast shown\");",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** A name that only contains the variable's name is another variable. */
    @Test
    public void aLongerNameContainingTheVariableIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    final String msg = xdrip.gs(R.string.a);",
                "    Log.d(TAG, msgCount + \" \" + lastmsg);",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).isEmpty();
    }

    /** A string declared {@code translatable="false"} reads the same in every language. */
    @Test
    public void anUntranslatableStringIsNotFound() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.d(TAG, context.getString(R.string.app_name));",
                "    final String name = context.getString(R.string.app_name);",
                "    Log.d(TAG, name);",
                "    Log.d(TAG, context.getString(R.string.other));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings =
                TranslatedLogScanner.scan("A.java", source, Collections.singleton("app_name"));

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:5  Log.d");
    }

    // ================================================= Line numbers ==================================================

    /** Blanking comments and literals must not move anything, or every reported line would be wrong. */
    @Test
    public void lineNumbersSurviveCommentsAndLiteralsAbove() {
        // :: Setup
        final String source = source(
                "/*",
                " * a block comment",
                " */",
                "void f() {",
                "    String s = \"text with // and /* in it\";",
                "    char c = '\"';",
                "    Log.e(TAG, xdrip.gs(R.string.a));",
                "}");

        // :: Act
        final List<TranslatedLogScanner.Finding> findings = TranslatedLogScanner.scan("A.java", source);

        // :: Verify
        assertWithMessage("findings").that(lines(findings)).containsExactly("A.java:7  Log.e");
    }

    /** The blanked text keeps every offset, so a position in one is the same position in the other. */
    @Test
    public void blankingKeepsLengthNewlinesAndQuotes() {
        // :: Setup
        final String source = source(
                "a(\"R.string.x\"); // gs(R.string.y)",
                "/* two",
                "   lines */ b('c');");

        // :: Act
        final String blanked = TranslatedLogScanner.blankCommentsAndLiterals(source);

        // :: Verify
        assertWithMessage("blanked source").that(blanked)
                .isEqualTo(source(
                        "a(\"          \");" + spaces(18),
                        "      ",
                        "            b(' ');"));
    }

    // ==================================================== Counts =====================================================

    /**
     * The tree test's guard counts with these, so they count what the scanner sees: code, not a
     * call or a resource named in a comment or a string.
     */
    @Test
    public void countsSeeOnlyCode() {
        // :: Setup
        final String source = source(
                "void f() {",
                "    Log.d(TAG, xdrip.gs(R.string.a));",
                "    Log.e(TAG, \"no resource\");",
                "    // Log.d(TAG, xdrip.gs(R.string.b));",
                "    toast(\"Log.d(TAG, gs(R.string.c))\");",
                "    final String d = getString(R.string.d);",
                "}");

        // :: Act
        final int logCalls = TranslatedLogScanner.logCallsIn(source);
        final int resourceReads = TranslatedLogScanner.resourceReadsIn(source);

        // :: Verify
        assertWithMessage("log calls").that(logCalls).isEqualTo(2);
        assertWithMessage("resource reads").that(resourceReads).isEqualTo(2);
    }

    private static String spaces(final int count) {
        return new String(new char[count]).replace('\0', ' ');
    }

    private static String source(final String... lines) {
        return String.join("\n", lines) + "\n";
    }

    /** Each finding as {@code path:line  Log.method}, which is what the assertions care about. */
    private static List<String> lines(final List<TranslatedLogScanner.Finding> findings) {
        final List<String> lines = new ArrayList<>();
        for (final TranslatedLogScanner.Finding finding : findings) {
            lines.add(finding.path() + ":" + finding.line() + "  Log." + finding.method());
        }
        return lines;
    }
}
