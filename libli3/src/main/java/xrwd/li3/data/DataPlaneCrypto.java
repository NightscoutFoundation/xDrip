package xrwd.li3.data;

import java.security.GeneralSecurityException;
import java.util.UUID;

import xrwd.li3.ble.Gatt;
import xrwd.li3.crypto.AesCcm;

/**
 * AES-CCM for the data plane, keyed by the session the handshake derived. The nonce is
 * {@code sequenceLE2 || descriptor3 || ivEnc8} and the tag is 4 bytes. Each channel has a preferred descriptor;
 * when its MAC fails, the remaining descriptors are tried, as RoundWhiteDiscKit does. Ported from
 * RoundWhiteDiscKit {@code DataPlane/DataPlaneCrypto.swift} (MIT).
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public final class DataPlaneCrypto {

    private static final int KEY_SIZE = 16;
    private static final int IV_SIZE = 8;
    private static final int TAG_SIZE = 4;

    /** The nonce descriptor a frame was sealed under. */
    public enum PacketKind {
        KIND0(0x00, 0x00, 0x00),
        HANDSHAKE(0x00, 0x00, 0x0f),
        KIND2(0x00, 0x00, 0xf0),
        KIND3(0x00, 0x0f, 0x00),
        KIND4(0x00, 0xf0, 0x00),
        KIND5(0x0f, 0x00, 0x00),
        KIND6(0xf0, 0x00, 0x00),
        PATCH_DATA(0x44, 0x00, 0x00);

        private final byte[] descriptor;

        PacketKind(int b0, int b1, int b2) {
            descriptor = new byte[]{(byte) b0, (byte) b1, (byte) b2};
        }
    }

    /** The notifying characteristics whose frames are decrypted. */
    public enum Channel {
        PATCH_STATUS(PacketKind.KIND2),
        GLUCOSE_DATA(PacketKind.KIND3),
        HISTORIC_DATA(PacketKind.KIND4);

        private final PacketKind preferred;

        Channel(PacketKind preferred) {
            this.preferred = preferred;
        }

        /** @return the channel for a characteristic, or null for one that is not decrypted */
        public static Channel forCharacteristic(UUID characteristic) {
            if (Gatt.PATCH_STATUS.equals(characteristic)) {
                return PATCH_STATUS;
            }
            if (Gatt.GLUCOSE_DATA.equals(characteristic)) {
                return GLUCOSE_DATA;
            }
            if (Gatt.HISTORIC_DATA.equals(characteristic)) {
                return HISTORIC_DATA;
            }
            return null;
        }
    }

    private final byte[] kEnc;
    private final byte[] ivEnc;

    /** @throws IllegalArgumentException unless the key is 16 bytes and the IV 8 */
    public DataPlaneCrypto(byte[] kEnc, byte[] ivEnc) {
        if (kEnc.length != KEY_SIZE || ivEnc.length != IV_SIZE) {
            throw new IllegalArgumentException("Session key must be 16 bytes and IV 8 bytes");
        }
        this.kEnc = kEnc.clone();
        this.ivEnc = ivEnc.clone();
    }

    public byte[] nonce(int sequence, PacketKind kind) {
        byte[] nonce = new byte[2 + 3 + IV_SIZE];
        nonce[0] = (byte) sequence;
        nonce[1] = (byte) (sequence >> 8);
        System.arraycopy(kind.descriptor, 0, nonce, 2, 3);
        System.arraycopy(ivEnc, 0, nonce, 5, IV_SIZE);
        return nonce;
    }

    /**
     * Opens one frame from a channel.
     *
     * @throws GeneralSecurityException if the frame is too short or no descriptor opens it
     */
    public byte[] decrypt(Channel channel, byte[] wire) throws GeneralSecurityException {
        if (wire.length < DataFrame.TRAILER_SIZE + 1) {
            throw new GeneralSecurityException("Data frame of " + wire.length + " bytes is too short");
        }
        DataFrame frame = DataFrame.parse(wire);
        byte[] sealed = frame.encrypted();
        if (sealed.length < TAG_SIZE) {
            throw new GeneralSecurityException("Data frame shorter than its tag");
        }
        try {
            return AesCcm.decrypt(kEnc, nonce(frame.sequence(), channel.preferred), sealed, TAG_SIZE);
        } catch (GeneralSecurityException preferredFailed) {
            for (PacketKind kind : PacketKind.values()) {
                if (kind == channel.preferred) {
                    continue;
                }
                try {
                    return AesCcm.decrypt(kEnc, nonce(frame.sequence(), kind), sealed, TAG_SIZE);
                } catch (GeneralSecurityException ignored) {
                    // try the next descriptor
                }
            }
            throw new GeneralSecurityException("No descriptor opens the " + channel + " frame");
        }
    }

    /** @return the wire frame: ciphertext, tag and sequence trailer */
    public byte[] encrypt(PacketKind kind, int sequence, byte[] plain) throws GeneralSecurityException {
        return DataFrame.toWire(AesCcm.encrypt(kEnc, nonce(sequence, kind), plain, TAG_SIZE), sequence);
    }
}
