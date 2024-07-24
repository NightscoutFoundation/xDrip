package com.eveningoutpost.dexdrip.cloud.backup;


import static com.eveningoutpost.dexdrip.models.JoH.readLine;
import static com.eveningoutpost.dexdrip.models.JoH.roundDouble;
import static com.eveningoutpost.dexdrip.xdrip.gs;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * JamOrHam
 * Backup file header metadata data and handler class
 */

public class BackupMetaData {

    private static final String title = "xDrip";
    private static final String type = "backup";
    private static final int version = 1;
    final String OB1 = "5861bdb93f5ba8effa43c8ff7f26c3be";
    static final String ID = String.format(Locale.US, "%s-%s-v%d", title, type, version);

    public volatile String id;
    public volatile String timestamp;
    public volatile String sourceDevice;
    public volatile String ob2;
    public volatile String exp3;
    public volatile String exp4;
    public volatile Exception exception;
    public volatile String displayName;
    public volatile long size;
    public volatile boolean getMetaDataOnly;
    public volatile boolean successResult;

    public boolean populateFromInputStream(final InputStream inputStream) {
        id = readLine(inputStream);
        timestamp = readLine(inputStream);
        sourceDevice = readLine(inputStream);
        ob2 = readLine(inputStream);
        exp3 = readLine(inputStream);
        exp4 = readLine(inputStream);
        return true;
    }

    public boolean writeToOutputStream(final OutputStream outputStream) {
        try {
            timestamp = "" + JoH.tsl();
            id = ID;
            writeString(id, outputStream);
            writeString(timestamp, outputStream);
            writeString(sourceDevice, outputStream);
            writeString(ob2, outputStream);
            writeString(exp3, outputStream);
            writeString(exp4, outputStream);
        } catch (IOException e) {
            exception = e;
            return false;
        }
        return true;
    }

    private void writeString(final String string, final OutputStream outputStream) throws IOException {
        outputStream.write(((string != null ? string : "") + "\n").getBytes(StandardCharsets.UTF_8));
    }

    // Todo i18n
    public String getTimeStampString() {
        try {
            return timestamp != null ? JoH.dateTimeText(Long.parseLong(timestamp)) : gs(R.string.none);
        } catch (Exception e) {
            return gs(R.string.invalid_time);
        }
    }

    public String getTimeSinceString() {
        try {
            return timestamp != null ? gs(R.string.format_string_ago, JoH.niceTimeScalar(JoH.msSince(Long.parseLong(timestamp)))) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public double getSizeInMb() {
        return roundDouble(size / (1024*1024d),2);
    }

    public boolean looksOkay() {
        return id != null && id.equals(ID);
    }

}
