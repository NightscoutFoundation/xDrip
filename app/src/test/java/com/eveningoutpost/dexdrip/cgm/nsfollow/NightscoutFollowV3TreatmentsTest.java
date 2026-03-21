package com.eveningoutpost.dexdrip.cgm.nsfollow;

import static com.google.common.truth.Truth.assertThat;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Verifies v3→v1 treatment field normalization in {@link NightscoutFollowV3}.
 * <p>
 * The v3 API returns {@code identifier} where the NightscoutTreatments processor
 * expects {@code _id}. The normalizer maps the field before processing.
 *
 * @author Asbjørn Aarrestad
 */
public class NightscoutFollowV3TreatmentsTest extends RobolectricTestWithConfig {

    /** Parses a JSON string into a JsonObject for test setup. */
    private static JsonObject json(final String s) {
        return new Gson().fromJson(s, JsonObject.class);
    }

    // ===== normalizeV3Treatments =================================================================

    @Test
    public void normalizeV3Treatments_addsIdFromIdentifier() {
        // :: Setup — v3 item with identifier but no _id
        List<JsonObject> items = Collections.singletonList(json(
                "{\"identifier\":\"abc-123\",\"eventType\":\"Correction Bolus\",\"insulin\":0.5}"));

        // :: Act
        NightscoutFollowV3.normalizeV3Treatments(items);

        // :: Verify — _id is added from identifier
        assertThat(items.get(0).get("_id").getAsString()).isEqualTo("abc-123");
    }

    @Test
    public void normalizeV3Treatments_doesNotOverwriteExistingId() {
        // :: Setup — item already has both _id and identifier
        List<JsonObject> items = Collections.singletonList(json(
                "{\"_id\":\"original\",\"identifier\":\"new\",\"eventType\":\"Correction Bolus\"}"));

        // :: Act
        NightscoutFollowV3.normalizeV3Treatments(items);

        // :: Verify — _id is unchanged
        assertThat(items.get(0).get("_id").getAsString()).isEqualTo("original");
    }

    @Test
    public void normalizeV3Treatments_handlesMultipleItems() {
        // :: Setup — two v3 items
        List<JsonObject> items = new ArrayList<>();
        items.add(json("{\"identifier\":\"id-1\",\"eventType\":\"Correction Bolus\",\"insulin\":0.5}"));
        items.add(json("{\"identifier\":\"id-2\",\"eventType\":\"Meal Bolus\",\"carbs\":30.0}"));

        // :: Act
        NightscoutFollowV3.normalizeV3Treatments(items);

        // :: Verify — both items have _id set
        assertThat(items.get(0).get("_id").getAsString()).isEqualTo("id-1");
        assertThat(items.get(1).get("_id").getAsString()).isEqualTo("id-2");
    }

    @Test
    public void normalizeV3Treatments_handlesEmptyList() {
        // :: Setup
        List<JsonObject> items = Collections.emptyList();

        // :: Act + Verify — no exception
        NightscoutFollowV3.normalizeV3Treatments(items);
    }

    @Test
    public void normalizeV3Treatments_preservesAllOtherFields() {
        // :: Setup
        List<JsonObject> items = Collections.singletonList(json(
                "{\"identifier\":\"abc-123\",\"eventType\":\"Correction Bolus\","
                        + "\"insulin\":1.2,\"carbs\":30.0,\"created_at\":\"2026-03-22T10:25:22.246Z\"}"));

        // :: Act
        NightscoutFollowV3.normalizeV3Treatments(items);

        // :: Verify — other fields are intact
        JsonObject item = items.get(0);
        assertThat(item.get("eventType").getAsString()).isEqualTo("Correction Bolus");
        assertThat(item.get("insulin").getAsDouble()).isEqualTo(1.2);
        assertThat(item.get("carbs").getAsDouble()).isEqualTo(30.0);
        assertThat(item.get("created_at").getAsString()).isEqualTo("2026-03-22T10:25:22.246Z");
    }
}
