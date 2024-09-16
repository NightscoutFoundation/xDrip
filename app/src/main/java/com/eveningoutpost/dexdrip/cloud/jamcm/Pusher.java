
package com.eveningoutpost.dexdrip.cloud.jamcm;


import static website.bluejay.Protocol.*;
import static website.bluejay.Constants.*;

import static com.eveningoutpost.dexdrip.cloud.jamcm.Upstream.getBestServerAddress;
import static com.eveningoutpost.dexdrip.models.JoH.getWakeLock;
import static com.eveningoutpost.dexdrip.models.JoH.msSince;
import static com.eveningoutpost.dexdrip.models.JoH.tsl;


import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Base64;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.GcmListenerSvc;
import com.eveningoutpost.dexdrip.JamListenerSvc;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.google.firebase.messaging.RemoteMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Random;

import lombok.Getter;
import lombok.val;
import website.bluejay.Protocol;
import xdrip.cloud.Xdrip;

/**
 * JamOrHam
 */
public class Pusher {
    private static final String TAG = "Pusher";
    private final Object inLock = new Object();
    private final Object outLock = new Object();

    private static final byte[] NEW_LINE = new byte[]{'\n'};

    private static final int CONNECT_TIMEOUT = 45_000;
    private static final int MAX_RECONNECT_TIME = 180_000;
    private static final int WAKELOCK_DURATION = 15_000;

    private volatile long reconnectTimer = 0;

    private static volatile boolean lastMessageReceived;

    private static JamListenerSvc service;
    private Socket clientSocket;
    private BufferedInputStream bis;
    private BufferedOutputStream bos;
    private static final PowerManager.WakeLock wakeLock = getWakeLock("pusher-wake", 1000);

    private static volatile boolean running;
    private static volatile boolean reconnect;

    private static volatile Pusher instance;

    private final Random random = new Random();
    private final MessageStatistics sentStatistics = new MessageStatistics("send");
    private final MessageStatistics receivedStatistics = new MessageStatistics("recv");

    private volatile boolean layerConnected = false;
    private volatile long layerConnectedTime = 0;

    public volatile Bundle offlineMessage = null;

    @Getter
    private volatile String statusString;

    @Getter
    private volatile long nextReconnectionDue = -1;


    public static synchronized Pusher immortality() {
        if (!running) {
            log("Starting");
            running = true;
            final Pusher client = new Pusher();
            instance = client;
            reconnect = false; // already new
            final Thread t = new Thread(() -> {
                client.start();
                running = false;
                instance = null;
                log("Client exited");
            });
            t.start();
            return client;
        } else {
            // log("Already running");
            return instance;
        }
    }

    public static synchronized Pusher getInstance() {
        final Pusher client = immortality();
        if (client == null || !running) {
            // race condition - try again?
            logError("Struggled to get instance");
            return immortality();
        } else {
            return client;
        }
    }

    public static synchronized void requestReconnect() {
            reconnect = true;
            log("Reconnect requested");
    }

    public static boolean enabled() {
        return Pref.getBoolean("use_xdrip_cloud_sync", false);
    }

    public static boolean connected() {
        return getInstance().layerConnected;
    }

    public static int sentLastHour() {
        return getInstance().sentStatistics.getTotalMessagesInLastHour();
    }

    public static int receivedLastHour() {
        return getInstance().receivedStatistics.getTotalMessagesInLastHour();
    }

    public static int sentTotal() {
        return getInstance().sentStatistics.getTotalMessages();
    }

    public static int receivedTotal() {
        return getInstance().receivedStatistics.getTotalMessages();
    }

    public static long connectedTime() {
        if (getInstance().layerConnected) {
            return msSince(getInstance().layerConnectedTime);
        } else {
            return -1;
        }
    }


    /**
     * @noinspection BusyWait
     */

