package com.eveningoutpost.dexdrip.G5Model;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Note that if there was documentation on range boundaries
 * for the values in voltageA/B/resistance, temprature and
 * so on, we could have a much MUCH more stable environment
 * in a short time frame.
 */
public class BatteryInfoRXTests {
    byte opCode = 0x23;
    byte XopCode = 0x12;
    byte status = 1;
    short voltageA = 1100;
    short voltageB = 1200;
    short resistance = 1100;
    byte runtime = 67;
    byte temperature = 21;
    @Test
    public void standardPacketFullSize() {

        ByteBuffer b = ByteBuffer.allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(opCode)
                .put(status)
                .putShort(voltageA)
                .putShort(voltageB)
                .putShort(resistance)
                .put(runtime)
                .put(temperature)
                .putShort((short)0);
        int pos = b.position();
        b.rewind();
        byte[] packet =new byte[pos];
        b.get(packet);
        byte[] crc =FastCRC16.calculate(packet, packet.length - 2);
        packet[packet.length-2] = crc[1];
        packet[packet.length-1] = crc[0];
        BatteryInfoRxMessage msg = new BatteryInfoRxMessage(packet);
        Assert.assertEquals(voltageA,msg.voltagea);
        Assert.assertEquals(voltageB,msg.voltageb);
        Assert.assertEquals(resistance,msg.resist);
        Assert.assertEquals(runtime,msg.runtime);
        Assert.assertEquals(temperature,msg.temperature);
    }
    @Test
    public void standardPacketSmallSize() {
        /**
         * We are supposed to get a 0 for resistance here
         */
        ByteBuffer b = ByteBuffer.allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(opCode)
                .put(status)
                .putShort(voltageA)
                .putShort(voltageB)
                .put(runtime)
                .put(temperature)
                .putShort((short)0);
        int pos = b.position();
        b.rewind();
        byte[] packet =new byte[pos];
        b.get(packet);
        byte[] crc =FastCRC16.calculate(packet, packet.length - 2);
        packet[packet.length-2] = crc[1];
        packet[packet.length-1] = crc[0];
        BatteryInfoRxMessage msg = new BatteryInfoRxMessage(packet);
        Assert.assertEquals(voltageA,msg.voltagea);
        Assert.assertEquals(voltageB,msg.voltageb);
        Assert.assertEquals(0,msg.resist);
        Assert.assertEquals(runtime,msg.runtime);
        Assert.assertEquals(temperature,msg.temperature);
    }

    //The fact that this isn't standard behavior is
    //so bizarre.
    //@Test(expected=IllegalArgumentException.class)
    @Test
    public void brokenOpCode() {

        ByteBuffer b = ByteBuffer.allocate(12)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(XopCode)
                .put(status)
                .putShort(voltageA)
                .putShort(voltageB)
                .putShort(resistance)
                .put(runtime)
                .put(temperature)
                .putShort((short)0);
        int pos = b.position();
        b.rewind();
        byte[] packet =new byte[pos];
        b.get(packet);
        byte[] crc =FastCRC16.calculate(packet, packet.length - 2);
        packet[packet.length-2] = crc[1];
        packet[packet.length-1] = crc[0];
        BatteryInfoRxMessage msg = new BatteryInfoRxMessage(packet);

        Assert.assertEquals(0,msg.resist);
    }

}
