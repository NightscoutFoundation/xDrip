package com.eveningoutpost.dexdrip.glucosemeter.caresens;

import com.eveningoutpost.dexdrip.models.JoH;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContextRxTest {

    @Test
    public void ketoneTest() {
        ContextRx contextRx = new ContextRx(JoH.hexStringToByteArray("02040006"));
        assertTrue(contextRx.ketone());
    }

    @Test
    public void toStringTest() {
        ContextRx contextRx = new ContextRx(JoH.hexStringToByteArray("02040006"));
        assertEquals("Context: Sequence: 4  KETONE  MEALTYPE: 6 ", contextRx.toString());
    }
}