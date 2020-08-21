package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class Header implements HeaderInterface {
    protected int unknown;
    protected int parametersSize;
    protected String signature;

    public int getUnknown() {
        return unknown;
    }

    public abstract int getParamOffset();

    public abstract int getHeaderSize();

    public abstract String getSignature();

    public boolean isValid() {
        return (signature.equals(getSignature()));
    }

    public int getParametersSize() {
        return parametersSize;
    }

    public Header readFrom(InputStream stream) throws IOException {
        ByteBuffer b;
        byte[] bytes = new byte[getHeaderSize()];
        stream.read(bytes, 0, bytes.length);

        b = ByteBuffer.allocate(getSignature().length());
        b.put(bytes, 0, getSignature().length());
        signature = new String(b.array());

        b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        b.put(bytes, getParamOffset(), 8);
        b.rewind();
        unknown = b.getInt();
        parametersSize = b.getInt();
        return this;
    }
}
