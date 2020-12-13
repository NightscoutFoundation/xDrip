package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;


import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

public class TextSettings {

    @SerializedName("font_size")
    public int fontSize = 10;
    private String color = "#FFFFFF";

    public int getColor() {
        return Color.parseColor(color);
    }
}
