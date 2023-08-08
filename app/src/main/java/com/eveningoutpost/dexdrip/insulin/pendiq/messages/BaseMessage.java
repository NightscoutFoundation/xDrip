package com.eveningoutpost.dexdrip.insulin.pendiq.messages;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utils.CRC16ccitt;
import com.google.gson.annotations.Expose;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;

import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.COMMAND_PACKET;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.CONTROL_SHIFT;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.END_OF_MESSAGE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.ESCAPE_CHARACTER;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.LAST_MESSAGE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.MORE_TO_FOLLOW;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.START_OF_MESSAGE;

public class BaseMessage {

    private static final int HEADER_LENGTH = 8;
    private static final int FOOTER_LENGTH = 3;
    private static final int BLUETOOTH_MTU = 20;
    private static final int CONTROL_LENGTH = 3;
    private static final int SEGMENT_LENGTH = BLUETOOTH_MTU - CONTROL_LENGTH;

    public ByteBuffer data = null;
    public byte[] byteSequence = null;
    @Expose
    public byte[] escapedByteSequence = null;


    void init(short counter, int length) {
        length = length + HEADER_LENGTH + FOOTER_LENGTH;
        data = ByteBuffer.allocate(length);
        data.order(ByteOrder.LITTLE_ENDIAN);

        data.put(START_OF_MESSAGE);
        data.putInt(length - 2);
        data.putShort(counter);
        data.put(COMMAND_PACKET);
    }

    byte[] appendFooter() {
        data.put(getCrc(getByteSequence()));
        data.put(END_OF_MESSAGE);
        return getByteSequence();
    }

    byte[] getByteSequence() {
        byteSequence = data.array();
        escapedByteSequence = escapeControlCharacters(byteSequence);
        return byteSequence;
    }

    public String toS() {
        return JoH.defaultGsonInstance().toJson(this);
    }



    public static byte[] getCrc(byte[] packet) {
        return CRC16ccitt.crc16ccitt(packet, 3, true, 0x0000);
    }

    public static byte[] getPureCrc(byte[] packet) {
        return CRC16ccitt.crc16ccitt(packet, 2, false, 0x0000);
    }

    public static boolean checkPureCrc(byte[] packet) {
        if (packet == null || packet.length < 3) return false;
        final byte[] checksum = getPureCrc(packet);
        return checksum[0] == packet[packet.length - 2] && checksum[1] == packet[packet.length - 1];
    }


    @SuppressWarnings("UnusedAssignment")
    byte[] escapeControlCharacters(byte[] source) {
        if (source == null || source.length < 3) return null;
        final ByteArrayOutputStream output = new ByteArrayOutputStream(source.length + 64);
        output.write(source[0]); // message start
        int i = 1;
        for (i = 1; i < source.length - 1; i++) {
            if (isControlCharacter(source[i])) {
                output.write(ESCAPE_CHARACTER);
                output.write(source[i] + CONTROL_SHIFT);
            } else {
                output.write(source[i]);
            }
        }
        output.write(source[i]); // message end
        return output.toByteArray();
    }

    public static byte[] unescapeControlCharacters(byte[] source) {
        if (source == null) return null;
        try {
            final ByteArrayOutputStream output = new ByteArrayOutputStream(source.length);
            for (int i = 0; i < source.length; i++) {
                if (isEscapeCharacter(source[i])) {
                    i++;
                    output.write(source[i] - CONTROL_SHIFT);
                } else {
                    output.write(source[i]);
                }
            }
            return output.toByteArray();
        } catch (ArrayIndexOutOfBoundsException e) {
            UserError.Log.e("Pendiq", "Array index out of bounds when unescaping: " + JoH.bytesToHex(source));
            return null;
        }
    }


    public List<byte[]> getFragmentStream() {
        return getFragmentStream(escapedByteSequence);
    }


    @SuppressWarnings("UnusedAssignment")
    static List<byte[]> getFragmentStream(byte[] source) {
        final List<byte[]> stream = new LinkedList<>();
        final int whole_packets = (source.length - 2) / SEGMENT_LENGTH;
        final int last_bytes = (source.length - 2) % SEGMENT_LENGTH;

        int source_idx = 1;
        int whole_packet_number;

        for (whole_packet_number = 1, source_idx = 1; whole_packet_number <= whole_packets;
             whole_packet_number++, source_idx = source_idx + SEGMENT_LENGTH) {
            final byte[] new_whole_packet = new byte[BLUETOOTH_MTU];
            new_whole_packet[0] = START_OF_MESSAGE;
            System.arraycopy(source, source_idx, new_whole_packet, 1, SEGMENT_LENGTH);
            if (last_bytes == 0 && whole_packet_number == whole_packets) {
                new_whole_packet[SEGMENT_LENGTH + 1] = LAST_MESSAGE;
            } else {
                new_whole_packet[SEGMENT_LENGTH + 1] = MORE_TO_FOLLOW;
            }
            new_whole_packet[SEGMENT_LENGTH + 2] = END_OF_MESSAGE;

            stream.add(new_whole_packet);
        }

        if (last_bytes != 0) {
            final byte[] new_semi_packet = new byte[(last_bytes + CONTROL_LENGTH)];
            new_semi_packet[0] = START_OF_MESSAGE;
            System.arraycopy(source, source_idx, new_semi_packet, 1, last_bytes);
            new_semi_packet[last_bytes + 1] = LAST_MESSAGE;
            new_semi_packet[last_bytes + 2] = END_OF_MESSAGE;

            stream.add(new_semi_packet);
        }
        return stream;
    }

    static boolean isControlCharacter(byte value) {
        return (value > 1) && (value < 7);
    }

    static boolean isEscapeCharacter(byte value) {
        return value == ESCAPE_CHARACTER;
    }

    static int getTimeZoneOffsetSeconds() {
        int offset_seconds = TimeZone.getDefault().getRawOffset() / 1000;
        UserError.Log.d("Pendiq", "Timezone Offset seconds: " + offset_seconds);
        return offset_seconds;
    }

    static byte getTimeZoneOffsetByte() {
        final int tzHour = (int) (TimeZone.getDefault().getDSTSavings() / Constants.HOUR_IN_MS);
        return (byte) tzHour;
    }
}


