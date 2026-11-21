package xrwd.li3.ble;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import xrwd.li3.util.Hex;

/**
 * Tests for the Li3 GATT map and BLE framing: offset-prefixed writes, sequence-prefixed notifies, and the
 * two-part glucose notify. Ported from RoundWhiteDiscKit 7dd6396 {@code BleFramingTests.swift} and
 * {@code DataPlaneTests.swift:63-91}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class FramingTest {

    /** BleFramingTests.swift:8-18: the 162-byte phone certificate from the pairing transcript. */
    private static final byte[] PHASE1_PHONE_162 = Hex.toBytes(""
            + "03030102030405060708090a0b0c0d0e0f10000161897655010000000000000000048242"
            + "be33f1a330880112fa62cc4842a43d1204922ad201d8775bb226f611f75b0ef3d5bc6cc4"
            + "317caa457584ab003f1712336089d3a4f29838ed0dc666deaea2d65a00dfff5d7bcae216"
            + "55e302e3458e774daaaaca87af75f1b87884b18d4ce875d0d108c903a834471a4ff674b2"
            + "d30bcba062373014b7786e4437b177aec3c8");

    /** BleFramingTests.swift:21-30: the 140-byte sensor certificate from the pairing transcript. */
    private static final byte[] PHASE2_SENSOR_140 = Hex.toBytes(""
            + "0157259416c6007ae00550043e1f46f25d44b3d72a8c37dcfebc7c339ed01fc5668a63874580"
            + "84ac9cafebe7438b649f76b81eeca9343287da162b07c5c07362997e40e13035df14cdf3d5d8"
            + "1a5fe9ffc84de8c8ecc33be609bf0d8ebf97bed6bae899e181bb32960c81e99d5aab30b2fc7a"
            + "77b0d45eff9885d46722371cd9375e14352d890f60fa1e7159e6");

    // ===================================================== Gatt ======================================================

    /** Every characteristic shares the Li3 base UUID; only the 16-bit slot differs. */
    @Test
    public void gatt_uuids_useLi3Base() {
        // :: Act
        UUID glucose = Gatt.GLUCOSE_DATA;

        // :: Verify
        assertThat(glucose).isEqualTo(UUID.fromString("0898177A-EF89-11E9-81B4-2A2AE2DBCCE4"));
        assertThat(Gatt.SERVICE).isEqualTo(UUID.fromString("089810CC-EF89-11E9-81B4-2A2AE2DBCCE4"));
        assertThat(Gatt.SEC_COMMAND_RESPONSE).isEqualTo(UUID.fromString("08982198-EF89-11E9-81B4-2A2AE2DBCCE4"));
    }

    /** Only the three security characteristics carry handshake traffic. */
    @Test
    public void gatt_isSecurity_onlySecurityCharacteristics() {
        // :: Act
        boolean cert = Gatt.isSecurity(Gatt.SEC_CERT_DATA);
        boolean glucose = Gatt.isSecurity(Gatt.GLUCOSE_DATA);

        // :: Verify
        assertThat(cert).isTrue();
        assertThat(Gatt.isSecurity(Gatt.SEC_CHALLENGE_DATA)).isTrue();
        assertThat(Gatt.isSecurity(Gatt.SEC_COMMAND_RESPONSE)).isTrue();
        assertThat(glucose).isFalse();
        assertThat(Gatt.isSecurity(Gatt.PATCH_STATUS)).isFalse();
    }

    // ================================================ WriteFragmenter ================================================

    /** BleFramingTests.swift:33-50: the 162-byte cert goes out as nine offset-prefixed fragments of up to 18 bytes. */
    @Test
    public void writeFragmenter_phoneCert_nineOffsetFragments() {
        // :: Act
        List<byte[]> fragments = WriteFragmenter.split(PHASE1_PHONE_162);

        // :: Verify
        assertThat(fragments).hasSize(9);
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        for (int i = 0; i < fragments.size(); i++) {
            byte[] fragment = fragments.get(i);
            assertThat((fragment[0] & 0xff) | ((fragment[1] & 0xff) << 8)).isEqualTo(i * 18);
            assertThat(fragment.length - 2).isAtMost(18);
            joined.write(fragment, 2, fragment.length - 2);
        }
        assertThat(Hex.of(Arrays.copyOfRange(fragments.get(0), 2, 20)))
                .isEqualTo("03030102030405060708090a0b0c0d0e0f10");
        assertThat(joined.toByteArray()).isEqualTo(PHASE1_PHONE_162);
    }

    // =============================================== NotifyReassembler ===============================================

    /** BleFramingTests.swift:52-68: the sensor cert reassembles from 19-byte notifies behind a sequence byte. */
    @Test
    public void notifyReassembler_sensorCert_reassembles() throws IOException {
        // :: Setup
        NotifyReassembler reassembler = new NotifyReassembler(140);
        boolean complete = false;

        // :: Act
        for (int offset = 0, seq = 0; offset < PHASE2_SENSOR_140.length; offset += 19, seq++) {
            assertThat(complete).isFalse();
            complete = reassembler.add(notify(seq, Arrays.copyOfRange(PHASE2_SENSOR_140, offset,
                    Math.min(offset + 19, PHASE2_SENSOR_140.length))));
        }

        // :: Verify
        assertThat(complete).isTrue();
        assertThat(reassembler.result()).isEqualTo(PHASE2_SENSOR_140);
    }

    /** BleFramingTests.swift:70-76: a skipped sequence number is a lost notify and fails the message. */
    @Test
    public void notifyReassembler_sequenceGap_throws() throws IOException {
        // :: Setup
        NotifyReassembler reassembler = new NotifyReassembler(4);
        reassembler.add(Hex.toBytes("00aa"));

        // :: Act
        ThrowingRunnable add = () -> reassembler.add(Hex.toBytes("02bb"));

        // :: Verify
        assertThrows(IOException.class, add);
    }

    /** BleFramingTests.swift:78-84: asking for the message before it is complete is an error, not a short read. */
    @Test
    public void notifyReassembler_resultBeforeComplete_throws() throws IOException {
        // :: Setup
        NotifyReassembler reassembler = new NotifyReassembler(5);
        reassembler.add(Hex.toBytes("00aabb"));

        // :: Act
        ThrowingRunnable result = reassembler::result;

        // :: Verify
        assertThrows(IllegalStateException.class, result);
    }

    /** Handshake messages have fixed sizes, so a notify that runs past the expected length is a protocol error. */
    @Test
    public void notifyReassembler_overrun_throws() throws IOException {
        // :: Setup
        NotifyReassembler reassembler = new NotifyReassembler(2);
        reassembler.add(Hex.toBytes("00aa"));

        // :: Act
        ThrowingRunnable add = () -> reassembler.add(Hex.toBytes("01bbcc"));

        // :: Verify
        assertThrows(IOException.class, add);
    }

    // ============================================ GlucoseNotifyAssembler =============================================

    /** DataPlaneTests.swift:69-74: the live 15-byte first fragment is held and joined with the 20-byte second. */
    @Test
    public void glucoseNotifyAssembler_liveFragments_joined() {
        // :: Setup
        GlucoseNotifyAssembler assembler = new GlucoseNotifyAssembler();
        byte[] prefix = Hex.toBytes("3c937752c72e2dfdcfe05a5dce315b");
        byte[] suffix = Hex.toBytes("6f3785f23dbc85066856d60027c5a41f45c90100");

        // :: Act
        byte[] first = assembler.add(prefix);
        byte[] second = assembler.add(suffix);

        // :: Verify
        assertThat(first).isNull();
        assertThat(Hex.of(second)).isEqualTo("3c937752c72e2dfdcfe05a5dce315b6f3785f23dbc85066856d60027c5a41f45c90100");
    }

    /** A notify that is not a 15-byte first half passes through whole. */
    @Test
    public void glucoseNotifyAssembler_wholeFrame_passesThrough() {
        // :: Setup
        GlucoseNotifyAssembler assembler = new GlucoseNotifyAssembler();
        byte[] whole = Hex.toBytes("6f3785f23dbc85066856d60027c5a41f45c90100");

        // :: Act
        byte[] out = assembler.add(whole);

        // :: Verify
        assertThat(out).isEqualTo(whole);
    }

    private static byte[] notify(int seq, byte[] chunk) {
        byte[] out = new byte[chunk.length + 1];
        out[0] = (byte) seq;
        System.arraycopy(chunk, 0, out, 1, chunk.length);
        return out;
    }
}
