package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import com.eveningoutpost.dexdrip.models.UserError.Log;

import java.util.ArrayList;
import java.util.List;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project

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
    public ArrayList<Byte> packet;
    public int command;
    public ArrayList<Byte> payload;

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
        Log.d("ShareTest", "About to start adding to Byte, size: " + this.packet.size());
        return this.toBytes();
    }

    public List<byte[]> composeList() {
        packet = new ArrayList<Byte>();
        packet.add(OFFSET_SOF, SOF);
        packet.add(OFFSET_LENGTH, getLength());
        packet.add(OFFSET_NULL, NULL);
        packet.add(OFFSET_CMD, (byte) command);
        if (this.payload != null) { this.packet.addAll(OFFSET_PAYLOAD, this.payload); }
        byte[] crc16 = CRC16.calculate(toBytes(), 0, this.packet.size());
        this.packet.add(crc16[0]);
        this.packet.add(crc16[1]);
        Log.d("ShareTest", "About to start adding to ByteList, size: " + this.packet.size());
        return this.toBytesList();
    }

    private byte getLength() {
        int packetSize = payload == null ? MIN_LEN : payload.size() + CRC_LEN + HEADER_LEN;

        if (packetSize > MAX_LEN) {
            throw new IndexOutOfBoundsException(packetSize + " bytes, but packet must between "
                    + MIN_LEN + " and " + MAX_LEN + " bytes.");
        }

        return (byte) packetSize;
    }

    public byte[] toBytes() {
        byte[] b = new byte[this.packet.size()];
        for (int i = 0; i < this.packet.size(); i++) {
            b[i] = this.packet.get(i).byteValue();
        }
        return b;
    }

    public List<byte[]> toBytesList() {
        List<byte[]> byteMessages = new ArrayList<byte[]>();
        double totalPacketSize = packet.size();
        int messages =(int) Math.ceil(totalPacketSize/18);
        for(int m = 0; m < messages; m++) {
            int thisPacketSize;
            if (m == messages - 1) {
                thisPacketSize = ((this.packet.size()+2) % 18);
            } else {
                thisPacketSize = (20);
            }
            int offset = m * 18;
            Log.d("ShareTest", "This packet size: " + thisPacketSize);
            byte[] b = new byte[thisPacketSize];
            b[0] = (byte) (m + 1);
            b[1] = (byte) (messages);
            for (int i = 2; i < thisPacketSize; i++) {
                b[i] = packet.get(offset + i - 2).byteValue();
            }
            byteMessages.add(b);
        }
        return byteMessages;
    }
}
