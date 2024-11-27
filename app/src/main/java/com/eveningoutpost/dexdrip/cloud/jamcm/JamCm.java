package com.eveningoutpost.dexdrip.cloud.jamcm;

import static com.eveningoutpost.dexdrip.utils.CipherUtils.hexToBytes;

import android.os.Bundle;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utils.CipherUtils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import lombok.val;

/**
 * JamOrHam
 * <p>
 * Replacement for GCM
 */
public class JamCm {

    private static final String TAG = "JamCm";
    private static final String serverInstance = "jamcm3749021";
    private static final String serverDomain = "bluejay.website";
    private static final String serverAddress = serverInstance + "." + serverDomain;
    private static final int serverPort = 5228;

    private static final byte PROTOCOL_VERSION = 1;

    /**
     * @noinspection DataFlowIssue
     */
    public static String getId() {
        if (GcmActivity.token == null) {
            return null;
        }
        try {
            return CipherUtils.getSHA256(GcmActivity.token).substring(0, 32);
        } catch (Exception e) {
            UserError.Log.wtf(TAG, "Got exception in getId: " + e);
            return null;
        }
    }

    /**
     * @noinspection DataFlowIssue
     */
    public static void sendMessage(final Bundle input) {
            if (Pusher.enabled()) {
                Pusher.sendMessage(input);
            } else {
                sendMessagePrevious(input);
            }
    }

    public static void sendMessageBackground(final Bundle input) {
        new Thread(() -> sendMessage(input)).start();
    }

    public static void sendMessagePrevious(Bundle input) {

        val ids = getId();
        if (ids == null) {
            if (JoH.ratelimit("sendMessage error", 1200)) {
                UserError.Log.wtf(TAG, "Cannot send message due to missing id");
            }
            return;
        }
        try {
            UserError.Log.d(TAG, "sendMessage called");
            InetAddress address = InetAddress.getByName(serverAddress);

            byte[] id = hexToBytes(ids);
            if (id.length != 16) {
                throw new RuntimeException("Invalid id length: " + id.length);
            }
            byte[] channel = GcmActivity.myIdentity().getBytes(StandardCharsets.UTF_8);
            if (channel.length != 32) {
                throw new RuntimeException("Invalid channel length: " + channel.length);
            }

            byte[] type = new byte[16];
            byte[] actionb = input.getString("action").getBytes(StandardCharsets.UTF_8);
            System.arraycopy(actionb, 0, type, 0, Math.min(type.length, actionb.length));
            byte[] payload = input.getString("payload").getBytes(StandardCharsets.UTF_8);
            short messageSize = (short) payload.length;

            val buffer = ByteBuffer.allocate(1 + 16 + 32 + 16 + 2 + payload.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.put(PROTOCOL_VERSION);
            buffer.put(id);
            buffer.put(channel);
            buffer.put(type);
            buffer.putShort(messageSize);
            buffer.put(payload);

            val data = buffer.array();
            val packet = new DatagramPacket(data, data.length, address, serverPort);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.send(packet);
                UserError.Log.d(TAG, "Message sent to server");
            }
        } catch (Exception e) {
            UserError.Log.e(TAG, "Error: " + e);
        }
    }

}
