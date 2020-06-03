package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Header {
    private static String dialSignature = "HMDIAL\0";

    public String getSignature() {
        return signature;
    }

    public int getUnknown() {
        return unknown;
    }

    public int getParametersSize() {
        return parametersSize;
    }

    public static int getHeaderSize() {
        return headerSize;
    }


    private String signature = dialSignature;
    private int unknown;
    private int parametersSize;
    private static int headerSize = 40;

    public boolean isValid() {
        return (signature.equals(dialSignature));
    }

    public static Header readFrom(InputStream stream) throws IOException {
        Header header = new Header();
        ByteBuffer b;
        byte[] bytes = new byte[headerSize];
        stream.read(bytes, 0, bytes.length);

        b = ByteBuffer.allocate(dialSignature.length());
        b.put(bytes, 0, dialSignature.length());
        String signature = new String(b.array());

        b = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        b.put(bytes, 32, 8);
        b.rewind();
        int unknown = b.getInt();
        int parametersSize = b.getInt();

        header.signature = signature;
        header.unknown = unknown;
        header.parametersSize = parametersSize;
        return header;
    }
}
