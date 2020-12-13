package com.eveningoutpost.dexdrip.watch.miband.Firmware.WatchFaceParts.ConfigPOJO;

import com.eveningoutpost.dexdrip.watch.miband.MiBandType;
import com.google.gson.annotations.SerializedName;

public class WatchfaceConfig {
    @SerializedName("canvas_image_offset")
    public int canvasImageOffset = 0;

    @SerializedName("graph")
    public GraphSettings graph = new GraphSettings();

    @SerializedName("arrow_position")
    public Position arrowPosition = new Position();

    @SerializedName("iob_text")
    public SimpleText iob = new SimpleText();

    @SerializedName("delta_text")
    public ValueTimeText deltaText = new ValueTimeText();

    @SerializedName("delta_time_text")
    public SimpleText deltaTimeText = new SimpleText();

    @SerializedName("treatment_text")
    public ValueTimeText treatmentText = new ValueTimeText();

    @SerializedName("treatment_time_text")
    public SimpleText treatmentTimeText  = new SimpleText();

}
