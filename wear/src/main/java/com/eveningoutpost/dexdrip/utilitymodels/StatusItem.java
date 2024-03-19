package com.eveningoutpost.dexdrip.utilitymodels;

import android.graphics.Color;
import androidx.annotation.ColorInt;

import com.google.common.base.MoreObjects;

import java.util.HashMap;

import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.BAD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.CRITICAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.GOOD;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NORMAL;
import static com.eveningoutpost.dexdrip.utilitymodels.StatusItem.Highlight.NOTICE;

/**
 * Created by jamorham on 14/01/2017.
 * <p>
 * For representing row items suitable for MegaStatus
 */

public class StatusItem {

    private static final HashMap<Highlight, Integer> colorHints = new HashMap<>();

    static {
        colorHints.put(NORMAL, Color.TRANSPARENT);
        colorHints.put(GOOD, Color.parseColor("#003000"));
        colorHints.put(BAD, Color.parseColor("#480000"));
        colorHints.put(NOTICE, Color.parseColor("#403000"));
        colorHints.put(CRITICAL, Color.parseColor("#770000"));
    }

    public enum Highlight {
        NORMAL,
        GOOD,
        BAD,
        NOTICE,
        CRITICAL;

        @ColorInt
        public int color() {
            return colorHint(this);
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

    @ColorInt
    public static int colorHint(final Highlight highlight) {
        return MoreObjects.firstNonNull(colorHints.get(highlight), Color.TRANSPARENT);
    }

}
