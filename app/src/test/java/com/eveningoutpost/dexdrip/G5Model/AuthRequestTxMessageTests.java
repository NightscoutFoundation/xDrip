package com.eveningoutpost.dexdrip.G5Model;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;

/**
 * Tests for the AuthRequestTxMessage class
 */
public class AuthRequestTxMessageTests {

    @Test
    public void shortKeyTest() {
        //standard length, alt set to true
        boolean alt = true;
        int tokenSize = 8;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
    }

    @Test
    public void shortKeyTest2() {
        //standard length, this time with alt false
        boolean alt = false;
        int tokenSize = 8;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
    }

    @Test
    public void longerKeyJustToMakeSure() {
        //standard length, this time with alt false
        boolean alt = false;
        int tokenSize = 128;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
        Assert.assertThat(msg.byteSequence.length, is(tokenSize+2));
    }
    @Test(expected = IllegalArgumentException.class)
    public void ensureZeroLengthKeyFails() {
        //standard length, this time with alt false
        boolean alt = false;
        int tokenSize = 0;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
        Assert.assertThat(msg.byteSequence.length, is(tokenSize+2));
    }
    @Test(expected = IllegalArgumentException.class)
    public void ensureKeyLengthLargerthan2048Fails() {
        //standard length, this time with alt false
        boolean alt = false;
        int tokenSize = 2050;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
        Assert.assertThat(msg.byteSequence.length, is(tokenSize+2));
    }
    @Test
    public void ensureKeyLengthEqualTo2048Succeeds() {
        //standard length, this time with alt false
        boolean alt = false;
        int tokenSize = 2048;
        AuthRequestTxMessage msg = new AuthRequestTxMessage(tokenSize,alt);
        Assert.assertThat(msg.opcode, is(msg.byteSequence[0]));
        Assert.assertThat(msg.getEndByte(alt), is(msg.byteSequence[tokenSize+1]));
        Assert.assertThat(msg.byteSequence.length, is(tokenSize+2));
    }

    /**
     * The previous implementation generated keys which were distributed like this:
     * 45: 3000
     * 48: 904
     * 49: 954
     * 50: 992
     * 51: 1907
     * 52: 935
     * 53: 941
     * 54: 928
     * 55: 950
     * 56: 1149
     * 57: 1205
     * 97: 1236
     * 98: 1156
     * 99: 883
     * 100: 919
     * 101: 973
     * 102: 968
     * 239 plausible values had no value
     *
     * This is BAD and was the result of converting random bytes to a UUID which was converted to
     * a hex string which was converted to bytes which with 8 bytes  and 16 outcomes per byte would
     * 4 bits per byte and a keysize of .. well you get the idea. Hence a quick check
     */
    @Test
    public void ensureRandomness() {
        boolean alt = false;
        int tokenSize = 20;
        int rounds = 100000;
        int[] outcomes = new int[256];
        Arrays.fill(outcomes,0);
        AuthRequestTxMessage msg = null;
        for (int i =  0; i < rounds; i++) {
            msg = new AuthRequestTxMessage(tokenSize,alt);
            Assert.assertThat(msg.singleUseToken.length, is(tokenSize));
            for(int j = 0; j < tokenSize;j++) {
                outcomes[((int)msg.singleUseToken[j])&0xFF] +=1;
            }
        }
        int max = 0;
        int min = rounds;
        int zeroOutcomes = 0;
        for(int outcome: outcomes) {
            max = Math.max(max,outcome);
            min = Math.min(min,outcome);
            if (outcome == 0) {
                zeroOutcomes++;
            }
        }
        int mean = (rounds*tokenSize)/outcomes.length;

        //deviation from the mean is nothing fancy basically
        int[] dev = new int[outcomes.length];
        int total = 0;
        int devSq = 0;
        for (int i = 0; i < outcomes.length;i++) {
            devSq = outcomes[i]-mean;
            total+=(devSq*devSq);
        }
        double variance = ((double)total/outcomes.length);
        double stdDev = Math.sqrt(variance);


        //the total will obviously be 20 000 so
        System.out.println("Max: " + max);
        System.out.println("Min: " + min);
        System.out.println("Zero outcomes: " + zeroOutcomes);
        Assert.assertThat(zeroOutcomes,is(0));
        //so with n rounds and we get n * tokensizeBytes which means the mean
        //should be (n*tokenSize)/256
        System.out.println("Mean: " + mean);
        Arrays.sort(outcomes);
        System.out.println("Median: " + outcomes[127]);
        System.out.println("Variance: " + variance);
        System.out.println("Std dev: " + stdDev);

        //calculate deviation from the mean



    }
}
