package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

public class HeaderMiBand5 extends Header {
    protected static final String headerSignature = "UIHH\1\0";
    protected static final int headerSize = 87;
    protected static final int paramOffset = 79;

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
