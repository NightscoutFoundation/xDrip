package com.eveningoutpost.dexdrip.cgm.medtrumfollow;

import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.google.gson.JsonParser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MedtrumFollowDataTest {

    @Test
    public void selectsConfiguredPatientAndConvertsMmol() {
        final String json = "{\"res\":\"OK\",\"monitorlist\":["
                + "{\"username\":\"first@example.com\"},"
                + "{\"username\":\"patient@example.com\",\"sensor_status\":{"
                + "\"glucose\":7.1,\"glucoseRate\":3,\"updateTime\":1752563388}}]}";

        final MedtrumFollowData.Result result = MedtrumFollowData.parseMonitor(
                new JsonParser().parse(json).getAsJsonObject(), "PATIENT@example.com");

        assertTrue(result.isSuccessful());
        assertEquals("patient@example.com", result.patient);
        assertEquals(1, result.entries.size());
        assertEquals(1752563388000L, result.entries.get(0).timestamp);
        assertEquals(7.1 * Constants.MMOLL_TO_MGDL, result.entries.get(0).glucose, 0.001);
        assertEquals("DoubleUp", result.entries.get(0).trendName);
    }

    @Test
    public void choosesFirstPatientWithSensorAndKeepsMgDl() {
        final String json = "{\"monitorlist\":["
                + "{\"username\":\"no-sensor@example.com\"},"
                + "{\"username\":\"sensor@example.com\",\"sensor_status\":{"
                + "\"glucose\":128,\"glucoseRate\":8,\"updateTime\":100}}]}";

        final MedtrumFollowData.Result result = MedtrumFollowData.parseMonitor(
                new JsonParser().parse(json).getAsJsonObject(), "");

        assertTrue(result.isSuccessful());
        assertEquals("sensor@example.com", result.patient);
        assertEquals(128, result.entries.get(0).glucose, 0);
        assertEquals("Flat", result.entries.get(0).trendName);
    }

    @Test
    public void reportsWarmupInsteadOfImportingZero() {
        final String json = "{\"res\":\"OK\",\"monitorlist\":[{\"username\":\"p\","
                + "\"sensor_status\":{\"glucose\":0,\"updateTime\":100,\"sequence\":15}}]}";

        final MedtrumFollowData.Result result = MedtrumFollowData.parseMonitor(
                new JsonParser().parse(json).getAsJsonObject(), "p");

        assertFalse(result.isSuccessful());
        assertTrue(result.error.contains("warming up"));
        assertTrue(result.entries.isEmpty());
    }

    @Test
    public void rejectsOutOfRangeGlucose() {
        final String json = "{\"monitorlist\":[{\"username\":\"p\",\"sensor_status\":{"
                + "\"glucose\":999,\"updateTime\":100}}]}";

        final MedtrumFollowData.Result result = MedtrumFollowData.parseMonitor(
                new JsonParser().parse(json).getAsJsonObject(), "p");

        assertFalse(result.isSuccessful());
        assertTrue(result.error.contains("out-of-range"));
    }

    @Test
    public void parsesValidGraphRowsAndSkipsMalformedOrOldRows() {
        final String json = "{\"res\":\"OK\",\"data\":["
                + "[\"id1\",100,11.7,4.8,\"C\",0],"
                + "[\"bad\"],"
                + "[\"id2\",200,12.0,5.0,\"C\",0]]}";

        final MedtrumFollowData.Result result = MedtrumFollowData.parseGraph(
                new JsonParser().parse(json).getAsJsonObject(), 150000L);

        assertTrue(result.isSuccessful());
        assertEquals(1, result.entries.size());
        assertEquals(200000L, result.entries.get(0).timestamp);
        assertEquals(5.0 * Constants.MMOLL_TO_MGDL, result.entries.get(0).glucose, 0.001);
        assertNull(result.entries.get(0).trendName);
    }

    @Test
    public void exposesServerErrorMessage() {
        final MedtrumFollowData.Result result = MedtrumFollowData.parseMonitor(
                new JsonParser().parse("{\"res\":\"Err\",\"msg\":\"Invalid session\"}")
                        .getAsJsonObject(), "");

        assertFalse(result.isSuccessful());
        assertEquals("Invalid session", result.error);
    }

    @Test
    public void onlyAllowsKnownServerDomains() {
        assertEquals("https://easyview.medtrum.eu/", MedtrumFollowDownloader.serverUrl("eu"));
        assertEquals("https://easyview.medtrum.fr/", MedtrumFollowDownloader.serverUrl("fr"));
        assertEquals("https://easyview.medtrum.eu/", MedtrumFollowDownloader.serverUrl("evil.example"));
    }
}


