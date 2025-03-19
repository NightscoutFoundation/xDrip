package com.eveningoutpost.dexdrip.profileeditor;

public class BasalProfileEntryTimed {
    public Float absolute = null;
    public Long timestamp = null;

    BasalProfileEntryTimed(Float absolute, Long timestamp) {
        this.absolute = absolute;
        this.timestamp = timestamp;
    }
}
