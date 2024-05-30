package com.eveningoutpost.dexdrip.alert;

import com.eveningoutpost.dexdrip.models.UserError.Log;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.Getter;
import lombok.val;

/**
 * JamOrHam
 *
 * BaseAlert abstract helper class
 */

public abstract class BaseAlert implements Condition, Action, Pollable {

    @Expose
    @Getter
    private final String name;
    @Expose
    @Getter
    private final List<When> when = new ArrayList<>();

    @Expose
    private boolean oneShot;


    protected When lastEvent;

    public BaseAlert(final String name, final When... when) {
        this.name = name;
        this.when.addAll(Arrays.asList(when));
    }

    public BaseAlert setOneShot(final boolean oneShot) {
        this.oneShot = oneShot;
        return this;
    }

    private void log(final String msg) {
        Log.uel("Alert:" + name, msg); // TODO i18n ?
    }

    @Override
    public PollResult poll(final When event) {
        val ret = new PollResult();
        if (when.contains(event)) {
            lastEvent = event;
            if (isMet()) {
                val result = activate();
                ret.triggered = true;
                log("Activated: " + result); // TODO i18n?
                ret.remove = oneShot;
            }
        }
        return ret;
    }

    @Override
    public String group() {
        return name;
    }

    @Override
    public int sortPos() {
        return 0;
    }

}
