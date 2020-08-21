package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

public class HeaderMiBand4 extends Header{
    protected static final String headerSignature = "HMDIAL\0";
    protected static final int headerSize = 40;
    protected static final int paramOffset = 32;

    @Override
    public int getParamOffset() {
        return paramOffset;
    }

    @Override
    public int getHeaderSize() {
        return headerSize;
    }

    @Override
    public String getSignature() {
        return headerSignature;
    }
}
