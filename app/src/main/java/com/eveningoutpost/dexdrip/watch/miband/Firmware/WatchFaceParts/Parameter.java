package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Parameter {
    private byte id;
    private long value;
    private ArrayList<Parameter> children;
    protected boolean isNoDraw;

    public byte getId() {
        return id;
    }

    public long getValue() {
        return value;
    }

    public ArrayList<Parameter> getChildren() {
        return children;
    }

    public boolean isNoDraw() {
        return isNoDraw;
    }

    public boolean hasChildren() {
        return children != null;
    }

    public static class ParameterFlags {
        public static final byte UNKNOWN = 0;
        public static final byte HAS_CHILDREN = 1;
        public static final byte UNKNOWN2 = 2;
    }

    public Parameter(byte id, long value, boolean isNoDraw) {
        this.id = id;
        this.value = value;
        this.isNoDraw = isNoDraw;
    }

    public Parameter(byte id, ArrayList<Parameter> value, boolean isNoDraw) {
        this.id = id;
        this.children = value;
        this.isNoDraw = isNoDraw;
    }

    public static Parameter readFrom(InputStream stream, int traceOffset) throws IOException {
        byte rawId = ReadByte(stream);
        byte id = (byte) ((rawId & 0xf8) >> 3);
        byte flags = (byte) (rawId & 0x7);
        if (id == 0)
            throw new IllegalArgumentException("Parameter with zero Id is invalid.");
        long value = readValue(stream);
        if (isFlagSet(flags, ParameterFlags.HAS_CHILDREN)) {
            byte[] buffer = new byte[(int) value];
            stream.read(buffer, 0, buffer.length);
            InputStream childrenStream = new ByteArrayInputStream(buffer);
            ArrayList<Parameter> list = readList(childrenStream, traceOffset + 1);
            return new Parameter(id, list, false);
        }
        return new Parameter(id, value, false);
    }

    private static byte ReadByte(InputStream stream) throws IOException {
        int currentByte = stream.read();
        if (currentByte == -1)
            throw new IllegalArgumentException("Reading buffer is empty.");
        return (byte) currentByte;
    }

    public static ArrayList<Parameter> readList(InputStream stream, int traceOffset) throws IOException {
        ArrayList<Parameter> result = new ArrayList<>();
        Parameter parameter;
        while (stream.available() > 0) {
            parameter = readFrom(stream, traceOffset);
            result.add(parameter);
        }
        return result;
    }

    private static long readValue(InputStream stream) throws IOException {
        int bytesLength = 0;
        long value = 0;
        int offset = 0;

        byte currentByte = ReadByte(stream);
        bytesLength += 1;

        while ((currentByte & 0x80) > 0) {
            if (bytesLength > 9)
                throw new IllegalArgumentException("Value of the parameter too long.");
            value = value | ((long) (currentByte & 0x7f) << offset);
            offset += 7;
            currentByte = ReadByte(stream);
            bytesLength += 1;
        }
        value = value | ((long) (currentByte & 0x7f) << offset);
        return value;
    }

    public static boolean isFlagSet(byte value, byte x) {
        return ((value & (1L << x)) != 0);
    }
}
