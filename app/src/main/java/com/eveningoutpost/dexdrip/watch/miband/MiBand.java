package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;

import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NAME_2;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NAME_3;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NAME_3_1;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NAME_4;
import static com.eveningoutpost.dexdrip.watch.miband.Const.MIBAND_NOTIFY_TYPE_ALARM;
import static com.eveningoutpost.dexdrip.watch.miband.MiBandEntry.PREF_MIBAND_AUTH_KEY;
import static com.eveningoutpost.dexdrip.watch.miband.MiBandEntry.PREF_MIBAND_MAC;

/**
 * Jamorham
 * <p>
 * Lefun Lightweight logic class
 */

public class MiBand {

    public enum MiBandType {
        MI_BAND2(MIBAND_NAME_2),
        MI_BAND3(MIBAND_NAME_3),
        MI_BAND3_1(MIBAND_NAME_3_1),
        MI_BAND4(MIBAND_NAME_4),
        UNKNOWN("");

        private final String text;

        /**
         * @param text
         */
        MiBandType(final String text) {
            this.text = text;
        }

        /* (non-Javadoc)
         * @see java.lang.Enum#toString()
         */
        @Override
        public String toString() {
            return text;
        }

        public static MiBandType fromString(String text) {
            for (MiBandType b : MiBandType.values()) {
                if (b.text.equalsIgnoreCase(text)) {
                    return b;
                }
            }
            return UNKNOWN;
        }
    }

    private static final String PREF_MIBAND_AUTH_MAC = "miband_auth_mac";
    private static final String PREF_MIBAND_PERSISTANT_AUTH_KEY = "miband_persist_authkey";
    private static final String PREF_MIBAND_MODEL = "miband_model_";
    private static final String PREF_MIBAND_VERSION = "miband_version_";

    public static MiBandType getMibandType() {
        return MiBandType.fromString(getModel());
    }

    public static boolean isAuthenticated() {
        return MiBand.getPersistentAuthMac().isEmpty() ? false : true;
    }

    public static void sendCall(final String message_type, final String message) {
        Inevitable.task("miband-send-alert-debounce", 3000, () -> JoH.startService(MiBandService.class, "function", "message",
                "message", message,
                "message_type", message_type));
    }

    // convert multi-line text to string for display constraints
    public static void sendAlert(String title, String message, int defaultSnoozle) {
        Inevitable.task("miband-send-alert-debounce", 100, () -> JoH.startService(MiBandService.class, "function", "message",
                "message", message,
                "title", title,
                "message_type", MIBAND_NOTIFY_TYPE_ALARM,
                "default_snoozle", Integer.toString(defaultSnoozle)));
    }

    public static String getMac() {
        return Pref.getString(PREF_MIBAND_MAC, "");
    }

    static void setMac(final String mac) {
        Pref.setString(PREF_MIBAND_MAC, mac);
        MiBandEntry.sendPrefIntent(MiBandService.MIBAND_INTEND_STATES.UPDATE_PREF_DATA, 0, "");
    }

    static void setAuthKey(final String key) {
        Pref.setString(PREF_MIBAND_AUTH_KEY, key.toLowerCase());
        MiBandEntry.sendPrefIntent(MiBandService.MIBAND_INTEND_STATES.UPDATE_PREF_DATA, 0, "");
    }

    public static String getAuthKey() {
        return Pref.getString(PREF_MIBAND_AUTH_KEY, "");
    }

    public static String getPersistentAuthMac() {
        return PersistentStore.getString(PREF_MIBAND_AUTH_MAC);
    }

    static void setPersistentAuthMac(final String mac) {
        if (mac.isEmpty()) {
            String authMac = getPersistentAuthMac();
            setVersion("", authMac);
            setModel("", authMac);
            setPersistentAuthKey("", authMac);
            PersistentStore.removeItem(PREF_MIBAND_AUTH_MAC);
            return;
        }
        PersistentStore.setString(PREF_MIBAND_AUTH_MAC, mac);
    }


    static String getPersistentAuthKey() {
        final String mac = getPersistentAuthMac();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac);
        }
        return "";
    }

    static void setPersistentAuthKey(final String key, String mac) {
        if (key.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_PERSISTANT_AUTH_KEY + mac, key.toLowerCase());
        }
    }

    static String getModel() {
        final String mac = getMac();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_MODEL + mac);
        }
        return "";
    }

    static void setModel(final String model, String mac) {
        if (mac.isEmpty()) mac = getMac();
        if (model.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_MODEL + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_MODEL + mac, model);
        }
    }

    static String getVersion() {
        final String mac = getMac();
        if (!mac.isEmpty()) {
            return PersistentStore.getString(PREF_MIBAND_VERSION + mac);
        }
        return "";
    }

    static void setVersion(final String version, String mac) {
        if (mac.isEmpty()) mac = getMac();
        if (version.isEmpty()) {
            PersistentStore.removeItem(PREF_MIBAND_MODEL + mac);
            return;
        }
        if (!mac.isEmpty()) {
            PersistentStore.setString(PREF_MIBAND_VERSION + mac, version);
        }
    }


}
