package com.eveningoutpost.dexdrip.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the log calls in one source file that log text read from a translatable string resource.
 * <p>
 * This reads source as text, not as a syntax tree, so that it needs nothing beyond the JDK. Two
 * things keep that honest. Comments and the contents of literals are blanked out first, newlines
 * kept, so a commented-out call or a string mentioning {@code R.string.} is never mistaken for
 * code and line numbers stay true. And a hit needs a log call <i>and</i> a resource read together,
 * either in the call's own arguments or in a variable the call logs, which is narrow enough that
 * an ordinary log line does not trip it.
 * <p>
 * A variable is traced from wherever translated text is put in it - its declaration, or a later
 * {@code x = …} or {@code x += …} - to the end of the block that declares it: the method body for a
 * local, the class body for a field. Text built in another method and returned from it, or passed
 * to a helper that logs its argument, is not traced. Nor are {@code getQuantityString} and
 * {@code getStringArray}; nothing in the tree logs either today.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
final class TranslatedLogScanner {

    /** Every log method xDrip routes to logcat or the Event Log, under any qualifier. */
    private static final Pattern LOG_CALL =
            Pattern.compile("\\bLog\\s*\\.\\s*(e|d|i|w|v|wtf|ueh|uel)\\s*\\(");

    /** A read of a string resource, which returns the text in the user's language. Group 1 is its name. */
    private static final Pattern TRANSLATED =
            Pattern.compile("\\b(?:gs|getString|getText)\\s*\\(\\s*R\\s*\\.\\s*string\\s*\\.\\s*(\\w+)");

    /** A declaration whose value may be logged later. The optional part is a Kotlin type. */
    private static final Pattern DECLARATION =
            Pattern.compile("\\b(?:String|CharSequence|val|var)\\s+(\\w+)\\s*(?::\\s*\\w+\\??\\s*)?=(?!=)");

    /** Any assignment, a declaration's included, so that text put in a variable later is seen too. */
    private static final Pattern ASSIGNMENT = Pattern.compile("\\b(\\w+)\\s*\\+?=(?!=)");

    private TranslatedLogScanner() {
    }

    /** One log call that logs translated text. */
    static final class Finding {
        private final String path;
        private final int line;
        private final String method;
        private final String snippet;
        private final String through;

        private Finding(final String path, final int line, final String method, final String snippet,
                        final String through) {
            this.path = path;
            this.line = line;
            this.method = method;
            this.snippet = snippet;
            this.through = through;
        }

        String path() {
            return path;
        }

        /** The line the log call starts on, counted from 1. */
        int line() {
            return line;
        }

        /** The log method called: {@code e}, {@code ueh} and so on. */
        String method() {
            return method;
        }

        /** The call as written, whitespace collapsed, cut at 120 characters. */
        String snippet() {
            return snippet;
        }

        /**
         * For a call that logs a variable, which variable and the line where translated text was put
         * in it, since the call alone does not show it; empty for a resource read in the call itself.
         */
        String through() {
            return through;
        }

        @Override
        public String toString() {
            return path + ":" + line + "  Log." + method + "  " + snippet
                    + (through.isEmpty() ? "" : "  (" + through + ")");
        }
    }

    /** Where one log call sits: where it starts, and where its argument list opens and closes. */
    private static final class Call {
        private final int start;
        private final int argumentsStart;
        private final int argumentsEnd;
        private final String method;

        private Call(final int start, final int argumentsStart, final int argumentsEnd, final String method) {
            this.start = start;
            this.argumentsStart = argumentsStart;
            this.argumentsEnd = argumentsEnd;
            this.method = method;
        }
    }

    /**
     * Every log call in {@code source} that logs translated text, in the order they appear. A call
     * is reported once, however many reasons it has to be.
     */
    static List<Finding> scan(final String path, final String source) {
        return scan(path, source, Collections.emptySet());
    }

