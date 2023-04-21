package com.eveningoutpost.dexdrip.importedlibraries.dexcom;

import java.util.Arrays;

// This code and this particular library are from the NightScout android uploader
// Check them out here: https://github.com/nightscout/android-uploader
// Some of this code may have been modified for use in this project


public class ReadPacket {
    private int command;
    private byte[] data;
    private byte[] crc_calc;
    private byte[] crc;
    private int OFFSET_CMD = 3;
    private int OFFSET_DATA = 4;
    private int CRC_LEN = 2;

    public ReadPacket(byte[] readPacket) {
        this.command = readPacket[OFFSET_CMD];
        this.data = Arrays.copyOfRange(readPacket, OFFSET_DATA, readPacket.length - CRC_LEN);
        this.crc = Arrays.copyOfRange(readPacket, readPacket.length - CRC_LEN, readPacket.length);
        this.crc_calc=CRC16.calculate(readPacket, 0, readPacket.length - 2);
        if (!Arrays.equals(this.crc, this.crc_calc)) {
            throw new CRCFailRuntimeException("CRC check failed: " + Utils.bytesToHex(this.crc) + " vs " + Utils.bytesToHex(this.crc_calc));
        }
    }

    public int getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
