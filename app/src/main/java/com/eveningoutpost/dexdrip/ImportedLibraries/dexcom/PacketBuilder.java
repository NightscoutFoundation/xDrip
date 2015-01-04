package com.eveningoutpost.dexdrip.ImportedLibraries.dexcom;

import java.util.ArrayList;

public class PacketBuilder {
    public static final int MAX_PAYLOAD = 1584;
    public static final int MIN_LEN = 6;
    public static final int MAX_LEN = MAX_PAYLOAD + MIN_LEN;
    public static final byte SOF = 0x01;
    public static final int OFFSET_SOF = 0;
    public static final int OFFSET_LENGTH = 1;
    public static final int OFFSET_NULL = 2;
    public static final byte NULL = 0x00;
    public static final int OFFSET_CMD = 3;
    public static final int OFFSET_PAYLOAD = 4;
    public static final int CRC_LEN = 2;
    public static final int HEADER_LEN = 4;
    private ArrayList<Byte> packet;
    private int command;
    private ArrayList<Byte> payload;

    public PacketBuilder(int command) {
        this.command = command;
    }

    public PacketBuilder(int command, ArrayList<Byte> payload) {
        this.command = command;
        this.payload = payload;
    }

    public byte[] compose() {
        packet = new ArrayList<Byte>();
        packet.add(OFFSET_SOF, SOF);
        packet.add(OFFSET_LENGTH, getLength());
        packet.add(OFFSET_NULL, NULL);
        packet.add(OFFSET_CMD, (byte) command);
        if (this.payload != null) { this.packet.addAll(OFFSET_PAYLOAD, this.payload); }
        byte[] crc16 = CRC16.calculate(toBytes(), 0, this.packet.size());
        this.packet.add(crc16[0]);
        this.packet.add(crc16[1]);
        return this.toBytes();
    }
    private byte getLength() {
        int packetSize = payload == null ? MIN_LEN : payload.size() + CRC_LEN + HEADER_LEN;

        if (packetSize > MAX_LEN) {
            throw new IndexOutOfBoundsException(packetSize + " bytes, but packet must between "
                    + MIN_LEN + " and " + MAX_LEN + " bytes.");
        }

        return (byte) packetSize;
    }

    private byte[] toBytes() {
        byte[] b = new byte[this.packet.size()];
        for (int i = 0; i < this.packet.size(); i++) {
            b[i] = this.packet.get(i).byteValue();
        }
        return b;
    }
}
