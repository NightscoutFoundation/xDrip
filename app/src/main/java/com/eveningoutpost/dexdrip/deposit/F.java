package com.eveningoutpost.dexdrip.deposit;

import android.os.Build;

import java.util.function.Consumer;

import android.support.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)

// F is just shorthand for typical functional interface
@FunctionalInterface
interface F extends Consumer<String> {
    default void apply(String t) {
        accept(t);
    }
}
