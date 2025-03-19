package com.eveningoutpost.dexdrip.models;

import static org.junit.Assert.*;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SensorTest extends RobolectricTestWithConfig {

    @Before
    public void setUp() {
        cleanEverything();
    }

    @After
    public void tearDown() {
        cleanEverything();
    }

    private void cleanEverything() {
        BgReading.deleteALL();
        Calibration.deleteAll();
        Sensor.deleteAll();
    }

    @Test // check basic functionality
    public void continuityTest() {
        assertNull("start with no active sensor", Sensor.currentSensor());
        assertNull("start with no previous sensor", Sensor.lastStopped());
        assertFalse("sensor not running", Sensor.isActive());

        long time = 1741707535293L;
        // sensor 1
        Sensor one = Sensor.create(time);

        assertNotNull("first sensor is active sensor", Sensor.currentSensor());
        assertEquals("first sensor is active sensor we know", one, Sensor.currentSensor());
        assertTrue("sensor is running", Sensor.isActive());

        time += Constants.HOUR_IN_MS;

        Sensor.setUnitTestStopTime(time); // force stop time to this


        time += Constants.HOUR_IN_MS;
        Sensor two = Sensor.create(time);
        time += Constants.HOUR_IN_MS;

        assertNotNull("second sensor is active sensor", Sensor.currentSensor());
        assertEquals("second sensor is active sensor we know", two, Sensor.currentSensor());

        //System.out.println("TWO: " + two);
        two.stopped_at = time;
        two.save();

        assertFalse("sensor is not running", Sensor.isActive());
        assertNull("any sensor is not active sensor", Sensor.currentSensor());

        // restore sensor 1 to simulate out of order sequence previously possible
        //one.stopped_at = 0; // not necessary as our cached version doesn't reflect the stop made to the database
        one.save();

        assertFalse("sensor is not running b", Sensor.isActive());
        assertNull("any sensor is not active sensor b", Sensor.currentSensor());
    }

    @Test // check correct start/end times
    public void testCreate() {
        assertNull("start with no active sensor", Sensor.currentSensor());
        assertNull("start with no previous sensor", Sensor.lastStopped());
        assertFalse("sensor not running", Sensor.isActive());

        long time = 1741707535292L;
        //System.out.println("TIME start: " + JoH.dateTimeText(time));

        // sensor 1
        Sensor one = Sensor.create(time);

        assertNotNull("first sensor is active sensor", Sensor.currentSensor());
        assertEquals("first sensor is active sensor we know", one, Sensor.currentSensor());
        // assertSame("first sensor is active sensor we know",one, Sensor.currentSensor());
        assertNull("first sensor no previous sensor", Sensor.lastStopped());
        assertTrue("sensor 1 is running", Sensor.isActive());

        //System.out.println(one);
        // sensor 2 create while sensor 1 running

        time += Constants.HOUR_IN_MS;

        Sensor.setUnitTestStopTime(time); // force stop time to this

        // Try and start a sensor 3 hours in the past which should then be started at the last sensor stop time +1ms
        Sensor two = Sensor.create(time - Constants.HOUR_IN_MS * 3);


        assertNotNull("second sensor is active sensor", Sensor.currentSensor());
        assertEquals("second sensor is active sensor we know", two, Sensor.currentSensor());
        // assertSame("first sensor is active sensor we know",one, Sensor.currentSensor());
        assertEquals("first sensor is previous sensor", one, Sensor.lastStopped());
        assertTrue("sensor 2 is running", Sensor.isActive());

        assertEquals("last sensor (one) stopped at now time", time, Sensor.lastStopped().stopped_at);
        assertEquals("current sensor (two) started at now time + 1 ms", Sensor.lastStopped().stopped_at + 1, Sensor.currentSensor().started_at);


        //System.out.println(Sensor.currentSensor());
        //System.out.println(Sensor.lastStopped());

        time += Constants.HOUR_IN_MS;
        // add a reading
        BgReading reading1 = BgReading.bgReadingInsertFromG5(123, time, "Backfill");
        assertNotNull("bg reading created", reading1);

        //System.out.println("Bg Reading created at: " + JoH.dateTimeText(reading1.getEpochTimestamp()));

        time += Constants.HOUR_IN_MS;
        Sensor.setUnitTestStopTime(time); // force stop time to this

        // Try and start a sensor 3 hours in the past which should then be started at the last reading time
        Sensor three = Sensor.create(time - Constants.HOUR_IN_MS * 3);
        //System.out.println(three);

        assertNotNull("third sensor is active sensor", Sensor.currentSensor());
        assertEquals("third sensor is active sensor we know", three, Sensor.currentSensor());
        // assertSame("first sensor is active sensor we know",one, Sensor.currentSensor());
        assertEquals("third sensor is previous sensor", two, Sensor.lastStopped());
        assertTrue("sensor 3 is running", Sensor.isActive());

        assertEquals("last sensor (two) stopped at now time", reading1.timestamp + 1, Sensor.lastStopped().stopped_at);
        assertEquals("current sensor (three) started at now time + 1 ms", Sensor.lastStopped().stopped_at + 1, Sensor.currentSensor().started_at);


        time += Constants.HOUR_IN_MS;
        // add a reading
        BgReading reading2 = BgReading.bgReadingInsertFromG5(124, time, "Backfill");
        assertNotNull("bg reading created 2", reading2);

        time += Constants.HOUR_IN_MS;
        Sensor.setUnitTestStopTime(time); // force stop time to this

        long timeAtStop = time;
        // Manually stop the sensor
        Sensor.stopSensor();
        assertNull("fourth sensor is not active", Sensor.currentSensor());
        assertFalse("sensor 4 is not running", Sensor.isActive());


        // advance time another hour
        time += Constants.HOUR_IN_MS;
        Sensor.setUnitTestStopTime(time); // force stop time to this

        // Try and start a sensor 3 hours in the past which should then be started at the last reading time
        Sensor four = Sensor.create(time - Constants.HOUR_IN_MS * 3);
        //System.out.println("FOUR:" + four);


        assertNotNull("fourth sensor is active sensor", Sensor.currentSensor());
        assertEquals("fourth sensor is active sensor we know", four, Sensor.currentSensor());
        // assertSame("first sensor is active sensor we know",one, Sensor.currentSensor());
        assertEquals("third sensor is previous sensor", three, Sensor.lastStopped());
        assertTrue("sensor 4 is running", Sensor.isActive());

        assertEquals("last sensor (three) stopped at now time when stopped", timeAtStop, Sensor.lastStopped().stopped_at);
        assertEquals("current sensor (four) started at now time + 1 ms", Sensor.lastStopped().stopped_at + 1, Sensor.currentSensor().started_at);

    }
}