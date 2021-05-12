package com.eveningoutpost.dexdrip.utils;

// jamorham TriState Boolean

public class TriState {

    private volatile Boolean value;

    public TriState() {
    }

    public TriState(boolean value) {
        this.value = value;
    }

    public boolean isNull() {
        return value == null;
    }

    public boolean isTrue() {
        return value != null && value;
    }

    public boolean isFalse() {
        return value != null && !value;
    }

    public void set(boolean value) {
        this.value = value;
    }

    public void setTrue() {
        value = true;
    }

    public void setFalse() {
        value = false;
    }

    public Object trinary(Object nullResult, Object trueResult, Object falseResult) {
        return value == null ? nullResult : value ? trueResult : falseResult;
    }

}