    /**
     * As {@link #scan(String, String)}, but a read of one of {@code untranslatable} does not count:
     * a string declared {@code translatable="false"}, such as {@code app_name}, is the same in every
     * language.
     */
    static List<Finding> scan(final String path, final String source, final Set<String> untranslatable) {
        final String code = blankCommentsAndLiterals(source);
        final List<Call> calls = logCalls(code);
        final Map<Integer, Finding> findings = new TreeMap<>();
        for (final Call call : calls) {
            if (readsTranslatedText(code.substring(call.argumentsStart, call.argumentsEnd), untranslatable)) {
                findings.put(call.start, finding(path, source, code, call, ""));
            }
        }
        final boolean kotlin = path.endsWith(".kt");
        final Matcher declaration = DECLARATION.matcher(code);
        while (declaration.find()) {
            traceVariable(path, source, code, calls, findings, untranslatable,
                    declaration.group(1), declaration.end(), kotlin);
        }
        final Matcher assignment = ASSIGNMENT.matcher(code);
        while (assignment.find()) {
            if (!endsWithTypeColon(code, assignment.start())) { // val x: String = … is a declaration
                traceVariable(path, source, code, calls, findings, untranslatable,
                        assignment.group(1), assignment.end(), kotlin);
            }
        }
        return new ArrayList<>(findings.values());
    }

    /** How many log calls the scanner sees in {@code source}, so a guard can count with its eyes. */
    static int logCallsIn(final String source) {
        return logCalls(blankCommentsAndLiterals(source)).size();
    }

    /** How many string-resource reads the scanner sees in {@code source}, for the same reason. */
    static int resourceReadsIn(final String source) {
        final Matcher read = TRANSLATED.matcher(blankCommentsAndLiterals(source));
        int reads = 0;
        while (read.find()) {
            reads++;
        }
        return reads;
    }

    /**
     * Reports every log call that logs {@code name} after the value starting at {@code valueStart},
     * if that value reads a string resource. The scope is the block of the variable's declaration,
     * so that text put in it inside an {@code if} is still followed to a log call after the
     * {@code if}; without a declaration in sight it is the block of the assignment.
     */
    private static void traceVariable(final String path, final String source, final String code,
                                      final List<Call> calls, final Map<Integer, Finding> findings,
                                      final Set<String> untranslatable, final String name,
                                      final int valueStart, final boolean kotlin) {
        final int valueEnd = endOfValue(code, valueStart, kotlin);
        if (!readsTranslatedText(code.substring(valueStart, valueEnd), untranslatable)) {
            return;
        }
        final int declared = lastDeclaration(code, name, valueStart);
        int scopeEnd = declared < 0 ? -1 : endOfEnclosingBlock(code, declared);
        if (scopeEnd < valueEnd) {
            scopeEnd = endOfEnclosingBlock(code, valueEnd);
        }
        final String through = name + " set from a string resource on line " + lineOf(code, valueStart);
        final Pattern use = Pattern.compile("\\b" + Pattern.quote(name) + "\\b");
        for (final Call call : calls) {
            if (call.start >= valueEnd && call.start < scopeEnd
                    && use.matcher(code.substring(call.argumentsStart, call.argumentsEnd)).find()) {
                findings.putIfAbsent(call.start, finding(path, source, code, call, through));
            }
        }
    }

    /** Whether {@code code} reads a string resource that is not one of {@code untranslatable}. */
    private static boolean readsTranslatedText(final String code, final Set<String> untranslatable) {
        final Matcher read = TRANSLATED.matcher(code);
        while (read.find()) {
            if (!untranslatable.contains(read.group(1))) {
                return true;
            }
        }
        return false;
    }

