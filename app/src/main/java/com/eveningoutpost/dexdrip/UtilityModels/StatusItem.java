package com.eveningoutpost.dexdrip.UtilityModels;

/**
 * Created by jamorham on 14/01/2017.
 * <p>
 * For representing row items suitable for MegaStatus
 */

public class StatusItem {

    public enum Highlight {
        NORMAL,
        GOOD,
        BAD,
        NOTICE,
        CRITICAL
    }

    public String name;
    public String value;
    public Highlight highlight;


    public StatusItem(String name, String value) {
        this(name, value, Highlight.NORMAL);
    }

    public StatusItem(String name, String value, Highlight highlight) {
        this.name = name;
        this.value = value;
        this.highlight = highlight;
    }

    public StatusItem(String name, Integer value) {
        this(name, value, Highlight.NORMAL);
    }

    public StatusItem(String name, Integer value, Highlight highlight) {
        this.name = name;
        this.value = Integer.toString(value);
        this.highlight = highlight;
    }

}
