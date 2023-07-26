package com.eveningoutpost.dexdrip.utils.bt;

import com.eveningoutpost.dexdrip.GcmActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.services.Ob1G5CollectionService;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utilitymodels.WholeHouse;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.Root;
import com.eveningoutpost.dexdrip.xdrip;
import com.google.gson.annotations.Expose;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.IllegalCharsetNameException;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.COPY_COLLISION_KEY;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.COPY_DEVICE_KEY;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.COPY_SCAN;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.INJECT_COLLISION_KEY;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.INJECT_DEVICE_KEY;
import static com.eveningoutpost.dexdrip.utils.bt.Mimeograph.SearchState.SCAN;

// jamorham

public class Mimeograph {

    private static final String TAG = "Mimeograph";
    private static final String KEY_STORE = "/data/misc/bluedroid/bt_config.conf";
    private static final String MAC_STORE = "/data/property/persist.service.bdroid.bdaddr";
    private static final String ADAPTER_MARKER = "[Adapter]";
    private static final int MAX_BYTES = 40000;
    private static volatile int spinner;

    private static String localMac;
    private static String lastWrite = "";
    private static String lastBroadcast = "";
    private static long lastLocalReception;

    enum SearchState {
        SCAN,
        COPY_COLLISION_KEY,
        COPY_DEVICE_KEY,
        COPY_SCAN,
        INJECT_COLLISION_KEY,
        INJECT_DEVICE_KEY,
    }

    // should only be called by positive local reception
    public static void poll(final boolean hint) {
        if (!enabled()) return;
        lastLocalReception = JoH.tsl();
        Inevitable.task("mimeograph poll", 2000, () -> pollTask(hint));
    }

    private static String extractCachedBtMac() {
        if (localMac == null) {
            localMac = extractBtMac();
        }
        return localMac;
    }

    private static String extractBtMac() {
        if (!enabled()) {
            UserError.Log.d(TAG, "Not tested except on RPI - skipping");
            return null;
        }
        return readSystemFileContent(MAC_STORE);
    }

    private static String extractConfig() {
        if (!enabled()) {
            UserError.Log.d(TAG, "Not tested except on RPI - skipping");
            return null;
        }
        return readSystemFileContent(KEY_STORE);
    }

    private static String getExtractedXferJson() {
        final Xfer xfer = getXfer(extractConfig());
        return xfer != null ? xfer.valid() ? xfer.toJson() : null : null;
    }

    private synchronized static void pollTask(boolean hint) {
        // TODO rate limit checking without hint? allow force push and improve persistence
        final String result = getExtractedXferJson();
        if (result != null) {
            if (result.equals(lastBroadcast) && JoH.pratelimit("mimeograph last bcast", 3600)) {
                UserError.Log.d(TAG, "Data unchanged since last broadcast");
            } else {
                GcmActivity.sendMimeoGraphUpdate(result);
                lastBroadcast = result;
            }
        } else {
            UserError.Log.d(TAG, "No valid Xfer on poll");
        }
    }

    private static String readFileContent(final String path) {
        try (final FileInputStream fileInputStream = new FileInputStream(path)) {
            final int length = fileInputStream.available();
            if (length > MAX_BYTES) {
                UserError.Log.e(TAG, "File too large: " + path + " vs " + MAX_BYTES);
                return null;
            }
            final byte[] buffer = new byte[length];
            readAllToBuffer(fileInputStream, buffer);
            return new String(buffer, "UTF-8");
        } catch (FileNotFoundException e) {
            UserError.Log.e(TAG, "Could not find: " + path);
        } catch (IOException e) {
            UserError.Log.e(TAG, "IO Error: " + e);
        }
        return null;
    }

    private static boolean writeFileContent(final String path, final String content) {
        try (final FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            final byte[] bytes = content.getBytes("UTF-8");
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
            final File file = new File(path);
            return file.length() == bytes.length;
        } catch (FileNotFoundException e) {
            UserError.Log.e(TAG, "Could not find: " + path);
        } catch (IOException e) {
            UserError.Log.e(TAG, "IO Error: " + e);
        } catch (IllegalCharsetNameException e) {
            UserError.Log.e(TAG, "Could not find charset in write");
        }
        return false;
    }

    private static int readAllToBuffer(final InputStream stream, final byte[] buffer) throws IOException {
        int readBytes = 0;
        int thisRead;
        while ((thisRead = stream.read(buffer, readBytes, buffer.length - readBytes)) > 0) {
            readBytes += thisRead;
            if (readBytes > MAX_BYTES) break;
        }
        return readBytes;
    }

    private static Xfer getXfer(final String data) {
        if (data == null) return null;
        final String[] array = split(data);
        SearchState state = SCAN;
        final StringBuilder asb = new StringBuilder();
        final StringBuilder dsb = new StringBuilder();

        final String deviceMac = Ob1G5CollectionService.getMac();
        if (deviceMac == null) {
            UserError.Log.d(TAG, "OB1 device mac not known");
            return null;
        }
        final String deviceHunt = String.format("[%s]", deviceMac).toLowerCase();
        UserError.Log.d(TAG, "Hunting for: " + deviceHunt);
        for (final String line : array) {
            if (line.startsWith(ADAPTER_MARKER)) {
                state = COPY_COLLISION_KEY;
            } else if (line.startsWith(deviceHunt)) {
                state = COPY_DEVICE_KEY;
            } else if (line.length() == 0) {
                state = SCAN;
            }
            switch (state) {
                case COPY_COLLISION_KEY:
                    add(line, asb);
                    break;
                case COPY_DEVICE_KEY:
                    add(line, dsb);
                    break;
            }
        }

        final Xfer xfer = new Xfer(asb.toString(), dsb.toString())
                .setSpoofMac(extractCachedBtMac());
        return xfer.valid() ? xfer : null;
    }

