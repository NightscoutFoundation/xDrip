package com.eveningoutpost.dexdrip;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;

/**
 * Stands in for the app context so that text read from a string resource can be told apart.
 * <p>
 * Unit tests run on the {@code fast} flavor, whose {@code resConfigs "en"} leaves only English in
 * the test APK, so a test cannot switch the language and watch the text change. Instead, text read
 * with {@code getString} or {@code getText} - directly, or through {@code xdrip.gs} - comes back
 * between {@link #OPEN} and {@link #CLOSE}. Text that carries the marks would be translated for a
 * user in another language; text without them would not. Plurals, arrays and everything else go
 * to the real context underneath unmarked, and so does code that held on to a context or a
 * {@code Resources} from before this one was installed.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class TranslationMarkingContext extends ContextWrapper {

    public static final String OPEN = "«";
    public static final String CLOSE = "»";

    private final Resources marking;

    @SuppressWarnings("deprecation") // the only public way to build Resources around real assets
    public TranslationMarkingContext(final Context base) {
        super(base);
        final Resources real = base.getResources();
        marking = new Resources(real.getAssets(), real.getDisplayMetrics(), real.getConfiguration()) {
            @Override
            public String getString(final int id) {
                return mark(real.getString(id));
            }

            @Override
            public String getString(final int id, final Object... formatArgs) {
                return mark(real.getString(id, formatArgs));
            }

            @Override
            public CharSequence getText(final int id) {
                return mark(real.getText(id).toString());
            }
        };
    }

    /** {@code text} as this context returns it when it is read from a resource. */
    public static String mark(final String text) {
        return OPEN + text + CLOSE;
    }

    @Override
    public Resources getResources() {
        return marking;
    }
}
