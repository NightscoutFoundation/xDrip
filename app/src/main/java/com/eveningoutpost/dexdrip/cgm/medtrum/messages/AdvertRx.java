package com.eveningoutpost.dexdrip.cgm.medtrum.messages;

// jamorham

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.cgm.medtrum.Crypt;
import com.eveningoutpost.dexdrip.cgm.medtrum.DeviceType;
import com.eveningoutpost.dexdrip.cgm.medtrum.Medtrum;
import com.google.gson.annotations.Expose;

import lombok.Getter;

public class AdvertRx extends BaseMessage {

    @Getter
    boolean valid = false;
    @Expose
    public long serial = -1;
    @Expose
    public int deviceType = -1;
    @Expose
    public int version = -1;
    @Expose
    @Getter
    AnnexARx annex = null;

    public AdvertRx(byte[] packet) {
        if (packet == null || packet.length < 6) return;
        wrap(packet);

        serial = getUnsignedInt();
        deviceType = getUnsignedByte();
        version = getUnsignedByte();

        // device specific
        if (!Medtrum.isDeviceTypeSupported(deviceType)) {
            if (JoH.quietratelimit("medtrum-low-error", 120)) {
                UserError.Log.wtf(TAG, "Unknown device type: " + deviceType);
            }
            // continue anyway for now
        }

        // Process Annex
        final byte[] remainder = new byte[data.remaining()];
        data.get(remainder);
        Crypt.code(remainder, serial);

        if (remainder.length == 14) {
            annex = new AnnexARx(remainder);
            valid = true; // TODO some more checks
        }
    }

    public String getDeviceName() {
        try {
            return DeviceType.get(deviceType).name();
        } catch (NullPointerException e) {
            return "Unsupported";
        }
    }
}
