package com.eveningoutpost.dexdrip.utils;

import android.content.Context;
import android.content.res.Configuration;

import androidx.annotation.StringRes;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import lombok.val;

/**
 * JamOrHam
 *
 * Matches text against a string resource, in any translated language.
 */
public class Localized {

    private static final String TAG = "Localized";

    // where a value is substituted into a format string - %s, %1$s, %.1f - or an escaped percent
    private static final Pattern FORMAT_SPECIFIER =
            Pattern.compile("%%|%(?:\\d+\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?[a-zA-Z]");
    private static final String WILDCARD = "(.+?)";

    private static final Map<String, List<Pattern>> cache = new HashMap<>();

    /**
     * Is this text just the string on its own?
     */
    public static boolean matches(@StringRes final int resId, final String text) {
        return matches(resId, text, true);
    }

    /**
     * Is the string somewhere inside this text?
     */
    public static boolean appearsIn(@StringRes final int resId, final String text) {
        return matches(resId, text, false);
    }

    private static boolean matches(final int resId, final String text, final boolean whole) {
        if (text == null || text.trim().isEmpty()) return false;
        val candidate = text.trim();
        for (val pattern : patterns(resId)) {
            val matcher = pattern.matcher(candidate);
            if (whole ? matcher.matches() : matcher.find()) return true;
        }
        return false;
    }

    private static synchronized List<Pattern> patterns(final int resId) {
        val context = xdrip.getAppContext();
        val ours = context.getResources().getConfiguration().getLocales().get(0);
        val key = resId + " " + ours.toLanguageTag();
        List<Pattern> patterns = cache.get(key);
        if (patterns == null) {
            val expressions = new LinkedHashSet<String>();
            for (val locale : localesToTry(ours)) {
                try {
                    val expression = expressionFor(stringIn(context, locale, resId));
                    if (expression != null) expressions.add(expression);
                } catch (Exception e) {
                    Log.e(TAG, "Could not read string in " + locale + ": " + e);
                }
            }
            patterns = new ArrayList<>();
            for (val expression : expressions) {
                patterns.add(Pattern.compile(expression, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
            cache.put(key, patterns);
            Log.d(TAG, "Matching string " + resId + " against " + patterns.size() + " wordings");
        }
        return patterns;
    }

    private static List<Locale> localesToTry(final Locale ours) {
        val locales = new LinkedHashSet<Locale>();
        locales.add(ours);
        locales.add(Locale.ROOT); // resolves to the untranslated string
        val resources = xdrip.getAppContext().getResources();
        for (val language : resources.getStringArray(R.array.LocaleChoicesValues)) {
            locales.add(Locale.forLanguageTag(language));
        }
        val assetLocales = resources.getAssets().getLocales();
        if (assetLocales != null) {
            for (val locale : assetLocales) {
                locales.add(Locale.forLanguageTag(locale));
            }
        }
        return new ArrayList<>(locales);
    }

    private static String stringIn(final Context context, final Locale locale, final int resId) {
        val configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration).getString(resId);
    }

    /**
     * Builds an expression which matches text written from this string, treating anything
     * substituted into it as a wildcard.
     */
    private static String expressionFor(final String string) {
        val expression = new StringBuilder();
        val specifier = FORMAT_SPECIFIER.matcher(string);
        boolean hasWording = false;
        int position = 0;
        while (specifier.find()) {
            hasWording |= quote(expression, string.substring(position, specifier.start()));
            if (specifier.group().equals("%%")) {
                hasWording |= quote(expression, "%"); // an escaped percent is part of the wording
            } else {
                expression.append(WILDCARD);
            }
            position = specifier.end();
        }
        hasWording |= quote(expression, string.substring(position));
        return hasWording ? expression.toString() : null;
    }

    private static boolean quote(final StringBuilder expression, final String wording) {
        if (wording.isEmpty()) return false;
        expression.append(Pattern.quote(wording));
        return !wording.trim().isEmpty(); // ignore empty strings
    }

}
