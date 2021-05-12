package com.eveningoutpost.dexdrip.watch.thinjam;

import com.eveningoutpost.dexdrip.Models.JoH;
import com.eveningoutpost.dexdrip.UtilityModels.Constants;
import com.eveningoutpost.dexdrip.UtilityModels.PersistentStore;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.BaseTx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.PushRx;
import com.eveningoutpost.dexdrip.watch.thinjam.messages.SetTimeTx;
import com.google.gson.annotations.Expose;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Locale;

import static com.eveningoutpost.dexdrip.Models.JoH.msSince;


public class BlueJayInfo extends BaseTx {

    private static final String BLUEJAY_INFO_PERSIST = "BLUEJAY_INFO_PERSIST-";
    private static final HashMap<String, BlueJayInfo> infos = new HashMap<>();

    private static final int STATUS1_BIT_CORE_PRESENT = 0;

    private static final int STATUS_BIT_CHARGER_CONNECTED = 6;

    @Expose
    public final String mac;

    @Expose
    public int thinJamVersion;
    @Expose
    public int buildNumber;
    @Expose
    public int coreNumber;
    @Expose
    public long uptime;

    @Expose
    public int state;
    @Expose
    public int status;
    @Expose
    public byte trend;
    @Expose
    public int glucose;
    @Expose
    public int bitfield;
    @Expose
    public long bitfield1;
    @Expose
    public int connectionAttempts;
    @Expose
    public int successfulReplies;
    @Expose
    public long lastReadingTime;
    @Expose
    public long timeMismatch;
    @Expose
    public long timeLastUpdated;
    @Expose
    public long displayLastUpdated;
    @Expose
    public long lastStatus1;
    @Expose
    public long lastStatus2;
    @Expose
    public long lastStatus3;
    @Expose
    public int batteryPercent;
    @Expose
    public int fragmentationLevel = -1;
    @Expose
    public int fragmentationCounter = -1;
    @Expose
    public int fragmentationIndex = -1;

    BlueJayInfo(final String mac) {
        this.mac = mac;
    }

    public boolean status1Due() {
        return (JoH.tsl() - lastStatus1) > (Constants.MINUTE_IN_MS * 120);
    }

    public void invalidateStatus() {
        lastStatus1 = 0;
        lastStatus2 = 0;
        lastStatus3 = 0;
        persistentSave();
    }

    public boolean status2Due() {
        return (JoH.tsl() - lastStatus2) > (Constants.SECOND_IN_MS * 60);
    }

    public boolean isTimeSetDue() {
        return msSince(timeLastUpdated) > (Constants.MINUTE_IN_MS * 30);
    }

    public void invalidateTime() {
        timeLastUpdated = 0;
        persistentSave();
    }

    public boolean isDisplayUpdatedue() {
        return msSince(displayLastUpdated) > (Constants.MINUTE_IN_MS * 10);
    }

    public void displayUpdated() {
        displayLastUpdated = JoH.tsl();
        persistentSave();
    }

    public void parseStatus1(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (packet.length >= 17) {
            thinJamVersion = getUnsignedByte();
            uptime = getUnsignedInt();
            buildNumber = (int) getUnsignedInt() & 0xFFFFFF;
            coreNumber = (int) getUnsignedInt() & 0xFFFFFF;
            bitfield1 = getUnsignedInt();
            lastStatus1 = JoH.tsl();
            persistentSave();
        } else {
            // packet too short
        }
    }


    public void parseStatus2(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (packet.length >= 12) {

            lastReadingTime = getUnsignedInt();
            glucose = getUnsignedShort();
            status = getUnsignedByte();
            state = getUnsignedByte();
            trend = data.get();
            bitfield = getUnsignedByte();
            connectionAttempts = getUnsignedByte();
            successfulReplies = getUnsignedByte();

            if (packet.length > 12) {
                batteryPercent = getUnsignedByte() & 0x7f;
            } else {
                batteryPercent = -1;
            }

            lastStatus2 = JoH.tsl();
            persistentSave();
        } else {
            // packet too short
        }
    }

    public void parseStatus3(final byte[] packet) {
        data = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        if (packet.length >= 5) {
            fragmentationCounter = getUnsignedShort();
            fragmentationIndex = getUnsignedShort();
            fragmentationLevel = getUnsignedByte();
            lastStatus3 = JoH.tsl();
            persistentSave();
        } else {
            // packet too short
        }
    }

    public void processPushRx(final PushRx pushRx) {
        switch (pushRx.type) {
            case Charging:
                setChargerConnected(pushRx.value != 0);
                break;
        }
        persistentSave();
    }

    public void parseSetTime(SetTimeTx reply, SetTimeTx outgoing) {
        timeLastUpdated = JoH.tsl();
        timeMismatch = outgoing.getTimestamp() - reply.getTimestamp();
    }

    public long getTimestamp() {
        if (lastReadingTime != 0) {
            return lastStatus2 - (lastReadingTime * Constants.SECOND_IN_MS);
        } else {
            return -1;
        }
    }

    public long getUptimeTimeStamp() {
        return uptime * Constants.SECOND_IN_MS;
    }

    public Double getTrend() {
        return trend != 127 ? ((double) trend) / 10d : Double.NaN;
    }


    public void persistentSave() {
        PersistentStore.setString(BLUEJAY_INFO_PERSIST + mac, JoH.defaultGsonInstance().toJson(this));
    }

    public int captureSuccessPercent() {
        if (connectionAttempts == 0) return -1;
        return (int) (((double) successfulReplies / (double) connectionAttempts) * 100);
    }

    public boolean hasCoreModule() {
        return isStatus1BitSet(STATUS1_BIT_CORE_PRESENT);
    }

    public boolean isChargerConnected() {
        return isStatusBitSet(STATUS_BIT_CHARGER_CONNECTED);
    }

    public void setChargerConnected(boolean connected) {
        setStatusBit(STATUS_BIT_CHARGER_CONNECTED, connected);
    }

    public String getMetrics() {
        return String.format(Locale.US, "%d-%d-%d-%d-%d-%d", buildNumber, coreNumber, captureSuccessPercent(), connectionAttempts, bitfield, bitfield1);
    }

    private boolean isStatus1BitSet(final int bit) {
        return (bitfield1 & (1 << bit)) != 0;
    }

    private boolean isStatusBitSet(final int bit) {
        return (bitfield & (1 << bit)) != 0;
    }

    private void setStatusBit(final int bit, final boolean set) {
        if (set) {
            bitfield |= (1 << bit);
        } else {
            bitfield &= ~(1 << bit);
        }
    }

    public static BlueJayInfo getInfo(final String mac) {
        synchronized (infos) {
            BlueJayInfo info = infos.get(mac);
            if (info == null) {
                final BlueJayInfo tmp = JoH.defaultGsonInstance().fromJson(PersistentStore.getString(BLUEJAY_INFO_PERSIST + mac), BlueJayInfo.class);
                info = tmp != null ? tmp : new BlueJayInfo(mac);
                infos.put(mac, info);
            }
            return info;
        }
    }
}


