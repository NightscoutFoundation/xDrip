package com.eveningoutpost.dexdrip.utils;

// jamorham

public class ExtensionMethods {

    // object.or("default value")
    public static <T> T or(T obj, T ifNull) {
        return obj != null ? obj : ifNull;
    }

    // string extension method
    public static boolean containsIgnoreCase(final String haystack, final String needle) {
        return haystack != null && needle != null && haystack.toLowerCase().contains(needle.toLowerCase());
    }

    // faster version of above where we assume needle is already lowercased and no nulls
    public static boolean containsIgnoreCaseF(final String haystack, final String needle) {
        return haystack.toLowerCase().contains(needle);
    }

}
