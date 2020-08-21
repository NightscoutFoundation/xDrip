package com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence;

import com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations;

import java.util.ArrayList;
import java.util.List;

public class SequenceState {
    protected final List<String> sequence = new ArrayList<>();

    public static final String INIT = "INIT";
    public static final String NOTIFICATION_ENABLE = "Notification enable";
    public static final String SET_NIGHTMODE = "Set nightmode";
    public static final String PREPARE_UPLOAD = "Prepare upload";
    public static final String TRANSFER_SEND_WF_INFO = "Send firmware info";
    public static final String TRANSFER_FW_START = "Send firmware start command";
    public static final String TRANSFER_FW_DATA = "Send firmware data";
    public static final String SEND_CHECKSUM = "Send firmware checksum";
    public static final String CHECKSUM_VERIFIED = "Checksum was verified";
    public static final String SLEEP = "SLEEP";

    private FirmwareOperations firmwareOperations;

    {
        sequence.add(INIT);
        sequence.add(NOTIFICATION_ENABLE);
        sequence.add(SET_NIGHTMODE);
        sequence.add(PREPARE_UPLOAD);
        sequence.add(TRANSFER_SEND_WF_INFO);
        sequence.add(TRANSFER_FW_START);
        sequence.add(TRANSFER_FW_DATA);
        sequence.add(SEND_CHECKSUM);
        sequence.add(CHECKSUM_VERIFIED);
    }

    public SequenceState setFirmwareOperations(final FirmwareOperations firmwareOperations) {
        this.firmwareOperations = firmwareOperations;
        return this;
    }

    public String next() {
        try {
            return sequence.get(sequence.indexOf(firmwareOperations.getSequence()) + 1);
        } catch (Exception e) {
            return SLEEP;
        }
    }
}