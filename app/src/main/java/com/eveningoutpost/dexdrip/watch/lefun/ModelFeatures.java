package com.eveningoutpost.dexdrip.watch.lefun;

// jamorham

import static com.eveningoutpost.dexdrip.models.JoH.emptyString;

public class ModelFeatures {

    static int getScreenWidth() {
        return getScreenWidth(LeFun.getModel());
    }

    static int getScreenWidth(final String model) {

        if (emptyString(model)) return 4; // unknown default

        switch (model) {

            case "W3":
            case "F3S":
            case "F11":
                return 7;

            case "F3":          // doesn't id by string
                return 10;

            case "F12":
            default:
                return 4;


        }
    }

}
