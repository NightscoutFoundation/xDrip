package com.eveningoutpost.dexdrip.G5Model;


import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Note that if there was documentation on range boundaries
 * for the values in voltageA/B/resistance, temprature and
 * so on, we could have a much MUCH more stable environment
 * in a short time frame.
 * This class contains Tests for
 */
public class BatteryInfoRXMessageTest {
    byte opCode = 0x23;
    byte XopCode = 0x12;
    byte status = 1;
    short voltageA = 1100;
    short voltageB = 1200;
    short resistance = 1100;
    byte runtime = 67;
    byte temperature = 21;
    BatteryInfoRxMessage.PacketValidator validator = new BatteryInfoRxMessage.PacketValidator();   

    /**
     * Positive test, a full length packet with correct opcode passes
     */
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

    /**
     * Positive test for the shorter packet variant. We should hence
     * get 0 for resistance.
     */
    @Test
    public void standardPacketSmallSize() {

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

    /**
     * Note that this is the only reasonable way we have to
     * test if something is inherently wrong. We are sending
     * the wrong opcode but with a packet length that should give
     * us resistance value. So to prove that we didn't process it
     * we check that resistance is 0.
     *
     */
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

    /**
     * Positive test
     * This model is so broken that there is no point in splitting these apart.
     * The proof is in the fact that we can give it any garbage, INCLUDING
     * a resistance of 0 in 12 length packet. Do we know if it's correct?
     * Probably not.
     * Should NOT cause an exception
     */
    @Test
    public void testPositiveResultForCorrectlySizedArraysAndOpcodes() {
        byte[] array10 = new byte[10];
        byte[] array12 = new byte[12];
        Arrays.fill(array10,opCode);
        Arrays.fill(array12,opCode);

        Assert.assertTrue(validator.test(array10));
        Assert.assertTrue(validator.test(array12));
    }

    /**
     * Testing for null? That never happens right?
     */
    @Test
    public void testNegativeResultForNull() {
        Assert.assertFalse(validator.test(null));
    }

    /**
     * Testing for zero length? That never happens right?
     */
    @Test
    public void testNegativeResultForZero() {
        Assert.assertFalse(validator.test(new byte[0]));
    }

    /**
     * And here are a plethora of false ones. I will simply do a trivial fuzzing,
     * not that it needs it.
     */
    @Test
    public void fuzzyRandomizedErroneousData() {
        int passes = 100;
        int min = 1;
        int max = 5285;
        for (int i = 0; i < passes;i++) {
            byte[] fuzzed = new byte[1 + (int)(Math.random()*max)];
            Arrays.fill(fuzzed,(fuzzed.length == 10 || fuzzed.length == 12 ? XopCode:opCode));
            Assert.assertFalse(validator.test(fuzzed));
        }
    }


}
