package com.eveningoutpost.dexdrip.glucosemeter;

// jamorham

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class RecordsCmdTx {

    private static final byte OPCODE_REPORT_RECORDS = 0x01;

    private static final byte ALL_RECORDS = 0x01;
    private static final byte LESS_THAN_OR_EQUAL = 0x02;
    private static final byte GREATER_THAN_OR_EQUAL = 0x03;
    private static final byte WITHIN_RANGE = 0x04;
    private static final byte FIRST_RECORD = 0x05; // first/last order needs verifying on device
    private static final byte LAST_RECORD = 0x06; // first/last order needs verifying on device

    private static final byte FILTER_TYPE_SEQUENCE_NUMBER = 1;
    private static final byte FILTER_TYPE_USER_FACING_TIME = 2;


    public static byte[] getAllRecords() {
        return new byte[]{OPCODE_REPORT_RECORDS, ALL_RECORDS};
    }

    public static byte[] getFirstRecord() {
        return new byte[]{OPCODE_REPORT_RECORDS, FIRST_RECORD};
    }

    public static byte[] getNewerThanSequence(int sequence) {
        final ByteBuffer data = ByteBuffer.allocate(5);
        data.order(ByteOrder.LITTLE_ENDIAN);
        data.put(OPCODE_REPORT_RECORDS);
        data.put(GREATER_THAN_OR_EQUAL);
        data.put(FILTER_TYPE_SEQUENCE_NUMBER);
        data.putShort((short) sequence);
        return data.array();
    }

}
