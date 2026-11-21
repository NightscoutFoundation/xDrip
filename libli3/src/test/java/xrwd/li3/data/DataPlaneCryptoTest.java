package xrwd.li3.data;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import java.security.GeneralSecurityException;

import xrwd.li3.ble.Gatt;
import xrwd.li3.ble.GlucoseNotifyAssembler;
import xrwd.li3.data.DataPlaneCrypto.Channel;
import xrwd.li3.data.DataPlaneCrypto.PacketKind;
import xrwd.li3.util.Hex;

/**
 * Tests for the data-plane frame trailer, the AES-CCM wrapper keyed by the Phase 6 session, and the patch-control
 * write. Live vectors are from RoundWhiteDiscKit 7dd6396 {@code DataPlaneTests.swift}.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class DataPlaneCryptoTest {

    /** Session from the live capture in DataPlaneTests.swift. */
    private static final byte[] LIVE_K_ENC = Hex.toBytes("7c536d2ec3c76c741a776de85596959d");
    private static final byte[] LIVE_IV_ENC = Hex.toBytes("00000000bcc696a2");

    /** Synthetic session from DataPlaneTests.swift's round-trip tests. */
    private static final byte[] K_ENC = Hex.toBytes("4bbce496a63cc9a435adeeb4f78e1617");
    private static final byte[] IV_ENC = Hex.toBytes("0000000067c72c01");

    // =================================================== DataFrame ===================================================

    /** The last two bytes are the little-endian sequence; everything before them is ciphertext and tag. */
    @Test
    public void dataFrame_parse_splitsTrailer() {
        // :: Setup
        byte[] wire = Hex.toBytes("aabbccddee0700");

        // :: Act
        DataFrame frame = DataFrame.parse(wire);

        // :: Verify
        assertThat(Hex.of(frame.encrypted())).isEqualTo("aabbccddee");
        assertThat(frame.sequence()).isEqualTo(7);
        assertThat(frame.toWire()).isEqualTo(wire);
    }

    /** Session-control frames from the live capture carry 11 or 8 encrypted bytes behind the same trailer. */
    @Test
    public void dataFrame_parse_liveSessionControlFrames() {
        // :: Act
        DataFrame first = DataFrame.parse(Hex.toBytes("ed7df1f64812b45afa57010100"));
        DataFrame second = DataFrame.parse(Hex.toBytes("665f5b31bd9f9add0100"));
        DataFrame third = DataFrame.parse(Hex.toBytes("0fdf3ccfe55e0045cb71920200"));

        // :: Verify
        assertThat(first.encrypted()).hasLength(11);
        assertThat(first.sequence()).isEqualTo(1);
        assertThat(second.encrypted()).hasLength(8);
        assertThat(third.sequence()).isEqualTo(2);
    }

    /** A frame with no room for the trailer and a byte of ciphertext is rejected. */
    @Test
    public void dataFrame_parse_tooShort_throws() {
        // :: Act
        ThrowingRunnable parse = () -> DataFrame.parse(new byte[]{0x01});

        // :: Verify
        assertThrows(IllegalArgumentException.class, parse);
    }

    // ==================================================== Channel ====================================================

    /** The three decoded characteristics map to channels; the rest are not decrypted. */
    @Test
    public void channel_forCharacteristic_mapsDecodedCharacteristics() {
        // :: Act
        Channel status = Channel.forCharacteristic(Gatt.PATCH_STATUS);
        Channel eventLog = Channel.forCharacteristic(Gatt.EVENT_LOG);

        // :: Verify
        assertThat(status).isEqualTo(Channel.PATCH_STATUS);
        assertThat(Channel.forCharacteristic(Gatt.GLUCOSE_DATA)).isEqualTo(Channel.GLUCOSE_DATA);
        assertThat(Channel.forCharacteristic(Gatt.HISTORIC_DATA)).isEqualTo(Channel.HISTORIC_DATA);
        assertThat(eventLog).isNull();
    }

    // ================================================ DataPlaneCrypto ================================================

    /** The nonce is {@code seqLE2 || descriptor3 || ivEnc8}. */
    @Test
    public void nonce_isSequenceDescriptorAndIv() {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);

        // :: Act
        byte[] nonce = crypto.nonce(0x0001, PacketKind.PATCH_DATA);

        // :: Verify
        assertThat(Hex.of(nonce)).isEqualTo("01004400000000000067c72c01");
    }

    /** The live 15 + 20 byte glucose notify joins and opens under the glucose channel's descriptor. */
    @Test
    public void decrypt_liveGlucoseNotify_opens() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(LIVE_K_ENC, LIVE_IV_ENC);
        GlucoseNotifyAssembler assembler = new GlucoseNotifyAssembler();
        assembler.add(Hex.toBytes("3c937752c72e2dfdcfe05a5dce315b"));
        byte[] wire = assembler.add(Hex.toBytes("6f3785f23dbc85066856d60027c5a41f45c90100"));

        // :: Act
        byte[] plain = crypto.decrypt(Channel.GLUCOSE_DATA, wire);

        // :: Verify
        assertThat(Hex.of(plain)).isEqualTo("da386700e3ff0000ac26c73871000b670071001a0abf03f33c59100000");
    }

    /** The live patch-status notify opens to the 12-byte status record. */
    @Test
    public void decrypt_livePatchStatus_opens() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(LIVE_K_ENC, LIVE_IV_ENC);

        // :: Act
        byte[] plain = crypto.decrypt(Channel.PATCH_STATUS, Hex.toBytes("4e6fe9573b6365476193e9c42009acd20100"));

        // :: Verify
        assertThat(Hex.of(plain)).isEqualTo("9d280000e0000704db381607");
    }

    /** The live historic-data notify opens to the 14-byte page. */
    @Test
    public void decrypt_liveHistoricData_opens() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(LIVE_K_ENC, LIVE_IV_ENC);

        // :: Act
        byte[] plain = crypto.decrypt(Channel.HISTORIC_DATA,
                Hex.toBytes("4637aa722274876d69a34f20096c49adced70100"));

        // :: Verify
        assertThat(Hex.of(plain)).isEqualTo("98036d006b0069006e006a006500");
    }

    /** What is sealed under a descriptor opens again, with the sequence carried in the trailer. */
    @Test
    public void encrypt_roundTrips() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);
        byte[] plain = Hex.toBytes("00112233445566778899aabb");

        // :: Act
        byte[] wire = crypto.encrypt(PacketKind.PATCH_DATA, 0x0201, plain);

        // :: Verify
        assertThat(DataFrame.parse(wire).sequence()).isEqualTo(0x0201);
        assertThat(Hex.of(wire).substring(wire.length * 2 - 4)).isEqualTo("0102");
        assertThat(crypto.decrypt(Channel.HISTORIC_DATA, wire)).isEqualTo(plain);
    }

    /** A frame sealed under another descriptor fails the channel's preferred MAC and is found by trying the rest. */
    @Test
    public void decrypt_wrongDescriptor_fallsThroughToMatchingKind() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);
        byte[] plain = Hex.toBytes("0102030405060708");
        byte[] wire = crypto.encrypt(PacketKind.PATCH_DATA, 3, plain);

        // :: Act
        byte[] opened = crypto.decrypt(Channel.GLUCOSE_DATA, wire);

        // :: Verify
        assertThat(opened).isEqualTo(plain);
    }

    /** A frame no descriptor opens is rejected. */
    @Test
    public void decrypt_tampered_throws() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);
        byte[] wire = crypto.encrypt(PacketKind.KIND3, 1, Hex.toBytes("0102030405060708"));
        wire[0] ^= 0x01;

        // :: Act
        ThrowingRunnable decrypt = () -> crypto.decrypt(Channel.GLUCOSE_DATA, wire);

        // :: Verify
        assertThrows(GeneralSecurityException.class, decrypt);
    }

    /** A frame too short to hold the 4-byte tag is rejected, not read past. */
    @Test
    public void decrypt_shorterThanTag_throws() {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);

        // :: Act
        ThrowingRunnable decrypt = () -> crypto.decrypt(Channel.PATCH_STATUS, Hex.toBytes("aabbcc0100"));

        // :: Verify
        assertThrows(GeneralSecurityException.class, decrypt);
    }

    /** A notify shorter than the trailer is a decrypt failure, so the caller handles one exception type. */
    @Test
    public void decrypt_shorterThanTrailer_throws() {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);

        // :: Act
        ThrowingRunnable decrypt = () -> crypto.decrypt(Channel.PATCH_STATUS, new byte[]{0x01});

        // :: Verify
        assertThrows(GeneralSecurityException.class, decrypt);
    }

    /** Session keys of the wrong size are a caller error. */
    @Test
    public void constructor_wrongKeySize_throws() {
        // :: Act
        ThrowingRunnable create = () -> new DataPlaneCrypto(new byte[15], IV_ENC);

        // :: Verify
        assertThrows(IllegalArgumentException.class, create);
    }

    // ================================================= PatchControl ==================================================

    /** The historic backfill request is {@code 01 00 01 lifeCountLE2 00 00}. */
    @Test
    public void backfillFrom_matchesIosBuilder() {
        // :: Act
        byte[] fromFive = PatchControl.backfillFrom(5);

        // :: Verify
        assertThat(Hex.of(fromFive)).isEqualTo("01000105000000");
        assertThat(Hex.of(PatchControl.backfillFrom(907))).isEqualTo("0100018b030000");
        assertThat(Hex.of(PatchControl.backfillFrom(1073))).isEqualTo("01000131040000");
    }

    /** A patch-control write seals to 11 encrypted bytes plus the 2-byte sequence, under descriptor kind 0. */
    @Test
    public void backfillFrom_encryptsTo13ByteWrite() throws GeneralSecurityException {
        // :: Setup
        DataPlaneCrypto crypto = new DataPlaneCrypto(K_ENC, IV_ENC);

        // :: Act
        byte[] wire = crypto.encrypt(PatchControl.WRITE_KIND, 1, PatchControl.backfillFrom(5));

        // :: Verify
        assertThat(wire).hasLength(13);
        assertThat(PatchControl.WRITE_KIND).isEqualTo(PacketKind.KIND0);
        assertThat(Hex.of(wire).substring(22)).isEqualTo("0100");
    }
}
