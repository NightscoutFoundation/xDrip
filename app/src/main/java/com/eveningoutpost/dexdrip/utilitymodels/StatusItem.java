package com.eveningoutpost.dexdrip.utilitymodels;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;

/**
 * Created by jamorham on 14/01/2017.
 * <p>
 * For representing row items suitable for MegaStatus
 */
public class StatusItem {

    public enum Highlight {
        NORMAL(Color.TRANSPARENT),
        GOOD(Color.parseColor("#003000")),
        BAD(Color.parseColor("#480000")),
        NOTICE(Color.parseColor("#403000")),
        CRITICAL(Color.parseColor("#770000"));

        int colorHint;

        Highlight(int colorHint) {
            this.colorHint = colorHint;
        }

        @ColorInt
        public int color() {
            return colorHint;
        }
    }

    public String name;
    public String value;
    public Highlight highlight;
    public String button_name;
    public Runnable runnable;

    public StatusItem(String name, String value) {
        this(name, value, NORMAL);
    }

    public StatusItem() {
        this("line-break", "", NORMAL);
    }

    public StatusItem(String name, Highlight highlight) {
        this("heading-break", name, highlight);
    }

    public StatusItem(String name, Runnable runnable) {
        this("button-break", "", NORMAL, name, runnable);
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
        this(name, value, NORMAL);
    }

    public StatusItem(String name, Integer value, Highlight highlight) {
        this.name = name;
        this.value = Integer.toString(value);
        this.highlight = highlight;
    }
}