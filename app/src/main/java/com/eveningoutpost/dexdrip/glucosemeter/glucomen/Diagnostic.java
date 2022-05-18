package com.eveningoutpost.dexdrip.glucosemeter.glucomen;

import lombok.RequiredArgsConstructor;

/**
 * JamOrHam
 * GlucoMen diagnostics
 */

@RequiredArgsConstructor
public class Diagnostic {

    private final GlucoMenNfc p;

    private static final String TAG = "GluocoMenDiag";

    public boolean check(final int stage) {
        // used during development
        return true;
    }

}