    public void start() {
        while (enabled()) {
            try {
                waitForToken();
                if (GcmActivity.cease_all_activity) {
                    UserError.Log.wtf(TAG, "Cannot connect as cease all activity is activated");
                    return;
                }
                establishConnection();
                listenForMessages();
            } catch (IOException e) {
                val reconnectTime = getReconnectTimer();
                log("Connection lost, retrying in " + reconnectTime + " ... " + e);
                closeConnection();
                nextReconnectionDue = tsl() + reconnectTime;
                while (tsl() < nextReconnectionDue) {
                    status("Waiting to reconnect");
                    try {
                        Thread.sleep(3000); // Wait before retrying
                    } catch (InterruptedException ignored) {
                        //
                    }
                }
                nextReconnectionDue = -1;
            } catch (Exception e) {
                logError("Caught unexpected exception: " + e);
                e.printStackTrace();
                JoH.threadSleep(30_000);
            }
        }
    }

    private void status(final String msg) {
        statusString = msg;
    }

    private long getReconnectTimer() {
        reconnectTimer = Math.min(reconnectTimer + (5_000 + random.nextInt(5_000)), MAX_RECONNECT_TIME + random.nextInt(60_000));
        return reconnectTimer;
    }


    private void waitForToken() {
        while (GcmActivity.token == null) {
            status("Waiting for token");
            log("No token yet");
            try {
                Thread.sleep(20_000);
            } catch (InterruptedException e) {
                //
            }
        }
        log("Got token");
    }

    private void establishConnection() throws IOException {
        log("establishConnection");
        status("Connecting");
        clientSocket = new Socket();
        clientSocket.connect(new InetSocketAddress(getBestServerAddress(), PORT), CONNECT_TIMEOUT);
        bis = new BufferedInputStream(clientSocket.getInputStream());
        bos = new BufferedOutputStream(clientSocket.getOutputStream());
        clientSocket.setSoTimeout(READ_TIMEOUT_MS);
        status("Network Connected");
        sendIdentity();
        status("Waiting for server");
        waitForOK();
        status("Fully connected");
    }

    private void sendIdentity() throws IOException {
        log("sendIdentity()");
        String token = GcmActivity.token;
        if (token != null) {
            outprintln(SUPPORTED_VERSIONS);
            outprintln(token);
            outprintln(GcmActivity.myIdentity());
        } else {
            logError("Token went to null unexpectedly");
        }
    }

    private void waitForOK() throws IOException {
        log("waitForOk");

        if ("OK".equals(inreadLine())) {
            log("Received OK, connection established.");
            // reconnectTimer = 0;
            layerConnected = true;
            layerConnectedTime = tsl();
            sendAnyStoredMessage();
        } else {
            throw new IOException("Didn't get OK message");
        }

    }

    private void sendAnyStoredMessage() {
        val msg = offlineMessage;
        if (msg != null) {
            offlineMessage = null;
            log("Sending previosuly stored message");
            sendMessage(msg);
        }
    }

    private void listenForMessages() throws IOException {
        long lastMessageTime = SystemClock.elapsedRealtime();
        String inputLine;
        clientSocket.setSoTimeout(WAIT_PING_TIMEOUT_MS); // must send a message once per this period
        do {
            try {
                status("Idle");
                inputLine = inreadLine();
                if (inputLine != null) {
                    acquireWakeLock();

                    if (!handleMessage(inputLine)) {
                        log("Read failure - disconnecting!");
                        closeConnection();
                    } else {
                        reconnectTimer = Math.max(0, reconnectTimer - 1000); // For each success reduce the reconnect timer by 1 second
                        receivedStatistics.onMessage();
                        if (reconnect) {
                            reconnect = false;
                            log("Initiating reconnection");
                            closeConnection();
                        }
                    }
                }
            } catch (SocketTimeoutException te) {
                if (SystemClock.elapsedRealtime() - lastMessageTime > IDLE_TIMEOUT_MS) {
                    log("Server timed out!");
                    throw new SocketTimeoutException("server timeout");
                } else {
                    log("Server idle - sending ping");
                    outputMsg(PING);
                    inputLine = "NOP"; // fake to stop loop exiting
                }
            } finally {
                releaseWakeLock();
            }
        } while (inputLine != null);
    }

