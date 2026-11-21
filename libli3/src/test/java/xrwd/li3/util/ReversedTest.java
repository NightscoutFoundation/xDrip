package xrwd.li3.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

/**
 * Tests for {@link Reversed}: the reversal that keeps fixed addresses and IDs out of plain text in the source.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public class ReversedTest {

    // ====================================================== of =======================================================

    /** A string comes back with its characters in reverse order. */
    @Test
    public void of_reversesCharacters() {
        // :: Act
        String reversed = Reversed.of("/ten.elpmaxe//:sptth");

        // :: Verify
        assertThat(reversed).isEqualTo("https://example.net/");
    }

    /** Reversing twice gives the original back. */
    @Test
    public void of_twice_givesOriginal() {
        // :: Act
        String twice = Reversed.of(Reversed.of("abc-123"));

        // :: Verify
        assertThat(twice).isEqualTo("abc-123");
    }
}
