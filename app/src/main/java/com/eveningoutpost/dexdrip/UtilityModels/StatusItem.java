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
    public String button_name;
    public Runnable runnable;


    public StatusItem(String name, String value) {
        this(name, value, Highlight.NORMAL);
    }

    public StatusItem() {
        this("line-break", "", Highlight.NORMAL);
    }

    public StatusItem(String name, Highlight highlight) {
        this("heading-break", name, highlight);
    }

    public StatusItem(String name, Runnable runnable) {
        this("button-break", "", Highlight.NORMAL, name, runnable);
    }

    public StatusItem(String name, String value, Highlight highlight) {
        this(name, value, highlight, null, null);
    }

    public StatusItem(String name, String value, Highlight highlight, String button_name, Runnable runnable) {
        this.name = name;
        this.value = value;
        this.highlight = highlight;
        this.button_name = button_name;
        this.runnable = runnable;
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
