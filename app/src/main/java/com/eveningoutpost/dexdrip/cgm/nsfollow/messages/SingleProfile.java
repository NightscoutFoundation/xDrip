package com.eveningoutpost.dexdrip.cgm.nsfollow.messages;

import com.google.gson.annotations.Expose;

import java.util.ArrayList;

public class SingleProfile {
    @Expose
    public ArrayList<BasalProfileEntry> basal;
}
