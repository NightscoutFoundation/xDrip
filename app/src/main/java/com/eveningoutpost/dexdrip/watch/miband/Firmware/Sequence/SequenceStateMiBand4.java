package com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence;

public class SequenceStateMiBand4 extends SequenceState {
    {
        sequence.add(INIT);
        sequence.add(NOTIFICATION_ENABLE);
        sequence.add(SET_NIGHTMODE);
        sequence.add(PREPARE_UPLOAD);
        sequence.add(WAITING_PREPARE_UPLOAD_RESPONSE);
        sequence.add(TRANSFER_SEND_WF_INFO);
        sequence.add(WAITING_TRANSFER_SEND_WF_INFO_RESPONSE);
        sequence.add(TRANSFER_FW_START);
        sequence.add(TRANSFER_FW_DATA);
        sequence.add(SEND_CHECKSUM);
        sequence.add(WAITING_SEND_CHECKSUM_RESPONSE);
        sequence.add(CHECKSUM_VERIFIED);
    }
}