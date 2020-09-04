package com.eveningoutpost.dexdrip.watch.miband.Firmware.Sequence;

import java.util.ArrayList;
import java.util.List;

public abstract class SequenceState {
    protected final List<String> sequence = new ArrayList<>();


    private String sequenceState = SequenceState.INIT;


    public static final String INIT = "INIT";
    public static final String NOTIFICATION_ENABLE = "NOTIFICATION_ENABLE";
    public static final String SET_NIGHTMODE = "SET_NIGHTMODE";
    public static final String PREPARE_UPLOAD = "PREPARE_UPLOAD";
    public static final String WAITING_PREPARE_UPLOAD_RESPONSE = "WAITING_PREPARE_UPLOAD_RESPONSE";
    public static final String TRANSFER_SEND_WF_INFO = "TRANSFER_SEND_WF_INFO";
    public static final String WAITING_TRANSFER_SEND_WF_INFO_RESPONSE = "WAITING_TRANSFER_SEND_WF_INFO_RESPONSE";
    public static final String TRANSFER_FW_START = "TRANSFER_FW_START";
    public static final String TRANSFER_FW_DATA = "TRANSFER_FW_DATA";
    public static final String SEND_CHECKSUM = "SEND_CHECKSUM";
    public static final String WAITING_SEND_CHECKSUM_RESPONSE = "WAITING_SEND_CHECKSUM_RESPONSE";
    public static final String CHECKSUM_VERIFIED = "CHECKSUM_VERIFIED";
    public static final String SLEEP = "SLEEP";

    public String getSequence() {
        return sequenceState;
    }

    public void setSequenceState(String sequenceState) {
        this.sequenceState = sequenceState;
    }

    public String next() {
        try {
            sequenceState = sequence.get(sequence.indexOf(getSequence()) + 1);
        } catch (Exception e) {
            sequenceState = SLEEP;
        }
        return sequenceState;
    }
}