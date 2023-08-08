package jamorham.keks.util;

/**
 * JamOrHam
 */

public class Log {

    public static void d(final String TAG, final String msg) {
        System.out.println(TAG + " :: " + msg);
    }

    public static void l(final String msg) {
        d("KEKS-Plugin", msg);
    }
}
