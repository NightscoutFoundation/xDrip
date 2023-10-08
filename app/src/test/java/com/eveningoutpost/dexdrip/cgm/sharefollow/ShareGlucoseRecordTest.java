package com.eveningoutpost.dexdrip.cgm.sharefollow;

import com.eveningoutpost.dexdrip.importedlibraries.dexcom.Dex_Constants;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.truth.Truth.assertWithMessage;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShareGlucoseRecordTest {

    private GsonBuilder gsonBuilder;
    private static Gson gson;

    @BeforeClass
    public static void setUp() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Dex_Constants.TREND_ARROW_VALUES.class, new ShareTrendDeserializer());
        gson = gsonBuilder.create();
    }

    @Test
    public void slopePerMsFromDirectionTest() {
        for (int direction = 1; direction < 8; direction++) {
            final ShareGlucoseRecord record = new ShareGlucoseRecord();
            record.Trend = Dex_Constants.TREND_ARROW_VALUES.getEnum(direction);
            assertWithMessage("Slope check " + direction).that(BgReading.slopeName(record.slopePerMsFromDirection() * 60000)).isEqualTo(record.Trend.friendlyTrendName());
        }
    }

    @Test
    public void deserializeTrendNumber() {
        String json = "{\"WT\":\"Date(1638396953000)\",\"ST\":\"Date(1638396953000)\",\"DT\":\"Date(1638396953000+0000)\",\"Value\":167,\"Trend\":4,\"extraString\":\"two\",\"extraFloat\":2.2}";
        ShareGlucoseRecord record = gson.fromJson(json, ShareGlucoseRecord.class);
        assertEquals(new Double(167), record.Value);
        assertEquals(Dex_Constants.TREND_ARROW_VALUES.FLAT, record.Trend);
    }

    @Test
    public void deserializeTrendText() {
        String json = "{\"WT\":\"Date(1638396953000)\",\"ST\":\"Date(1638396953000)\",\"DT\":\"Date(1638396953000+0000)\",\"Value\":167,\"Trend\":\"Flat\"}";
        ShareGlucoseRecord record = gson.fromJson(json, ShareGlucoseRecord.class);
        assertEquals(new Double(167), record.Value);
        assertEquals(Dex_Constants.TREND_ARROW_VALUES.FLAT, record.Trend);
    }

    @Test
    public void deserializeIntoArray() {
        String json = "[{\"WT\":\"Date(1638396953000)\",\"ST\":\"Date(1638396953000)\",\"DT\":\"Date(1638396953000+0000)\",\"Value\":167,\"Trend\":\"Flat\"},{\"WT\":\"Date(1638386459000)\",\"ST\":\"Date(1638386459000)\",\"DT\":\"Date(1638386459000+0000)\",\"Value\":147,\"Trend\":\"SINGLE_DOWN\"}]";
        Type targetClassType = new TypeToken<ArrayList<ShareGlucoseRecord>>() { }.getType();
        Collection<ShareGlucoseRecord> records = gson.fromJson(json, targetClassType);
        assertTrue(records instanceof ArrayList);
        assertEquals(2, records.size());
        assertEquals(Dex_Constants.TREND_ARROW_VALUES.SINGLE_DOWN, ((ArrayList<ShareGlucoseRecord>) records).get(1).Trend);
    }

}