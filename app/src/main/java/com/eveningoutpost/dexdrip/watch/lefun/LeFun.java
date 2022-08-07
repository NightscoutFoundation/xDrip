package com.eveningoutpost.dexdrip.watch.lefun;

import androidx.annotation.VisibleForTesting;
import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Inevitable;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.UtilityModels.Pref;
import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Jamorham
 *
 * Lefun Lightweight logic class
 */

public class LeFun {

    private static final String PREF_LEFUN_MAC = "lefun_mac";

    public static void sendAlert(final String... lines) {
        sendAlert(false, lines);
    }

    // convert multi-line text to string for display constraints
    public static void sendAlert(boolean isCall, final String... lines) {

        final int width = ModelFeatures.getScreenWidth();

        final String resultString = formatAlertMessage(width, Arrays.asList(lines));

        Inevitable.task("lefun-send-alert-debounce", isCall ? 300 : 3000, () -> JoH.startService(LeFunService.class, "function", "message",
                "message", resultString,
                "message_type", isCall ? "call" : "glucose"));
    }

    @VisibleForTesting
    static String formatAlertMessage(int width, List<String> lines) {
        final StringBuilder result = new StringBuilder();

        for (final String message : lines) {
            result.append(padToWidth(width, message));
        }

        final String resultRaw = result.toString();
        final int trailing_space = resultRaw.lastIndexOf(' ');
        final String resultString = trailing_space >= width ? result.substring(0, trailing_space) : resultRaw;
        return resultString;
    }

    /** Pads a message with spaces to center it in the given width. */
    @VisibleForTesting
    static String padToWidth(int width, String message) {
        if (width <= message.length()) {
            return message;
        }
        int leftSpaces = (width - message.length()) / 2;
        int rightSpaces = (width - message.length() + 1) / 2;
        return new StringBuilder()
            .append(Strings.repeat(" ", leftSpaces))
            .append(message)
            .append(Strings.repeat(" ", rightSpaces))
            .toString();
    }

    public static void showLatestBG() {
        if (LeFunEntry.isEnabled()) {
            JoH.startService(LeFunService.class);
        }
    }

    static boolean shakeToSnooze() {
        return Pref.getBooleanDefaultFalse("lefun_option_shake_snoozes");
    }

    static String getMac() {
        return Pref.getString(PREF_LEFUN_MAC, null);
    }

    static void setMac(final String mac) {
        Pref.setString(PREF_LEFUN_MAC, mac);
    }

    static String getModel() {
        final String mac = getMac();
        if (mac != null) {
            return PersistentStore.getString("lefun_model_" + mac);
        }
        return null;
    }

    static void setModel(final String model) {
        final String mac = getMac();
        if (mac != null) {
            PersistentStore.setString("lefun_model_" + mac, model);
        }
    }

    public static byte calculateCRC(final byte[] buffer, final int length) {
        int result = 0;
        for (int index = 0; index < length; index++) {
            for (int bit = 0; bit < 8; bit++) {
                result = ((buffer[index] >> bit ^ result) & 1) == 0 ? result >> 1 : (result ^ 0x18) >> 1 | 0x80;
            }
        }
        return (byte) result;
    }

}
