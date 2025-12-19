package lwld.glucose.profile.iface;

/**
 * JamOrHam
 * <p>
 * Logging interface
 */
public interface ILog {

    int d(String tag, String msg);

    int e(String tag, String msg);

    int i(String tag, String msg);

    int wtf(String tag, String msg);

}
