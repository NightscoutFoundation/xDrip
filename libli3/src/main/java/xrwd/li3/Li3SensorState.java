package xrwd.li3;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import xrwd.li3.data.Lifecycle;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.pairing.ReceiverId;
import xrwd.li3.util.Hex;

/**
 * What is kept about one sensor between connections: its address and PIN from activation, the cycle read over NFC,
 * the receiver ID it was started or taken over with, the cached kAuth, the last reading and the minutes of history
 * the sensor still owes. The PIN and kAuth are secrets: {@link #toString()} leaves them out, and the JSON is for
 * private app storage only. It is shared between the Bluetooth worker and the status screen, so every access holds its
 * lock. Ported from RoundWhiteDiscKit {@code *SensorState.swift} and {@code *SensorStateLoader.swift} (MIT); the JSON
 * keys it shares with them are kept.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class Li3SensorState {

    private static final int PIN_SIZE = 4;
    private static final int KAUTH_SIZE = 16;
    private static final int SENSOR_KEY_SIZE = 65;
    /** Life counts refused for their value or time, kept behind the newest one for as long as history can reach. */
    private static final int KEEP_REJECTED_MINUTES = 24 * 60;

    private final String serial;
    private final String bleAddress;
    private final byte[] blePin;
    private ReceiverId receiverId;
    private ReceiverId.Fold fold;
    /** 0 when unknown. */
    private long activationMs;
    private Integer warmupMinutes;
    private Integer wearMinutes;
    private Integer productType;
    private Integer generation;
    private PatchInfo.Region region;
    private String identityLabel;
    private byte[] kAuth;
    /** The static key from the sensor's certificate at its first pairing; public, not a secret. */
    private byte[] sensorKey;
    private Integer lastLifeCount;
    private Integer lastMgdl;
    private Set<Integer> rejectedLifeCounts = new HashSet<>();
    /** Oldest minute the sensor still owes history for; 0 when nothing is outstanding. */
    private int backfillFrom;
    /** Newest minute of the outstanding hole, on the five-minute grid history is written on. */
    private int backfillTo;
    /** Life count the last ask for this hole went out at; -1 when it has never been asked for. */
    private int backfillAskedAt = -1;
    /**
     * Newest minute no longer owed: history came back for it or the sensor had its chances. A refused reading leaves
     * the last stored minute behind, so without this every one would open those minutes again; 0 when none.
     */
    private int settledThrough;

    /** @throws IllegalArgumentException unless the PIN is 4 bytes */
    public Li3SensorState(String serial, String bleAddress, byte[] blePin) {
        if (blePin == null || blePin.length != PIN_SIZE) {
            throw new IllegalArgumentException("BLE PIN must be " + PIN_SIZE + " bytes, got "
                    + (blePin == null ? "none" : blePin.length));
        }
        this.serial = serial;
        this.bleAddress = bleAddress;
        this.blePin = blePin.clone();
    }

    /** Keeps the warm-up, wear, product, generation and region the sensor reported over NFC. */
    public synchronized void applySensorCycle(PatchInfo info) {
        warmupMinutes = Math.max(0, info.warmupMinutes());
        wearMinutes = Math.max(0, info.wearMinutes());
        productType = info.productType();
        generation = info.generation();
        region = info.region();
    }

    public synchronized void updateLastGlucose(int lifeCount, int mgdl) {
        lastLifeCount = lifeCount;
        lastMgdl = mgdl;
    }

    /** The stored cycle; without one, the default warm-up and no wear end, as a reconnect without NFC has. */
    public synchronized Lifecycle lifecycle() {
        return new Lifecycle(warmupMinutes != null ? warmupMinutes : Lifecycle.DEFAULT_WARMUP_MINUTES,
                wearMinutes != null ? wearMinutes : Integer.MAX_VALUE);
    }

    public synchronized String toJson() {
        try {
            JSONObject json = new JSONObject()
                    .put("serialNumber", serial)
                    .put("bleAddress", bleAddress)
                    .put("blePIN", Hex.of(blePin))
                    .put("activationMs", activationMs);
            if (receiverId != null) {
                json.put("receiverID", receiverId.littleEndianHex());
            }
            if (fold != null) {
                json.put("fold", fold.name());
            }
            json.putOpt("warmupDurationMinutes", warmupMinutes);
            json.putOpt("wearDurationMinutes", wearMinutes);
            json.putOpt("productType", productType);
            json.putOpt("generation", generation);
            if (region != null) {
                json.put("region", region.name());
            }
            json.putOpt("identityLabel", identityLabel);
            if (kAuth != null) {
                json.put("kAuth", Hex.of(kAuth));
            }
            if (sensorKey != null) {
                json.put("sensorKey", Hex.of(sensorKey));
            }
            json.putOpt("lastGlucoseLifeCount", lastLifeCount);
            json.putOpt("lastGlucoseMgDL", lastMgdl);
            JSONArray rejected = new JSONArray();
            for (Integer lifeCount : rejectedLifeCounts) {
                rejected.put(lifeCount);
            }
            json.put("rejectedLifeCounts", rejected);
            if (settledThrough != 0) {
                json.put("historySettledThrough", settledThrough);
            }
            if (backfillFrom != 0) { // only while a hole is outstanding, so a settled state stays as small as it was
                json.put("backfillFrom", backfillFrom)
                        .put("backfillTo", backfillTo)
                        .put("backfillAskedAt", backfillAskedAt);
            }
            return json.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Cannot write sensor state", e);
        }
    }

    /** @throws IllegalArgumentException when the text is not a stored state, or the PIN or kAuth has the wrong size */
    public static Li3SensorState fromJson(String text) {
        try {
            JSONObject json = new JSONObject(text);
            Li3SensorState state = new Li3SensorState(json.getString("serialNumber"),
                    json.getString("bleAddress"), Hex.toBytes(json.getString("blePIN")));
            if (json.has("receiverID")) {
                state.receiverId = ReceiverId.fromLittleEndianHex(json.getString("receiverID"));
            }
            if (json.has("fold")) {
                state.fold = ReceiverId.Fold.valueOf(json.getString("fold"));
            }
            state.activationMs = json.optLong("activationMs", 0);
            state.warmupMinutes = optNonNegative(json, "warmupDurationMinutes");
            state.wearMinutes = optNonNegative(json, "wearDurationMinutes");
            state.productType = optInt(json, "productType");
            state.generation = optInt(json, "generation");
            if (json.has("region")) {
                state.region = PatchInfo.Region.valueOf(json.getString("region"));
            }
            state.identityLabel = json.has("identityLabel") ? json.getString("identityLabel") : null;
            if (json.has("kAuth")) {
                state.setKAuth(Hex.toBytes(json.getString("kAuth")));
            }
            if (json.has("sensorKey")) {
                state.setSensorKey(Hex.toBytes(json.getString("sensorKey")));
            }
            state.lastLifeCount = optInt(json, "lastGlucoseLifeCount");
            state.lastMgdl = optInt(json, "lastGlucoseMgDL");
            JSONArray rejected = json.optJSONArray("rejectedLifeCounts");
            if (rejected != null) {
                for (int i = 0; i < rejected.length(); i++) {
                    state.rejectedLifeCounts.add(rejected.getInt(i));
                }
            }
            state.backfillFrom = json.optInt("backfillFrom", 0);
            state.backfillTo = json.optInt("backfillTo", 0);
            state.backfillAskedAt = json.optInt("backfillAskedAt", -1);
            state.settledThrough = json.optInt("historySettledThrough", 0);
            return state;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Not a stored Li3 sensor state", e);
        }
    }

    private static Integer optInt(JSONObject json, String key) throws JSONException {
        return json.has(key) ? json.getInt(key) : null;
    }

    private static Integer optNonNegative(JSONObject json, String key) throws JSONException {
        Integer value = optInt(json, key);
        return value == null ? null : Math.max(0, value);
    }

    public String serial() {
        return serial;
    }

    public String bleAddress() {
        return bleAddress;
    }

    public byte[] blePin() {
        return blePin.clone();
    }

    public synchronized ReceiverId receiverId() {
        return receiverId;
    }

    public synchronized void setReceiverId(ReceiverId receiverId) {
        this.receiverId = receiverId;
    }

    public synchronized ReceiverId.Fold fold() {
        return fold;
    }

    public synchronized void setFold(ReceiverId.Fold fold) {
        this.fold = fold;
    }

    public synchronized long activationMs() {
        return activationMs;
    }

    public synchronized void setActivationMs(long activationMs) {
        this.activationMs = activationMs;
    }

    public synchronized Integer warmupMinutes() {
        return warmupMinutes;
    }

    public synchronized Integer wearMinutes() {
        return wearMinutes;
    }

    public synchronized Integer productType() {
        return productType;
    }

    public synchronized Integer generation() {
        return generation;
    }

    public synchronized PatchInfo.Region region() {
        return region;
    }

    public synchronized String identityLabel() {
        return identityLabel;
    }

    public synchronized void setIdentityLabel(String identityLabel) {
        this.identityLabel = identityLabel;
    }

    public synchronized byte[] kAuth() {
        return kAuth == null ? null : kAuth.clone();
    }

    /** @param kAuth 16 bytes, or null to forget it and force a first pair */
    public synchronized void setKAuth(byte[] kAuth) {
        if (kAuth != null && kAuth.length != KAUTH_SIZE) {
            throw new IllegalArgumentException("kAuth must be " + KAUTH_SIZE + " bytes");
        }
        this.kAuth = kAuth == null ? null : kAuth.clone();
    }

    public synchronized Integer lastLifeCount() {
        return lastLifeCount;
    }

    public synchronized Integer lastMgdl() {
        return lastMgdl;
    }

    /** @return the sensor's static key from its first pairing, {@code 04 || X || Y}, or null before one */
    public synchronized byte[] sensorKey() {
        return sensorKey == null ? null : sensorKey.clone();
    }

    /** @param sensorKey 65 bytes, or null to accept whatever key the next first pairing shows */
    public synchronized void setSensorKey(byte[] sensorKey) {
        if (sensorKey != null && sensorKey.length != SENSOR_KEY_SIZE) {
            throw new IllegalArgumentException("Sensor key must be " + SENSOR_KEY_SIZE + " bytes");
        }
        this.sensorKey = sensorKey == null ? null : sensorKey.clone();
    }

    /** Life counts whose reading was refused, so a backfill does not store them again; a copy. */
    public synchronized Set<Integer> rejectedLifeCounts() {
        return Collections.unmodifiableSet(new HashSet<>(rejectedLifeCounts));
    }

    public synchronized void setRejectedLifeCounts(Set<Integer> rejectedLifeCounts) {
        this.rejectedLifeCounts = new HashSet<>(rejectedLifeCounts);
    }

    /** Adds a refused life count and drops those more than a day older than the newest. */
    public synchronized void addRejectedLifeCount(int lifeCount) {
        rejectedLifeCounts.add(lifeCount);
        int oldest = Collections.max(rejectedLifeCounts) - KEEP_REJECTED_MINUTES;
        Iterator<Integer> it = rejectedLifeCounts.iterator();
        while (it.hasNext()) {
            if (it.next() < oldest) {
                it.remove();
            }
        }
    }

    /** @return the oldest minute the sensor still owes history for, 0 when nothing is outstanding */
    public synchronized int backfillFrom() {
        return backfillFrom;
    }

    /** @return the newest minute of the outstanding hole, 0 when nothing is outstanding */
    public synchronized int backfillTo() {
        return backfillTo;
    }

    /** @return the life count the last ask went out at, or -1 when this hole has never been asked for */
    public synchronized int backfillAskedAt() {
        return backfillAskedAt;
    }

    /**
     * Opens a hole the sensor owes history for, or widens the one already open. Minutes already settled are not owed
     * again, so the hole starts after them. Only a new oldest minute restarts the chase, so minutes that have been
     * asked about keep the ask they had. A hole that ends before it starts holds no minute and opens nothing.
     *
     * @param from the first minute that is missing
     * @param to   the newest minute of the hole, on the grid history is written on
     */
    public synchronized void openHole(int from, int to) {
        from = Math.max(from, settledThrough + 1);
        if (to < from) {
            return;
        }
        if (backfillFrom == 0 || from < backfillFrom) {
            backfillFrom = from;
            backfillAskedAt = -1; // these minutes have never been asked for
        }
        backfillTo = Math.max(backfillTo, to);
    }

    /**
     * Moves the oldest end of the hole forward over the minutes the sensor answered with a value, so the next ask
     * starts at the first slot it left empty. Closes the hole once the front passes its end.
     */
    public synchronized void advanceHoleFrom(int lifeCount) {
        if (backfillFrom == 0) {
            return;
        }
        if (lifeCount > backfillFrom) {
            backfillFrom = lifeCount;
            settledThrough = Math.max(settledThrough, lifeCount - 1);
        }
        if (backfillFrom > backfillTo) {
            closeHole();
        }
    }

    /** Says the sensor owes us nothing more, whether its history came back or it has had its chances. */
    public synchronized void closeHole() {
        settledThrough = Math.max(settledThrough, backfillTo);
        backfillFrom = 0;
        backfillTo = 0;
        backfillAskedAt = -1;
    }

    /** Records that the outstanding hole was asked for at this life count. */
    public synchronized void markHoleAskedAt(int lifeCount) {
        if (backfillFrom != 0) {
            backfillAskedAt = lifeCount;
        }
    }

    @Override
    public synchronized String toString() {
        return "Li3SensorState(serial=" + serial + ", address=" + bleAddress
                + ", receiver=" + (receiverId == null ? "none" : receiverId.littleEndianHex())
                + ", fold=" + fold + ", region=" + region + ", warmup=" + warmupMinutes + ", wear=" + wearMinutes
                + ", identity=" + identityLabel + ", paired=" + (kAuth != null) + ", last=" + lastLifeCount
                + (backfillFrom == 0 ? "" : ", backfill=" + backfillFrom + " to " + backfillTo) + ")";
    }
}
