package com.eveningoutpost.dexdrip.utils.validation;


import javax.annotation.Nullable;

public class Ensure {

    public static void notNull(Object o, @Nullable String message) {
        if (o == null) {
            throw new IllegalArgumentException(message!=null?message:"Argument supplied was null");
        }
    }
    public static void notNullAny(Object ...o) {
        if (o == null) {
            throw new IllegalArgumentException("Argument(s) supplied were null");
        }
        for (Object i: o) {
            Ensure.notNull(o,"At least one of the arguments supplied was null");
        }
    }
}
