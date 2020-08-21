package com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence;

import com.eveningoutpost.dexdrip.watch.miband.Firmware.FirmwareOperations;

import java.util.ArrayList;
import java.util.List;

public class SequenceStateMiBand5 extends SequenceState {
    public static final String UNKNOWN_REQUEST = "UNKNOWN REQUEST";
    public static final String UNKNOWN_INIT_COMMAND = "UNKNOWN INIT COMMAND";

    {
        sequence.clear();
        sequence.add(INIT);
        sequence.add(UNKNOWN_INIT_COMMAND);
        sequence.add(NOTIFICATION_ENABLE);
        sequence.add(SET_NIGHTMODE);
        sequence.add(PREPARE_UPLOAD);
        sequence.add(UNKNOWN_REQUEST);
        sequence.add(TRANSFER_SEND_WF_INFO);
        sequence.add(TRANSFER_FW_START);
        sequence.add(TRANSFER_FW_DATA);
        sequence.add(SEND_CHECKSUM);
        sequence.add(CHECKSUM_VERIFIED);
    }
}