    private synchronized static void mergeXfer(final Xfer xfer) {
        if (xfer == null) return;
        UserError.Log.d(TAG, "Merging");
        if (!xfer.valid()) {
            UserError.Log.e(TAG, "Xfer is not valid");
            return;
        }

        final String data = extractConfig();
        if (data == null) {
            UserError.Log.e(TAG, "Cannot extract local config for merge");
            return;
        }

        if (xfer.lastRecord == 0) {
            UserError.Log.d(TAG, "Sender has not processed a record - rejecting");
            return;
        }

        if (xfer.lastRecord <= lastLocalReception) {
            UserError.Log.d(TAG, "Sender has not processed a record newer than ours");
            return;
        }
        final String sourceHash = CipherUtils.getSHA256(data);
        final StringBuilder output = new StringBuilder();

        final String[] adapterArray = split(xfer.adapter);
        final String[] deviceArray = split(xfer.device);
        final String[] array = split(data);

        int processedSections = 0;
        boolean processedAdapter = false;
        boolean processedDevice = false;

        SearchState state = COPY_SCAN;

        for (final String line : array) {
            if (line.startsWith(adapterArray[0])) {
                state = INJECT_COLLISION_KEY;
                processedSections++;
            } else if (line.startsWith(deviceArray[0])) {
                state = INJECT_DEVICE_KEY;
                processedSections++;
            } else if (line.length() == 0) {
                state = COPY_SCAN;
            }
            switch (state) {
                case INJECT_COLLISION_KEY:
                    if (!processedAdapter) {
                        addAll(adapterArray, output);
                        processedAdapter = true;
                    }
                    break;
                case INJECT_DEVICE_KEY:
                    if (!processedDevice) {
                        addAll(deviceArray, output);
                        processedDevice = true;
                    }
                    break;
                case COPY_SCAN:
                    add(line, output);
                    break;
            }
        }

        if (processedSections == 0) {
            UserError.Log.e(TAG, "Could not process merge as there were no hits");
            return;
        } else if (processedSections > 2) {
            UserError.Log.e(TAG, "Too many processed sections: " + processedSections);
            return;
        } else if (processedSections == 1) {
            if (processedAdapter) {
                UserError.Log.d(TAG, "No current device - appending");
                output.append("\n");
                addAll(deviceArray, output);
            } else {
                UserError.Log.e(TAG, "Device section without adapter");
                return;
            }
        }

        try {
            final String fOutput = output.toString();
            final String destHash = CipherUtils.getSHA256(fOutput);
            //noinspection ConstantConditions
            if (!destHash.equals(sourceHash)) {
                writeSystemFileContent(KEY_STORE, fOutput);
                UserError.Log.d(TAG, "Merge probably successful - restarting bluetooth");
                JoH.restartBluetooth(xdrip.getAppContext());
            } else {
                UserError.Log.d(TAG, "Merge successful but no update required");
            }
        } catch (NullPointerException e) {
            UserError.Log.wtf(TAG, "Got null pointer in write: " + e);
        }
    }

    private static synchronized String readSystemFileContent(final String path) {
        final String tmpPath = getUniqueTmpFileName();
        final List<String> results = Root.exec(
                String.format("cp '%s' '%s'", path, tmpPath),
                String.format("chmod 777 '%s'", tmpPath));
        if (results == null) return null;
        final String result = readFileContent(tmpPath);
        Root.exec(String.format("rm -f '%s'", tmpPath));
        return result;
    }

    private static synchronized boolean writeSystemFileContent(final String path, final String content) {
        final String tmpPath = getUniqueTmpFileName();
        if (!writeFileContent(tmpPath, content)) {
            UserError.Log.e(TAG, "Failed to write to: " + path);
            return false;
        }
        // TODO detect failure??
        Root.exec(
                String.format("cat '%s' > '%s'", tmpPath, path),
                String.format("rm -f '%s'", tmpPath));
        return true;
    }

    private synchronized static String getUniqueTmpFileName() {
        return xdrip.getAppContext().getFilesDir() + "/tmpfile-" + JoH.tsl() + "-" + spinner++;
    }

    private static String[] split(final String str) {
        return str.split("\n");
    }

    private synchronized static void add(final String aLine, final StringBuilder output) {
        if (aLine.length() != 0 || lastWrite.length() != 0) {
            output.append(aLine);
            output.append("\n");
            lastWrite = aLine;
        }
    }

    private static void addAll(final String[] array, final StringBuilder output) {
        for (final String aLine : array) {
            add(aLine, output);
        }
        add("", output);
    }

    public static void putXferFromJson(final String json) {
        if (enabled()) {
            mergeXfer(Xfer.fromJson(json));
        }
    }

    @RequiredArgsConstructor
    private static class Xfer {
        @Expose
        public String spoofMac;
        @Expose
        final public String adapter;
        @Expose
        final public String device;
        @Expose
        public long lastRecord;

        {
            try {
                lastRecord = lastLocalReception;
            } catch (NullPointerException e) {
                //
            }
        }

        boolean valid() {
            return !emptyString(device) && !emptyString(adapter);
        }

        String toJson() {
            return JoH.defaultGsonInstance().toJson(this);
        }

        static Xfer fromJson(final String json) {
            try {
                return JoH.defaultGsonInstance().fromJson(json, Xfer.class);
            } catch (Exception e) {
                return null;
            }
        }

        Xfer setSpoofMac(final String mac) {
            this.spoofMac = mac;
            return this;
        }
    }

    private static boolean enabled() {
        return WholeHouse.isRpi();
    }

}