    private boolean handleMessage(final String inputLine) throws IOException {

        if (!enabled()) {
            log("Service disabled so exiting");
            return false;
        }

        final String[] parts = inputLine.split(" ", 3);
        final String command = parts.length > 0 ? parts[0] : null;
        try {
            final int size = parts.length > 1 ? Math.min(Integer.parseInt(parts[1]), MAX_BINARY_SIZE) : 0;
            final String parameter = parts.length > 2 ? parts[2] : "";
            final byte[] binary = getBytesFromInput(size);

            if (binary == null) {
                log("Failed to get binary packet component");
                return false;
            }

            val cmd = Protocol.findByValue(command);
            if (cmd != null) {
                status("Data from server");
                switch (cmd) {
                    case PONG:
                        log("Got server heartbeat");
                        outputMsg(HEARTBEAT);
                        break;

                    case MESSAGE:
                        log("Got message from server");
                        return gotMessageFromServer(binary, parameter);

                    case MESSAGE_RECEIVED:
                        log("Server reports message received");
                        lastMessageReceived = true;
                        break;

                    case CEASE:
                        log("Received cease message!");
                        GcmActivity.cease_all_activity = true;
                        closeConnection();
                        break;
                    default:
                        log("Unhandled message " + cmd);
                        break;
                }
            } else {
                log("Unknown message " + command);
            }

            return true;
        } catch (NumberFormatException e) {
            log("Failed to parse input line: " + inputLine);
            return false;
        }
    }

    private boolean gotMessageFromServer(final byte[] binary, String parameter) {
        try {
            val trans = Xdrip.Transport.parseFrom(binary);

            switch (trans.getType()) {
                case TRANSPORT_SYNC_MSG:
                    val msg = Xdrip.SyncMsg.parseFrom(trans.getPayload());
                    new Thread(() -> {
                        val wl = getWakeLock("pusher omr", 15_000); // TODO reuse wakelock??
                        getJamServiceInstance().onMessageReceived(getMessage(msg));
                        JoH.releaseWakeLock(wl);
                    }).start();
                    break;

                default:
                    log("Unknown transport message type: " + trans.getType());
            }

            outputMsg(MESSAGE_RECEIVED, parameter);
            return true;
        } catch (InvalidProtocolBufferException e) {
            log("Invalid protocol buffer! " + e);
            return false;
        } catch (IOException e) {
            log("Failure to send message received msg bck");
            return false;
        }
    }

    private static JamListenerSvc getJamServiceInstance() {
        if (service == null) {
            service = new GcmListenerSvc();
            service.setInjectable();
        }
        return service;
    }

    private RemoteMessage getMessage(Xdrip.SyncMsg msg) {
        final HashMap<String, String> map = new HashMap<>();
        map.put("message", "From xDrip Cloud");
        map.put("xfrom", msg.getFrom());
        map.put("yfrom", com.eveningoutpost.dexdrip.xdrip.gs(R.string.gcmtpc) + msg.getTopic());
        map.put("datum", Base64.encodeToString(msg.getPayload().toByteArray(), Base64.NO_WRAP));
        map.put("action", msg.getAction());
        return new RemoteMessage.Builder("internal").setData(map).build();
    }

    private byte[] getBytesFromInput(int size) {
        if (size == 0) {
            return new byte[0];
        }
        int oldTimeout = -1;
        try {
            final byte[] buffer = new byte[size];
            oldTimeout = clientSocket.getSoTimeout();
            clientSocket.setSoTimeout(READ_TIMEOUT_MS);

            int bytesRead = readNBytes(bis, buffer, 0, size);
            if (bytesRead != size) {
                log("Failed to read " + size + " bytes - got: " + bytesRead);
                return null;
            } else {
                // success
                log("Success getting " + size + " bytes");
                //log(HexDump.dumpHexString(buffer));
                // updateLastClientMessageIdleTime();
                return buffer;
            }
        } catch (Exception e) {
            log("Got exception while trying to read bytes: " + size + " " + e);
            return null;
        } finally {
            if (oldTimeout != -1) {
                try {
                    // restore timeout
                    clientSocket.setSoTimeout(oldTimeout);
                } catch (Exception e) {
                    // exceptions all the way down
                }
            }
        }
    }

