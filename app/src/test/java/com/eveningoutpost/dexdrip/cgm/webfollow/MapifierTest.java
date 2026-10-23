package com.eveningoutpost.dexdrip.cgm.webfollow;

import org.junit.Test;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * Behavioural tests for {@link Mapifier}, which web-follow uses to walk a JSON response by dotted path.
 * <p>
 * The walk descends into nested objects by checking the runtime type gson gives them, and returns numbers in
 * gson's {@code Double} form. A gson upgrade that changes either would break every web-follow lookup silently.
 *
 * @author Asbjørn Aarrestad - 2026.10
 */
public class MapifierTest {

    private static final String RESPONSE =
            "{\"data\":{\"user\":{\"name\":\"Alice\"},\"readings\":[{\"value\":\"120\"},{\"value\":\"118\"}],"
                    + "\"one\":[{\"id\":\"only\"}],\"glucose\":{\"mgdl\":120}}}";

    // ===== Nested objects ============================================================================================

    /** A dotted path descends through nested objects to the leaf value. */
    @Test
    public void pluckString_nestedObject_returnsLeaf() {
        // :: Setup
        final Mapifier map = new Mapifier(RESPONSE);

        // :: Act
        final String name = map.pluckString("data.user.name");

        // :: Verify
        assertThat(name).isEqualTo("Alice");
    }

    /** A path through a single-element array descends into that element. */
    @Test
    public void pluckString_throughSingleElementArray_returnsLeaf() {
        // :: Setup
        final Mapifier map = new Mapifier(RESPONSE);

        // :: Act
        final String id = map.pluckString("data.one.id");

        // :: Verify
        assertThat(id).isEqualTo("only");
    }

    // ===== Numbers ===================================================================================================

    /** A JSON integer comes back in gson's double form, which {@code pluckDouble} parses. */
    @Test
    public void pluck_numericLeaf_returnsGsonDoubleForm() {
        // :: Setup
        final Mapifier map = new Mapifier(RESPONSE);

        // :: Act
        final String asString = map.pluckString("data.glucose.mgdl");
        final Double asDouble = map.pluckDouble("data.glucose.mgdl");

        // :: Verify
        assertThat(asString).isEqualTo("120.0");
        assertThat(asDouble).isEqualTo(120.0);
    }

    // ===== Arrays ====================================================================================================

    /** A path ending at an array returns the whole array. */
    @Test
    public void pluckAny_pathEndingAtArray_returnsAllElements() {
        // :: Setup
        final Mapifier map = new Mapifier(RESPONSE);

        // :: Act
        final Object readings = map.pluckAny("data.readings");

        // :: Verify
        assertThat((List<?>) readings).hasSize(2);
    }

    /** A single-value lookup through an array with more than one element refuses rather than guessing. */
    @Test
    public void pluckString_throughMultiElementArray_throws() {
        // :: Setup
        final Mapifier map = new Mapifier(RESPONSE);

        // :: Act
        final RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> map.pluckString("data.readings.value"));

        // :: Verify
        assertThat(thrown).hasMessageThat().contains("Invalid singular");
    }
}
