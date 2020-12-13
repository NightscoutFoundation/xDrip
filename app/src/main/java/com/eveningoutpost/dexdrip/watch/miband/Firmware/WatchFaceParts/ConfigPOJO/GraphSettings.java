package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import android.graphics.Color;

import com.google.gson.annotations.SerializedName;

public class GraphSettings {
    public Position position = new Position();
    public int width = -1;
    public int height = 0;
    @SerializedName("display_graph")
    public boolean displayGraph = false;
    @SerializedName("bg_color")
    private String bgColor = "#FFFFFF";

    public int getBgColor() {
        return Color.parseColor(bgColor);
    }
}