    private String inreadLine() throws IOException {
        synchronized (inLock) {
            if (bis == null || !clientSocket.isConnected()) {
                throw new IOException("Not connected");
            }
            final byte[] buffer = new byte[1];
            final StringBuilder sb = new StringBuilder();
            while (bis.read(buffer) == 1) {
                if (buffer[0] != NEW_LINE[0] && sb.length() < MAX_BINARY_SIZE) {
                    sb.append((char) buffer[0]);
                } else {
                    return sb.toString();
                }
            }
            return null;
        }
    }

    private void outprintln(String msg) throws IOException {
        if (msg == null) {
            msg = "null";
        }
        synchronized (outLock) {
            if (bos == null || !clientSocket.isConnected()) {
                throw new IOException("Not connected");
            }
            bos.write(msg.getBytes(StandardCharsets.UTF_8));
            bos.write(NEW_LINE);
            bos.flush();
        }
    }

    void outputMsg(final Protocol cmd) throws IOException {
        outputMsg(cmd, (byte[]) null);
    }

    void outputMsg(final Protocol cmd, final String params) throws IOException {
        outputMsg(cmd, null, !params.isEmpty() ? params : null);
    }

    void outputMsg(final Protocol cmd, final byte[] payload) throws IOException {
        outputMsg(cmd, payload, null);
    }

    void outputMsg(final Protocol cmd, final byte[] payload, final String params) throws IOException {
        synchronized (outLock) {
            final StringBuilder header = new StringBuilder(cmd.getValue());
            final int l = payload != null ? payload.length : 0;
            if (l != 0 || params != null) {
                header.append(" ");
                header.append(l);
            }
            if (params != null) {
                header.append(" ");
                header.append(params);
            }
            outprintln(header.toString());
            if (payload != null) {
                bos.write(payload);
                bos.flush();
            }
        }
    }

    public int readNBytes(BufferedInputStream bis, byte[] b, int off, int len) throws IOException {

        int n = 0;
        while (n < len) {
            int count = bis.read(b, off + n, len - n);
            if (count < 0)
                break;
            n += count;
        }
        return n;
    }

    public static boolean sendMessage(Bundle input) {
        try {
            val client = getInstance();
            val enqueue_time = input.getLong("enqueue-time");
            val resend_from_queue = input.getBoolean("resend-from-queue");
            if (enqueue_time != 0L && !resend_from_queue && msSince(enqueue_time) > 300_000) {
                log("Item enqueued more than 5 minutes ago so dropping");
                return false;
            }
            if (!client.layerConnected) {
                log("Output layer not connected so cannot send message");
                if (enqueue_time == 0L) {
                    log("Storing message for resend");
                    input.putLong("enqueue-time", tsl());
                    client.offlineMessage = input;
                }
                return false;
            }

            val msg = Xdrip.SyncMsg.newBuilder()
                    .setAction(input.getString("action"))
                    .setPayload(ByteString.copyFrom(Base64.decode(input.getString("payload"), Base64.NO_WRAP)))
                    .build().toByteArray();

            val trans = Xdrip.Transport.newBuilder()
                    .setId(1)
                    .setType(TRANSPORT_SYNC_MSG)
                    .setPayload(ByteString.copyFrom(msg))
                    .build().toByteArray();

            log("Attempting to send message of size: " + msg.length + "(" + trans.length + ") action: " + input.get("action"));

            lastMessageReceived = false; // message now in flight
            client.outputMsg(MESSAGE, trans);
            client.sentStatistics.onMessage();
        } catch (Exception e) {
            logError("Got exception in sendMessage: " + e);
            return false;
        }
        return true;
    }

    private synchronized void acquireWakeLock() {
        wakeLock.acquire(WAKELOCK_DURATION);
    }


    private synchronized void releaseWakeLock() {
        JoH.releaseWakeLock(wakeLock);
        log("WakeLock released");
    }

    private synchronized void closeConnection() {
        log("closeConnection");
        status("Disconnecting");
        layerConnected = false;
        try {
            if (bos != null) bos.close();
            if (bis != null) bis.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
        } catch (IOException e) {
            log("Error closing connection: " + e.getMessage());
        }
    }

    private static void log(String msg) {
        UserError.Log.d(TAG, msg);
    }

    private static void logError(String msg) {
        UserError.Log.e(TAG, msg);
    }
}


