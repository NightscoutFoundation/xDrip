package lwld.glucose.profile.util;

import lombok.Setter;
import lwld.glucose.profile.iface.ILog;

/**
 * JamOrHam
 * <p>
 * Pluggable log implementation
 */
public class Log {

    @Setter
    public static volatile ILog logger = new ILog() {
        @Override
        public int d(String tag, String msg) {
            android.util.Log.d(tag, msg);
            return 1;
        }

        @Override
        public int e(String tag, String msg) {
            android.util.Log.e(tag, msg);
            return 1;
        }

        @Override
        public int i(String tag, String msg) {
            android.util.Log.i(tag, msg);
            return 1;
        }

        @Override
        public int wtf(String tag, String msg) {
            android.util.Log.wtf(tag, msg);
            return 1;
        }
    };

    public static int d(String tag, String msg) {
        return logger.d(tag, msg);
    }

    public static int i(String tag, String msg) {
        return logger.i(tag, msg);
    }

    public static int e(String tag, String msg) {
        return logger.e(tag, msg);
    }

    public static int wtf(String tag, String msg) {
        return logger.wtf(tag, msg);
    }
}
