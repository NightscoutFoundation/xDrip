package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts;

import java.io.ByteArrayOutputStream;

public class WatchfaceBitWriter {
    private byte[] masks = {(byte) 128, (byte) 192, (byte) 224, (byte) 240, (byte) 248, (byte) 252, (byte) 254, (byte) 255};
    private ByteArrayOutputStream stream;
    private int currentBit;
    private byte currentByte;

    WatchfaceBitWriter(ByteArrayOutputStream stream) {
        this.stream = stream;
    }

    public void writeBits(long data, int length) {
        while (length > 0) {
            int freeBits = 8 - currentBit;
            int dataLength = Math.min(freeBits, length);

            long currentByteData;
            if (length > 8)
                currentByteData = data >> (length - 8);
            else
                currentByteData = data << (8 - length);

            int appendData = (int) ((currentByteData & masks[dataLength - 1]) >> currentBit);
            currentByte = (byte) (currentByte | appendData);

            currentBit += dataLength;
            length -= dataLength;
            if (currentBit != 8) continue;

            stream.write(currentByte);
            currentBit = 0;
            currentByte = 0;
        }
    }

    public void flush() {
        if (currentBit > 0)
            stream.write(currentByte);
    }
}
