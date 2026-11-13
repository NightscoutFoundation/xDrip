package xrwd.li3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import xrwd.li3.data.Lifecycle;
import xrwd.li3.nfc.PatchInfo;
import xrwd.li3.pairing.ReceiverId;
import xrwd.li3.util.Hex;

/**
 * Tests for {@link Li3SensorState}: what is kept about one sensor between connections. Vectors are from
 * RoundWhiteDiscKit 7dd6396 {@code *SensorStateLoaderTests.swift}; the fields xDrip adds beyond it are checked in
 * the round trip.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
@RunWith(RobolectricTestRunner.class)
public class Li3SensorStateTest {

    private static final String SERIAL = "0RRC989AQ";
    private static final String ADDRESS = "CC:22:DF:B8:F9:58";
    private static final byte[] PIN = {0x32, 0x25, (byte) 0xec, 0x72};
    private static final String KAUTH_HEX = "00112233445566778899aabbccddeeff";
    private static final String ACTIVE = "00a50001000200010060541e020401040c04305252433938394151c6ca";
    private static final String SENSOR_KEY_HEX = "04" + "11111111111111111111111111111111111111111111111111111111111111111111111111111111"
            + "111111111111111111111111111111111111111111111111";

    // =================================================== fromJson ====================================================

    /** *SensorStateLoaderTests.swift:5-20: a hex PIN and a little-endian receiver ID are read back as stored. */
    @Test
    public void fromJson_hexPin_readsFields() {
        // :: Setup
        String json = "{\"serialNumber\":\"0RRC989AQ\",\"bleAddress\":\"CC:22:DF:B8:F9:58\",\"blePIN\":\"3225ec72\","
                + "\"receiverID\":\"78830d6f\",\"lastGlucoseLifeCount\":1073,\"lastGlucoseMgDL\":151}";

        // :: Act
        Li3SensorState state = Li3SensorState.fromJson(json);

        // :: Verify
        assertThat(state.serial()).isEqualTo(SERIAL);
        assertThat(state.bleAddress()).isEqualTo(ADDRESS);
        assertThat(Hex.of(state.blePin())).isEqualTo("3225ec72");
        assertThat(state.receiverId().value()).isEqualTo(0x6f0d8378L);
        assertThat(state.receiverId().littleEndianHex()).isEqualTo("78830d6f");
        assertThat(state.lastLifeCount()).isEqualTo(1073);
        assertThat(state.lastMgdl()).isEqualTo(151);
    }

    /** *SensorStateLoaderTests.swift:29-35: a PIN that is not 4 bytes is refused. */
    @Test
    public void fromJson_wrongPinSize_throws() {
        // :: Setup
        String json = "{\"serialNumber\":\"0RRC989AQ\",\"bleAddress\":\"CC:22:DF:B8:F9:58\",\"blePIN\":\"3225ec\"}";

        // :: Act
        ThrowingRunnable load = () -> Li3SensorState.fromJson(json);

        // :: Verify
        assertThrows(IllegalArgumentException.class, load);
    }

    /** Text that is not JSON is refused with the same exception as a bad field, not a JSON-library one. */
    @Test
    public void fromJson_notJson_throws() {
        // :: Act
        ThrowingRunnable load = () -> Li3SensorState.fromJson("not json");

        // :: Verify
        assertThrows(IllegalArgumentException.class, load);
    }

    /** *SensorStateLoaderTests.swift:88-97: without a stored cycle the default warm-up applies, and no wear end. */
    @Test
    public void lifecycle_noStoredCycle_usesDefaultWarmup() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        Lifecycle lifecycle = state.lifecycle();

        // :: Verify
        assertThat(state.warmupMinutes()).isNull();
        assertThat(state.wearMinutes()).isNull();
        assertThat(lifecycle.usable(Lifecycle.DEFAULT_WARMUP_MINUTES - 1)).isFalse();
        assertThat(lifecycle.usable(Lifecycle.DEFAULT_WARMUP_MINUTES)).isTrue();
        assertThat(lifecycle.usable(1_000_000)).isTrue();
    }

    // ==================================================== toJson =====================================================

    /** *SensorStateLoaderTests.swift:37-56: every field, including the ones xDrip adds, survives a round trip. */
    @Test
    public void toJson_allFields_roundTrips() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.setReceiverId(new ReceiverId(0x6f0d8378L));
        state.setFold(ReceiverId.Fold.MULTI_SENSOR_APP);
        state.setActivationMs(1_761_800_000_000L);
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(ACTIVE)));
        state.setIdentityLabel("a");
        state.setKAuth(Hex.toBytes(KAUTH_HEX));
        state.updateLastGlucose(1073, 151);
        state.setRejectedLifeCounts(new HashSet<>(Arrays.asList(1070, 1071)));
        state.setSensorKey(Hex.toBytes(SENSOR_KEY_HEX));

        // :: Act
        String json = state.toJson();
        Li3SensorState restored = Li3SensorState.fromJson(json);

        // :: Verify
        assertThat(json).contains("\"blePIN\":\"3225ec72\"");
        assertThat(json).contains("\"receiverID\":\"78830d6f\"");
        assertThat(json).contains("\"lastGlucoseLifeCount\":1073");
        assertThat(json).contains("\"lastGlucoseMgDL\":151");
        assertThat(restored.serial()).isEqualTo(SERIAL);
        assertThat(restored.bleAddress()).isEqualTo(ADDRESS);
        assertThat(restored.blePin()).isEqualTo(PIN);
        assertThat(restored.receiverId()).isEqualTo(new ReceiverId(0x6f0d8378L));
        assertThat(restored.fold()).isEqualTo(ReceiverId.Fold.MULTI_SENSOR_APP);
        assertThat(restored.activationMs()).isEqualTo(1_761_800_000_000L);
        assertThat(restored.warmupMinutes()).isEqualTo(60);
        assertThat(restored.wearMinutes()).isEqualTo(21600);
        assertThat(restored.productType()).isEqualTo(4);
        assertThat(restored.generation()).isEqualTo(1);
        assertThat(restored.region()).isEqualTo(PatchInfo.Region.US);
        assertThat(restored.identityLabel()).isEqualTo("a");
        assertThat(Hex.of(restored.kAuth())).isEqualTo(KAUTH_HEX);
        assertThat(restored.lastLifeCount()).isEqualTo(1073);
        assertThat(restored.lastMgdl()).isEqualTo(151);
        assertThat(restored.rejectedLifeCounts()).containsExactly(1070, 1071);
        assertThat(Hex.of(restored.sensorKey())).isEqualTo(SENSOR_KEY_HEX);
    }

    /** A freshly activated sensor with only the required fields round-trips with the rest left unknown. */
    @Test
    public void toJson_requiredOnly_roundTripsWithUnknowns() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        Li3SensorState restored = Li3SensorState.fromJson(state.toJson());

        // :: Verify
        assertThat(restored.blePin()).isEqualTo(PIN);
        assertThat(restored.receiverId()).isNull();
        assertThat(restored.fold()).isNull();
        assertThat(restored.activationMs()).isEqualTo(0L);
        assertThat(restored.region()).isNull();
        assertThat(restored.kAuth()).isNull();
        assertThat(restored.lastLifeCount()).isNull();
        assertThat(restored.rejectedLifeCounts()).isEmpty();
        assertThat(restored.sensorKey()).isNull();
    }

    // ================================================= Last glucose ==================================================

    /** *SensorStateLoaderTests.swift:58-72: the last reading is updated and nothing else changes. */
    @Test
    public void updateLastGlucose_keepsIdentity() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.updateLastGlucose(1080, 149);

        // :: Verify
        assertThat(state.serial()).isEqualTo(SERIAL);
        assertThat(state.blePin()).isEqualTo(PIN);
        assertThat(state.lastLifeCount()).isEqualTo(1080);
        assertThat(state.lastMgdl()).isEqualTo(149);
    }

    // ============================================= Rejected life counts ==============================================

    /** A refused life count more than a day older than the newest one is dropped, so the set stays small. */
    @Test
    public void addRejectedLifeCount_dayOlderThanNewest_dropped() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.addRejectedLifeCount(1000);
        state.addRejectedLifeCount(1001);
        state.addRejectedLifeCount(2441);

        // :: Verify
        assertThat(state.rejectedLifeCounts()).containsExactly(1001, 2441);
    }

    /** A refused life count older than the newest one, but within a day, is kept. */
    @Test
    public void addRejectedLifeCount_olderWithinDay_kept() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.addRejectedLifeCount(2441);

        // :: Act
        state.addRejectedLifeCount(1001);

        // :: Verify
        assertThat(state.rejectedLifeCounts()).containsExactly(1001, 2441);
    }

    /** The refused life counts handed out are a copy, so a reader is not hit by a later add on another thread. */
    @Test
    public void rejectedLifeCounts_laterAdd_notInEarlierCopy() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.addRejectedLifeCount(1001);
        Set<Integer> before = state.rejectedLifeCounts();

        // :: Act
        state.addRejectedLifeCount(1002);

        // :: Verify
        assertThat(before).containsExactly(1001);
    }

    // ================================================== Sensor key ===================================================

    /** A sensor key that is not an uncompressed 65-byte point is refused, so a broken one is never pinned. */
    @Test
    public void setSensorKey_wrongSize_throws() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        ThrowingRunnable set = () -> state.setSensorKey(new byte[64]);

        // :: Verify
        assertThrows(IllegalArgumentException.class, set);
        assertThat(state.sensorKey()).isNull();
    }

    // ================================================= Sensor cycle ==================================================

    /** *SensorStateLoaderTests.swift:74-86: the cycle read over NFC is kept, so a reconnect knows it. */
    @Test
    public void applySensorCycle_persistsAndRestores() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.applySensorCycle(PatchInfo.parse(Hex.toBytes(ACTIVE)));
        Li3SensorState restored = Li3SensorState.fromJson(state.toJson());

        // :: Verify
        assertThat(restored.warmupMinutes()).isEqualTo(60);
        assertThat(restored.wearMinutes()).isEqualTo(21600);
        assertThat(restored.lifecycle().usable(21599)).isTrue();
        assertThat(restored.lifecycle().usable(21600)).isFalse();
    }

    // ================================================= Backfill hole =================================================

    /** A first hole opens from its oldest minute to its newest, and has never been asked for. */
    @Test
    public void openHole_noneOpen_opensUnasked() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.openHole(1061, 1070);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1061);
        assertThat(state.backfillTo()).isEqualTo(1070);
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** A hole that ends before it starts holds no minute, and opens nothing. */
    @Test
    public void openHole_endBeforeStart_opensNothing() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.openHole(1001, 1000);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(0);
        assertThat(state.backfillTo()).isEqualTo(0);
    }

    /** A hole that starts earlier than the open one moves its oldest end back and starts the asking over. */
    @Test
    public void openHole_olderStart_widensAndForgetsAsk() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);

        // :: Act
        state.openHole(1050, 1065);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1050);
        assertThat(state.backfillTo()).isEqualTo(1070);
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** A hole that starts later than the open one only moves its newest end, and the last ask stands. */
    @Test
    public void openHole_laterStart_widensEndAndKeepsAsk() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);

        // :: Act
        state.openHole(1080, 1085);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1061);
        assertThat(state.backfillTo()).isEqualTo(1085);
        assertThat(state.backfillAskedAt()).isEqualTo(1073);
    }

    /**
     * A minute behind the front history has already answered for does not move the front back or start the asking
     * over: a reading that is refused keeps the last stored minute where it was, and must not undo the history.
     */
    @Test
    public void openHole_behindAnsweredFront_keepsFrontAndAsk() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1080);
        state.markHoleAskedAt(1083);
        state.advanceHoleFrom(1071);

        // :: Act
        state.openHole(1061, 1085);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1071);
        assertThat(state.backfillTo()).isEqualTo(1085);
        assertThat(state.backfillAskedAt()).isEqualTo(1083);
    }

    /** After a hole has all come back, its minutes are not owed again: a new hole starts after them. */
    @Test
    public void openHole_afterHoleCameBack_startsAfterIt() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.advanceHoleFrom(1071);

        // :: Act
        state.openHole(1061, 1075);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1071);
        assertThat(state.backfillTo()).isEqualTo(1075);
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** After a hole is closed, whether it came back or was given up on, the same minutes open nothing. */
    @Test
    public void openHole_sameMinutesAfterClose_opensNothing() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.closeHole();

        // :: Act
        state.openHole(1061, 1070);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(0);
        assertThat(state.backfillTo()).isEqualTo(0);
    }

    /** Minutes that came back move the oldest end of the hole forward, and the ask stands. */
    @Test
    public void advanceHoleFrom_insideHole_movesStart() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1080);
        state.markHoleAskedAt(1083);

        // :: Act
        state.advanceHoleFrom(1071);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1071);
        assertThat(state.backfillTo()).isEqualTo(1080);
        assertThat(state.backfillAskedAt()).isEqualTo(1083);
    }

    /** A minute before the oldest end of the hole leaves it where it is. */
    @Test
    public void advanceHoleFrom_beforeStart_unchanged() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1080);

        // :: Act
        state.advanceHoleFrom(1050);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(1061);
        assertThat(state.backfillTo()).isEqualTo(1080);
    }

    /** Once the oldest end passes the newest, nothing is owed and the hole closes. */
    @Test
    public void advanceHoleFrom_pastEnd_closes() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);

        // :: Act
        state.advanceHoleFrom(1071);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(0);
        assertThat(state.backfillTo()).isEqualTo(0);
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** With no hole open, history that comes back opens none. */
    @Test
    public void advanceHoleFrom_noHole_staysClosed() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.advanceHoleFrom(1071);

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(0);
        assertThat(state.backfillTo()).isEqualTo(0);
    }

    /** Closing a hole forgets its minutes and its last ask. */
    @Test
    public void closeHole_open_forgetsAll() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);

        // :: Act
        state.closeHole();

        // :: Verify
        assertThat(state.backfillFrom()).isEqualTo(0);
        assertThat(state.backfillTo()).isEqualTo(0);
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** An ask is only recorded while a hole is open, so a later hole starts unasked. */
    @Test
    public void markHoleAskedAt_noHole_ignored() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);

        // :: Act
        state.markHoleAskedAt(1073);
        state.openHole(1080, 1085);

        // :: Verify
        assertThat(state.backfillAskedAt()).isEqualTo(-1);
    }

    /** An open hole and its last ask survive a restart, so the chase goes on where it was. */
    @Test
    public void toJson_openHole_roundTrips() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.markHoleAskedAt(1073);

        // :: Act
        Li3SensorState restored = Li3SensorState.fromJson(state.toJson());

        // :: Verify
        assertThat(restored.backfillFrom()).isEqualTo(1061);
        assertThat(restored.backfillTo()).isEqualTo(1070);
        assertThat(restored.backfillAskedAt()).isEqualTo(1073);
    }

    /** The minutes already settled survive a restart, so a refused reading after it does not ask for them again. */
    @Test
    public void toJson_settledMinutes_roundTrip() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.closeHole();

        // :: Act
        Li3SensorState restored = Li3SensorState.fromJson(state.toJson());
        restored.openHole(1061, 1070);

        // :: Verify
        assertThat(restored.backfillFrom()).isEqualTo(0);
    }

    /** With no hole open the stored state carries no backfill keys, so it stays as small as before. */
    @Test
    public void toJson_noHole_omitsBackfillKeys() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.openHole(1061, 1070);
        state.closeHole();

        // :: Act
        String json = state.toJson();

        // :: Verify
        assertThat(json).doesNotContain("backfill");
    }

    /** A state stored before holes were kept reads back with none open. */
    @Test
    public void fromJson_noBackfillKeys_noHole() {
        // :: Setup
        String json = new Li3SensorState(SERIAL, ADDRESS, PIN).toJson();

        // :: Act
        Li3SensorState restored = Li3SensorState.fromJson(json);

        // :: Verify
        assertThat(restored.backfillFrom()).isEqualTo(0);
        assertThat(restored.backfillTo()).isEqualTo(0);
        assertThat(restored.backfillAskedAt()).isEqualTo(-1);
    }

    // =================================================== toString ====================================================

    /** The PIN and kAuth never reach a log line through {@code toString()}. */
    @Test
    public void toString_omitsSecrets() {
        // :: Setup
        Li3SensorState state = new Li3SensorState(SERIAL, ADDRESS, PIN);
        state.setKAuth(Hex.toBytes(KAUTH_HEX));

        // :: Act
        String text = state.toString();

        // :: Verify
        assertThat(text).contains(SERIAL);
        assertThat(text.toLowerCase()).doesNotContain("3225ec72");
        assertThat(text.toLowerCase()).doesNotContain(KAUTH_HEX);
    }
}
