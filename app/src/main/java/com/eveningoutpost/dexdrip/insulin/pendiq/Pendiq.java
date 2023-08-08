package com.eveningoutpost.dexdrip.insulin.pendiq;

// Jamorham

// Pendiq 2.0 driver control and utility class

import android.content.Intent;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.xdrip;

import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.MINIMUM_DOSE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.NOT_OK;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.OK;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.PROGRESS_PACKET;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.REPORT_PACKET;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.RESULT_PACKET;

public class Pendiq {

    static final String PENDIQ_INSTRUCTION = "Pendiq-service-command";
    static final String PENDIQ_TIMESTAMP = "Pendiq-service-timestamp";
    static final String PENDIQ_PARAMETER = "Pendiq-service-parameter";
    static final String PENDIQ_ACTION = "PENDIQ_ACTION";

    static final String PENDIQ_COMMAND_DOSE_PREP = "Dose Prep";

    static boolean isPendiqName(String name) {
        return name != null && (name.equalsIgnoreCase("pendiq2") || isEightHexChars(name));
    }


    static boolean isEightHexChars(String string) {
        return (string.length() == 8
                && string.replaceAll("[0-9A-F]", "").length() == 0);

    }

    static int getErrorCode(byte[] packet) {
        if (packet == null || packet.length < 9) return 1; // invalid packet structure
        if (packet[6] == PROGRESS_PACKET && packet[7] == NOT_OK) return packet[8];
        // 1 = crc
        // 2 = invalid sequence number
        // 3 = invalid structure
        // 4 = unknown??
        // 5 = unknown??
        return 0; // no error
    }

    static int getPacketType(byte[] packet) {
        if (packet == null || packet.length < 9) return -1; // invalid packet structure
        return packet[6];
    }

    static int getResultPacketType(byte[] packet) {
        return (getPacketType(packet) == RESULT_PACKET) ? packet[8] : -1;
    }

    static boolean isResultPacketOk(byte[] packet) {
        return (getPacketType(packet) == RESULT_PACKET && packet[7] == OK);
    }

    static boolean isProgressPacket(byte[] packet) {
        return getPacketType(packet) == PROGRESS_PACKET;
    }

    static boolean isReportPacket(byte[] packet) {
        return getPacketType(packet) == REPORT_PACKET;
    }

    static boolean isResultPacket(byte[] packet) {
        return getPacketType(packet) == RESULT_PACKET;
    }

    public static boolean enabled() {
        return Pref.getBooleanDefaultFalse("use_pendiq");
    }

    public static void immortality() {
        if (enabled()) {
            if (JoH.ratelimit("pendiq-auto-start-service", 30)) {
                JoH.startService(PendiqService.class);
            }
        }
    }

    public static boolean handleTreatment(double insulin) {
        if (insulin > 0) {
            if (enabled() && Pref.getBooleanDefaultFalse("pendiq_send_treatments")) {
                if (insulin >= MINIMUM_DOSE) {
                    if (JoH.ratelimit("pendiq-dose-prep", 20)) {
                        sendInstruction(PENDIQ_COMMAND_DOSE_PREP, "" + insulin);
                    }
                    return true;
                } else {
                    JoH.static_toast_long("Insulin dose below minimum of: " + MINIMUM_DOSE + "U for Pendiq");
                }
            }
        }
        return false;
    }

    private static void sendInstruction(String command, String parameter) {
        final Intent intent = new Intent(xdrip.getAppContext(), PendiqService.class)
                .setAction(PENDIQ_ACTION)
                .putExtra(PENDIQ_TIMESTAMP, JoH.tsl())
                .putExtra(PENDIQ_INSTRUCTION, command)
                .putExtra(PENDIQ_PARAMETER, parameter);
        xdrip.getAppContext().startService(intent);
    }

    private static final String PENDIQ_PIN_PREFIX = "pendiq-pin-";

    static void setPin(String address, String pin) {
        if (pin == null || address == null) return;
        if (getPin(address).equals(pin)) return;
        PersistentStore.setString(PENDIQ_PIN_PREFIX + address, pin);
    }

    private static String getPin(String address) {
        return PersistentStore.getString(PENDIQ_PIN_PREFIX + address);
    }

    static boolean checkPin(String pin) {
        String pinPref = Pref.getString("pendiq_pin", "0000");
        if (pinPref.equals("0")) pinPref = "0000"; // annoying preference issue
        return pin != null && pin.equals(pinPref);
    }
}



