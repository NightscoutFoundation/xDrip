package xrwd.li3.util;

/**
 * Fixed addresses and IDs are kept reversed in the source, so a plain-text search does not find them; this turns
 * them back.
 *
 * @author Asbjørn Aarrestad - 2026.11
 */
public final class Reversed {

    private Reversed() {
    }

    /** The string with its characters in reverse order. */
    public static String of(String reversed) {
        return new StringBuilder(reversed).reverse().toString();
    }
}
