package com.eveningoutpost.dexdrip.glucosemeter.caresens;

import com.eveningoutpost.dexdrip.Models.JoH;

import org.junit.Test;

import java.util.Calendar;

import static com.google.common.truth.Truth.assertWithMessage;

public class TimeTxTest {

    @Test
    public void create() {

        final Calendar cal = Calendar.getInstance();
        cal.set(2018, Calendar.NOVEMBER, 29, 22, 45, 59);

        TimeTx packet = new TimeTx(cal.getTimeInMillis());

        String result = JoH.bytesToHex(packet.byteSequence);
        System.out.println(result);
        assertWithMessage("TimeTx creation invalid").that(result).isEqualTo("C0030100E2070B1D162D3B");

    }

}