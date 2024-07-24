package com.eveningoutpost.dexdrip.watch.thinjam.messages;

import com.eveningoutpost.dexdrip.models.JoH;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.eveningoutpost.dexdrip.watch.thinjam.Const.OPCODE_NOTIFY_MSG;

// jamorham

public class NotifyTx extends BaseTx {

    private static final int MAX_CHUNK_SIZE = 18;

    public NotifyTx(final int notificationType, final byte[] msgChunk) {

        if (msgChunk == null || msgChunk.length > MAX_CHUNK_SIZE) {
            throw new RuntimeException("Invalid msg chunk string in NotifyTx");
        }

        init(OPCODE_NOTIFY_MSG, 19);
        data.put((byte) (notificationType & 0xFF));     // notification type
        byte[] payload = new byte[MAX_CHUNK_SIZE];
        System.arraycopy(msgChunk, 0, payload, 0, Math.min(msgChunk.length, payload.length));
        data.put(payload);

    }

    public static List<NotifyTx> getPacketStreamForNotification(final int notificationType, final String msg) {
        if (JoH.emptyString(msg)) return null;
        final List<NotifyTx> packets = new ArrayList<>();
        try {
            final byte[] msgBytes = msg.getBytes("UTF-8");
            final int count = Math.min((msgBytes.length / MAX_CHUNK_SIZE) + 1, 15); // cannot have more than 15 parts (0 reserved)
            for (int i = count; i >= 1; i--) {
                final int pos = (i - 1) * MAX_CHUNK_SIZE;
                final byte[] slice = Arrays.copyOfRange(msgBytes, pos, Math.min(pos + MAX_CHUNK_SIZE, msgBytes.length));
                packets.add(new NotifyTx(notificationType << 4 | i, slice));
            }
        } catch (UnsupportedEncodingException e) {
            return null;
        }
        return packets;
    }

    // TODO check numeric references
    public enum TJ_NotifyClass {
        NoneClass,
        Call,
        TextMessage,
        HighAlert,
        LowAlert,
        OtherAlert
    }
}
