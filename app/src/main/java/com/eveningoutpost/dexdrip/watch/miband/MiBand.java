package com.eveningoutpost.dexdrip.watch.miband;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
/**
 * Jamorham
 *
 * Lefun Lightweight logic class
 */

public class MiBand {

    private static final String PREF_MIBAND_MAC = "miband_mac";
    private static final String PREF_MIBAND_AUTH_MAC = "miband_auth_mac";
    private static final String PREF_MIBAND_MODEL = "miband_model_";
    private static final String PREF_MIBAND_VERSION = "miband_version_";

    public static boolean isAuthenticated() {
        return MiBand.getAuthMac().isEmpty() ? false: true;
    }

    public static void sendAlert(final String... lines) {
        sendAlert(false, lines);
    }

    // convert multi-line text to string for display constraints
    public static void sendAlert(boolean isCall, final String... lines) {

      //  final int width = ModelFeatures.getScreenWidth();
        int width = 5;
        final StringBuilder result = new StringBuilder();

        for (final String message : lines) {
            final StringBuilder messageBuilder = new StringBuilder(message);
            while (messageBuilder.length() < width) {
                if ((messageBuilder.length() % 2) == 0) {
                    messageBuilder.insert(0, " ");
                } else {
                    messageBuilder.append(" ");
                }
            }
            result.append(messageBuilder.toString());
        }

        final String resultRaw = result.toString();
        final int trailing_space = resultRaw.lastIndexOf(' ');
        final String resultString = trailing_space >= width ? result.toString().substring(0, trailing_space) : resultRaw;

        Inevitable.task("miband-send-alert-debounce", isCall ? 300 : 3000, () -> JoH.startService(MiBandService.class, "function", "message",
                "message", resultString,
                "message_type", isCall ? "call" : "glucose"));
    }

    public static void showLatestBG() {
        if (MiBandEntry.isNeedSendReading()) {
            JoH.startService(MiBandService.class, "function", "set_time");
        }
    }

    static String getMac() {
        return Pref.getString(PREF_MIBAND_MAC, null);
    }

    static String getAuthMac() {
        return PersistentStore.getString(PREF_MIBAND_AUTH_MAC);
    }

    static void setAuthMac(final String mac) {
        setVersion("");
        setModel("");
        PersistentStore.setString(PREF_MIBAND_AUTH_MAC, mac);
    }

    static void setMac(final String mac) {
        Pref.setString(PREF_MIBAND_MAC, mac);
    }

    static String getModel() {
        final String mac = getMac();
        if (mac != null) {
            return PersistentStore.getString(PREF_MIBAND_MODEL + mac);
        }
        return null;
    }

    static void setModel(final String model) {
        final String mac = getMac();
        if (mac != null) {
            PersistentStore.setString(PREF_MIBAND_MODEL + mac, model);
        }
    }

    static String getVersion() {
        final String mac = getMac();
        if (mac != null) {
            return PersistentStore.getString(PREF_MIBAND_VERSION + mac);
        }
        return null;
    }

    static void setVersion(final String version) {
        final String mac = getMac();
        if (mac != null) {
            PersistentStore.setString(PREF_MIBAND_VERSION + mac, version);
        }
    }


}
