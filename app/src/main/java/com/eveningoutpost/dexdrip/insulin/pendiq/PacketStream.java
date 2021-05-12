package com.eveningoutpost.dexdrip.insulin.pendiq;

import com.eveningoutpost.dexdrip.insulin.pendiq.messages.BaseMessage;
import com.eveningoutpost.dexdrip.utils.CircularArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.END_OF_MESSAGE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.LAST_MESSAGE;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.MORE_TO_FOLLOW;
import static com.eveningoutpost.dexdrip.insulin.pendiq.Const.START_OF_MESSAGE;

public class PacketStream {

    private static final CircularArrayList<Byte> buffer = new CircularArrayList<>(4096);
    private static final ByteBuffer currentMessage = ByteBuffer.allocate(2048);
    private static final LinkedList<byte[]> completePackets = new LinkedList<>();
    private static volatile int packet_start = -1;
    private static volatile int packet_end = -1;

    static {
        buffer.setAutoEvict(true);
        currentMessage.order(ByteOrder.LITTLE_ENDIAN);
    }

    // process an incoming stream of bytes typically from a notification.
    // identify and reconstruct packets from fragments
    // remove control character escaping, sanity check and return valid packets after each burst
    public static synchronized List<byte[]> addBytes(byte[] bytes) {
        for (byte b : bytes) {
            if (buffer.size() != 0 || b == START_OF_MESSAGE) {
                buffer.add(b);
                if (b == START_OF_MESSAGE) {
                    packet_start = buffer.size() - 1;
                    packet_end = -1;
                } else if (b == END_OF_MESSAGE) {
                    packet_end = buffer.size();
                    if (packet_start > -1) {
                        byte type = buffer.tail(1);
                        System.out.println("Packet identified: " + packet_start + "-" + packet_end + " type: " + type);
                        currentMessage.put(getBytes(packet_start + 1, packet_end - 2));
                        if (type == LAST_MESSAGE) {
                            System.out.println("Sequence finished");
                            byte[] result = new byte[currentMessage.position()];
                            currentMessage.rewind();
                            currentMessage.get(result, 0, result.length);
                            currentMessage.clear();
                            buffer.clear();
                            result = BaseMessage.unescapeControlCharacters(result);
                            final int reported_length = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN).getInt();
                            if (result == null) {
                                System.out.println("Invalid escaping");
                                continue; // invalid escaping
                            }
                            if (reported_length == result.length) {
                                if (BaseMessage.checkPureCrc(result)) {
                                    System.out.println("Valid packet! adding to return list");
                                    completePackets.add(result);
                                } else {
                                    System.out.println("Checksum invalid!");
                                }
                            } else {
                                System.out.println("Message length invalid: " + reported_length + " vs " + result.length);
                            }
                        } else if (type != MORE_TO_FOLLOW) {
                            currentMessage.clear();
                            System.out.println("Invalid end type: " + type + " resetting state");
                        }
                    }
                }
            }
        }
        final int returningCount = completePackets.size();
        System.out.println("Completed packets to return: "+returningCount);
        try {
            return returningCount > 0 ? (List)completePackets.clone() : null;
        } finally {
            if (returningCount > 0) {
                completePackets.clear();
            }
        }
    }

    private static byte[] getBytes(int start, int end) {
        System.out.println("getbytes buffer size: " + buffer.size() + " vs " + (end - start));
        byte[] result = new byte[(end - start)];
        for (int i = start, j = 0; i < end; i++, j++) {
            result[j] = buffer.get(i);
        }
        return result;
    }

}