    /** Where {@code name} was last declared before {@code before}, or -1 if nowhere in the file. */
    private static int lastDeclaration(final String code, final String name, final int before) {
        final Matcher declaration = Pattern.compile(
                "\\b(?:String|CharSequence|val|var)\\s+" + Pattern.quote(name) + "\\b").matcher(code);
        int last = -1;
        while (declaration.find() && declaration.start() < before) {
            last = declaration.start();
        }
        return last;
    }

    /** Whether the code just before {@code at}, spaces aside, is the colon of a Kotlin type. */
    private static boolean endsWithTypeColon(final String code, final int at) {
        int i = at - 1;
        while (i >= 0 && Character.isWhitespace(code.charAt(i))) {
            i--;
        }
        return i >= 0 && code.charAt(i) == ':';
    }

    /**
     * {@code source} with comments and the contents of string, char and text-block literals
     * replaced by spaces. Newlines are kept, and so are the quotes, so every offset and every line
     * number means the same in both.
     */
    static String blankCommentsAndLiterals(final String source) {
        final StringBuilder code = new StringBuilder(source);
        final int length = source.length();
        int i = 0;
        while (i < length) {
            if (source.startsWith("//", i)) {
                while (i < length && source.charAt(i) != '\n') {
                    blank(code, i++);
                }
            } else if (source.startsWith("/*", i)) {
                final int end = source.indexOf("*/", i + 2);
                final int stop = end < 0 ? length : end + 2;
                while (i < stop) {
                    blank(code, i++);
                }
            } else if (source.startsWith("\"\"\"", i)) {
                final int end = source.indexOf("\"\"\"", i + 3);
                final int stop = end < 0 ? length : end;
                i += 3;
                while (i < stop) {
                    blank(code, i++);
                }
                i += 3;
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                final char quote = source.charAt(i++);
                while (i < length && source.charAt(i) != quote && source.charAt(i) != '\n') {
                    if (source.charAt(i) == '\\' && i + 1 < length) {
                        blank(code, i++);
                    }
                    blank(code, i++);
                }
                i++;
            } else {
                i++;
            }
        }
        return code.toString();
    }

    private static void blank(final StringBuilder code, final int i) {
        if (code.charAt(i) != '\n') {
            code.setCharAt(i, ' ');
        }
    }

    private static List<Call> logCalls(final String code) {
        final List<Call> calls = new ArrayList<>();
        final Matcher call = LOG_CALL.matcher(code);
        while (call.find()) {
            calls.add(new Call(call.start(), call.end(), closingParenthesis(code, call.end()), call.group(1)));
        }
        return calls;
    }

    /** The index of the parenthesis that closes the one just before {@code from}. */
    private static int closingParenthesis(final String code, final int from) {
        int depth = 1;
        for (int i = from; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')' && --depth == 0) {
                return i;
            }
        }
        return code.length();
    }

    /**
     * Where the value assigned from {@code from} ends: at the {@code ;} that ends the statement, or
     * in Kotlin at the end of the line, unless a bracket is still open; or at a closing bracket that
     * was opened before the value began, as in {@code foo(x = 1)}.
     */
    private static int endOfValue(final String code, final int from, final boolean kotlin) {
        int depth = 0;
        for (int i = from; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            } else if (depth == 0 && (c == ';' || kotlin && c == '\n')) {
                return i;
            }
        }
        return code.length();
    }

    /** The index of the brace that closes the block {@code from} sits in. */
    private static int endOfEnclosingBlock(final String code, final int from) {
        int depth = 0;
        for (int i = from; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth < 0) {
                return i;
            }
        }
        return code.length();
    }

    private static Finding finding(final String path, final String source, final String code, final Call call,
                                   final String through) {
        String snippet = source.substring(call.start, Math.min(call.argumentsEnd + 1, source.length()))
                .replaceAll("\\s+", " ");
        if (snippet.length() > 120) {
            snippet = snippet.substring(0, 117) + "...";
        }
        return new Finding(path, lineOf(code, call.start), call.method, snippet, through);
    }

    private static int lineOf(final String code, final int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (code.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
