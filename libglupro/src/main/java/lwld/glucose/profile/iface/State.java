package lwld.glucose.profile.iface;

/**
 * JamOrHam
 * <p>
 * Collector states and classifier
 */

public enum State {
    INIT,
    SLEEPING,
    SCANNING,
    SCANNING_ERROR,
    INSUFFICIENT_PERMISSIONS,
    BLUETOOTH_DISABLED,
    SCAN_STOPPED,
    SETUP_FAILED,
    CONNECTING,
    CONNECTED,
    CONFIGURING,
    CONFIGURED,
    READY,
    CONNECT_FAILED,
    BONDING,
    BONDED,
    BONDING_FAILED,
    DISCONNECTED,
    SHUTDOWN,
    UNKNOWN;


    public static State getState(String stateName) {
        if (stateName == null) {
            return UNKNOWN;
        }
        try {
            return State.valueOf(stateName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

    public static boolean isCriticalError(State state) {
        switch (state) {
            case BLUETOOTH_DISABLED:
            case INSUFFICIENT_PERMISSIONS:
            case SETUP_FAILED:
                return true;
            default:
                return false;
        }
    }

    public static boolean isError(State state) {
        switch (state) {
            case BONDING_FAILED:
            case CONNECT_FAILED:
            case SCANNING_ERROR:
                return true;
            default:
                return false;
        }
    }

    public static boolean isWarning(State state) {
        switch (state) {
            case CONNECTING:
            case CONNECTED:
            case SCANNING:
                return true;
            default:
                return false;
        }
    }

    public static boolean isGood(State state) {
        switch (state) {
            case READY:
                return true;
            default:
                return false;
        }
    }
}
