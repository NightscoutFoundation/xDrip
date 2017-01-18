package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jamorham on 14/01/2017.
 * <p>
 * For representing row items suitable for MegaStatus
 */

public class StatusItem {

    public String name;
    public String value;

    public StatusItem(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public StatusItem(String name, Integer value) {
        this.name = name;
        this.value = Integer.toString(value);
    }

}
